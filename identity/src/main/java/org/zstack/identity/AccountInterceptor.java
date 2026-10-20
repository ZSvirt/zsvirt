package org.zstack.identity;

import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APICheckPasswordMessage;
import org.zstack.header.message.APIMessage;

import java.util.Collections;
import java.util.List;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.err;

/**
 * Created by MaJin on 2019/7/4.
 */
public class AccountInterceptor implements GlobalApiMessageInterceptor {
    @Override
    public List<Class> getMessageClassToIntercept() {
        return Collections.singletonList(APICheckPasswordMessage.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICheckPasswordMessage) {
            validate((APICheckPasswordMessage) msg);
        }
        return msg;
    }

    private void validate(APICheckPasswordMessage msg) {
        SessionInventory session = Session.getSession(msg.getSession().getUuid());
        if (session == null) {
            throw new ApiMessageInterceptionException(err(IdentityErrors.INVALID_SESSION,
                    "Session expired"));
        }

        String accountUuid = session.getAccountUuid();
        boolean correct = Q.New(AccountVO.class)
                .eq(AccountVO_.uuid, accountUuid)
                .eq(AccountVO_.password, msg.getPassword())
                .isExists();
        if (!correct) {
            throw new ApiMessageInterceptionException(argerr("wrong password"));
        }
    }
}
