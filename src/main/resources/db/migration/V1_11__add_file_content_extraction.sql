alter table document_data
    add column content_hash char(64),
    add column extracted_text longtext,
    add column extraction_status varchar(32) not null default 'PENDING_REINDEX';

-- Index on content_hash drives the same-bytes dedupe lookup in DocumentMapper.
create index ix_document_data_content_hash on document_data (content_hash);

-- The default is kept so existing rows (and raw SQL seed data in tests) pick up PENDING_REINDEX
-- automatically. The application's mapper always writes an explicit status — new rows become
-- SUCCESS / FAILED / UNSUPPORTED, never PENDING_REINDEX in practice.
