package org.zstack.sns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SNSApplicationPlatformBase implements SNSApplicationPlatform {
    protected SNSApplicationPlatformVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected SNSManager snsMgr;
    @Autowired
    protected AccountManager acntMgr;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    protected PluginRegistry pluginRgty;

    public SNSApplicationPlatformBase() {}

    public SNSApplicationPlatformBase(SNSApplicationPlatformVO self) {
        this.self = self;
    }

    protected SNSApplicationPlatformVO getSelf() {
        return self;
    }

    protected SNSApplicationPlatformInventory getInventory() {
        return SNSApplicationPlatformInventory.valueOf(getSelf());
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage)msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    @Override
    public void publish(SNSTopicInventory topic, MessageStruct message, ReturnValueCompletion<List<SNSPublishError>> completion) {
        List<String> endpointUuids = SQL.New("select e.uuid from SNSApplicationEndpointVO e, SNSSubscriberVO s" +
                " where e.uuid = s.endpointUuid and s.topicUuid = :topicUuid").param("topicUuid", topic.getUuid()).list();

        if (endpointUuids.isEmpty()) {
            completion.success(null);
            return;
        }

        List<SNSPublishError> errors = new ArrayList<>();

        SNSApplicationEndpointFactory f = snsMgr.getSNSApplicationEndpointFactory(self.getType());
        List<SNSApplicationEndpoint> endpoints = f.getSNSApplicationEndpoints(endpointUuids);
        new While<>(endpoints).each((item, c) -> item.publish(message, new Completion(c) {
            @Override
            public void success() {
                c.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SNSPublishError error = new SNSPublishError();
                error.setEndpointUuid(item.getInventory().getPlatformUuid());
                error.setPlatformType(self.getType());
                error.setPlatformUuid(self.getUuid());
                error.setError(errorCode);
                errors.add(error);
                c.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success(errors);
            }
        });
    }

    private void operationNoAllowedForSystemPlatform() {
        if (self.getType().equals(SNSConstants.SYSTEM_PLATFORM)) {
            throw new OperationFailureException(operr("the operation is not permitted for the system application platform"));
        }
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof SNSApplicationPlatformDeletionMsg) {
            handle((SNSApplicationPlatformDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(SNSApplicationPlatformDeletionMsg msg) {
        SNSApplicationPlatformDeletionReply reply = new SNSApplicationPlatformDeletionReply();
        delete(new Completion(msg) {
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

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateSNSApplicationEndpointMsg) {
            handle((APICreateSNSApplicationEndpointMsg) msg);
        } else if (msg instanceof APIChangeSNSApplicationPlatformStateMsg) {
            operationNoAllowedForSystemPlatform();
            handle((APIChangeSNSApplicationPlatformStateMsg) msg);
        } else if (msg instanceof APIDeleteSNSApplicationPlatformMsg) {
            operationNoAllowedForSystemPlatform();
            handle((APIDeleteSNSApplicationPlatformMsg) msg);
        } else if (msg instanceof APIUpdateSNSApplicationPlatformMsg) {
            handle((APIUpdateSNSApplicationPlatformMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateSNSApplicationPlatformMsg msg) {
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
        }

        self = dbf.updateAndRefresh(self);

        APIUpdateSNSApplicationPlatformEvent evt = new APIUpdateSNSApplicationPlatformEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    protected void delete(Completion completion) {
        final String issuer = SNSApplicationPlatformVO.class.getSimpleName();

        final List<SNSApplicationPlatformInventory> ctx = Collections.singletonList(SNSApplicationPlatformInventory.valueOf(self));

        final FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-sns-platform-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                dbf.remove(self);
                completion.success();

            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(final ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIDeleteSNSApplicationPlatformMsg msg) {
        APIDeleteSNSApplicationPlatformEvent evt = new APIDeleteSNSApplicationPlatformEvent(msg.getId());

        delete(new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });


    }

    private void handle(APIChangeSNSApplicationPlatformStateMsg msg) {
        if (msg.getStateEvent() == SNSApplicationPlatformStateEvent.enable) {
            self.setState(SNSApplicationPlatformState.Enabled);
        } else {
            self.setState(SNSApplicationPlatformState.Disabled);
        }

        self = dbf.updateAndRefresh(self);

        APIChangeSNSApplicationPlatformStateEvent evt = new APIChangeSNSApplicationPlatformStateEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    private void handle(APICreateSNSApplicationEndpointMsg msg) {
        String type = msg.getApplicationEndpointType() == null ? self.getType() : msg.getApplicationEndpointType();
        SNSApplicationEndpointVO vo = new SNSApplicationEndpointVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setType(type);
        vo.setPlatformUuid(msg.getApplicationPlatformUuid());
        vo.setState(SNSApplicationEndpointState.Enabled);
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        SNSApplicationEndpointFactory f = snsMgr.getSNSApplicationEndpointFactory(type);
        SNSApplicationEndpointVO rvo = f.createApplicationEndpoint(vo, msg);
        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(rvo);
                reload(rvo);
            }
        }.execute();

        List<AfterCreateSNSApplicationEndpointExtensionPoint> afterExtPoints =
                pluginRgty.getExtensionList(AfterCreateSNSApplicationEndpointExtensionPoint.class);
        afterExtPoints.forEach( ext -> {
            ext.afterCreateSNSApplicationEndpoint(rvo.getUuid(), msg);
        });

        APICreateSNSApplicationEndpointEvent evt = new APICreateSNSApplicationEndpointEvent(msg.getId());
        evt.setInventory(f.getSNSApplicationEndpointInventory(rvo));
        bus.publish(evt);
    }
}
