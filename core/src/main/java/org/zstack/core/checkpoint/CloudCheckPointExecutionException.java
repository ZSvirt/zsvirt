package org.zstack.core.checkpoint;

import org.zstack.header.exception.SerialVersionUID;

public class CloudCheckPointExecutionException extends Exception {
    private static final long serialVersionUID = SerialVersionUID.CloudCheckPointExecutionException;
    private final String checkPointUuid;
    
    CloudCheckPointExecutionException(String uuid, Throwable t) {
        super(t);
        this.checkPointUuid = uuid;
    }

    public String getCheckPointUuid() {
        return checkPointUuid;
    }
}
