package org.zstack.header.sshkeypair.quota;

import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.sshkeypair.SshKeyPairVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class SshKeyPairQuotaDefinition implements QuotaDefinition {
    private static final CLogger logger = Utils.getLogger(SshKeyPairQuotaDefinition.class);

    @Override
    public String getName() {
        return SshKeyPairQuotaConstant.SSH_KEY_PAIR_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return SshKeyPairQuotaGlobalConfig.SSH_KEY_PAIR_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return ResourceHelper.countOwnResources(SshKeyPairVO.class, accountUuid);
    }
}
