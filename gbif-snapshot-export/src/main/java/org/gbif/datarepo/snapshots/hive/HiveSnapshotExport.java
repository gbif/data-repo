package org.gbif.datarepo.snapshots.hive;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.gbif.dwc.terms.TermFactory;

import java.util.List;

public class HiveSnapshotExport {

    //private HiveMetaStoreClient hiveMetaStoreClient;

    public static void main(String[] arg) throws  Exception {
        HiveConf hiveConf = new HiveConf();
        hiveConf.setVar(HiveConf.ConfVars.METASTOREURIS, "thrift://c5master1-vh.gbif.org:9083");
        HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
        SessionState hiveSession = new SessionState(hiveConf);
        SessionState.start(hiveSession);
        Driver driver = new Driver(hiveConf);
        driver.init();
        driver.compile("select count(*) from snapshot.raw_20180409");
        driver.close();
        hiveSession.close();
        List<FieldSchema> fieldSchemas = hiveMetaStoreClient.getFields("snapshot","raw_20180409");
        fieldSchemas.forEach(fieldSchema -> System.out.println(getTerm(fieldSchema.getName().replaceFirst("v_", "")) + ":" + fieldSchema.getType()));
    }

    private static String getTerm(String fieldName) {
        try {
            return TermFactory.instance().findTerm(fieldName).simpleName();
        } catch (Exception ex) {
            return fieldName;
        }
    }
}
