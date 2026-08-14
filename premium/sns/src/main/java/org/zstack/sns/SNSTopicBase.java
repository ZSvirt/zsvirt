package org.zstack.sns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SNSTopicBase implements SNSTopic {
    private static final CLogger logger = Utils.getLogger(SNSTopicBase.class);

    private SNSTopicVO self;

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private SNSManager snsMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    public SNSTopicBase(SNSTopicVO self) {
        this.self = self;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SNSPublishMsg) {
            handle((SNSPublishMsg) msg);
        } else if (msg instanceof SNSTopicDeletionMsg) {
            handle((SNSTopicDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(SNSTopicDeletionMsg msg) {
        delete();
        bus.reply(msg, new SNSTopicDeletionReply());
    }

    protected SNSTopicVO getSelf() {
        return self;
    }

    protected SNSTopicInventory getSelfInventory() {
        return SNSTopicInventory.valueOf(getSelf());
    }

    protected MessageStruct createMessageStruct(SNSPublishMsg msg, String endpointType) {
        String message = msg.getMessage().get(endpointType);
        if (message == null) {
            message = msg.getDefaultMessage();
        }
        Map metadata = (Map) msg.getMetadata().get(endpointType);

        MessageStruct struct = new MessageStruct();
        struct.setMessage(message);
        struct.setMetadata(metadata);
        return struct;
    }

    protected void handle(SNSPublishMsg msg) {
        if (self.getState() == SNSTopicState.Disabled) {
            logger.debug(String.format("topic[uuid:%s] is Disabled, won't publish to endpoints", self.getUuid()));

            SNSPublishReply reply = new SNSPublishReply();
            bus.reply(msg, reply);
            return;
        }

        List<SNSApplicationPlatformVO> platformVOS = SQL.New("select p from SNSApplicationPlatformVO p," +
                " SNSSubscriberVO s, SNSApplicationEndpointVO e where e.uuid = s.endpointUuid and e.platformUuid = p.uuid" +
                " and s.topicUuid = :topicUuid and p.type != :ptype group by p.uuid", SNSApplicationPlatformVO.class)
                .param("topicUuid", self.getUuid())
                .param("ptype", SNSConstants.SYSTEM_PLATFORM)
                .list();

        List<SNSApplicationEndpointVO> endpointVOS =  SQL.New("select e from SNSApplicationPlatformVO p," +
                " SNSSubscriberVO s, SNSApplicationEndpointVO e where e.uuid = s.endpointUuid and e.platformUuid = p.uuid" +
                " and s.topicUuid = :topicUuid and p.type = :ptype group by e.uuid", SNSApplicationEndpointVO.class)
                .param("topicUuid", self.getUuid())
                .param("ptype", SNSConstants.SYSTEM_PLATFORM)
                .list();

        if (platformVOS.isEmpty() && endpointVOS.isEmpty()) {
            throw new OperationFailureException(operr("the topic is not subscribed by any endpoints"));
        }

        SNSPublishReply reply = new SNSPublishReply();
        List<SNSPublishError> errors = new ArrayList<>();

        SNSTopicInventory inv = getSelfInventory();

        List<SNSPublishExtension> extensions = pluginRgty.getExtensionList(SNSPublishExtension.class);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("publish-topic-%s-%s", self.getName(), self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "call-non-system-application-platforms";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(platformVOS).all(new While.Do<SNSApplicationPlatformVO>() {
                            @Override
                            @AsyncThread
                            public void accept(SNSApplicationPlatformVO vo, WhileCompletion completion) {
                                if (vo.getState() == SNSApplicationPlatformState.Disabled) {
                                    SNSPublishError err = new SNSPublishError();
                                    err.setError(operr("application platform is disabled"));
                                    err.setPlatformType(vo.getType());
                                    err.setPlatformUuid(vo.getUuid());
                                    errors.add(err);
                                    completion.done();
                                    return;
                                }

                                SNSApplicationPlatformFactory f = snsMgr.getSNSApplicationPlatformFactory(vo.getType());
                                SNSApplicationPlatform platform = f.getSNSApplicationPlatform(vo.getUuid());

                                platform.publish(inv, createMessageStruct(msg, vo.getType()), new ReturnValueCompletion<List<SNSPublishError>>(completion) {
                                    @Override
                                    public void success(List<SNSPublishError> returnValue) {
                                        if (returnValue != null) {
                                            errors.addAll(returnValue);
                                        }

                                        List<String> endpointUuids = SQL.New("select e.uuid from SNSApplicationPlatformVO p," +
                                                " SNSSubscriberVO s, SNSApplicationEndpointVO e where e.uuid = s.endpointUuid and e.platformUuid = p.uuid" +
                                                " and s.topicUuid = :topicUuid and p.type != :ptype " +
                                                " and p.uuid = :puuid group by p.uuid", String.class)
                                                .param("topicUuid", self.getUuid())
                                                .param("puuid", vo.getUuid())
                                                .param("ptype", SNSConstants.SYSTEM_PLATFORM)
                                                .list();

                                        CollectionUtils.safeForEach(extensions, new ForEachFunction<SNSPublishExtension>() {
                                            @Override
                                            public void run(SNSPublishExtension ext) {
                                                for (String endpointUuid : endpointUuids) {
                                                    ext.afterPublishToEndpoint(endpointUuid, msg.getMessage());
                                                }
                                            }
                                        });

                                        completion.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        SNSPublishError err = new SNSPublishError();
                                        err.setError(errorCode);
                                        err.setPlatformType(vo.getType());
                                        err.setPlatformUuid(vo.getUuid());
                                        errors.add(err);
                                        completion.done();
                                    }
                                });
                            }
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "call-system-application-platform-endpoints";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        new While<>(endpointVOS).all(new While.Do<SNSApplicationEndpointVO>() {
                            @Override
                            @AsyncThread
                            public void accept(SNSApplicationEndpointVO item, WhileCompletion completion) {
                                if (item.getState() == SNSApplicationEndpointState.Disabled) {
                                    SNSPublishError err = new SNSPublishError();
                                    err.setError(operr("application endpoint is disabled"));
                                    err.setPlatformType(item.getType());
                                    err.setEndpointUuid(item.getUuid());
                                    errors.add(err);
                                    completion.done();
                                    return;
                                }

                                SNSApplicationEndpointFactory f = snsMgr.getSNSApplicationEndpointFactory(item.getType());
                                SNSApplicationEndpoint endpoint = f.getSNSApplicationEndpoint(item.getUuid());
                                endpoint.publish(createMessageStruct(msg, item.getType()), new Completion(completion) {
                                    @Override
                                    public void success() {
                                        CollectionUtils.safeForEach(extensions, new ForEachFunction<SNSPublishExtension>() {
                                            @Override
                                            public void run(SNSPublishExtension ext) {
                                                ext.afterPublishToEndpoint(item.getUuid(), msg.getMessage());
                                            }
                                        });

                                        completion.done();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        SNSPublishError err = new SNSPublishError();
                                        err.setError(errorCode);
                                        err.setPlatformType(item.getType());
                                        err.setEndpointUuid(item.getUuid());
                                        errors.add(err);
                                        completion.done();
                                    }
                                });
                            }
                        }).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        if (!errors.isEmpty()) {
                            reply.setErrors(errors);
                        }

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDeleteSNSTopicMsg) {
            handle((APIDeleteSNSTopicMsg) msg);
        } else if (msg instanceof APIUpdateSNSTopicMsg) {
            handle((APIUpdateSNSTopicMsg) msg);
        } else if (msg instanceof APIChangeSNSTopicStateMsg) {
            handle((APIChangeSNSTopicStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeSNSTopicStateMsg msg) {
        if (SNSTopicStateEvent.disable.toString().equals(msg.getStateEvent())) {
            self.setState(SNSTopicState.Disabled);
        } else {
            self.setState(SNSTopicState.Enabled);
        }

        self = dbf.updateAndRefresh(self);

        APIChangeSNSTopicStateEvent evt = new APIChangeSNSTopicStateEvent(msg.getId());
        evt.setInventory(getSelfInventory());
        bus.publish(evt);
    }

    private void handle(APIUpdateSNSTopicMsg msg) {
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
        }
        if (msg.getLocale() != null) {
            self.setLocale(msg.getLocale());
        }

        self = dbf.updateAndRefresh(self);

        APIUpdateSNSTopicEvent event = new APIUpdateSNSTopicEvent(msg.getId());
        event.setInventory(getSelfInventory());
        bus.publish(event);
    }

    protected void delete() {
        SNSTopicInventory inv = getSelfInventory();
        pluginRgty.getExtensionList(BeforeDeleteSNSTopicExtensionPoint.class).forEach(ext -> ext.beforeDeleteSNSTopic(inv));

        new SQLBatch() {
            @Override
            protected void scripts() {
                remove(self);

                pluginRgty.getExtensionList(AfterDeleteSNSTopicExtensionPoint.class).forEach(ext -> ext.afterDeleteSNSTopic(inv));
            }
        }.execute();
    }

    private void handle(APIDeleteSNSTopicMsg msg) {
        delete();
        APIDeleteSNSTopicEvent evt = new APIDeleteSNSTopicEvent(msg.getId());
        bus.publish(evt);
    }
}
