package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.CarrierMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.gson.JSONObjectUtil;

public abstract class KVMHostHttpCallMsg extends NeedReplyMessage implements HostMessage, CarrierMessage {
    private String path;
    @NoLogging(behavior = NoLogging.Behavior.Auto, classNameField = "commandClassName")
    private String command;
    private String hostUuid;
    private boolean noStatusCheck;
    private String commandClassName;

    public String getCommandClassName() {
        return commandClassName;
    }

    public boolean isNoStatusCheck() {
        return noStatusCheck;
    }

    public void setNoStatusCheck(boolean noStatusCheck) {
        this.noStatusCheck = noStatusCheck;
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

    public void setCommand(Object command) {
        this.command = JSONObjectUtil.toJsonString(command);
        commandClassName = command.getClass().getName();
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
