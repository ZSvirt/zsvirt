package org.zstack.header.exception;

public class CloudStateMachineException extends CloudRuntimeException {
    private static final long serialVersionUID = SerialVersionUID.CloudStateMachineException;

    public CloudStateMachineException(String msg) {
        super(msg);
    }

    public CloudStateMachineException(String msg, Exception e) {
        super(msg, e);
    }
}
