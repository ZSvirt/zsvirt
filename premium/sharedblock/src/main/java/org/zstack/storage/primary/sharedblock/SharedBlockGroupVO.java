package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = PrimaryStorageEO.class, needView = false)
@AutoDeleteTag
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = SharedBlockVO.class, myField = "sharedBlockGroupUuid", targetField = "uuid")
        }
)
public class SharedBlockGroupVO extends PrimaryStorageVO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name="sharedBlockGroupUuid", insertable=false, updatable=false)
    @NoView
    private Set<SharedBlockVO> sharedBlocks = new HashSet<SharedBlockVO>();

    @Column
    @Enumerated(value = EnumType.STRING)
    private SharedBlockGroupType sharedBlockGroupType;

    public SharedBlockGroupVO() {
    }

    public SharedBlockGroupVO(PrimaryStorageVO other) {
        super(other);
    }

    public SharedBlockGroupVO(SharedBlockGroupVO other) {
        super(other);
        this.sharedBlockGroupType = other.sharedBlockGroupType;
    }

    public Set<SharedBlockVO> getSharedBlocks() {
        return sharedBlocks;
    }

    public void setSharedBlocks(Set<SharedBlockVO> sharedBlocks) {
        this.sharedBlocks = sharedBlocks;
    }

    public SharedBlockGroupType getSharedBlockGroupType() {
        return sharedBlockGroupType;
    }

    public void setSharedBlockGroupType(SharedBlockGroupType sharedBlockGroupType) {
        this.sharedBlockGroupType = sharedBlockGroupType;
    }
}
