package me.whizvox.wsite.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class IOUtils {

  public static void mkdirs(Path path) {
    if (!Files.exists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException e) {
        throw new IllegalArgumentException("Could not create directories", e);
      }
    }
  }

  public static void touch(Path path) {
    if (!Files.exists(path)) {
      try {
        Files.createFile(path);
      } catch (IOException e) {
        throw new IllegalArgumentException("Could not create new file", e);
      }
    }
  }

  public static void copyDirectory(Path sourcePath, Path targetPath) throws IOException {
    Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
        Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void delete(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not delete file", e);
    }
  }

  public static URL getResource(String path) {
    return IOUtils.class.getClassLoader().getResource(path);
  }

  public static boolean copyFromResource(String resourcePath, Path outputFilePath, boolean replaceExisting) {
    if (!Files.exists(outputFilePath)) {
      Path parent = outputFilePath.getParent();
      if (parent != null) {
        mkdirs(parent);
      }
      touch(outputFilePath);
    } else if (!replaceExisting) {
      return false;
    }
    URL resourceUrl = getResource(resourcePath);
    if (resourceUrl == null) {
      return false;
    }
    try (InputStream in = resourceUrl.openStream()) {
      Files.copy(in, outputFilePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not copy internal resource to external file", e);
    }
    return true;
  }

  public static String readStringFromStream(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    final int BUFFER_SIZE = 1024;
    byte[] buffer = new byte[BUFFER_SIZE];
    int read;
    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  public static String readStringFromFile(Path input) throws IOException {
    return new String(Files.readAllBytes(input), StandardCharsets.UTF_8);
  }

  public static void writeStringToFile(Path output, String str) throws IOException {
    Files.write(output, str.getBytes(StandardCharsets.UTF_8));
  }

}
