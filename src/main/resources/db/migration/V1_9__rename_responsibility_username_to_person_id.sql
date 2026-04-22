alter table document_responsibility
    drop index uq_document_responsibility;

alter table document_responsibility
    drop index ix_document_responsibility_lookup;

alter table document_responsibility
    change username person_id varchar(36) not null;

alter table document_responsibility
    add constraint uq_document_responsibility unique (municipality_id, registration_number, person_id);

create index ix_document_responsibility_lookup
    on document_responsibility (municipality_id, person_id);
