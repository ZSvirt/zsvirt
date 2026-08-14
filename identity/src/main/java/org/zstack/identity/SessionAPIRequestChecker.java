package org.zstack.identity;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.*;

public class SessionAPIRequestChecker implements APIRequestChecker {
    @Override
    public void check(APIMessage message) {
        if (message.getSession() == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "session of message[%s] is null", message.getMessageName()));
        }

        if (message.getSession().getUuid() == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "session uuid is null"));
        }

        Session.errorOnTimeout(message.getSession().getUuid());
        message.setSession(Session.getSession(message.getSession().getUuid()));
    }
}
