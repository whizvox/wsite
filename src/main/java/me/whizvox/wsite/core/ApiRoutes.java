package me.whizvox.wsite.core;

import me.whizvox.wsite.database.Page;
import me.whizvox.wsite.database.User;
import me.whizvox.wsite.util.HttpUtils;
import me.whizvox.wsite.util.JsonUtils;
import me.whizvox.wsite.util.Pair;
import me.whizvox.wsite.util.Utils;
import spark.*;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static me.whizvox.wsite.core.WsiteConfiguration.*;

public class ApiRoutes {
  private ApiRoutes() {}

  private static void haltInvalidApiRequest(int status, WsiteResult result) {
    Spark.halt(status, JsonUtils.toJson(new ApiGenericResult(false, result.toString())));
  }

  private static void haltInvalidApiRequest(WsiteResult result) {
    haltInvalidApiRequest(400, result);
  }

  private static Object getApiResult(WsiteResult result) {
    if (result == WsiteResult.SUCCESS) {
      return JsonUtils.toJson(new ApiGenericResult(true, result.toString()));
    }
    haltInvalidApiRequest(result);
    return null;
  }

  private static void setupMultipartEncoding(WsiteService wsite, Request request) {
    request.attribute("org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(wsite.resolvePath(Reference.TEMP_DIR).toString())
    );
  }

  private static Page getPage(QueryParamsMap params) {
    Page page = new Page();
    page.path = HttpUtils.getString(params, "path");
    page.title = HttpUtils.getString(params, "title");
    page.syntax = Page.Syntax.fromString(HttpUtils.getString(params, "syntax"));
    page.contents = HttpUtils.getString(params, "contents");
    boolean base64Encoded = HttpUtils.getBool(params, "encoded");
    if (base64Encoded) {
      page.contents = new String(Base64.getDecoder().decode(page.contents), StandardCharsets.UTF_8);
    }
    return page;
  }

  public static User getUser(QueryParamsMap params, String idField, String usernameField, String emailField, String passwordField, String operatorField) {
    User user = new User();
    if (idField != null) {
      user.id = HttpUtils.getUuid(params, idField);
    }
    user.username = HttpUtils.getString(params, usernameField);
    user.emailAddress = HttpUtils.getString(params, emailField);
    user.password = HttpUtils.getString(params, passwordField);
    user.operator = HttpUtils.getBool(params, operatorField);
    return user;
  }

  public static Map<String, Object> getConfig(WsiteService wsite, Request request) {
    QueryParamsMap params = request.queryMap();
    Map<String, Object> cfg = new HashMap<>();
    if (params.hasKey(KEY_SITE_NAME)) {
      cfg.put(KEY_SITE_NAME, HttpUtils.getString(params, KEY_SITE_NAME));
    }
    if (params.hasKey(KEY_PORT)) {
      int port = HttpUtils.getInt(params, KEY_PORT, -1);
      if (port == -1) {
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_PORT);
      }
      cfg.put(KEY_PORT, port);
    }
    if (params.hasKey(KEY_CONTINUOUSLY_RESTART)) {
      cfg.put(KEY_CONTINUOUSLY_RESTART, HttpUtils.getBool(params, KEY_CONTINUOUSLY_RESTART));
    }
    if (params.hasKey(KEY_USERNAME_PATTERN)) {
      String usernamePattern = HttpUtils.getString(params, KEY_USERNAME_PATTERN);
      try {
        Pattern.compile(usernamePattern);
      } catch (PatternSyntaxException e) {
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_USERNAME_PATTERN);
      }
      cfg.put(KEY_USERNAME_PATTERN, usernamePattern);
    }
    if (params.hasKey(KEY_PASSWORD_PATTERN)) {
      String passwordPattern = HttpUtils.getString(params, KEY_PASSWORD_PATTERN);
      try {
        Pattern.compile(passwordPattern);
      } catch (PatternSyntaxException e) {
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_PASSWORD_PATTERN);
      }
      cfg.put(KEY_PASSWORD_PATTERN, passwordPattern);
    }
    if (params.hasKey(KEY_INDEX_PAGE)) {
      cfg.put(KEY_INDEX_PAGE, HttpUtils.getString(params, KEY_INDEX_PAGE));
    }
    if (params.hasKey(KEY_DATABASE_URL)) {
      cfg.put(KEY_DATABASE_URL, HttpUtils.getString(params, KEY_DATABASE_URL));
    }
    if (params.hasKey(KEY_DATABASE_USERNAME)) {
      cfg.put(KEY_DATABASE_USERNAME, HttpUtils.getString(params, KEY_DATABASE_USERNAME));
    }
    if (params.hasKey(KEY_DATABASE_PASSWORD)) {
      cfg.put(KEY_DATABASE_PASSWORD, HttpUtils.getString(params, KEY_DATABASE_PASSWORD));
    }
    if (params.hasKey(KEY_DATABASE_PROPERTIES)) {
      cfg.put(KEY_DATABASE_PROPERTIES,
          Utils.parseProperties(HttpUtils.getString(params, KEY_DATABASE_PROPERTIES))
      );
    }
    if (params.hasKey(KEY_ENABLE_SSL)) {
      cfg.put(KEY_ENABLE_SSL, HttpUtils.getBool(params, KEY_ENABLE_SSL));
    }
    Path secureDir = wsite.resolvePath(Reference.SECURE_DIR);
    if (params.hasKey(KEY_KEYSTORE_FILE)) {
      try {
        Path output = HttpUtils.copyFile(request, KEY_KEYSTORE_FILE, secureDir, true);
        if (output == null) {
          wsite.getLogger().error("Received keystore file not encoded in multipart format");
          haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_KEYSTORE);
        }
        cfg.put(KEY_KEYSTORE_FILE, output.toString());
      } catch (IOException | ServletException e) {
        wsite.getLogger().error("Could not copy keystore file", e);
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_KEYSTORE);
      }
    } else if (params.hasKey("keystoreString")) {
      String keystoreString = HttpUtils.getString(params, "keystoreString");
      Path output = secureDir.resolve("keystore");
      try {
        Files.write(output, Base64.getDecoder().decode(keystoreString));
        cfg.put(KEY_KEYSTORE_FILE, output.toString());
      } catch (IOException e) {
        wsite.getLogger().error("Could not decode base64-encoded keystore string", e);
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_KEYSTORE);
      }
    }
    if (params.hasKey(KEY_KEYSTORE_PASSWORD)) {
      cfg.put(KEY_KEYSTORE_PASSWORD, HttpUtils.getString(params, KEY_KEYSTORE_PASSWORD));
    }
    if (params.hasKey(KEY_TRUSTSTORE_FILE)) {
      try {
        Path output = HttpUtils.copyFile(request, KEY_TRUSTSTORE_FILE, secureDir, true);
        if (output == null) {
          wsite.getLogger().error("Received truststore file not encoded in multipart form");
          haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_TRUSTSTORE);
        }
        cfg.put(KEY_TRUSTSTORE_FILE, output.toString());
      } catch (ServletException | IOException e) {
        wsite.getLogger().error("Could not copy truststore file", e);
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_TRUSTSTORE);
      }
    } else if (params.hasKey("truststoreString")) {
      String str = HttpUtils.getString(params, "truststoreString");
      Path output = secureDir.resolve("truststore");
      try {
        Files.write(output, Base64.getDecoder().decode(str));
        cfg.put(KEY_TRUSTSTORE_FILE, output.toString());
      } catch (IOException | IllegalArgumentException e) {
        wsite.getLogger().error("Could not write truststore string", e);
        haltInvalidApiRequest(WsiteResult.CONFIG_INVALID_TRUSTSTORE);
      }
    }
    if (params.hasKey(KEY_TRUSTSTORE_PASSWORD)) {
      cfg.put(KEY_TRUSTSTORE_PASSWORD, HttpUtils.getString(params, KEY_TRUSTSTORE_PASSWORD));
    }
    if (params.hasKey(KEY_ENABLE_SMTP)) {
      cfg.put(KEY_ENABLE_SMTP, HttpUtils.getString(params, KEY_ENABLE_SMTP));
    }
    if (params.hasKey(KEY_SMTP_HOST)) {
      cfg.put(KEY_SMTP_HOST, HttpUtils.getString(params, KEY_SMTP_HOST));
    }
    if (params.hasKey(KEY_SMTP_FROM)) {
      cfg.put(KEY_SMTP_FROM, HttpUtils.getString(params, KEY_SMTP_FROM));
    }
    if (params.hasKey(KEY_SMTP_USER)) {
      cfg.put(KEY_SMTP_USER, HttpUtils.getString(params, KEY_SMTP_USER));
    }
    if (params.hasKey(KEY_SMTP_PASSWORD)) {
      cfg.put(KEY_SMTP_PASSWORD, HttpUtils.getString(params, KEY_SMTP_PASSWORD));
    }
    return cfg;
  }

  private static void checkPermission(WsiteService wsite, Request request) {
    String token = HttpUtils.getString(request.queryMap(), "token");
    if (token == null) {
      haltInvalidApiRequest(WsiteResult.NO_TOKEN);
    }
    User user = wsite.getUserFromLoginToken(token);
    if (user == null) {
      haltInvalidApiRequest(WsiteResult.LOGIN_TOKEN_NOT_FOUND);
    }
    if (!user.operator) {
      haltInvalidApiRequest(WsiteResult.UNAUTHORIZED);
    }
  }

  public static abstract class WsiteApiRoute implements Route {
    private WsiteService wsite;
    public WsiteApiRoute(WsiteService wsite) {
      this.wsite = wsite;
    }
    @Override
    public Object handle(Request request, Response response) throws Exception {
      try {
        response.header("Content-Type", "application/json");
        return JsonUtils.toJson(handle_do(request, response, wsite));
      } catch (HaltException e) {
        throw e;
      } catch (Exception e) {
        wsite.getLogger().error("An exception occurred trying to handle an API request", e);
        return JsonUtils.toJson(new ApiGenericResult(false, Utils.stacktraceToString(e)));
      }
    }
    protected abstract Object handle_do(Request request, Response response, WsiteService wsite) throws Exception;
  }

  public static class PageCreateRoute extends WsiteApiRoute {
    public static WsiteResult createPage(Request request, WsiteService wsite) {
      Page page = getPage(request.queryMap());
      return wsite.createNewPage(page.path, page.title, page.syntax.toString(), page.contents);
    }
    public PageCreateRoute(WsiteService wsite) { super(wsite); }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      return getApiResult(createPage(request, wsite));
    }
  }

  public static class PageUpdateRoute extends WsiteApiRoute {
    public static WsiteResult updatePage(WsiteService wsite, Request request) {
      Page page = getPage(request.queryMap());
      String origPath = HttpUtils.getString(request.queryMap(), "origPath");
      return wsite.updatePage(origPath, page.path, page.title, page.syntax.toString(), page.contents);
    }
    public PageUpdateRoute(WsiteService wsite) { super(wsite); }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      return getApiResult(updatePage(wsite, request));
    }
  }

  public static class PageGetRoute extends WsiteApiRoute {
    public static Page getPage(WsiteService wsite, Request request, String pathField, String noContentsField) {
      // TODO: maybe check for permission?
      String path = HttpUtils.getString(request.queryMap(), pathField);
      // TODO: Implement a way to get a page without its contents or syntax
      boolean noContents = HttpUtils.getBool(request.queryMap(), noContentsField);
      Page page = wsite.getPage(path);
      if (page == null) {
        haltInvalidApiRequest(WsiteResult.PAGE_PATH_NOT_FOUND);
      }
      return page;
    }
    public static Page getPage(WsiteService wsite, Request request) {
      return getPage(wsite, request, "path", "noContents");
    }
    public PageGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      return getPage(wsite, request);
    }
  }

  public static class PageDeleteRoute extends WsiteApiRoute {
    public static WsiteResult deletePage(WsiteService wsite, Request request, String pathField) {
      String path = HttpUtils.getString(request.queryMap(), pathField);
      return wsite.deletePage(path);
    }
    public static WsiteResult deletePage(WsiteService wsite, Request request) {
      return deletePage(wsite, request, "path");
    }
    public PageDeleteRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      return getApiResult(deletePage(wsite, request));
    }
  }

  public static class UserCreateRoute extends WsiteApiRoute {
    public static WsiteResult createUser(WsiteService wsite, Request request, String idField, String usernameField, String emailField, String passwordField, String operatorField) {
      User user = getUser(request.queryMap(), idField, usernameField, emailField, passwordField, operatorField);
      return wsite.createNewUser(user.username, user.emailAddress, user.password.toCharArray(), user.operator);
    }
    public static WsiteResult createUser(WsiteService wsite, Request request) {
      return createUser(wsite, request, "id", "username", "email", "password", "operator");
    }
    public UserCreateRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      return getApiResult(createUser(wsite, request));
    }
  }

  public static class UserUpdateUsernameRoute extends WsiteApiRoute {
    public UserUpdateUsernameRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      UUID id = HttpUtils.getUuid(params, "id");
      String username = HttpUtils.getString(params, "username");
      return getApiResult(wsite.updateUserUsername(id, username));
    }
  }

  public static class UserUpdateEmailAddressRoute extends WsiteApiRoute {
    public UserUpdateEmailAddressRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      UUID id = HttpUtils.getUuid(params, "id");
      String emailAddress = HttpUtils.getString(params, "email");
      return getApiResult(wsite.updateUserEmailAddress(id, emailAddress));
    }
  }

  public static class UserUpdatePasswordRoute extends WsiteApiRoute {
    public UserUpdatePasswordRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      UUID id = HttpUtils.getUuid(params, "id");
      char[] password = HttpUtils.getString(params, "password").toCharArray();
      return getApiResult(wsite.updateUserPassword(id, password));
    }
  }

  public static class UserUpdateOperatorRoute extends WsiteApiRoute {
    public UserUpdateOperatorRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      UUID id = HttpUtils.getUuid(params, "id");
      boolean operator = HttpUtils.getBool(params, "operator");
      return getApiResult(wsite.updateUserOperator(id, operator));
    }
  }

  public static class UserDeleteRoute extends WsiteApiRoute {
    public static WsiteResult deleteUser(WsiteService wsite, Request request, String idField) {
      UUID id = HttpUtils.getUuid(request.queryMap(), idField);
      return wsite.deleteUser(id);
    }
    public static WsiteResult deleteUser(WsiteService wsite, Request request) {
      return deleteUser(wsite, request, "id");
    }
    public UserDeleteRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      return getApiResult(deleteUser(wsite, request));
    }
  }

  public static class UserGetRoute extends WsiteApiRoute {
    public UserGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      User user = null;
      UUID id = HttpUtils.getUuid(params, "id");
      String username = HttpUtils.getString(params, "username");
      String emailAddress = HttpUtils.getString(params, "email");
      if (id != null) {
        user = wsite.getUserFromId(id);
      } else if (username != null) {
        user = wsite.getUserFromUsername(username);
      } else if (emailAddress != null) {
        user = wsite.getUserFromEmailAddress(emailAddress);
      }
      if (user == null) {
        haltInvalidApiRequest(WsiteResult.USER_INVALID_QUERY);
      }
      return user;
    }
  }

  public static class LoginCreateRoute extends WsiteApiRoute {
    public static Pair<WsiteResult, String> createLogin(WsiteService wsite, Request request, String queryField, String passwordField, String rememberField) {
      QueryParamsMap params = request.queryMap();
      String query = HttpUtils.getString(params, queryField);
      char[] password = HttpUtils.getCharArray(params, passwordField);
      boolean rememberMe = HttpUtils.getBool(params, rememberField);
      int minutesUntilExpire = rememberMe ? 10008 : 60; // 1 week vs 1 hour
      CharBuffer tokenBuffer = CharBuffer.allocate(Reference.LOGIN_TOKEN_LENGTH);
      WsiteResult result = wsite.createLogin(
          query, password, minutesUntilExpire, request.userAgent(), request.ip(), tokenBuffer
      );
      tokenBuffer.rewind();
      return new Pair<>(result, result != WsiteResult.SUCCESS ? null : tokenBuffer.toString());
    }
    public static Pair<WsiteResult, String> createLogin(WsiteService wsite, Request request) {
      return createLogin(wsite, request, "query", "password", "remember");
    }
    public LoginCreateRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      Pair<WsiteResult, String> res = createLogin(wsite, request);
      if (res.left == WsiteResult.SUCCESS) {
        return JsonUtils.toJson(new ApiGenericResult(true, res.right));
      }
      return JsonUtils.toJson(new ApiGenericResult(false, res.left.toString()));
    }
  }

  public static class LoginDeleteRoute extends WsiteApiRoute {
    public LoginDeleteRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      // TODO: Is this secure? Maybe rely on an API key instead?
      String token = HttpUtils.getString(request.queryMap(), "token");
      return getApiResult(wsite.deleteLogin(token));
    }
  }

  public static class UploadAssetRoute extends WsiteApiRoute {
    public UploadAssetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws IOException {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      String contentsStr = HttpUtils.getString(params, "contents");
      boolean encoded = HttpUtils.getBool(params, "encoded");
      String encoding = HttpUtils.getString(params, "encoding");
      boolean replace = HttpUtils.getBool(params, "replace");
      ByteArrayInputStream in;
      if (contentsStr != null) {
        if (encoded) {
          in = new ByteArrayInputStream(Base64.getDecoder().decode(contentsStr));
        } else {
          Charset charset;
          if (encoding == null) {
            charset = StandardCharsets.UTF_8;
          } else {
            charset = Charset.forName(encoding);
          }
          in = new ByteArrayInputStream(contentsStr.getBytes(charset));
        }
      } else {
        in = null;
      }
      return wsite.uploadAsset(path, in, replace);
    }
  }

  public static class DeleteAssetRoute extends WsiteApiRoute {
    public DeleteAssetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws IOException {
      checkPermission(wsite, request);
      String path = HttpUtils.getString(request.queryMap(), "path");
      return wsite.deleteAsset(path);
    }
  }

  public static class ConfigGetRoute extends WsiteApiRoute {
    public ConfigGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      String filter = HttpUtils.getString(request.queryMap(), "filter", "all").toLowerCase();
      return wsite.getConfigValues(filter);
    }
  }

  public static class ConfigUpdateRoute extends WsiteApiRoute {
    public ConfigUpdateRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      setupMultipartEncoding(wsite, request);
      Map<String, Object> cfg = getConfig(wsite, request);
      wsite.restartWithNewConfiguration(cfg);
      return getApiResult(WsiteResult.SUCCESS);
    }
  }

}
