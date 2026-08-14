package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class ChangeHostStateMsg extends NeedReplyMessage implements HostMessage {
    private String uuid;
    private String stateEvent;
    private boolean forceChange;

    // different behaviours for APIChangeHostStateMsg and ChangeHostStateMsg
    private boolean justChangeState = true;

    public ChangeHostStateMsg() {
    }

    public ChangeHostStateMsg(String uuid, String stateEvent) {
        this.uuid = uuid;
        this.stateEvent = stateEvent;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }

    public boolean isJustChangeState() {
        return justChangeState;
    }

    public void setJustChangeState(boolean justChangeState) {
        this.justChangeState = justChangeState;
    }

    public boolean isForceChange() {
        return forceChange;
    }

    public void setForceChange(boolean forceChange) {
        this.forceChange = forceChange;
    }
}
