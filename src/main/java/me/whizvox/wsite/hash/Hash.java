package me.whizvox.wsite.hash;

import me.whizvox.wsite.util.Utils;

import java.text.MessageFormat;
import java.util.Arrays;

public class Hash {

  public byte[] hashedPassword;
  public byte[] salt;

  public String compileAsHexString() {
    byte[] full = new byte[hashedPassword.length + salt.length];
    System.arraycopy(hashedPassword, 0, full, 0, hashedPassword.length);
    System.arraycopy(salt, 0, full, hashedPassword.length, salt.length);
    return Utils.hexStringFromBytes(full);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Hash) {
      Hash other = (Hash) o;
      return Arrays.equals(hashedPassword, other.hashedPassword) && Arrays.equals(salt, other.salt);
    }
    return false;
  }

  @Override
  public String toString() {
    return compileAsHexString();
  }

  public static Hash fromString(String str, int keyLengthBytes, int saltSize) {
    if (keyLengthBytes + saltSize != str.length() / 2) {
      throw new IllegalArgumentException(MessageFormat.format("Hash string ({0}) cannot be parsed with " +
          "key length of {1,number,#} bytes and salt length of {2,number,#} bytes", str, keyLengthBytes, saltSize));
    }
    byte[] bytes = Utils.bytesFromHexString(str);
    Hash hash = new Hash();
    hash.hashedPassword = new byte[keyLengthBytes];
    hash.salt = new byte[saltSize];
    System.arraycopy(bytes, 0, hash.hashedPassword, 0, keyLengthBytes);
    System.arraycopy(bytes, keyLengthBytes, hash.salt, 0, saltSize);
    return hash;
  }

}
