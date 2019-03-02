package me.whizvox.wsite.core;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {

  public static void main(String[] args) throws Exception {

    WsiteConfiguration config = new WsiteConfiguration();
    Path rootDir;
    // TODO: Maybe have Gradle handle the working directory?
    if (Reference.usingDevBuild()) {
      rootDir = Paths.get("rundir");
    } else {
      rootDir = Paths.get(".");
    }
    WsiteService service = WsiteService.builder()
        .setConfig(config)
        .setRootDirectory(rootDir)
        .build();
    service.run();

  }

}
