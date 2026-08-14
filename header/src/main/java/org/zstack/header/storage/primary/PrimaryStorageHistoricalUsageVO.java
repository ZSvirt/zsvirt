package org.zstack.header.storage.primary;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@BaseResource
@AutoDeleteTag
public class PrimaryStorageHistoricalUsageVO extends PrimaryStorageHistoricalUsageBaseVO {

    public PrimaryStorageHistoricalUsageVO() {
        resourceType = PrimaryStorageVO.class.getSimpleName();
    }
}
