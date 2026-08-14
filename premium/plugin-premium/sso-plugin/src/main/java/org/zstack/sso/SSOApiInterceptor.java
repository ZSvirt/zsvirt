package org.zstack.sso;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;
import org.zstack.sso.header.APICreateSSORedirectTemplateMsg;
import org.zstack.sso.header.APIDeleteSSOClientMsg;
import org.zstack.sso.header.APIDeleteSSORedirectTemplateMsg;
import org.zstack.sso.header.APIUpdateCasClientMsg;
import org.zstack.sso.header.APIUpdateOAuthClientMsg;
import org.zstack.sso.header.APIUpdateSSORedirectTemplateMsg;
import org.zstack.sso.header.SSORedirectTemplateVO;
import org.zstack.sso.header.SSORedirectTemplateVO_;

import static org.zstack.core.Platform.*;
import static org.zstack.sso.SSOConstants.*;
import static org.zstack.utils.CollectionDSL.*;

@InterceptorForService("sso")
public class SSOApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUpdateSSORedirectTemplateMsg) {
            validate((APIUpdateSSORedirectTemplateMsg) msg);
        } else if (msg instanceof APICreateSSORedirectTemplateMsg) {
            validate((APICreateSSORedirectTemplateMsg) msg);
        } else if (msg instanceof APIDeleteSSORedirectTemplateMsg) {
            validate((APIDeleteSSORedirectTemplateMsg) msg);
        } else if (msg instanceof APIUpdateOAuthClientMsg) {
            validate((APIUpdateOAuthClientMsg) msg);
        } else if (msg instanceof APIUpdateCasClientMsg) {
            validate((APIUpdateCasClientMsg) msg);
        } else if (msg instanceof APIDeleteSSOClientMsg) {
            validate((APIDeleteSSOClientMsg) msg);
        }
        return msg;
    }

    private void validate(APIUpdateSSORedirectTemplateMsg msg) {
        String accountSourceUuid = Q.New(SSORedirectTemplateVO.class)
                .eq(SSORedirectTemplateVO_.uuid, msg.getUuid())
                .select(SSORedirectTemplateVO_.clientUuid)
                .findValue();
        msg.setAccountSourceUuid(accountSourceUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, accountSourceUuid);
    }

    private void validate(APIDeleteSSORedirectTemplateMsg msg) {
        String accountSourceUuid = Q.New(SSORedirectTemplateVO.class)
                .eq(SSORedirectTemplateVO_.uuid, msg.getUuid())
                .select(SSORedirectTemplateVO_.clientUuid)
                .findValue();
        msg.setAccountSourceUuid(accountSourceUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, accountSourceUuid);
    }

    private void validate(APICreateSSORedirectTemplateMsg msg) {
        bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, msg.getSourceUuid());
    }

    private void validate(APIUpdateOAuthClientMsg msg) {
        bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, msg.getSourceUuid());
    }

    private void validate(APIUpdateCasClientMsg msg) {
        bus.makeTargetServiceIdByResourceUuid(msg, AccountImportsConstant.SERVICE_ID, msg.getSourceUuid());
    }

    private void validate(APIDeleteSSOClientMsg msg) {
        String type = Q.New(ThirdPartyAccountSourceVO.class)
                .eq(ThirdPartyAccountSourceVO_.uuid, msg.getUuid())
                .select(ThirdPartyAccountSourceVO_.type)
                .findValue();
        boolean support = list(CAS_CLIENT_TYPE, OAUTH2_CLIENT_TYPE).contains(type);
        if (!support) {
            throw new ApiMessageInterceptionException(
                    operr("SSO client type[%s] not support for DeleteSSOClientAction", type));
        }
    }
}