package se.sundsvall.document.integration.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndexEntity, String> {
}
