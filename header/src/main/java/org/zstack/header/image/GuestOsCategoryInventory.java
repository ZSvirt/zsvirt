package org.zstack.header.image;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = GuestOsCategoryVO.class)
@PythonClassInventory
@ExpandedQueries({
})
public class GuestOsCategoryInventory implements Serializable {
    private String uuid;
    private String platform;
    private String name;
    private String osRelease;
    private String version;

    public static GuestOsCategoryInventory valueOf(GuestOsCategoryVO vo) {
        GuestOsCategoryInventory inv = new GuestOsCategoryInventory();
        inv.setUuid(vo.getUuid());
        inv.setPlatform(vo.getPlatform());
        inv.setName(vo.getName());
        inv.setVersion(vo.getVersion());
        inv.setOsRelease(vo.getOsRelease());
        return inv;
    }

    public static List<GuestOsCategoryInventory> valueOf(Collection<GuestOsCategoryVO> vos) {
        List<GuestOsCategoryInventory> invs = new ArrayList<GuestOsCategoryInventory>(vos.size());
        for (GuestOsCategoryVO vo : vos) {
            invs.add(GuestOsCategoryInventory.valueOf(vo));
        }
        return invs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getOsRelease() {
        return osRelease;
    }

    public void setOsRelease(String osRelease) {
        this.osRelease = osRelease;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

