package org.zstack.sso.oauth2.compute;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.Message;
import org.zstack.sso.header.APIUpdateOAuthClientEvent;
import org.zstack.sso.header.APIUpdateOAuthClientMsg;
import org.zstack.sso.header.OAuth2ClientInventory;
import org.zstack.sso.header.OAuth2ClientVO;
import org.zstack.sso.oauth2.header.OAuth2AccountSourceSpec;
import org.zstack.sso.service.compute.SSOAccountSource;

import java.net.MalformedURLException;
import java.net.URL;

import static org.zstack.core.Platform.operr;
import static org.zstack.identity.imports.AccountImportsManager.accountSourceQueueSyncSignature;
import static org.zstack.sso.SSOConstants.*;

public class OAuth2AccountSource extends SSOAccountSource {
    protected OAuth2AccountSource(OAuth2ClientVO self) {
        super(self);
    }

    private OAuth2ClientVO refreshVO() {
        final OAuth2ClientVO clientVO = databaseFacade.findByUuid(self.getUuid(), OAuth2ClientVO.class);
        if (clientVO == null) {
            throw new OperationFailureException(operr("oAuth2Client[uuid:%s, name:%s] has been deleted",
                    self.getUuid(), self.getResourceName()));
        }
        self = clientVO;
        return clientVO;
    }

    @Override
    public String type() {
        return OAUTH2_CLIENT_TYPE;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateOAuthClientMsg) {
            handle((APIUpdateOAuthClientMsg) msg);
        } else {
            super.handleMessage(msg);
        }
    }

    private void handle(APIUpdateOAuthClientMsg message) {
        APIUpdateOAuthClientEvent event = new APIUpdateOAuthClientEvent(message.getId());
        final String sourceUuid = message.getSourceUuid();

        OAuth2AccountSourceSpec spec = new OAuth2AccountSourceSpec();
        spec.setUuid(message.getUuid());
        spec.setName(message.getName());
        spec.setDescription(message.getDescription());
        spec.setClientId(message.getClientId());
        spec.setClientSecret(message.getClientSecret());
        spec.setAuthorizationUrl(message.getAuthorizationUrl());
        spec.setTokenUrl(message.getTokenUrl());
        spec.setUserInfoUrl(message.getUserinfoUrl());
        spec.setLogoutUrl(message.getLogoutUrl());
        spec.setUsernameProperty(message.getUsernameProperty());

        if (message.getRedirectUrl() != null) {
            spec.setRedirectUrl(message.getRedirectUrl());
            spec.setLoginMNUrl(String.format(OAUTH2_LOGIN_URL, getIpPort(message.getRedirectUrl()), spec.getUuid()));
        }

        threadFacade.chainSubmit(new ChainTask(message) {
            @Override
            public void run(SyncTaskChain chain) {
                final OAuth2ClientInventory inventory = updateOAuth2Client(spec);
                chain.next();
                event.setInventory(inventory);
                bus.publish(event);
            }

            @Override
            public String getSyncSignature() {
                return accountSourceQueueSyncSignature(sourceUuid);
            }

            @Override
            public String getName() {
                return "update-oauth2-client-" + sourceUuid;
            }
        });
    }

    protected OAuth2ClientInventory updateOAuth2Client(OAuth2AccountSourceSpec spec) {
        final OAuth2ClientVO vo = refreshVO();

        if (spec.getName() != null) {
            vo.setResourceName(spec.getName());
        }
        if (spec.getDescription() != null) {
            vo.setDescription(spec.getDescription());
        }
        if (spec.getClientId() != null) {
            vo.setClientId(spec.getClientId());
        }
        if (spec.getClientSecret() != null) {
            vo.setClientSecret(spec.getClientSecret());
        }
        if (spec.getAuthorizationUrl() != null) {
            vo.setAuthorizationUrl(spec.getAuthorizationUrl());
        }
        if (spec.getTokenUrl() != null) {
            vo.setTokenUrl(spec.getTokenUrl());
        }
        if (spec.getUserInfoUrl() != null) {
            vo.setUserinfoUrl(spec.getUserInfoUrl());
        }
        if (spec.getLogoutUrl() != null) {
            vo.setLogoutUrl(spec.getLogoutUrl());
        }
        if (spec.getRedirectUrl() != null) {
            vo.setRedirectUrl(spec.getRedirectUrl());
        }
        if (spec.getLoginMNUrl() != null) {
            vo.setLoginMNUrl(spec.getLoginMNUrl());
        }
        if (spec.getUsernameProperty() != null) {
            vo.setUsernameProperty(spec.getUsernameProperty());
        }

        return OAuth2ClientInventory.valueOf(databaseFacade.updateAndRefresh(vo));
    }

    public static String getIpPort(String redirectUrl) {
        String ssoIp = Platform.getManagementServerVip();
        Integer ssoPort = Platform.getManagementNodeServicePort();

        if (!StringUtils.isEmpty(redirectUrl)) {
            try {
                URL url = new URL(redirectUrl);
                ssoIp = url.getHost();
                ssoPort = url.getPort();
            } catch (MalformedURLException e) {
                throw new OperationFailureException(operr("redirectUrl is error, %s", redirectUrl));
            }
        }

        if (ssoPort == -1) {
            return ssoIp;
        }

        return String.format("%s:%s", ssoIp, ssoPort);
    }

    @Override
    protected void destroySource(Completion completion) {
        databaseFacade.removeByPrimaryKey(self.getUuid(), OAuth2ClientVO.class);
        completion.success();
    }
}