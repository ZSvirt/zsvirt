package org.zstack.utils.ssh;

import org.zstack.utils.opaque.OpaqueScripts;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_EXCEPTION;

public class SshException extends RuntimeException implements OpaqueScripts {
    private SshResult result;

    public SshException(String msg) {
        super(msg);
    }

    public SshException(String msg, Throwable t) {
        super(msg, t);
    }

    public SshException(Throwable t) {
        super(t);
    }

    public SshResult getResult() {
        return result;
    }

    public SshException withResult(SshResult result) {
        this.result = result;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> opaqueScripts() {
        if (result != null) {
            return result.opaqueScripts();
        }
        return map(e(OPAQUE_KEY_EXCEPTION, getMessage()));
    }
}
