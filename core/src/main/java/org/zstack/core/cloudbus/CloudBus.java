package org.zstack.core.cloudbus;

import org.springframework.http.HttpEntity;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.message.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.function.Consumer;

public interface CloudBus extends Component {
    /**
     * submit a message sending task into the queue.
     * @return {@link FutureCompletion} which can be used to check whether the message was successfully sent or not.
     */
    FutureCompletion send(Message msg);

    <T extends Message> void send(List<T> msgs);

    @Deprecated
    void send(APIMessage msg, Consumer<APIEvent> consumer);

    FutureCompletion send(NeedReplyMessage msg, CloudBusCallBack callback);

    @Deprecated
    void send(List<? extends NeedReplyMessage> msgs, CloudBusListCallBack callBack);

    @Deprecated
    void send(List<? extends NeedReplyMessage> msgs, int parallelLevel, CloudBusListCallBack callBack);

    @Deprecated
    void send(List<? extends NeedReplyMessage> msgs, int parallelLevel, CloudBusSteppingCallback callback);

    void route(List<Message> msgs);
    
    void route(Message msg);
    
    void reply(Message request, MessageReply reply);

    void cancel(String correlationId, String error);
    
    void publish(List<Event> events);
    
    void publish(Event event);
    
    MessageReply call(NeedReplyMessage msg);
    
    <T extends NeedReplyMessage> List<MessageReply> call(List<T> msg);
    
    void registerService(Service serv) throws CloudConfigureFailException;
    
    void unregisterService(Service serv);
    
    EventSubscriberReceipt subscribeEvent(CloudBusEventListener listener, Event...events);
    
    void dealWithUnknownMessage(Message msg);
    
    void replyErrorByMessageType(Message msg, Exception e);
    
    void replyErrorByMessageType(Message msg, String err);
    
    void replyErrorByMessageType(Message msg, ErrorCode err);
    
    void logExceptionWithMessageDump(Message msg, Throwable e);

    String getServiceId(String targetServiceId);

    String makeLocalServiceId(String serviceId);

    void makeLocalServiceId(Message msg, String serviceId);

    String makeServiceIdByManagementNodeId(String serviceId, String managementNodeId);

    void makeServiceIdByManagementNodeId(Message msg, String serviceId, String managementNodeId);

    String makeTargetServiceIdByResourceUuid(String serviceId, String resourceUuid);

    void makeTargetServiceIdByResourceUuid(Message msg, String serviceId, String resourceUuid);

    void installBeforeDeliveryMessageInterceptor(BeforeDeliveryMessageInterceptor interceptor, List<Class<? extends Message>> classes);

    void installBeforeDeliveryMessageInterceptor(BeforeDeliveryMessageInterceptor interceptor, Class<? extends Message>...classes);

    void installBeforeSendMessageInterceptor(BeforeSendMessageInterceptor interceptor, Class<? extends Message>...classes);

    void installBeforePublishEventInterceptor(BeforePublishEventInterceptor interceptor, Class<? extends Event>...classes);

    /**
     *  this method should be inherited by implementations using HTTP
     * @param e
     * @param rsp
     */
    default void handleHttpRequest(HttpEntity<String> e, HttpServletResponse rsp) {
        throw new AbstractMethodError("not supported");
    }

    String HEADER_NO_NEED_REPLY_MSG = "noReply";
    String HEADER_CORRELATION_ID = "correlationId";
    String HEADER_REPLY_TO = "replyTo";
    String HEADER_IS_MESSAGE_REPLY = "isReply";
    String HEADER_MESSAGE_META_DATA = "metaData";
    String HEADER_DEAD_LETTER = "dead-message";
    String HEADER_TASK_STACK = "task-stack";
    String HEADER_TASK_CONTEXT = "task-context";
    String HEADER_SCHEMA = "schema";

    default int getEnvelopeSize() {
        return 0;
    }
}
