package me.whizvox.wsite.core;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import me.whizvox.wsite.database.*;
import me.whizvox.wsite.event.EventListener;
import me.whizvox.wsite.event.EventManager;
import me.whizvox.wsite.hash.HashManager;
import me.whizvox.wsite.util.IOUtils;
import me.whizvox.wsite.util.JsonUtils;
import me.whizvox.wsite.util.Utils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Spark;
import spark.TemplateEngine;
import spark.template.freemarker.FreeMarkerEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class WsiteService implements Runnable {

  private WsiteConfiguration config;
  private Map<String, Object> newConfig;
  private Logger logger;
  private boolean created;
  private Path rootDir;
  private Connection conn;
  private DSLContext dslContext;
  private EventManager eventManager;
  private HashManager hashManager;
  private PageRepository pageRepo;
  private UserRepository userRepo;
  private LoginRepository loginRepo;
  private TemplateEngine templateEngine;

  private Pattern usernamePattern;
  private Pattern passwordPattern;

  // TODO: Include SMTP support
  // TODO: Include SSL support

  private boolean shouldRestart;
  private boolean shouldShutdown;

  private ScheduledExecutorService scheduledExecutorService;

  private WsiteService() {
    created = false;
    shouldRestart = false;
    shouldShutdown = false;
    eventManager = new EventManager();
    newConfig = new HashMap<>();
  }

  public String getSiteName() {
    return config.siteName;
  }

  public Logger getLogger() {
    return logger;
  }

  public Map<String, Object> getConfigValues(String filter) {
    Map<String, Object> values = new HashMap<>();
    if (filter != null) {
      switch (filter.toLowerCase()) {
        case "all":
          config.getAllValues(values);
          break;
        case "site":
          config.getSiteValues(values);
          break;
        case "database":
          config.getDatabaseValues(values);
          break;
        case "ssl":
          config.getSslValues(values);
          break;
        case "smtp":
          config.getSmtpValues(values);
          break;
      }
    }
    return values;
  }

  public Path resolvePath(String path) {
    return rootDir.resolve(path);
  }

  public <T> void registerEventListener(Class<T> eventClass, EventListener<T> listener) {
    logger.info("A new event listener for {} has been registered", eventClass.toString());
    eventManager.registerListener(listener, eventClass);
  }

  public void postEvent(Object event) {
    eventManager.post(event);
  }

  // FIXME: This is kind of a hacky way to allow the setup route to create a new user without changing the internal
  // username and password patterns
  public WsiteResult createNewUser(String username, String emailAddress, char[] password, boolean operator,
                                   Pattern usernameRequirements, Pattern passwordRequirements) {
    if (username == null || !usernameRequirements.matcher(username).matches()) {
      return WsiteResult.USER_INVALID_USERNAME;
    }
    if (emailAddress == null || !Utils.checkEmailAddress(emailAddress)) {
      return WsiteResult.USER_INVALID_EMAIL_ADDRESS;
    }
    User uCheck = userRepo.selectFromUsername(username);
    if (uCheck != null) {
      return WsiteResult.USER_USERNAME_CONFLICT;
    }
    uCheck = userRepo.selectFromEmailAddress(emailAddress);
    if (uCheck != null) {
      return WsiteResult.USER_EMAIL_ADDRESS_CONFLICT;
    }
    if (!passwordRequirements.matcher(new String(password)).matches()) {
      return WsiteResult.USER_INVALID_PASSWORD;
    }

    User user = new User();
    user.id = UUID.randomUUID();
    user.username = username;
    user.emailAddress = emailAddress;
    user.password = hashManager.generate(password).compileAsHexString();
    user.whenCreated = Instant.now();
    user.operator = operator;
    logger.info("Creating new user with user id {} and username {}", user.id, user.username);
    userRepo.insert(user);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult createNewUser(String username, String emailAddress, char[] password, boolean operator) {
    return createNewUser(username, emailAddress, password, operator, usernamePattern, passwordPattern);
  }

  public WsiteResult updateUserUsername(UUID id, String username) {
    User user = userRepo.selectFromId(id);
    if (user == null) {
      return WsiteResult.USER_ID_NOT_FOUND;
    }
    if (username == null || !Utils.checkUsername(username)) {
      return WsiteResult.USER_INVALID_USERNAME;
    }
    User uCheck = userRepo.selectFromUsername(username);
    if (uCheck != null) {
      return WsiteResult.USER_USERNAME_CONFLICT;
    }
    if (user.username.equals(username)) {
      return WsiteResult.USER_USERNAME_NOT_CHANGED;
    }
    logger.info("Updating username of user id {} and username {} to {}", user.id, user.username, username);
    user.username = username;
    userRepo.update(user);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult updateUserEmailAddress(UUID id, String emailAddress) {
    User user = userRepo.selectFromId(id);
    if (user == null) {
      return WsiteResult.USER_ID_NOT_FOUND;
    }
    if (emailAddress == null || !Utils.checkEmailAddress(emailAddress)) {
      return WsiteResult.USER_INVALID_EMAIL_ADDRESS;
    }
    User uCheck = userRepo.selectFromEmailAddress(emailAddress);
    if (uCheck != null) {
      return WsiteResult.USER_EMAIL_ADDRESS_CONFLICT;
    }
    if (user.emailAddress.equals(emailAddress)) {
      return WsiteResult.USER_EMAIL_ADDRESS_NOT_CHANGED;
    }
    logger.info("Updating email address of user id {} and username {} to {} from {}",
        user.id, user.username, emailAddress, user.emailAddress);
    user.emailAddress = emailAddress;
    userRepo.update(user);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult updateUserPassword(UUID id, char[] password) {
    User user = userRepo.selectFromId(id);
    if (user == null) {
      return WsiteResult.USER_ID_NOT_FOUND;
    }
    if (!passwordPattern.matcher(new String(password)).matches()) {
      return WsiteResult.USER_INVALID_PASSWORD;
    }
    // checking a password attempt clears the array. create a temporary one in case the passwords don't match
    char[] temp = Arrays.copyOf(password, password.length);
    if (hashManager.check(password, user.password)) {
      Arrays.fill(temp, ' ');
      return WsiteResult.USER_PASSWORD_NOT_CHANGED;
    }
    user.password = hashManager.generate(temp).compileAsHexString();
    logger.info("Updating password of user id {} and username {}", user.id, user.username);
    userRepo.update(user);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult updateUserOperator(UUID id, boolean operator) {
    User user = userRepo.selectFromId(id);
    if (user == null) {
      return WsiteResult.USER_ID_NOT_FOUND;
    }
    if (user.operator == operator) {
      return WsiteResult.USER_OPERATOR_NOT_CHANGED;
    }
    user.operator = operator;
    logger.info("Updating user's operator status to {} with id {} and username {}",
        user.operator, user.id, user.username);
    userRepo.update(user);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult deleteUser(UUID id) {
    User user = userRepo.selectFromId(id);
    if (user == null) {
      return WsiteResult.USER_ID_NOT_FOUND;
    }
    logger.info("Deleting user with id {} username {}", user.id, user.username);
    userRepo.delete(id);
    return WsiteResult.SUCCESS;
  }

  public User getUserFromId(UUID id) {
    return userRepo.selectFromId(id);
  }

  public User getUserFromUsername(String username) {
    return userRepo.selectFromUsername(username);
  }

  public User getUserFromEmailAddress(String emailAddress) {
    return userRepo.selectFromEmailAddress(emailAddress);
  }

  public int getNumberOfUsers() {
    return userRepo.selectNumberOfUsers();
  }

  public WsiteResult createLogin(String query, char[] password, long minutesUntilExpire, String userAgent,
                                 String ipAddress, CharBuffer tokenBuffer) {
    User user;
    if (query != null && Utils.checkUsername(query)) {
      user = userRepo.selectFromUsername(query);
    } else if (query != null && Utils.checkEmailAddress(query)) {
      user = userRepo.selectFromEmailAddress(query);
    } else {
      return WsiteResult.LOGIN_INVALID_QUERY;
    }
    if (user == null) {
      return WsiteResult.LOGIN_QUERY_NOT_FOUND;
    }
    if (minutesUntilExpire <= 0) {
      minutesUntilExpire = Reference.LOGIN_DEFAULT_EXPIRATION;
    }
    if (minutesUntilExpire < 10 || minutesUntilExpire > 524160) {
      return WsiteResult.LOGIN_INVALID_EXPIRATION;
    }
    if (userAgent == null) {
      userAgent = "";
    }
    if (Utils.isNullOrEmpty(ipAddress)) {
      return WsiteResult.LOGIN_INVALID_IP_ADDRESS;
    }
    if (!hashManager.check(password, user.password)) {
      return WsiteResult.LOGIN_INCORRECT_PASSWORD;
    }
    Login lCheck = loginRepo.selectFromClientInfo(user.id, userAgent, ipAddress);
    if (lCheck != null) {
      loginRepo.delete(lCheck.token);
    }
    Login login = new Login();
    login.token = Utils.hexStringFromBytes(hashManager.fillBytes(new byte[Reference.LOGIN_TOKEN_LENGTH / 2]));
    login.userId = user.id;
    login.userAgent = userAgent;
    login.ipAddress = ipAddress;
    login.expirationDate = Instant.now().plusSeconds(minutesUntilExpire * 60);
    logger.info("Creating new login token for user {}, user agent {}, and ip {}",
        login.userId, login.userAgent, login.ipAddress);
    loginRepo.insert(login);
    tokenBuffer.put(login.token);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult deleteLogin(String token) {
    Login login = loginRepo.selectFromToken(token);
    if (login == null) {
      return WsiteResult.LOGIN_TOKEN_NOT_FOUND;
    }
    logger.info("Deleting login token from user {}, user agent {}, and ip {}",
        login.userId, login.userAgent, login.ipAddress);
    loginRepo.delete(token);
    return WsiteResult.SUCCESS;
  }

  public User getUserFromLoginToken(String token) {
    Login login = loginRepo.selectFromToken(token);
    if (login != null) {
      return userRepo.selectFromId(login.userId);
    }
    return null;
  }

  private Page preparePage(Page page) {
    if (page.title == null) {
      page.title = "";
    }
    if (page.contents == null) {
      page.contents = "";
    }
    if (page.syntax == null) {
      page.syntax = Page.Syntax.PLAIN;
    }
    return page;
  }

  public WsiteResult createNewPage(Page page) {
    if (page.path == null) {
      return WsiteResult.PAGE_INVALID_PATH;
    }
    Page pePage = pageRepo.selectFromPath(page.path);
    if (pePage != null) {
      return WsiteResult.PAGE_PATH_CONFLICT;
    }
    page.published = Instant.now();
    page.lastEdited = null;
    logger.info("Creating new page {}...", page.path);
    pageRepo.insert(preparePage(page));
    return WsiteResult.SUCCESS;
  }

  public WsiteResult createNewPage(String path, String title, String syntax, String contents) {
    return createNewPage(new Page(path, title, contents, Page.Syntax.fromString(syntax), null, null));
  }

  public WsiteResult updatePage(String origPath, Page page) {
    if (origPath == null || page.path == null) {
      return WsiteResult.PAGE_INVALID_PATH;
    }
    Page checkPage = pageRepo.selectFromPath(page.path);
    if (checkPage != null) {
      return WsiteResult.PAGE_PATH_CONFLICT;
    }
    Page oldPage = pageRepo.selectFromPath(origPath);
    if (oldPage == null) {
      return WsiteResult.PAGE_PATH_NOT_FOUND;
    }
    Page newPage = preparePage(page);
    if (!page.path.equalsIgnoreCase(origPath)) {
      logger.info("Updating page {} to {}...", origPath, newPage.path);
      pageRepo.delete(origPath);
      pageRepo.insert(newPage);
    } else {
      logger.info("Updating page {}...", page.path);
      pageRepo.update(newPage);
    }
    return WsiteResult.SUCCESS;
  }

  public WsiteResult updatePage(String origPath, String newPath, String title, String syntax, String contents) {
    return updatePage(origPath, new Page(newPath, title, contents, Page.Syntax.fromString(syntax), null, null));
  }

  public WsiteResult deletePage(String path) {
    Page page = pageRepo.selectFromPath(path);
    if (page == null) {
      return WsiteResult.PAGE_PATH_NOT_FOUND;
    }
    logger.info("Deleting page {}...", path);
    pageRepo.delete(page.path);
    return WsiteResult.SUCCESS;
  }

  public Page getPage(String path) {
    return pageRepo.selectFromPath(path);
  }

  public WsiteResult uploadAsset(String path, InputStream input, boolean replaceExisting) throws IOException {
    if (Utils.isNullOrEmpty(path)) {
      return WsiteResult.ASSET_NO_PATH;
    }
    Path outputFile = resolvePath(Reference.ASSETS_DIR).resolve(path);
    if (!replaceExisting && Files.exists(outputFile)) {
      return WsiteResult.ASSET_FILE_ALREADY_EXISTS;
    }
    logger.info("Creating new asset at {}...", path);
    IOUtils.mkdirs(outputFile.getParent());
    if (input == null) {
      IOUtils.touch(outputFile);
    } else {
      Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING);
    }
    return WsiteResult.SUCCESS;
  }

  public WsiteResult deleteAsset(String path) throws IOException {
    if (Utils.isNullOrEmpty(path)) {
      return WsiteResult.ASSET_NO_PATH;
    }
    Path assetFile = resolvePath(Reference.ASSETS_DIR).resolve(path);
    if (!Files.exists(assetFile)) {
      return WsiteResult.ASSET_NOT_FOUND;
    }
    if (!Files.isRegularFile(assetFile)) {
      return WsiteResult.ASSET_CANNOT_DELETE_NONFILE;
    }
    logger.info("Deleting asset {}...", path);
    Files.delete(assetFile);
    return WsiteResult.SUCCESS;
  }

  public String parseTemplate(String templateName, Object dataModel) {
    return templateEngine.render(new ModelAndView(dataModel, templateName));
  }

  public boolean hasBeenCreated() {
    return created;
  }

  public void create() throws Exception {
    if (created) {
      throw new RuntimeException("Wsite service has already been created");
    }

    logger.info("Creating Wsite service...");
    if (Reference.usingDevBuild()) {
      logger.warn("You are currently using a development build of Wsite. Use at your own risk!");
    } else {
      logger.warn("Running Wsite version {}", Reference.VERSION);
    }
    logger.info("Build released {}", Reference.RELEASED.atZone(ZoneOffset.UTC));

    if (!Files.exists(rootDir)) {
      logger.warn("Creating root directory...");
      Files.createDirectories(rootDir);
    }
    IOUtils.mkdirs(resolvePath(Reference.TEMP_DIR));

    Path configFile = resolvePath(Reference.CONFIG_FILE);
    if (Files.exists(configFile)) {
      try (InputStream in = Files.newInputStream(configFile)) {
        config = JsonUtils.readJson(in, WsiteConfiguration.class);
      }
    }

    if (!newConfig.isEmpty()) {
      logger.warn("A new configuration has been specified");
      config.loadFromMap(newConfig);
      newConfig.clear();
    }

    logger.info("Connecting to SQL database...");
    if (Utils.isNullOrEmpty(config.databaseUrl)) {
      throw new IllegalArgumentException("Database URL must be specified");
    }
    String resolvedDatabaseUrl = config.databaseUrl.replace("${ROOT}", rootDir.toString());
    if (!resolvedDatabaseUrl.equals(config.databaseUrl)) {
      logger.info("Database URL path has been resolved");
    }
    if (config.databaseProperties != null) {
      conn = DriverManager.getConnection(resolvedDatabaseUrl, config.databaseProperties);
    } else if (config.databaseUsername != null && config.databasePassword != null) {
      conn = DriverManager.getConnection(resolvedDatabaseUrl, config.databaseUsername, config.databasePassword);
    } else {
      conn = DriverManager.getConnection(resolvedDatabaseUrl);
    }
    dslContext = DSL.using(conn);

    logger.info("Initializing repositories...");
    userRepo = new UserRepository(dslContext);
    pageRepo = new PageRepository(dslContext);
    loginRepo = new LoginRepository(dslContext);
    userRepo.create();
    pageRepo.create();
    loginRepo.create();

    logger.info("Compiling username and password requirement patterns...");
    usernamePattern = Pattern.compile(config.usernamePattern);
    passwordPattern = Pattern.compile(config.passwordPattern);

    logger.info("Initializing scheduled executor service...");
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    if (config.continuouslyRestart) {
      logger.info("Scheduling continuous restart task...");
      LocalDateTime midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).plusDays(1);
      long untilRestart = LocalDateTime.now().until(midnight, ChronoUnit.MINUTES);
      scheduledExecutorService.scheduleWithFixedDelay(
          this::restart, untilRestart , config.restartInterval, TimeUnit.MINUTES
      );
      logger.info("Scheduled to continuously restart every {} hours and {} minutes",
          config.restartInterval / 60, config.restartInterval % 60);
      logger.info("Scheduled to restart in {} hours and {} minutes", untilRestart / 60, untilRestart % 60);
    } else {
      logger.info("Continuous restarting has been disabled");
    }
    logger.info("Scheduling continuous login cleanup task...");
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      int changed = loginRepo.deleteAllExpired();
      if (changed > 0) {
        logger.warn("Deleted {} expired login credentials", changed);
      }
    }, 0, 1, TimeUnit.MINUTES);
    logger.info("Login cleanup will commence every 1 minute");

    Path staticDir = resolvePath(Reference.STATIC_DIR);
    logger.info("Copying internal resources...");
    Path staticAssetsDir = resolvePath(Reference.ASSETS_DIR);
    IOUtils.copyFromResource("css/main.css", staticAssetsDir.resolve("css/main.css"), false);
    IOUtils.copyFromResource("css/normalize.css", staticAssetsDir.resolve("css/normalize.css"), false);
    IOUtils.copyFromResource("scripts/wsite.js", staticAssetsDir.resolve("scripts/wsite.js"), false);
    IOUtils.copyFromResource("scripts/cookies.min.js", staticAssetsDir.resolve("scripts/cookies.min.js"), false);

    logger.info("Configuring Spark service...");
    IOUtils.mkdirs(staticDir);
    Spark.staticFiles.externalLocation(staticDir.toString());
    logger.info("Setting port to {}...", config.port);
    Spark.port(config.port);

    logger.info("Initializing Spark service...");
    Spark.init();
    Spark.awaitInitialization();

    Spark.notFound(Routes.generateHaltBody(this, 404));
    Spark.internalServerError(Routes.generateHaltBody(this, 500));
    Spark.before("/control/*", new Routes.UserFilter(this));
    Spark.get("/control/shutdown", new Routes.ShutdownRoute(this));
    Spark.get("/control/restart", new Routes.RestartRoute(this));
    Spark.get("/login", Routes.getTemplatedRoute(this, "login.ftlh", false));
    Spark.post("/login", new Routes.LoginPostRoute(this));
    Spark.get("/logout", new Routes.LogoutGetRoute(this));
    Spark.post("/control/config", new Routes.ConfigPostRoute(this));

    if (getNumberOfUsers() > 0) {
      logger.info("Adding API routes...");
      Spark.post("/api/asset/upload", new ApiRoutes.UploadAssetRoute(this));
      Spark.post("/api/asset/delete", new ApiRoutes.DeleteAssetRoute(this));
      Spark.get("/api/user/fetch", new ApiRoutes.UserGetRoute(this));
      Spark.post("/api/user/create", new ApiRoutes.UserCreateRoute(this));
      Spark.post("/api/user/updateUsername", new ApiRoutes.UserUpdateUsernameRoute(this));
      Spark.post("/api/user/updateEmail", new ApiRoutes.UserUpdateEmailAddressRoute(this));
      Spark.post("/api/user/updatePassword", new ApiRoutes.UserUpdatePasswordRoute(this));
      Spark.post("/api/user/updateOperator", new ApiRoutes.UserUpdateOperatorRoute(this));
      Spark.post("/api/user/delete", new ApiRoutes.UserDeleteRoute(this));
      Spark.get("/api/page/fetch", new ApiRoutes.PageGetRoute(this));
      Spark.post("/api/page/create", new ApiRoutes.PageCreateRoute(this));
      Spark.post("/api/page/update", new ApiRoutes.PageUpdateRoute(this));
      Spark.post("/api/page/delete", new ApiRoutes.PageDeleteRoute(this));
      Spark.post("/api/login/create", new ApiRoutes.LoginCreateRoute(this));
      Spark.post("/api/login/delete", new ApiRoutes.LoginDeleteRoute(this));
      Spark.get("/api/config/fetch", new ApiRoutes.ConfigGetRoute(this));
      Spark.post("/api/config/update", new ApiRoutes.ConfigUpdateRoute(this));

      logger.info("Adding standard routes...");
      Spark.get("/control/uploadAsset", Routes.getTemplatedRoute(this, "uploadAsset.ftlh", true));
      Spark.post("/control/uploadAsset", new Routes.UploadAssetPostRoute(this));
      Spark.get("/control/deleteAsset", Routes.getTemplatedRoute(this, "deleteAsset.ftlh", true));
      Spark.post("/control/deleteAsset", new Routes.DeleteAssetPostRoute(this));
      Spark.get("/control/createPage", Routes.getTemplatedRoute(this, "newPage.ftlh", true));
      Spark.post("/control/createPage", new Routes.NewPagePostRoute(this));
      Spark.get("/control/deletePage", Routes.getTemplatedRoute(this, "deletePage.ftlh", true));
      Spark.post("/control/deletePage", new Routes.DeletePagePostRoute(this));
      Spark.get("/control/createUser", Routes.getTemplatedRoute(this, "newUser.ftlh", true));
      Spark.post("/control/createUser", new Routes.NewUserPostRoute(this));
      Spark.get("/control/deleteUser", Routes.getTemplatedRoute(this, "deleteUser.ftlh", true));
      Spark.post("/control/deleteUser", new Routes.DeleteUserPostRoute(this));
      Spark.get("/control/configSite", Routes.getTemplatedRoute(this, "configSite.ftlh", true));
      Spark.get("/control/configDatabase", Routes.getTemplatedRoute(this, "configDatabase.ftlh", true));
      Spark.get("/control/configSsl", Routes.getTemplatedRoute(this, "configSsl.ftlh", true));
      Spark.get("/control/configSmtp", Routes.getTemplatedRoute(this, "configSmtp.ftlh", true));
      Spark.get("/veryimportant/teapot", new Routes.TeapotRoute(this));
      Spark.get("/:path", new Routes.PageGetRoute(this));
      Spark.get("/", new Routes.IndexPageRoute(this, config.indexPage));
    } else {
      // TODO: Include some sort of system where a setup file can instead be used
      logger.warn("No users found. Will instead add setup route...");
      Spark.get("/", new Routes.SetupGetRoute(this));
      Spark.post("/", new Routes.SetupPostRoute(this));
    }

    save();

    created = true;
    postEvent(new WsiteEvent.Create(this));

    logger.info("Wsite service has successfully been created");
  }

  public void save() throws IOException {
    logger.info("Saving settings...");
    Path configFile = resolvePath(Reference.CONFIG_FILE);
    try (OutputStream out = Files.newOutputStream(configFile)) {
      JsonUtils.writeJson(out, config);
    }
  }

  public void tick() {
    eventManager.tick();
  }

  private void destroy_do() throws Exception {
    logger.warn("Destroying Wsite service...");

    logger.info("Stopping Spark service...");
    Spark.stop();
    Spark.awaitStop();

    if (scheduledExecutorService != null) {
      logger.info("Shutting down scheduled executor service...");
      scheduledExecutorService.shutdownNow();
      scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
      scheduledExecutorService = null;
    }

    if (eventManager != null) {
      logger.info("Clearing event manager...");
      eventManager.dropAllListeners();
    }

    logger.info("Saving settings...");
    save();

    if (conn != null) {
      logger.info("Closing repositories...");
      conn.close();
      conn = null;
      dslContext = null;
      // TODO: Maybe give an option to drop the databases when destroying?
      userRepo = null;
      pageRepo = null;
      loginRepo = null;
    }

    created = false;
    postEvent(new WsiteEvent.Shutdown(this));

    logger.warn("Wsite service has successfully been destroyed");
  }

  public void destroy() throws Exception {
    if (!created) {
      throw new RuntimeException("Wsite service has not yet been created");
    }
    destroy_do();
  }

  public void restart() {
    scheduledExecutorService.schedule(() -> shouldRestart = true, 1, TimeUnit.SECONDS);
  }

  public void restartWithNewConfiguration(Map<String, Object> config) {
    newConfig.putAll(config);
    restart();
  }

  public void shutdown() {
    scheduledExecutorService.schedule(() -> shouldShutdown = true, 1, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    boolean run = false;
    try {
      create();
      run = true;
    } catch (Exception e) {
      logger.error("Could not create Wsite service", e);
    }
    while (run) {
      tick();
      try {
        Thread.sleep(Reference.TICK_DELAY);
      } catch (InterruptedException e) {
        logger.error("Tick delay was interrupted", e);
      }
      if (shouldRestart) {
        try {
          logger.warn("Wsite service is now restarting...");
          postEvent(new WsiteEvent.Restarting(this));
          destroy();
          create();
          logger.warn("Wsite service has successfully been restarted");
        } catch (Exception e) {
          logger.error("Could not restart Wsite service", e);
          run = false;
        }
        shouldRestart = false;
      }
      if (shouldShutdown) {
        run = false;
      }
    }
    try {
      destroy_do();
    } catch (Exception e) {
      throw new RuntimeException("Could not shutdown Wsite service", e);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    public WsiteConfiguration config;
    public Path rootDirectory;
    public Logger logger;
    public HashManager hashManager;
    public TemplateEngine templateEngine;

    public Builder() {
      config = null;
      logger = null;
      hashManager = null;
      templateEngine = null;
    }

    public Builder setConfig(WsiteConfiguration config) {
      this.config = config;
      return this;
    }

    public Builder setRootDirectory(Path rootDirectory) {
      this.rootDirectory = rootDirectory;
      return this;
    }

    public Builder setLogger(Logger logger) {
      this.logger = logger;
      return this;
    }

    public Builder setHashManager(HashManager hashManager) {
      this.hashManager = hashManager;
      return this;
    }

    public Builder setTemplateEngine(TemplateEngine templateEngine) {
      this.templateEngine = templateEngine;
      return this;
    }

    public WsiteService build() throws SQLException {
      if (config == null) {
        config = new WsiteConfiguration();
      }
      if (logger == null) {
        logger = LoggerFactory.getLogger(config.siteName);
      }
      if (hashManager == null) {
        hashManager = HashManager.builder().build();
      }
      if (templateEngine == null) {
        Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_26);
        freemarkerConfig.setTemplateLoader(
            new ClassTemplateLoader(WsiteService.class.getClassLoader(), Reference.TEMPLATES_DIR)
        );
        freemarkerConfig.setLocalizedLookup(false);
        templateEngine = new FreeMarkerEngine(freemarkerConfig);
      }

      WsiteService service = new WsiteService();
      service.config = config;
      service.logger = logger;
      service.rootDir = rootDirectory.toAbsolutePath().normalize();
      service.hashManager = hashManager;
      service.templateEngine = templateEngine;
      return service;
    }
  }

}
