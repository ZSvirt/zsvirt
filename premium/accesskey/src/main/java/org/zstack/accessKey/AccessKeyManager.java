package org.zstack.accessKey;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.*;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RestAuthenticationBackend;
import org.zstack.header.rest.RestAuthenticationParams;
import org.zstack.header.rest.RestAuthenticationType;
import org.zstack.header.rest.RestException;
import org.zstack.license.LicenseCanonicalEvents;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AccessKeyManager extends AbstractService implements RestAuthenticationBackend {
    private static final CLogger logger = Utils.getLogger(AccessKeyManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ThreadFacade thdf;

    private Mac macInstance;

    private static AccessKeyInventory createAccessKey(CreateAccessKey msg) {
        final AccessKeyVO vo = new AccessKeyVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setAccountUuid(msg.getAccountUuid());
        vo.setDescription(msg.getDescription());
        vo.setState(AccessKeyState.Enabled);
        /* access key and secret can not include ":" */
        if (msg.getAccessKeyID() != null) {
            vo.setAccessKeyID(msg.getAccessKeyID());
            vo.setAccessKeySecret(msg.getAccessKeySecret());
        } else {
            vo.setAccessKeyID(Platform.randomAlphanumeric(AccessKeyConstant.ACCESSKEY_ID_LEN));
            vo.setAccessKeySecret(Platform.randomAlphanumeric(AccessKeyConstant.ACCESSKEY_SECRET_LEN));
        }

        return new SQLBatchWithReturn<AccessKeyInventory>() {
            @Override
            protected AccessKeyInventory scripts() {
                persist(vo);
                return AccessKeyInventory.valueOf(reload(vo));
            }
        }.execute();
    }

    private static SessionInventory getAccessKeySession(AccessKeyInventory accessKey) {
        SessionInventory session = new SessionInventory();
        session.setUuid(accessKey.getUuid());
        session.setAccountUuid(accessKey.getAccountUuid());
        session.setUuid(accessKey.getUuid());
        session.setNoSessionEvaluation(true);
        return session;
    }

    private static AccessKeyInventory getAccessKey(String accessKeyId) {
        AccessKeyVO vo = Q.New(AccessKeyVO.class).eq(AccessKeyVO_.AccessKeyID, accessKeyId).find();
        if (vo != null) {
            return AccessKeyInventory.valueOf(vo);
        } else {
            return null;
        }
    }

    public void handleMessage(Message msg) {
        if (msg instanceof APICreateAccessKeyMsg) {
            handle((APICreateAccessKeyMsg) msg);
        }  else if (msg instanceof APIDeleteAccessKeyMsg) {
            handle((APIDeleteAccessKeyMsg) msg);
        } else if (msg instanceof APIChangeAccessKeyStateMsg) {
            handle((APIChangeAccessKeyStateMsg) msg);
        } else if (msg instanceof DeleteAccessKeyMsg) {
            handle((DeleteAccessKeyMsg) msg);
        } else if (msg instanceof CreateAccessKeyMsg) {
            handle((CreateAccessKeyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }

    }

    private void handle(APICreateAccessKeyMsg msg) {
        APICreateAccessKeyEvent event = new APICreateAccessKeyEvent(msg.getId());
        AccessKeyInventory inv = createAccessKey(msg);
        event.setInventory(inv);
        bus.publish(event);
    }

    private String getAccessKeySyncSignature(String accessKeyUuid) {
        return String.format("accesskey-%s", accessKeyUuid);
    }

    private void forceLogoutSessionWithAccessKey(AccessKeyVO accessKey, Completion completion) {
        final FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-access-key-%s", accessKey.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-console-proxy-by-access-key";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        SessionInventory session = getAccessKeySession(AccessKeyInventory.valueOf(accessKey));
                        Platform.getComponentLoader().getComponent(PluginRegistry.class)
                                .getExtensionList(SessionLogoutExtensionPoint.class)
                                .forEach(ext -> ext.sessionLogout(session));
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void deleteAccessKey(AccessKeyVO accessKey, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getAccessKeySyncSignature(accessKey.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                forceLogoutSessionWithAccessKey(accessKey, new Completion(completion) {
                    @Override
                    public void success() {
                        dbf.removeByPrimaryKey(accessKey.getUuid(), AccessKeyVO.class);
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-accesskey-%s", accessKey.getUuid());
            }
        });
    }

    private void disableAccessKey(String accessKeyUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getAccessKeySyncSignature(accessKeyUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                AccessKeyVO vo = dbf.findByUuid(accessKeyUuid, AccessKeyVO.class);
                if (vo == null) {
                    completion.success();
                    chain.next();
                    return;
                }

                forceLogoutSessionWithAccessKey(vo, new Completion(completion) {
                    @Override
                    public void success() {
                        SQL.New(AccessKeyVO.class)
                                .eq(AccessKeyVO_.uuid, accessKeyUuid)
                                .set(AccessKeyVO_.state, AccessKeyState.Disabled)
                                .update();
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-accesskey-%s", accessKeyUuid);
            }
        });
    }

    private void handle(APIDeleteAccessKeyMsg msg) {
        APIDeleteAccessKeyEvent event = new APIDeleteAccessKeyEvent(msg.getId());
        AccessKeyVO vo = dbf.findByUuid(msg.getUuid(), AccessKeyVO.class);
        if (vo == null) {
            bus.publish(event);
            return;
        }

        deleteAccessKey(vo, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void deleteAccessKeys(List<AccessKeyVO> accessKeys, Completion completion) {
        new While<>(accessKeys).all((accessKey, compl) -> {
            deleteAccessKey(accessKey, new Completion(compl) {
                @Override
                public void success() {
                    compl.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("delete access key[%s] failed:\n%s",
                            accessKey.getUuid(), errorCode.getReadableDetails()));
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    private void handle(DeleteAccessKeyMsg msg) {
        DeleteAccessKeyReply reply = new DeleteAccessKeyReply();
        List<AccessKeyVO> vos = Q.New(AccessKeyVO.class).eq(AccessKeyVO_.accountUuid, msg.getAccountUuid()).list();
        if (CollectionUtils.isEmpty(vos)) {
            bus.reply(msg, reply);
            return;
        }

        deleteAccessKeys(vos, new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(CreateAccessKeyMsg msg) {
        CreateAccessKeyReply reply = new CreateAccessKeyReply();
        AccessKeyInventory inv = createAccessKey(msg);
        reply.setInventory(inv);
        bus.reply(msg, reply);
    }

    private void handle(APIChangeAccessKeyStateMsg msg) {
        APIChangeAccessKeyStateEvent event = new APIChangeAccessKeyStateEvent(msg.getId());
        if (msg.getStateEvent() == AccessKeyStateEvent.disable) {
            disableAccessKey(msg.getUuid(), new Completion(msg) {
                @Override
                public void success() {
                    AccessKeyVO accessKeyVO = dbf.findByUuid(msg.getUuid(), AccessKeyVO.class);
                    event.setInventory(AccessKeyInventory.valueOf(accessKeyVO));
                    bus.publish(event);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    event.setError(errorCode);
                    bus.publish(event);
                }
            });
        } else {
            AccessKeyVO accessKeyVO = dbf.findByUuid(msg.getUuid(), AccessKeyVO.class);
            accessKeyVO.setState(AccessKeyState.Enabled);
            accessKeyVO = dbf.updateAndRefresh(accessKeyVO);
            event.setInventory(AccessKeyInventory.valueOf(accessKeyVO));
            bus.publish(event);
        }
    }

    public String getId() {
        return bus.makeLocalServiceId(AccessKeyConstant.SERVICE_ID);
    }

    private void setupCanonicalEvents() {
        evtf.on(IdentityCanonicalEvents.ACCOUNT_DELETED_PATH, new EventCallback<IdentityCanonicalEvents.AccountDeletedData>() {
            @Override
            protected void run(Map tokens, IdentityCanonicalEvents.AccountDeletedData d) {
                DeleteAccessKeyMsg innerMessage = new DeleteAccessKeyMsg();
                innerMessage.setAccountUuid(d.getAccountUuid());
                bus.makeLocalServiceId(innerMessage, AccessKeyConstant.SERVICE_ID);
                bus.send(innerMessage);
            }
        });

        evtf.on(LicenseCanonicalEvents.LICENSE_TYPE_CHANGED_PATH, new EventCallback<LicenseCanonicalEvents.LicenseTypeChangeData>() {
            @Override
            protected void run(Map tokens, LicenseCanonicalEvents.LicenseTypeChangeData d) {
                if (d.isCurEnterpriseLicense()) {
                    return;
                }

                /* if not enterprise, remove all the accesskey */
                logger.debug("license is community, remove all the access-keys");
                final List<String> accountUuids = Q.New(AccessKeyVO.class)
                        .select(AccessKeyVO_.accountUuid)
                        .listValues();
                for (String accountUuid : new HashSet<>(accountUuids)) {
                    DeleteAccessKeyMsg innerMessage = new DeleteAccessKeyMsg();
                    innerMessage.setAccountUuid(accountUuid);
                    bus.makeLocalServiceId(innerMessage, AccessKeyConstant.SERVICE_ID);
                    bus.send(innerMessage);
                }
            }
        });

    }

    public boolean start() {
        try {
            // Because Mac.getInstance(String) calls a synchronized method, it
            // could block on
            // invoked concurrently, so use prototype pattern to improve perf.
            macInstance = Mac.getInstance(AccessKeyConstant.ACCESS_KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            logger.info("initialize HMAC-SHA1 md5 failed");
        }

        setupCanonicalEvents();

        return true;
    }

    public boolean stop() {
        return true;
    }

    public RestAuthenticationType getAuthenticationType() {
        return AccessKeyConstant.ACCESSKEY_REST_AUTHENTICATION_TYPE;
    }

    public SessionInventory doAuth(RestAuthenticationParams params) throws RestException {
        int colonIndex = params.authKey.indexOf(':');
        if (colonIndex == -1 || colonIndex == params.authKey.length() - 1) {
            throw new RestException(HttpStatus.BAD_REQUEST.value(), "Invalid authKey format");
        }

        String accessKeyId = params.authKey.substring(0, colonIndex);
        String signature = params.authKey.substring(colonIndex + 1);

        AccessKeyInventory key = getAccessKey(accessKeyId);
        if (key == null || AccessKeyState.Disabled.equals(key.getState())) {
            throw new RestException(HttpStatus.FORBIDDEN.value(), String.format("access key id: %s does not existed or disabled", accessKeyId));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(params.method).append("\n");
        sb.append(params.date).append("\n").append(params.uri);

        SecretKeySpec secret = new SecretKeySpec(key.getAccessKeySecret().getBytes(), AccessKeyConstant.ACCESS_KEY_ALGORITHM);
        String sign;
        try {
            Mac mac = (Mac) macInstance.clone();
            mac.init(secret);
            sign = new String(Base64.encodeBase64(mac.doFinal(sb.toString().getBytes())));
        } catch (CloneNotSupportedException | InvalidKeyException e) {
            throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "initialize HmacSHA1 signature failed");
        }

        if (!sign.equals(signature)) {
            if (logger.isTraceEnabled()) {
                logger.trace("accessKey authentication params: " + JSONObjectUtil.toJsonString(params));
                logger.trace("accessKey authentication buf: " + sb.toString() + ", signature " + sign);
            }
            throw  new RestException(HttpStatus.FORBIDDEN.value(), "wrong accessKey signature");
        }

        return getAccessKeySession(key);
    }
}
