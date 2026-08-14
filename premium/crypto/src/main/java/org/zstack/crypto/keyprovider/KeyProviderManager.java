package org.zstack.crypto.keyprovider;

import org.zstack.header.keyprovider.KeyProviderType;

public interface KeyProviderManager {
    /**
     * @param type key provider type
     * @return key provider factory
     */
    KeyProviderFactory getKeyProviderFactory(KeyProviderType type);
}
