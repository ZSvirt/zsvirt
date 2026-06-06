package org.zstack.crypto.keyprovider;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.keyprovider.KeyProviderGlobalConfig;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;

public final class KeyProviderHealthScope {
    private KeyProviderHealthScope() {
    }

    public static String getDefaultProviderUuid() {
        String uuid = KeyProviderGlobalConfig.DEFAULT_KEY_PROVIDER_UUID.value(String.class);
        if (StringUtils.isBlank(uuid) || KeyProviderGlobalConfig.NONE.equals(uuid)) {
            return null;
        }
        return uuid;
    }

    public static boolean isDefaultProvider(String providerUuid) {
        if (StringUtils.isBlank(providerUuid)) {
            return false;
        }
        String defaultUuid = getDefaultProviderUuid();
        return defaultUuid != null && defaultUuid.equals(providerUuid);
    }

    public static boolean hasResourceRef(String providerUuid) {
        if (StringUtils.isBlank(providerUuid)) {
            return false;
        }

        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.providerUuid, providerUuid)
                .count();
        if (count != null && count > 0) {
            return true;
        }

        String providerName = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.uuid, providerUuid)
                .select(KeyProviderVO_.name)
                .findValue();
        if (StringUtils.isBlank(providerName)) {
            return false;
        }

        count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.providerName, providerName)
                .count();
        return count != null && count > 0;
    }
}
