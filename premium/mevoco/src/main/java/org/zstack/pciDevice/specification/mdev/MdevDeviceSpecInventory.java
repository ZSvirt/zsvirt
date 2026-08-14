package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceType;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-04-17.
 */
@PythonClassInventory
@Inventory(mappingVOClass = MdevDeviceSpecVO.class)
public class MdevDeviceSpecInventory {
    private String uuid;
    private String name;
    private String description;
    private String specification;
    private MdevDeviceType type;
    private MdevDeviceSpecState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public MdevDeviceSpecInventory() {
    }

    public MdevDeviceSpecInventory(MdevDeviceSpecVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.specification = vo.getSpecification();
        this.type = vo.getType();
        this.state = vo.getState();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static MdevDeviceSpecInventory valueOf(MdevDeviceSpecVO vo) {
        return new MdevDeviceSpecInventory(vo);
    }

    public static List<MdevDeviceSpecInventory> valueOf(Collection<MdevDeviceSpecVO> vos) {
        return vos.stream().map(MdevDeviceSpecInventory::valueOf).collect(Collectors.toList());
    }

    public static MdevDeviceSpecInventory __example__() {
        MdevDeviceSpecInventory inv = new MdevDeviceSpecInventory();
        inv.setUuid(DocUtils.createFixedUuid(MdevDeviceSpecVO.class));
        inv.setName("GRID_M60-2A");
        Map<String, String> spec = new HashMap<>();
        spec.put("Name", "GRID_M60-2A");
        spec.put("Vendor", "NVIDIA");
        spec.put("Instance Number", "4");
        spec.put("RAM", "2048MB");
        spec.put("Display Heads", "4");
        spec.put("Max Resolution", "1920*1080");
        spec.put("Frame Rate Limit", "60FPS");
        spec.put("GRID License", "GRID-Virtual-Apps,3.0");
        inv.setSpecification(spec.toString());
        inv.setType(MdevDeviceType.GPU_Video_Controller);
        inv.setState(MdevDeviceSpecState.Enabled);
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public MdevDeviceType getType() {
        return type;
    }

    public void setType(MdevDeviceType type) {
        this.type = type;
    }

    public MdevDeviceSpecState getState() {
        return state;
    }

    public void setState(MdevDeviceSpecState state) {
        this.state = state;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
