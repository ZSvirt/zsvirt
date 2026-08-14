package org.zstack.header.exception;

import org.zstack.utils.opaque.OpaqueScripts;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_EXCEPTION;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_PARAMETER_NAME;

public class CloudInvalidParameterException extends CloudRuntimeException implements CloudException, OpaqueScripts {
    private static final long serialVersionUID = SerialVersionUID.CloudInvalidParameterException;

    public final String parameterName;
    public final String extensionMessage;

    public CloudInvalidParameterException(String parameterName, String value, String msg) {
        super(new StringBuilder("[Invalid Parameter: ").append(parameterName).append(" has value ").append(value).append("]: ").append(msg).toString());
        this.parameterName = parameterName;
        this.extensionMessage = msg;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> opaqueScripts() {
        return map(
            e(OPAQUE_KEY_PARAMETER_NAME, parameterName),
            e(OPAQUE_KEY_EXCEPTION, extensionMessage)
        );
    }
}
