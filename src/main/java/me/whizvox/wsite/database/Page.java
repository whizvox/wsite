package me.whizvox.wsite.database;

import java.time.Instant;

public class Page {

  public String path;
  public String title;
  public String contents;
  public Syntax syntax;
  public Instant published;
  public Instant lastEdited;

  public Page(String path, String title, String contents, Syntax syntax, Instant published, Instant lastEdited) {
    this.path = path;
    this.title = title;
    this.contents = contents;
    this.syntax = syntax;
    this.published = published;
    this.lastEdited = lastEdited;
  }

  public Page() {
  }

  public enum Syntax {
    HTML_LIMITED,
    HTML,
    HTML_LIMITED_EMBEDDED,
    HTML_EMBEDDED,
    PLAIN,
    EMBEDDED,
    MARKDOWN;

    public static Syntax fromString(String str) {
      for (Syntax value : Syntax.values()) {
        if (value.toString().equalsIgnoreCase(str)) {
          return value;
        }
      }
      return null;
    }
  }

}
