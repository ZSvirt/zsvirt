package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.CarrierMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.TimeUnit;

/**
 */
public class VirtualRouterAsyncHttpCallMsg extends NeedReplyMessage implements VmInstanceMessage, CarrierMessage {
    private String vmInstanceUuid;
    private String path;
    private String command;
    private boolean checkStatus;
    private String commandClassName;

    public String getCommandClassName() {
        return commandClassName;
    }

    public boolean isCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(boolean checkStatus) {
        this.checkStatus = checkStatus;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(Object cmd) {
        setCommandByString(JSONObjectUtil.toJsonString(cmd));
        commandClassName = cmd.getClass().getName();
    }

    public void setCommandByString(String command) {
        this.command = command;
    }
}
