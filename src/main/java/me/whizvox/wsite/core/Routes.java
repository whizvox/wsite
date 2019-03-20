package me.whizvox.wsite.core;

import me.whizvox.wsite.database.Page;
import me.whizvox.wsite.database.User;
import me.whizvox.wsite.util.HttpUtils;
import me.whizvox.wsite.util.Pair;
import me.whizvox.wsite.util.Utils;
import spark.*;

import javax.servlet.MultipartConfigElement;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
      wsite.shutdown();
      return "Server is now shutting down...";
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
      return "Server is now restarting...";
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
      return getPage(wsite, request, request.params(":path"));
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

  public static class EditUserUsernamePostRoute extends WsiteRoute {
    public EditUserUsernamePostRoute(WsiteService wsite) {
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
      return wsite.updateUserUsername(user.id, newUsername);
    }
  }

  public static class EditUserEmailPostRoute extends WsiteRoute {
    public EditUserEmailPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      User user = request.attribute(UserFilter.ATTRIBUTE_USER);
      if (user == null) {
        haltWithBody(wsite, 403);
      }
      QueryParamsMap params = request.queryMap();
      String newEmail = HttpUtils.getString(params, "email");
      return wsite.updateUserEmailAddress(user.id, newEmail);
    }
  }

  public static class EditUserPasswordRoute extends WsiteRoute {
    public EditUserPasswordRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      User user = request.attribute(UserFilter.ATTRIBUTE_USER);
      if (user == null) {
        haltWithBody(wsite, 403);
      }
      QueryParamsMap params = request.queryMap();
      char[] newPassword = HttpUtils.getCharArray(params, "password");
      return wsite.updateUserPassword(user.id, newPassword);
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
    private final String templateName;
    private final String filterName;
    public ConfigGetRoute(WsiteService wsite, String baseName) {
      super(wsite);
      templateName = "config" + baseName.substring(0, 1).toUpperCase() + baseName.substring(1) + ".ftlh";
      filterName = baseName;
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> cfg = wsite.getConfigValues(filterName);
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put("config", cfg);
      return wsite.parseTemplate(templateName, dataModel);
    }
  }

  public static class ConfigPostRoute extends WsiteRoute {
    public ConfigPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      setupMultipartConfig(wsite, request);
      Map<String, Object> config = ApiRoutes.getConfig(wsite, request);
      wsite.restartWithNewConfiguration(config);
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
        return wsite.uploadAsset(path, in, replace);
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
      String origPath = HttpUtils.getString(params, "origPath");
      String path = HttpUtils.getString(params, "path");
      String contents = HttpUtils.getString(params, "contents");
      return wsite.editAsset(origPath, path, contents.getBytes(StandardCharsets.UTF_8));
    }
  }

  public static class DeleteAssetPostRoute extends WsiteRoute {
    public DeleteAssetPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      checkUserPermission(wsite, request);
      String path = HttpUtils.getString(request.queryMap(), "path");
      return wsite.deleteAsset(path);
    }
  }

  public static class SetupGetRoute extends WsiteRoute {
    public SetupGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfigValues("all"));
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
      Map<String, Object> cfg = ApiRoutes.getConfig(wsite, request);
      WsiteResult res = ApiRoutes.UserCreateRoute.createUser(wsite, request);
      // TODO: As of right now it's possible to change the client's HTML to create a user that's not an operator
      if (res == WsiteResult.SUCCESS) {
        wsite.getLogger().info("Setup sequence has successfully completed");
        wsite.restartWithNewConfiguration(cfg);
      }
      return res;
    }
  }

  public static class TeapotRoute extends WsiteRoute {
    public TeapotRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response, WsiteService wsite) throws Exception {
      haltWithBody(wsite, 418);
      return null;
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
      case 418:
        summary = "I'm a teapot!";
        message = "Short and stout!";
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
