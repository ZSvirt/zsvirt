package org.zstack.compute.vm.quota;

import org.zstack.compute.vm.VmQuotaConstant;
import org.zstack.compute.vm.VmQuotaGlobalConfig;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.volume.VolumeVO;

public class VolumeSizeQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VmQuotaConstant.VOLUME_SIZE;
    }

    @Override
    public Long getDefaultValue() {
        return VmQuotaGlobalConfig.VOLUME_SIZE.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select sum(vol.size)" +
                " from VolumeVO vol, AccountResourceRefVO ref" +
                " where ref.resourceUuid = vol.uuid" +
                " and ref.accountUuid = :auuid" +
                " and ref.type = :type" +
                " and ref.resourceType = :rtype";
        Long used = SQL.New(sql, Long.class)
                .param("auuid", accountUuid)
                .param("type", AccessLevel.Own)
                .param("rtype", VolumeVO.class.getSimpleName())
                .find();
        return used == null ? 0L : used;
    }
}