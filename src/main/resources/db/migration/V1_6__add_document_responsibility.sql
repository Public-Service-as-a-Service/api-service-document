create table document_responsibility (
    created datetime(6),
    updated datetime(6),
    created_by varchar(255),
    id varchar(255) not null,
    municipality_id varchar(255) not null,
    registration_number varchar(255) not null,
    updated_by varchar(255),
    username varchar(255) not null,
    primary key (id)
) engine=InnoDB;

alter table if exists document_responsibility
    add constraint uq_document_responsibility unique (municipality_id, registration_number, username);

create index ix_document_responsibility_lookup
    on document_responsibility (municipality_id, username);

create index ix_document_responsibility_document
    on document_responsibility (municipality_id, registration_number);
