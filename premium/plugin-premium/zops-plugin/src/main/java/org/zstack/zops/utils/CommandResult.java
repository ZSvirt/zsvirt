package org.zstack.zops.utils;

public class CommandResult {
    private int retCode = 0;
    private String stderr = "";
    private String stdout = "";
    private String desensitizeCmd;

    public CommandResult(String stdout) {
        this.stdout = stdout;
    }

    public CommandResult() {
    }

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getDesensitizeCmd() {
        return desensitizeCmd;
    }

    public void setDesensitizeCmd(String desensitizeCmd) {
        this.desensitizeCmd = desensitizeCmd;
    }

    public String getExecutionLog() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\nshell command[%s]", desensitizeCmd));
        sb.append(String.format("\nret code: %s", retCode));
        sb.append(String.format("\nstderr: %s", stderr));
        sb.append(String.format("\nstdout: %s", stdout));
        return sb.toString();
    }

    public Boolean isSuccess() {
        return retCode == 0;
    }
}
