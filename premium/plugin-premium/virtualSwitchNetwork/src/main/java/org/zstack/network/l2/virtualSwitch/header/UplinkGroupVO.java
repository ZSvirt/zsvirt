package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;

import javax.persistence.*;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = HostNetworkBondingVO.class, myField = "bondingUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostNetworkInterfaceVO.class, myField = "interfaceUuid", targetField = "uuid"),
        }
)
public class UplinkGroupVO extends L2NetworkHostRefVO {
    @Column
    private String interfaceName;

    @Column
    @Enumerated(EnumType.STRING)
    private UplinkGroupType type;

    // work around for front-end queries
    @Column
    @ForeignKey(parentEntityClass = HostNetworkBondingVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String bondingUuid;

    // work around for front-end queries
    @Column
    @ForeignKey(parentEntityClass = HostNetworkInterfaceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String interfaceUuid;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String name) {
        this.interfaceName = name;
    }

    public UplinkGroupType getType() {
        return type;
    }

    public void setType(UplinkGroupType type) {
        this.type = type;
    }

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    @Override
    public String toString() {
        return String.format("UplinkGroupVO[hostUuid:%s, vSwitchUuid:%s, interfaceName:%s, type:%s]",
                getHostUuid(), getL2NetworkUuid(), interfaceName, type);
    }
}
