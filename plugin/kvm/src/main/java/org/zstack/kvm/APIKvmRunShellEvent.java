package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/3/14.
 */
@RestResponse(allTo = "inventory")
public class APIKvmRunShellEvent extends APIEvent {
    public static class ShellResult {
        private int returnCode;
        private String stdout;
        private String stderr;
        private ErrorCode errorCode;

        public int getReturnCode() {
            return returnCode;
        }

        public void setReturnCode(int returnCode) {
            this.returnCode = returnCode;
        }

        public String getStdout() {
            return stdout;
        }

        public void setStdout(String stdout) {
            this.stdout = stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public void setStderr(String stderr) {
            this.stderr = stderr;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }
    }

    public APIKvmRunShellEvent() {
    }

    public APIKvmRunShellEvent(String apiId) {
        super(apiId);
    }

    private Map<String, ShellResult> inventory = new HashMap<String, ShellResult>();

    public Map<String, ShellResult> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, ShellResult> inventory) {
        this.inventory = inventory;
    }

    public static APIKvmRunShellEvent __example__() {
        APIKvmRunShellEvent event = new APIKvmRunShellEvent();
        ShellResult sr = new ShellResult();
        sr.setErrorCode(new ErrorCode());
        sr.setReturnCode(100);
        sr.setStderr(null);
        sr.setStdout("hello");
        Map<String, ShellResult> inventory = new HashMap<String, ShellResult>();
        inventory.put(uuid(), sr);
        event.setInventory(inventory);
        return event;
    }

}
