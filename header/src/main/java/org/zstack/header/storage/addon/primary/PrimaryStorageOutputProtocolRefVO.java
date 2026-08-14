package org.zstack.header.storage.addon.primary;

import org.zstack.header.vo.EntityGraph;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ExternalPrimaryStorageVO.class, myField = "primaryStorageUuid", targetField = "uuid")
        }
)
public class PrimaryStorageOutputProtocolRefVO {
    @Column
    @Id
    private long id;

    @Column
    //@ForeignKey(parentEntityClass = ExternalPrimaryStorageVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    private String outputProtocol;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getOutputProtocol() {
        return outputProtocol;
    }

    public void setOutputProtocol(String outputProtocol) {
        this.outputProtocol = outputProtocol;
    }
}
