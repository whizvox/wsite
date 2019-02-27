package me.whizvox.wsite.database;

import java.time.Instant;
import java.util.UUID;

public class User {

  public UUID id;
  public String username;
  public String emailAddress;
  public String password;
  public boolean operator;
  public Instant whenCreated;

}
