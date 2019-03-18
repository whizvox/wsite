package me.whizvox.wsite.util;

import spark.QueryParamsMap;
import spark.Request;

import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

public class HttpUtils {

  public static String getString(QueryParamsMap params, String key, String defaultValue) {
    return Optional.ofNullable(params.get(key).value()).orElse(defaultValue);
  }

  public static String getString(QueryParamsMap params, String key) {
    return params.get(key).value();
  }

  public static String getBase64String(QueryParamsMap params, String key, String defaultValue) {
    String str = getString(params, key, defaultValue);
    if (str != null) {
      return Utils.decodeBase64(str);
    }
    return null;
  }

  public static String getBase64String(QueryParamsMap params, String key) {
    return getBase64String(params, key, null);
  }

  public static boolean getBool(QueryParamsMap params, String key, boolean defaultValue) {
    return Optional.ofNullable(params.get(key).booleanValue()).orElse(defaultValue);
  }

  public static boolean getBool(QueryParamsMap params, String key) {
    return getBool(params, key, false);
  }

  public static int getInt(QueryParamsMap params, String key, int defaultValue) {
    try {
      return Optional.ofNullable(params.get(key).integerValue()).orElse(defaultValue);
    } catch (NumberFormatException ignored) {}
    return defaultValue;
  }

  public static int getInt(QueryParamsMap params, String key) {
    return getInt(params, key, 0);
  }

  public static long getLong(QueryParamsMap params, String key, long defaultValue) {
    try {
      return Optional.ofNullable(params.get(key).longValue()).orElse(defaultValue);
    } catch (NumberFormatException ignored) {}
    return defaultValue;
  }

  public static long getLong(QueryParamsMap params, String key) {
    return getLong(params, key, 0L);
  }

  public static float getFloat(QueryParamsMap params, String key, float defaultValue) {
    try {
      return Optional.ofNullable(params.get(key).floatValue()).orElse(defaultValue);
    } catch (NumberFormatException ignored) {}
    return defaultValue;
  }

  public static float getFloat(QueryParamsMap params, String key) {
    return getFloat(params, key, 0.0f);
  }

  public static double getDouble(QueryParamsMap params, String key, double defaultValue) {
    try {
      return Optional.ofNullable(params.get(key).doubleValue()).orElse(defaultValue);
    } catch (NumberFormatException ignored) {}
    return defaultValue;
  }

  public static double getDouble(QueryParamsMap params, String key) {
    return getDouble(params, key, 0.0);
  }

  public static UUID getUuid(QueryParamsMap params, String key, UUID defaultValue) {
    try {
      String str = params.get(key).value();
      if (str != null) {
        return UUID.fromString(str);
      }
    } catch (IllegalArgumentException e) {}
    return defaultValue;
  }

  public static UUID getUuid(QueryParamsMap params, String key) {
    return getUuid(params, key, null);
  }

  public static char[] getCharArray(QueryParamsMap params, String key, char[] defaultValue) {
    if (params.hasKey(key)) {
      return params.get(key).value().toCharArray();
    }
    return defaultValue;
  }

  public static char[] getCharArray(QueryParamsMap params, String key) {
    return getCharArray(params, key, null);
  }

  public static String getFile(Request request, String key, String defaultValue) throws IOException, ServletException {
    Part part = request.raw().getPart(key);
    if (part != null) {
      try (InputStream in = part.getInputStream()) {
        return IOUtils.readStringFromStream(in);
      }
    }
    return defaultValue;
  }

  public static InputStream getFile(Request request, String key) throws IOException, ServletException {
    Part part = request.raw().getPart(key);
    if (part != null) {
      return part.getInputStream();
    }
    return null;
  }

  public static Path copyFile(Request request, String key, Path output, boolean isDir) throws IOException, ServletException {
    Part part = request.raw().getPart(key);
    if (part != null) {
      if (isDir) {
        output = output.resolve(part.getSubmittedFileName());
      }
      try (InputStream in = part.getInputStream()) {
        Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING);
      }
      return output;
    }
    return null;
  }

}
