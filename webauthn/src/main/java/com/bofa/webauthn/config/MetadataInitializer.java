package com.bofa.webauthn.config;

import com.bofa.webauthn.service.MetadataProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes MetadataProvider on application startup
 * This ensures FIDO MDS3 metadata is available before the first registration
 */
@Slf4j
@Component
public class MetadataInitializer {
    
//    private final MetadataProviderService metadataProviderService;
//
//    public MetadataInitializer(MetadataProviderService metadataProviderService) {
//        this.metadataProviderService = metadataProviderService;
//    }
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void initializeMetadata() {
//        log.info("Application startup - Loading FIDO MDS metadata...");
//        metadataProviderService.loadMetadata();
//    }
}
