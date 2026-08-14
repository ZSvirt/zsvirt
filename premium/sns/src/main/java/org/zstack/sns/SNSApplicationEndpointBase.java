package org.zstack.sns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SNSApplicationEndpointBase implements SNSApplicationEndpoint {
    protected SNSApplicationEndpointVO self;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;

    protected void deleteHook() {
    }

    public SNSApplicationEndpointBase() {
    }

    public SNSApplicationEndpointBase(SNSApplicationEndpointVO self) {
        this.self = self;
    }

    protected SNSApplicationEndpointVO getSelf() {
        return self;
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

    @Override
    public void publish(MessageStruct message, Completion completion) {
        throw new CloudRuntimeException("method not implemented");
    }

    @Override
    public SNSApplicationEndpointInventory getInventory() {
        return SNSApplicationEndpointInventory.valueOf(self);
    }

    protected void handleLocalMessage(Message msg) {
        if (msg instanceof SNSApplicationEndpointDeletionMsg) {
            handle((SNSApplicationEndpointDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(SNSApplicationEndpointDeletionMsg msg) {
        delete();
        bus.reply(msg, new SNSApplicationEndpointDeletionReply());
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISubscribeSNSTopicMsg) {
            handle((APISubscribeSNSTopicMsg) msg);
        } else if (msg instanceof APIUnsubscribeSNSTopicMsg) {
            handle((APIUnsubscribeSNSTopicMsg) msg);
        } else if (msg instanceof APIChangeSNSApplicationEndpointStateMsg) {
            handle((APIChangeSNSApplicationEndpointStateMsg) msg);
        } else if (msg instanceof APIDeleteSNSApplicationEndpointMsg) {
            handle((APIDeleteSNSApplicationEndpointMsg) msg);
        } else if (msg instanceof APIUpdateSNSApplicationEndpointMsg) {
            handle((APIUpdateSNSApplicationEndpointMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateSNSApplicationEndpointMsg msg) {
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
        }
        if (msg.getPlatformUuid() != null) {
            self.setPlatformUuid(msg.getPlatformUuid());
        }

        self = dbf.updateAndRefresh(self);

        APIUpdateSNSApplicationEndpointEvent evt = new APIUpdateSNSApplicationEndpointEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    protected void delete() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                deleteHook();
                // NOTE: use hardDelete() instead of remove() because sub-entity like SNSDingTalkEndpointVO
                // may have one-to-many mapping, the remove() will first merge() `self` to re-attach it
                // into JPA context, which will cause the one-to-many entities(e.g. SNSDingTalkAtPersonVO) that
                // have deleted by deleteHook() to be re-persisted
                sql(SNSApplicationEndpointVO.class).eq(SNSApplicationEndpointVO_.uuid, self.getUuid()).hardDelete();
            }
        }.execute();
    }

    private void handle(APIDeleteSNSApplicationEndpointMsg msg) {
        delete();
        APIDeleteSNSApplicationEndpointEvent evt = new APIDeleteSNSApplicationEndpointEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIChangeSNSApplicationEndpointStateMsg msg) {
        if (msg.getStateEvent() == SNSApplicationEndpointStateEvent.enable) {
            self.setState(SNSApplicationEndpointState.Enabled);
        } else {
            self.setState(SNSApplicationEndpointState.Disabled);
        }

        self = dbf.updateAndRefresh(self);

        APIChangeSNSApplicationEndpointStateEvent evt = new APIChangeSNSApplicationEndpointStateEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }


    private void handle(APIUnsubscribeSNSTopicMsg msg) {
        List<BeforeUnsubscribeTopicExtensionPoint> beforeExtps = pluginRgty.getExtensionList(BeforeUnsubscribeTopicExtensionPoint.class);
        List<AfterUnsubscribeTopicExtensionPoint> afterExtps = pluginRgty.getExtensionList(AfterUnsubscribeTopicExtensionPoint.class);

        Runnable callBeforeExtensions = null;
        Runnable callAfterExtensions = null;

        if (!beforeExtps.isEmpty() || !afterExtps.isEmpty()) {
            SNSApplicationEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSApplicationEndpointVO.class);
            SNSTopicVO topicVO = dbf.findByUuid(msg.getTopicUuid(), SNSTopicVO.class);
            SNSApplicationEndpointInventory endpointInventory = SNSApplicationEndpointInventory.valueOf(endpointVO);
            SNSTopicInventory topicInventory = SNSTopicInventory.valueOf(topicVO);

            if (!beforeExtps.isEmpty()) {
                callBeforeExtensions = () -> beforeExtps.forEach(ext -> ext.beforeUnsubscribeTopic(topicInventory, endpointInventory));
            }

            if (!afterExtps.isEmpty()) {
                callAfterExtensions = () -> afterExtps.forEach(ext -> ext.afterUnsubscribeTopic(topicInventory, endpointInventory));
            }
        }

        if (callBeforeExtensions != null) {
            callBeforeExtensions.run();
        }

        SQL.New(SNSSubscriberVO.class).eq(SNSSubscriberVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSSubscriberVO_.topicUuid, msg.getTopicUuid()).hardDelete();

        if (callAfterExtensions != null) {
            callAfterExtensions.run();
        }

        APIUnsubscribeSNSTopicEvent evt = new APIUnsubscribeSNSTopicEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APISubscribeSNSTopicMsg msg) {
        List<BeforeSubscribeTopicExtensionPoint> beforeExtps = pluginRgty.getExtensionList(BeforeSubscribeTopicExtensionPoint.class);
        List<AfterSubscribeTopicExtensionPoint> afterExtps = pluginRgty.getExtensionList(AfterSubscribeTopicExtensionPoint.class);

        Runnable callBeforeExtensions = null;
        Runnable callAfterExtensions = null;

        if (!beforeExtps.isEmpty() || !afterExtps.isEmpty()) {
            SNSApplicationEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSApplicationEndpointVO.class);
            SNSTopicVO topicVO = dbf.findByUuid(msg.getTopicUuid(), SNSTopicVO.class);
            SNSApplicationEndpointInventory endpointInventory = SNSApplicationEndpointInventory.valueOf(endpointVO);
            SNSTopicInventory topicInventory = SNSTopicInventory.valueOf(topicVO);

            if (!beforeExtps.isEmpty()) {
                callBeforeExtensions = () -> beforeExtps.forEach(ext -> ext.beforeSubscribeTopic(topicInventory, endpointInventory));
            }
            if (!afterExtps.isEmpty()) {
                callAfterExtensions = () -> afterExtps.forEach(ext -> ext.afterSubscribeTopic(topicInventory, endpointInventory));
            }
        }

        if (callBeforeExtensions != null) {
            callBeforeExtensions.run();
        }

        SNSSubscriberVO vo = new SNSSubscriberVO();
        vo.setTopicUuid(msg.getTopicUuid());
        vo.setEndpointUuid(msg.getEndpointUuid());
        dbf.persist(vo);

        if (callAfterExtensions != null) {
            callAfterExtensions.run();
        }

        APISubscribeSNSTopicEvent evt = new APISubscribeSNSTopicEvent(msg.getId());
        bus.publish(evt);
    }
}
