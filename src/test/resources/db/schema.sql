
    create table document (
        archive bit not null,
        confidential bit not null,
        revision integer not null,
        valid_from date,
        valid_to date,
        created datetime(6),
        created_by varchar(255),
        description varchar(8192) not null,
        document_type_id varchar(255) not null,
        id varchar(255) not null,
        legal_citation varchar(255),
        municipality_id varchar(255),
        registration_number varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table document_data (
        file_size_in_bytes bigint default 0,
        storage_backend varchar(16) default 'jdbc' not null,
        document_id varchar(255) not null,
        file_name varchar(255),
        id varchar(255) not null,
        mime_type varchar(255),
        storage_locator varchar(255),
        primary key (id)
    ) engine=InnoDB;

    create table document_data_binary (
        id varchar(255) not null,
        binary_file longblob,
        primary key (id)
    ) engine=InnoDB;

    create table document_metadata (
        document_id varchar(255) not null,
        `key` varchar(255),
        `value` varchar(255)
    ) engine=InnoDB;

    create table document_type (
        created datetime(6),
        last_updated datetime(6),
        created_by varchar(255),
        display_name varchar(255) not null,
        id varchar(255) not null,
        last_updated_by varchar(255),
        municipality_id varchar(255),
        `type` varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table registration_number_sequence (
        sequence_number integer,
        created datetime(6),
        modified datetime(6),
        id varchar(255) not null,
        municipality_id varchar(255),
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

    alter table if exists document 
       add constraint uq_revision_and_registration_number unique (revision, registration_number);

    create index ix_key 
       on document_metadata (`key`);

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
