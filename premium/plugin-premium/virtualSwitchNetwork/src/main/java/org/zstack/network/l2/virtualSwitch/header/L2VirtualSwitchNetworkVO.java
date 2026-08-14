package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class L2VirtualSwitchNetworkVO extends L2NetworkVO {
    @Column
    private Boolean isDistributed;

    @Column
    private Integer vSwitchIndex;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vSwitchUuid", insertable = false, updatable = false)
    @NoView
    private Set<PortGroupVO> portGroups = new HashSet<PortGroupVO>();

    public L2VirtualSwitchNetworkVO() {
    }

    public L2VirtualSwitchNetworkVO(L2NetworkVO vo) {
        super(vo);
    }

    public Boolean getDistributed() {
        return isDistributed;
    }

    public void setDistributed(Boolean distributed) {
        isDistributed = distributed;
    }

    public Integer getVSwitchIndex() {
        return vSwitchIndex;
    }

    public void setVSwitchIndex(Integer index) {
        this.vSwitchIndex = index;
    }

    public Set<PortGroupVO> getPortGroups() {
        return portGroups;
    }

    public void setPortGroups(Set<PortGroupVO> portGroups) {
        this.portGroups = portGroups;
    }
}
