package org.zstack.guesttools;

import org.zstack.header.message.NeedReplyMessage;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class UpdateGuestToolsStateMsg extends NeedReplyMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private List<GuestToolsServiceType> serviceTypeList = new ArrayList<>(EnumSet.allOf(GuestToolsServiceType.class));

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<GuestToolsServiceType> getServiceTypeList() {
        return serviceTypeList;
    }

    public void setServiceTypeList(List<GuestToolsServiceType> serviceTypeList) {
        this.serviceTypeList = serviceTypeList;
    }
}
