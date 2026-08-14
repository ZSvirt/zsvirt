package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.UserTagInventory;

public class AttachTagResult  {

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

    public UserTagInventory inventory;
    public void setInventory(UserTagInventory inventory) {
        this.inventory = inventory;
    }
    public UserTagInventory getInventory() {
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
