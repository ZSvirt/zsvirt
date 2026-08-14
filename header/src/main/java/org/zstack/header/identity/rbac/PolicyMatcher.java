package org.zstack.header.identity.rbac;

import org.springframework.util.AntPathMatcher;

public class PolicyMatcher {
    private AntPathMatcher matcher = new AntPathMatcher();

    public boolean match(String policy, String path) {
        policy = policy.replaceAll("\\.", "/");
        path = path.replaceAll("\\.", "/");
        return matcher.match(policy, path) || path.endsWith(policy);
    }
}
