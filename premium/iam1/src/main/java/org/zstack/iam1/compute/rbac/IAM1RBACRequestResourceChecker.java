package org.zstack.iam1.compute.rbac;

import org.zstack.header.identity.role.RolePolicyResourceRefVO;
import org.zstack.header.identity.role.RolePolicyVO;
import org.zstack.identity.rbac.ResourcePolicyChecker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IAM1RBACRequestResourceChecker implements ResourcePolicyChecker {
    Map<String, Class<?>> uuidTypeMapNeedCheck = new HashMap<>();

    public boolean matchResources(RolePolicyVO policy) {
        Set<String> uuidSet = uuidTypeMapNeedCheck.entrySet().stream()
                .filter(entry -> entry.getValue().getSimpleName().equals(policy.getResourceType()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (RolePolicyResourceRefVO ref : policy.getResourceRefs()) {
            uuidSet.remove(ref.getResourceUuid());

            if (uuidSet.isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
