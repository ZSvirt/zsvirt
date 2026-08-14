package org.zstack.accessKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.Account;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 * Created by shixin on 11/12/18.
 */
@InterceptorForService("accessKey")
public class AccessKeyApiInterceptor implements ApiMessageInterceptor {

    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateAccessKeyMsg) {
            validate((APICreateAccessKeyMsg) msg);
        } else if (msg instanceof APIDeleteAccessKeyMsg) {
            validate((APIDeleteAccessKeyMsg) msg);
        } else if (msg instanceof APIChangeAccessKeyStateMsg) {
            validate((APIChangeAccessKeyStateMsg) msg);
        }

        return msg;
    }

    private void validateAccount(SessionInventory session, String accountUuid) {
        if (Account.isAdminPermission(session)) {
            return;
        }

        if (!session.getAccountUuid().equals(accountUuid)) {
            throw new ApiMessageInterceptionException(argerr(
                    "No permission to modify the AccessKey for current account"));
        }
    }

    private void validate(APICreateAccessKeyMsg msg) {
        if (msg.getAccessKeyID() != null || msg.getAccessKeySecret() != null) {
            if (msg.getAccessKeySecret() == null || msg.getAccessKeyID() == null) {
                throw new ApiMessageInterceptionException(argerr("If a specified Accesskey is expected, the AccesskeyId and the AccesskeySecret must be provided at the same time."));
            }
        }

        if (msg.getAccessKeyID() != null && msg.getAccessKeyID().contains(":") ||
                msg.getAccessKeySecret() != null && msg.getAccessKeySecret().contains(":")) {
            throw new ApiMessageInterceptionException(argerr("Access key ID and secret cannot contain ':'"));
        }

        /* there is no limitation on SystemAdmin for accessKey */
        if (Account.isAdminPermission(msg.getAccountUuid())) {
            return;
        }

        long count = Q.New(AccessKeyVO.class)
                .eq(AccessKeyVO_.accountUuid, msg.getAccountUuid())
                .count();
        if (count >= AccessKeyConstant.ACCESSKEY_QUOTA) {
            throw new ApiMessageInterceptionException(argerr(
                    "The number of access keys for account[uuid=%s] has exceeded the maximum limit",
                    msg.getAccountUuid()));
        }
    }

    private void validate(APIDeleteAccessKeyMsg msg) {
        final List<String> accountUuidList = Q.New(AccessKeyVO.class)
                .eq(AccessKeyVO_.uuid, msg.getUuid())
                .select(AccessKeyVO_.accountUuid)
                .listValues();

        if (accountUuidList.isEmpty()) {
            return;
        }

        validateAccount(msg.getSession(), accountUuidList.get(0));
    }

    private void validate(APIChangeAccessKeyStateMsg msg) {
        final List<String> accountUuidList = Q.New(AccessKeyVO.class)
                .eq(AccessKeyVO_.uuid, msg.getUuid())
                .select(AccessKeyVO_.accountUuid)
                .listValues();
        validateAccount(msg.getSession(), accountUuidList.get(0));
    }
}
