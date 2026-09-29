package org.zstack.ha.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VmHaVO.class)
@PythonClassInventory
public class VmHaInventory {
    private String uuid;
    private String haLevel;
    private Timestamp haLevelUpdateTime;
    private String inhibitionReason;
    private Timestamp inhibitionTime;
    private String lastFencerName;
    private String lastFenceHostUuid;
    private Timestamp lastFenceTime;

    public static VmHaInventory valueOf(VmHaVO vo) {
        VmHaInventory inventory = new VmHaInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setHaLevel(vo.getHaLevel().toString());
        inventory.setHaLevelUpdateTime(vo.getHaLevelUpdateTime());
        inventory.setInhibitionReason(vo.getInhibitionReason());
        inventory.setInhibitionTime(vo.getInhibitionTime());
        inventory.setLastFencerName(vo.getLastFencerName());
        inventory.setLastFenceHostUuid(vo.getLastFenceHostUuid());
        inventory.setLastFenceTime(vo.getLastFenceTime());
        return inventory;
    }

    public static List<VmHaInventory> valueOf(Collection<VmHaVO> vos) {
        return CollectionUtils.transform(vos, VmHaInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHaLevel() {
        return haLevel;
    }

    public void setHaLevel(String haLevel) {
        this.haLevel = haLevel;
    }

    public Timestamp getHaLevelUpdateTime() {
        return haLevelUpdateTime;
    }

    public void setHaLevelUpdateTime(Timestamp haLevelUpdateTime) {
        this.haLevelUpdateTime = haLevelUpdateTime;
    }

    public String getInhibitionReason() {
        return inhibitionReason;
    }

    public void setInhibitionReason(String inhibitionReason) {
        this.inhibitionReason = inhibitionReason;
    }

    public Timestamp getInhibitionTime() {
        return inhibitionTime;
    }

    public void setInhibitionTime(Timestamp inhibitionTime) {
        this.inhibitionTime = inhibitionTime;
    }

    public String getLastFencerName() {
        return lastFencerName;
    }

    public void setLastFencerName(String lastFencerName) {
        this.lastFencerName = lastFencerName;
    }

    public String getLastFenceHostUuid() {
        return lastFenceHostUuid;
    }

    public void setLastFenceHostUuid(String lastFenceHostUuid) {
        this.lastFenceHostUuid = lastFenceHostUuid;
    }

    public Timestamp getLastFenceTime() {
        return lastFenceTime;
    }

    public void setLastFenceTime(Timestamp lastFenceTime) {
        this.lastFenceTime = lastFenceTime;
    }

    @Override
    public String toString() {
        return "VmHaInventory{" +
        "uuid='" + uuid + '\'' +
        ", haLevel='" + haLevel + '\'' +
        ", haLevelUpdateTime=" + haLevelUpdateTime +
        ", inhibitionReason='" + inhibitionReason + '\'' +
        ", inhibitionTime=" + inhibitionTime +
        ", lastFencerName='" + lastFencerName + '\'' +
        ", lastFenceHostUuid='" + lastFenceHostUuid + '\'' +
        ", lastFenceTime=" + lastFenceTime +
        '}';
    }

    public static VmHaInventory __example__() {
        VmHaInventory inventory = new VmHaInventory();
        inventory.setUuid("a917abdd4ebe42db904289e244779b47");
        inventory.setHaLevel("NeverStop");
        inventory.setHaLevelUpdateTime(new Timestamp(1735192572615L));
        inventory.setInhibitionReason("APIStopVmInstanceMsg");
        inventory.setInhibitionTime(new Timestamp(1735192610654L));
        inventory.setLastFencerName("hostFileSystemFencer");
        inventory.setLastFenceHostUuid(DocUtils.createFixedUuid(HostVO.class));
        inventory.setLastFenceTime(new Timestamp(1735192610654L));
        return inventory;
    }
}
