package org.zstack.ovf.datatype;

/**
 * Created by Qi Le on 2022/3/4
 */
public class OvfEthernetAdapterInfo {
    private String networkName;
    private String nicModel;
    private String nicName;
    private Boolean autoAllocation;

    public String getNicModel() {
        return nicModel;
    }

    public void setNicModel(String nicModel) {
        this.nicModel = nicModel;
    }

    public String getNicName() {
        return nicName;
    }

    public void setNicName(String nicName) {
        this.nicName = nicName;
    }

    public Boolean getAutoAllocation() {
        return autoAllocation;
    }

    public void setAutoAllocation(Boolean autoAllocation) {
        this.autoAllocation = autoAllocation;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }
}
