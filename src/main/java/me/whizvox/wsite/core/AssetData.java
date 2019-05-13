package me.whizvox.wsite.core;

import java.time.Instant;

public class AssetData {

  public String path;
  public boolean protect;
  public long size;
  public Instant uploaded;
  public Instant lastEdited;

  public AssetData(String path, boolean protect, long size, Instant uploaded, Instant lastEdited) {
    this.path = path;
    this.protect = protect;
    this.size = size;
    this.uploaded = uploaded;
    this.lastEdited = lastEdited;
  }

  public AssetData() {
  }

}
