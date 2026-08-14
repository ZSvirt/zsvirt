package org.zstack.ha;

import org.zstack.header.configuration.PythonClassInventory;
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

    public static VmHaInventory valueOf(VmHaVO vo) {
        VmHaInventory inventory = new VmHaInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setHaLevel(vo.getHaLevel().toString());
        inventory.setHaLevelUpdateTime(vo.getHaLevelUpdateTime());
        inventory.setInhibitionReason(vo.getInhibitionReason());
        inventory.setInhibitionTime(vo.getInhibitionTime());
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

    @Override
    public String toString() {
        return "VmHaInventory{" +
        "uuid='" + uuid + '\'' +
        ", haLevel='" + haLevel + '\'' +
        ", haLevelUpdateTime=" + haLevelUpdateTime +
        ", inhibitionReason='" + inhibitionReason + '\'' +
        ", inhibitionTime=" + inhibitionTime +
        '}';
    }

    public static VmHaInventory __example__() {
        VmHaInventory inventory = new VmHaInventory();
        inventory.setUuid("a917abdd4ebe42db904289e244779b47");
        inventory.setHaLevel("NeverStop");
        inventory.setHaLevelUpdateTime(new Timestamp(1735192572615L));
        inventory.setInhibitionReason("APIStopVmInstanceMsg");
        inventory.setInhibitionTime(new Timestamp(1735192610654L));
        return inventory;
    }
}
