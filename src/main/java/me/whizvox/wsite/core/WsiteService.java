package me.whizvox.wsite.core;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import me.whizvox.wsite.database.*;
import me.whizvox.wsite.event.EventListener;
import me.whizvox.wsite.event.EventManager;
import me.whizvox.wsite.hash.HashManager;
import me.whizvox.wsite.util.IOUtils;
import me.whizvox.wsite.util.Utils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.*;
import spark.template.freemarker.FreeMarkerEngine;

import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class WsiteService implements Runnable {

  private WsiteConfiguration config;
  private WsiteConfiguration newConfig;
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

  private boolean shouldRestart;
  private boolean shouldShutdown;

  private ScheduledExecutorService scheduledExecutorService;

  private WsiteService() {
    created = false;
    shouldRestart = false;
    shouldShutdown = false;
  }

  public String getSiteName() {
    return config.siteName;
  }

  public Logger getLogger() {
    return logger;
  }

  public Path resolvePath(String path) {
    return rootDir.resolve(path);
  }

  public <T> void registerEventListener(Class<T> eventClass, EventListener<T> listener) {
    eventManager.registerListener(listener, eventClass);
  }

  public void postEvent(Object event) {
    eventManager.post(event);
  }

  // FIXME: This is kind of a hacky way to allow the setup route to create a new user without changing the internal username and password patterns
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
    userRepo.update(user);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult deleteUser(UUID id) {
    User user = userRepo.selectFromId(id);
    if (user == null) {
      return WsiteResult.USER_ID_NOT_FOUND;
    }
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

  public WsiteResult createLogin(String query, char[] password, long minutesUntilExpire, String userAgent, String ipAddress, CharBuffer tokenBuffer) {
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
    loginRepo.insert(login);
    tokenBuffer.put(login.token);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult deleteLogin(String token) {
    Login login = loginRepo.selectFromToken(token);
    if (login == null) {
      return WsiteResult.LOGIN_TOKEN_NOT_FOUND;
    }
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

  public WsiteResult createNewPage(String path, String title, String syntax, String contents) {
    if (path == null) {
      return WsiteResult.PAGE_INVALID_PATH;
    }
    Page pePage = pageRepo.selectFromPath(path);
    if (pePage != null) {
      return WsiteResult.PAGE_PATH_CONFLICT;
    }
    Page page = preparePage(new Page(path, title, contents, Page.Syntax.fromString(syntax), Instant.now(), null));
    pageRepo.insert(page);
    return WsiteResult.SUCCESS;
  }

  public WsiteResult updatePage(String oldPath, String newPath, String title, String syntax, String contents) {
    if (oldPath == null || newPath == null) {
      return WsiteResult.PAGE_INVALID_PATH;
    }
    Page checkPage = pageRepo.selectFromPath(newPath);
    if (checkPage != null) {
      return WsiteResult.PAGE_PATH_CONFLICT;
    }
    Page oldPage = pageRepo.selectFromPath(oldPath);
    if (oldPage == null) {
      return WsiteResult.PAGE_PATH_NOT_FOUND;
    }
    Page newPage = preparePage(
        new Page(newPath, title, contents, Page.Syntax.fromString(syntax), oldPage.published, Instant.now())
    );
    if (!newPath.equalsIgnoreCase(oldPath)) {
      pageRepo.delete(oldPath);
      pageRepo.insert(newPage);
    } else {
      pageRepo.update(newPage);
    }
    return WsiteResult.SUCCESS;
  }

  public WsiteResult deletePage(String path) {
    Page page = pageRepo.selectFromPath(path);
    if (page == null) {
      return WsiteResult.PAGE_PATH_NOT_FOUND;
    }
    pageRepo.delete(page.path);
    return WsiteResult.SUCCESS;
  }

  public Page getPage(String path) {
    return pageRepo.selectFromPath(path);
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
    logger.warn("Build released {}", Utils.formatFileSafeString(Reference.RELEASED));

    if (newConfig != null) {
      logger.warn("A new configuration has been specified");

      Path newRootDir = Paths.get(newConfig.rootDirectory);
      if (!Files.isSameFile(rootDir, newRootDir)) {
        logger.warn("A new root directory has been specified ({}). Will move contents...", newRootDir.toString());
        Files.move(rootDir, newRootDir);
      }

      config = newConfig;
      newConfig = null;
    }

    rootDir = Paths.get(config.rootDirectory).toAbsolutePath().normalize();
    if (Reference.usingDevBuild()) {
      rootDir = rootDir.resolve("rundir");
    }
    Files.createDirectories(rootDir);

    logger.info("Connecting to SQL database...");
    if (Utils.isNullOrEmpty(config.databaseUrl)) {
      throw new IllegalArgumentException("Database URL must be specified");
    }
    String oldUrl = config.databaseUrl;
    config.databaseUrl = config.databaseUrl.replace("${ROOT}", rootDir.toString());
    if (!oldUrl.equals(config.databaseUrl)) {
      logger.info("Database URL path has been resolved");
    }
    if (config.databaseProperties != null) {
      conn = DriverManager.getConnection(config.databaseUrl, config.databaseProperties);
    } else if (config.databaseUsername != null && config.databasePassword != null) {
      conn = DriverManager.getConnection(config.databaseUrl, config.databaseUsername, config.databasePassword);
    } else {
      conn = DriverManager.getConnection(config.databaseUrl);
    }
    dslContext = DSL.using(conn);

    logger.info("Initializing repositories...");
    userRepo = new UserRepository(dslContext);
    pageRepo = new PageRepository(dslContext);
    loginRepo = new LoginRepository(dslContext);
    userRepo.create();
    pageRepo.create();
    loginRepo.create();

    logger.info("Initializing scheduled executor service...");
    scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    if (config.continuouslyRestart) {
      logger.info("Scheduling continuous restart task...");
      LocalDateTime midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT).plusDays(1);
      long untilRestart = LocalDateTime.now().until(midnight, ChronoUnit.MINUTES);
      scheduledExecutorService.scheduleWithFixedDelay(this::restart, untilRestart , config.restartInterval, TimeUnit.MINUTES);
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

    Path staticDir = resolvePath("static");
    logger.info("Copying internal resources...");
    Path staticCssDir = staticDir.resolve("assets/css");
    IOUtils.mkdirs(staticCssDir);
    IOUtils.copyFromResource("css/main.css", staticCssDir.resolve("main.css"), false);
    IOUtils.copyFromResource("css/normalize.css", staticCssDir.resolve("normalize.css"), false);

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
    Spark.get("/login", new Routes.LoginGetRoute(this));
    Spark.post("/login", new Routes.LoginPostRoute(this));
    Spark.get("/logout", new Routes.LogoutGetRoute(this));

    if (getNumberOfUsers() > 0) {
      logger.info("Adding standard routes...");
      Spark.get("/control/newPage", new Routes.NewPageGetRoute(this));
      Spark.post("/control/newPage", new Routes.NewPagePostRoute(this));
      Spark.get("/control/deletePage", new Routes.DeletePageGetRoute(this));
      Spark.post("/control/deletePage", new Routes.DeletePagePostRoute(this));
      Spark.get("/control/newUser", new Routes.NewUserGetRoute(this));
      Spark.post("/control/newUser", new Routes.NewUserPostRoute(this));
      Spark.get("/control/deleteUser", new Routes.DeleteUserGetRoute(this));
      Spark.post("/control/deleteUser", new Routes.DeleteUserPostRoute(this));
      Spark.get("/veryimportant/teapot", new Routes.TeapotRoute(this));
      Spark.get("/:pagePath", new Routes.PageGetRoute(this));
    } else {
      /*Path initialUserPath = resolvePath("initialUser.json");
      if (!Files.exists(initialUserPath)) {
        throw new FileNotFoundException("Cannot start server without initialUser.json");
      }
      try (InputStream in = Files.newInputStream(initialUserPath)) {
        User user = IOUtils.readJson(in, User.class);
        WsiteResult result = createNewUser(user.username, user.emailAddress, user.password.toCharArray(), true);
        if (result != WsiteResult.SUCCESS) {
          throw new RuntimeException("Could not create initial user: " + result);
        }
      } catch (IOException e) {
        throw new RuntimeException("Could not read from initialUser.json", e);
      }
      Path setupPath = resolvePath("setup.json");
      if (Files.exists(setupPath)) {
        try (InputStream in = Files.newInputStream(setupPath)) {
          WsiteConfiguration setupConfig = IOUtils.readJson(in, WsiteConfiguration.class);
          restartWithNewConfiguration(setupConfig);
        } catch (IOException e) {
          throw new RuntimeException("Could not read setup.json", e);
        }
      } else {*/
        logger.warn("No setup file found. Will instead add setup route...");
        Spark.get("/", new Routes.SetupGetRoute(this));
        Spark.post("/", new Routes.SetupPostRoute(this));
      //}
    }

    created = true;
    postEvent(new WsiteEvent.Create(this));

    logger.info("Wsite service has successfully been created");
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

  public void restartWithNewConfiguration(WsiteConfiguration config) {
    newConfig = config;
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
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.error("Sleep was interrupted", e);
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
        freemarkerConfig.setTemplateLoader(new ClassTemplateLoader(WsiteService.class.getClassLoader(), "templates"));
        freemarkerConfig.setLocalizedLookup(false);
        templateEngine = new FreeMarkerEngine(freemarkerConfig);
      }

      WsiteService service = new WsiteService();
      service.config = config;
      service.logger = logger;
      service.rootDir = Paths.get(config.rootDirectory);
      service.eventManager = new EventManager();
      service.hashManager = hashManager;
      service.templateEngine = templateEngine;
      return service;
    }
  }

}
