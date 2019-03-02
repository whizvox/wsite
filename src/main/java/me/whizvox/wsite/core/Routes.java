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
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
        Map<String, Object> dataModel = setupBasicDataModel(wsite);
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
    public Object handle_do(Request request, Response response) throws Exception {
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
  }

  public static class NewPageGetRoute extends WsiteRoute {
    public NewPageGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      return wsite.parseTemplate("newPage.ftlh", setupBasicDataModel(wsite));
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
      return wsite.parseTemplate("deletePage.ftlh", setupBasicDataModel(wsite));
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
      return wsite.parseTemplate("newUser.ftlh", setupBasicDataModel(wsite));
    }
  }

  public static class NewUserPostRoute extends WsiteRoute {
    static WsiteResult createNewUser(WsiteService wsite, Request request) {
      QueryParamsMap params = request.queryMap();
      String username = params.get("userUsername").value();
      String emailAddress = params.get("userEmailAddress").value();
      String password = params.get("userPassword").value();
      boolean operator;
      if (wsite.getNumberOfUsers() > 0) {
        operator = Optional.ofNullable(params.get("userOperator").booleanValue()).orElse(false);
      } else {
        operator = true;
      }
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

  public static class DeleteUserGetRoute extends WsiteRoute {
    public DeleteUserGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    public Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      return wsite.parseTemplate("deleteUser.ftlh", setupBasicDataModel(wsite));
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
      return wsite.parseTemplate("login.ftlh", setupBasicDataModel(wsite));
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

  public static class ConfigSiteGetRoute extends WsiteRoute {
    public ConfigSiteGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfig());
      return wsite.parseTemplate("configSite.ftlh", dataModel);
    }
  }

  public static class ConfigSitePostRoute extends WsiteRoute {
    static WsiteResult getSiteConfig(Request request, Map<String, Object> config) {
      QueryParamsMap params = request.queryMap();
      String siteName = params.get(WsiteConfiguration.KEY_SITE_NAME).value();
      if (!Utils.isNullOrEmpty(siteName)) {
        config.put(WsiteConfiguration.KEY_SITE_NAME, siteName);
      } else {
        return WsiteResult.CONFIG_NO_SITE_NAME;
      }

      String portStr = params.get(WsiteConfiguration.KEY_PORT).value();
      boolean validPort = false;
      if (!Utils.isNullOrEmpty(portStr)) {
        try {
          int port = Integer.parseInt(portStr);
          if (port >= 0 && port <= Short.MAX_VALUE) {
            config.put(WsiteConfiguration.KEY_PORT, port);
            validPort = true;
          }
        } catch (NumberFormatException ignored) {}
      }
      if (!validPort) {
        return WsiteResult.CONFIG_INVALID_PORT;
      }

      boolean continuouslyRestart = Optional.ofNullable(
          params.get(WsiteConfiguration.KEY_CONTINUOUSLY_RESTART).booleanValue()).orElse(true);
      config.put(WsiteConfiguration.KEY_CONTINUOUSLY_RESTART, continuouslyRestart);

      String restartIntervalStr = params.get(WsiteConfiguration.KEY_RESTART_INTERVAL).value();
      boolean validRestartInterval = false;
      if (!Utils.isNullOrEmpty(restartIntervalStr)) {
        try {
          int restartInterval = Integer.parseInt(restartIntervalStr);
          if (restartInterval >= Reference.MIN_RESTART_INTERVAL && restartInterval <= Reference.MAX_RESTART_INTERVAL) {
            config.put(WsiteConfiguration.KEY_RESTART_INTERVAL, restartInterval);
            validRestartInterval = true;
          }
        } catch (NumberFormatException ignored) {}
      }
      if (!validRestartInterval) {
        return WsiteResult.CONFIG_INVALID_RESTART_INTERVAL;
      }

      String usernamePattern = params.get(WsiteConfiguration.KEY_USERNAME_PATTERN).value();
      if (!Utils.isNullOrEmpty(usernamePattern)) {
        try {
          Pattern.compile(usernamePattern);
          config.put(WsiteConfiguration.KEY_USERNAME_PATTERN, usernamePattern);
        } catch (PatternSyntaxException e) {
          return WsiteResult.CONFIG_INVALID_USERNAME_PATTERN;
        }
      }

      String passwordPattern = params.get(WsiteConfiguration.KEY_PASSWORD_PATTERN).value();
      if (!Utils.isNullOrEmpty(passwordPattern)) {
        try {
          Pattern.compile(passwordPattern);
          config.put(WsiteConfiguration.KEY_PASSWORD_PATTERN, passwordPattern);
        } catch (PatternSyntaxException e) {
          return WsiteResult.CONFIG_INVALID_PASSWORD_PATTERN;
        }
      }
      return WsiteResult.SUCCESS;
    }
    public ConfigSitePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> config = new HashMap<>();
      WsiteResult result = getSiteConfig(request, config);
      if (result == WsiteResult.SUCCESS) {
        wsite.restartWithNewConfiguration(config);
      }
      return result;
    }
  }

  public static class ConfigDatabaseGetRoute extends WsiteRoute {
    public ConfigDatabaseGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfig());
      return wsite.parseTemplate("configDatabase.ftlh", dataModel);
    }
  }

  public static class ConfigDatabasePostRoute extends WsiteRoute {
    static WsiteResult getDatabaseConfig(Request request, Map<String, Object> config) {
      QueryParamsMap params = request.queryMap();
      String databaseUrl = params.get(WsiteConfiguration.KEY_DATABASE_URL).value();
      if (!Utils.isNullOrEmpty(databaseUrl)) {
        config.put(WsiteConfiguration.KEY_DATABASE_URL, databaseUrl);
      } else {
        return WsiteResult.CONFIG_NO_DATABASE_URL;
      }

      String databaseUsername = params.get(WsiteConfiguration.KEY_DATABASE_USERNAME).value();
      if (!Utils.isNullOrEmpty(databaseUsername)) {
        config.put(WsiteConfiguration.KEY_DATABASE_USERNAME, databaseUsername);
        String databasePassword = params.get(WsiteConfiguration.KEY_DATABASE_PASSWORD).value();
        if (!Utils.isNullOrEmpty(databasePassword)) {
          config.put(WsiteConfiguration.KEY_DATABASE_PASSWORD, databasePassword);
        }
      }

      String databasePropertiesStr = params.get(WsiteConfiguration.KEY_DATABASE_PROPERTIES).value();
      if (!Utils.isNullOrEmpty(databasePropertiesStr)) {
        config.put(WsiteConfiguration.KEY_DATABASE_PROPERTIES, Utils.parseProperties(databasePropertiesStr));
      }

      return WsiteResult.SUCCESS;
    }
    public ConfigDatabasePostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> config = new HashMap<>();
      WsiteResult result = getDatabaseConfig(request, config);
      if (result == WsiteResult.SUCCESS) {
        wsite.restartWithNewConfiguration(config);
      }
      return result;
    }
  }

  public static class ConfigSslGetRoute extends WsiteRoute {
    public ConfigSslGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfig());
      return wsite.parseTemplate("setupSsl.ftlh", dataModel);
    }
  }

  public static class ConfigSslPostRoute extends WsiteRoute {
    static WsiteResult getSslConfig(WsiteService wsite, Request request, Map<String, Object> config) throws Exception {
      QueryParamsMap params = request.queryMap();

      boolean enableSsl = Optional.ofNullable(params.get(WsiteConfiguration.KEY_ENABLE_SSL).booleanValue()).orElse(false);
      config.put(WsiteConfiguration.KEY_ENABLE_SSL, enableSsl);

      if (enableSsl) {
        Path secureDir = wsite.resolvePath(Reference.SECURE_DIR);
        IOUtils.mkdirs(secureDir);

        Part keystorePart = request.raw().getPart(WsiteConfiguration.KEY_KEYSTORE_FILE);
        if (keystorePart != null) {
          Path keystorePath = secureDir.resolve(keystorePart.getSubmittedFileName());
          wsite.getLogger().warn("Downloading new keystore to {}...", keystorePath);
          try (InputStream in = keystorePart.getInputStream()) {
            Files.copy(in, keystorePath, StandardCopyOption.REPLACE_EXISTING);
          }
          config.put(WsiteConfiguration.KEY_KEYSTORE_FILE, keystorePath.toString());
          String keystorePassword = params.get(WsiteConfiguration.KEY_KEYSTORE_PASSWORD).value();
          if (keystorePassword != null) {
            config.put(WsiteConfiguration.KEY_KEYSTORE_PASSWORD, keystorePassword);
          }
        } else {
          return WsiteResult.CONFIG_NO_KEYSTORE;
        }

        Part truststorePart = request.raw().getPart(WsiteConfiguration.KEY_TRUSTSTORE_FILE);
        if (truststorePart != null) {
          Path truststorePath = secureDir.resolve(truststorePart.getSubmittedFileName());
          wsite.getLogger().warn("Downloading new truststore to {}...", truststorePath);
          try (InputStream in = truststorePart.getInputStream()) {
            Files.copy(in, truststorePath, StandardCopyOption.REPLACE_EXISTING);
          }
          config.put(WsiteConfiguration.KEY_TRUSTSTORE_FILE, truststorePath.toString());
          String truststorePassword = params.get(WsiteConfiguration.KEY_TRUSTSTORE_PASSWORD).value();
          if (truststorePassword != null) {
            config.put(WsiteConfiguration.KEY_TRUSTSTORE_PASSWORD, truststorePassword);
          }
        }
      }

      return WsiteResult.SUCCESS;
    }
    public ConfigSslPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      setupMultipartConfig(wsite, request);
      checkUserPermission(wsite, request);
      Map<String, Object> config = new HashMap<>();
      WsiteResult result = getSslConfig(wsite, request, config);
      if (result == WsiteResult.SUCCESS) {
        wsite.restartWithNewConfiguration(config);
      }
      return result;
    }
  }

  public static class ConfigSmtpGetRoute extends WsiteRoute {
    public ConfigSmtpGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfig());
      return wsite.parseTemplate("configSmtp.ftlh", dataModel);
    }
  }

  public static class ConfigSmtpPostRoute extends WsiteRoute {
    static WsiteResult getSmtpConfig(Request request, Map<String, Object> config) {
      QueryParamsMap params = request.queryMap();

      boolean enableSmtp = Optional.ofNullable(params.get(WsiteConfiguration.KEY_ENABLE_SMTP).booleanValue()).orElse(false);
      config.put(WsiteConfiguration.KEY_ENABLE_SMTP, enableSmtp);

      if (enableSmtp) {
        String smtpHost = params.get(WsiteConfiguration.KEY_SMTP_HOST).value();
        if (!Utils.isNullOrEmpty(smtpHost)) {
          config.put(WsiteConfiguration.KEY_SMTP_HOST, smtpHost);
        } else {
          return WsiteResult.CONFIG_NO_SMTP_HOST;
        }

        String smtpFrom = params.get(WsiteConfiguration.KEY_SMTP_FROM).value();
        if (!Utils.isNullOrEmpty(smtpFrom)) {
          config.put(WsiteConfiguration.KEY_SMTP_FROM, smtpFrom);
        } else {
          return WsiteResult.CONFIG_NO_SMTP_FROM;
        }

        String smtpUser = params.get(WsiteConfiguration.KEY_SMTP_USER).value();
        if (!Utils.isNullOrEmpty(smtpUser)) {
          config.put(WsiteConfiguration.KEY_SMTP_USER, smtpUser);
        }

        String smtpPassword = params.get(WsiteConfiguration.KEY_SMTP_PASSWORD).value();
        if (smtpPassword != null) {
          config.put(WsiteConfiguration.KEY_SMTP_PASSWORD, smtpPassword);
        }
      }

      return WsiteResult.SUCCESS;
    }
    public ConfigSmtpPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      checkUserPermission(wsite, request);
      Map<String, Object> config = new HashMap<>();
      getSmtpConfig(request, config);
      wsite.restartWithNewConfiguration(config);
      return WsiteResult.SUCCESS;
    }
  }

  public static class SetupGetRoute extends WsiteRoute {
    public SetupGetRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      Map<String, Object> dataModel = setupBasicDataModel(wsite);
      dataModel.put(Reference.KEY_CONFIG, wsite.getConfig());
      return wsite.parseTemplate("setup.ftlh", dataModel);
    }
  }

  public static class SetupPostRoute extends WsiteRoute {
    public SetupPostRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
      setupMultipartConfig(wsite, request);
      Map<String, Object> config = new HashMap<>();

      WsiteResult siteCfgResult = ConfigSitePostRoute.getSiteConfig(request, config);
      if (siteCfgResult != WsiteResult.SUCCESS) {
        return siteCfgResult;
      }

      WsiteResult dbCfgResult = ConfigDatabasePostRoute.getDatabaseConfig(request, config);
      if (dbCfgResult != WsiteResult.SUCCESS) {
        return dbCfgResult;
      }

      WsiteResult sslCfgResult = ConfigSslPostRoute.getSslConfig(wsite, request, config);
      if (sslCfgResult != WsiteResult.SUCCESS) {
        return sslCfgResult;
      }

      WsiteResult smtpCfgResult = ConfigSmtpPostRoute.getSmtpConfig(request, config);
      if (smtpCfgResult != WsiteResult.SUCCESS) {
        return smtpCfgResult;
      }

      WsiteResult newUserResult = NewUserPostRoute.createNewUser(wsite, request);
      if (newUserResult != WsiteResult.SUCCESS) {
        return newUserResult;
      }

      wsite.restartWithNewConfiguration(config);
      return WsiteResult.SUCCESS;
    }
  }

  public static class TeapotRoute extends WsiteRoute {
    public TeapotRoute(WsiteService wsite) {
      super(wsite);
    }
    @Override
    protected Object handle_do(Request request, Response response) throws Exception {
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
