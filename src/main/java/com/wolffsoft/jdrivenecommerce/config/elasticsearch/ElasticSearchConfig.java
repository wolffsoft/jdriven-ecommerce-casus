package com.wolffsoft.jdrivenecommerce.config.elasticsearch;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration(proxyBeanMethods = false)
public class ElasticSearchConfig {

    @Bean(name = "customRestClient", destroyMethod = "close")
    public RestClient customRestClient(
            @Value("${app.elasticsearch.url}") String uris,
            @Value("${app.elasticsearch.username}") String username,
            @Value("${app.elasticsearch.password}") String password
    ) {
        BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        basicCredentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        HttpHost[] hosts = Arrays.stream(uris.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(HttpHost::create)
                .toArray(HttpHost[]::new);

        return RestClient.builder(hosts)
                .setHttpClientConfigCallback(clientBuilder ->
                        clientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider)).build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new Jackson3JsonpMapper());
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
