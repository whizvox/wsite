package me.whizvox.wsite.core;

import java.util.Properties;

public class WsiteConfiguration {

  public String siteName;
  public String rootDirectory;
  public boolean continuouslyRestart;
  public int restartInterval;
  public String usernamePattern;
  public String passwordPattern;

  public String databaseUrl;
  public String databaseUsername;
  public String databasePassword;
  public Properties databaseProperties;

  public int port;
  public boolean enableSsl;
  public String keystoreFile;
  public String keystorePassword;
  public String truststoreFile;
  public String truststorePassword;

  public boolean enableSmtp;
  public String smtpHost;
  public String smtpUser;
  public String smtpPassword;

  public WsiteConfiguration() {
    setDefaults();
  }

  public WsiteConfiguration setDefaults() {
    siteName = "Wsite";
    rootDirectory = ".";
    continuouslyRestart = true;
    restartInterval = 1440;
    usernamePattern = "^[a-zA-Z0-9_]{1,16}$";
    passwordPattern = ".{7,}";

    databaseUrl = "jdbc:sqlite:${ROOT}/wsite.db";
    databaseUsername = null;
    databasePassword = null;
    databaseProperties = new Properties();

    port = 4568;
    enableSsl = false;
    keystoreFile = null;
    keystorePassword = null;
    truststoreFile = null;
    truststorePassword = null;

    enableSmtp = false;
    smtpHost = null;
    smtpUser = null;
    smtpPassword = null;
    return this;
  }

}
