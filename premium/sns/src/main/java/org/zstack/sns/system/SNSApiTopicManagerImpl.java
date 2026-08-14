package org.zstack.sns.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusGson;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.identity.AccountManager;
import org.zstack.sns.*;
import org.zstack.sns.platform.http.SNSHttpEndpointFactory;
import static org.zstack.core.Platform.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SNSApiTopicManagerImpl implements SNSApiTopicManager, Component,
        PrepareDbInitialValueExtensionPoint, BeforeDeliveryMessageInterceptor, BeforePublishEventInterceptor, BeforeSendMessageInterceptor,
        BeforeSubscribeTopicExtensionPoint, BeforeUnsubscribeTopicExtensionPoint, BeforeDeleteSNSTopicExtensionPoint {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private SNSManager snsManager;
    @Autowired
    private AccountManager acntMgr;

    private Map<String, SNSApplicationEndpoint> apiTopicEndpoints = new ConcurrentHashMap<>();

    @Override
    public boolean start() {
        bus.installBeforeDeliveryMessageInterceptor(this);
        bus.installBeforePublishEventInterceptor(this);
        bus.installBeforeSendMessageInterceptor(this);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void prepareDbInitialValue() {
        createApiTopic();
    }

    private void createApiTopic() {
        SNSTopicVO apiTopicVO = Q.New(SNSTopicVO.class).eq(SNSTopicVO_.uuid, SNSApiTopicManager.API_TOPIC_UUID).find();
        if (apiTopicVO == null) {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    SNSTopicVO vo = new SNSTopicVO();
                    vo.setUuid(SNSApiTopicManager.API_TOPIC_UUID);
                    vo.setName(SNSApiTopicManager.API_TOPIC_NAME);
                    vo.setState(SNSTopicState.Enabled);
                    vo.setDescription("the topic to publish API requests and responses");
                    vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    persist(vo);
                    flush();
                }
            }.execute();
        }
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 0;
    }

    @Override
    @ExceptionSafe
    public void beforeDeliveryMessage(Message msg) {
        if (!(msg instanceof MessageReply)) {
            return;
        }

        publish(msg);
    }

    @Override
    public int orderOfBeforePublishEventInterceptor() {
        return 0;
    }

    @Override
    @ExceptionSafe
    public void beforePublishEvent(Event evt) {
        if (!(evt instanceof APIEvent)) {
            return;
        }

        publish(evt);
    }

    @Override
    public int orderOfBeforeSendMessageInterceptor() {
        return 0;
    }

    private String toErasedApiString(Message msg) {
        // the clone is shadow clone
        // we have to create another header
        Message copy = msg.clone();

        Map<String, Object> headers = new HashMap<>();
        if (msg.getHeaders().containsKey(CloudBus.HEADER_CORRELATION_ID)) {
            headers.put(CloudBus.HEADER_CORRELATION_ID, msg.getHeaders().get(CloudBus.HEADER_CORRELATION_ID));
        }

        copy.setHeaders(headers);

        return CloudBusGson.toJson(copy);
    }

    @AsyncThread
    private void publish(Message msg) {
        apiTopicEndpoints.values().forEach(e -> {
            SNSApplicationEndpointInventory inventory = e.getInventory();
            SNSApplicationEndpointState state = Q.New(SNSApplicationEndpointVO.class)
                    .select(SNSApplicationEndpointVO_.state)
                    .eq(SNSApplicationEndpointVO_.uuid, inventory.getUuid())
                    .findValue();
            if (SNSApplicationEndpointState.Enabled.equals(state)){
                MessageStruct struct = new MessageStruct();
                struct.setMessage(toErasedApiString(msg));
                e.publish(struct, new NopeCompletion());
            }
        });
    }

    @Override
    @ExceptionSafe
    public void beforeSendMessage(Message msg) {
        if (!(msg instanceof APIMessage)) {
            return;
        }

        APIMessage amsg = (APIMessage) msg;
        if (!amsg.getServiceId().equals(ApiMediatorConstant.SERVICE_ID)) {
            // the API message will be routed by ApiMediator,
            // filter out the routed message to avoid reporting the same
            // API message twice
            return;
        }

        publish(msg);
    }

    @Override
    public void beforeSubscribeTopic(SNSTopicInventory topic, SNSApplicationEndpointInventory endpoint) {
        if (!topic.getUuid().equals(SNSApiTopicManager.API_TOPIC_UUID)) {
            return;
        }

        if (!SNSHttpEndpointFactory.type.toString().equals(endpoint.getType())) {
            throw new OperationFailureException(operr("only HTTP endpoint can subscribe API topic, the endpoint[type:%s] is not a HTTP endpoint", endpoint.getType()));
        }

        SNSApplicationEndpointFactory f = snsManager.getSNSApplicationEndpointFactory(endpoint.getType());
        apiTopicEndpoints.put(endpoint.getUuid(), f.getSNSApplicationEndpoint(endpoint.getUuid()));
    }

    @Override
    public void beforeUnsubscribeTopic(SNSTopicInventory topic, SNSApplicationEndpointInventory endpoint) {
        if (!topic.getUuid().equals(SNSApiTopicManager.API_TOPIC_UUID)) {
            return;
        }

        apiTopicEndpoints.remove(endpoint.getUuid());
    }

    @Override
    public void beforeDeleteSNSTopic(SNSTopicInventory topic) {
        if (SNSApiTopicManager.API_TOPIC_UUID.equals(topic.getUuid())) {
            throw new OperationFailureException(operr("API topic cannot be deleted"));
        }
    }
}
