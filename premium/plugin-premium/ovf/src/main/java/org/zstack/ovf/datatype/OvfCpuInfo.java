package org.zstack.ovf.datatype;

/**
 * Created by Qi Le on 2022/3/4
 */
public class OvfCpuInfo {
    private String instanceId;
    private Integer quantity;
    private Integer coresPerSocket;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getCoresPerSocket() {
        return coresPerSocket;
    }

    public void setCoresPerSocket(Integer coresPerSocket) {
        this.coresPerSocket = coresPerSocket;
    }
}
