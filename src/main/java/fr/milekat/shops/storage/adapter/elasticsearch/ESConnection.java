package fr.milekat.shops.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;

import java.io.IOException;

public class ESConnection implements AutoCloseable {
    private final RestClient restClient;
    private final RestClientTransport transport;

    public ESConnection(RestClient restClient) {
        this.restClient = restClient;
        //  Load custom mapper(s) [Optional]
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
        jsonpMapper.objectMapper().registerModule(new CustomMappers().getModule());
        this.transport = new RestClientTransport(restClient, jsonpMapper);
    }

    public ElasticsearchClient getClient() {
        return new ElasticsearchClient(transport);
    }

    public void close() throws IOException {
        transport.close();
        restClient.close();
    }
}
