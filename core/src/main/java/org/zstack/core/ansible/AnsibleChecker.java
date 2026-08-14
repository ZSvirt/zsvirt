package org.zstack.core.ansible;

import org.zstack.header.errorcode.ErrorCode;

/**
 */
public interface AnsibleChecker {
    boolean needDeploy();

    void deleteDestFile();

    default ErrorCode stopAnsible() {
        return null;
    }
}
