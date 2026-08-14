package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.network.l2.L2NetworkEO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vtep.RemoteVtepVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by weiwang on 02/03/2017.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
@EO(EOClazz = L2NetworkEO.class, needView = false)
@AutoDeleteTag
public class VxlanNetworkPoolVO extends L2NetworkVO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "poolUuid", insertable = false, updatable = false)
    @NoView
    private Set<VtepVO> attachedVtepRefs = new HashSet<VtepVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "poolUuid", insertable = false, updatable = false)
    @NoView
    private Set<RemoteVtepVO> remoteVteps = new HashSet<RemoteVtepVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l2NetworkUuid", insertable = false, updatable = false)
    @NoView
    private Set<VniRangeVO> attachedVniRanges = new HashSet<>();

    public VxlanNetworkPoolVO() {
    }

    public VxlanNetworkPoolVO(L2NetworkVO vo) {
        super(vo);
    }

    public Set<RemoteVtepVO> getRemoteVteps() {
        return remoteVteps;
    }

    public void setRemoteVteps(Set<RemoteVtepVO> remoteVteps) {
        this.remoteVteps = remoteVteps;
    }

    public Set<VtepVO> getAttachedVtepRefs() {
        return attachedVtepRefs;
    }

    public void setAttachedVtepRefs(Set<VtepVO> attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }

    public Set<VniRangeVO> getAttachedVniRanges() {
        return attachedVniRanges;
    }

    public void setAttachedVniRanges(Set<VniRangeVO> attachedVniRanges) {
        this.attachedVniRanges = attachedVniRanges;
    }
}
