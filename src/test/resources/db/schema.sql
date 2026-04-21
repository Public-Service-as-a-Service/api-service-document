
    create table document (
        archive bit not null,
        confidential bit not null,
        municipality_id varchar(4),
        revision integer not null,
        valid_from date,
        valid_to date,
        created datetime(6),
        created_by varchar(255),
        description varchar(8192) not null,
        document_type_id varchar(255) not null,
        id varchar(255) not null,
        legal_citation varchar(255),
        registration_number varchar(255) not null,
        title varchar(255) not null,
        updated_by varchar(255),
        status enum ('ACTIVE','DRAFT','EXPIRED','REVOKED','SCHEDULED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table document_data (
        file_size_in_bytes bigint default 0,
        content_hash varchar(64),
        document_id varchar(255) not null,
        extracted_text LONGTEXT,
        file_name varchar(255),
        id varchar(255) not null,
        mime_type varchar(255),
        storage_locator varchar(255) not null,
        extraction_status enum ('FAILED','PENDING_REINDEX','SUCCESS','UNSUPPORTED') not null,
        primary key (id)
    ) engine=InnoDB;

    create table document_metadata (
        document_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table document_responsibility (
        municipality_id varchar(4) not null,
        created datetime(6),
        updated datetime(6),
        person_id varchar(36) not null,
        created_by varchar(255),
        id varchar(255) not null,
        registration_number varchar(255) not null,
        updated_by varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table document_type (
        municipality_id varchar(4),
        created datetime(6),
        last_updated datetime(6),
        created_by varchar(255),
        display_name varchar(255) not null,
        id varchar(255) not null,
        last_updated_by varchar(255),
        `type` varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table registration_number_sequence (
        municipality_id varchar(4),
        sequence_number integer,
        created datetime(6),
        modified datetime(6),
        id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create index ix_registration_number 
       on document (registration_number);

    create index ix_created_by 
       on document (created_by);

    create index ix_municipality_id 
       on document (municipality_id);

    create index ix_confidential 
       on document (confidential);

    create index ix_status 
       on document (status);

    alter table if exists document 
       add constraint uq_revision_and_registration_number unique (revision, registration_number);

    create index ix_key 
       on document_metadata (`key`);

    create index ix_document_responsibility_lookup 
       on document_responsibility (municipality_id, person_id);

    create index ix_document_responsibility_document 
       on document_responsibility (municipality_id, registration_number);

    alter table if exists document_responsibility 
       add constraint uq_document_responsibility unique (municipality_id, registration_number, person_id);

    create index ix_municipality_id_type 
       on document_type (municipality_id, `type`);

    create index ix_municipality_id 
       on document_type (municipality_id);

    alter table if exists document_type 
       add constraint uq_municipality_id_and_type unique (municipality_id, `type`);

    create index ix_municipality_id 
       on registration_number_sequence (municipality_id);

    alter table if exists registration_number_sequence 
       add constraint uq_municipality_id unique (municipality_id);

    alter table if exists document 
       add constraint fk_document_document_type 
       foreign key (document_type_id) 
       references document_type (id);

    alter table if exists document_data 
       add constraint fk_document_data_document 
       foreign key (document_id) 
       references document (id);

    alter table if exists document_metadata 
       add constraint fk_document_metadata_document 
       foreign key (document_id) 
       references document (id);
