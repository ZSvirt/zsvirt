package org.zstack.storage.device.iscsi;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storageDevice.ScsiLunHostRefInventory;
import org.zstack.header.storageDevice.ScsiLunInventory;
import org.zstack.header.storageDevice.ScsiLunVmInstanceRefInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = IscsiLunVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "iscsiTarget", inventoryClass = IscsiTargetInventory.class,
                foreignKey = "iscsiTargetUuid", expandedInventoryKey = "uuid"),
})
public class IscsiLunInventory extends ScsiLunInventory implements Serializable, Comparable<IscsiLunInventory> {
    private String iscsiTargetUuid;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IscsiLunInventory)) return false;

        IscsiLunInventory that = (IscsiLunInventory) o;
        return (StringUtils.isNotBlank(getComparableSerial()) || StringUtils.isNotBlank(getComparableWwid())) &&
                getComparableSerial().equals(that.getComparableSerial()) &&
                getComparableWwid().equals(that.getComparableWwid());
    }

    @Override
    public int hashCode() {
        int result = getComparableSerial().hashCode();
        result = 31 * result + getComparableWwid().hashCode();
        return result;
    }

    public IscsiLunInventory() {
    }

    public IscsiLunInventory(IscsiLunVO vo) {
        super(vo);
        this.setIscsiTargetUuid(vo.getIscsiTargetUuid());
    }

    private String getProperWwid(List<String> wwids) {
        List<String> validWwids = new ArrayList<>();
        if (wwids != null) {
            for (String wwid : wwids) {
                if (StringUtils.isNotBlank(wwid)) {
                    validWwids.add(wwid);
                }
            }
        }

        if (validWwids.isEmpty()) {
            return "";
        }

        //NOTE(weiw): lvm-pv as a wwid is not stable, a simple wipefs will change it
        Collections.sort(validWwids);
        for (String wwid : validWwids) {
            if (wwid.contains("lvm-pv")) {
                continue;
            }
            return wwid;
        }
        return validWwids.get(0);
    }

    public IscsiLunInventory(IscsiLunStruct s) {
        if (s.wwids != null) {
            Collections.sort(s.wwids);
        }
        this.setWwid(getProperWwid(s.wwids));
        this.setVendor(s.vendor);
        this.setModel(s.model);
        this.setWwn(s.wwn);
        this.setSerial(s.serial);
        this.setHctl(s.hctl);
        this.setType(s.type);
        this.setPath(s.path);
        this.setSize(s.size);
        this.setMultipathDeviceUuid(s.multipathDeviceUuid);
    }

    public static List<IscsiLunInventory> valueOf3(Collection<IscsiLunStruct> structs) {
        List<IscsiLunInventory> invs = new ArrayList<IscsiLunInventory>();
        for (IscsiLunStruct s : structs) {
            invs.add(new IscsiLunInventory(s));
        }

        return invs;
    }

    public static IscsiLunInventory valueOf(IscsiLunVO vo) {
        return new IscsiLunInventory(vo);
    }

    public static List<IscsiLunInventory> valueOf2(Collection<IscsiLunVO> vos) {
        List<IscsiLunInventory> invs = new ArrayList<IscsiLunInventory>();
        for (IscsiLunVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getIscsiTargetUuid() {
        return iscsiTargetUuid;
    }

    public void setIscsiTargetUuid(String iscsiTargetUuid) {
        this.iscsiTargetUuid = iscsiTargetUuid;
    }

    @Override
    public int compareTo(IscsiLunInventory o) {
        int result = getComparableSerial().compareTo(o.getComparableSerial());
        return result != 0 ? result : getComparableWwid().compareTo(o.getComparableWwid());
    }

    private String getComparableSerial() {
        return StringUtils.trimToEmpty(getSerial());
    }

    private String getComparableWwid() {
        return StringUtils.trimToEmpty(getWwid());
    }
}
