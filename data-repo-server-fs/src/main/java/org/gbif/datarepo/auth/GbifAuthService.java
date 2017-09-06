package org.gbif.datarepo.auth;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class
 * The GBIF authentication scheme is modelled after the Amazon scheme on how to sign REST http requests
 * using a private key. It uses the standard Http Authorization Header to transport the following information:
 * Authorization: GBIF applicationKey:Signature
 *
 * <br/>
 * The header starts with the authentication scheme (GBIF), followed by the plain applicationKey (the public key)
 * and a unique signature for the very request which is generated using a fixed set from request attributes
 * which are then encrypted by a standard HMAC-SHA1 algorithm.
 *
 * <br/>
 * A POST request with a GBIF header would look like this:
 *
 * <pre>
 * POST /dataset HTTP/1.1
 * Host: johnsmith.s3.amazonaws.com
 * Date: Mon, 26 Mar 2007 19:37:58 +0000
 * x-gbif-user: trobertson
 * Content-MD5: LiFThEP4Pj2TODQXa/oFPg==
 * Authorization: GBIF gbif.portal:frJIUN8DYpKDtOLCwo//yllqDzg=
 * </pre>
 *
 * When signing an http request in addition to the Authentication header some additional custom headers are added
 * which are used to sign and digest the message.
 * <br/>
 * x-gbif-user is added to transport a proxied user in which the application is acting.
 * <br/>
 * Content-MD5 is added if a body entity exists.
 * See Concent-MD5 header specs: http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
 */
@Singleton
public class GbifAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(GbifAuthService.class);

  private static final String ALGORITHM = "HmacSHA1";
  private static final String CHAR_ENCODING = "UTF8";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_CONTENT_MD5 = "Content-MD5";
  public static final String GBIF_SCHEME = "GBIF";
  public static final String HEADER_GBIF_USER = "x-gbif-user";
  public static final String HEADER_ORIGINAL_REQUEST_URL = "x-url";
  private static final char NEWLINE = '\n';
  private static final Pattern COLON_PATTERN = Pattern.compile(":");

  private final ImmutableMap<String, String> keyStore;
  private final String appKey;
  private final ObjectMapper mapper;

  private final boolean selfAuthenticated;

  private GbifAuthService(Map<String, String> appKeys, String appKey, ObjectMapper mapper,
                          boolean selfAuthenticated) {
    this.mapper = mapper;
    keyStore = ImmutableMap.copyOf(appKeys);
    this.appKey = appKey;
    this.selfAuthenticated = selfAuthenticated;
    LOG.info("Initialised appkey store with {} keys", keyStore.size());
  }

  /**
   * Creates a new GBIF authentication service for clients that want to sign requests always using a single
   * application key. Used by the GBIF portal and other trusted applications that need to proxy a user.
   */
  public static GbifAuthService singleKeyAuthService(String appKey, String secret, ObjectMapper mapper,
                                                     boolean selfAuthenticated) {
    LOG.info("Initialising auth service with key {}", appKey);
    return new GbifAuthService(ImmutableMap.of(appKey, secret), appKey, mapper, selfAuthenticated);
  }

  /**
   * Creates a new GBIF authentication service for clients that want to sign requests always using a single
   * application key. Used by the GBIF portal and other trusted applications that need to proxy a user.
   */
  public static GbifAuthService singleKeySelfAuthService(String appKey, String secret, ObjectMapper mapper) {
    LOG.info("Initialising auth service with key {}", appKey);
    return new GbifAuthService(ImmutableMap.of(appKey, secret), appKey, mapper, true);
  }

  /**
   * Extracts the information to be encrypted from a request and concatenates them into a single String.
   * When the server receives an authenticated request, it compares the computed request signature
   * with the signature provided in the request in StringToSign.
   * For that reason this string may only contain information also available in the exact same form to the server.
   *
   * @return unique string for a request
   *
   * @see <a href="http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html">AWS Docs</a>
   */
  private static String buildStringToSign(ContainerRequestContext request) {
    StringBuilder sb = new StringBuilder();

    sb.append(request.getMethod());
    sb.append(NEWLINE);
    // custom header set by varnish overrides real URI
    // see http://dev.gbif.org/issues/browse/GBIFCOM-137
    if (request.getHeaders().containsKey(HEADER_ORIGINAL_REQUEST_URL)) {
      sb.append(request.getHeaders().getFirst(HEADER_ORIGINAL_REQUEST_URL));
    } else {
      sb.append(getCanonicalizedPath(request.getUriInfo().getRequestUri()));
    }

    appendHeader(sb, request.getHeaders(), HEADER_CONTENT_TYPE, false);
    appendHeader(sb, request.getHeaders(), HEADER_CONTENT_MD5, true);
    appendHeader(sb, request.getHeaders(), HEADER_GBIF_USER, true);

    return sb.toString();
  }

  /**
   * Build the string to be signed for a client request by extracting header information from the request.
   * For PUT/POST requests that contain a body content it is required that the Content-MD5 header
   * is already present on the request instance!
   */
  private static String buildStringToSign(ClientRequestContext request) {
    StringBuilder sb = new StringBuilder();

    sb.append(request.getMethod());
    sb.append(NEWLINE);
    sb.append(getCanonicalizedPath(request.getUri()));

    appendHeader(sb, request.getHeaders(), HEADER_CONTENT_TYPE, false);
    appendHeader(sb, request.getHeaders(), HEADER_CONTENT_MD5, true);
    appendHeader(sb, request.getHeaders(), HEADER_GBIF_USER, true);

    return sb.toString();
  }

  private static void appendHeader(StringBuilder sb, MultivaluedMap<String, ?> request, String header,
                                   boolean caseSensitive) {
    if (request.containsKey(header)) {
      sb.append(NEWLINE);
      if (caseSensitive) {
        sb.append(request.getFirst(header));
      } else {
        sb.append(request.getFirst(header).toString().toLowerCase());
      }
    }
  }

  /**
   * @return an absolute uri from the resource path alone, excluding host, scheme and query parameters
   */
  private static String getCanonicalizedPath(URI uri) {
    return uri.normalize().getPath();
  }

  private static String buildAuthHeader(String applicationKey, String signature) {
    return GBIF_SCHEME + " " + applicationKey + ':' + signature;
  }

  /**
   * Generates a Base64 encoded HMAC-SHA1 signature from the passed in string with the given secret key.
   * See Message Authentication Code specs http://tools.ietf.org/html/rfc2104
   * @param stringToSign the string to be signed
   * @param secretKey the secret key to use in the
   */
  private static String buildSignature(String stringToSign, String secretKey) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      Key secret = new SecretKeySpec(secretKey.getBytes(Charset.forName("UTF8")), ALGORITHM);
      mac.init(secret);
      byte[] digest = mac.doFinal(stringToSign.getBytes(Charset.forName("UTF8")));

      return new String(Base64.getMimeEncoder().encode(digest), "ASCII");

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Cant find " + ALGORITHM + " message digester", e);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported character encoding " + CHAR_ENCODING, e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException("Invalid secret key " + secretKey, e);
    }
  }

  /**
   * Signs a request by adding a Content-MD5 and Authorization header.
   * For PUT/POST requests that contain a body entity the Content-MD5 header is created using the same
   * JSON mapper for serialization as the clients use.
   *
   * Other format than JSON are not supported currently !!!
   */
  public void signRequest(ClientRequestContext request) {
    Preconditions.checkNotNull(appKey, "To sign request a single application key is required");

    if (selfAuthenticated && !request.getHeaders().containsKey(HEADER_GBIF_USER)) {
      request.getHeaders().add(HEADER_GBIF_USER, appKey);
    }
    // first add custom GBIF headers so we can use them to build the string to sign

    // the canonical path header
    request.getHeaders().putSingle(HEADER_ORIGINAL_REQUEST_URL, getCanonicalizedPath(request.getUri()));
    // adds content md5
    if (request.getEntity() != null) {
      request.getHeaders().putSingle(HEADER_CONTENT_MD5, buildContentMD5(request.getEntity()));
    }

    // build the unique string to sign
    String stringToSign = buildStringToSign(request);
    // find private key for this app
    String secretKey = getPrivateKey(appKey);
    if (secretKey == null) {
      LOG.warn("Skip signing request with unknown application key: {}", appKey);
      return;
    }
    // sign
    String signature = buildSignature(stringToSign, secretKey);
    // build authorization header string
    String header = buildAuthHeader(appKey, signature);
    // add authorization header
    LOG.debug("Adding authentication header to request {} : {}", request.getUri(), header);
    request.getHeaders().putSingle(HEADER_AUTHORIZATION, header);
  }

  /**
   * Generates the Base64 encoded 128 bit MD5 digest from the entire content string suitable for the
   * Content-MD5 header value.
   * See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.15
   */
  private String buildContentMD5(Object entity) {
    try {
      byte[] content = mapper.writeValueAsBytes(entity);
      return new String(Base64.getMimeEncoder().encode(DigestUtils.md5(content)), "ASCII");

    } catch (IOException e) {
      LOG.error("Failed to serialize http entity [{}]", entity);
      throw Throwables.propagate(e);
    }
  }

  public boolean isValidRequest(ContainerRequestContext request) {
    // parse auth header
    String authHeader = request.getHeaderString(HEADER_AUTHORIZATION);
    if (Strings.isNullOrEmpty(authHeader) || !authHeader.startsWith(GBIF_SCHEME + " ")) {
      LOG.info(HEADER_AUTHORIZATION + " header is no GBIF scheme");
      return false;
    }

    String[] values = COLON_PATTERN.split(authHeader.substring(5), 2);
    if (values.length < 2) {
      LOG.warn("Invalid syntax for application key and signature: {}", authHeader);
      return false;
    }

    String requestAppKey = values[0];
    String signatureFound = values[1];
    if (requestAppKey == null || signatureFound == null) {
      LOG.warn("Authentication header missing applicationKey or signature: {}", authHeader);
      return false;
    }

    String secretKey = getPrivateKey(requestAppKey);
    if (secretKey == null) {
      LOG.warn("Unknown application key: {}", requestAppKey);
      return false;
    }
    //
    String stringToSign = buildStringToSign(request);
    // sign
    String signature = buildSignature(stringToSign, secretKey);
    // compare signatures
    if (signatureFound.equals(signature)) {
      LOG.debug("Trusted application with matching signatures");
      return true;

    }
    LOG.info("Invalid signature: {}", authHeader);
    LOG.debug("StringToSign: {}", stringToSign);
    return false;
  }

  private String getPrivateKey(String applicationKey) {
    return keyStore.get(applicationKey);
  }
}

