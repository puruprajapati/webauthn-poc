package com.bofa.webauthn.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.anchor.TrustAnchorRepository;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.metadata.LocalFileMetadataBLOBProvider;
import com.webauthn4j.metadata.MetadataBLOBProvider;
import com.webauthn4j.metadata.anchor.AggregatingTrustAnchorRepository;
import com.webauthn4j.metadata.anchor.MetadataBLOBBasedTrustAnchorRepository;
import com.webauthn4j.verifier.attestation.statement.androidkey.AndroidKeyAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.androidsafetynet.AndroidSafetyNetAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.apple.AppleAnonymousAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.none.NoneAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.packed.PackedAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.tpm.TPMAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.u2f.FIDOU2FAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.CertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.DefaultCertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.NullCertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.self.DefaultSelfAttestationTrustworthinessVerifier;
import com.webauthn4j.verifier.attestation.trustworthiness.self.SelfAttestationTrustworthinessVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class WebAuthnConfig {
    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter, FidoServerConfig properties) throws URISyntaxException {
        log.info("======== Initializing webauthnManager with certificate attestation verfication =========");

        // initialize webauthn manager with all verifier
        FIDOU2FAttestationStatementVerifier fidou2FAttestationStatementVerifier = new FIDOU2FAttestationStatementVerifier();
        PackedAttestationStatementVerifier packedAttestationStatementVerifier = new PackedAttestationStatementVerifier();
        TPMAttestationStatementVerifier tpmAttestationStatementVerifier = new TPMAttestationStatementVerifier();
        AndroidKeyAttestationStatementVerifier androidKeyAttestationStatementVerifier = new AndroidKeyAttestationStatementVerifier();
        AndroidSafetyNetAttestationStatementVerifier androidSafetyNetAttestationStatementVerifier = new AndroidSafetyNetAttestationStatementVerifier();
        AppleAnonymousAttestationStatementVerifier appleAnonymousAttestationStatementVerifier = new AppleAnonymousAttestationStatementVerifier();
        NoneAttestationStatementVerifier noneAttestationStatementVerifier = new NoneAttestationStatementVerifier();

        CertPathTrustworthinessVerifier certPathTrustworthinessVerifier = buildCertPathVerifier(objectConverter, properties);
        SelfAttestationTrustworthinessVerifier selfAttestationTrustworthinessVerifier = new DefaultSelfAttestationTrustworthinessVerifier();

        log.info("WebAuthnManager initialized with cert-path verification: {}, self-attestation verification: {}", certPathTrustworthinessVerifier.getClass().getSimpleName(), selfAttestationTrustworthinessVerifier.getClass().getSimpleName());

        return new WebAuthnManager(
                Arrays.asList(
                        fidou2FAttestationStatementVerifier,
                        packedAttestationStatementVerifier,
                        tpmAttestationStatementVerifier,
                        androidKeyAttestationStatementVerifier,
                        androidSafetyNetAttestationStatementVerifier,
                        appleAnonymousAttestationStatementVerifier,
                        noneAttestationStatementVerifier
                ),
                certPathTrustworthinessVerifier,
                selfAttestationTrustworthinessVerifier
        );

    }


    private CertPathTrustworthinessVerifier buildCertPathVerifier(ObjectConverter objectConverter, FidoServerConfig properties) throws URISyntaxException {
        if(!properties.isCertPathVerificationEnabled() || properties.getAttesattionMode() == AttestationMode.NONE){
            return new NullCertPathTrustworthinessVerifier();
        }

        Path blobPath = Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("blob.jwt")).toURI());
        log.info("Loading FIDO MDS Blob from : {}", blobPath);

        MetadataBLOBProvider blobProvider = new LocalFileMetadataBLOBProvider(objectConverter, blobPath);
        TrustAnchorRepository mdsTrustAnchorRepository = new MetadataBLOBBasedTrustAnchorRepository(blobProvider);
        log.info("Trust Anchor Repository initialized with FIDO MDS data");

        // Aggregate MDS anchors with other like pinned roots like Apple
        TrustAnchorRepository trustAnchorRepository = withPInnedRoots(mdsTrustAnchorRepository, properties);

        DefaultCertPathTrustworthinessVerifier strictVerifier = new DefaultCertPathTrustworthinessVerifier(trustAnchorRepository);
        strictVerifier.setRevocationCheckEnabled(properties.isCertRevocationCheckEnabled());

        if(properties.getAttesattionMode() == AttestationMode.REQUIRED){
            log.info("Attestation MODE REQUIRED - strict cert-path verification (missing MDS trust anchor = rejected)");
            log.info("Revocation CRL/OCSP check: {}", properties.isCertRevocationCheckEnabled() ? "ENABLED" : "DISABLED");
            return strictVerifier;
        }

        log.info("Attestation MODE PREFERRED - best effort cert-path verification (missing MDS trust anchor = allowed)");
        log.info("Revocation CRL/OCSP check: {}", properties.isCertRevocationCheckEnabled() ? "ENABLED" : "DISABLED");
        log.info("certificates are validated when AAGUID/root is in blob.jwt, unknown roots are allowed with warnign");
        return new BestEffortCertPathTrustWorthVerifier(strictVerifier);

    }

    private TrustAnchorRepository withPInnedRoots(TrustAnchorRepository mdsTrustAnchorRepository, FidoServerConfig properties) {
        FidoServerConfig.PinnedRoots pinnedRoots = properties.getPinnedRoots();
        if(pinnedRoots == null || !pinnedRoots.isEnabled()) {
            log.info("Pinned roots verification is disabled, ONly FIDO MDS anchors will be trusted");
            return mdsTrustAnchorRepository;
        }

        List<X509Certificate> pinnedCertificates = loadPinnedRoots(pinnedRoots.getLocation());
        if(pinnedCertificates.isEmpty()){
            log.info("No pinned roots found at location '{}', only FIDO MDS anchors will be trusted", pinnedRoots.getLocation());
            return mdsTrustAnchorRepository;
        }

        log.info("Loaded {} pinned roots from location '{}', adding to trust anchor repository", pinnedCertificates.size(), pinnedRoots.getLocation());
        auditPinnedRoots(pinnedCertificates, pinnedRoots);

        if(hasAppleWebAuthnRoot(pinnedCertificates)) {
            log.info("Apple WebAuthn root found in pinned certificates");
        } else {
            log.warn("Apple webauthn root ca is not pinned, apple attestation will be allowed in preferred mod bu not fully chain validated");
        }

        PinnedRootTrustAnchorRepository pinnedRepo = new PinnedRootTrustAnchorRepository(pinnedCertificates);
        return new AggregatingTrustAnchorRepository(mdsTrustAnchorRepository, pinnedRepo);
    }

    private boolean hasAppleWebAuthnRoot(List<X509Certificate> pinnedCertificates) {
//        for (X509Certificate cert : pinnedCertificates) {
//            if ("Apple WebAuthn Root".equals(cert.getSubjectX500Principal().getName())) {
//                return true;
//            }
//        }
//        return false;
        return pinnedCertificates.stream().anyMatch(cert -> cert.getSubjectX500Principal().getName().contains("CN=Apple WebAuthn Root CA"));
    }

    private void auditPinnedRoots(List<X509Certificate> pinnedCertificates, FidoServerConfig.PinnedRoots pinnedRoots) {
        // log startup diagnostics for pinned roots and performs roatation/drift checks
    }

    private List<X509Certificate> loadPinnedRoots(String location) {
        List<X509Certificate> pinnedCertificates = new java.util.ArrayList<>();
        try{
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for(String ext: new String[]{"pem", "crt", "cer", "der"}){
                Resource[] resources = resolver.getResources("classpath*:" + location + "/*." + ext);
                for(Resource resource: resources){
                    try(InputStream is = resource.getInputStream()){
                        for(Certificate certificate: cf.generateCertificates(is)){
                            if(certificate instanceof X509Certificate){
                                pinnedCertificates.add((X509Certificate) certificate);
                                log.info("Pinned root loaded: {} (subject : {})", resource.getFilename(), ((X509Certificate) certificate).getSubjectX500Principal().getName());
                            }
                        }
                    } catch (Exception e) {
                        log.warn(" skipping unreadable pinned root '{}' : {}", resource.getFilename(), e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error occurred while loading pinned roots {} {}", location, e.getMessage());
        }
        return pinnedCertificates;
    }
}
