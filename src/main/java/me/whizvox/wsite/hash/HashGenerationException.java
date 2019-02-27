package me.whizvox.wsite.hash;

public class HashGenerationException extends RuntimeException {

  public HashGenerationException() {
  }

  public HashGenerationException(String s) {
    super(s);
  }

  public HashGenerationException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public HashGenerationException(Throwable throwable) {
    super(throwable);
  }

}
