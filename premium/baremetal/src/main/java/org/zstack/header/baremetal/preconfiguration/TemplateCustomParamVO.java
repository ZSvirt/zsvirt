package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;

/**
 * Created by GuoYi on 2018-12-28.
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = PreconfigurationTemplateVO.class, myField = "templateUuid", targetField = "uuid")
        }
)
public class TemplateCustomParamVO implements ToInventory {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = PreconfigurationTemplateVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String templateUuid;

    @Column
    private String param;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
