package liblks.security;


import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Simple SHA-256 and SHA-512 hashing.
 */
public class CoreSHA {
    /**
     * Hashes the given string with SHA-512 without salt. It is recommended to use salt for higher security.
     * @param toDigest The string to hash.
     * @return The hashed string of 128 characters.
     */
    public static String hash512(String toDigest) {
        return hash(toDigest, "", "sha-512");
    }

    /**
     * Hashes the given string with SHA-512 with salt.
     * @param toDigest The string to hash.
     * @param salt The salt to use. (Salt: A string that is added to the string to make hash more secure and unique.)
     * @return The hashed string of 128 characters.
     */
    public static String hash512(String toDigest, String salt) {
        return hash(toDigest, salt, "sha-512");
    }

    /**
     * Hashes the given string with SHA-256 without salt. It is recommended to use salt for higher security.
     * @param toDigest The string to hash.
     * @return The hashed string of 64 characters.
     */
    public static String hash256(String toDigest) {
        return hash(toDigest, "", "sha-256");
    }

    /**
     * Hashes the given string with SHA-256 with salt.
     * @param toDigest The string to hash.
     * @param salt The salt to use. (Salt: A string that is added to the string to make hash more secure and unique.)
     * @return The hashed string of 64 characters.
     */
    public static String hash256(String toDigest, String salt) {
        return hash(toDigest, salt, "sha-256");
    }

    /**
     * Hashes the given string with the given algorithm. Unless you manually set the algorithm, it is recommended to use {@link #hash256(String)}, {@link #hash512(String)}, {@link #hash256(String, String)}, or {@link #hash512(String, String)}.
     * @param toDigest The string to hash.
     * @param salt The salt to use. (Salt: A string that is added to the string to make hash more secure and unique.)
     * @param algorithm The algorithm to use. (SHA-256, SHA-512, etc.)
     * @return The hashed string.
     */
    public static String hash(String toDigest, String salt, String algorithm) {
        try {
            String hashtext = "";
            for (int i = 0; i < 3; i++) {
                toDigest = toDigest + salt;
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] messageDigest = md.digest(toDigest.getBytes());
                BigInteger no = new BigInteger(1, messageDigest);
                hashtext = no.toString(16);
                while (hashtext.length() < 32) {
                    hashtext = "0" + hashtext;
                }
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}
