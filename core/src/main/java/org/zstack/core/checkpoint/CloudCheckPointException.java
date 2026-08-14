package org.zstack.core.checkpoint;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.exception.SerialVersionUID;

public class CloudCheckPointException extends CloudRuntimeException {
    private static final long serialVersionUID = SerialVersionUID.CloudCheckPointException;
    
    public CloudCheckPointException(String err, Throwable cause) {
        super(err, cause);
    }
    
    public CloudCheckPointException(String err) {
        super(err);
    }
}
