package io.github.bohdanrakov.resourceprocessor.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Value("${song.service.url}")
    private String songServiceUrl;

    @Value("${resource.service.url}")
    private String resourceServiceUrl;

    @Bean
    public RestClient songsRestClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        return loadBalancedRestClientBuilder.clone()
                .baseUrl(songServiceUrl)
                .build();
    }

    @Bean
    public RestClient resourceRestClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder) {
        return loadBalancedRestClientBuilder.clone()
                .baseUrl(resourceServiceUrl)
                .build();
    }
}
