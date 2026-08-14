package org.zstack.identity.rbac;

import org.zstack.header.message.APIMessage;

import java.util.Collection;

public interface ResourcePolicyCheckerFactory {
    public ResourcePolicyChecker build(Collection<APIMessage.FieldParam> fieldParams, APIMessage message);
}
