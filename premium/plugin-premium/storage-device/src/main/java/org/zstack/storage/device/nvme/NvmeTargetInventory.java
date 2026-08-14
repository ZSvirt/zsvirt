package org.zstack.storage.device.nvme;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.storage.device.iscsi.IscsiTargetInventory;
import org.zstack.utils.CollectionUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = NvmeTargetVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "nvmeLun", inventoryClass = NvmeLunInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "nvmeTargetUuid"),
})
public class NvmeTargetInventory implements Serializable {
    private String uuid;

    private String name;

    private String nqn;

    private String nvmeServerUuid;

    private String state;

    private List<NvmeLunInventory> nvmeLuns;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public NvmeTargetInventory() {
    }

    public NvmeTargetInventory(NvmeTargetVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setNqn(vo.getNqn());
        this.setNvmeServerUuid(vo.getNvmeServerUuid());
        this.setState(vo.getState());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setNvmeLuns(NvmeLunInventory.valueOf1(vo.getNvmeLuns()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NvmeTargetInventory that = (NvmeTargetInventory) o;

        if (!nqn.equals(that.nqn)) return false;
        return new HashSet<>(nvmeLuns).equals(new HashSet<>(that.nvmeLuns));
    }

    @Override
    public int hashCode() {
        int result = nqn.hashCode();
        Collections.sort(nvmeLuns);
        result = 31 * result + nvmeLuns.hashCode();
        return result;
    }

    public static NvmeTargetInventory valueOf(NvmeTargetVO vo) {
        return new NvmeTargetInventory(vo);
    }

    public static List<NvmeTargetInventory> valueOf(Collection<NvmeTargetVO> vos) {
        return vos.stream().map(NvmeTargetInventory::new).collect(Collectors.toList());
    }

    public static List<NvmeTargetInventory> valueOf2(Collection<NvmeLunStruct> structs) {
        List<NvmeTargetInventory> inventories = new ArrayList<>();
        if (CollectionUtils.isEmpty(structs)) {
            return inventories;
        }
        Map<String, NvmeTargetInventory> nqns = new HashMap<>();
        for (NvmeLunStruct struct : structs) {
            NvmeTargetInventory nvmeTargetInventory = nqns.computeIfAbsent(struct.getNqn(), k -> new NvmeTargetInventory());
            nvmeTargetInventory.setNqn(struct.getNqn());
            if (nvmeTargetInventory.getNvmeLuns() == null) {
                nvmeTargetInventory.setNvmeLuns(new ArrayList<>());
            }

            nvmeTargetInventory.getNvmeLuns().add(new NvmeLunInventory(struct));
        }
        return new ArrayList<>(nqns.values());
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNqn() {
        return nqn;
    }

    public void setNqn(String nqn) {
        this.nqn = nqn;
    }

    public List<NvmeLunInventory> getNvmeLuns() {
        return nvmeLuns;
    }

    public void setNvmeLuns(List<NvmeLunInventory> nvmeLuns) {
        this.nvmeLuns = nvmeLuns;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getNvmeServerUuid() {
        return nvmeServerUuid;
    }

    public void setNvmeServerUuid(String nvmeServerUuid) {
        this.nvmeServerUuid = nvmeServerUuid;
    }
}
