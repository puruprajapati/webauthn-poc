package com.bofa.webauthn.config;

import com.webauthn4j.anchor.TrustAnchorRepository;
import com.webauthn4j.data.attestation.authenticator.AAGUID;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class PinnedRootTrustAnchorRepository implements TrustAnchorRepository{

    private final Set<TrustAnchor> anchors;

    public PinnedRootTrustAnchorRepository(Collection<X509Certificate> pinnedCertificates) {
        this.anchors = pinnedCertificates.stream()
                .map(cert -> new TrustAnchor(cert, null))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TrustAnchor> find(AAGUID aaguid){return anchors;}

    @Override
    public Set<TrustAnchor> find(byte[] attestationCertificateKeyIdentifier){return anchors;}
}
