package org.zstack.sso.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vo.ResourceVO;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO;
import org.zstack.identity.imports.entity.ThirdPartyAccountSourceVO_;
import org.zstack.identity.imports.message.DestroyThirdPartyAccountSourceMsg;
import org.zstack.sso.SSOConstants;
import org.zstack.sso.cas.header.CasAccountSourceSpec;
import org.zstack.sso.cas.message.CreateCasClientMsg;
import org.zstack.sso.header.*;
import org.zstack.sso.oauth2.AuthCache;
import org.zstack.sso.oauth2.compute.OAuth2AccountSource;
import org.zstack.sso.oauth2.header.OAuth2AccountSourceSpec;
import org.zstack.sso.oauth2.message.CreateOAuth2ClientMsg;
import org.zstack.sso.service.redirect.RedirectParamsBuilder;
import org.zstack.sso.service.redirect.UIRedirectUrl;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.ctl.ConfigureCommand;
import org.zstack.utils.ctl.ZStackCtlResult;
import org.zstack.utils.logging.CLogger;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.sso.SSOConstants.CAS_CLIENT_TYPE;
import static org.zstack.sso.SSOConstants.OAUTH2_CLIENT_TYPE;
import static org.zstack.utils.CollectionDSL.list;

/**
 * @Author: DaoDao
 * @Date: 2022/8/24
 */
public class SSOManagerImpl extends AbstractService implements SSOManager {
    private static final CLogger logger = Utils.getLogger(SSOManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacadeImpl evtf;
    @Autowired
    private PluginRegistry pluginRgty;

    private static final String iam2ModuleName = "project-management";

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateOAuthClientMsg) {
            handle((APICreateOAuthClientMsg) msg);
        } else if (msg instanceof APICreateCasClientMsg) {
            handle((APICreateCasClientMsg) msg);
        } else if (msg instanceof APIDeleteSSOClientMsg) {
            handle((APIDeleteSSOClientMsg) msg);
        } else if (msg instanceof APIGetOAuth2TokenMsg) {
            handle((APIGetOAuth2TokenMsg) msg);
        } else if (msg instanceof APIGetSSOClientMsg) {
            handle((APIGetSSOClientMsg) msg);
        } else if (msg instanceof APIGetOAuthClientSecretMsg) {
            handle((APIGetOAuthClientSecretMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }

    }

    private void handle(APIGetSSOClientMsg msg) {
        APIGetSSOClientReply reply = new APIGetSSOClientReply();
        List<ThirdPartyAccountSourceVO> clientVOS = Q.New(ThirdPartyAccountSourceVO.class)
                .in(ThirdPartyAccountSourceVO_.type, list(OAUTH2_CLIENT_TYPE, CAS_CLIENT_TYPE))
                .list();
        reply.setInventories(new ArrayList<>());

        for (ThirdPartyAccountSourceVO vo : clientVOS) {
            if (vo.getType().equals(OAUTH2_CLIENT_TYPE)) {
                reply.getInventories().add(OAuth2ClientInventory.valueOf((OAuth2ClientVO) vo));
            } else if (vo.getType().equals(CAS_CLIENT_TYPE)) {
                reply.getInventories().add(CasClientInventory.valueOf((CasClientVO) vo));
            }
        }

        bus.reply(msg, reply);
    }

    private void handle(APIGetOAuthClientSecretMsg msg) {
        APIGetOAuthClientSecretReply reply = new APIGetOAuthClientSecretReply();
        OAuth2ClientVO vo = dbf.findByUuid(msg.getUuid(), OAuth2ClientVO.class);
        if (vo == null) {
            bus.replyErrorByMessageType(msg, err(SysErrors.RESOURCE_NOT_FOUND,
                    "unable to find OAuth2 client[uuid=%s]", msg.getUuid()));
            return;
        }
        reply.setClientSecret(vo.getClientSecret());
        bus.reply(msg, reply);
    }

    private void handle(APIGetOAuth2TokenMsg msg) {
        APIGetOAuth2TokenReply reply = new APIGetOAuth2TokenReply();
        OAuth2TokenVO vo = Q.New(SSOTokenVO.class)
                .eq(SSOTokenVO_.userUuid, msg.getSession().getAccountUuid())
                .find();
        if (vo == null) {
            bus.replyErrorByMessageType(msg, err(SysErrors.RESOURCE_NOT_FOUND,
                    "unable to find oAuth2Token[userUuid=%s]", msg.getSession().getAccountUuid()));
            return;
        }
        reply.setInventory(OAuth2TokenInventory.valueOf(vo));
        bus.reply(msg, reply);
    }

    private void handle(APIDeleteSSOClientMsg msg) {
        DestroyThirdPartyAccountSourceMsg innerMsg = new DestroyThirdPartyAccountSourceMsg();
        innerMsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, msg.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APIDeleteSSOClientEvent event = new APIDeleteSSOClientEvent(msg.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APICreateCasClientMsg msg) {
        APICreateCasClientEvent event = new APICreateCasClientEvent(msg.getId());
        CasAccountSourceSpec spec = new CasAccountSourceSpec();
        spec.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        spec.setType(CAS_CLIENT_TYPE);
        spec.setName(msg.getName());
        spec.setDescription(msg.getDescription());
        spec.setCasServerLoginUrl(msg.getCasServerLoginUrl());
        spec.setCasServerUrlPrefix(msg.getCasServerUrlPrefix());
        spec.setServerName(msg.getServerName());
        spec.setLoginMNUrl(String.format(SSOConstants.CAS_LOGIN_URL, Platform.getManagementServerVip(),
                Platform.getManagementNodeServicePort(), spec.getUuid()));
        spec.setUrlTemplate(msg.getUrlTemplate());
        spec.setUsernameProperty(msg.getUsernameProperty());
        CreateCasClientMsg innerMsg = new CreateCasClientMsg();
        innerMsg.setSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, CasClientVO.class.getName());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                final CasClientVO client = dbf.findByUuid(spec.getUuid(), CasClientVO.class);
                event.setInventory(CasClientInventory.valueOf(client));
                bus.publish(event);
            }
        });
    }

    private ZStackCtlResult configureProperties(CasClientInventory inventory) {
        return new ConfigureCommand().Configure(String.format("%s=%s %s=%s %s=%s",
                SSOConstants.CAS_SERVER_LOGINURL_NAME, inventory.getCasServerLoginUrl(), SSOConstants.CAS_SERVER_URLPREFIX_NAME,
                inventory.getCasServerUrlPrefix(), SSOConstants.CAS_SERVER_NAME, inventory.getServerName())).exec();
    }

    private void handle(APICreateOAuthClientMsg msg) {
        APICreateOAuthClientEvent event = new APICreateOAuthClientEvent(msg.getId());
        OAuth2AccountSourceSpec spec = new OAuth2AccountSourceSpec();
        spec.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        spec.setType(OAUTH2_CLIENT_TYPE);
        spec.setName(msg.getName());
        spec.setDescription(msg.getDescription());
        spec.setClientId(msg.getClientId());
        spec.setClientSecret(msg.getClientSecret());
        spec.setAuthorizationUrl(msg.getAuthorizationUrl());
        spec.setTokenUrl(msg.getTokenUrl());
        spec.setUserInfoUrl(msg.getUserinfoUrl());
        spec.setRedirectUrl(msg.getRedirectUrl());
        spec.setGrantType(msg.getGrantType());
        spec.setLogoutUrl(msg.getLogoutUrl());
        spec.setLoginMNUrl(String.format(SSOConstants.OAUTH2_LOGIN_URL,
                OAuth2AccountSource.getIpPort(msg.getRedirectUrl()), spec.getUuid()));
        spec.setUrlTemplate(msg.getUrlTemplate());
        spec.setUsernameProperty(msg.getUsernameProperty());
        CreateOAuth2ClientMsg innerMsg = new CreateOAuth2ClientMsg();
        innerMsg.setSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, OAuth2ClientVO.class.getName());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    final OAuth2ClientVO client = dbf.findByUuid(spec.getUuid(), OAuth2ClientVO.class);
                    event.setInventory(OAuth2ClientInventory.valueOf(client));
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
             }
        });
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }


    @Override
    public String getId() {
        return bus.makeLocalServiceId(SSOConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        setCasClientDetailsStatus();
        setupCanonicalEvents();
        return true;
    }

    private void setupCanonicalEvents() {
        evtf.on(SSOCanonicalEvents.CAS_MODIFY_CONFIGURE_PATH, new EventCallback<SSOCanonicalEvents.CasModifyConfigureData>() {
            @Override
            protected void run(Map tokens, SSOCanonicalEvents.CasModifyConfigureData data) {
                if (CoreGlobalProperty.UNIT_TEST_ON) {
                    logger.debug("ignore /sso/cas/configure event for unit test");
                    return;
                }
                configureProperties(data.getInventory());
            }
        });
    }


    private void setCasClientDetailsStatus() {
        CasClientVO vo = Q.New(CasClientVO.class).find();

        if (vo == null) {
            return;
        }

        if (!vo.getCasServerLoginUrl().equals(Platform.getGlobalProperty(SSOConstants.CAS_SERVER_LOGINURL_NAME))
            || !vo.getCasServerUrlPrefix().equals(Platform.getGlobalProperty(SSOConstants.CAS_SERVER_URLPREFIX_NAME))
            || !vo.getServerName().equals(Platform.getGlobalProperty(SSOConstants.CAS_SERVER_NAME))) {
            vo.setState(CasState.Disabled);
            dbf.update(vo);
            return;
        }

        vo.setState(CasState.Enabled);
        dbf.update(vo);
    }

    @Override
    public boolean stop() {
        return true;
    }


    @Override
    public void redirect(HttpServletRequest request, HttpServletResponse response, SessionInventory session,
                         String clientUuid, AuthCache cache) {

        Query q = dbf.getEntityManager().createNativeQuery("select uuid, resourceName, resourceType from ResourceVO where uuid = :uuid");
        q.setParameter("uuid", session.getAccountUuid());
        List<Object[]> objs = q.getResultList();

        List<ResourceVO> vos = objs.stream().map(ResourceVO::new).collect(Collectors.toList());

        DebugUtils.Assert(vos.size() == 1, String.format("Account[uuid:%s] is not exists", session.getAccountUuid()));

        ResourceVO resourceVO = vos.get(0);

        RedirectParamsBuilder builder = null;
        try {
            builder = new RedirectParamsBuilder()
                    .setAccountUuid(session.getAccountUuid())
                    .setUserUuid(session.getAccountUuid())
                    .setUserName(URLEncoder.encode(resourceVO.getResourceName(), "UTF-8"))
                    .setSessionId(session.getUuid());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        if (cache.getRedirectUrl() != null) {
            builder.setRedirect(cache.getRedirectUrl());
        }

        if (cache.getZoneUuid() != null) {
            builder.setZoneUUid(cache.getZoneUuid());
        }

        if (cache.getProjectUuid() != null) {
            builder.setProjectUuid(cache.getProjectUuid());
        }

        String templateUuid = null;

        if (Q.New(SSORedirectTemplateVO.class)
                .eq(SSORedirectTemplateVO_.clientUuid, clientUuid)
                .isExists()) {
            templateUuid = Q.New(SSORedirectTemplateVO.class)
                    .eq(SSORedirectTemplateVO_.clientUuid, clientUuid)
                    .select(SSORedirectTemplateVO_.uuid)
                    .findValue();
        }

        String uiRedirect = new UIRedirectUrl().getRedirectUrl(builder.build(), templateUuid);

        if (logger.isTraceEnabled()) {
            logger.trace("response url :" + uiRedirect);
        }

        try {
            response.sendRedirect(uiRedirect);
        } catch (IOException e) {
            throw new OperationFailureException(operr("response has error : %s", e));
        }
    }
}
