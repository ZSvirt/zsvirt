package org.zstack.sso.oauth2.compute;

import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.header.AbstractAccountSourceSpec;
import org.zstack.sso.header.OAuth2ClientVO;
import org.zstack.sso.header.OAuth2ClientVO_;
import org.zstack.sso.oauth2.header.OAuth2AccountSourceSpec;
import org.zstack.sso.service.compute.SSOAccountSourceFactory;

import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.sso.SSOConstants.OAUTH2_CLIENT_TYPE;

public class OAuth2AccountSourceFactory extends SSOAccountSourceFactory {
    @Override
    public String type() {
        return OAUTH2_CLIENT_TYPE;
    }

    @Override
    public OAuth2AccountSource createBase(ThirdPartyAccountSourceVO vo) {
        final OAuth2ClientVO oAuth2ClientVO = (vo instanceof OAuth2ClientVO) ?
                (OAuth2ClientVO) vo :
                databaseFacade.findByUuid(vo.getUuid(), OAuth2ClientVO.class);
        if (oAuth2ClientVO == null) {
            throw new OperationFailureException(operr("unable to find OAuth2 client[uuid=%s]", vo.getUuid()));
        }
        return new OAuth2AccountSource(oAuth2ClientVO);
    }

    protected OAuth2ClientVO generateAccountSourceVO(AbstractAccountSourceSpec rawSpec) {
        OAuth2AccountSourceSpec spec = (OAuth2AccountSourceSpec) rawSpec;

        OAuth2ClientVO clientVO = new OAuth2ClientVO();
        clientVO.setUuid(spec.getUuid());
        clientVO.setResourceName(spec.getName());
        clientVO.setDescription(spec.getDescription());
        clientVO.setType(OAUTH2_CLIENT_TYPE);
        clientVO.setCreateAccountStrategy(spec.getCreatedAccountStrategy());
        clientVO.setDeleteAccountStrategy(spec.getDeleteAccountStrategy());

        clientVO.setLoginMNUrl(spec.getLoginMNUrl());
        clientVO.setRedirectUrl(spec.getRedirectUrl());
        clientVO.setClientId(spec.getClientId());
        clientVO.setClientSecret(spec.getClientSecret());
        clientVO.setAuthorizationUrl(spec.getAuthorizationUrl());
        clientVO.setTokenUrl(spec.getTokenUrl());
        clientVO.setGrantType(spec.getGrantType());
        clientVO.setUserinfoUrl(spec.getUserInfoUrl());
        clientVO.setLogoutUrl(spec.getLogoutUrl());
        clientVO.setUsernameProperty(spec.getUsernameProperty());
        return clientVO;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Flow buildSSOUrlTemplateFlow(Context context) {
        OAuth2AccountSourceSpec spec = (OAuth2AccountSourceSpec) context.spec;

        return new NoRollbackFlow() {
            String __name__ = "add-sso-url-template";

            @Override
            public boolean skip(Map data) {
                return spec.getUrlTemplate() == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                OAuth2ClientVO client = (OAuth2ClientVO) context.vo;
                createBase(client).createSSORedirectTemplate(spec.getUrlTemplate());
                trigger.next();
            }
        };
    }

    @Override
    protected ErrorCode checkSameSSOServerExists(AbstractAccountSourceSpec rawSpec) {
        OAuth2AccountSourceSpec spec = (OAuth2AccountSourceSpec) rawSpec;
        boolean duplicate = Q.New(OAuth2ClientVO.class)
                .eq(OAuth2ClientVO_.authorizationUrl, spec.getAuthorizationUrl())
                .isExists();
        return duplicate ?
                operr("duplicate oauth2 server[authorizationUrl=%s]", spec.getAuthorizationUrl()) :
                null;
    }
}