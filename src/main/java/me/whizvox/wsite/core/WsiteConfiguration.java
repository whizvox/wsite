package me.whizvox.wsite.core;

import lombok.Getter;
import me.whizvox.wsite.util.Utils;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class WsiteConfiguration implements Cloneable {

  @Getter public String siteName;
  @Getter public int port;
  @Getter public boolean continuouslyRestart;
  @Getter public int restartInterval;
  @Getter public String usernamePattern;
  @Getter public String passwordPattern;
  @Getter public String indexPage;

  @Getter public String databaseUrl;
  @Getter public String databaseUsername;
  @Getter public String databasePassword;
  @Getter public Properties databaseProperties;

  @Getter public boolean enableSsl;
  @Getter public String keystoreFile;
  @Getter public String keystorePassword;
  @Getter public String truststoreFile;
  @Getter public String truststorePassword;

  @Getter public boolean enableSmtp;
  @Getter public String smtpHost;
  @Getter public String smtpFrom;
  @Getter public String smtpUser;
  @Getter public String smtpPassword;

  public WsiteConfiguration() {
    setDefaults();
  }

  public WsiteConfiguration setDefaults() {
    siteName = "Wsite";
    port = 4568;
    continuouslyRestart = true;
    restartInterval = 1440;
    usernamePattern = "^[a-zA-Z0-9_]{1,16}$";
    passwordPattern = ".{7,}";
    indexPage = "home";

    databaseUrl = "jdbc:sqlite:${ROOT}/wsite.db";
    databaseUsername = null;
    databasePassword = "";
    databaseProperties = new Properties();

    enableSsl = false;
    keystoreFile = null;
    keystorePassword = "";
    truststoreFile = null;
    truststorePassword = "";

    enableSmtp = false;
    smtpHost = null;
    smtpFrom = null;
    smtpUser = null;
    smtpPassword = "";
    return this;
  }

  public WsiteConfiguration copy() {
    try {
      return (WsiteConfiguration) clone();
    } catch (CloneNotSupportedException ignored) {}
    return null;
  }

  public void loadFromMap(Map<String, Object> map) {
    Optional.ofNullable(map.get(KEY_SITE_NAME)).ifPresent((siteName) -> this.siteName = (String) siteName);
    Optional.ofNullable(map.get(KEY_PORT)).ifPresent((port) -> this.port = (int) port);
    Optional.ofNullable(map.get(KEY_CONTINUOUSLY_RESTART)).ifPresent((continuouslyRestart) -> this.continuouslyRestart = (boolean) continuouslyRestart);
    Optional.ofNullable(map.get(KEY_RESTART_INTERVAL)).ifPresent((restartInterval) -> this.restartInterval = (int) restartInterval);
    Optional.ofNullable(map.get(KEY_USERNAME_PATTERN)).ifPresent((usernamePattern) -> this.usernamePattern = (String) usernamePattern);
    Optional.ofNullable(map.get(KEY_PASSWORD_PATTERN)).ifPresent((passwordPattern) -> this.passwordPattern = (String) passwordPattern);
    Optional.ofNullable(map.get(KEY_INDEX_PAGE)).ifPresent((indexPage) -> this.indexPage = (String) indexPage);
    Optional.ofNullable(map.get(KEY_DATABASE_URL)).ifPresent((databaseUrl) -> this.databaseUrl = (String) databaseUrl);
    Optional.ofNullable(map.get(KEY_DATABASE_USERNAME)).ifPresent((databaseUsername) -> this.databaseUsername = (String) databaseUsername);
    Optional.ofNullable(map.get(KEY_DATABASE_PASSWORD)).ifPresent((databasePassword) -> this.databasePassword = (String) databasePassword);
    Optional.ofNullable(map.get(KEY_DATABASE_PROPERTIES)).ifPresent((databaseProperties) -> this.databaseProperties = new Properties((Properties) databaseProperties));
    Optional.ofNullable(map.get(KEY_ENABLE_SSL)).ifPresent((enableSsl) -> this.enableSsl = (boolean) enableSsl);
    Optional.ofNullable(map.get(KEY_KEYSTORE_FILE)).ifPresent((keystoreFile) -> this.keystoreFile = (String) keystoreFile);
    Optional.ofNullable(map.get(KEY_KEYSTORE_PASSWORD)).ifPresent((keystorePassword) -> this.keystorePassword = (String) keystorePassword);
    Optional.ofNullable(map.get(KEY_TRUSTSTORE_FILE)).ifPresent((truststoreFile) -> this.truststoreFile = (String) truststoreFile);
    Optional.ofNullable(map.get(KEY_TRUSTSTORE_PASSWORD)).ifPresent((truststorePassword) -> this.truststorePassword = (String) truststorePassword);
    Optional.ofNullable(map.get(KEY_ENABLE_SMTP)).ifPresent((enableSmtp) -> this.enableSmtp = (boolean) enableSmtp);
    Optional.ofNullable(map.get(KEY_SMTP_HOST)).ifPresent((smtpHost) -> this.smtpHost = (String) smtpHost);
    Optional.ofNullable(map.get(KEY_SMTP_FROM)).ifPresent((smtpFrom) -> this.smtpFrom = (String) smtpFrom);
    Optional.ofNullable(map.get(KEY_SMTP_USER)).ifPresent((smtpUser) -> this.smtpUser = (String) smtpUser);
    Optional.ofNullable(map.get(KEY_SMTP_PASSWORD)).ifPresent((smtpPassword) -> this.smtpPassword = (String) smtpPassword);
  }

  private void getValues(Map<String, Object> values, String... keys) {
    for (String key : keys) {
      switch (key) {
        case KEY_SITE_NAME:
          values.put(KEY_SITE_NAME, siteName);
          break;
        case KEY_PORT:
          values.put(KEY_PORT, port);
          break;
        case KEY_CONTINUOUSLY_RESTART:
          values.put(KEY_CONTINUOUSLY_RESTART, continuouslyRestart);
          break;
        case KEY_RESTART_INTERVAL:
          values.put(KEY_RESTART_INTERVAL, restartInterval);
          break;
        case KEY_USERNAME_PATTERN:
          values.put(KEY_USERNAME_PATTERN, usernamePattern);
          break;
        case KEY_PASSWORD_PATTERN:
          values.put(KEY_PASSWORD_PATTERN, passwordPattern);
          break;
        case KEY_INDEX_PAGE:
          values.put(KEY_INDEX_PAGE, indexPage);
        case KEY_DATABASE_URL:
          values.put(KEY_DATABASE_URL, databaseUrl);
          break;
        case KEY_DATABASE_USERNAME:
          values.put(KEY_DATABASE_USERNAME, databaseUsername);
          break;
        case KEY_DATABASE_PASSWORD:
          values.put(KEY_DATABASE_PASSWORD, databasePassword);
          break;
        case KEY_DATABASE_PROPERTIES:
          values.put(KEY_DATABASE_PROPERTIES, Utils.propertiesToString(databaseProperties));
          break;
        case KEY_ENABLE_SSL:
          values.put(KEY_ENABLE_SSL, enableSsl);
          break;
        case KEY_KEYSTORE_FILE:
          values.put(KEY_KEYSTORE_FILE, keystoreFile);
          break;
        case KEY_KEYSTORE_PASSWORD:
          values.put(KEY_KEYSTORE_PASSWORD, keystorePassword);
          break;
        case KEY_TRUSTSTORE_FILE:
          values.put(KEY_TRUSTSTORE_FILE, truststoreFile);
          break;
        case KEY_TRUSTSTORE_PASSWORD:
          values.put(KEY_TRUSTSTORE_PASSWORD, truststorePassword);
          break;
        case KEY_ENABLE_SMTP:
          values.put(KEY_ENABLE_SMTP, enableSmtp);
          break;
        case KEY_SMTP_HOST:
          values.put(KEY_SMTP_HOST, smtpHost);
          break;
        case KEY_SMTP_FROM:
          values.put(KEY_SMTP_FROM, smtpFrom);
          break;
        case KEY_SMTP_USER:
          values.put(KEY_SMTP_USER, smtpUser);
          break;
        case KEY_SMTP_PASSWORD:
          values.put(KEY_SMTP_PASSWORD, smtpPassword);
          break;
      }
    }
  }

  public void getSiteValues(Map<String, Object> values) {
    getValues(values,
        KEY_SITE_NAME,
        KEY_PORT,
        KEY_CONTINUOUSLY_RESTART,
        KEY_RESTART_INTERVAL,
        KEY_USERNAME_PATTERN,
        KEY_PASSWORD_PATTERN,
        KEY_INDEX_PAGE
    );
  }

  public void getDatabaseValues(Map<String, Object> values) {
    getValues(values,
        KEY_DATABASE_URL,
        KEY_DATABASE_USERNAME,
        KEY_DATABASE_PASSWORD,
        KEY_DATABASE_PROPERTIES
    );
  }

  public void getSslValues(Map<String, Object> values) {
    getValues(values,
        KEY_ENABLE_SSL,
        KEY_KEYSTORE_FILE,
        KEY_KEYSTORE_PASSWORD,
        KEY_TRUSTSTORE_FILE,
        KEY_TRUSTSTORE_PASSWORD
    );
  }

  public void getSmtpValues(Map<String, Object> values) {
    getValues(values,
        KEY_ENABLE_SMTP,
        KEY_SMTP_HOST,
        KEY_SMTP_HOST,
        KEY_SMTP_USER,
        KEY_SMTP_PASSWORD
    );
  }

  public void getAllValues(Map<String, Object> values) {
    getSiteValues(values);
    getDatabaseValues(values);
    getSslValues(values);
    getSmtpValues(values);
  }

  public static final String
      KEY_SITE_NAME = "siteName",
      KEY_PORT = "port",
      KEY_CONTINUOUSLY_RESTART = "continuouslyRestart",
      KEY_RESTART_INTERVAL = "restartInterval",
      KEY_USERNAME_PATTERN = "usernamePattern",
      KEY_PASSWORD_PATTERN = "passwordPattern",
      KEY_INDEX_PAGE = "indexPage",
      KEY_DATABASE_URL = "databaseUrl",
      KEY_DATABASE_USERNAME = "databaseUsername",
      KEY_DATABASE_PASSWORD = "databasePassword",
      KEY_DATABASE_PROPERTIES = "databaseProperties",
      KEY_ENABLE_SSL = "enableSsl",
      KEY_KEYSTORE_FILE = "keystoreFile",
      KEY_KEYSTORE_PASSWORD = "keystorePassword",
      KEY_TRUSTSTORE_FILE = "truststoreFile",
      KEY_TRUSTSTORE_PASSWORD = "truststorePassword",
      KEY_ENABLE_SMTP = "enableSmtp",
      KEY_SMTP_HOST = "smtpHost",
      KEY_SMTP_FROM = "smtpFrom",
      KEY_SMTP_USER = "smtpUser",
      KEY_SMTP_PASSWORD = "smtpPassword";

}
