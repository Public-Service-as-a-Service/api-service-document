alter table document_data
    add column page_count int null,
    add column page_offsets longtext null;

-- page_offsets: CSV-encoded 0-based char offsets into extracted_text where each page starts
-- (see PageOffsetsConverter). null for formats without a page concept and for rows not yet
-- reprocessed by the V1_15 backfill. The ReindexScheduler repopulates both columns from S3.

-- Index backing ReindexScheduler.findReindexCandidates / countReindexCandidates. The predicate
-- is (extraction_status in (...)) combined with page_count is null — a composite on both columns
-- lets both queries, including the per-scrape backlog gauge, avoid a full scan as the table grows.
-- Placed here rather than on extraction_status alone because the SUCCESS branch requires the
-- page_count filter to cut the candidate set down to paged rows that still need backfill.
create index ix_document_data_reindex on document_data (extraction_status, page_count);
