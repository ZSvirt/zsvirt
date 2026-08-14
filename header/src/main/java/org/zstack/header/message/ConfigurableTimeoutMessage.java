package org.zstack.header.message;

public interface ConfigurableTimeoutMessage {
    long getTimeout();

    void setTimeout(long timeout);

    long getMessageDeadline();

    void setMessageDeadline(long messageDeadline);
}
