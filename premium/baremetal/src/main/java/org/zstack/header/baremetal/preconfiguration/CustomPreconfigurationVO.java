package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by GuoYi on 2018-12-28.
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = BaremetalInstanceVO.class, myField = "baremetalInstanceUuid", targetField = "uuid")
        }
)
public class CustomPreconfigurationVO implements ToInventory {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = BaremetalInstanceVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String baremetalInstanceUuid;

    @Column
    private String param;

    @Column
    private String value;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBaremetalInstanceUuid() {
        return baremetalInstanceUuid;
    }

    public void setBaremetalInstanceUuid(String baremetalInstanceUuid) {
        this.baremetalInstanceUuid = baremetalInstanceUuid;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
