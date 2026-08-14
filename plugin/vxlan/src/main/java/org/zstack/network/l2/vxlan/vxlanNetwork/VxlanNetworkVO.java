package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

import javax.persistence.*;

/**
 * Created by weiwang on 02/03/2017.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class VxlanNetworkVO extends L2NetworkVO {
    @Column
    private Integer vni;

    @Column
    @ForeignKey(parentEntityClass = VxlanNetworkPoolVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String poolUuid;

    public VxlanNetworkVO() {
    }

    public VxlanNetworkVO(L2NetworkVO vo) {
        super(vo);
    }

    public int getVni() {
        return vni;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

}
