package org.zstack.header.tag;

/**
 * Hook to transparently decrypt sensitive token values when read from the database.
 * Implementations must only decrypt whitelisted tags; non-target tags must be returned unchanged.
 */
public interface SystemTagTokenDecryptExtensionPoint {
    String decryptTokenValue(String resourceType, String tagHead, String tokenName, String tokenValue);
}
