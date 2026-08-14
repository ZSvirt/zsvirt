package org.zstack.identity.rbac;

import org.zstack.header.identity.role.RolePolicyVO;

/**
 * May use it to check API call permissions
 */
public interface ResourcePolicyChecker {
    boolean matchResources(RolePolicyVO policy);
}
