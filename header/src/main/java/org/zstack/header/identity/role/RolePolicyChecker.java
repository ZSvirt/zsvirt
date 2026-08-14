package org.zstack.header.identity.role;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

public interface RolePolicyChecker {
    /**
     * check and merge role policies
     */
    ErrorCode checkRolePolicies(List<RolePolicyStatement> policies);
}
