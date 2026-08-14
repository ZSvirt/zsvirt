package org.zstack.header.storage.primary;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

@org.zstack.header.vo.EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = PrimaryStorageEO.class, myField = "primaryStorageUuid", targetField = "uuid")
        }
)
@Table
@MappedSuperclass
public class PrimaryStorageHistoricalUsageBaseVO extends HistoricalUsageAO {

    public PrimaryStorageHistoricalUsageBaseVO() {
        resourceType = PrimaryStorageHistoricalUsageBaseVO.class.getSimpleName();
    }

    @Column
    @Index
    @org.zstack.header.vo.ForeignKey(parentEntityClass = PrimaryStorageEO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    protected String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getResourceUuid() {
        return primaryStorageUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.primaryStorageUuid = resourceUuid;
    }
}
