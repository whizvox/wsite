package me.whizvox.wsite.core;

import lombok.Getter;
import lombok.Setter;

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

  public static final String
      KEY_SITE_NAME = "siteName",
      KEY_PORT = "port",
      KEY_CONTINUOUSLY_RESTART = "continuouslyRestart",
      KEY_RESTART_INTERVAL = "restartInterval",
      KEY_USERNAME_PATTERN = "usernamePattern",
      KEY_PASSWORD_PATTERN = "passwordPattern",
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
