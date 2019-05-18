package me.whizvox.wsite.core;

import me.whizvox.wsite.database.Page;
import me.whizvox.wsite.database.PageRepository;
import me.whizvox.wsite.database.User;
import me.whizvox.wsite.database.UserRepository;
import me.whizvox.wsite.util.*;
import spark.*;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static me.whizvox.wsite.core.WsiteConfiguration.*;

public class ApiRoutes {
  private ApiRoutes() {}

  public static class GenericResult {
    public boolean success;
    public String message;

    public GenericResult(boolean success, String message) {
      this.success = success;
      this.message = message;
    }

    public GenericResult(Exception e) {
      this(false, Utils.stacktraceToString(e));
    }

    public GenericResult() {
      this(true, null);
    }
  }

  public static class ExistsResult {
    public boolean success;
    public boolean exists;

    public ExistsResult(boolean success, boolean exists) {
      this.success = success;
      this.exists = exists;
    }

    public ExistsResult() {
    }
  }

  private static void haltInvalidRequest(int status, WsiteResult result) {
    Spark.halt(status, JsonUtils.toJson(new GenericResult(false, result.toString())));
  }

  private static void haltInvalidRequest(WsiteResult result) {
    haltInvalidRequest(400, result);
  }

  private static Object formResult(WsiteResult result) {
    if (result == WsiteResult.SUCCESS) {
      return new GenericResult(true, result.toString());
    }
    haltInvalidRequest(result);
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
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_PORT);
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
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_USERNAME_PATTERN);
      }
      cfg.put(KEY_USERNAME_PATTERN, usernamePattern);
    }
    if (params.hasKey(KEY_PASSWORD_PATTERN)) {
      String passwordPattern = HttpUtils.getString(params, KEY_PASSWORD_PATTERN);
      try {
        Pattern.compile(passwordPattern);
      } catch (PatternSyntaxException e) {
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_PASSWORD_PATTERN);
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
          haltInvalidRequest(WsiteResult.CONFIG_INVALID_KEYSTORE);
        }
        cfg.put(KEY_KEYSTORE_FILE, output.toString());
      } catch (IOException | ServletException e) {
        wsite.getLogger().error("Could not copy keystore file", e);
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_KEYSTORE);
      }
    } else if (params.hasKey("keystoreString")) {
      String keystoreString = HttpUtils.getString(params, "keystoreString");
      Path output = secureDir.resolve("keystore");
      try {
        Files.write(output, Base64.getDecoder().decode(keystoreString));
        cfg.put(KEY_KEYSTORE_FILE, output.toString());
      } catch (IOException e) {
        wsite.getLogger().error("Could not decode base64-encoded keystore string", e);
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_KEYSTORE);
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
          haltInvalidRequest(WsiteResult.CONFIG_INVALID_TRUSTSTORE);
        }
        cfg.put(KEY_TRUSTSTORE_FILE, output.toString());
      } catch (ServletException | IOException e) {
        wsite.getLogger().error("Could not copy truststore file", e);
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_TRUSTSTORE);
      }
    } else if (params.hasKey("truststoreString")) {
      String str = HttpUtils.getString(params, "truststoreString");
      Path output = secureDir.resolve("truststore");
      try {
        Files.write(output, Base64.getDecoder().decode(str));
        cfg.put(KEY_TRUSTSTORE_FILE, output.toString());
      } catch (IOException | IllegalArgumentException e) {
        wsite.getLogger().error("Could not write truststore string", e);
        haltInvalidRequest(WsiteResult.CONFIG_INVALID_TRUSTSTORE);
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
      haltInvalidRequest(WsiteResult.NO_TOKEN);
    }
    User user = wsite.getUserFromLoginToken(token);
    if (user == null) {
      haltInvalidRequest(WsiteResult.LOGIN_TOKEN_NOT_FOUND);
    }
    if (!user.operator) {
      haltInvalidRequest(WsiteResult.UNAUTHORIZED);
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
        return JsonUtils.toJson(new GenericResult(false, Utils.stacktraceToString(e)));
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
      return formResult(createPage(request, wsite));
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
      return formResult(updatePage(wsite, request));
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
        haltInvalidRequest(WsiteResult.PAGE_PATH_NOT_FOUND);
      }
      page.contents = Utils.encodeBase64(page.contents);
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

  public static class PageListRoute extends WsiteApiRoute {
    public PageListRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      int limit = HttpUtils.getInt(params, "limit");
      int page = HttpUtils.getInt(params, "page");
      String orderingSchemeStr = HttpUtils.getString(params, "order", "path");
      boolean descending = HttpUtils.getBool(params, "desc", false);
      PageRepository.OrderingScheme orderingScheme = PageRepository.OrderingScheme.fromString(orderingSchemeStr);
      if (orderingScheme == null) {
        orderingScheme = PageRepository.OrderingScheme.PATH;
      }
      return wsite.listPages(limit, page, orderingScheme, descending);
    }
  }

  public static class PageCountRoute extends WsiteApiRoute {
    public static class PageCount {
      public int count;
      public PageCount(int count) {
        this.count = count;
      }
      public PageCount() {
      }
    }
    public PageCountRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      return new PageCount(wsite.getPageCount());
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
      return formResult(deletePage(wsite, request));
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
      return formResult(createUser(wsite, request));
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
      return formResult(wsite.updateUserUsername(id, username));
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
      return formResult(wsite.updateUserEmailAddress(id, emailAddress));
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
      return formResult(wsite.updateUserPassword(id, password));
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
      return formResult(wsite.updateUserOperator(id, operator));
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
      return formResult(deleteUser(wsite, request));
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
        haltInvalidRequest(WsiteResult.USER_INVALID_QUERY);
      }
      return user;
    }
  }

  public static class ListUsersRoute extends WsiteApiRoute {
    public static class UserInfo {
      public String id;
      public String username;
      public String email;
      public String created;
      public boolean operator;
      public UserInfo(String id, String username, String email, String created, boolean operator) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.created = created;
        this.operator = operator;
      }
      public UserInfo() {
      }
    }
    public ListUsersRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      int limit = HttpUtils.getInt(params, "limit", 20);
      int page = HttpUtils.getInt(params, "page", 0);
      String orderingSchemeStr = HttpUtils.getString(params, "order", "username");
      boolean descending = HttpUtils.getBool(params, "desc", false);
      if (limit < 0) {
        limit = 0;
      }
      if (page < 0) {
        page = 0;
      }
      UserRepository.OrderingScheme orderingScheme = UserRepository.OrderingScheme.fromString(orderingSchemeStr);
      if (orderingScheme == null) {
        orderingScheme = UserRepository.OrderingScheme.USERNAME;
      }
      List<User> users = wsite.getUsers(limit, page, orderingScheme, descending);
      List<UserInfo> userInfos = new ArrayList<>(users.size());
      users.forEach(user -> userInfos.add(new UserInfo(user.id.toString(), user.username, user.emailAddress,
          Utils.formatFileSafeInstant(user.whenCreated), user.operator))
      );
      return userInfos;
    }
  }

  public static class UserCountRoute extends WsiteApiRoute {
    public static class UserCount {
      public int count;
      public UserCount(int count) {
        this.count = count;
      }
      public UserCount() {
      }
    }
    public UserCountRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      return new UserCount(wsite.getNumberOfUsers());
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
      ((Buffer) tokenBuffer).rewind(); // Java 8 compatibility building on Java 9+
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
        return JsonUtils.toJson(new GenericResult(true, res.right));
      }
      return JsonUtils.toJson(new GenericResult(false, res.left.toString()));
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
      return formResult(wsite.deleteLogin(token));
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
      boolean toRoot = HttpUtils.getBool(params, "root");
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
      return wsite.uploadAsset(path, in, replace, toRoot);
    }
  }

  public static class EditAssetRoute extends WsiteApiRoute {
    public EditAssetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      boolean root = HttpUtils.getBool(params, "root", false);
      String newPath = HttpUtils.getString(params, "newPath", null);
      boolean newRoot = HttpUtils.getBool(params, "newRoot", false);
      byte[] contents = HttpUtils.getBase64EncodedBytes(params, "contents");
      if (contents == null) {
        contents = new byte[0];
      }
      return wsite.editAsset(path, root, newPath, newRoot, contents);
    }
  }

  public static class DeleteAssetRoute extends WsiteApiRoute {
    public DeleteAssetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws IOException {
      checkPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      boolean root = HttpUtils.getBool(params, "root", false);
      return wsite.deleteAsset(path, root);
    }
  }

  public static class GetAssetRoute extends WsiteApiRoute {
    public static class Asset {
      public String path;
      public String contents;
      public Asset() {
      }
      public Asset(String path, String contents) {
        this.path = path;
        this.contents = contents;
      }
    }
    public GetAssetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      boolean root = HttpUtils.getBool(params, "root", false);
      if (path == null) {
        haltInvalidRequest(WsiteResult.ASSET_NO_PATH);
      }
      try (InputStream in = wsite.getAssetStream(path, root)) {
        if (in != null) {
          byte[] contents = IOUtils.readBytesFromStream(in);
          Path assetPath = wsite.toLocalPath(wsite.getAssetPath(path, root));
          return new Asset(assetPath.subpath(1, assetPath.getNameCount()).toString(), Base64.getEncoder().encodeToString(contents));
        }
      }
      haltInvalidRequest(WsiteResult.ASSET_PATH_NOT_FOUND);
      return null;
    }
  }

  public static class AssetExistsRoute extends WsiteApiRoute {
    public AssetExistsRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      boolean root = HttpUtils.getBool(params, "root");
      boolean exists = wsite.doesAssetExist(path, root);
      return Collections.singletonMap("exists", exists);
    }
  }

  public static class ListAssetsRoute extends WsiteApiRoute {
    public ListAssetsRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkPermission(wsite, request);
      return wsite.listAssets(); // TODO: Implement a limit and page system
    }
  }

  public static class ReloadProtectedAssetsRoute extends WsiteApiRoute {
    public ReloadProtectedAssetsRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkPermission(wsite, request);
      wsite.reloadProtectedAssets();
      return formResult(WsiteResult.SUCCESS);
    }
  }

  public static class UserExistsRoute extends WsiteApiRoute {
    public UserExistsRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      QueryParamsMap params = request.queryMap();
      UUID id = HttpUtils.getUuid(params, "id");
      if (id != null) {
        return new ExistsResult(true, wsite.getUserFromId(id) != null);
      }
      String username = HttpUtils.getString(params, "username");
      if (username != null) {
        return new ExistsResult(true, wsite.getUserFromUsername(username) != null);
      }
      String email = HttpUtils.getString(params, "email");
      if (email != null) {
        return new ExistsResult(true, wsite.getUserFromEmailAddress(email) != null);
      }
      haltInvalidRequest(WsiteResult.USER_NO_VALID_CHECK_FIELDS);
      return null;
    }
  }

  public static class PageExistsRoute extends WsiteApiRoute {
    public PageExistsRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      if (path != null) {
        return new ExistsResult(true, wsite.getPage(path) != null);
      }
      haltInvalidRequest(WsiteResult.PAGE_PATH_MISSING);
      return null;
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
      return wsite.getConfigValues();
    }
  }

  public static class ConfigUpdateRoute extends WsiteApiRoute {
    public ConfigUpdateRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) {
      checkPermission(wsite, request);
      Map<String, Object> cfg = getConfig(wsite, request);
      wsite.restartWithNewConfiguration(cfg);
      return formResult(WsiteResult.SUCCESS);
    }
  }

}
