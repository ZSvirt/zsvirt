package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;
import java.util.Map;

/**
 * Created by LiangHanYu on 2022/6/24 10:58
 */
public class ChangeVmNicNetworkMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmNicUuid;
    private String destL3NetworkUuid;
    private String vmInstanceUuid;
    private Map<String, List<String>> requiredIpMap;
    private List<VmNicParam> vmNicParams;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getDestL3NetworkUuid() {
        return destL3NetworkUuid;
    }

    public void setDestL3NetworkUuid(String destL3NetworkUuid) {
        this.destL3NetworkUuid = destL3NetworkUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Map<String, List<String>> getRequiredIpMap() {
        return requiredIpMap;
    }

    public void setRequiredIpMap(Map<String, List<String>> requiredIpMap) {
        this.requiredIpMap = requiredIpMap;
    }

    public List<VmNicParam> getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(List<VmNicParam> vmNicParams) {
        this.vmNicParams = vmNicParams;
    }
}
