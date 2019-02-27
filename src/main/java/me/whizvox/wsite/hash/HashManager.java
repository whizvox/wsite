package me.whizvox.wsite.hash;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.Arrays;

public class HashManager {

  public final int keyLength;
  public final int saltSize;
  public final String algorithm;
  public final int iterations;

  private final SecureRandom rand;

  HashManager(int keyLength, int saltSize, String algorithm, int iterations) {
    this.keyLength = keyLength;
    this.saltSize = saltSize;
    this.algorithm = algorithm;
    this.iterations = iterations;
    rand = new SecureRandom();
  }

  private byte[] generateSalt() {
    byte[] salt = new byte[saltSize];
    rand.nextBytes(salt);
    return salt;
  }

  private byte[] generateHash(char[] password, byte[] salt) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
      SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
      byte[] hash = f.generateSecret(spec).getEncoded();
      Arrays.fill(password, ' ');
      spec.clearPassword();
      return hash;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new HashGenerationException("Could not generate a hash", e);
    }
  }

  public Hash generate(char[] password) {
    Hash hash = new Hash();
    hash.salt = generateSalt();
    hash.hashedPassword = generateHash(password, hash.salt);
    return hash;
  }

  public boolean check(char[] attempt, String storedHashString) {
    Hash hash = Hash.fromString(storedHashString, keyLength / 8, saltSize);
    byte[] attemptedHash = generateHash(attempt, hash.salt);
    Arrays.fill(attempt, ' ');
    return Arrays.equals(attemptedHash, hash.hashedPassword);
  }

  public byte[] fillBytes(byte[] bytes) {
    rand.nextBytes(bytes);
    return bytes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private static final String
        DEFAULT_ALGORITHM = "PBKDF2WITHHMACSHA256";
    private static final int
        DEFAULT_KEY_LENGTH = 256,
        DEFAULT_SALT_SIZE = 8,
        DEFAULT_ITERATIONS = 5_000,
        MIN_HASH_SIZE = 64,
        MAX_HASH_SIZE = 1024,
        MIN_SALT_LENGTH = 4,
        MAX_SALT_LENGTH = 64,
        MIN_ITERATIONS = 1,
        MAX_ITERATIONS = 100_000;

    public int keyLength;
    public int saltSize;
    public String algorithm;
    public int iterations;
    public boolean defaultsIfFail;

    public Builder() {
      keyLength = DEFAULT_KEY_LENGTH;
      saltSize = DEFAULT_SALT_SIZE;
      algorithm = DEFAULT_ALGORITHM;
      iterations = DEFAULT_ITERATIONS;
      defaultsIfFail = true;
    }

    public Builder setKeyLength(int keyLength) {
      this.keyLength = keyLength;
      return this;
    }

    public Builder setSaltSize(int saltSize) {
      this.saltSize = saltSize;
      return this;
    }

    public Builder setAlgorithm(String algorithm) {
      this.algorithm = algorithm;
      return this;
    }

    public Builder setIterations(int iterations) {
      this.iterations = iterations;
      return this;
    }

    public Builder setDefaultsIfFail(boolean defaultsIfFail) {
      this.defaultsIfFail = defaultsIfFail;
      return this;
    }

    public HashManager build() {
      boolean algoFound = false;
      for (String algoName : Security.getAlgorithms("SecretKeyFactory")) {
        if (algoName.equalsIgnoreCase(algorithm)) {
          algoFound = true;
          break;
        }
      }
      if (!algoFound) {
        if (defaultsIfFail) {
          algorithm = DEFAULT_ALGORITHM;
        } else {
          throw new IllegalArgumentException(MessageFormat.format("Algorithm ({0}) could not be found", algorithm));
        }
      }
      if (keyLength < MIN_HASH_SIZE || keyLength > MAX_HASH_SIZE) {
        if (defaultsIfFail) {
          keyLength = DEFAULT_KEY_LENGTH;
        } else {
          throw new IllegalArgumentException(MessageFormat.format("Hash size ({0,number,#}) exceeds bounds: " +
              "[{1,number,#}, {2,number,#}]", keyLength, MIN_HASH_SIZE, MAX_HASH_SIZE));
        }
      }
      if (saltSize < MIN_SALT_LENGTH || saltSize > MAX_SALT_LENGTH) {
        if (defaultsIfFail) {
          saltSize = DEFAULT_SALT_SIZE;
        } else {
          throw new IllegalArgumentException(MessageFormat.format("Salt length ({0,number,#}) exceeds bounds: " +
              "[{1,number,#}, {2,number,#}]", saltSize, MIN_SALT_LENGTH, MAX_SALT_LENGTH));
        }
      }
      if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
        if (defaultsIfFail) {
          iterations = DEFAULT_ITERATIONS;
        } else {
          throw new IllegalArgumentException(MessageFormat.format("Iterations ({0,number,#}) exceeds bounds: " +
              "[{1,number,#}, {2,number,#}]", iterations, MIN_ITERATIONS, MAX_ITERATIONS));
        }
      }
      return new HashManager(keyLength, saltSize, algorithm, iterations);
    }

  }

}
