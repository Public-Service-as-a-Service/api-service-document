create table document_responsibility (
    created datetime(6),
    updated datetime(6),
    created_by varchar(255),
    id varchar(255) not null,
    municipality_id varchar(255) not null,
    principal_id varchar(255) not null,
    principal_type varchar(32) not null,
    registration_number varchar(255) not null,
    updated_by varchar(255),
    primary key (id)
) engine=InnoDB;

alter table if exists document_responsibility
    add constraint uq_document_responsibility unique (municipality_id, registration_number, principal_type, principal_id);

create index ix_document_responsibility_lookup
    on document_responsibility (municipality_id, principal_type, principal_id);

create index ix_document_responsibility_document
    on document_responsibility (municipality_id, registration_number);
