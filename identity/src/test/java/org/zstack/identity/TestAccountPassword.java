package org.zstack.identity;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestAccountPassword {
    private static final String LEGACY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
            + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    public void legacyCredentialsStillAuthenticate() {
        assertTrue(AccountPassword.matches(LEGACY, LEGACY));
        assertTrue(AccountPassword.matches("legacy-api-input", "legacy-api-input"));
        assertFalse(AccountPassword.matches(LEGACY + "1", LEGACY));
        assertFalse(AccountPassword.matches(null, LEGACY));
        assertFalse(AccountPassword.matches(LEGACY, null));
        assertFalse(AccountPassword.matches("\uD800", "?"));
    }

    @Test
    public void nullCredentialRemainsNull() {
        assertNull(AccountPassword.encodeForStorage(null));
    }

    @Test
    public void saltedStorageAcceptsTheUnchangedClientCredential() {
        String first = AccountPassword.encodeForStorage(LEGACY);
        String second = AccountPassword.encodeForStorage(LEGACY);
        assertTrue(AccountPassword.isEncoded(first));
        assertTrue(first.length() <= 255);
        assertNotEquals(first, second);
        assertTrue(AccountPassword.matches(LEGACY, first));
        // All 128 input characters must matter; no bcrypt-style 72-byte truncation.
        assertFalse(AccountPassword.matches(LEGACY.substring(0, 127) + "0", first));
        assertFalse(AccountPassword.matches(first, first));
        assertTrue(AccountPassword.matchesAccount(LEGACY, "account", first));
        assertFalse(AccountPassword.matchesAccount(first, "account", first));
        assertFalse(AccountPassword.matchesAccount("wrong", "account", first));
    }

    @Test
    public void malformedSaltedRecordsFailClosed() {
        String stored = AccountPassword.encode(LEGACY);
        String truncated = stored.substring(0, stored.length() - 1);
        assertFalse(AccountPassword.matches(LEGACY, truncated));
        assertFalse(AccountPassword.matches(truncated, truncated));
        assertFalse(AccountPassword.matchesAccount(truncated, "account", truncated));
        assertFalse(AccountPassword.matches(LEGACY, stored.substring(0, stored.indexOf('}') + 1) + "not-hex"));
    }

    @Test
    public void historyCanCompareBothFormats() {
        String stored = AccountPassword.encode(LEGACY);
        assertTrue(AccountPassword.matches(LEGACY, stored));
        assertTrue(AccountPassword.matches(LEGACY, LEGACY));
        assertFalse(AccountPassword.matches("another", stored));
        assertTrue(AccountPassword.isEncoded(stored));
        assertFalse(AccountPassword.isEncoded(LEGACY));
    }
}
