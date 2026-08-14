package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class VmAttachNicMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String l3NetworkUuid;
    private Map<String, List<String>> staticIpMap = new HashMap<>();
    private boolean allowDuplicatedAddress = false;
    private boolean applyToBackend = true;
    private String driverType;
    private boolean allowDuplicatedMac = false;

    public boolean isAllowDuplicatedAddress() {
        return allowDuplicatedAddress;
    }

    public void setAllowDuplicatedAddress(boolean allowDuplicatedAddress) {
        this.allowDuplicatedAddress = allowDuplicatedAddress;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Map<String, List<String>> getStaticIpMap() {
        return staticIpMap;
    }

    public void setStaticIpMap(Map<String, List<String>> staticIpMap) {
        this.staticIpMap = staticIpMap;
    }

    public boolean isApplyToBackend() {
        return applyToBackend;
    }

    public void setApplyToBackend(boolean applyToBackend) {
        this.applyToBackend = applyToBackend;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public boolean isAllowDuplicatedMac() {
        return allowDuplicatedMac;
    }

    public void setAllowDuplicatedMac(boolean allowDuplicatedMac) {
        this.allowDuplicatedMac = allowDuplicatedMac;
    }
}
