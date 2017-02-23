CREATE TABLE data_package (
    doi text NOT NULL PRIMARY KEY,
    metadata text,
    files text[],
    created timestamp with time zone NOT NULL DEFAULT now(),
    modified timestamp with time zone NOT NULL DEFAULT now(),
    deleted timestamp with time zone,
    created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3),
    modified_by varchar(255) NOT NULL CHECK (length(modified_by) >= 3)
);
CREATE INDEX data_package_idx ON data_package (doi, created, created_by);
