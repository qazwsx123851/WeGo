package com.wego.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Shared HTTP client configuration with connection pooling.
 *
 * Provides named RestTemplate beans backed by Apache HttpClient 5
 * with PoolingHttpClientConnectionManager for connection reuse.
 *
 * @contract
 *   - externalApiRestTemplate: connect 5s, read 10s (Google Maps, ExchangeRate, OpenWeatherMap)
 *   - geminiRestTemplate: connect 5s, read 30s (Gemini AI, longer timeout for generation)
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public PoolingHttpClientConnectionManager connectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(50);
        cm.setDefaultMaxPerRoute(10);
        return cm;
    }

    @Bean
    @Qualifier("externalApiRestTemplate")
    public RestTemplate externalApiRestTemplate(PoolingHttpClientConnectionManager connectionManager) {
        return buildRestTemplate(connectionManager, 5_000, 10_000);
    }

    @Bean
    @Qualifier("geminiRestTemplate")
    public RestTemplate geminiRestTemplate(PoolingHttpClientConnectionManager connectionManager) {
        return buildRestTemplate(connectionManager, 5_000, 30_000);
    }

    private RestTemplate buildRestTemplate(PoolingHttpClientConnectionManager connectionManager,
                                           int connectTimeoutMs, int readTimeoutMs) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectTimeoutMs);

        return new RestTemplate(factory);
    }
}
