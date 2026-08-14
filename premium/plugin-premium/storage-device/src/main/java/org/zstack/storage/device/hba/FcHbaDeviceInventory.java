package org.zstack.storage.device.hba;

import org.zstack.header.host.HostInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostVO;
import org.zstack.storage.device.StorageConstant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/25 09:50
 */
@Inventory(mappingVOClass = FcHbaDeviceVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = FcHbaDeviceInventory.class, type = StorageConstant.HBA_TYPE)})
public class FcHbaDeviceInventory extends HbaDeviceInventory {
    private String portName;
    private String portState;
    private String speed;
    private String supportedSpeeds;
    private String symbolicName;
    private String supportedClasses;
    private String nodeName;


    public FcHbaDeviceInventory() {
    }

    public static List<FcHbaDeviceInventory> valueOf1(Collection<FcHbaDeviceVO> vos) {
        List<FcHbaDeviceInventory> invs = new ArrayList<FcHbaDeviceInventory>();
        for (FcHbaDeviceVO vo : vos) {
            invs.add(new FcHbaDeviceInventory(vo));
        }

        return invs;
    }

    public static FcHbaDeviceInventory valueOf(FcHbaDeviceVO vo) {
        return new FcHbaDeviceInventory(vo);
    }

    public FcHbaDeviceInventory(FcHbaDeviceVO vo) {
        super(vo);
        this.portName = vo.getPortName();
        this.portState = vo.getPortState();
        this.speed = vo.getSpeed();
        this.supportedSpeeds = vo.getSupportedSpeeds();
        this.symbolicName = vo.getSymbolicName();
        this.supportedClasses = vo.getSupportedClasses();
        this.nodeName = vo.getNodeName();
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public String getPortState() {
        return portState;
    }

    public void setPortState(String portState) {
        this.portState = portState;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public String getSupportedSpeeds() {
        return supportedSpeeds;
    }

    public void setSupportedSpeeds(String supportedSpeeds) {
        this.supportedSpeeds = supportedSpeeds;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getSupportedClasses() {
        return supportedClasses;
    }

    public void setSupportedClasses(String supportedClasses) {
        this.supportedClasses = supportedClasses;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }
}
