package me.whizvox.wsite.database;

import java.time.Instant;
import java.util.UUID;

public class Login {

  public String token;
  public UUID userId;
  public String userAgent;
  public String ipAddress;
  public Instant expirationDate;

}
