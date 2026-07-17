package com.bofa.webauthn.config;

import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.attestation.statement.AttestationCertificatePath;
import com.webauthn4j.data.attestation.statement.CertificateBaseAttestationStatement;
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.CertPathTrustworthinessVerifier;
import com.webauthn4j.verifier.exception.TrustAnchorNotFoundException;
import lombok.extern.slf4j.Slf4j;

import java.security.cert.X509Certificate;
import java.time.Instant;


// verify device attestation certificate chain when possible but does not reject authenticator merely becaule its trush anchor is absent

@Slf4j
public class BestEffortCertPathTrustWorthVerifier implements CertPathTrustworthinessVerifier{

    private final CertPathTrustworthinessVerifier delegate;

    public BestEffortCertPathTrustWorthVerifier(CertPathTrustworthinessVerifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public void verify(AAGUID aaguid, CertificateBaseAttestationStatement attestationStatement, Instant timeStamp){
        logChainCompositionForTrustValidation(aaguid, attestationStatement);
        log.info("Certificate path trust check: certificate receied for AAGUID {} - validating agains FIDO MDS.... {}", aaguid, timeStamp);
        try{
            delegate.verify(aaguid, attestationStatement, timeStamp);
            AttestationTrustContext.setStatus(AttestationTrustContext.Status.TRUSTED);
            log.info("Certificate path trust check: certificate chain validated agains t a FIDO MDS trust anchor for AAGUID {} - proceeding with registration", aaguid);
        }catch (TrustAnchorNotFoundException e) {
            AttestationTrustContext.setStatus(AttestationTrustContext.Status.UNTRUSTED_ALLOWED);
            log.warn("Certificate path trust check: No trust anchor found for AAGUID {} - proceeding with best effort verification", aaguid);
        }
    }

    private void logChainCompositionForTrustValidation(AAGUID aaguid, CertificateBaseAttestationStatement attestationStatement) {
        AttestationCertificatePath x5c = attestationStatement.getX5c();
        if(x5c == null || x5c.isEmpty()){
            log.info("Certificate-path trust check: no x5c chain present for AAGUID {} - skipping trust validation", aaguid);

        }

        // AttestationCertificatePath extends ABstractList<X509Certificate> - use direclty as a list
        int chainSize = x5c.size();
        boolean leafOnly = chainSize == 1;

        log.info("-------------- Trust store validation MDS for AAGUID {} --------", aaguid);
        log.info("Chain received                  : {} certificate(s)", chainSize);

        if(leafOnly){
            log.info("Chain received                  : leaf only - no intermediate or root certificate present");
            log.info("Validation path                 : FIDO MDS will try to chain teh LEAF certificate's issuer direclty to a trust anchor");
            X509Certificate leafCert = x5c.get(0);
            log.info("Leaf subject                    : {}", leafCert.getSubjectX500Principal());
            log.info("Leaf issuer (-> trust store)    : {}", leafCert.getIssuerX500Principal().getName());

        } else {
            int intermediateCount = chainSize - 1;
            log.info("Chain received                  : {} intermediate certificate(s) present", intermediateCount);
            log.info("Validation path                 : FIDO MDS will try to chain the LEAF certificate's issuer to the first intermediate, then chain the last intermediate to a trust anchor");
            X509Certificate leafCert = x5c.get(0);
            X509Certificate firstIntermediateCert = x5c.get(1);
            X509Certificate lastIntermediateCert = x5c.get(intermediateCount);
            log.info("Leaf subject                    : {}", leafCert.getSubjectX500Principal());
            log.info("Leaf issuer (-> first intermediate): {}", leafCert.getIssuerX500Principal().getName());
            log.info("First intermediate subject      : {}", firstIntermediateCert.getSubjectX500Principal());
            log.info("First intermediate issuer       : {}", firstIntermediateCert.getIssuerX500Principal().getName());
            log.info("Last intermediate subject       : {}", lastIntermediateCert.getSubjectX500Principal());
            log.info("Last intermediate issuer        : {}", lastIntermediateCert.getIssuerX500Principal().getName());

            for(int i=1;i<chainSize;i++){
                X509Certificate intermediateCert = x5c.get(i);

                log.info("Intermediate certificate {}     : {}", i, intermediateCert.getSubjectX500Principal());
                log.info("Intermediate validation path {}     : {}", i, intermediateCert.getIssuerX500Principal().getName());
            }
        }
    }
}
