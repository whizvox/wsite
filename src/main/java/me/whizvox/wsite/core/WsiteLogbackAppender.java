package me.whizvox.wsite.core;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

public class WsiteLogbackAppender extends AppenderBase<ILoggingEvent> {

  private static WsiteLogbackAppender instance;

  public static WsiteLogbackAppender getInstance() {
    return instance;
  }

  private Encoder<ILoggingEvent> encoder;
  private Routes.ConsoleRoute consoleRoute;

  public WsiteLogbackAppender() {
    if (instance != null) {
      throw new IllegalArgumentException("Only one instance of WsiteLogbackAppender can exist");
    }
    // really hacky, but the only way to do this
    instance = this;
    consoleRoute = null;
  }

  public Encoder<ILoggingEvent> getEncoder() {
    return encoder;
  }

  public void setEncoder(Encoder<ILoggingEvent> encoder) {
    this.encoder = encoder;
  }

  public void setConsoleRoute(Routes.ConsoleRoute consoleRoute) {
    this.consoleRoute = consoleRoute;
  }

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (consoleRoute != null) {
      String str = new String(encoder.encode(eventObject));
      consoleRoute.broadcast(new LogEvent(eventObject.getLevel().toInt(), str));
    }
  }

  public static class LogEvent {
    public int level;
    public String message;
    public LogEvent() {
    }
    public LogEvent(int level, String message) {
      this.level = level;
      this.message = message;
    }
  }


}
