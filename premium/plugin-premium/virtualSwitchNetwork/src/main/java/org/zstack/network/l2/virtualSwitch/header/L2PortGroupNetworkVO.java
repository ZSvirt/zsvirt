package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class L2PortGroupNetworkVO extends L2NetworkVO {

    @Column
    @ForeignKey(parentEntityClass = L2VirtualSwitchNetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String vSwitchUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private PortGroupVlanMode vlanMode;

    @Column
    private Integer vlanId;

    @Column
    private String vlanRanges;

    public L2PortGroupNetworkVO() {
    }

    public L2PortGroupNetworkVO(L2NetworkVO vo) {
        super(vo);
    }

    public String getvSwitchUuid() {
        return vSwitchUuid;
    }

    public void setvSwitchUuid(String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }

    public int getVlanId() {
        return vlanId;
    }

    public void setVlanId(int vlanId) {
        this.vlanId = vlanId;
    }

    public PortGroupVlanMode getVlanMode() {
        return vlanMode;
    }

    public void setVlanMode(PortGroupVlanMode vlanMode) {
        this.vlanMode = vlanMode;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public String getVlanRanges() {
        return vlanRanges;
    }

    public void setVlanRanges(String vlanRanges) {
        this.vlanRanges = vlanRanges;
    }
}
