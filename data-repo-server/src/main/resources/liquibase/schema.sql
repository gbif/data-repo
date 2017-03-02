CREATE TABLE data_package (
    doi text NOT NULL PRIMARY KEY,
    title text NOT NULL CHECK (length(created_by) >= 3),
    description text,
    metadata text,
    files text[] NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    modified timestamp with time zone NOT NULL DEFAULT now(),
    deleted timestamp with time zone,
    created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3),
    modified_by varchar(255) NOT NULL CHECK (length(modified_by) >= 3)
);
CREATE INDEX data_package_idx ON data_package (doi, created, created_by);
