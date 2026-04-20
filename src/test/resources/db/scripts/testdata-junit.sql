INSERT INTO document_type (id, created, last_updated, created_by, display_name, last_updated_by, municipality_id,
                           `type`)
VALUES ('86b9efc9-c649-40d5-ade0-ac415ea146f1', '2024-10-25 14:00:00.000', null, 'User1', 'Anställningsbevis', null,
        '2281', 'EMPLOYEE_CERTIFICATE'),
       ('3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2024-10-25 14:00:00.000', null, 'User2', 'Semesterväxlingsdokument',
        null, '2281', 'HOLIDAY_EXCHANGE'),
       ('257506b2-f870-470d-9a1b-d095acb212a7', '2024-10-25 14:00:00.000', null, 'User1', 'Anställningsbevis', null,
        '2282', 'EMPLOYEE_CERTIFICATE'),
       ('1e5447b7-8941-43a4-afb7-cab09375efad', '2024-10-25 14:00:00.000', null, 'User2', 'Sekretessavtal', null,
        '2262', 'CONFIDENTIALITY_AGREEMENT');

INSERT INTO document (id, revision, created, created_by, registration_number, confidential, archive, description,
                      document_type_id, municipality_id, status)
VALUES ('159c10bf-1b32-471b-b2d3-c4b4b13ea152', 1, '2023-06-28 12:01:00.000', "User1", '2023-2281-123', false, false,
        'Document 1', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', 'ACTIVE'), -- Document-1, revision 1
       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', 2, '2023-06-28 12:02:00.000', "User2", '2023-2281-123', false, false,
        'Document 1', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', 'ACTIVE'), -- Document-1, revision 2
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', 3, '2023-06-28 12:03:00.000', "User3", '2023-2281-123', false, false,
        'Document 1', '3fdecd8b-d295-4222-b60c-e95ba5f5075a', '2281', 'ACTIVE'), -- Document-1, revision 3
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', 1, '2023-06-28 12:01:00.000', "User4", '2024-2281-999', true, true,
        'Document 2', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', 'ACTIVE'), -- Document-2, revision 1
       ('8901694b-8e3a-46b7-83ea-cd351ccc0f52', 1, '2023-06-28 12:04:00.000', "User5", '2024-2281-666', true, true,
        'Document 3', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', 'ACTIVE'), -- Document-3, revision 1

       ('1901694b-8e3a-46b7-83ea-cd351ccc0f52', 1, '2023-06-28 12:04:00.000', "User5", '2024-2281-601', true, true,
        'Document 4', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', 'ACTIVE'), -- Document-4, revision 1
       ('2901694b-8e3a-46b7-83ea-cd351ccc0f52', 1, '2023-06-28 12:04:00.000', "User5", '2024-2281-602', true, true,
        'Document 5', '257506b2-f870-470d-9a1b-d095acb212a7', '2281', 'ACTIVE'), -- Document-5, revision 1
       ('3901694b-8e3a-46b7-83ea-cd351ccc0f52', 1, '2023-06-28 12:04:00.000', "User5", '2024-2281-603', true, true,
        'Document 6', '86b9efc9-c649-40d5-ade0-ac415ea146f1', '2281', 'ACTIVE'); -- Document-6, revision 1

INSERT INTO document_data (id, document_id, storage_locator, file_name, file_size_in_bytes, mime_type)
VALUES ('faa3547e-f775-4799-9ac0-e07fab1df362', '159c10bf-1b32-471b-b2d3-c4b4b13ea152',
        'd35254ce-d26c-47e3-806f-4cf68cf2fa56', 'file1.jpg', 5068, 'image/jpeg'), -- Document-1, revision 1
       ('50167fa9-6b50-428e-9383-b6cbfaffc63b', '8efd63a3-b525-4581-8b0b-9759f381a5a5',
        '3b570ff2-b631-4584-a9fb-77dce2f6d85b', 'file2.jpg', 5068, 'image/jpeg'), -- Document-1, revision 2
       ('4f0a04af-942d-4ad2-b2d9-151887fc995c', '612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2',
        '53978846-e715-455b-a4e7-440084f0b49b', 'file3.jpg', 5068, 'image/jpeg'), -- Document-1, revision 3
       ('8f7be4bb-ef79-4a16-95bf-a0619e320745', '03d33a6a-bc8c-410c-95f6-2c890822967d',
        '297282c6-d06e-4c33-8bc8-0828866ff7e5', 'file4.jpg', 5068, 'image/jpeg'), -- Document-2, revision 1
       ('cba078aa-9335-4b21-b04c-630e27ade51e', '8901694b-8e3a-46b7-83ea-cd351ccc0f52',
        '0ac27b16-88c3-4180-9617-d8502e24932b', 'file5.jpg', 5068, 'image/jpeg'), -- Document-3, revision 1
       ('aba078aa-9335-4b21-b04c-630e27ade51e', '1901694b-8e3a-46b7-83ea-cd351ccc0f52',
        '397282c6-d06e-4c33-8bc8-0828866ff7e5', 'file6.jpg', 5068, 'image/jpeg'), -- Document-4, revision 1
       ('bba078aa-9335-4b21-b04c-630e27ade51e', '2901694b-8e3a-46b7-83ea-cd351ccc0f52',
        '497282c6-d06e-4c33-8bc8-0828866ff7e5', 'file7.jpg', 5068, 'image/jpeg'), -- Document-5, revision 1
       ('dba078aa-9335-4b21-b04c-630e27ade51e', '3901694b-8e3a-46b7-83ea-cd351ccc0f52',
        '597282c6-d06e-4c33-8bc8-0828866ff7e5', 'file8.jpg', 5068, 'image/jpeg'); -- Document-6, revision 1

INSERT INTO document_metadata (document_id, `key`, value)
VALUES ('159c10bf-1b32-471b-b2d3-c4b4b13ea152', "document1-key1", "value-1"),     -- Document 1, revision 1

       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', "document1-key1", "value-1"),     -- Document-1, revision 2
       ('8efd63a3-b525-4581-8b0b-9759f381a5a5', "document1-key2", "value-2"),     -- Document-1, revision 2

       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', "document1-key1", "value-1"),     -- Document-1, revision 3
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', "document1-key2", "value-2"),     -- Document-1, revision 3
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', "document1-key3", "value-3"),     -- Document-1, revision 3
       ('612dc8d0-e6b7-426c-abcc-c9b49ae1e7e2', "document1-key4", "value-4"),     -- Document-1, revision 3

       ('03d33a6a-bc8c-410c-95f6-2c890822967d', "document2-key1", "value-1"),     -- Document-2, revision 1
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', "document2-key2", "value-2"),     -- Document-2, revision 1
       ('03d33a6a-bc8c-410c-95f6-2c890822967d', "document2-key3", "value-3"),     -- Document-2, revision 1

       ('1901694b-8e3a-46b7-83ea-cd351ccc0f52', "EMPLOYEE_TYPE", "Vaktmästare"),  -- Document-4, revision 1
       ('1901694b-8e3a-46b7-83ea-cd351ccc0f52', "EMPLOYEE_UNIT", "Sidsjö skola"), -- Document-4, revision 1
       ('1901694b-8e3a-46b7-83ea-cd351ccc0f52', "personid", "19000101-1234"),     -- Document-4, revision 1
       ('2901694b-8e3a-46b7-83ea-cd351ccc0f52', "EMPLOYEE_TYPE", "Vikarie"),      -- Document-5, revision 1
       ('2901694b-8e3a-46b7-83ea-cd351ccc0f52', "EMPLOYEE_UNIT", "Sidsjö skola"), -- Document-5, revision 1
       ('3901694b-8e3a-46b7-83ea-cd351ccc0f52', "EMPLOYEE_TYPE", "Vikarie"),      -- Document-6, revision 1
       ('3901694b-8e3a-46b7-83ea-cd351ccc0f52', "EMPLOYEE_UNIT", "Livets tuffa skola"); -- Document-6, revision 1

INSERT INTO document_responsibility (id, municipality_id, registration_number, person_id, created_by,
                                     created)
VALUES ('5ac3f0bd-0229-451a-9b66-736c510a0cf1', '2281', '2023-2281-123', '11111111-1111-1111-1111-111111111111', 'User3',
        '2023-06-28 12:03:00.000'),
       ('7c542b7c-9432-403a-af77-78eda79b4a9e', '2281', '2024-2281-601', '55555555-5555-5555-5555-555555555555', 'User5',
        '2023-06-28 12:04:00.000'),
       ('3e8eb73b-e6be-4909-a7dc-0ad101bdc510', '2281', '2024-2281-602', '55555555-5555-5555-5555-555555555555', 'User5',
        '2023-06-28 12:04:00.000'),
       ('79223a90-c2a6-40fc-a52b-7fb2920d0373', '2281', '2024-2281-603', '55555555-5555-5555-5555-555555555555', 'User5',
        '2023-06-28 12:04:00.000');

INSERT INTO registration_number_sequence (sequence_number, created, modified, id, municipality_id)
VALUES (665, '2023-06-28 12:01:00.000', '2023-06-28 12:01:00.000', 'b734c963-b8d1-4ca0-b392-067f6f217794', '2321');