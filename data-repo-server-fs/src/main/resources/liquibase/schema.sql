CREATE TABLE data_package (
    doi text NOT NULL PRIMARY KEY,
    title text NOT NULL CHECK (length(created_by) >= 3),
    description text,
    metadata text,
    created timestamp with time zone NOT NULL DEFAULT now(),
    modified timestamp with time zone NOT NULL DEFAULT now(),
    deleted timestamp with time zone,
    created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3),
    modified_by varchar(255) NOT NULL CHECK (length(modified_by) >= 3),
    checksum varchar(32) NOT NULL CHECK (length(checksum) = 32),
    size bigint
);
CREATE INDEX data_package_idx ON data_package (doi, created, created_by);

CREATE TABLE data_package_file (
    data_package_doi text NOT NULL REFERENCES data_package(doi) ON DELETE CASCADE,
    file_name text NOT NULL,
    checksum varchar(32) NOT NULL CHECK (length(checksum) = 32),
    size bigint,
    PRIMARY KEY (data_package_doi, file_name)
);
CREATE INDEX data_package_file_idx ON data_package_file (data_package_doi, file_name, checksum);


CREATE TYPE alternative_identifier_type AS ENUM ('URL', 'LSID', 'HANDLER', 'DOI', 'UUID', 'FTP', 'URI', 'UNKNOWN');
CREATE TABLE alternative_identifier (
    identifier varchar(800) UNIQUE NOT NULL PRIMARY KEY,
    data_package_doi text NOT NULL REFERENCES data_package(doi) ON DELETE CASCADE,
    type alternative_identifier_type NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3)
);
CREATE INDEX alternative_identifier_idx ON alternative_identifier(data_package_doi, created, created_by);