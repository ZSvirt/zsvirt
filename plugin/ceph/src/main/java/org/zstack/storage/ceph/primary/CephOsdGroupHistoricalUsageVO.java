package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageHistoricalUsageBaseVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@BaseResource
@AutoDeleteTag
public class CephOsdGroupHistoricalUsageVO extends PrimaryStorageHistoricalUsageBaseVO {

    public CephOsdGroupHistoricalUsageVO() {
        resourceType = CephOsdGroupVO.class.getSimpleName();
    }

    @Column
    private String osdGroupUuid;

    public String getOsdGroupUuid() {
        return osdGroupUuid;
    }

    public void setOsdGroupUuid(String osdGroupUuid) {
        this.osdGroupUuid = osdGroupUuid;
    }

    @Override
    public String getResourceUuid() {
        return osdGroupUuid;
    }

    @Override
    public void setResourceUuid(String resourceUuid) {
        this.osdGroupUuid = resourceUuid;
    }
}
