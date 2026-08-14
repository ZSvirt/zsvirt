package org.zstack.twoFactorAuthentication;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import edu.emory.mathcs.backport.java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.login.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AuthorizationManager;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

@InterceptorForService(TwoFactorAuthenticationConstant.SERVICE_ID)
public class TwoFactorAuthenticationManagerImpl extends AbstractService
        implements TwoFactorAuthenticationManager, LoginAuthExtensionPoint, ApiMessageInterceptor, AfterCreateAccountExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TwoFactorAuthenticationManagerImpl.class);

    private LoginAuthenticationProcedureDesc loginAuthenticationProcedureDesc;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, TwoFactorAuthenticationFactory> factories = new HashMap<>();

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIGetTwoFactorAuthenticationSecretMsg) {
            handle((APIGetTwoFactorAuthenticationSecretMsg) msg);
        } else if (msg instanceof APIGetTwoFactorAuthenticationStateMsg) {
            handle((APIGetTwoFactorAuthenticationStateMsg) msg);
        } else if (msg instanceof APIResetTwoFactorAuthenticationSecretMsg) {
            handle((APIResetTwoFactorAuthenticationSecretMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetTwoFactorAuthenticationStateMsg msg) {
        APIGetTwoFactorAuthenticationStateReply reply = new APIGetTwoFactorAuthenticationStateReply();

        if (TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            reply.setState(TwoFactorAuthenticationState.Enable.toString());
        } else {
            reply.setState(TwoFactorAuthenticationState.Disable.toString());
        }

        bus.reply(msg, reply);
    }

    private void handle(APIResetTwoFactorAuthenticationSecretMsg msg) {
        APIResetTwoFactorAuthenticationSecretEvent event = new APIResetTwoFactorAuthenticationSecretEvent(msg.getId());

        TwoFactorAuthenticationParamStruct param = TwoFactorAuthenticationParamStruct.fromApiMessage(msg);
        TwoFactorAuthenticationFactory factory = factories.get(msg.getType());
        if (factory == null) {
            logger.warn(String.format("unsupported two-factor authentication type: %s", msg.getType()));
            event.setError(err(IdentityErrors.AUTHENTICATION_ERROR, "two-factor authentication failed"));
            bus.publish(event);
            return;
        }

        TwoFactorAuthenticationStruct struct = factory.createAuthentication(param);
        if (struct == null) {
            event.setError(err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account name or password"));
            bus.publish(event);
            return;
        }

        SQL.New(TwoFactorAuthenticationSecretVO.class)
                .eq(TwoFactorAuthenticationSecretVO_.accountUuid, struct.getAccountUuid())
                .delete();
        TwoFactorAuthenticationSecretVO secretVO = new TwoFactorAuthenticationSecretVO();
        secretVO.setUuid(Platform.getUuid());
        secretVO.setAccountUuid(struct.getAccountUuid());
        secretVO.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated);
        secretVO.setAccountUuid(struct.getAccountUuid());
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        secretVO.setSecret(key.getKey());
        secretVO = dbf.persistAndRefresh(secretVO);

        TwoFactorAuthenticationSecretInventory inv = TwoFactorAuthenticationSecretInventory.valueOf(secretVO);
        event.setInventory(inv);

        bus.publish(event);
    }

    private void handle(APIGetTwoFactorAuthenticationSecretMsg msg) {
        APIGetTwoFactorAuthenticationSecretReply apiReply = new APIGetTwoFactorAuthenticationSecretReply();

        if (!TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            apiReply.setError(operr("two factor authenticator is not enabled"));
            bus.reply(msg, apiReply);
            return;
        }

        TwoFactorAuthenticationStruct struct = new TwoFactorAuthenticationStruct();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("two-factor-get-secret-flow");
        TwoFactorAuthenticationFactory factory = factories.get(msg.getType());
        if (factory == null) {
            logger.warn(String.format("unsupported two-factor authentication type: %s", msg.getType()));
            apiReply.setError(err(IdentityErrors.AUTHENTICATION_ERROR, "two-factor authentication failed"));
            bus.reply(msg, apiReply);
            return;
        }

        chain.then(new NoRollbackFlow() {
            String __name__ = "verify-account-login";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                LogInMsg logInMsg = new LogInMsg();
                logInMsg.setUsername(msg.getName());
                logInMsg.setPassword(msg.getPassword());
                logInMsg.setLoginType(factory.getLoginType());
                logInMsg.setCaptchaUuid(msg.getCaptchaUuid());
                logInMsg.setVerifyCode(msg.getVerifyCode());
                logInMsg.setValidateOnly(true);
                // two factor need to skip itself to get the secret
                logInMsg.setIgnoreAdditionalFeatures(Collections.singletonList(LoginAuthConstant.twoFactor));
                bus.makeTargetServiceIdByResourceUuid(logInMsg, LoginManager.SERVICE_ID, logInMsg.getUsername());
                bus.send(logInMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        LogInReply logInReply = reply.castReply();
                        struct.setAccountUuid(logInReply.getSession().getAccountUuid());
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-two-factor-auth-secret";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                TwoFactorAuthenticationSecretVO secretVO = Q.New(TwoFactorAuthenticationSecretVO.class)
                        .eq(TwoFactorAuthenticationSecretVO_.accountUuid, struct.getAccountUuid())
                        .find();
                if (secretVO == null) {
                    secretVO = new TwoFactorAuthenticationSecretVO();
                    secretVO.setUuid(Platform.getUuid());
                    secretVO.setAccountUuid(struct.getAccountUuid());
                    secretVO.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated);
                    secretVO.setAccountUuid(struct.getAccountUuid());
                    GoogleAuthenticator gAuth = new GoogleAuthenticator();
                    final GoogleAuthenticatorKey key = gAuth.createCredentials();
                    secretVO.setSecret(key.getKey());
                    secretVO = dbf.persistAndRefresh(secretVO);
                }

                TwoFactorAuthenticationSecretInventory inv = TwoFactorAuthenticationSecretInventory.valueOf(secretVO);
                if (Objects.equals(inv.getStatus(), TwoFactorAuthenticationSecretStatus.Logined.toString())) {
                    inv.setSecret("");
                }
                apiReply.setInventory(inv);
                trigger.next();
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, apiReply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                apiReply.setError(errCode);
                bus.reply(msg, apiReply);
            }
        }).start();
    }

    public String getId() {
        return bus.makeLocalServiceId(TwoFactorAuthenticationConstant.SERVICE_ID);
    }

    private void populateExtensions() {
        for (TwoFactorAuthenticationFactory ext : pluginRgty.getExtensionList(TwoFactorAuthenticationFactory.class)) {
            TwoFactorAuthenticationFactory old = factories.get(ext.getType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("Duplicate TwoFactorAuthenticationFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            factories.put(ext.getType(), ext);
        }
    }

    public boolean start() {
        try {
            setupCanonicalEvents();
            populateExtensions();
            initAdditionalAuthDesc();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        return true;
    }

    private void initAdditionalAuthDesc() {
        loginAuthenticationProcedureDesc = new LoginAuthenticationProcedureDesc();
        loginAuthenticationProcedureDesc.setOrder(1);
        loginAuthenticationProcedureDesc.setName(TwoFactorAuthenticationConstant.SERVICE_ID);
        loginAuthenticationProcedureDesc.addProperty(
                TwoFactorAuthenticationConstant.AUTHENTICATION_SOURCE,
                GoogleAuthenticator.class.getSimpleName());
    }

    public boolean stop() {
        return true;
    }

    private void updateTwoFactorAuthenticationSecretStatus(String accountUuid) {
        if (!TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            return;
        }

        TwoFactorAuthenticationSecretVO secretVO = Q.New(TwoFactorAuthenticationSecretVO.class)
                .eq(TwoFactorAuthenticationSecretVO_.accountUuid, accountUuid)
                .find();

        if (secretVO == null) {
            logger.error(String.format(
                    "failed to update TwoFactorAuthenticationSecretStatus: invalid account[uuid=%s]", accountUuid));
        } else {
            secretVO.setStatus(TwoFactorAuthenticationSecretStatus.Logined);
            dbf.update(secretVO);
        }
    }

    private void setupCanonicalEvents() {
        evtf.on(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, new EventCallback<IdentityCanonicalEvents.AccountDeletedData>() {
            @Override
            protected void run(Map tokens, IdentityCanonicalEvents.AccountDeletedData data) {
                SQL.New(TwoFactorAuthenticationSecretVO.class)
                        .eq(TwoFactorAuthenticationSecretVO_.accountUuid, data.getAccountUuid())
                        .delete();
            }
        });

        TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.installLocalUpdateExtension(
                this::updateTwoFactorAuthenticationSecret);
    }

    private void updateTwoFactorAuthenticationSecret(GlobalConfig oldConfig, GlobalConfig newConfig){
        SQL.New(TwoFactorAuthenticationSecretVO.class).delete();

        if (newConfig.value(Boolean.class)) {
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            List<TwoFactorAuthenticationSecretVO> secretVOS = new ArrayList<>();

            /* generate secret for account */
            List<String> allAccountUuidList = Q.New(AccountVO.class)
                    .select(AccountVO_.uuid)
                    .listValues();
            List<String> bypassAccountUuidList = Q.New(TwoFactorAuthenticationSecretVO.class)
                    .select(TwoFactorAuthenticationSecretVO_.accountUuid)
                    .listValues();
            allAccountUuidList.removeAll(bypassAccountUuidList);

            for (String accountUuid : allAccountUuidList) {
                GoogleAuthenticatorKey key = gAuth.createCredentials();
                TwoFactorAuthenticationSecretVO secretVO = new TwoFactorAuthenticationSecretVO();
                secretVO.setUuid(Platform.getUuid());
                secretVO.setAccountUuid(accountUuid);
                secretVO.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated);
                secretVO.setSecret(key.getKey());
                secretVOS.add(secretVO);
            }

            dbf.persistCollection(secretVOS);
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        return msg;
    }

    @Override
    public void afterCreateAccount(AccountInventory account) {
        if (!TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            return;
        }

        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        GoogleAuthenticatorKey key = gAuth.createCredentials();
        TwoFactorAuthenticationSecretVO secretVO = new TwoFactorAuthenticationSecretVO();
        secretVO.setUuid(Platform.getUuid());
        secretVO.setAccountUuid(account.getUuid());
        secretVO.setStatus(TwoFactorAuthenticationSecretStatus.NewCreated);
        secretVO.setSecret(key.getKey());

        dbf.persist(secretVO);
    }

    private ErrorCode createAdditionAuthErrorCode(String resourceUuid) {
        Map<String, String> properties = new HashMap<>();
        properties.put("credentials", resourceUuid);

        return AuthorizationManager.createAdditionAuthErrorCode(properties,
                TwoFactorAuthenticationConstant.GOOGLE_AUTHENTICATION_TYPE);
    }

    @Override
    public ErrorCode beforeExecuteLogin(LoginContext loginContext) {
        if (!TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            return null;
        }

        if (loginContext.getSystemTags() == null) {
            logger.warn("two factor authentication failed because there is no system tags in msg");
            return createAdditionAuthErrorCode(loginContext.getUsername());
        }

        String token = SystemTagUtils.findTagValue(loginContext.getSystemTags(), TwoFactorAuthenticationSystemTags.TWOFA_TOKEN,
                TwoFactorAuthenticationSystemTags.TWOFA_TOKEN_TOKEN);
        if (token == null) {
            logger.warn("two factor authentication failed because there is no token in msg system tag");
            return createAdditionAuthErrorCode(loginContext.getUsername());
        }

        TwoFactorAuthenticationSecretVO secretVO = Q.New(TwoFactorAuthenticationSecretVO.class)
                .eq(TwoFactorAuthenticationSecretVO_.accountUuid, loginContext.getAccountUuid())
                .find();
        if (secretVO == null) {
            logger.warn("two factor authentication failed: No secret for current account");
            return err(IdentityErrors.ADDITION_AUTHENTICATION_ERROR, "additional authentication failed");
        }

        String secret = secretVO.getSecret();
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        Integer itoken = Integer.valueOf(token);

        return gAuth.authorize(secret, itoken) ? null : err(IdentityErrors.ADDITION_AUTHENTICATION_FAIL, "additional authentication failed");
    }

    @Override
    public ErrorCode postLogin(LoginContext loginContext, LoginSessionInfo info) {
        if (!TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            return null;
        }

        if (loginContext.getSystemTags() == null) {
            return operr("two factor authentication failed because there is no system tags in msg");
        }

        String token = SystemTagUtils.findTagValue(loginContext.getSystemTags(), TwoFactorAuthenticationSystemTags.TWOFA_TOKEN,
                TwoFactorAuthenticationSystemTags.TWOFA_TOKEN_TOKEN);
        if (token == null) {
            return operr("two factor authentication failed because there is no token in msg system tag");
        }

        TwoFactorAuthenticationSecretVO secretVO = Q.New(TwoFactorAuthenticationSecretVO.class)
                .eq(TwoFactorAuthenticationSecretVO_.accountUuid, info.getAccountUuid())
                .find();
        if (secretVO == null) {
            return operr("two factor authentication failed because there is no secret for %s:%s",
                    loginContext.getLoginBackendType(),
                    info.getAccountUuid());
        }

        String secret = secretVO.getSecret();
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        Integer itoken = Integer.valueOf(token);

        if (!gAuth.authorize(secret, itoken)) {
            return operr("failed to verify two factor authentication code");
        }

        return null;
    }

    @Override
    public void afterLoginSuccess(LoginContext context, LoginSessionInfo info) {
        updateTwoFactorAuthenticationSecretStatus(info.getAccountUuid());
    }

    @Override
    public void afterLoginFailure(LoginContext loginContext, LoginSessionInfo info, ErrorCode errorCode) {

    }

    @Override
    public AdditionalAuthFeature getAdditionalAuthFeature() {
        return LoginAuthConstant.twoFactor;
    }

    @Override
    public LoginAuthenticationProcedureDesc getAdditionalAuthDesc(LoginContext loginContext) {
        if (!TwoFactorAuthenticationGlobalConfig.ENABLE_TWOFA_AUTH.value(Boolean.class)) {
            return null;
        }

        return loginAuthenticationProcedureDesc;
    }
}
