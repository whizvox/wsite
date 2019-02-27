package me.whizvox.wsite.core;

public class Launcher {

  public static void main(String[] args) throws Exception {

    WsiteConfiguration config = new WsiteConfiguration();
    WsiteService service = WsiteService.builder()
        .setConfig(config)
        .build();
    service.run();

  }

}
