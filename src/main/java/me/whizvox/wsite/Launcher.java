package me.whizvox.wsite;

import me.whizvox.wsite.core.Reference;
import me.whizvox.wsite.core.WsiteConfiguration;
import me.whizvox.wsite.core.WsiteService;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {

  public static void main(String[] args) throws Exception {

    WsiteConfiguration config = new WsiteConfiguration();
    Path rootDir;
    String rootDirProp = System.getProperty("wsite.rootDirectory");
    if (rootDirProp != null) {
      rootDir = Paths.get(rootDirProp);
    } else {
      if (Reference.usingDevBuild()) {
        rootDir = Paths.get("rundir");
      } else {
        rootDir = Paths.get(".");
      }
    }
    rootDir = rootDir.toAbsolutePath();
    // used for logback, as you can't declare variable in "realtime"
    System.setProperty("wsite.rootDirectory", rootDir.toString());

    WsiteService service = WsiteService.builder()
        .setConfig(config)
        .setRootDirectory(rootDir)
        .build();
    service.run();

  }

}
