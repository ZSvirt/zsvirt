package org.zstack.identity.login;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APILogInReply;
import org.zstack.header.identity.APISessionMessage;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.login.*;
import org.zstack.header.message.Message;
import org.zstack.identity.Session;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class LoginManagerImpl extends AbstractService implements LoginManager {
    private static final CLogger logger = Utils.getLogger(LoginManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    private final Map<String, LoginBackend> loginBackends = new HashMap<>();
    private final Map<String, LoginAPIAdapter> loginAPIAdapters = new HashMap<>();
    private final List<LoginAuthExtensionPoint> loginAuthExtensionPoints = new ArrayList<>();

    @Override
    public LoginBackend getLoginBackend(String loginType) {
        LoginBackend loginBackend = loginBackends.get(loginType);

        if (loginBackend == null) {
            throw new OperationFailureException(operr("unsupported login type %s", loginType));
        }

        return loginBackend;
    }

    @Override
    public String getUserIdByName(String username, String loginType) {
        LoginBackend loginBackend = getLoginBackend(loginType);

        String userId = loginBackend.getAccountIdByName(username);
        if (userId != null) {
            return userId;
        }

        return String.format(AccountConstant.NO_EXIST_ACCOUNT, username);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APILogInMsg) {
            handle((APILogInMsg) msg);
        } else if (msg instanceof LogInMsg) {
            handle((LogInMsg) msg);
        } else if (msg instanceof APIGetLoginProceduresMsg) {
            handle((APIGetLoginProceduresMsg) msg);
        } else if (msg instanceof APISessionMessage) {
            handle((APISessionMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetLoginProceduresMsg msg) {
        APIGetLoginProceduresReply reply = new APIGetLoginProceduresReply();

        LoginContext loginContext = LoginContext.fromAPIGetLoginProceduresMsg(msg);
        LoginBackend loginBackend = getLoginBackend(msg.getLoginType());
        loginContext.setPossibleUsersGetter(loginBackend::possibleAccountUuidSetForGettingProcedures);
        List<LoginAuthExtensionPoint> matchedAuthExtensions =
                getBackendSupportedLoginAuthExtension(
                        loginBackend.getRequiredAdditionalAuthFeature(),
                        new ArrayList<>());

        List<LoginAuthenticationProcedureDesc> descList = matchedAuthExtensions
                .stream()
                .map(ext -> ext.getAdditionalAuthDesc(loginContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        reply.setProcedures(descList);
        bus.reply(msg, reply);
    }

    private void handle(APISessionMessage msg) {
         LoginAPIAdapter apiAdapter = loginAPIAdapters.get(msg.getClass().getCanonicalName());

        if (apiAdapter == null) {
            logger.debug(String.format("no LoginAPIAdapter found for msg: %s", msg.getClass().getCanonicalName()));
            bus.dealWithUnknownMessage(msg);
            return;
        }

        APILogInMsg apiLogInMsg = apiAdapter.transferToAPILogInMsg(msg);
        LoginContext loginContext = LoginContext.fromApiLoginMessage(apiLogInMsg);
        APILogInReply reply = new APILogInReply();
        doLogIn(loginContext, new ReturnValueCompletion<SessionInventory>(msg) {
            @Override
            public void success(SessionInventory session) {
                reply.setInventory(session);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(LogInMsg msg) {
        LoginContext loginContext = LoginContext.fromLoginMessage(msg);
        LogInReply reply = new LogInReply();
        doLogIn(loginContext, new ReturnValueCompletion<SessionInventory>(msg) {
            @Override
            public void success(SessionInventory session) {
                reply.setSession(session);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APILogInMsg msg) {
        LoginContext loginContext = LoginContext.fromApiLoginMessage(msg);
        APILogInReply reply = new APILogInReply();
        doLogIn(loginContext, new ReturnValueCompletion<SessionInventory>(msg) {
            @Override
            public void success(SessionInventory session) {
                msg.setSession(session);
                reply.setInventory(session);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private List<LoginAuthExtensionPoint> getBackendSupportedLoginAuthExtension(List<AdditionalAuthFeature> types, List<AdditionalAuthFeature> ignoreFeatures) {
        if (types == null || types.isEmpty()) {
            return new ArrayList<>();
        }

        return loginAuthExtensionPoints
                .stream()
                .filter(ext -> types.contains(ext.getAdditionalAuthFeature())
                        && !ignoreFeatures.contains(ext.getAdditionalAuthFeature()))
                .collect(Collectors.toList());
    }

    private void doLogIn(LoginContext loginContext, ReturnValueCompletion<SessionInventory> completion) {
        LoginBackend loginBackend = getLoginBackend(loginContext.getLoginBackendType());
        List<LoginAuthExtensionPoint> matchedAuthExtensions =
                getBackendSupportedLoginAuthExtension(
                        loginBackend.getRequiredAdditionalAuthFeature(),
                        loginContext.getIgnoreFeatures());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        loginBackend.collectUserInfoIntoContext(loginContext);

        if (loginContext.getAccountUuid() != null) {
            logger.debug(String.format("login account[uuid=%s]", loginContext.getAccountUuid()));
        } else {
            logger.debug("no account uuid exists, treat this login as a not existing account login operation");
            loginContext.setAccountUuid(String.format(AccountConstant.NO_EXIST_ACCOUNT,
                    loginContext.getUsername()));
        }

        chain.setName(String.format("login-%s-with-backend-%s", loginContext.getUsername(), loginContext.getLoginBackendType()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "run-before-login-extensions";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (LoginAuthExtensionPoint ext : matchedAuthExtensions) {
                    ErrorCode errorCode = ext.beforeExecuteLogin(loginContext);

                    if (errorCode != null) {
                        trigger.fail(errorCode);
                        return;
                    }
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "process-login";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                loginBackend.login(loginContext, new ReturnValueCompletion<LoginSessionInfo>(trigger) {
                    @Override
                    public void success(LoginSessionInfo info) {
                        data.put(LoginAuthConstant.LOGIN_SESSION_INFO, info);

                        ErrorCode errorCode = runPostLoginExtensions(loginContext, info);
                        if (errorCode != null) {
                            trigger.fail(errorCode);
                            return;
                        }

                        SessionInventory session = processSession(info);
                        data.put(LoginAuthConstant.LOGIN_SESSION_INVENTORY, session);

                        for (LoginAuthExtensionPoint ext : matchedAuthExtensions) {
                            ext.afterLoginSuccess(loginContext, info);
                        }

                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }

                    private ErrorCode runPostLoginExtensions(LoginContext context, LoginSessionInfo info) {
                        for (LoginAuthExtensionPoint ext : matchedAuthExtensions) {
                            ErrorCode errorCode = ext.postLogin(loginContext, info);

                            if (errorCode == null) {
                                continue;
                            }

                            return errorCode;
                        }

                        return null;
                    }

                    private SessionInventory processSession(LoginSessionInfo info) {
                        if (info.isLogoutOperatorSession()) {
                            Session.logout(loginContext.getOperatorSession().getUuid());
                        }

                        SessionInventory session;
                        // manually create a SessionInventory as validation result
                        if (loginContext.isValidateOnly()) {
                            session = new SessionInventory();
                            session.setAccountUuid(info.getAccountUuid());
                        } else {
                            session = Session.login(info.getAccountUuid());
                        }

                        return session;
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success((SessionInventory) data.get(LoginAuthConstant.LOGIN_SESSION_INVENTORY));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                LoginSessionInfo info = (LoginSessionInfo) data.get(LoginAuthConstant.LOGIN_SESSION_INFO);
                for (LoginAuthExtensionPoint ext : matchedAuthExtensions) {
                    ext.afterLoginFailure(loginContext, info, errCode);
                }

                completion.fail(errCode);
            }
        }).start();

    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LoginManager.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        loginAuthExtensionPoints.addAll(pluginRgty.getExtensionList(LoginAuthExtensionPoint.class));

        for (LoginBackend backend : pluginRgty.getExtensionList(LoginBackend.class)) {
            LoginBackend oldBackend = loginBackends.get(backend.getLoginType().toString());
            if (oldBackend != null) {
                throw new CloudRuntimeException(String.format
                        ("duplicate backend with login type %s", oldBackend.getLoginType()));
            }

            loginBackends.put(backend.getLoginType().toString(), backend);
        }

        BeanUtils.reflections.getSubTypesOf(LoginAPIAdapter.class).forEach(clz -> {
            LoginAPIAdapter apiAdapter;
            try {
                apiAdapter = clz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }

            loginAPIAdapters.put(apiAdapter.getMessageClass().getCanonicalName(), apiAdapter);
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
