package me.whizvox.wsite.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonUtils {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static <T> T readJson(InputStream in, Class<T> cls) throws IOException {
    return MAPPER.readValue(in, cls);
  }

  public static void writeJson(OutputStream out, Object obj) throws IOException {
    MAPPER.writeValue(out, obj);
  }

  public static String toJson(Object obj) {
    try {
      return MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(e);
    }
  }

}
