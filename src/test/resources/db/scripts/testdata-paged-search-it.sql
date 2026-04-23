-- Seed for PagedSearchIT. Isolated from testdata-it.sql so this test can assert exact match
-- offsets (which depend on the extracted_text byte layout) without the other IT's hardcoded
-- totalRecords values drifting.
--
-- Single document → single data row → pre-computed extracted_text + page_offsets that simulate
-- a 2-page PDF. The text is chosen so "bandwidth" appears once per page, giving deterministic
-- match offsets for the assertion.

INSERT INTO document_type (id, created, last_updated, created_by, display_name, last_updated_by, municipality_id, `type`)
VALUES ('86b9efc9-c649-40d5-ade0-ac415ea146f1', '2024-10-25 14:00:00.000', null,
        'a0000001-0000-0000-0000-000000000001', 'Anställningsbevis', null, '2281', 'EMPLOYEE_CERTIFICATE');

INSERT INTO document (id, revision, created, created_by, registration_number, confidential, legal_citation, archive,
                      title, description, document_type_id, municipality_id, valid_from, valid_to, status)
VALUES ('dddd0000-0000-0000-0000-000000001111', 1, '2026-01-01 00:00:00.000',
        'a0000001-0000-0000-0000-000000000001', '2026-2281-0001', false, null, false,
        'Paged search test', 'Paged search test',
        '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', null, null, 'ACTIVE');

-- extracted_text: "Router bandwidth spec. Total bandwidth per node."
-- "bandwidth" occurs at char offsets 7 (page 1) and 29 (page 2).
-- Page 2 starts at offset 23 ("T" of "Total").
-- storage_locator reuses a key seeded in MinIO by AbstractDocumentAppTest — /file-matches
-- doesn't stream bytes, so content doesn't matter for this test.
INSERT INTO document_data (id, document_id, storage_locator, file_name, file_size_in_bytes, mime_type,
                           extracted_text, extraction_status, page_count, page_offsets)
VALUES ('eeee0000-0000-0000-0000-000000001111', 'dddd0000-0000-0000-0000-000000001111',
        'd35254ce-d26c-47e3-806f-4cf68cf2fa56', 'paged.pdf', 1024, 'application/pdf',
        'Router bandwidth spec. Total bandwidth per node.', 'SUCCESS', 2, '0,23');
