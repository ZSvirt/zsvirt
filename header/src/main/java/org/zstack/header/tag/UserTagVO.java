package org.zstack.header.tag;

import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;

import javax.persistence.*;

/**
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = TagPatternVO.class, myField = "tagPatternUuid", targetField = "uuid"),
        }
)
public class UserTagVO extends TagAO {
    @Column
    @Index
    @ForeignKey(parentEntityClass = TagPatternVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String tagPatternUuid;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tagPatternUuid", referencedColumnName = "uuid", insertable = false, updatable = false)
    @NoView
    private TagPatternVO tagPattern;

    public UserTagVO() {
        setType(TagType.User);
    }

    public UserTagVO(UserTagVO other) {
        super(other);
    }

    public String getTagPatternUuid() {
        return tagPatternUuid;
    }

    public void setTagPatternUuid(String tagPatternUuid) {
        this.tagPatternUuid = tagPatternUuid;
    }

    public TagPatternVO getTagPattern() {
        return tagPattern;
    }

    public void setTagPattern(TagPatternVO tagPattern) {
        this.tagPattern = tagPattern;
    }
}
