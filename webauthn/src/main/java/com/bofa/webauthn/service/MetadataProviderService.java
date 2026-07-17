package com.bofa.webauthn.service;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.verifier.attestation.statement.androidkey.AndroidKeyAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.androidsafetynet.AndroidSafetyNetAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.apple.AppleAnonymousAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.none.NoneAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.packed.PackedAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.tpm.TPMAttestationStatementVerifier;
import com.webauthn4j.verifier.attestation.statement.u2f.FIDOU2FAttestationStatementVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MetadataProviderService loads FIDO Alliance MDS3 blob.jwt from resources.
 * 
 * ⚠️ IMPORTANT - USER FEEDBACK CORRECTION:
 * The original implementation used createNonStrictWebAuthnManager() which uses NullAttestationStatementVerifier.
 * This means NO actual certificate verification was happening.
 * 
 * WHAT NEEDS TO HAPPEN (for full certificate verification):
 * 1. The blob.jwt contains FIDO MDS data with root CA certificates
 * 2. The blob.jwt needs to be PASSED to WebAuthnManager during verification
 * 3. Currently webauthn4j doesn't expose a simple public API to pass metadata at verification time
 * 4. The metadata should be used to verify x5c certificate chains against FIDO MDS root CAs
 * 
 * WORKAROUND IN CURRENT IMPLEMENTATION:
 * - Load and parse blob.jwt to extract root certificates
 * - Manually verify x5c chain against extracted roots in application logic
 * - Or upgrade to webauthn4j version that supports MetadataProvider in verify() method
 */
@Slf4j
@Service
public class MetadataProviderService {

//    private WebAuthnManager webAuthnManager;
//    private boolean metadataAvailable = false;
//    private byte[] metadataBlob;
//
//    @Value("${metadata.blob.path:metadata/blob.jwt}")
//    private String blobPath;
//
//    /**
//     * Initialize and load the FIDO MDS3 blob.jwt file
//     */
//    public void loadMetadata() {
//        try {
//            log.info("═══════════════════════════════════════════════════════════════");
//            log.info("Initializing FIDO MetadataService from blob.jwt");
//            log.info("═══════════════════════════════════════════════════════════════");
//
//            if (loadFromFile()) {
//                metadataAvailable = true;
//                log.info("✓ FIDO MDS3 Metadata blob loaded successfully");
//                log.info("  Blob size: {} bytes", metadataBlob.length);
//            } else {
//                log.warn("✗ FIDO MDS3 Metadata blob not available");
//            }
//
//            // Initialize WebAuthnManager
//            initializeWebAuthnManager();
//
//        } catch (Exception e) {
//            log.error("Error during metadata initialization: {}", e.getMessage(), e);
//            initializeWebAuthnManager();
//        }
//    }
//
//    /**
//     * Load metadata blob from resource file
//     */
//    private boolean loadFromFile() {
//        try {
//            String cleanPath = blobPath.replace("classpath:", "");
//            Path path = Paths.get(cleanPath);
//
//            if (!Files.exists(path)) {
//                log.warn("  Metadata blob file not found at: {}", path.toAbsolutePath());
//                return false;
//            }
//
//            log.info("  Loading metadata blob from: {}", path.toAbsolutePath());
//            metadataBlob = Files.readAllBytes(path);
//            log.info("  Blob file size: {} bytes", metadataBlob.length);
//            return true;
//
//        } catch (IOException e) {
//            log.error("Failed to load metadata blob: {}", e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Initialize WebAuthnManager
//     *
//     * NOTE: For REAL certificate verification against FIDO MDS,
//     * the blob.jwt needs to be explicitly passed during verification.
//     * See WebAuthnRegistrationService for how x5c certificates are verified.
//     */
//    private void initializeWebAuthnManager() {
//        try {
//            // Create WebAuthnManager - currently uses null verifiers by default
//            this.webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
//
//            // initialize webauthn manager with all verifier
//            FIDOU2FAttestationStatementVerifier fidou2FAttestationStatementVerifier = new FIDOU2FAttestationStatementVerifier();
//            PackedAttestationStatementVerifier packedAttestationStatementVerifier = new PackedAttestationStatementVerifier();
//            TPMAttestationStatementVerifier tpmAttestationStatementVerifier = new TPMAttestationStatementVerifier();
//            AndroidKeyAttestationStatementVerifier androidKeyAttestationStatementVerifier = new AndroidKeyAttestationStatementVerifier();
//            AndroidSafetyNetAttestationStatementVerifier androidSafetyNetAttestationStatementVerifier = new AndroidSafetyNetAttestationStatementVerifier();
//            AppleAnonymousAttestationStatementVerifier appleAnonymousAttestationStatementVerifier = new AppleAnonymousAttestationStatementVerifier();
//            NoneAttestationStatementVerifier noneAttestationStatementVerifier = new NoneAttestationStatementVerifier();
//
//            log.info("  WebAuthnManager initialized");
//
//
//        } catch (Exception e) {
//            log.error("Failed to initialize WebAuthnManager: {}", e.getMessage());
//            throw new RuntimeException("Cannot initialize WebAuthnManager", e);
//        }
//    }
//
//    /**
//     * Get the WebAuthnManager
//     */
//    public WebAuthnManager getWebAuthnManager() {
//        return webAuthnManager;
//    }
//
//    /**
//     * Check if metadata blob is available
//     */
//    public boolean isMetadataAvailable() {
//        return metadataAvailable;
//    }
//
//    /**
//     * Get the metadata blob if available
//     */
//    public byte[] getMetadataBlob() {
//        return metadataBlob;
//    }
}
