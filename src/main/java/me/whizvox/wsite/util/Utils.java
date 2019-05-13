package me.whizvox.wsite.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.Renderer;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Pattern;

public class Utils {

  private static final char[] HEX_CHARS = new char[] {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
  
  public static byte[] bytesFromHexString(String hex) {
    if (hex == null) {
      return null;
    }
    byte[] bytes = new byte[hex.length() / 2];
    if (bytes.length > 0) {
      if (hex.length() % 2 != 0) {
        hex = "0" + hex;
      }
      for (int i = 0; i < bytes.length; i++) {
        bytes[i] = (byte) (Character.digit(hex.charAt(i * 2), 16) << 4 | Character.digit(hex.charAt(i * 2 + 1), 16));
      }
    }
    return bytes;
  }

  public static String hexStringFromBytes(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    if (bytes.length > 0) {
      for (byte b : bytes) {
        sb.append(HEX_CHARS[(b & 0xf0) >> 4])
            .append(HEX_CHARS[b & 0xf]);
      }
    }
    return sb.toString();
  }

  public static boolean isNullOrEmpty(String str) {
    return str == null || str.isEmpty();
  }

  public static String stacktraceToString(Exception e) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      PrintStream ps = new PrintStream(bos);
      e.printStackTrace(ps);
      return bos.toString();
    } catch (IOException ignored) {}
    return null;
  }

  private static final DateTimeFormatter DATE_FORMAT_FILE_SAFE = DateTimeFormatter.ofPattern("yyyyMMdd-kkmmss");

  public static String formatFileSafeInstant(Instant instant) {
    return LocalDateTime.from(instant.atOffset(ZoneOffset.UTC)).format(DATE_FORMAT_FILE_SAFE);
  }

  public static Instant parseFileSafeInstant(String str) {
    return LocalDateTime.parse(str, DATE_FORMAT_FILE_SAFE).toInstant(ZoneOffset.UTC);
  }

  public static Instant timestampToInstant(Timestamp timestamp) {
    return Instant.ofEpochMilli(timestamp.getTime());
  }

  private static final Pattern
      PATTERN_USERNAME = Pattern.compile("^[a-zA-Z0-9-_]{2,16}$"),
      PATTERN_EMAIL = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

  public static boolean checkUsername(String username) {
    return PATTERN_USERNAME.matcher(username).matches();
  }

  public static boolean checkEmailAddress(String emailAddress) {
    return PATTERN_EMAIL.matcher(emailAddress).matches();
  }

  private static final PolicyFactory HTML_FULL_SANITIZER = new HtmlPolicyBuilder()
      .allowElements()
      .toFactory();

  private static final PolicyFactory HTML_PARTIAL_SANITIZER = new HtmlPolicyBuilder()
      .disallowElements("script", "form", "input")
      .toFactory();

  public static String htmlToPlainText(String html) {
    return HTML_FULL_SANITIZER.sanitize(html);
  }

  public static String sanitizeHtml(String html) {
    return HTML_PARTIAL_SANITIZER.sanitize(html);
  }

  private static final Parser COMMONMARK_PARSER = Parser.builder().build();
  private static final Renderer COMMONMARK_RENDERER = HtmlRenderer.builder().build();

  public static String parseMarkdown(String markdown) {
    Node doc = COMMONMARK_PARSER.parse(markdown);
    return COMMONMARK_RENDERER.render(doc);
  }

  public static Properties parseProperties(String str) {
    if (str == null) {
      return null;
    }
    Properties props = new Properties();
    if (!str.isEmpty()) {
      String[] entries = str.split(";");
      for (String e : entries) {
        String[] keyValue = e.split("=");
        if (keyValue.length == 2) {
          props.put(keyValue[0], keyValue[1]);
        }
      }
    }
    return props;
  }

  public static String propertiesToString(Properties props) {
    if (props == null) {
      return null;
    }
    if (!props.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      props.forEach((key, value) -> sb.append(key).append('=').append(value).append(';'));
      sb.setLength(sb.length() - 1); // remove the last semicolon
      return sb.toString();
    }
    return "";
  }

  public static String encodeBase64(String str) {
    return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
  }

  public static String decodeBase64(String base64) {
    return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
  }

}
