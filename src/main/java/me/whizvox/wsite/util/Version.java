package me.whizvox.wsite.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

  public final int major;
  public final int minor;
  public final int revision;
  public final String type;
  public final int build;

  public Version(int major, int minor, int revision, String type, int build) {
    this.major = major;
    this.minor = minor;
    this.revision = revision;
    this.type = type;
    this.build = build;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Version) {
      Version v = (Version) o;
      return v.major == major && v.minor == minor && v.revision == revision && v.type.equalsIgnoreCase(type) &&
          v.build == build;
    }
    return false;
  }

  @Override
  public int compareTo(Version version) {
    return version.build - build;
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + revision + "-" + type + "-" + build;
  }

  private static final Pattern PATTERN_VERSION = Pattern.compile("(\\d).(\\d).(\\d)-([a-zA-Z0-9_]+)-(\\d)");

  public static Version fromString(String str) {
    Matcher m = PATTERN_VERSION.matcher(str);
    if (!m.find()) {
      return null;
    }
    int major = Integer.parseInt(m.group(1));
    int minor = Integer.parseInt(m.group(2));
    int revision = Integer.parseInt(m.group(3));
    String type = m.group(4);
    int build = Integer.parseInt(m.group(5));
    return new Version(major, minor, revision, type, build);
  }
}
