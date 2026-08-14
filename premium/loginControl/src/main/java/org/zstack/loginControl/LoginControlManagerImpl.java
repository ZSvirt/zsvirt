package org.zstack.loginControl;

import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.config.APIQueryGlobalConfigMsg;
import org.zstack.core.db.*;
import org.zstack.core.thread.SyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.captcha.Captcha;
import org.zstack.header.core.captcha.CaptchaVO;
import org.zstack.header.core.captcha.CaptchaVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.login.*;
import org.zstack.header.license.LicenseMessage;
import org.zstack.header.managementnode.APIQueryManagementNodeMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.PasswordUpdateExtensionPoint;
import org.zstack.loginControl.api.*;
import org.zstack.loginControl.entity.*;
import org.zstack.rest.RestConstants;
import org.zstack.rest.RestServer;
import org.zstack.rest.RestServletRequestInterceptor;
import org.zstack.utils.IpRangeSet;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.loginControl.LoginControlErrors.REQUEST_REFUSED_ERROR;

/**
 * Created by kayo on 2018/7/9.
 */
public class LoginControlManagerImpl extends AbstractService implements LoginControlManager, LoginAuthExtensionPoint
        , GlobalApiMessageInterceptor, PasswordUpdateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LoginControlManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private Captcha captcha;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacadeImpl evtf;
    @Autowired
    private RestServer restServer;
    @Autowired
    private LoginManager loginManager;

    private static final List<LoginControlOperation> loginControlOperations = new ArrayList<>();

    public boolean start() {
        loginControlOperations.add(new LoginControlLockIdentityOperation());
        loginControlOperations.add(new LoginControlCaptchaOperation());
        loginControlOperations.add(new LoginControlForcePasswordChangeOperation());

        setupCanonicalEvents();
        registerRestInterceptor();
        installGlobalConfigExtensions();

        return true;
    }

    private void installGlobalConfigExtensions() {
        PasswordStrategyGlobalConfig.PASSWORD_STRENGTH_CHECK_CONFIG.installValidateExtension((category, name, oldValue, newValue) -> {
            PasswordStrengthConfig config;

            List<String> supportedFields = Arrays.stream(PasswordStrengthConfig.class.getDeclaredFields())
                    .filter(field -> !field.isSynthetic()) //open jacoco, PasswordStrengthConfig.class will have synthetic members
                    .map(Field::getName)
                    .collect(Collectors.toList());

            try {
                JSONObject jsonObject = new JSONObject(newValue);
                Iterator<String> jsonKeyIterator = jsonObject.keys();
                while (jsonKeyIterator.hasNext()) {
                    String key = jsonKeyIterator.next();

                    if (!supportedFields.contains(key)) {
                        throw new ApiMessageInterceptionException(argerr("unrecognized key: %s", key));
                    }
                }

                Optional opt = supportedFields.stream().filter(f -> !jsonObject.has(f)).findFirst();
                if (opt.isPresent()) {
                    throw new ApiMessageInterceptionException(argerr("missing key:value of %s", opt.get()));
                }

                config = JSONObjectUtil.toObject(newValue, PasswordStrengthConfig.class);
            } catch (JSONException | JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("wrong format of password strength config"));
            }

            if (config.getMinimum().compareTo(config.getMaximum()) > 0) {
                throw new ApiMessageInterceptionException(argerr("minimum can not be larger than maximum"));
            }
        });
    }

    private void registerRestInterceptor() {
        restServer.registerRestServletRequestInterceptor(new RestServletRequestInterceptor() {
            @Override
            public void intercept(HttpServletRequest req) throws RestServletRequestInterceptorException {
                if (!AccessControlGlobalConfig.ENABLE_REQUEST_SOURCE_IP_ADDRESS_CHECK.value(Boolean.class)) {
                    return;
                }

                String srcIp = req.getHeader(RestConstants.HEADER_REQUEST_IP);
                ErrorCode error = validateIpAddress(srcIp);
                if (error != null) {
                    throw new RestServletRequestInterceptor.RestServletRequestInterceptorException(HttpStatus.FORBIDDEN.value(), error.toString());
                }
            }

            private ErrorCode validateIpAddress(String srcIp) {
                if (srcIp == null) {
                    return null;
                }

                if (!NetworkUtils.isIpv4Address(srcIp)) {
                    return err(REQUEST_REFUSED_ERROR,"The visitor ip[%s] is not an IPv4 address", srcIp);
                }

                Long srcIpInLong = NetworkUtils.ipv4StringToLong(srcIp);
                List<String> acceptRules = Q.New(AccessControlRuleVO.class)
                        .select(AccessControlRuleVO_.rule)
                        .eq(AccessControlRuleVO_.strategy, ControlStrategy.ACCEPT)
                        .listValues();

                for (String rule : acceptRules) {
                    IpRangeSet set = IpRangeSet.generateIpRangeSet(rule);

                    if (!set.contains(srcIpInLong)) {
                        continue;
                    }

                    return null;
                }

                List<String> rejectRules = Q.New(AccessControlRuleVO.class)
                        .select(AccessControlRuleVO_.rule)
                        .eq(AccessControlRuleVO_.strategy, ControlStrategy.REJECT)
                        .listValues();

                for (String rule : rejectRules) {
                    IpRangeSet set = IpRangeSet.generateIpRangeSet(rule);

                    if (!set.contains(srcIpInLong)) {
                        continue;
                    }

                    return err(REQUEST_REFUSED_ERROR,"Request from [%s] is refused", srcIp);
                }

                return null;
            }
        });
    }

    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetLoginCaptchaMsg) {
            handle((APIGetLoginCaptchaMsg) msg);
        } else if (msg instanceof APIUnlockIdentityMsg) {
            handle((APIUnlockIdentityMsg) msg);
        } else if (msg instanceof APIAddAccessControlRuleMsg) {
            handle((APIAddAccessControlRuleMsg) msg);
        } else if (msg instanceof APIDeleteAccessControlRuleMsg) {
            handle((APIDeleteAccessControlRuleMsg) msg);
        } else if (msg instanceof APIUpdateAccessControlRuleMsg) {
            handle((APIUpdateAccessControlRuleMsg) msg);
        } else if (msg instanceof APIValidatePasswordMsg) {
            handle((APIValidatePasswordMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIValidatePasswordMsg msg) {
        APIValidatePasswordReply reply = new APIValidatePasswordReply();

        if (msg.getLoginType() == null || msg.getLoginType().isEmpty()) {
            reply.setError(err(IdentityErrors.AUTHENTICATION_ERROR, "AccountType cannot be empty"));
            bus.reply(msg, reply);
            return;
        }

        LoginBackend backend = loginManager.getLoginBackend(msg.getLoginType());
        reply.setAvailable(backend.authenticate(msg.getLoginName(), msg.getPassword()));
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateAccessControlRuleMsg msg) {
        APIUpdateAccessControlRuleEvent evt = new APIUpdateAccessControlRuleEvent(msg.getId());
        AccessControlRuleVO vo = dbf.findByUuid(msg.getUuid(), AccessControlRuleVO.class);

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        if (msg.getRule() != null) {
            vo.setRule(msg.getRule());
        }

        vo = dbf.updateAndRefresh(vo);
        evt.setInventory(AccessControlRuleInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteAccessControlRuleMsg msg) {
        APIDeleteAccessControlRuleEvent evt = new APIDeleteAccessControlRuleEvent(msg.getId());
        dbf.removeByPrimaryKey(msg.getUuid(), AccessControlRuleVO.class);
        bus.publish(evt);
    }

    private void handle(APIAddAccessControlRuleMsg msg) {
        APIAddAccessControlRuleEvent evt = new APIAddAccessControlRuleEvent(msg.getId());
        AccessControlRuleVO vo = new AccessControlRuleVO();

        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }

        vo.setName(msg.getName());
        vo.setRule(msg.getRule());
        vo.setDescription(msg.getDescription());
        vo.setStrategy(ControlStrategy.valueOf(msg.getControlStrategy()));
        vo = dbf.persistAndRefresh(vo);

        evt.setInventory(AccessControlRuleInventory.valueOf(vo));
        bus.publish(evt);
    }

    @SyncThread(signature = "unlock-identity")
    private void handle(APIUnlockIdentityMsg msg) {
        APIUnlockIdentityReply reply = new APIUnlockIdentityReply();
        if (!PasswordStrategyGlobalConfig.ENABLE_LOCK_LOGIN_ATTEMPTS_MAXIMUM.value(Boolean.class)) {
            bus.reply(msg, reply);
            return;
        }

        LoginBackend loginBackend = loginManager.getLoginBackend(msg.getLoginType());
        String targetResourceIdentity = loginBackend.getAccountIdByName(msg.getResourceName());

        if (targetResourceIdentity == null) {
            throw new OperationFailureException(operr("No available user with name: %s, type: %s",
                    msg.getResourceName(), msg.getLoginType()));
        }

        SQL.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .set(LoginAttemptsVO_.locked, false)
                .set(LoginAttemptsVO_.attempts, 0)
                .set(LoginAttemptsVO_.unlockDate, new Timestamp(new Date().getTime()))
                .update();

        bus.reply(msg, reply);
    }

    private boolean isAttemptRecordExists(String targetResourceIdentity) {
        return !Q.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .isExists();
    }

    private boolean isUserLocked(String targetResourceIdentity) {
        return Q.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .eq(LoginAttemptsVO_.locked, true).isExists();
    }

    private ErrorCode checkUserLocked(String targetResourceIdentity) {
        if (!PasswordStrategyGlobalConfig.ENABLE_LOCK_LOGIN_ATTEMPTS_MAXIMUM.value(Boolean.class)
                || !isUserLocked(targetResourceIdentity)) {
            return null;
        }

        Timestamp timestamp = Q.New(LoginAttemptsVO.class)
                .select(LoginAttemptsVO_.unlockDate)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .findValue();

        return err(LoginControlErrors.IDENTITY_LOCKED_ERROR, "this user is locked, remaining time: %s seconds", TimeUnit.MILLISECONDS.toSeconds(timestamp.getTime() - System.currentTimeMillis()));
    }

    private CaptchaVO getCaptcha(String targetResourceIdentity, String captchaUuid) {
        CaptchaVO vo;
        if (captchaUuid != null) {
            vo = Q.New(CaptchaVO.class)
                    .eq(CaptchaVO_.uuid, captchaUuid)
                    .eq(CaptchaVO_.targetResourceIdentity, targetResourceIdentity)
                    .find();
        } else {
            vo = captcha.generateCaptcha(targetResourceIdentity);
        }

        return vo;
    }

    /**
     * This method should not return any message related to existence of username
     * to avoid enumeration attack
     */
    @SyncThread(signature="login-control")
    private CaptchaVO getResourceCaptcha(String resourceName, String loginType, String captchaUuid) {
        if (!LoginControlGlobalConfig.LOGIN_CONTROL.value(Boolean.class)) {
            return null;
        }

        String targetResourceIdentity = loginManager.getUserIdByName(resourceName,
                loginType);
        if (targetResourceIdentity == null) {
            logger.debug(String.format("No available user with name: %s type: %s", resourceName,
                    loginType));
            return null;
        }

        // check if user have any attempt, if no means no login failure before
        // just return success
        if (isAttemptRecordExists(targetResourceIdentity)) {
            return null;
        }

        // if user already locked, report user locked error
        ErrorCode errorCode = checkUserLocked(targetResourceIdentity);
        if (errorCode != null) {
            throw new OperationFailureException(errorCode);
        }

        int attempts = Q.New(LoginAttemptsVO.class)
                .select(LoginAttemptsVO_.attempts)
                .eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity)
                .findValue();
        // attempts still below the maximum, no need to generate or return captcha
        if (attempts < LoginControlGlobalConfig.LOGIN_ATTEMPTS_MAXIMUM.value(Long.class)) {
            return null;
        }

        return getCaptcha(targetResourceIdentity, captchaUuid);
    }

    private void handle(APIGetLoginCaptchaMsg msg) {
        APIGetLoginCaptchaReply reply = new APIGetLoginCaptchaReply();

        CaptchaVO vo = getResourceCaptcha(msg.getResourceName(), msg.getLoginType(),
                msg.getCaptchaUuid());
        if (vo == null) {
            logger.debug(String.format("can not get suitable captcha with[uuid:%s], " +
                    "related to resourceName[uuid:%s]", msg.getCaptchaUuid(),
                    msg.getResourceName()));
            bus.reply(msg, reply);
            return;
        }

        reply.setCaptcha(vo.getCaptcha());
        reply.setCaptchaUuid(vo.getUuid());
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LoginControlConstants.SERVICE_ID);
    }

    private void setupCanonicalEvents() {
        evtf.on(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                IdentityCanonicalEvents.AccountDeletedData d = (IdentityCanonicalEvents.AccountDeletedData) data;
                SQL.New(LoginAttemptsVO.class).eq(LoginAttemptsVO_.targetResourceIdentity, d.getAccountUuid()).delete();
                SQL.New(HistoricalPasswordVO.class).eq(HistoricalPasswordVO_.uuid, d.getAccountUuid()).delete();
            }
        });
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return null;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (!PasswordStrategyGlobalConfig.ENABLE_FORCE_CHANGE_PASSWORD_PERIOD.value(Boolean.class)
                || msg instanceof APIUpdateAccountMsg
                || msg instanceof APISessionMessage
                || msg instanceof APIQueryGlobalConfigMsg
                || msg instanceof APIQueryManagementNodeMsg
                || msg instanceof LicenseMessage) {
            return msg;
        }

        SessionInventory session = msg.getSession();

        if (session == null) {
            return msg;
        }

        if (Q.New(LoginAttemptsVO.class)
                .eq(LoginAttemptsVO_.forceChangePassword, true)
                .eq(LoginAttemptsVO_.targetResourceIdentity, session.getAccountUuid())
                .isExists()) {
            throw new ApiMessageInterceptionException(err(LoginControlErrors.FORCE_CHANGE_PASSWORD_ERROR, "Need to change password"));
        }

        return msg;
    }

    @Override
    public void preUpdatePassword(String accountUuid, String currentPassword, String newPassword) {
        if (!PasswordStrategyGlobalConfig.ENABLE_HISTORICAL_PASSWORD_COMPARE.value(Boolean.class)) {
            return;
        }

        Integer limit = PasswordStrategyGlobalConfig.HISTORICAL_PASSWORD_NUM.value(Integer.class);

        List<String> latestPasswords = Q.New(HistoricalPasswordVO.class)
                .select(HistoricalPasswordVO_.password)
                .eq(HistoricalPasswordVO_.uuid, accountUuid)
                .orderBy(HistoricalPasswordVO_.id, SimpleQuery.Od.DESC)
                .limit(limit)
                .listValues();
        if (currentPassword.equals(newPassword) || latestPasswords.contains(newPassword)) {
            throw new OperationFailureException(err(LoginControlErrors.HISTORICAL_PASSWORD_REPEATED_ERROR, "Password repeated, please use another one"));
        }
    }

    @Override
    public void afterUpdatePassword(String accountUuid, String password) {
        persistHistoricalPasswordInQueue(accountUuid, password);
    }

    @SyncThread(signature = "persist-historical-password")
    private void persistHistoricalPasswordInQueue(String accountUuid, String password) {
        logger.debug(String.format("password of %s changed, no need to force change it next time, persist the old password to DB", accountUuid));

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(LoginAttemptsVO.class)
                        .set(LoginAttemptsVO_.forceChangePassword, false)
                        .eq(LoginAttemptsVO_.targetResourceIdentity, accountUuid).update();

                HistoricalPasswordVO passwordVO = new HistoricalPasswordVO();
                passwordVO.setUuid(accountUuid);
                passwordVO.setPassword(password);
                persist(passwordVO);
            }
        }.execute();
    }

    @Override
    public ErrorCode beforeExecuteLogin(LoginContext loginContext) {
        // auth params not supported
        if (loginContext.getAccountUuid() == null && loginContext.getLastUpdatedTime() == null) {
            logger.debug(String.format("userUuid: %s, lastUpdatedTime: %s, parameters missing, skip pre check",
                    loginContext.getAccountUuid(), loginContext.getLastUpdatedTime()));
            return null;
        }

        String targetResourceIdentity = loginContext.getAccountUuid();

        LoginStruct struct = new LoginStruct();
        struct.setTargetResourceIdentity(targetResourceIdentity);
        struct.setCaptchaUuid(loginContext.getCaptchaUuid());
        struct.setCaptchaCode(loginContext.getVerifyCode());
        struct.setLastUpdatedTime(loginContext.getLastUpdatedTime());

        synchronized (this) {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    if (!q(LoginAttemptsVO.class).eq(LoginAttemptsVO_.targetResourceIdentity, targetResourceIdentity).isExists()) {
                        // if no record persist one
                        LoginAttemptsVO lvo = new LoginAttemptsVO();
                        lvo.setUuid(Platform.getUuid());
                        lvo.setTargetResourceIdentity(targetResourceIdentity);
                        lvo.setAttempts(0);
                        lvo.setForceChangePassword(false);
                        persist(lvo);
                        flush();

                        for (LoginControlOperation operation : loginControlOperations) {
                            operation.prepare(targetResourceIdentity);
                        }
                    }

                }
            }.execute();
        }

        for (LoginControlOperation operation : loginControlOperations) {
            ErrorCode err = operation.preLoginCheck(struct);

            if (err != null) {
                return err;
            }
        }

        return null;
    }

    @Override
    public ErrorCode postLogin(LoginContext loginContext, LoginSessionInfo info) {
        return null;
    }

    @Override
    public void afterLoginSuccess(LoginContext context, LoginSessionInfo info) {
        handleLoginSuccess(context, info);
    }

    @Override
    public void afterLoginFailure(LoginContext loginContext, LoginSessionInfo info, ErrorCode errorCode) {
        LoginStruct struct = new LoginStruct();
        struct.setTargetResourceIdentity(loginContext.getAccountUuid());
        struct.setCaptchaUuid(loginContext.getCaptchaUuid());
        struct.setCaptchaCode(loginContext.getVerifyCode());
        struct.setLastUpdatedTime(loginContext.getLastUpdatedTime());

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (struct.getTargetResourceIdentity() == null) {
                    return;
                }

                int attempts = increaseLoginAttemptsCount();

                for (LoginControlOperation operation : loginControlOperations) {
                    operation.loginFail(struct, attempts);
                }
            }

            private int increaseLoginAttemptsCount() {
                sql("Update LoginAttemptsVO set attempts = attempts + 1 WHERE targetResourceIdentity = :targetResourceIdentity")
                        .param("targetResourceIdentity", struct.getTargetResourceIdentity())
                        .execute();

                return q(LoginAttemptsVO.class).select(LoginAttemptsVO_.attempts)
                        .eq(LoginAttemptsVO_.targetResourceIdentity, struct.getTargetResourceIdentity())
                        .findValue();
            }
        }.execute();
    }

    @Override
    public AdditionalAuthFeature getAdditionalAuthFeature() {
        return LoginAuthConstant.basicLoginControl;
    }

    @Override
    public LoginAuthenticationProcedureDesc getAdditionalAuthDesc(LoginContext loginContext) {
        if (!LoginControlGlobalConfig.LOGIN_CONTROL.value(Boolean.class)) {
            return null;
        }

        LoginAuthenticationProcedureDesc loginAuthenticationProcedureDesc;
        loginAuthenticationProcedureDesc = new LoginAuthenticationProcedureDesc();
        loginAuthenticationProcedureDesc.setName(LoginControlConstants.SERVICE_ID);

        CaptchaVO vo = getResourceCaptcha(loginContext.getUsername(), loginContext.getLoginBackendType(), loginContext.getCaptchaUuid());

        if (vo == null) {
            loginAuthenticationProcedureDesc.addProperty("captchaUuid", Strings.EMPTY);
            loginAuthenticationProcedureDesc.addProperty("captcha", Strings.EMPTY);
        } else {
            loginAuthenticationProcedureDesc.addProperty("captchaUuid", vo.getUuid());
            loginAuthenticationProcedureDesc.addProperty("captcha", vo.getCaptcha());
        }

        return loginAuthenticationProcedureDesc;
    }

    private void handleLoginSuccess(LoginContext context, LoginSessionInfo info) {
        LoginStruct struct = new LoginStruct();
        struct.setTargetResourceIdentity(context.getAccountUuid());
        struct.setLastUpdatedTime(context.getLastUpdatedTime());

        new SQLBatch() {
            @Override
            protected void scripts() {
                resetFailureAttempts();

                for (LoginControlOperation operation : loginControlOperations) {
                    operation.loginSuccess(struct);
                }
            }

            private void resetFailureAttempts() {
                sql(LoginAttemptsVO.class)
                        .set(LoginAttemptsVO_.attempts, 0)
                        .eq(LoginAttemptsVO_.targetResourceIdentity, struct.getTargetResourceIdentity())
                        .update();
            }
        }.execute();
    }
}
