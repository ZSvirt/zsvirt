package org.zstack.utils;

import org.zstack.utils.opaque.OpaqueScripts;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_CMD;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_CODE;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_ERROR;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_BASH_OUTPUT;

/**
 */
public class ShellResult implements OpaqueScripts {
    private int retCode;
    private String stderr;
    private String stdout;
    private String desensitizeCmd;

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

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> opaqueScripts() {
        return map(
            e(OPAQUE_KEY_BASH_CMD, desensitizeCmd),
            e(OPAQUE_KEY_BASH_CODE, retCode),
            e(OPAQUE_KEY_BASH_OUTPUT, stdout),
            e(OPAQUE_KEY_BASH_ERROR, stderr)
        );
    }

    public void raiseExceptionIfFail() {
        raiseExceptionIfNotMatch(0);
    }

    public void raiseExceptionIfNotMatch(int expectedRetCode) {
        if (retCode == expectedRetCode) {
            return;
        }

        if (stderr != null && stderr.contains("Account expired")) {
            throw new ShellUtils.ShellException(
                    String.format("local account '%s' has expired", System.getProperty("user.name")))
                    .withResult(this);
        }
        throw new ShellUtils.ShellException("failed to execute shell command").withResult(this);
    }

    public String getDesensitizeCmd() {
        return desensitizeCmd;
    }

    public void setDesensitizeCmd(String desensitizeCmd) {
        this.desensitizeCmd = desensitizeCmd;
    }

    public boolean isReturnCode(int code) {
        return retCode == code;
    }
}
