package com.bofa.webauthn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class WebauthnApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebauthnApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corConfigurer(){
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
					.allowedHeaders("*")
					.allowedMethods("GET","POST", "PUT", "DELETE", "OPTIONS");
			}
		};
	}

}
