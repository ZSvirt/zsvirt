package org.zstack.guesttools;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

public class UpdateVmNetworkConfigMsg extends NeedReplyMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = VmNicVO.class, nonempty = true)
    private List<String> vmNicUuids;

    private String haState;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public String getHaState() {
        return haState;
    }

    public void setHaState(String haState) {
        this.haState = haState;
    }
}
