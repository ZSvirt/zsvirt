package org.zstack.compute.vm.quota;

import org.zstack.compute.vm.VmQuotaConstant;
import org.zstack.compute.vm.VmQuotaGlobalConfig;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;

public class DataVolumeNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VmQuotaConstant.DATA_VOLUME_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VmQuotaGlobalConfig.DATA_VOLUME_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        String sql = "select count(vol)" +
                " from VolumeVO vol, AccountResourceRefVO ref " +
                " where vol.type = :vtype" +
                " and ref.resourceUuid = vol.uuid " +
                " and ref.accountUuid = :auuid" +
                " and ref.resourceType = :rtype" +
                " and ref.type = :type" +
                " and vol.status != :status ";
        Long used = SQL.New(sql, Long.class)
                .param("auuid", accountUuid)
                .param("rtype", VolumeVO.class.getSimpleName())
                .param("type", AccessLevel.Own)
                .param("vtype", VolumeType.Data)
                .param("status", VolumeStatus.Deleted)
                .find();
        return used == null ? 0L : used;
    }
}
