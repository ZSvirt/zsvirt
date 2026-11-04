package org.zstack.pciDevice.entity.virtual.vfio_mdev;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.mttyDevice.entity.MttyDeviceInventory;
import org.zstack.pciDevice.entity.PciDeviceInventory;
import org.zstack.pciDevice.entity.PciDeviceVO;
import org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecInventory;
import org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecVO;
import org.zstack.pciDevice.header.virtual.vfio_mdev.MdevDeviceChooser;
import org.zstack.pciDevice.header.virtual.vfio_mdev.MdevDeviceState;
import org.zstack.pciDevice.header.virtual.vfio_mdev.MdevDeviceStatus;
import org.zstack.pciDevice.header.virtual.vfio_mdev.MdevDeviceType;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-04-17.
 */
@PythonClassInventory
@Inventory(mappingVOClass = MdevDeviceVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "parent", inventoryClass = PciDeviceInventory.class,
                foreignKey = "parentUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "mtty", inventoryClass = MttyDeviceInventory.class,
                foreignKey = "mttyUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "mdevDeviceSpec", inventoryClass = MdevDeviceSpecInventory.class,
                foreignKey = "mdevSpecUuid", expandedInventoryKey = "uuid"),
})
public class MdevDeviceInventory {
    private String uuid;
    private String name;
    private String description;
    private String parentUuid;
    private String mttyUuid;
    private String hostUuid;
    private String vmInstanceUuid;
    private String mdevSpecUuid;
    private MdevDeviceType type;
    private MdevDeviceState state;
    private MdevDeviceStatus status;
    private MdevDeviceChooser chooser;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String vendor;

    public MdevDeviceInventory() {
    }

    public MdevDeviceInventory(MdevDeviceVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.parentUuid = vo.getParentUuid();
        this.mttyUuid = vo.getMttyUuid();
        this.hostUuid = vo.getHostUuid();
        this.vmInstanceUuid = vo.getVmInstanceUuid();
        this.mdevSpecUuid = vo.getMdevSpecUuid();
        this.type = vo.getType();
        this.state = vo.getState();
        this.status = vo.getStatus();
        this.chooser = vo.getChooser();
        this.lastOpDate = vo.getLastOpDate();
        this.createDate = vo.getCreateDate();
        this.vendor = vo.getVendor();
    }

    public static MdevDeviceInventory __example__() {
        MdevDeviceInventory inv = new MdevDeviceInventory();
        inv.setUuid(DocUtils.createFixedUuid(MdevDeviceVO.class));
        inv.setName("GRID M60-2A");
        inv.setHostUuid(DocUtils.createFixedUuid(HostVO.class));
        inv.setParentUuid(DocUtils.createFixedUuid(PciDeviceVO.class));
        inv.setMdevSpecUuid(DocUtils.createFixedUuid(MdevDeviceSpecVO.class));
        inv.setType(MdevDeviceType.GPU_Video_Controller);
        inv.setState(MdevDeviceState.Enabled);
        inv.setStatus(MdevDeviceStatus.Active);
        inv.setChooser(MdevDeviceChooser.None);
        inv.setVendor("Intel");
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public static MdevDeviceInventory valueOf(MdevDeviceVO vo) {
        return new MdevDeviceInventory(vo);
    }

    public static List<MdevDeviceInventory> valueOf(Collection<MdevDeviceVO> vos) {
        return vos.stream().map(MdevDeviceInventory::valueOf).collect(Collectors.toList());
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

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getMttyUuid() {
        return mttyUuid;
    }

    public void setMttyUuid(String mttyUuid) {
        this.mttyUuid = mttyUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getMdevSpecUuid() {
        return mdevSpecUuid;
    }

    public void setMdevSpecUuid(String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }

    public MdevDeviceType getType() {
        return type;
    }

    public void setType(MdevDeviceType type) {
        this.type = type;
    }

    public MdevDeviceState getState() {
        return state;
    }

    public void setState(MdevDeviceState state) {
        this.state = state;
    }

    public MdevDeviceStatus getStatus() {
        return status;
    }

    public void setStatus(MdevDeviceStatus status) {
        this.status = status;
    }

    public MdevDeviceChooser getChooser() {
        return chooser;
    }

    public void setChooser(MdevDeviceChooser chooser) {
        this.chooser = chooser;
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

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    private static String uuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
