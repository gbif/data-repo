CREATE EXTENSION unaccent;

CREATE TYPE datapackage_license AS ENUM ('CC0_1_0', 'CC_BY_4_0', 'CC_BY_NC_4_0', 'UNSPECIFIED', 'UNSUPPORTED');
CREATE TYPE identifier_type AS ENUM ('URL', 'LSID', 'HANDLER', 'DOI', 'UUID', 'FTP', 'URI', 'UNKNOWN');
CREATE TYPE identifier_relation_type AS ENUM('IsAlternativeOf' ,'IsCitedBy', 'Cites', 'IsSupplementTo', 'IsSupplementedBy',
                                              'IsContinuedBy', 'Continues', 'HasMetadata', 'IsMetadataFor',
                                              'IsNewVersionOf', 'IsPreviousVersionOf', 'IsPartOf', 'HasPart',
                                              'IsReferencedBy', 'References', 'IsDocumentedBy', 'Documents',
                                              'IsCompiledBy', 'Compiles', 'IsVariantFormOf', 'IsOriginalFormOf',
                                              'IsIdenticalTo', 'IsReviewedBy', 'Reviews', 'IsDerivedFrom', 'IsSourceOf');
CREATE TYPE identifier_scheme AS ENUM ('ORCID', 'ISNI', 'FUND_REF', 'OTHER');

CREATE TABLE data_package (
    key uuid NOT NULL PRIMARY KEY,
    doi text UNIQUE,
    title text NOT NULL CHECK (length(title) >= 3),
    description text,
    citation text,
    license datapackage_license,
    created timestamp with time zone NOT NULL DEFAULT now(),
    modified timestamp with time zone NOT NULL DEFAULT now(),
    deleted timestamp with time zone,
    created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3),
    modified_by varchar(255) NOT NULL CHECK (length(modified_by) >= 3),
    checksum varchar(32) NOT NULL CHECK (length(checksum) = 32),
    size bigint,
    fulltext_search tsvector
);

CREATE INDEX data_package_idx ON data_package (doi, created, created_by);

CREATE INDEX dp_fulltext_search_idx ON data_package USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION dp_change_trigger()
RETURNS TRIGGER AS
$dpchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.doi,''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.title,''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.description,'')));
      RETURN NEW;
    END;
$dpchange$
LANGUAGE plpgsql;

CREATE TRIGGER dp_fulltext_update
  BEFORE INSERT OR UPDATE ON data_package
  FOR EACH ROW EXECUTE PROCEDURE dp_change_trigger();

CREATE TABLE creator (
  key serial NOT NULL PRIMARY KEY,
  data_package_key uuid NOT NULL REFERENCES data_package(key) ON DELETE CASCADE,
  name text NOT NULL  CHECK (length(name) >= 2),
  affiliation text[],
  identifier varchar(254),
  identifier_scheme identifier_scheme,
  scheme_uri varchar(2048),
  created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3),
  created timestamp with time zone NOT NULL DEFAULT now(),
  fulltext_search tsvector
);

CREATE INDEX creator_fulltext_search_idx ON creator USING gin(fulltext_search);

CREATE OR REPLACE FUNCTION creator_change_trigger()
RETURNS TRIGGER AS
$dpchange$
    BEGIN
      NEW.fulltext_search :=
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.name,''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.identifier,''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(array_to_string(NEW.affiliation,' '),''))) ||
        TO_TSVECTOR('pg_catalog.english', unaccent(COALESCE(NEW.scheme_uri,'')));
      RETURN NEW;
    END;
$dpchange$
LANGUAGE plpgsql;

CREATE TRIGGER creator_fulltext_update
  BEFORE INSERT OR UPDATE ON creator
  FOR EACH ROW EXECUTE PROCEDURE creator_change_trigger();


CREATE TABLE data_package_file (
    data_package_key uuid NOT NULL REFERENCES data_package(key) ON DELETE CASCADE,
    file_name text NOT NULL,
    checksum varchar(32) NOT NULL CHECK (length(checksum) = 32),
    size bigint,
    PRIMARY KEY (data_package_key, file_name)
);
CREATE INDEX data_package_file_idx ON data_package_file (data_package_key, file_name, checksum);

CREATE TABLE identifier (
    key serial UNIQUE NOT NULL PRIMARY KEY,
    identifier varchar(800) NOT NULL,
    data_package_key uuid NOT NULL REFERENCES data_package(key) ON DELETE CASCADE,
    type identifier_type NOT NULL,
    relation_type identifier_relation_type NOT NULL,
    created timestamp with time zone NOT NULL DEFAULT now(),
    created_by varchar(255) NOT NULL CHECK (length(created_by) >= 3),
    UNIQUE (identifier, data_package_key, relation_type)
);
CREATE INDEX identifier_idx ON identifier(data_package_key, created, created_by, relation_type);


CREATE TABLE tag (
  key serial NOT NULL,
  data_package_key uuid NOT NULL REFERENCES data_package(key) ON DELETE CASCADE,
  value text NOT NULL,
  created_by character varying(255) NOT NULL,
  created timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tag_pkey PRIMARY KEY (key),
  CONSTRAINT tag_created_by_check CHECK (length(created_by) >= 3),
  CONSTRAINT tag_value_check CHECK (length(value) >= 1)
);
CREATE INDEX tag_idx ON tag(data_package_key, value, created_by);
