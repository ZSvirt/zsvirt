package org.zstack.sdk.identity.imports.header;



public class ImportStage  {

    public int total;
    public void setTotal(int total) {
        this.total = total;
    }
    public int getTotal() {
        return this.total;
    }

    public int success;
    public void setSuccess(int success) {
        this.success = success;
    }
    public int getSuccess() {
        return this.success;
    }

    public int fail;
    public void setFail(int fail) {
        this.fail = fail;
    }
    public int getFail() {
        return this.fail;
    }

}
