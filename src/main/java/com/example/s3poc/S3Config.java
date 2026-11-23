package com.example.s3poc;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3Config {

    /**
     * 認証情報は SDK の既定チェーン(AWS_SHARED_CREDENTIALS_FILE 等)から取得する。
     * app.s3.endpoint が空なら実 AWS、指定があればエミュレータへ向ける。
     */
    @Bean
    S3Client s3Client(@Value("${app.s3.region}") String region,
                      @Value("${app.s3.endpoint:}") String endpoint) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.create());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}
