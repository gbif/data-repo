package org.gbif.datarepo.snapshots.hive;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import org.apache.hadoop.fs.Path;
import org.gbif.api.vocabulary.License;
import org.gbif.datarepo.api.DataRepository;
import org.gbif.datarepo.api.model.DataPackage;
import org.gbif.datarepo.api.model.FileInputContent;
import org.gbif.datarepo.api.model.Identifier;
import org.gbif.datarepo.api.model.Tag;

import org.gbif.datarepo.impl.util.MimeTypesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;

/**
 * Utility class to persist DataPackages for GBIF Snapshots.
 */
class DataPackageManager {

    private static final String CREATOR = "gbif-snapshot";
    // Note that this prefix has to match with the one used in dataone project
    private static final String FORMAT_ID_PREFIX = "DataOne:formatId:";

    private static final Logger LOG = LoggerFactory.getLogger(DataPackageManager.class);

    private final DataRepository dataRepository;

    /**
     * Builds an instance using the {@link DataRepository}.
     */
    DataPackageManager(DataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }


    /**
     * Builds a simple DataPackage using basic information and the 'relatedDoi' as a related identifier.
     */
    private DataPackage buildDataPackageWithDoi(String title, String description, String format, String relatedDoi) {
        DataPackage dataPackage = buildDataPackage(title, description, format);
        Identifier identifier = new Identifier();
        identifier.setIdentifier(relatedDoi);
        identifier.setType(Identifier.Type.DOI);
        identifier.setRelationType(Identifier.RelationType.References);
        identifier.setCreatedBy(CREATOR);
        dataPackage.addRelatedIdentifier(identifier);
        return dataPackage;
    }

    /**
     * Builds a DataPackage that will be shared in DataOne.
     */
    private DataPackage buildDataPackage(String title, String description, String format) {
        DataPackage dataPackage = new DataPackage();
        dataPackage.setCreatedBy(CREATOR);
        dataPackage.setTitle(title);
        dataPackage.setDescription(description);
        dataPackage.setLicense(License.CC_BY_4_0);
        dataPackage.setShareIn(Sets.newHashSet("DataOne"));
        Tag tag = new Tag();
        tag.setValue(FORMAT_ID_PREFIX + format);
        dataPackage.setTags(Collections.singleton(tag));
        return dataPackage;
    }

    /**
     * Creates and persists a DataPackage from Hadoop HDFS path.
     */
    public DataPackage createSnapshotDataPackage(URI file) {
        Path path = new Path(file);
        LOG.info("Creating DataPackage for file {}", file);
        return dataRepository.create(buildDataPackage(
                "GBIF occurrence data snapshot " + path.getName(),
                "GBIF snapshot data in compress format", MimeTypesUtil.detectMimeType(path.getName())),
                Collections.singletonList(FileInputContent.from(path.getName(), file)), true);
    }

    /**
     * Creates and persists a DataPackage from local file.
     */

    private DataPackage createDataPackage(File file, String title, String description, String doi) {
        LOG.info("Creating DataPackage for file {}", file);
        try (InputStream inputStream = new FileInputStream(file)) {
            return dataRepository.create(
                    buildDataPackageWithDoi(title, description, MimeTypesUtil.detectMimeType(file.getName()), doi),
                    Collections.singletonList(FileInputContent.from(file.getName(), inputStream)), true);
        } catch (IOException ex) {
            LOG.error("Error creating DataPackage", ex);
            throw Throwables.propagate(ex);
        }
    }

    /**
     * Creates RDF DataPackage related to the snapshotDOI param.
     */
    DataPackage createSnapshotRdfDataPackage(File file, String snapshotDOI) {
        return createDataPackage(file,
                "GBIF Snapshot RDF Metadata " + file.getName(),
                "RDF Metadata for GBIF Snapshot " + file.getName(), snapshotDOI);
    }

    /**
     * Creates EML DataPackage related to the snapshotDOI param.
     */

    DataPackage createSnapshotEmlDataPackage(File file, String snapshotDOI) {
        return createDataPackage(file, "GBIF Snapshot Metadata " + file.getName(),
                "EML Metadata for GBIF Snapshot " + file.getName(), snapshotDOI);
    }

}
