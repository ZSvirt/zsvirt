package org.zstack.header.baremetal.network;

import org.zstack.header.tag.AutoDeleteTag;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@AutoDeleteTag
public class BaremetalVlanNicVO extends BaremetalNicVO {
    @Column
    private int vlan;

    public int getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }

    public BaremetalVlanNicVO() {
    }

    public BaremetalVlanNicVO(BaremetalNicVO vo) {
        super(vo);
    }

    public BaremetalVlanNicVO(BaremetalVlanNicVO other) {
        super(other);
        this.setVlan(other.getVlan());
    }
}
