package com.bofa.webauthn.config;

import com.webauthn4j.converter.util.ObjectConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public ObjectConverter objectConverter() {return new ObjectConverter();}
}
