package org.zstack.tag2;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.tag.UserTagInventory;

public class AttachTagResult {
    private ErrorCode error;
    private UserTagInventory inventory;
    private boolean success = true;

    AttachTagResult() {

    }

    AttachTagResult(ErrorCode error) {
        this.error = error;
        this.success = error == null;
    }

    AttachTagResult(UserTagInventory inventory) {
        this.inventory = inventory;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.success = error == null;
        this.error = error;
    }

    public UserTagInventory getInventory() {
        return inventory;
    }

    public void setInventory(UserTagInventory inventory) {
        this.inventory = inventory;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
