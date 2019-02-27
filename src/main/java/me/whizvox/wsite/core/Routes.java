package me.whizvox.wsite.core;

import me.whizvox.wsite.database.Page;
import me.whizvox.wsite.database.User;
import me.whizvox.wsite.util.IOUtils;
import me.whizvox.wsite.util.Utils;
import spark.*;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static spark.Spark.halt;

public class Routes {
  private Routes() {}

  abstract static class WsiteFilter implements Filter {
    protected WsiteService wsite;
    public WsiteFilter(WsiteService wsite) {
      this.wsite = wsite;
    }
  }

  abstract static class WsiteApiRoute implements Route {
    protected WsiteService wsite;
    public WsiteApiRoute(WsiteService wsite) {
      this.wsite = wsite;
    }
  }

  abstract static class WsiteRoute implements Route {
    protected WsiteService wsite;
    public WsiteRoute(WsiteService wsite) {
      this.wsite = wsite;
    }
    protected abstract Object handle_do(Request request, Response response) throws Exception;
    @Override
    public Object handle(Request request, Response response) throws Exception {
      try {
        return handle_do(request, response);
      } catch (HaltException e) {
        throw e;
      } catch (Exception e) {
        wsite.getLogger().error("An exception has occurred while handling a request", e);
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("siteName", wsite.getSiteName());
        dataModel.put("message", e.getMessage());
        dataModel.put("stacktrace", Utils.stacktraceToString(e));
        return wsite.parseTemplate("exception.ftlh", dataModel);
      }
    }
  }

  abstract static class WsiteExceptionHandler<T extends Exception> implements ExceptionHandler<T> {
    protected WsiteService wsite;
    public WsiteExceptionHandler(WsiteService wsite) {
      this.wsite = wsite;
    }
  }

  private static void checkUserPermission(WsiteService wsite, Request request) {
    User user = request.attribute(UserFilter.ATTRIBUTE_USER);
    if (user == null || !user.operator) {
      haltWithBody(wsite, 403);
    }
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
    public Object handle_do(Request request, Response response) throws Exception {
      if (!Reference.usingDevBuild()) {
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
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      wsite.restart();
      return "Server is now restarting...";
    }
  }

  public static class PageGetRoute extends WsiteRoute {
    public PageGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      String path = request.params(":pagePath");
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
          Map<String, Object> dataModel = new HashMap<>();
          dataModel.put("siteName", wsite.getSiteName());
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
  }

  public static class NewPageGetRoute extends WsiteRoute {
    public NewPageGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dm = new HashMap<>();
      dm.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("newPage.ftlh", dm);
    }
  }

  public static class NewPagePostRoute extends WsiteRoute {
    public NewPagePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String path = params.get("path").value();
      String title = params.get("title").value();
      String contents = params.get("contents").value();
      String syntax = params.get("syntax").value();
      return wsite.createNewPage(path, title, syntax, contents);
    }
  }

  public static class DeletePageGetRoute extends WsiteRoute {
    public DeletePageGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("deletePage.ftlh", dataModel);
    }
  }

  public static class DeletePagePostRoute extends WsiteRoute {
    public DeletePagePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      String path = request.queryMap("path").value();
      return wsite.deletePage(path);
    }
  }

  public static class NewUserGetRoute extends WsiteRoute {
    public NewUserGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("newUser.ftlh", dataModel);
    }
  }

  public static class NewUserPostRoute extends WsiteRoute {
    static WsiteResult createNewUser(WsiteService wsite, Request request) {
      QueryParamsMap params = request.queryMap();
      String username = params.get("username").value();
      String emailAddress = params.get("email_address").value();
      String password = params.get("password").value();
      boolean operator = Optional.ofNullable(params.get("operator").booleanValue()).orElse(false);
      return wsite.createNewUser(username, emailAddress, password.toCharArray(), operator);
    }
    public NewUserPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      return createNewUser(wsite, request);
    }
  }

  public static class EditUserGetRoute extends WsiteRoute {
    public EditUserGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("editUser.ftlh", dataModel);
    }
  }

  public static class DeleteUserGetRoute extends WsiteRoute {
    public DeleteUserGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("deleteUser.ftlh", dataModel);
    }
  }

  public static class DeleteUserPostRoute extends WsiteRoute {
    public DeleteUserPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      QueryParamsMap params = request.queryMap();
      String idStr = params.get("id").value();
      UUID id = null;
      if (idStr != null) {
        try {
          id = UUID.fromString(idStr);
        } catch (IllegalArgumentException ignored) {}
      }
      String username = params.get("username").value();
      String emailAddress = params.get("email_address").value();
      User user = null;
      if (id != null) {
        user = wsite.getUserFromId(id);
      } else if (!Utils.isNullOrEmpty(username)) {
        user = wsite.getUserFromUsername(username);
      } else if (!Utils.isNullOrEmpty(emailAddress)) {
        user = wsite.getUserFromEmailAddress(emailAddress);
      }
      if (user == null) {
        return WsiteResult.USER_INVALID_QUERY;
      }
      User sUser = request.attribute(UserFilter.ATTRIBUTE_USER);
      if (sUser.id.equals(user.id)) {
        return WsiteResult.USER_MATCHING_IDS;
      }
      return wsite.deleteUser(user.id);
    }
  }

  public static class LoginGetRoute extends WsiteRoute {
    public LoginGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("login.ftlh", dataModel);
    }
  }

  public static class LoginPostRoute extends WsiteRoute {
    public LoginPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      QueryParamsMap params = request.queryMap();
      String query = params.get("username").value();
      String password = params.get("password").value();
      boolean rememberMe = Optional.ofNullable(params.get("remember_me").booleanValue()).orElse(false);
      int minutesUntilExpire = rememberMe ? 10008 : 60; // 1 week vs 1 hour
      CharBuffer tokenBuffer = CharBuffer.allocate(Reference.LOGIN_TOKEN_LENGTH);
      WsiteResult result = wsite.createLogin(query, password.toCharArray(), minutesUntilExpire, request.userAgent(),
          request.ip(), tokenBuffer);
      if (result == WsiteResult.SUCCESS) {
        tokenBuffer.flip();
        String token = tokenBuffer.toString();
        response.cookie(UserFilter.COOKIE_LOGIN_TOKEN, token);
      }
      return result;
    }
  }

  public static class LogoutGetRoute extends WsiteRoute {
    public LogoutGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      String token = request.cookie(UserFilter.COOKIE_LOGIN_TOKEN);
      return wsite.deleteLogin(token);
    }
  }

  public static class SetupGetRoute extends WsiteRoute {
    public SetupGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      return wsite.parseTemplate("setup.ftlh", new HashMap<>());
    }
  }

  public static class SetupPostRoute extends WsiteRoute {
    public SetupPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      request.attribute("org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(wsite.resolvePath("temp").toString())
      );

      QueryParamsMap params = request.queryMap();
      WsiteConfiguration config = new WsiteConfiguration();

      String siteName = params.get("siteName").value();
      if (!Utils.isNullOrEmpty(siteName)) {
        config.siteName = siteName;
      }
      String rootDirectory = params.get("rootDirectory").value();
      if (!Utils.isNullOrEmpty(rootDirectory)) {
        config.rootDirectory = rootDirectory;
      }
      String portStr = params.get("port").value();
      if (!Utils.isNullOrEmpty(portStr)) {
        config.port = Integer.parseInt(portStr);
      }
      Optional.ofNullable(params.get("continuouslyRestart").booleanValue()).ifPresent(r -> config.continuouslyRestart = r);
      String restartIntervalStr = params.get("restartInterval").value();
      if (!Utils.isNullOrEmpty(restartIntervalStr)) {
        config.restartInterval = Integer.parseInt(restartIntervalStr);
      }

      String usernamePattern = params.get("usernamePattern").value();
      if (!Utils.isNullOrEmpty(usernamePattern)) {
        config.usernamePattern = usernamePattern;
      }
      String passwordPattern = params.get("passwordPattern").value();
      if (!Utils.isNullOrEmpty(passwordPattern)) {
        config.passwordPattern = passwordPattern;
      }

      String dbUsername = params.get("dbUsername").value();
      String dbPassword = params.get("dbPassword").value();
      if (!Utils.isNullOrEmpty(dbUsername) && !Utils.isNullOrEmpty(dbPassword)) {
        config.databaseUsername = dbUsername;
        config.databasePassword = dbPassword;
      }
      String dbPropertiesStr = params.get("dbProperties").value();
      config.databaseProperties = Utils.parseProperties(dbPropertiesStr);

      Path keystoreFilePath = null;
      Path truststoreFilePath = null;
      if (config.enableSsl = Optional.ofNullable(params.get("enableSsl").booleanValue()).orElse(false)) {
        Path sslDir = wsite.resolvePath("ssl");
        Part keystoreFilePart = request.raw().getPart("keystoreFile");
        if (keystoreFilePart != null) {
          keystoreFilePath = sslDir.resolve(keystoreFilePart.getSubmittedFileName());
          try (InputStream in = keystoreFilePart.getInputStream()) {
            Files.copy(in, keystoreFilePath);
          }
          config.keystoreFile = keystoreFilePath.toString();
          config.keystorePassword = params.get("keystorePassword").value();
        } else {
          config.keystoreFile = null;
          config.keystorePassword = null;
        }
        Part truststoreFilePart = request.raw().getPart("truststoreFile");
        if (truststoreFilePart != null) {
          truststoreFilePath = sslDir.resolve(truststoreFilePart.getSubmittedFileName());
          try (InputStream in = truststoreFilePart.getInputStream()) {
            Files.copy(in, truststoreFilePath);
          }
          config.truststoreFile = truststoreFilePath.toString();
          config.truststorePassword = params.get("truststorePassword").value();
        } else {
          config.truststoreFile = null;
          config.truststorePassword = null;
        }
      }

      if (config.enableSmtp = Optional.ofNullable(params.get("enableSmtp").booleanValue()).orElse(false)) {
        config.smtpHost = params.get("smtpHost").value();
        config.smtpUser = params.get("smtpUser").value();
        config.smtpPassword = params.get("smtpPassword").value();
      }

      String iUsername = params.get("iUsername").value();
      String iEmailAddress = params.get("iEmailAddress").value();
      char[] iPassword = params.get("iPassword").value().toCharArray();
      WsiteResult newUserResult = wsite.createNewUser(iUsername, iEmailAddress, iPassword, true,
          Pattern.compile(config.usernamePattern), Pattern.compile(config.passwordPattern));
      if (newUserResult != WsiteResult.SUCCESS) {
        wsite.getLogger().error("Initial user could not be created: {}. Will delete any uploaded SSL files...",
            newUserResult);
        if (keystoreFilePath != null) {
          IOUtils.delete(keystoreFilePath);
        }
        if (truststoreFilePath != null) {
          IOUtils.delete(truststoreFilePath);
        }
        halt(400, generateHaltBody(wsite, 400, "Could not create new user: " + newUserResult));
      }

      wsite.restartWithNewConfiguration(config);
      return "SUCCESS";
    }
  }

  public static class TeapotRoute extends WsiteRoute {
    public TeapotRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      return generateHaltBody(wsite, 418);
    }
  }

  public static class NotFoundRoute extends WsiteRoute {
    public NotFoundRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      Map<String, Object> dataModel = new HashMap<>();
      dataModel.put("siteName", wsite.getSiteName());
      return wsite.parseTemplate("404.ftlh", dataModel);
    }
  }

  public static String generateHaltBody(WsiteService wsite, int status, String customMessage) {
    Map<String, Object> dataModel = new HashMap<>();
    dataModel.put("siteName", wsite.getSiteName());
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
