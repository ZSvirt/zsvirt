package org.zstack.storage.device.hba;

import org.zstack.header.search.Inventory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/18 16:07
 */
@Inventory(mappingVOClass = HbaDeviceVO.class)
public class HbaDeviceInventory implements Serializable {
    private String uuid;
    private String name;
    private String hostUuid;
    private String hbaType;
    private String createDate;
    private String lastOpDate;


    public HbaDeviceInventory() {
    }

    public HbaDeviceInventory(HbaDeviceVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.hostUuid = vo.getHostUuid();
        this.hbaType = vo.getHbaType().toString();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static List<HbaDeviceInventory> valueOf(Collection<HbaDeviceVO> vos) {
        List<HbaDeviceInventory> invs = new ArrayList<HbaDeviceInventory>();
        for (HbaDeviceVO vo : vos) {
            invs.add(new HbaDeviceInventory(vo));
        }

        return invs;
    }

    public static HbaDeviceInventory valueOf(HbaDeviceVO vo) {
        return new HbaDeviceInventory(vo);
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHbaType() {
        return hbaType;
    }

    public void setHbaType(String hbaType) {
        this.hbaType = hbaType;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public String getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(String lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
