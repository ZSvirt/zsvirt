package org.zstack.testlib.identity.ldap;

import org.zstack.identity.imports.source.AccountSourceSyncingExtensions;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class AccountSourceSyncingExtensionsSimulator extends AccountSourceSyncingExtensions {
    private static final CLogger logger = Utils.getLogger(AccountSourceSyncingExtensionsSimulator.class);

    {
        logger.info("use AccountSourceSyncingExtensionsSimulator to replace original AccountSourceSyncingExtensions");
    }

    public static final int RATE = 60;

    @Override
    protected long accountSourceAutoSyncIntervalInSeconds(String sourceUuid) {
        return super.accountSourceAutoSyncIntervalInSeconds(sourceUuid) / RATE;
    }
}
