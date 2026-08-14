package org.zstack.network.service.flat;

import java.util.Arrays;
import java.util.List;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.identity.AccountManager;
import org.zstack.identity.rbac.AccessibleResourceChecker;

import static org.zstack.core.Platform.operr;

/**
 * Created by Qi Le on 2019/9/9
 */
public class FlatApiInterceptor implements GlobalApiMessageInterceptor {

    @Autowired
    private AccountManager accountManager;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetL3NetworkIpStatisticMsg) {
            validate((APIGetL3NetworkIpStatisticMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetL3NetworkIpStatisticMsg msg) {
        if (accountManager.isAdmin(msg.getSession())) {
            return;
        }
        String accountUuid = msg.getSession().getAccountUuid();
        if (StringUtils.isBlank(accountUuid)) {
            throw new ApiMessageInterceptionException(Platform.argerr("Session/account uuid is not valid."));
        }

        final boolean accessible = AccessibleResourceChecker.forAccount(accountUuid)
                .allowGlobalReadableResource()
                .withResourceType(L3NetworkVO.class)
                .isAccessible(msg.getL3NetworkUuid());

        if (!accessible) {
            throw new ApiMessageInterceptionException(
                    operr("the account[uuid:%s] has no access to the resource[uuid:%s, type:L3NetworkVO]",
                    accountUuid, msg.getL3NetworkUuid()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIGetL3NetworkIpStatisticMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
