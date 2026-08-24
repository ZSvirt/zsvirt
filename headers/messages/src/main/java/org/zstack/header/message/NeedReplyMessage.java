package org.zstack.header.message;

import org.zstack.header.rest.APINoSee;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public abstract class NeedReplyMessage extends Message {
    /**
     * @desc in millisecond. Any reply/event received after timeout will be dropped
     * @optional
     */
    @APINoSee
    protected long timeout = -1;
    @APINoSee
    protected long messageDeadline = -1;
    protected List<String> systemTags;
    protected List<String> userTags;

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }

    public boolean hasSystemTag(Predicate<String> isMatch) {
        return systemTags != null && systemTags.stream().anyMatch(isMatch);
    }

    public void addSystemTag(String systemTag){
        if (systemTags == null) {
            systemTags = new ArrayList<>();
        }

        systemTags.add(systemTag);
    }

    public List<String> getUserTags() {
        return userTags;
    }

    public void setUserTags(List<String> userTags) {
        this.userTags = userTags;
    }

    public NeedReplyMessage() {
        super();
    }

    public NeedReplyMessage(long timeout) {
        super();
    }

    public String toErrorString() {
        return String.format("Message[name: %s, id: %s] timeout after %s seconds",
                this.getClass().getName(),
                this.getId(),
                TimeUnit.MILLISECONDS.toSeconds(getTimeout()));
    }

    public boolean hasSystemTag(String tag) {
        if (systemTags == null) {
            return false;
        }

        return systemTags.contains(tag);
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getMessageDeadline() {
        return messageDeadline;
    }

    public void setMessageDeadline(long messageDeadline) {
        this.messageDeadline = messageDeadline;
    }
}
