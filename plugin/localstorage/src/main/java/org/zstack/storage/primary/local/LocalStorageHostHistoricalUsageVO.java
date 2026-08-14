package org.zstack.storage.primary.local;

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
public class LocalStorageHostHistoricalUsageVO extends PrimaryStorageHistoricalUsageBaseVO {

    public LocalStorageHostHistoricalUsageVO() {
        resourceType = LocalStorageHostRefVO.class.getSimpleName();
    }

    @Column
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getResourceUuid() {
        return hostUuid;
    }

    @Override
    public void setResourceUuid(String resourceUuid) {
        this.hostUuid = resourceUuid;
    }
}
