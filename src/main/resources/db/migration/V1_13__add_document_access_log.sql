create table document_access_log (
    id varchar(36) not null,
    municipality_id varchar(4) not null,
    document_id varchar(36) not null,
    registration_number varchar(255) not null,
    revision integer not null,
    document_data_id varchar(36) not null,
    access_type varchar(20) not null,
    accessed_by varchar(255),
    accessed_at datetime(6) not null,
    primary key (id)
) engine=InnoDB;

create index ix_document_access_log_doc
    on document_access_log (municipality_id, registration_number);

create index ix_document_access_log_revision
    on document_access_log (municipality_id, registration_number, revision);

create index ix_document_access_log_accessed_at
    on document_access_log (accessed_at);
