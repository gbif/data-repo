package org.gbif.datarepo.store.fs.download;

import org.gbif.datarepo.api.model.FileInputContent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletResponse;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.ftp.FtpURLConnection;

/**
 * Utility class to read files form external sources: http, https, ftp and hdfs.
 */
public class FileDownload {

  private static final Logger LOG = LoggerFactory.getLogger(FileDownload.class);

  //Hadoop HDFS scheme
  private static final String HDFS_SCHEME = "hdfs";

  //FTP scheme
  private static final Set<String> FTP_SCHEMES = Sets.newHashSet("ftp","ftps");

  //HTTP schemes
  private static final Set<String> HTTP_SCHEMES = Sets.newHashSet("http", "https");

  //HTTP HEAD request
  private static final String HTTP_HEAD_METHOD = "HEAD";

  //Cached hadoop configuration
  private final FileSystem hdfs;

  /**
   * Uri to the HDFS name node or name service.
   */
  public FileDownload(FileSystem hdfs) {
    this.hdfs = hdfs;
  }

  /**
   * Does a file exists in the FTP server?.
   */
  private static boolean ftpFileExists(URI uri) throws IOException {
    FTPClient ftpClient = new FTPClient();
    try {
      //Connect to the FTP server using a Port if it was specified
      if (uri.getPort() > 0) {
        ftpClient.connect(InetAddress.getByName(uri.getHost()), uri.getPort());
      } else {
        ftpClient.connect(InetAddress.getByName(uri.getHost()));
      }
      //Use the user connections settings if present
      if (uri.getUserInfo() != null){
        String[] userData = uri.getUserInfo().split(":");
        ftpClient.login(userData[0], userData.length > 1? userData[1]: "");
      }
      //Can connect?
      if(!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
        return false;
      }
      ftpClient.enterLocalPassiveMode();
      return ftpClient.listFiles(uri.getPath()).length > 0;
    } catch (Exception ex) {
      LOG.error("Error connecting to {}", uri);
      return false;
    } finally {
      ftpClient.disconnect();
    }

  }

  /**
   * Does the file exist in HDFS?.
   */
  private boolean hdfsFileExists(URI uri) throws IOException {
      return hdfs.exists(new Path(uri));
  }

  /**
   * Does the file exists in the HTTP server?.
   */
  private static boolean httpFileExist(URI uri) {
    try {
      HttpURLConnection httpConnection = (HttpURLConnection)uri.toURL().openConnection();
      httpConnection.setRequestMethod(HTTP_HEAD_METHOD);
      httpConnection.connect();
      httpConnection.disconnect();
      return HttpServletResponse.SC_OK == httpConnection.getResponseCode();
    } catch (Exception ex){
      LOG.error("Error connecting to {}", uri);
      return false;
    }
  }

  /**
   * OPen a input stream to an external file, the supported protocols are: http, https, ftp and hdfs.
   * All the streams returned by this method should be closed by consumers of it.
   */
  public InputStream openStream(URI fileLocation) throws IOException {
    //Opens a stream to a reachable HDFS or external URL
    return fileLocation.getScheme().equalsIgnoreCase(HDFS_SCHEME) ?
      hdfs.open(new Path(fileLocation)) : fileLocation.toURL().openStream();
  }

  /**
   * Opens a connection/stream to the input file.
   * If the fileInputContent.inputStream is null its content is read from the fileInputContent.fileLocation.
   */
  private InputStream open(FileInputContent fileInputContent) {
    return Optional.ofNullable(fileInputContent.getInputStream())
      .orElseGet(() -> { //This has to be done as supplier to simulate lazy evaluation
        try {
         return openStream(fileInputContent.getFileLocation());
        } catch(IOException ex){
          throw new RuntimeException(ex);
        }
      });
  }

  /**
   * Check if the file exists in a remote HDFS, FTP or HTTP server.
   */
  public boolean exists(String fileLocation) throws IOException {
    //Parse URI
    URI uri = URI.create(fileLocation);
    String scheme = uri.getScheme();

    //it's an external URL
    if (HTTP_SCHEMES.contains(scheme)) {
      return httpFileExist(uri);
    }
    //HDFS
    if (HDFS_SCHEME .equalsIgnoreCase(scheme)) {
      return hdfsFileExists(uri);
    }

    //FTP
    if (FTP_SCHEMES.contains(scheme)) {
      return ftpFileExists(uri);
    }
    throw new IllegalArgumentException("Scheme not supported");
  }

  /**
   *  Copies the content of the fileInputContent into the destination path in the target file system.
   *  @return the number of bytes copied
   */
  public int copy(FileInputContent fileInputContent, Path destination, FileSystem fs) {
    Retryer<Integer> retryer = RetryerBuilder.<Integer>newBuilder()
      .retryIfExceptionOfType(IOException.class)
      .withStopStrategy(StopStrategies.stopAfterAttempt(3))
      .withWaitStrategy(WaitStrategies.fibonacciWait(10, 10, TimeUnit.SECONDS))
      .build();
    try {
      return retryer.call(() -> {
        try (FSDataOutputStream fos = fs.create(destination, true);
             InputStream inputStream = open(fileInputContent)) {
          return IOUtils.copy(inputStream, fos);
        }
      });
    } catch (ExecutionException | RetryException ex) {
      LOG.error("Error fetching fileInput {} ", fileInputContent, ex);
      throw new RuntimeException(ex);
    }
  }
}
