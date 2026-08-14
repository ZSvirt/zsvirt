package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class TrashCleanupResult  {

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public boolean success;
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return this.success;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

    public long trashId;
    public void setTrashId(long trashId) {
        this.trashId = trashId;
    }
    public long getTrashId() {
        return this.trashId;
    }

    public java.lang.Long size;
    public void setSize(java.lang.Long size) {
        this.size = size;
    }
    public java.lang.Long getSize() {
        return this.size;
    }

}
