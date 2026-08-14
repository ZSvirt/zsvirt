package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class ShellResult  {

    public int returnCode;
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }
    public int getReturnCode() {
        return this.returnCode;
    }

    public java.lang.String stdout;
    public void setStdout(java.lang.String stdout) {
        this.stdout = stdout;
    }
    public java.lang.String getStdout() {
        return this.stdout;
    }

    public java.lang.String stderr;
    public void setStderr(java.lang.String stderr) {
        this.stderr = stderr;
    }
    public java.lang.String getStderr() {
        return this.stderr;
    }

    public ErrorCode errorCode;
    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
    public ErrorCode getErrorCode() {
        return this.errorCode;
    }

}
