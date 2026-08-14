package org.zstack.ovf.datatype;

/**
 * Created by Qi Le on 2022/3/4
 */
public class OvfMemoryInfo {
    private String instanceId;
    private Long quantity;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}
