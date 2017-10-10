package org.gbif.datarepo.resource.download;

import org.gbif.datarepo.api.model.FileInputContent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import sun.net.www.protocol.ftp.FtpURLConnection;

/**
 * Utility class to read files form external sources: http, https, ftp and hdfs.
 */
public class FileDownload {

  //Hadoop HDFS scheme
  private static final String HDFS_SCHEME = "hdfs";

  //HTTP HEAD request
  private static final String HTTP_HEAD_METHOD = "HEAD";

  //Cached hadoop configuration
  private final Configuration hadoopConf;

  /**
   * Uri to the HDFS name node or name service.
   */
  public FileDownload(String hadoopDfsUri) {
    hadoopConf = new Configuration();
    hadoopConf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, hadoopDfsUri);
  }

  /**
   * Does a file exists in the FTP server?.
   */
  private static boolean ftpFileExists(URI uri) throws IOException {
    FTPClient ftpClient = new FTPClient();
    try {
      ftpClient.connect(InetAddress.getByName(uri.getHost()), uri.getPort());
      return ftpClient.listNames(uri.getPath()).length > 0;
    } finally {
      ftpClient.disconnect();
    }
  }

  /**
   * Does the file exist in HDFS?.
   */
  private boolean hdfsFileExists(URI uri) throws IOException {
    try (FileSystem fs = FileSystem.get(hadoopConf)) {
      return fs.exists(new Path(uri));
    }
  }

  /**
   * Does the file exists in the HTTP server?.
   */
  private static boolean httpFileExist(HttpURLConnection httpConnection) throws IOException {
    try {
      httpConnection.setRequestMethod(HTTP_HEAD_METHOD);
      httpConnection.connect();
      return HttpServletResponse.SC_OK == httpConnection.getResponseCode();
    } finally {
      httpConnection.disconnect();
    }
  }

  /**
   * Opens a input stream to an external file, the supported protocols are: http, https, ftp and hdfs.
   * All the streams returned by this method should be closed by consumers of it.
   */
  public FileInputContent open(String fileLocation) throws IOException {
    //Parse URI
    URI uri = URI.create(fileLocation);
    //Opens a stream to a reachable HDFS or external URL
    return FileInputContent.from(Paths.get(uri.getPath()).getFileName().toString(),
                                 uri.getScheme().equalsIgnoreCase(HDFS_SCHEME) ?
                                   FileSystem.get(hadoopConf).open(new Path(uri)) : uri.toURL().openStream());
  }

  /**
   * Check if the file exists in a remote HDFS, FTP or HTTP server.
   */
  public boolean exists(String fileLocation) throws IOException {
    //Parse URI
    URI uri = URI.create(fileLocation);
    if (uri.getScheme().equalsIgnoreCase(HDFS_SCHEME)) {
      return hdfsFileExists(uri);
    } else { //it's an external URL
      URLConnection connection =  uri.toURL().openConnection();
      if (HttpURLConnection.class.isInstance(connection)) {
        return httpFileExist((HttpURLConnection) connection);
      } else if (FtpURLConnection.class.isInstance(connection)) {
        return ftpFileExists(uri);
      }
    }
    throw new IllegalArgumentException("Scheme not supported");
  }

}
