package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.image.ImageVO;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.volume.*;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Entity
@Table
@EO(EOClazz = VmInstanceEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ClusterVO.class, myField = "clusterUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = ImageVO.class, myField = "imageUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = InstanceOfferingVO.class, myField = "instanceOfferingUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "rootVolumeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmNicVO.class, myField = "uuid", targetField = "vmInstanceUuid"),
                @EntityGraph.Neighbour(type = VmCdRomVO.class, myField = "uuid", targetField = "vmInstanceUuid"),
                @EntityGraph.Neighbour(type = TpmVO.class, myField = "uuid", targetField = "vmInstanceUuid"),
        }
)
public class VmInstanceVO extends VmInstanceAO implements OwnedByAccount, ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vmInstanceUuid", insertable = false, updatable = false)
    @NoView
    private Set<VmNicVO> vmNics = new HashSet<VmNicVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vmInstanceUuid", insertable = false, updatable = false)
    @NoView
    private Set<VolumeVO> allVolumes = new HashSet<VolumeVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vmInstanceUuid", insertable = false, updatable = false)
    @NoView
    private Set<VmCdRomVO> vmCdRoms = new HashSet<VmCdRomVO>();

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public VmInstanceVO() {
    }

    public VmInstanceVO(VmInstanceVO other) {
        super(other);
        this.vmNics = other.vmNics;
        this.allVolumes = other.allVolumes;
        this.accountUuid = other.accountUuid;
    }

    public Set<VmNicVO> getVmNics() {
        return vmNics;
    }

    public void setVmNics(Set<VmNicVO> vmNics) {
        this.vmNics = vmNics;
    }

    @Deprecated
    public Set<VolumeVO> getAllVolumes() {
        return allVolumes;
    }

    public void setAllVolumes(Set<VolumeVO> allVolumes) {
        this.allVolumes = allVolumes;
    }


    public Set<VolumeVO> getAllVolumes(Predicate<VolumeVO> predicate) {
        return allVolumes.stream().filter(predicate).collect(Collectors.toSet());
    }

    public VolumeVO getVolume(Predicate<VolumeVO> predicate) {
        return allVolumes.stream().filter(predicate).findFirst().orElse(null);
    }

    public Set<VolumeVO> getAllDiskVolumes() {
        return getAllVolumes(VolumeVO::isDisk);
    }

    public VolumeVO getMemoryVolume() {
        if (allVolumes == null) {
            return null;
        }
        return allVolumes.stream().filter(v -> v.getType().equals(VolumeType.Memory)).findAny().orElse(null);
    }

    public VolumeVO getRootVolume() {
        if (allVolumes == null) {
            return null;
        }

        return allVolumes.stream().filter(v -> v.getUuid().equals(getRootVolumeUuid())).findAny().orElse(null);
    }

    public Set<VolumeVO> getAllDataVolumes() {
        return getAllVolumes(VolumeAO::isDataVolume);
    }

    public Set<VmCdRomVO> getVmCdRoms() {
        return vmCdRoms;
    }

    public void setVmCdRoms(Set<VmCdRomVO> vmCdRoms) {
        this.vmCdRoms = vmCdRoms;
    }
}
