INSERT INTO document_type (id, created, last_updated, created_by, display_name, last_updated_by, municipality_id,
                           `type`)
VALUES ('86b9efc9-c649-40d5-ade0-ac415ea146f1', '2024-10-25 14:00:00.000', null, 'a0000001-0000-0000-0000-000000000001', 'Anställningsbevis', null,
        '2281', 'EMPLOYEE_CERTIFICATE'),
       ('3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2024-10-25 14:00:00.000', null, 'a0000002-0000-0000-0000-000000000002', 'Semesterväxlingsdokument',
        null, '2281', 'HOLIDAY_EXCHANGE'),
       ('257506b2-f870-470d-9a1b-d095acb212a7', '2024-10-25 14:00:00.000', null, 'a0000001-0000-0000-0000-000000000001', 'Anställningsbevis', null,
        '2282', 'EMPLOYEE_CERTIFICATE'),
       ('933622d0-4b69-4d96-a204-507f31e20e61', '2024-10-25 14:00:00.000', null, 'a0000001-0000-0000-0000-000000000001', 'Sekretessavtal', null,
        '2282', 'CONFIDENTIALITY_AGREEMENT'),
       ('1e5447b7-8941-43a4-afb7-cab09375efad', '2024-10-25 14:00:00.000', null, 'a0000002-0000-0000-0000-000000000002', 'Felstavat', null, '2262',
        'MISSPELLED'),
       ('227a66a6-7485-48ba-b536-f7f487daa92c', '2024-10-25 14:00:00.000', null, 'a0000001-0000-0000-0000-000000000001', 'Typ att ta bort', null,
        '2260', 'TYPE_TO_DELETE');

-- Validity-window profiles (for search/filter tests — see reactive-leaping-marble plan):
--   P1 active   : valid_from = created, valid_to = created + 3y   (covers 2026-04-15)
--   P2 expired  : valid_from = created, valid_to = created + 1y   (ended before 2026-04-15)
--   P3 future   : valid_from = 2027-01-01, valid_to = 2028-12-31
--   P4 open-end : valid_from = created, valid_to = NULL
--   P5 always   : both NULL
-- Profile is tied to registration_number so every revision of the same document agrees.
INSERT INTO document (id, revision, created, created_by, registration_number, confidential, legal_citation, archive,
                      title, description, document_type_id, municipality_id, valid_from, valid_to, status)
VALUES ('159c10bf-1b32-471b-b2d3-c4b4b13ea152', 1, '2023-06-28 12:01:00.000', 'a0000001-0000-0000-0000-000000000001', '2023-2281-123', false, null,
        false, 'Document 1', 'Document 1', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', '2023-06-28', '2026-06-28', 'ACTIVE'),        -- Document-1, revision 1 (P1)
       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', 2, '2023-06-28 12:02:00.000', 'a0000002-0000-0000-0000-000000000002', '2023-2281-123', false, null,
        false, 'Document 1', 'Document 1', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', '2023-06-28', '2026-06-28', 'ACTIVE'),        -- Document-1, revision 2 (P1)
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', 3, '2023-06-28 12:03:00.000', 'a0000003-0000-0000-0000-000000000003', '2023-2281-123', false, null,
        true, 'Document 1', 'Document 1', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', '2023-06-28', '2026-06-28', 'ACTIVE'),         -- Document-1, revision 3 (P1)
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 1, '2023-06-28 12:01:00.000', 'a0000004-0000-0000-0000-000000000004', '2024-2281-999', true,
        'Law §7.1', true, 'Document 2', 'Document 2', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', '2023-06-28', '2024-06-28', 'ACTIVE'), -- Document-2, revision 1 (P2)
       ('435bb041-2b02-4bb3-b3e7-3782a13f47d5', 2, '2023-06-28 12:01:00.000', 'a0000005-0000-0000-0000-000000000005', '2024-2281-999', true,
        'Law §7.2', true, 'Document 2', 'Document 2', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', '2023-06-28', '2024-06-28', 'ACTIVE'), -- Document-2, revision 2 (P2)
       ('8901694b-8e3a-46b7-83ea-cd351ccc0f52', 1, '2023-06-28 12:04:00.000', 'a0000006-0000-0000-0000-000000000006', '2024-2282-666', true, null,
        true, 'Document 3', 'Document 3', '257506b2-f870-470d-9a1b-d095acb212a7', '2282', '2023-06-28', null, 'ACTIVE'),                 -- Document-3 (P4)
       ('676eaf7a-d609-4885-9743-2dbcdffe6628', 1, '2024-06-23 08:31:00.000', 'a0000005-0000-0000-0000-000000000005', '2024-2281-417', false,
        'Law §7.1', false, 'Generated Document 1', 'Generated Document 1', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2024-06-23', '2027-06-23', 'ACTIVE'), -- Gen 1 (P1)
       ('34095a16-68c5-48f8-ac1e-9d6b7dd08562', 1, '2024-02-22 08:31:00.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-403', true, null,
        false, 'Generated Document 2', 'Generated Document 2', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', null, null, 'ACTIVE'),              -- Gen 2 (P5)
       ('c3ee6cd8-d9e1-499c-b483-a9956b43ab7d', 1, '2024-09-10 08:31:00.000', 'a0000003-0000-0000-0000-000000000003', '2024-2281-370', false, null,
        true, 'Generated Document 3', 'Generated Document 3', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', '2024-09-10', '2027-09-10', 'ACTIVE'), -- Gen 3 (P1)
       ('1cc8599b-8726-4cf7-869d-36737044400c', 1, '2024-01-23 08:31:00.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-283', false,
        'Law §7.2', false, 'Generated Document 4', 'Generated Document 4', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', '2024-01-23', null, 'ACTIVE'), -- Gen 4 (P4)
       ('e4c860ad-fce3-431e-965f-0229e5610fb7', 1, '2024-08-23 08:31:00.000', 'a0000001-0000-0000-0000-000000000001', '2024-2281-200', false,
        'Law §7.2', true, 'Generated Document 5', 'Generated Document 5', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', '2027-01-01', '2028-12-31', 'ACTIVE'), -- Gen 5 (P3)
       ('84ae9dbe-159a-472f-9dc7-8ecb03c2c3c1', 1, '2024-08-29 08:31:00.000', 'a0000004-0000-0000-0000-000000000004', '2024-2281-639', false, null,
        false, 'Generated Document 6', 'Generated Document 6', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', '2024-08-29', '2027-08-29', 'ACTIVE'), -- Gen 6 (P1)
       ('019d0963-b6c6-49fb-9f88-31ef5e525a1c', 1, '2024-02-05 08:31:00.000', 'a0000004-0000-0000-0000-000000000004', '2024-2281-991', false,
        'Law §7.2', false, 'Generated Document 7', 'Generated Document 7', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', '2024-02-05', '2025-02-05', 'ACTIVE'), -- Gen 7 (P2)
       ('df8e4237-4369-45e7-a365-3f46741814d0', 1, '2024-06-02 08:31:00.000', 'a0000004-0000-0000-0000-000000000004', '2024-2281-382', false,
        'Law §7.2', false, 'Generated Document 8', 'Generated Document 8', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2024-06-02', null, 'ACTIVE'), -- Gen 8 (P4)
       ('69d31844-1810-4857-bdb9-f7c533b675b1', 1, '2024-07-02 08:31:00.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-810', false,
        'Law §7.2', true, 'Generated Document 9', 'Generated Document 9', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', '2027-01-01', '2028-12-31', 'ACTIVE'), -- Gen 9 (P3)
       ('488c4523-4ae1-41ea-8a3a-cf991999b12f', 1, '2023-11-17 08:31:00.000', 'a0000001-0000-0000-0000-000000000001', '2024-2281-491', true,
        'Law §7.1', false, 'Generated Document 10', 'Generated Document 10', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', null, null, 'ACTIVE'),  -- Gen 10 (P5)
       ('82a4ecd5-b406-49d3-b7de-0922ff2f3b95', 1, '2024-03-08 08:33:07.000', 'a0000006-0000-0000-0000-000000000006', '2024-2281-797', true,
        'Law §7.1', false, 'Generated Document 11', 'Generated Document 11', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2024-03-08', '2027-03-08', 'ACTIVE'), -- Gen 11 rev 1 (P1)
       ('f182fd95-7a17-4cb7-843b-abf1f8ce8ce7', 1, '2024-01-28 08:33:07.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-632', false,
        'Law §7.2', false, 'Generated Document 12', 'Generated Document 12', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-01-28', '2025-01-28', 'ACTIVE'), -- Gen 12 (P2)
       ('9f2e05c4-a06d-4fed-b487-2003c591bc6a', 1, '2024-08-31 08:33:07.000', 'a0000001-0000-0000-0000-000000000001', '2024-2281-232', false, null,
        false, 'Generated Document 13', 'Generated Document 13', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2024-08-31', null, 'ACTIVE'),      -- Gen 13 rev 1 (P4)
       ('37a7aefa-5f24-460e-817b-f089b7dd84be', 1, '2024-06-28 08:33:07.000', 'a0000001-0000-0000-0000-000000000001', '2024-2281-646', false,
        'Law §7.1', false, 'Generated Document 14', 'Generated Document 14', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2024-06-28', '2027-06-28', 'ACTIVE'), -- Gen 14 (P1)
       ('fe9a0099-e407-4350-aaf2-f47edb20a770', 1, '2024-01-10 08:33:07.000', 'a0000004-0000-0000-0000-000000000004', '2024-2281-755', false,
        'Law §7.2', false, 'Generated Document 15', 'Generated Document 15', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2024-01-10', '2025-01-10', 'ACTIVE'), -- Gen 15 rev 1 (P2)
       ('db983069-1a9f-4e6a-8b21-865ebd2fb902', 1, '2024-08-27 08:33:07.000', 'a0000006-0000-0000-0000-000000000006', '2024-2281-252', false,
        'Law §7.1', true, 'Generated Document 16', 'Generated Document 16', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', '2024-08-27', null, 'ACTIVE'), -- Gen 16 rev 1 (P4)
       ('1a3e2060-c190-4c91-b221-b380493e5f4b', 1, '2023-12-07 08:33:07.000', 'a0000003-0000-0000-0000-000000000003', '2024-2281-369', false,
        'Law §7.2', false, 'Generated Document 17', 'Generated Document 17', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', '2023-12-07', '2026-12-07', 'ACTIVE'), -- Gen 17 (P1)
       ('ef87f332-50b1-4943-bf0d-c1fa798734a9', 1, '2023-12-29 08:33:07.000', 'a0000001-0000-0000-0000-000000000001', '2024-2281-266', true, null,
        false, 'Generated Document 18', 'Generated Document 18', '1e5447b7-8941-43a4-afb7-cab09375efad', '2281', null, null, 'ACTIVE'),              -- Gen 18 (P5)
       ('5a728f56-2fb1-460e-9158-7a4d5775e80f', 1, '2024-08-03 08:33:07.000', 'a0000004-0000-0000-0000-000000000004', '2024-2281-465', true, null,
        false, 'Generated Document 19', 'Generated Document 19', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', '2027-01-01', '2028-12-31', 'ACTIVE'), -- Gen 19 (P3)
       ('4ad5ecbf-15e5-4572-9b80-96bbfc2145b6', 1, '2024-04-06 08:33:07.000', 'a0000003-0000-0000-0000-000000000003', '2024-2281-139', false,
        'Law §7.2', false, 'Generated Document 20', 'Generated Document 20', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-04-06', null, 'ACTIVE'), -- Gen 20 rev 1 (P4)
       ('7f2e05c4-a06d-4fed-b487-2003c591bc6a', 2, '2024-04-06 08:33:07.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-232', false,
        'Law §7.2', false, 'Generated Document 20', 'Generated Document 20', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-08-31', null, 'ACTIVE'), -- Gen 13 rev 2 (P4)
       ('fe6a0099-e407-4350-aaf2-f47edb20a770', 2, '2024-04-06 08:33:07.000', 'a0000005-0000-0000-0000-000000000005', '2024-2281-755', false,
        'Law §7.2', false, 'Generated Document 20', 'Generated Document 20', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-01-10', '2025-01-10', 'ACTIVE'), -- Gen 15 rev 2 (P2)
       ('22a4ecd5-b406-49d3-b7de-0922ff2f3b95', 2, '2024-04-06 08:33:07.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-797', false,
        'Law §7.2', false, 'Generated Document 20', 'Generated Document 20', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-03-08', '2027-03-08', 'ACTIVE'), -- Gen 11 rev 2 (P1)
       ('db933069-1a9f-4e6a-8b21-865ebd2fb902', 2, '2024-04-06 08:33:07.000', 'a0000002-0000-0000-0000-000000000002', '2024-2281-252', false,
        'Law §7.2', false, 'Generated Document 20', 'Generated Document 20', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-08-27', null, 'ACTIVE'), -- Gen 16 rev 2 (P4)
       ('8ad5ecbf-15e5-4572-9b80-96bbfc2145b6', 2, '2024-04-06 08:33:07.000', 'a0000006-0000-0000-0000-000000000006', '2024-2281-139', false,
        'Law §7.2', false, 'Generated Document 20', 'Generated Document 20', '933622d0-4b69-4d96-a204-507f31e20e61', '2281', '2024-04-06', null, 'ACTIVE'); -- Gen 20 rev 2 (P4)

-- Document type + documents used exclusively by the validOn search IT (municipality 2999).
-- These rows are isolated from all other tests which target municipalities 2281 / 2282.
INSERT INTO document_type (id, created, last_updated, created_by, display_name, last_updated_by, municipality_id,
                           `type`)
VALUES ('a9f1d2b0-0000-4000-8000-000000000001', '2024-10-25 14:00:00.000', null, 'a0000001-0000-0000-0000-000000000001', 'Validity window', null,
        '2999', 'VALIDITY_TEST');

INSERT INTO document (id, revision, created, created_by, registration_number, confidential, legal_citation, archive,
                      title, description, document_type_id, municipality_id, valid_from, valid_to, status)
VALUES ('b0000001-0000-0000-0000-000000000001', 1, '2025-01-01 00:00:00.000', 'a000000a-0000-0000-0000-0000000000aa', '2025-2999-0001', false,
        null, false, 'Valid only in 2025', 'Valid only in 2025', 'a9f1d2b0-0000-4000-8000-000000000001', '2999', '2025-01-01', '2025-12-31', 'ACTIVE'),
       ('b0000001-0000-0000-0000-000000000002', 1, '2025-01-01 00:00:00.000', 'a000000a-0000-0000-0000-0000000000aa', '2025-2999-0002', false,
        null, false, 'Valid from 2026 onwards', 'Valid from 2026 onwards', 'a9f1d2b0-0000-4000-8000-000000000001', '2999', '2026-01-01', null, 'ACTIVE'),
       ('b0000001-0000-0000-0000-000000000003', 1, '2025-01-01 00:00:00.000', 'a000000a-0000-0000-0000-0000000000aa', '2025-2999-0003', false,
        null, false, 'Valid until 2025', 'Valid until 2025', 'a9f1d2b0-0000-4000-8000-000000000001', '2999', null, '2025-12-31', 'ACTIVE'),
       ('b0000001-0000-0000-0000-000000000004', 1, '2025-01-01 00:00:00.000', 'a000000a-0000-0000-0000-0000000000aa', '2025-2999-0004', false,
        null, false, 'Always valid', 'Always valid', 'a9f1d2b0-0000-4000-8000-000000000001', '2999', null, null, 'ACTIVE'),
       ('b0000001-0000-0000-0000-000000000005', 1, '2025-01-01 00:00:00.000', 'a000000a-0000-0000-0000-0000000000aa', '2025-2999-0005', false,
        null, false, 'Valid only in 2027', 'Valid only in 2027', 'a9f1d2b0-0000-4000-8000-000000000001', '2999', '2027-01-01', '2027-12-31', 'ACTIVE');


INSERT INTO document_data (id, document_id, storage_locator, file_name, file_size_in_bytes, mime_type)
VALUES ('faa3547e-f775-4799-9ac0-e07fab1df362', '159c10bf-1b32-471b-b2d3-c4b4b13ea152',
        'd35254ce-d26c-47e3-806f-4cf68cf2fa56', 'file1.jpg', 5068, 'image/jpeg'), -- Document-1, revision 1
       ('50167fa9-6b50-428e-9383-b6cbfaffc63b', '8efd63a3-b525-4581-8b0b-9759f381a5a5',
        '3b570ff2-b631-4584-a9fb-77dce2f6d85b', 'file2.jpg', 5068, 'image/jpeg'), -- Document-1, revision 2
       ('4f0a04af-942d-4ad2-b2d9-151887fc995c', '612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2',
        '53978846-e715-455b-a4e7-440084f0b49b', 'file3.jpg', 5068, 'image/jpeg'), -- Document-1, revision 3
       ('8f7be4bb-ef79-4a16-95bf-a0619e320745', '03d33a6a-bc8c-410c-95f6-2c890822967d',
        '297282c6-d06e-4c33-8bc8-0828866ff7e5', 'file4.jpg', 5068, 'image/jpeg'), -- Document-2, revision 1
       ('bd239ee1-27b8-43e7-bb0d-e4ba09b7220e', '435bb041-2b02-4bb3-b3e7-3782a13f47d5',
        'bfb3ad87-cb18-4b70-9594-128d284a7e6e', 'file5.jpg', 5068, 'image/jpeg'), -- Document-2, revision 2
       ('cba078aa-9335-4b21-b04c-630e27ade51e', '8901694b-8e3a-46b7-83ea-cd351ccc0f52',
        '0ac27b16-88c3-4180-9617-d8502e24932b', 'file6.jpg', 5068, 'image/jpeg'), -- Document-3, revision 1
       ('abc078aa-9335-4b21-b04c-630e27ade51e', '8ad5ecbf-15e5-4572-9b80-96bbfc2145b6',
        '93227b16-88c3-4180-9617-d8502e24932b', 'to-be-removed.jpg', 5068, 'image/jpeg');

INSERT INTO document_metadata (document_id, `key`, value)
VALUES ('159c10bf-1b32-471b-b2d3-c4b4b13ea152', 'document1-key1', 'value-1'), -- Document 1, revision 1

       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', 'document1-key1', 'value-1'), -- Document-1, revision 2
       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', 'document1-key2', 'value-2'), -- Document-1, revision 2

       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', 'document1-key1', 'value-1'), -- Document-1, revision 3
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', 'document1-key2', 'value-2'), -- Document-1, revision 3
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', 'document1-key3', 'value-3'), -- Document-1, revision 3
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', 'document1-key4', 'value-4'), -- Document-1, revision 3

       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 'document2-key1', 'value-1'), -- Document-2, revision 1
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 'document2-key2', 'value-2'), -- Document-2, revision 1
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 'document2-key3', 'value-3'), -- Document-2, revision 1

       ('435bb041-2b02-4bb3-b3e7-3782a13f47d5', 'document2-key1', 'value-1'), -- Document-2, revision 2
       ('435bb041-2b02-4bb3-b3e7-3782a13f47d5', 'document2-key2', 'value-2'), -- Document-2, revision 2

       ('8901694b-8e3a-46b7-83ea-cd351ccc0f52', 'document3-key1', 'value-1'), -- Document-3, revision 1
       ('159c10bf-1b32-471b-b2d3-c4b4b13ea152', 'EMPLOYEE_UNIT', 'Company B'),
       ('159c10bf-1b32-471b-b2d3-c4b4b13ea152', 'EMPLOYEE_TYPE', 'Manager'),
       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', 'EMPLOYEE_UNIT', 'Company C'),
       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', 'EMPLOYEE_TYPE', 'Developer'),
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 'EMPLOYEE_UNIT', 'Organization X'),
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 'EMPLOYEE_TYPE', 'Analyst'),
       ('435bb041-2b02-4bb3-b3e7-3782a13f47d5', 'EMPLOYEE_UNIT', 'Startup Y'),
       ('435bb041-2b02-4bb3-b3e7-3782a13f47d5', 'EMPLOYEE_TYPE', 'Developer'),
       ('676eaf7a-d609-4885-9743-2dbcdffe6628', 'EMPLOYEE_UNIT', 'Company C'),
       ('676eaf7a-d609-4885-9743-2dbcdffe6628', 'EMPLOYEE_TYPE', 'Manager'),
       ('34095a16-68c5-48f8-ac1e-9d6b7dd08562', 'EMPLOYEE_UNIT', 'Company B'),
       ('34095a16-68c5-48f8-ac1e-9d6b7dd08562', 'EMPLOYEE_TYPE', 'Developer'),
       ('c3ee6cd8-d9e1-499c-b483-a9956b43ab7d', 'EMPLOYEE_UNIT', 'Startup Y'),
       ('c3ee6cd8-d9e1-499c-b483-a9956b43ab7d', 'EMPLOYEE_TYPE', 'Manager'),
       ('1cc8599b-8726-4cf7-869d-36737044400c', 'EMPLOYEE_UNIT', 'Organization X'),
       ('1cc8599b-8726-4cf7-869d-36737044400c', 'EMPLOYEE_TYPE', 'Manager'),
       ('e4c860ad-fce3-431e-965f-0229e5610fb7', 'EMPLOYEE_UNIT', 'Startup Y'),
       ('e4c860ad-fce3-431e-965f-0229e5610fb7', 'EMPLOYEE_TYPE', 'Analyst'),
       ('84ae9dbe-159a-472f-9dc7-8ecb03c2c3c1', 'EMPLOYEE_UNIT', 'Startup Y'),
       ('84ae9dbe-159a-472f-9dc7-8ecb03c2c3c1', 'EMPLOYEE_TYPE', 'Analyst'),
       ('019d0963-b6c6-49fb-9f88-31ef5e525a1c', 'EMPLOYEE_UNIT', 'Company A'),
       ('019d0963-b6c6-49fb-9f88-31ef5e525a1c', 'EMPLOYEE_TYPE', 'Consultant'),
       ('df8e4237-4369-45e7-a365-3f46741814d0', 'EMPLOYEE_UNIT', 'Organization X'),
       ('df8e4237-4369-45e7-a365-3f46741814d0', 'EMPLOYEE_TYPE', 'Consultant'),
       ('69d31844-1810-4857-bdb9-f7c533b675b1', 'EMPLOYEE_UNIT', 'Company A'),
       ('69d31844-1810-4857-bdb9-f7c533b675b1', 'EMPLOYEE_TYPE', 'Developer'),
       ('488c4523-4ae1-41ea-8a3a-cf991999b12f', 'EMPLOYEE_UNIT', 'Startup Y'),
       ('488c4523-4ae1-41ea-8a3a-cf991999b12f', 'EMPLOYEE_TYPE', 'Developer'),
       ('82a4ecd5-b406-49d3-b7de-0922ff2f3b95', 'EMPLOYEE_UNIT', 'Organization X'),
       ('82a4ecd5-b406-49d3-b7de-0922ff2f3b95', 'EMPLOYEE_TYPE', 'Analyst'),
       ('f182fd95-7a17-4cb7-843b-abf1f8ce8ce7', 'EMPLOYEE_UNIT', 'Company B'),
       ('f182fd95-7a17-4cb7-843b-abf1f8ce8ce7', 'EMPLOYEE_TYPE', 'Developer'),
       ('9f2e05c4-a06d-4fed-b487-2003c591bc6a', 'EMPLOYEE_UNIT', 'Organization X'),
       ('9f2e05c4-a06d-4fed-b487-2003c591bc6a', 'EMPLOYEE_TYPE', 'Engineer'),
       ('37a7aefa-5f24-460e-817b-f089b7dd84be', 'EMPLOYEE_UNIT', 'Organization X'),
       ('37a7aefa-5f24-460e-817b-f089b7dd84be', 'EMPLOYEE_TYPE', 'Consultant'),
       ('fe9a0099-e407-4350-aaf2-f47edb20a770', 'EMPLOYEE_UNIT', 'Company B'),
       ('fe9a0099-e407-4350-aaf2-f47edb20a770', 'EMPLOYEE_TYPE', 'Manager'),
       ('db983069-1a9f-4e6a-8b21-865ebd2fb902', 'EMPLOYEE_UNIT', 'Company B'),
       ('db983069-1a9f-4e6a-8b21-865ebd2fb902', 'EMPLOYEE_TYPE', 'Consultant'),
       ('1a3e2060-c190-4c91-b221-b380493e5f4b', 'EMPLOYEE_UNIT', 'Company B'),
       ('1a3e2060-c190-4c91-b221-b380493e5f4b', 'EMPLOYEE_TYPE', 'Developer'),
       ('ef87f332-50b1-4943-bf0d-c1fa798734a9', 'EMPLOYEE_UNIT', 'Organization X'),
       ('ef87f332-50b1-4943-bf0d-c1fa798734a9', 'EMPLOYEE_TYPE', 'Manager'),
       ('5a728f56-2fb1-460e-9158-7a4d5775e80f', 'EMPLOYEE_UNIT', 'Company B'),
       ('5a728f56-2fb1-460e-9158-7a4d5775e80f', 'EMPLOYEE_TYPE', 'Analyst'),
       ('4ad5ecbf-15e5-4572-9b80-96bbfc2145b6', 'EMPLOYEE_UNIT', 'Startup Y'),
       ('4ad5ecbf-15e5-4572-9b80-96bbfc2145b6', 'EMPLOYEE_TYPE', 'Developer');


INSERT INTO registration_number_sequence (sequence_number, created, modified, id, municipality_id)
VALUES (665, '2023-06-28 12:01:00.000', '2023-06-28 12:01:00.000', 'b734c963-b8d1-4ca0-b392-067f6f217794', '2321');


-- Document type + documents used exclusively by status-lifecycle ITs (municipality 2998).
-- Isolated from other tests; covers DRAFT/ACTIVE/SCHEDULED/EXPIRED/REVOKED + reconcile + multi-rev cases.
INSERT INTO document_type (id, created, last_updated, created_by, display_name, last_updated_by, municipality_id, `type`)
VALUES ('c0000000-0000-4000-8000-000000000001', '2024-10-25 14:00:00.000', null, 'a0000001-0000-0000-0000-000000000001', 'Status test', null, '2998', 'STATUS_TEST');

INSERT INTO document (id, revision, created, created_by, registration_number, confidential, legal_citation, archive,
                      title, description, document_type_id, municipality_id, valid_from, valid_to, status)
VALUES
    -- Pure DRAFT (just created, never published)
    ('d0000001-0000-0000-0000-000000000001', 1, '2025-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0001', false, null, false, 'Pure draft document', 'Pure draft document', 'c0000000-0000-4000-8000-000000000001', '2998', null, null, 'DRAFT'),
    -- ACTIVE doc (published, currently valid)
    ('d0000001-0000-0000-0000-000000000002', 1, '2025-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0002', false, null, false, 'Active published doc', 'Active published doc', 'c0000000-0000-4000-8000-000000000001', '2998', '2025-01-01', '2027-12-31', 'ACTIVE'),
    -- SCHEDULED doc (published with future validFrom)
    ('d0000001-0000-0000-0000-000000000003', 1, '2025-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0003', false, null, false, 'Scheduled doc', 'Scheduled doc', 'c0000000-0000-4000-8000-000000000001', '2998', '2027-01-01', '2028-12-31', 'SCHEDULED'),
    -- EXPIRED doc (validTo passed)
    ('d0000001-0000-0000-0000-000000000004', 1, '2024-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0004', false, null, false, 'Expired doc', 'Expired doc', 'c0000000-0000-4000-8000-000000000001', '2998', '2024-01-01', '2024-12-31', 'EXPIRED'),
    -- REVOKED doc
    ('d0000001-0000-0000-0000-000000000005', 1, '2025-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0005', false, null, false, 'Revoked doc', 'Revoked doc', 'c0000000-0000-4000-8000-000000000001', '2998', '2025-01-01', '2027-12-31', 'REVOKED'),
    -- Pre-read reconcile case: ACTIVE but validTo passed -> reads should flip to EXPIRED
    ('d0000001-0000-0000-0000-000000000006', 1, '2024-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0006', false, null, false, 'Stale active doc that should expire on read', 'Stale active doc that should expire on read', 'c0000000-0000-4000-8000-000000000001', '2998', '2024-01-01', '2024-06-30', 'ACTIVE'),
    -- Pre-read reconcile case: SCHEDULED with validFrom in past -> reads should flip to ACTIVE
    ('d0000001-0000-0000-0000-000000000007', 1, '2024-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0007', false, null, false, 'Stale scheduled doc that should activate on read', 'Stale scheduled doc that should activate on read', 'c0000000-0000-4000-8000-000000000001', '2998', '2024-01-01', '2027-12-31', 'SCHEDULED'),
    -- onlyLatestRevision regression case: rev1=ACTIVE + rev2=DRAFT for same regNum
    ('d0000001-0000-0000-0000-000000000010', 1, '2025-01-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0008', false, null, false, 'Multi-rev: rev 1 active', 'Multi-rev: rev 1 active', 'c0000000-0000-4000-8000-000000000001', '2998', '2025-01-01', '2027-12-31', 'ACTIVE'),
    ('d0000001-0000-0000-0000-000000000011', 2, '2025-02-01 00:00:00.000', 'a0000000-0000-0000-0000-0000000000bb', '2025-2998-0008', false, null, false, 'Multi-rev: rev 2 draft', 'Multi-rev: rev 2 draft', 'c0000000-0000-4000-8000-000000000001', '2998', '2025-01-01', '2027-12-31', 'DRAFT');