package me.whizvox.wsite.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

  public final String name;
  public final String type;
  public final int build;

  public Version(String name, String type, int build) {
    this.name = name;
    this.type = type;
    this.build = build;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Version) {
      Version v = (Version) o;
      return v.name.equalsIgnoreCase(name) && v.type.equalsIgnoreCase(type) &&
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
    return name + "-" + type + "-" + build;
  }

  private static final Pattern PATTERN_VERSION = Pattern.compile("([^-]+)-([^-]+)-(\\d)");

  public static Version fromString(String str) {
    Matcher m = PATTERN_VERSION.matcher(str);
    if (!m.find()) {
      return null;
    }
    String name = m.group(1);
    String type = m.group(2);
    int build = Integer.parseInt(m.group(3));
    return new Version(name, type, build);
  }
}
