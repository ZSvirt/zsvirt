package org.zstack.identity;

import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.zstack.core.db.Q;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;

/** Stores the existing client credential using a server-side salted hash. */
public final class AccountPassword {
    private static final String PREFIX = "{zstack-pbkdf2-sha512-v1}";
    private static final Pbkdf2PasswordEncoder ENCODER = new Pbkdf2PasswordEncoder("", 16, 220000, 256);

    static {
        ENCODER.setAlgorithm(Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA512);
    }

    private AccountPassword() {
    }

    public static boolean isEncoded(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static String encode(String credential) {
        return PREFIX + ENCODER.encode(credential);
    }

    public static String encodeForStorage(String credential) {
        // New credentials always use salted storage; legacy records are read as-is.
        return credential == null ? null : encode(credential);
    }

    public static boolean matches(String credential, String stored) {
        if (credential == null || stored == null) {
            return false;
        }
        if (isEncoded(stored)) {
            String hash = stored.substring(PREFIX.length());
            return hash.matches("[0-9a-f]{96}") && ENCODER.matches(credential, hash);
        }
        // Legacy CLI/API clients already send SHA-512; do not hash that input again here.
        return credential.equals(stored);
    }

    public static boolean matchesAccount(String credential, String accountUuid, String stored) {
        if (credential == null || stored == null) {
            return false;
        }
        // Preserve the database's legacy password comparison, including its collation.
        return isEncoded(stored) ? matches(credential, stored) : Q.New(AccountVO.class)
                .eq(AccountVO_.uuid, accountUuid).eq(AccountVO_.password, credential).isExists();
    }
}
