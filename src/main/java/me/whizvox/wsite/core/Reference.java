package me.whizvox.wsite.core;

import me.whizvox.wsite.util.Version;

import java.time.Instant;

public class Reference {

  private static final String
      _VERSION_STRING = "@VERSION@",
      _RELEASED_STRING = "@RELEASED@";

  public static final Version VERSION;
  public static final Instant RELEASED;

  static {
    if (_VERSION_STRING.equals("\u0040VERSION\u0040")) {
      VERSION = null;
    } else {
      VERSION = Version.fromString(_VERSION_STRING);
    }
    if (_RELEASED_STRING.equals("\u0040RELEASED\u0040")) {
      RELEASED = Instant.now();
    } else {
      RELEASED = Instant.ofEpochMilli(Long.parseLong(_RELEASED_STRING));
    }
  }

  public static boolean usingDevBuild() {
    return VERSION == null;
  }

  public static final int
      LOGIN_TOKEN_LENGTH = 24,
      LOGIN_DEFAULT_EXPIRATION = 40320, // 28 days
      MIN_RESTART_INTERVAL = 360,       // 6 hours
      MAX_RESTART_INTERVAL = 524160,    // 1 year
      TICK_DELAY = 100;                 // 0.1 seconds
  public static final String
      TEMPLATES_DIR = "templates",
      SECURE_DIR = "secure",
      TEMP_DIR = "temp",
      STATIC_DIR = "static",
      ASSETS_DIR = "static/assets",
      CONFIG_FILE = "configuration.json",
      KEY_CONFIG = "config";

}
