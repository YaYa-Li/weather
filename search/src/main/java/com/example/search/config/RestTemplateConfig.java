package com.example.search.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @LoadBalanced
    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).build();
    }

    @Primary
    @Bean
    public RestTemplate externalRestTemplate(){
        return new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(5)).build();
    }
}
