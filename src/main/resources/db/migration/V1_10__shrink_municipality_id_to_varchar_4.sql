alter table document
    modify column municipality_id varchar(4);

alter table document_type
    modify column municipality_id varchar(4);

alter table document_responsibility
    modify column municipality_id varchar(4) not null;

alter table registration_number_sequence
    modify column municipality_id varchar(4);
