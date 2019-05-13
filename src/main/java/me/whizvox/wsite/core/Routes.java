package me.whizvox.wsite.core;

import me.whizvox.wsite.database.Page;
import me.whizvox.wsite.database.User;
import me.whizvox.wsite.util.HttpUtils;
import me.whizvox.wsite.util.JsonUtils;
import me.whizvox.wsite.util.Pair;
import me.whizvox.wsite.util.Utils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import spark.*;

import javax.servlet.MultipartConfigElement;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static spark.Spark.halt;

public class Routes {
  private Routes() {}

  abstract static class WsiteFilter implements Filter {
    protected WsiteService wsite;
    public WsiteFilter(WsiteService wsite) {
      this.wsite = wsite;
    }
  }

  abstract static class WsiteRoute implements Route {
    private WsiteService wsite;
    public WsiteRoute(WsiteService wsite) {
      this.wsite = wsite;
    }
    protected abstract Object handle_do(Request request, Response response, WsiteService wsite) throws Exception;
    @Override
    public Object handle(Request request, Response response) throws Exception {
      try {
        return handle_do(request, response, wsite);
      } catch (HaltException e) {
        throw e;
      } catch (Exception e) {
        wsite.getLogger().error("An exception has occurred while handling a request", e);
        Map<String, Object> dataModel = setupBasicDataModel(wsite);
        dataModel.put("message", e.getMessage());
        dataModel.put("stacktrace", Utils.stacktraceToString(e));
        return wsite.parseTemplate("exception.ftlh", dataModel);
      }
    }
  }

  public static Route getTemplatedRoute(WsiteService wsite, String name, boolean needsOperator) {
    return new WsiteRoute(wsite) {
      @Override
      protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
        if (needsOperator) {
          checkUserPermission(wsite, request);
        }
        return wsite.parseTemplate(name, setupBasicDataModel(wsite));
      }
    };
  }

  private static void checkUserPermission(WsiteService wsite, Request request) {
    User user = request.attribute(UserFilter.ATTRIBUTE_USER);
    // TODO: Check client's user agent, delete login and do not authorize if it doesn't match
    if (user == null || !user.operator) {
      haltWithBody(wsite, 403);
    }
  }

  private static void setupMultipartConfig(WsiteService wsite, Request request) {
    request.attribute("org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(wsite.resolvePath(Reference.TEMP_DIR).toString())
    );
  }

  private static Map<String, Object> setupBasicDataModel(WsiteService wsite) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put(WsiteConfiguration.KEY_SITE_NAME, wsite.getSiteName());
    return dataModel;
  }

  public static class UserFilter extends WsiteFilter {
    static final String
        COOKIE_LOGIN_TOKEN = "login",
        ATTRIBUTE_USER = "user";
    public UserFilter(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public void handle(Request request, Response response) throws Exception {
      String loginToken = request.cookie(COOKIE_LOGIN_TOKEN);
      User user = wsite.getUserFromLoginToken(loginToken);
      request.attribute(ATTRIBUTE_USER, user);
    }
  }

  public static class ShutdownRoute extends WsiteRoute {
    public ShutdownRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      if (!Reference.usingDevBuild()) { // allow the user of a dev build to shutdown a server
        checkUserPermission(wsite, request);
      }
      boolean confirm = HttpUtils.getBool(request.queryMap(), "confirm", false);
      if (!confirm) {
        return WsiteResult.NOT_CONFIRMED;
      }
      wsite.shutdown();
      return WsiteResult.SUCCESS;
    }
  }

  public static class RestartRoute extends WsiteRoute {
    public RestartRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      wsite.restart();
      return WsiteResult.SUCCESS;
    }
  }

  public static class PageGetRoute extends WsiteRoute {
    public static String getPage(WsiteService wsite, Request request, String path) {
      if (Utils.isNullOrEmpty(path)) {
        path = "home";
      }
      Page page = wsite.getPage(path);
      if (page == null) {
        haltWithBody(wsite, 404);
      }
      String contents;
      switch (page.syntax) {
        case HTML_LIMITED:
          contents = Utils.sanitizeHtml(page.contents);
          break;
        case HTML:
          contents = page.contents;
          break;
        case EMBEDDED:
        case HTML_EMBEDDED:
        case HTML_LIMITED_EMBEDDED:
        case MARKDOWN:
          Map<String, Object> dataModel = setupBasicDataModel(wsite);
          dataModel.put("pageTitle", page.title);
          dataModel.put("escape", false);
          switch (page.syntax) {
            case EMBEDDED:
              dataModel.put("contents", Utils.htmlToPlainText(page.contents));
              break;
            case HTML_EMBEDDED:
              dataModel.put("contents", page.contents);
              break;
            case HTML_LIMITED_EMBEDDED:
              dataModel.put("contents", Utils.sanitizeHtml(page.contents));
              break;
            case MARKDOWN:
              dataModel.put("contents", Utils.parseMarkdown(page.contents));
              break;
          }
          contents = wsite.parseTemplate("page.ftlh", dataModel);
          break;
        default: // safest option
          contents = Utils.htmlToPlainText(page.contents);
      }
      return contents;
    }
    public PageGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      String page = request.splat()[0];
      // TODO: Once/if control page routes are configurable, find a way to incorporate it here
      if (page.equals("control/wsconsole")) {
        return null;
      }
      return getPage(wsite, request, request.splat()[0]);
    }
  }

  public static class IndexPageRoute extends WsiteRoute {
    private final String path;
    public IndexPageRoute(WsiteService wsite, String path) {
      super(wsite);
      this.path = path;
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      return PageGetRoute.getPage(wsite, request, path);
    }
  }

  public static class NewPagePostRoute extends WsiteRoute {
    public NewPagePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      return ApiRoutes.PageCreateRoute.createPage(request, wsite);
    }
  }

  public static class EditPagePostRoute extends WsiteRoute {
    public EditPagePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String origPath = HttpUtils.getString(params, "origPath");
      String path = HttpUtils.getString(params, "path");
      String title = HttpUtils.getString(params, "title");
      String contents = HttpUtils.getString(params, "contents");
      String syntax = HttpUtils.getString(params, "syntax");
      return wsite.updatePage(origPath, path, title, syntax, contents);
    }
  }

  public static class DeletePagePostRoute extends WsiteRoute {
    public DeletePagePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      return ApiRoutes.PageDeleteRoute.deletePage(wsite, request);
    }
  }

  public static class NewUserPostRoute extends WsiteRoute {
    public NewUserPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      return ApiRoutes.UserCreateRoute.createUser(wsite, request);
    }
  }

  public static class EditSelfGetRoute extends WsiteRoute {
    public EditSelfGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      User user = request.attribute(UserFilter.ATTRIBUTE_USER);
      if (user == null) {
        haltWithBody(wsite, 403);
      }
      return wsite.parseTemplate("editSelf.ftlh", setupBasicDataModel(wsite));
    }
  }

  public static class EditSelfPostRoute extends WsiteRoute {
    public EditSelfPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      User user = request.attribute(UserFilter.ATTRIBUTE_USER);
      if (user == null) {
        haltWithBody(wsite, 403);
      }
      QueryParamsMap params = request.queryMap();
      String newUsername = HttpUtils.getString(params, "username");
      String newEmail = HttpUtils.getString(params, "email");
      char[] newPassword = HttpUtils.getCharArray(params, "password");
      char[] currentPassword = HttpUtils.getCharArray(params, "currentPassword");
      if (!wsite.getHashManager().check(currentPassword, user.password)) {
        return WsiteResult.LOGIN_INCORRECT_PASSWORD;
      }
      if (Utils.isNullOrEmpty(newUsername)) {
        newUsername = user.username;
      }
      if (Utils.isNullOrEmpty(newEmail)) {
        newEmail = user.emailAddress;
      }
      return wsite.updateUser(user.id, newUsername, newEmail, newPassword, user.operator);
    }
  }

  public static class EditUserRoute extends WsiteRoute {
    public EditUserRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      User user = request.attribute(UserFilter.ATTRIBUTE_USER);
      QueryParamsMap params = request.queryMap();
      UUID id = HttpUtils.getUuid(params, "id");
      if (id.equals(user.id)) {
        return WsiteResult.USER_MATCHING_IDS;
      }
      String username = HttpUtils.getString(params, "username");
      String email = HttpUtils.getString(params, "email");
      char[] password = HttpUtils.getCharArray(params, "password");
      boolean operator = HttpUtils.getBool(params, "operator");
      return wsite.updateUser(id, username, email, password, operator);
    }
  }

  public static class DeleteUserPostRoute extends WsiteRoute {
    public DeleteUserPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      return ApiRoutes.UserDeleteRoute.deleteUser(wsite, request);
    }
  }

  public static class LoginPostRoute extends WsiteRoute {
    public LoginPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      Pair<WsiteResult, String> res = ApiRoutes.LoginCreateRoute.createLogin(wsite, request);
      if (res.left == WsiteResult.SUCCESS) {
        response.cookie(UserFilter.COOKIE_LOGIN_TOKEN, res.right);
      }
      return res.left;
    }
  }

  public static class LogoutGetRoute extends WsiteRoute {
    public LogoutGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      String token = request.cookie(UserFilter.COOKIE_LOGIN_TOKEN);
      return wsite.deleteLogin(token);
    }
  }

  public static class ConfigGetRoute extends WsiteRoute {
    public ConfigGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> cfg = wsite.getConfigValues();
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put("config", cfg);
      return wsite.parseTemplate("config.ftlh", dataModel);
    }
  }

  public static class ConfigPostRoute extends WsiteRoute {
    public static Map<String, Object> getConfig(WsiteService wsite, Request request) {
      Map<String, Object> config = ApiRoutes.getConfig(wsite, request);
      List<String> keysToRemove = new ArrayList<>();
      config.forEach((key, value) -> {
        if (value == null || value.equals("") || value.equals(new Properties())) {
          keysToRemove.add(key);
        }
      });
      keysToRemove.forEach(config::remove);
      return config;
    }
    public ConfigPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      setupMultipartConfig(wsite, request);
      wsite.restartWithNewConfiguration(getConfig(wsite, request));
      return WsiteResult.SUCCESS;
    }
  }

  public static class UploadAssetPostRoute extends WsiteRoute {
    public UploadAssetPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      setupMultipartConfig(wsite, request);
      QueryParamsMap params = request.queryMap();
      try (InputStream in = HttpUtils.getFile(request, "file")) {
        String path = HttpUtils.getString(params, "path");
        boolean replace = HttpUtils.getBool(params, "replace");
        boolean toRoot = HttpUtils.getBool(params, "root");
        return wsite.uploadAsset(path, in, replace, toRoot);
      }
    }
  }

  public static class EditAssetPostRoute extends WsiteRoute {
    public EditAssetPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      boolean root = HttpUtils.getBool(params, "root");
      String newPath = HttpUtils.getString(params, "newPath");
      boolean newRoot = HttpUtils.getBool(params, "newRoot");
      String contents = HttpUtils.getString(params, "contents");
      return wsite.editAsset(path, root, newPath, newRoot, contents.getBytes(StandardCharsets.UTF_8));
    }
  }

  public static class DeleteAssetPostRoute extends WsiteRoute {
    public DeleteAssetPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String path = HttpUtils.getString(params, "path");
      boolean root = HttpUtils.getBool(params, "root");
      return wsite.deleteAsset(path, root);
    }
  }

  @WebSocket
  public static class ConsoleRoute {
    private Queue<Session> sessions;
    private WsiteService wsite;
    public ConsoleRoute(WsiteService wsite) {
      this.wsite = wsite;
      sessions = new ConcurrentLinkedQueue<>();
    }
    private boolean checkPermission(Session session) {
      List<HttpCookie> cookies = session.getUpgradeRequest().getCookies();
      HttpCookie loginCookie = cookies.stream()
          .filter(cookie -> cookie.getName().equals(UserFilter.COOKIE_LOGIN_TOKEN))
          .findFirst().orElse(null);
      boolean authorized = false;
      if (loginCookie != null) {
        User user = wsite.getUserFromLoginToken(loginCookie.getValue());
        authorized = user != null && user.operator;
      }
      if (!authorized) {
        sessions.remove(session);
        send_do(session, new WsiteLogbackAppender.LogEvent(0, "! UNAUTHORIZED !"));
        session.close();
      }
      return authorized;
    }
    private void send_do(Session session, Object msg) {
      try {
        session.getRemote().sendString(JsonUtils.toJson(msg));
      } catch (IOException e) {
        wsite.getLogger().error("Could not send message to session", e);
      }
    }
    public void send(Session session, Object msg) {
      if (checkPermission(session)) {
        send_do(session, msg);
      }
    }
    public void broadcast(Object msg) {
      sessions.forEach(session -> send(session, msg));
    }
    public void disconnectAll() {
      sessions.forEach(Session::close);
    }
    @OnWebSocketConnect
    public void connected(Session session) {
      if (checkPermission(session)) {
        wsite.getLogger().info("Client has connected to the remote console ({})", session.getRemoteAddress().toString());
        sessions.add(session);
      }
    }
    @OnWebSocketClose
    public void closed(Session session, int status, String reason) {
      wsite.getLogger().info("Client disconnected from the remote console ({}, {})", status, reason);
      sessions.remove(session);
    }
  }

  public static class SetupGetRoute extends WsiteRoute {
    public SetupGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfigValues());
      return wsite.parseTemplate("setup.ftlh", dataModel);
    }
  }

  public static class SetupPostRoute extends WsiteRoute {
    public SetupPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      setupMultipartConfig(wsite, request);
      Map<String, Object> cfg = ConfigPostRoute.getConfig(wsite, request);
      WsiteResult res = ApiRoutes.UserCreateRoute.createUser(wsite, request);
      // TODO: As of right now it's possible to change the client's HTML to create a user that's not an operator
      if (res == WsiteResult.SUCCESS) {
        wsite.getLogger().info("Setup sequence has successfully completed");
        wsite.restartWithNewConfiguration(cfg);
      }
      return res;
    }
  }

  public static String generateHaltBody(WsiteService wsite, int status, String customMessage) {
    Map<String, Object> dataModel = setupBasicDataModel(wsite);
    dataModel.put("code", status);
    String summary, message;
    switch (status) {
      case 403:
        summary = "Forbidden request";
        message = "The requested action is forbidden without special permissions.";
        break;
      case 404:
        summary = "Not found";
        message = "The requested page/resource could not be found.";
        break;
      case 500:
        summary = "Internal server error";
        message = "The server has experienced an unusual internal error. Please report this immediately!";
        break;
      default:
        summary = "Error";
        message = "The server could not complete the request due to an undefined error.";
    }
    if (customMessage != null) {
      message = customMessage;
    }
    dataModel.put("summary", summary);
    dataModel.put("message", message);
    return wsite.parseTemplate("halt.ftlh", dataModel);
  }

  public static String generateHaltBody(WsiteService wsite, int status) {
    return generateHaltBody(wsite, status, null);
  }

  public static void haltWithBody(WsiteService wsite, int status) {
    halt(status, generateHaltBody(wsite, status));
  }

}
