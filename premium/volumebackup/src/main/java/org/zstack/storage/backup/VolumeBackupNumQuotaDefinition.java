package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.identity.quota.QuotaDefinition;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VolumeBackupNumQuotaDefinition implements QuotaDefinition {

    @Override
    public String getName() {
        return VolumeBackupQuotaConstant.VOLUME_BACKUP_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VolumeBackupQuotaGlobalConfig.VOLUME_BACKUP_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return new VolumeBackupQuotaUtil().getUsedNum(accountUuid);
    }
}
