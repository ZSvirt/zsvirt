package org.zstack.sdk.attribute.entity;

import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.attribute.entity.ResourceAttributeValueInventory;

public class CreateResourceAttributeResult  {

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

    public ResourceAttributeValueInventory inventory;
    public void setInventory(ResourceAttributeValueInventory inventory) {
        this.inventory = inventory;
    }
    public ResourceAttributeValueInventory getInventory() {
        return this.inventory;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

}
