package me.whizvox.wsite.core;

public class WsiteEvent {

  protected WsiteService wsite;

  public WsiteEvent(WsiteService wsite) {
    this.wsite = wsite;
  }

  public static class Create extends WsiteEvent {
    public Create(WsiteService wsite) {
      super(wsite);
    }
  }

  public static class Restarting extends WsiteEvent {
    public Restarting(WsiteService wsite) {
      super(wsite);
    }
  }

  public static class Shutdown extends WsiteEvent {
    public Shutdown(WsiteService wsite) {
      super(wsite);
    }
  }

}
