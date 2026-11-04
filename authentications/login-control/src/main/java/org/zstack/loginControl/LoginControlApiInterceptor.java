package org.zstack.loginControl;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.loginControl.api.APIAddAccessControlRuleMsg;
import org.zstack.loginControl.api.APIUpdateAccessControlRuleMsg;
import org.zstack.utils.IpRangeSet;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("loginControl")
public class LoginControlApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddAccessControlRuleMsg) {
            validate((APIAddAccessControlRuleMsg) msg);
        } else if (msg instanceof APIUpdateAccessControlRuleMsg) {
            validate(((APIUpdateAccessControlRuleMsg) msg));
        }

        return msg;
    }

    private void validate(APIUpdateAccessControlRuleMsg msg) {
        if (msg.getRule() == null) {
            return;
        }

        try {
            IpRangeSet.generateIpRangeSet(msg.getRule());
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("Invalid rule expression, add access control rule fail because: %s", e.getMessage()));
        }
    }

    private void validate(APIAddAccessControlRuleMsg msg) {
        try {
            IpRangeSet.generateIpRangeSet(msg.getRule());
        } catch (IllegalArgumentException e) {
            throw new ApiMessageInterceptionException(argerr("Invalid rule expression, add access control rule fail because: %s", e.getMessage()));
        }
    }
}
