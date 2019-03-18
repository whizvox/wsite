package me.whizvox.wsite.core;

import me.whizvox.wsite.util.Utils;

public class ApiGenericResult {

  public boolean success;
  public String message;

  public ApiGenericResult(boolean success, String message) {
    this.success = success;
    this.message = message;
  }

  public ApiGenericResult(Exception e) {
    this(false, Utils.stacktraceToString(e));
  }

  public ApiGenericResult() {
    this(true, null);
  }

}
