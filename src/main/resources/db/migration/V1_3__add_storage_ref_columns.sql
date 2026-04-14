-- Adds per-row storage reference columns to document_data.
--
-- Stage 1 of the BinaryStore migration: rows can point at either the legacy
-- jdbc backend (document_data_binary) or a new s3 backend, selected by
-- `storage_backend`. Existing rows are backfilled to jdbc with their current
-- document_data_binary_id as the locator.
--
-- Stage 2 will drop the document_data_binary table and the jdbc rows once all
-- content has been migrated to s3.

alter table document_data
    add column storage_backend varchar(16) not null default 'jdbc',
    add column storage_locator varchar(255);

update document_data
set storage_locator = document_data_binary_id
where storage_locator is null
  and document_data_binary_id is not null;
