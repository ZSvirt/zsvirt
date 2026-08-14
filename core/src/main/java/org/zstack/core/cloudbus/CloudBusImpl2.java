package org.zstack.core.cloudbus;

import com.google.gson.*;
import com.rabbitmq.client.*;
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection;
import com.rabbitmq.client.impl.recovery.RecoveryAwareAMQConnection;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.jmx.JmxFacade;
import org.zstack.core.log.LogUtils;
import org.zstack.core.thread.*;
import org.zstack.core.thread.ThreadFacadeImpl.TimeoutTaskReceipt;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.Constants;
import org.zstack.header.Service;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.log.NoLogging;
import org.zstack.header.apimediator.APIIsReadyToGoMsg;
import org.zstack.header.apimediator.APIIsReadyToGoReply;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudConfigureFailException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.*;
import org.zstack.header.search.APISearchMessage;
import org.zstack.header.search.APISearchReply;
import org.zstack.utils.*;
import org.zstack.utils.gson.GsonTypeCoder;
import org.zstack.utils.gson.GsonUtil;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.management.MXBean;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.BeanUtils.getProperty;
import static org.zstack.utils.BeanUtils.setProperty;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;
import static org.zstack.utils.ExceptionDSL.throwableSafe;

/**
 */
@MXBean
public class CloudBusImpl2 implements CloudBus, CloudBusIN, ManagementNodeChangeListener, CloudBusMXBean {
    private static final CLogger logger = Utils.getLogger(CloudBusImpl2.class);
    private Connection conn;
    private BusQueue outboundQueue;
    private ChannelPool channelPool;

    @Autowired
    private ResourceDestinationMaker destMaker;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private JmxFacade jmxf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;

    private List<String> serverIps;
    private List<Service> services = new ArrayList<Service>();

    private Map<String, Envelope> envelopes = new ConcurrentHashMap<String, Envelope>();
    private Map<String, EndPoint> endpoints = new ConcurrentHashMap<String, EndPoint>();
    private AtomicBoolean stopped = new AtomicBoolean(false);
    private boolean trackerClose = false;
    private Map<String, MessageStatistic> statistics = new HashMap<String, MessageStatistic>();

    private Map<Class, List<MarshalReplyMessageExtensionPoint>> replyMessageMarshaller = new ConcurrentHashMap<Class, List<MarshalReplyMessageExtensionPoint>>();

    private Map<Class, List<BeforeDeliveryMessageInterceptor>> beforeDeliveryMessageInterceptors = new HashMap<Class, List<BeforeDeliveryMessageInterceptor>>();
    private Map<Class, List<BeforeSendMessageInterceptor>> beforeSendMessageInterceptors = new HashMap<Class, List<BeforeSendMessageInterceptor>>();
    private Map<Class, List<BeforePublishEventInterceptor>> beforeEventPublishInterceptors = new HashMap<Class, List<BeforePublishEventInterceptor>>();

    private List<BeforeDeliveryMessageInterceptor> beforeDeliveryMessageInterceptorsForAll = new ArrayList<BeforeDeliveryMessageInterceptor>();
    private List<BeforeSendMessageInterceptor> beforeSendMessageInterceptorsForAll = new ArrayList<BeforeSendMessageInterceptor>();
    private List<BeforePublishEventInterceptor> beforeEventPublishInterceptorsForAll = new ArrayList<BeforePublishEventInterceptor>();

    private long DEFAULT_MESSAGE_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
    private final String AMQP_PROPERTY_HEADER__COMPRESSED = "compressed";

    private String SERVICE_ID = makeLocalServiceId("cloudbus");
    public static final FutureCompletion SEND_CONFIRMED = new FutureCompletion(null);

    static {
        SEND_CONFIRMED.success();
    }

    public void setDEFAULT_MESSAGE_TIMEOUT(long timeout) {
        this.DEFAULT_MESSAGE_TIMEOUT = timeout;
    }

    private void createExchanges() throws IOException {
        Channel chan = channelPool.acquire();
        try {
            chan.exchangeDeclare(BusExchange.NO_ROUTE.toString(), BusExchange.NO_ROUTE.getType());
            Map<String, Object> args = map(e("alternate-exchange", (Object) BusExchange.NO_ROUTE.toString()));
            chan.exchangeDeclare(BusExchange.P2P.toString(), BusExchange.P2P.getType(), true, false, args);
            chan.exchangeDeclare(BusExchange.BROADCAST.toString(), BusExchange.BROADCAST.getType());
        } finally {
            channelPool.returnChannel(chan);
        }
    }

    private String makeMessageQueueName(String queueName) {
        return String.format("zstack.message.%s", queueName);
    }

    private String makeEventQueueName(String queueName) {
        return String.format("zstack.event.%s", queueName);
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        tracker.managementNodeLeft(inv.getUuid());
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
        tracker.managementNodeLeft(inv.getUuid());
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }


    private class ChannelPool {
        BlockingQueue<Channel> pool;

        @AsyncThread
        private void retry(Message msg) {
            try {
                TimeUnit.SECONDS.sleep(CloudBusGlobalProperty.RABBITMQ_RETRY_DELAY_ON_RETURN);
            } catch (InterruptedException e) {
                logger.warn(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }

            if (msg instanceof Event) {
                publish((Event) msg);
            } else {
                send(msg);
            }
        }

        ChannelPool(int size, Connection connection) {
            try {
                pool = new ArrayBlockingQueue<Channel>(size);
                for (int i = 0; i < size; i++) {
                    Channel chan = connection.createChannel();
                    pool.add(chan);
                    chan.addReturnListener(new ReturnListener() {
                        @Override
                        public void handleReturn(int i, String s, String s2, String s3, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
                            try {
                                Message msg = wire.toMessage(bytes, basicProperties);
                                if (msg instanceof NeedReplyMessage) {
                                    Envelope e = envelopes.get(msg.getId());
                                    if (e == null) {
                                        retry(msg);
                                        logger.warn(String.format("unable to deliver the message; the destination service[%s] is dead; please use rabbitmqctl to check if the queue is existing and if there is any consumers on that queue; message dump:\n%s",
                                                msg.getServiceId(), wire.dumpMessage(msg)));
                                    } else {
                                        MessageReply reply = new MessageReply();
                                        reply.setError(err(SysErrors.UNDELIVERABLE_ERROR,
                                                "unable to deliver the message; the destination service[%s] is dead; please use rabbitmqctl to check if the queue is existing and if any consumers on that queue", msg.getServiceId()));
                                        e.ack(reply);
                                    }
                                } else {
                                    retry(msg);
                                    logger.warn(String.format("unable to deliver an event; please use rabbitmqctl to check if the queue is existing and if there is any consumers on that queue; message dump:\n%s",
                                            wire.dumpMessage(msg)));
                                }
                            } catch (Throwable t) {
                                logger.warn("unhandled throwable", t);
                            }
                        }
                    });
                }
                logger.debug(String.format("created channel pool with size[%s]", CloudBusGlobalProperty.CHANNEL_POOL_SIZE));
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        Channel acquire() {
            try {
                final Channel chan = pool.poll(10, TimeUnit.MINUTES);
                DebugUtils.Assert(chan!=null, String.format("cannot get a channel after 10 minutes"));
                return chan;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CloudRuntimeException(e);
            }
        }

        void returnChannel(Channel chan) {
            pool.add(chan);
        }

        void destruct() throws IOException {
            for (Channel chan : pool) {
                try {
                    chan.close();
                } catch (IOException | TimeoutException e) {
                    chan.abort();
                }
            }
        }
    }

    private static abstract class MessageMetaData implements Serializable {
        String className;
        String serviceId;
        String messageName;
    }

    private static class RequestMessageMetaData extends MessageMetaData {
        String replyTo;
        Long timeout;
        String msgId;
        boolean needApiEvent;
    }

    private static class ResponseMessageMetaData extends MessageMetaData {
        boolean isApiEvent;
        String correlationId;
    }

    private static class LockMessageMetaData extends RequestMessageMetaData {
        String unlockKey;
        String reason;
        String senderManagementUuid;
    }


    private class NoRouteEndPoint extends AbstractConsumer {
        Channel nrouteChan;
        String nrouteName = makeMessageQueueName("NoRouteEndPoint");

        public void construct() {
            try {
                nrouteChan = conn.createChannel();
                nrouteChan.queueDeclare(nrouteName, false, false, true, null);
                nrouteChan.queueBind(nrouteName, BusExchange.NO_ROUTE.toString(), "");
                nrouteChan.basicConsume(nrouteName, true, this);
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        public void destruct() {
            try {
                nrouteChan.close();
            } catch (IOException | TimeoutException e) {
                throw new CloudRuntimeException(e);
            }
        }

        @Override
        public void handleDelivery(String s, final com.rabbitmq.client.Envelope envelope, final AMQP.BasicProperties basicProperties, final byte[] bytes) throws IOException {
            throwableSafe(new Runnable() {
                @Override
                public void run() {
                    Message msg = wire.toMessage(bytes, basicProperties);
                    if (CloudBus.HEADER_DEAD_LETTER.equals(envelope.getRoutingKey())) {
                        handleDeadLetter(msg);
                    } else {
                        handleNoRouteLetter(msg);
                    }
                }

                private void handleNoRouteLetter(Message msg) {
                    setThreadLoggingContext(msg);

                    if (msg instanceof APIIsReadyToGoMsg) {
                        APIIsReadyToGoReply reply = new APIIsReadyToGoReply();
                        reply.setManagementNodeId(Platform.getManagementServerId());
                        reply.setError(err(SysErrors.NOT_READY_ERROR, "management node[uuid:%s] is no ready", Platform.getManagementServerId()));
                        reply(msg, reply);
                        return;
                    }

                    String err = null;
                    if (msg instanceof MessageReply) {
                        replyErrorByMessageType(msg, err(SysErrors.NO_ROUTE_ERROR, "No route found for the reply[%s], the service[id:%s] waiting for this reply may have been quit. %s",
                                msg.getClass().getName(), msg.getServiceId(), wire.dumpMessage(msg)));
                    } else {
                        replyErrorByMessageType(msg, err(SysErrors.NO_ROUTE_ERROR, "No route found for the message[%s], the service[id:%s] may not be running. Checking Spring xml to make sure you have loaded it. Message dump:\n %s",
                                msg.getClass().getName(), msg.getServiceId(), wire.dumpMessage(msg)));
                    }
                }

                private void handleDeadLetter(Message msg) {
                    if (msg instanceof MessageReply || msg instanceof APIEvent) {
                        String err = String.format("the message reply or API event becomes a dead letter; the possible reason is the service it replied to has been dead, and the reply expired after TTL[%s secs]; reply dump:\n%s",
                                CloudBusGlobalProperty.MESSAGE_TTL, wire.dumpMessage(msg));
                        logger.warn(err);
                    } else {
                        ErrorCode err = err(SysErrors.NO_ROUTE_ERROR, "the message becomes a dead letter; the possible reason is the service[%s] it sends to has been dead", msg.getServiceId());
                        logger.warn(String.format("%s; message dump:%s", err.getDetails(), wire.dumpMessage(msg)));
                        replyErrorByMessageType(msg, err);
                    }
                }
            });
        }
    }

    private NoRouteEndPoint noRouteEndPoint = new NoRouteEndPoint();

    private Consumer consumer = new AbstractConsumer() {
        @AsyncThread
        @MessageSafe
        private void handle(Message msg) {
            setThreadLoggingContext(msg);

            if (logger.isTraceEnabled() && wire.logMessage(msg))  {
                logger.trace(String.format("[msg received]: %s", wire.dumpMessage(msg)));
            }

            if (msg instanceof MessageReply) {
                beforeDeliverMessage(msg);

                MessageReply r = (MessageReply) msg;
                String correlationId = r.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID);
                Envelope e = envelopes.get(correlationId);
                if (e == null) {
                    logger.warn(String.format("received a message reply but no envelope found," +
                            "maybe the message request has been timeout or sender doesn't care about reply." +
                            "drop it. reply dump:\n%s", wire.dumpMessage(r)));
                    return;
                }

                e.ack(r);
            } else {
                dealWithUnknownMessage(msg);
            }
        }

        @Override
        public void handleDelivery(String s, com.rabbitmq.client.Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
            try {
                Message msg = wire.toMessage(bytes, basicProperties);

                handle(msg);
            } catch (Throwable t) {
                logger.warn("unhandled throwable", t);
            }
        }
    };


    private class Wire implements GsonTypeCoder<Message> {
        private List<String> filterMsgNames = new ArrayList<String>();

        {
            if (CloudBusGlobalProperty.MESSAGE_LOG != null) {
                String[] msgNames = CloudBusGlobalProperty.MESSAGE_LOG.split(",");
                for (String name : msgNames) {
                    filterMsgNames.add(name.trim());
                }
            }
        }

        private final Gson gson = new GsonUtil().setCoder(Message.class, this).setExclusionStrategies(new ExclusionStrategy[]{
                new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        return fieldAttributes.getAnnotation(GsonTransient.class) != null;
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> aClass) {
                        return false;
                    }
                }
        }).create();

        private final Gson gson1 = new GsonUtil().setCoder(Message.class, this).setSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                return fieldAttributes.getAnnotation(NoLogging.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> aClass) {
                return false;
            }
        }).create();

        private class RecoverableSend {
            Channel chan;
            byte[] data;
            String serviceId;
            Message msg;
            BusExchange exchange;

            RecoverableSend(Channel chan, Message msg, String serviceId, BusExchange exchange) throws IOException {
                data = compressMessageIfNeeded(msg);
                this.chan = chan;
                this.serviceId = serviceId;
                this.msg = msg;
                this.exchange = exchange;
            }

            void send() throws IOException {
                try {
                    chan.basicPublish(exchange.toString(), serviceId,
                            true, msg.getAMQPProperties(), data);
                } catch (ShutdownSignalException e) {
                    if (!(conn instanceof AutorecoveringConnection) || serverIps.size() <= 1 || !Platform.IS_RUNNING) {
                        // the connection is not recoverable
                        throw e;
                    }

                    logger.warn(String.format("failed to send a message because %s; as the connection is recoverable," +
                            "we are doing recoverable send right now", e.getMessage()));

                    if (!recoverSend()) {
                        throw e;
                    }
                }
            }

            private byte[] compressMessageIfNeeded(Message msg) throws IOException {
                if (!CloudBusGlobalProperty.COMPRESS_NON_API_MESSAGE || msg instanceof APIEvent || msg instanceof APIMessage) {
                    return gson.toJson(msg, Message.class).getBytes();
                }

                msg.getAMQPHeaders().put(AMQP_PROPERTY_HEADER__COMPRESSED, "true");
                return Compresser.deflate(gson.toJson(msg, Message.class).getBytes());
            }

            private boolean recoverSend() throws IOException {
                int interval = conn.getHeartbeat() / 2;
                interval = interval > 0 ? interval : 1;
                int count = 0;

                // as the connection is lost, there is no need to wait heart beat missing 8 times
                // so we use reflection to fast the process
                RecoveryAwareAMQConnection delegate = FieldUtils.getFieldValue("delegate", conn);
                DebugUtils.Assert(delegate != null, "cannot get RecoveryAwareAMQConnection");
                Field _missedHeartbeats = FieldUtils.getField("_missedHeartbeats", RecoveryAwareAMQConnection.class);
                DebugUtils.Assert(_missedHeartbeats!=null, "cannot find _missedHeartbeats");
                _missedHeartbeats.setAccessible(true);
                try {
                    _missedHeartbeats.set(delegate, 100);
                } catch (IllegalAccessException e) {
                    throw new CloudRuntimeException(e);
                }

                while (count < CloudBusGlobalProperty.RABBITMQ_RECOVERABLE_SEND_TIMES) {
                    try {
                        TimeUnit.SECONDS.sleep(interval);
                    } catch (InterruptedException e1) {
                        logger.warn(e1.getMessage());
                        Thread.currentThread().interrupt();
                    }

                    try {
                        chan.basicPublish(exchange.toString(), serviceId,
                                true, msg.getAMQPProperties(), data);
                        return true;
                    } catch (ShutdownSignalException e) {
                        logger.warn(String.format("recoverable send fails %s times, will continue to retry %s times; %s",
                                count, CloudBusGlobalProperty.RABBITMQ_RECOVERABLE_SEND_TIMES-count, e.getMessage()));
                        count ++;
                    }
                }

                return false;
            }
        }

        @Override
        public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject jObj = jsonElement.getAsJsonObject();
            Map.Entry<String, JsonElement> entry = jObj.entrySet().iterator().next();
            String className = entry.getKey();
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(String.format("Unable to deserialize class[%s]", className), e);
            }
            return (Message) gson.fromJson(entry.getValue(), clazz);
        }

        @Override
        public JsonElement serialize(Message message, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jObj = new JsonObject();
            jObj.add(message.getClass().getName(), gson.toJsonTree(message));
            return jObj;
        }

        public void send(Message msg) {
            // for unit test finding invocation chain
            MessageCommandRecorder.record(msg.getClass());

            List<BeforeSendMessageInterceptor> interceptors = beforeSendMessageInterceptors.get(msg.getClass());
            if (interceptors != null) {
                for (BeforeSendMessageInterceptor interceptor : interceptors) {
                    interceptor.beforeSendMessage(msg);

                    /*
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("called %s for message[%s]", interceptor.getClass(), msg.getClass()));
                    }
                    */
                }
            }

            for (BeforeSendMessageInterceptor interceptor : beforeSendMessageInterceptorsForAll) {
                interceptor.beforeSendMessage(msg);

                /*
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("called %s for message[%s]", interceptor.getClass(), msg.getClass()));
                }
                */
            }

            send(msg, true);
        }

        public boolean logMessage(Message msg) {
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                return true;
            }

            if (msg instanceof APISyncCallMessage || msg instanceof APIReply) {
                return new LogUtils().isLogReadAPI();
            }

            if (CloudBusGlobalProperty.MESSAGE_LOG_FILTER_ALL) {
                return !filterMsgNames.contains(msg.getClass().getName());
            } else {
                return filterMsgNames.contains(msg.getClass().getName());
            }
        }

        private void buildSchema(Message msg) {
            try {
                msg.putHeaderEntry(CloudBus.HEADER_SCHEMA, new JsonSchemaBuilder(msg).build());
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }

        public void send(final Message msg, boolean makeQueueName) {
            /*
            StopWatch watch = new StopWatch();
            watch.start();
            */
            String serviceId = msg.getServiceId();
            if (makeQueueName) {
                serviceId = makeMessageQueueName(serviceId);
            }

            buildSchema(msg);

            evalThreadContextToMessage(msg);

            if (logger.isTraceEnabled() && logMessage(msg)) {
                logger.trace(String.format("[msg send]: %s", wire.dumpMessage(msg)));
            }


            Channel chan = channelPool.acquire();
            try {
                new RecoverableSend(chan, msg, serviceId, outboundQueue.getBusExchange()).send();
                /*
                watch.stop();
                logger.debug(String.mediaType("sending %s cost %sms", msg.getClass().getName(), watch.getTime()));
                */
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            } finally {
                channelPool.returnChannel(chan);
            }
        }

        public void publish(Event evt) {
            /*
            StopWatch watch = new StopWatch();
            watch.start();
            */

            buildSchema(evt);

            evalThreadContextToMessage(evt);

            if (logger.isTraceEnabled() && logMessage(evt)) {
                logger.trace(String.format("[event publish]: %s", wire.dumpMessage(evt)));
            }

            Channel chan = channelPool.acquire();
            try {
                new RecoverableSend(chan, evt, evt.getType().toString(), BusExchange.BROADCAST).send();
                /*
                watch.stop();
                logger.debug(String.mediaType("sending %s cost %sms", evt.getClass().getName(), watch.getTime()));
                */
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            } finally {
                channelPool.returnChannel(chan);
            }
        }

        private void restoreFromSchema(Message msg, byte[] binary) throws ClassNotFoundException {
            Map<String, String> schema = msg.getHeaderEntry(CloudBus.HEADER_SCHEMA);
            if (schema == null) {
                return;
            }

            Map raw = JSONObjectUtil.toObject(new String(binary), LinkedHashMap.class);
            raw = (Map) raw.values().iterator().next();
            List<String> paths = new ArrayList<>();
            paths.addAll(schema.keySet());
            //paths.sort(Comparator.reverseOrder());

            for (String p : paths) {
                Object dst = getProperty(msg, p);
                String type = schema.get(p);

                if (dst.getClass().getName().equals(type)) {
                    continue;
                }

                Class clz = Class.forName(type);
                setProperty(msg, p, JSONObjectUtil.rehashObject(getProperty(raw, p), clz));
            }
        }

        private void tryBestToReplyError(byte[] binary, String errMsg) {
            // try best to reply an error message to invalid JSON formed message
            String msgStr = new String(binary);
            try {
                Map msgObj = JSONObjectUtil.toObject(msgStr, HashMap.class);
                if (msgObj.size() != 1) {
                    return;
                }

                Map msg = (Map) msgObj.values().iterator().next();
                if (!msg.containsKey("id")) {
                    return;
                }
                Map headers = (Map) msg.get("headers");
                if (headers == null) {
                    return;
                }

                String msgName = (String) msgObj.keySet().iterator().next();
                Class msgClass = Class.forName(msgName);
                Message msgInstance = (Message) msgClass.newInstance();
                msgInstance.setHeaders(headers);
                msgInstance.setId((String) msg.get("id"));
                replyErrorByMessageType(msgInstance, argerr("message is not in correct JSON mediaType").withException(errMsg));
            } catch (Exception e) {
                logger.warn(String.format("unable to handle JsonSyntaxException of message: %s", msgStr), e);
            }
        }

        public Message toMessage(byte[] binary, AMQP.BasicProperties basicProperties) {
            /*
            StopWatch watch = new StopWatch();
            watch.start();
            */
            try {
                byte[] data;
                if (basicProperties.getHeaders() != null && basicProperties.getHeaders().containsKey(AMQP_PROPERTY_HEADER__COMPRESSED)) {
                    data = Compresser.inflate(binary);
                } else {
                    data = binary;
                }

                Message msg = gson.fromJson(new String(data), Message.class);
                msg.setAMQPProperties(basicProperties);

                try {
                    restoreFromSchema(msg, data);
                } catch (Exception e) {
                    logger.warn(String.format("error to restore the msg:\n%s", JSONObjectUtil.toJsonString(msg)), e);
                    throw new CloudRuntimeException(e);
                }

            /*
            watch.stop();
            logger.debug(String.mediaType("receive %s cost %sms", msg.getClass().getName(), watch.getTime()));
            */
                return msg;
            } catch (RuntimeException je) {
                logger.warn(je.getMessage(), je);
                tryBestToReplyError(binary, je.getMessage());
                throw je;
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        public String dumpMessage(Message msg) {
            return gson1.toJson(msg, Message.class);
        }
    }

    private interface EventListenerWrapper {
        void callEventListener(Event e);
    }

    private class EventMaid extends AbstractConsumer {
        Map<String, List<EventListenerWrapper>> listeners = new ConcurrentHashMap<String, List<EventListenerWrapper>>();
        Channel eventChan;
        String queueName = makeEventQueueName(String.format("eventMaid.%s", Platform.getUuid()));

        public void construct() {
            try {
                eventChan = conn.createChannel();
                eventChan.queueDeclare(queueName, false, false, true, queueArguments());
                eventChan.basicConsume(queueName, true, this);
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        public void destruct() {
            try {
                eventChan.close();
            } catch (IOException | TimeoutException e) {
                throw new CloudRuntimeException(e);
            }
        }


        public void listen(Event evt, EventListenerWrapper l) {
            String type = evt.getType().toString();
            try {
                synchronized (listeners) {
                    List<EventListenerWrapper> lst = listeners.get(type);
                    if (lst == null) {
                        lst = new CopyOnWriteArrayList<EventListenerWrapper>();
                        listeners.put(type, lst);
                        eventChan.queueBind(queueName, BusExchange.BROADCAST.toString(), type);
                        logger.debug(String.format("[listening event]: %s", type));
                    }

                    if (!lst.contains(l)) {
                        lst.add(l);
                    }
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        public void unlisten(Event evt, EventListenerWrapper l) {
            String type = evt.getType().toString();
            try {
                synchronized (listeners) {
                    List<EventListenerWrapper> lst = listeners.get(type);
                    if (lst == null) {
                        return;
                    }

                    lst.remove(l);
                    if (lst.isEmpty()) {
                        listeners.remove(type);
                        eventChan.queueUnbind(queueName, BusExchange.BROADCAST.toString(), type);
                        logger.debug(String.format("[unlistening event]: %s", type));
                    }
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        @SyncThread(level = 10)
        @MessageSafe
        private void dispatch(Event evt, EventListenerWrapper l) {
            setThreadLoggingContext(evt);

            l.callEventListener(evt);
        }


        private void handle(Event evt) {
            String type = evt.getType().toString();
            List<EventListenerWrapper> lst = listeners.get(type);
            if (lst == null) {
                return;
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[event received]: %s", wire.dumpMessage(evt)));
            }

            for (EventListenerWrapper l : lst) {
                dispatch(evt, l);
            }
        }

        @Override
        public void handleDelivery(String s, com.rabbitmq.client.Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
            Event evt = null;
            try {
                evt = (Event) wire.toMessage(bytes, basicProperties);
                handle(evt);
            } catch (final Throwable t) {
                final Event fevt = evt;
                throwableSafe(new Runnable() {
                    @Override
                    public void run() {
                        if (fevt != null) {
                            logger.warn(String.format("unhandled throwable when handling event[%s], dump: %s", fevt.getClass().getName(), wire.dumpMessage(fevt)), t);
                        } else {
                            logger.warn(String.format("unhandled throwable"), t);
                        }
                    }
                });
            }
        }
    }

    private EventMaid maid = new EventMaid();

    private abstract class Envelope {
        long startTime;

        {
            if (CloudBusGlobalConfig.STATISTICS_ON.value(Boolean.class)) {
                startTime = System.currentTimeMillis();
            }
        }

        void count(Message msg) {
            if (!CloudBusGlobalConfig.STATISTICS_ON.value(Boolean.class)) {
                return;
            }

            long timeCost = System.currentTimeMillis() - startTime;
            MessageStatistic statistic = statistics.get(msg.getClass().getName());
            statistic.count(timeCost);
        }

        abstract void ack(MessageReply reply);

        abstract void timeout();

        abstract List<Message> getRequests();
    }

    private interface EndPoint {
        void active();

        void inactive();

        void dismiss();
    }


    private final Wire wire = new Wire();


    private interface MessageTrackerEnvelope {
        String getMessageId();

        boolean isTimeout();

        void checkManagementNodeFailureApplyToMe(String nodeId);

        void dismiss();
    }

    private class MessageTracker extends AbstractConsumer {
        private Map<String, MessageTrackerEnvelope> messages = new ConcurrentHashMap<String, MessageTrackerEnvelope>();
        private List<String> bindingKeys = new ArrayList<String>();
        private final String name = makeLocalServiceId("MessageTracker");
        private Map<String, Class> metaDataClassCache = new HashMap<String, Class>();

        {
            metaDataClassCache.put(RequestMessageMetaData.class.getName(), RequestMessageMetaData.class);
            metaDataClassCache.put(ResponseMessageMetaData.class.getName(), ResponseMessageMetaData.class);
            metaDataClassCache.put(LockMessageMetaData.class.getName(), LockMessageMetaData.class);
        }

        void trackService(String servId) {
            if (trackerClose) {
                return;
            }

            String[] pairs = servId.split("\\.");
            if (pairs.length < 2) {
                // don't track services not using management node id in service id
                return;
            }
            pairs[pairs.length-1] = "*";
            String bindingKey = makeMessageQueueName(StringUtils.join(pairs, "."));

            bindingKeys.add(bindingKey);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("message tracker binds to key[%s], tracking service[%s]", bindingKey, pairs[0]));
            }

            try {
                Channel chan = channelPool.acquire();
                try {
                    chan.queueBind(name, BusExchange.P2P.toString(), bindingKey);
                } finally {
                    channelPool.returnChannel(chan);
                }
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        void construct() throws IOException {
            if (trackerClose) {
                return;
            }

            Channel chan = channelPool.acquire();
            try {
                chan.queueDeclare(name, false, false, true, null);
                chan.basicConsume(name, true, tracker);
                chan.queueBind(name, BusExchange.BROADCAST.toString(), "#");
            } finally {
                channelPool.returnChannel(chan);
            }

            final Integer interval = CloudBusGlobalProperty.TRACKER_GARBAGE_COLLECTOR_INTERVAL;
            thdf.submitPeriodicTask(new PeriodicTask() {
                @Override
                public TimeUnit getTimeUnit() {
                    return TimeUnit.SECONDS;
                }

                @Override
                public long getInterval() {
                    return interval;
                }

                @Override
                public String getName() {
                    return "cloudbus-message-tracker-garbage-collector";
                }

                @Override
                public void run() {
                    if (messages.size() > 500) {
                        logger.warn(String.format("there are more than 500 in message tracker[size:%s]", messages.size()));
                    }
                    Iterator<Map.Entry<String, MessageTrackerEnvelope>> it = messages.entrySet().iterator();
                    while (it.hasNext()) {
                        MessageTrackerEnvelope me = it.next().getValue();
                        if (me.isTimeout()) {
                            me.dismiss();
                        }
                    }
                }
            }, TimeUnit.SECONDS.toMillis(60));


            evtf.on(LockResourceMessage.UNLOCK_CANONICAL_EVENT_PATH, new EventCallback() {
                @Override
                public void run(Map tokens, Object data) {
                    if (data != null) {
                        MessageTrackerEnvelope e = messages.get(data);
                        if (e != null) {
                            e.dismiss();
                        }
                    }
                }
            });
        }

        void destruct() {
            if (trackerClose) {
                return;
            }

            managementNodeLeft(Platform.getManagementServerId());

            throwableSafe(new ExceptionDSL.RunnableWithThrowable() {
                @Override
                public void run() throws Throwable {
                    Channel chan = channelPool.acquire();
                    try {
                        chan.queueUnbind(name, BusExchange.BROADCAST.toString(), "#");
                    } finally {
                        channelPool.returnChannel(chan);
                    }
                }
            });

            for (final String servId : bindingKeys) {
                throwableSafe(new ExceptionDSL.RunnableWithThrowable() {
                    @Override
                    public void run() throws Throwable {
                        Channel chan = channelPool.acquire();
                        try {
                            chan.queueUnbind(name, BusExchange.P2P.toString(), servId);
                        } finally {
                            channelPool.returnChannel(chan);
                        }
                    }
                });
            }
        }

        @Override
        public void handleDelivery(String s, com.rabbitmq.client.Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
            try {
                Map<String, Object> headers = basicProperties.getHeaders();
                if (headers == null || !headers.containsKey(CloudBus.HEADER_MESSAGE_META_DATA)) {
                    return;
                }

                LongString metaData = (LongString) headers.get(CloudBus.HEADER_MESSAGE_META_DATA);
                Map m = JSONObjectUtil.toObject(new String(metaData.getBytes()), LinkedHashMap.class);
                trackMessage((MessageMetaData) JSONObjectUtil.rehashObject(m, metaDataClassCache.get(m.get("className"))));
            } catch (Throwable t) {
                logger.warn("unhandled throwable", t);
            }
        }

        @AsyncThread
        private void trackMessage(final MessageMetaData metaData) {
            if (metaData instanceof LockMessageMetaData) {
                final LockMessageMetaData lmeta = (LockMessageMetaData) metaData;
                MessageTrackerEnvelope e = new MessageTrackerEnvelope() {
                    @Override
                    public String getMessageId() {
                        return lmeta.unlockKey;
                    }

                    @Override
                    public boolean isTimeout() {
                        // lock message never time out
                        return false;
                    }

                    @Override
                    public void checkManagementNodeFailureApplyToMe(String nodeId) {
                        if (!nodeId.equals(lmeta.senderManagementUuid)) {
                            return;
                        }

                        unlock();
                    }

                    @Override
                    public void dismiss() {
                        messages.remove(getMessageId());
                    }

                    private void unlock() {
                        logger.warn(String.format("management node[uuid:%s] becomes unavailable, publish unlock event[unlock key:%s, lock reason:%s] for resource it holds",
                                lmeta.senderManagementUuid, lmeta.unlockKey, lmeta.reason));
                        evtf.fire(LockResourceMessage.UNLOCK_CANONICAL_EVENT_PATH, lmeta.unlockKey);
                        dismiss();
                    }
                };

                messages.put(e.getMessageId(), e);
            } else if (metaData instanceof RequestMessageMetaData) {
                final RequestMessageMetaData rmeta = (RequestMessageMetaData) metaData;
                String[] srvIds = rmeta.serviceId.split("\\.");
                if (srvIds.length < 2) {
                    return;
                }

                final String mgmtNodeId = srvIds[srvIds.length-1];

                MessageTrackerEnvelope e = new MessageTrackerEnvelope() {
                    Timestamp timeout = new Timestamp(new Date().getTime() + rmeta.timeout);
                    AtomicBoolean dismissed = new AtomicBoolean(false);

                    @Override
                    public String getMessageId() {
                        return rmeta.msgId;
                    }

                    @Override
                    public boolean isTimeout() {
                        Timestamp now = new Timestamp(new Date().getTime());
                        return now.after(timeout);
                    }

                    private void replyError() {
                        if (dismissed.get()) {
                            return;
                        }

                        ErrorCode err = err(SysErrors.MANAGEMENT_NODE_UNAVAILABLE_ERROR,
                                "management node[uuid:%s] is unavailable", mgmtNodeId);

                        logger.warn(String.format("management node[uuid:%s] becomes unavailable, reply %s to message[%s]. Message metadata dump: %s",
                                mgmtNodeId, err, rmeta.messageName, JSONObjectUtil.toJsonString(rmeta)));

                        if (rmeta.needApiEvent) {
                            APIEvent evt = new APIEvent(rmeta.msgId);
                            eventProperty(evt);
                            evt.setError(err);
                            wire.publish(evt);
                        } else {
                            MessageReply reply = new MessageReply();
                            reply.setError(err);
                            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
                            reply.setAMQPProperties(builder.deliveryMode(1).build());
                            reply.getHeaders().put(CloudBus.HEADER_IS_MESSAGE_REPLY, Boolean.TRUE.toString());
                            reply.putHeaderEntry(CloudBus.HEADER_CORRELATION_ID, rmeta.msgId);
                            reply.setServiceId(rmeta.replyTo);
                            wire.send(reply, false);
                        }

                        dismiss();
                    }

                    @Override
                    public void checkManagementNodeFailureApplyToMe(String nodeId) {
                        if (!nodeId.equals(mgmtNodeId)) {
                            return;
                        }

                        replyError();
                    }

                    @Override
                    public void dismiss() {
                        if (!dismissed.compareAndSet(false, true)) {
                            return;
                        }
                        messages.remove(getMessageId());
                    }
                };

                messages.put(e.getMessageId(), e);
            } else {
                ResponseMessageMetaData remeta = (ResponseMessageMetaData) metaData;
                MessageTrackerEnvelope e = messages.get(remeta.correlationId);
                if (e!=null) {
                    e.dismiss();
                }
            }
        }


        void managementNodeLeft(String nodeId) {
            Iterator<Map.Entry<String, MessageTrackerEnvelope>> it = messages.entrySet().iterator();
            while (it.hasNext()) {
                MessageTrackerEnvelope me = it.next().getValue();
                me.checkManagementNodeFailureApplyToMe(nodeId);
            }
        }
    }

    private MessageTracker tracker;

    void init() {
        trackerClose = CloudBusGlobalProperty.CLOSE_TRACKER;
        serverIps = CloudBusGlobalProperty.SERVER_IPS;
        tracker = new MessageTracker();

        ConnectionFactory connFactory = new ConnectionFactory();
        List<Address> addresses = transformAndRemoveNull(serverIps, Address::parseAddress);
        connFactory.setAutomaticRecoveryEnabled(true);
        connFactory.setRequestedHeartbeat(CloudBusGlobalProperty.RABBITMQ_HEART_BEAT_TIMEOUT);
        connFactory.setNetworkRecoveryInterval((int) TimeUnit.SECONDS.toMillis(CloudBusGlobalProperty.RABBITMQ_NETWORK_RECOVER_INTERVAL));
        connFactory.setConnectionTimeout((int) TimeUnit.SECONDS.toMillis(CloudBusGlobalProperty.RABBITMQ_CONNECTION_TIMEOUT));

        logger.info(String.format("use RabbitMQ server IPs: %s", serverIps));

        try {
            if (CloudBusGlobalProperty.RABBITMQ_USERNAME != null) {
                connFactory.setUsername(CloudBusGlobalProperty.RABBITMQ_USERNAME);
                logger.info(String.format("use RabbitMQ username: %s", CloudBusGlobalProperty.RABBITMQ_USERNAME));
            }
            if (CloudBusGlobalProperty.RABBITMQ_PASSWORD != null) {
                connFactory.setPassword(CloudBusGlobalProperty.RABBITMQ_PASSWORD);
                logger.info("use RabbitMQ password: ******");
            }
            if (CloudBusGlobalProperty.RABBITMQ_VIRTUAL_HOST != null) {
                connFactory.setVirtualHost(CloudBusGlobalProperty.RABBITMQ_VIRTUAL_HOST);
                logger.info(String.format("use RabbitMQ virtual host: %s", CloudBusGlobalProperty.RABBITMQ_VIRTUAL_HOST));
            }

            conn = connFactory.newConnection(addresses.toArray(new Address[]{}));
            logger.debug(String.format("rabbitmq connection is established on %s", conn.getAddress()));

            ((Recoverable)conn).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecovery(Recoverable recoverable) {
                    logger.info(String.format("rabbitmq connection is recovering on %s", conn.getAddress().toString()));
                }

                @Override
                public void handleRecoveryStarted(Recoverable recoverable) {
                    logger.info(String.format("start to recover rabbitmq connection on %s", conn.getAddress().toString()));
                }
            });

            channelPool = new ChannelPool(CloudBusGlobalProperty.CHANNEL_POOL_SIZE, conn);
            createExchanges();
            outboundQueue = new BusQueue(makeMessageQueueName(SERVICE_ID), BusExchange.P2P);
            Channel chan = channelPool.acquire();
            chan.queueDeclare(outboundQueue.getName(), false, false, true, queueArguments());
            chan.basicConsume(outboundQueue.getName(), true, consumer);
            chan.queueBind(outboundQueue.getName(), outboundQueue.getBusExchange().toString(), outboundQueue.getBindingKey());
            channelPool.returnChannel(chan);
            maid.construct();
            noRouteEndPoint.construct();
            tracker.construct();
            tracker.trackService(SERVICE_ID);
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public void activeService(Service serv) {
        activeService(serv.getId());
    }

    @Override
    public void activeService(String id) {
        EndPoint e = endpoints.get(id);
        e.active();
    }

    @Override
    public void deActiveService(Service serv) {
        deActiveService(serv.getId());
    }

    @Override
    public void deActiveService(String id) {
        EndPoint e = endpoints.get(id);
        e.inactive();
    }

    protected void basicProperty(Message msg) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        msg.setAMQPProperties(builder.deliveryMode(1).expiration(String.valueOf(TimeUnit.SECONDS.toMillis(CloudBusGlobalProperty.MESSAGE_TTL))).build());
    }

    protected void eventProperty(Event event) {
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        event.setAMQPProperties(builder.deliveryMode(1).expiration(String.valueOf(TimeUnit.SECONDS.toMillis(CloudBusGlobalProperty.MESSAGE_TTL))).build());
    }

    private void buildRequestMessageMetaData(Message msg) {
        if (msg instanceof APIMessage || (msg instanceof NeedReplyMessage && !Boolean.valueOf(msg.getHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG)))) {
            RequestMessageMetaData metaData;
            if (msg instanceof LockResourceMessage) {
                LockResourceMessage lmsg = (LockResourceMessage) msg;
                LockMessageMetaData lmetaData = new LockMessageMetaData();
                lmetaData.unlockKey = lmsg.getUnlockKey();
                lmetaData.reason = lmsg.getReason();
                lmetaData.senderManagementUuid = Platform.getManagementServerId();
                metaData = lmetaData;
            } else {
                metaData = new RequestMessageMetaData();
            }

            metaData.needApiEvent = msg instanceof APIMessage && !(msg instanceof APISyncCallMessage);
            metaData.msgId = msg.getId();
            metaData.replyTo = msg.getHeaderEntry(CloudBus.HEADER_REPLY_TO);
            metaData.timeout = msg instanceof NeedReplyMessage ? ((NeedReplyMessage) msg).getTimeout() : null;
            metaData.serviceId = msg.getServiceId();
            metaData.messageName = msg.getClass().getName();
            metaData.className = metaData.getClass().getName();
            msg.getAMQPHeaders().put(CloudBus.HEADER_MESSAGE_META_DATA, JSONObjectUtil.toJsonString(metaData));
        }
    }

    private void send(Message msg, Boolean noNeedReply) {
        if (msg.getServiceId() == null) {
            throw new IllegalArgumentException(String.format("service id cannot be null: %s", msg.getClass().getName()));
        }

        basicProperty(msg);

        msg.putHeaderEntry(CloudBus.HEADER_CORRELATION_ID, msg.getId());
        msg.putHeaderEntry(CloudBus.HEADER_REPLY_TO, outboundQueue.getBindingKey());
        if (msg instanceof APIMessage) {
            // API always need reply
            msg.putHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG, Boolean.FALSE.toString());
        } else if (msg instanceof NeedReplyMessage) {
            // for NeedReplyMessage sent without requiring receiver to reply,
            // mark it, then it will not be tracked and replied
            msg.putHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG, noNeedReply.toString());
        }

        buildRequestMessageMetaData(msg);
        wire.send(msg);
    }

    @Override
    public FutureCompletion send(Message msg) {
        send(msg, true);
        return SEND_CONFIRMED;
    }

    @Override
    public <T extends Message> void send(List<T> msgs) {
        for (Message msg : msgs) {
            send(msg, true);
        }
    }

    @Override
    public void send(APIMessage msg, java.util.function.Consumer<APIEvent> consumer) {
        subscribeEvent((e) -> {
            APIEvent ae = (APIEvent) e;
            if (ae.getApiId().equals(msg.getId())) {
                consumer.accept(ae);
                return true;
            }

            return false;
        }, new APIEvent());

        send(msg);
    }

    private void evaluateMessageTimeout(NeedReplyMessage msg) {
        timeoutMgr.setMessageTimeout(msg);
    }

    @Override
    public FutureCompletion send(final NeedReplyMessage msg, final CloudBusCallBack callback) {
        evaluateMessageTimeout(msg);

        Envelope e = new Envelope() {
            AtomicBoolean called = new AtomicBoolean(false);

            final Envelope self = this;
            TimeoutTaskReceipt timeoutTaskReceipt = thdf.submitTimeoutTask(new Runnable() {
                @Override
                public void run() {
                    self.timeout();
                }
            }, TimeUnit.MILLISECONDS, msg.getTimeout());

            @Override
            public void ack(MessageReply reply) {
                count(msg);

                envelopes.remove(msg.getId());

                if (!called.compareAndSet(false, true)) {
                    return;
                }

                timeoutTaskReceipt.cancel();

                callback.run(reply);
            }

            @Override
            public void timeout() {
                envelopes.remove(msg.getId());

                if (!called.compareAndSet(false, true)) {
                    return;
                }

                callback.run(createTimeoutReply(msg));
            }

            @Override
            List<Message> getRequests() {
                List<Message> requests = new ArrayList<Message>();
                requests.add(msg);
                return requests;
            }
        };

        envelopes.put(msg.getId(), e);

        send(msg, false);
        return SEND_CONFIRMED;
    }

    private MessageReply createTimeoutReply(NeedReplyMessage m) {
        MessageReply r = new MessageReply();
        r.putHeaderEntry(CloudBus.HEADER_CORRELATION_ID, m.getId());
        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        r.setAMQPProperties(builder.deliveryMode(1).build());
        r.setError(touterr(m.toErrorString()));
        return r;
    }

    @Override
    public void send(final List<? extends NeedReplyMessage> msgs, final CloudBusListCallBack callBack) {
        DebugUtils.Assert(!msgs.isEmpty(), "you can not pass an empty message list to me");

        long minTimeout = Long.MAX_VALUE;
        for (NeedReplyMessage msg : msgs) {
            evaluateMessageTimeout(msg);
            minTimeout = minTimeout < msg.getTimeout() ? minTimeout : msg.getTimeout();
        }

        final long timeout = minTimeout;
        Envelope e = new Envelope() {
            AtomicBoolean isTimeout = new AtomicBoolean(false);

            Map<String, MessageReply> replies = new HashMap(msgs.size());

            final Envelope self = this;
            TimeoutTaskReceipt timeoutTaskReceipt = thdf.submitTimeoutTask(new Runnable() {
                @Override
                public void run() {
                    self.timeout();
                }
            }, TimeUnit.MILLISECONDS, timeout);

            private void cleanup(boolean cancelTimeout) {
                for (Message msg : msgs) {
                    envelopes.remove(msg.getId());
                }

                if (cancelTimeout) {
                    timeoutTaskReceipt.cancel();
                }
            }

            private MessageReply findReply(final Message msg) {
                for (MessageReply arg : replies.values()) {
                    if (arg.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID).equals(msg.getId())) {
                        return arg;
                    }
                }
                return null;
            }

            private void doCount(MessageReply reply) {
                for (Message msg : msgs) {
                    if (msg.getId().equals(reply.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID))) {
                        count(msg);
                        return;
                    }
                }
            }

            @Override
            public synchronized void ack(MessageReply reply) {
                if (isTimeout.get()) {
                    return;
                }

                doCount(reply);

                replies.put(reply.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID), reply);

                if (replies.size() == msgs.size()) {
                    cleanup(true);
                    List<MessageReply> ret = new ArrayList<MessageReply>();
                    for (final Message m : msgs) {
                        MessageReply r = findReply(m);
                        DebugUtils.Assert(r != null, String.format("cannot find reply for message:%s", wire.dumpMessage(m)));
                        ret.add(r);
                    }
                    callBack.run(ret);
                }
            }

            @Override
            public void timeout() {
                if (!isTimeout.compareAndSet(false, true)) {
                    return;
                }

                cleanup(false);
                List<MessageReply> ret = new ArrayList<MessageReply>();
                for (final NeedReplyMessage m : msgs) {
                    MessageReply r = findReply(m);
                    if (r == null) {
                        r = createTimeoutReply(m);
                    }
                    ret.add(r);
                }
                callBack.run(ret);
            }

            @Override
            List<Message> getRequests() {
                List<Message> requests = new ArrayList<Message>();
                requests.addAll(msgs);
                return requests;
            }
        };

        for (NeedReplyMessage msg : msgs) {
            envelopes.put(msg.getId(), e);
        }

        for (NeedReplyMessage msg : msgs) {
            send(msg, false);
        }
    }

    private void parallelSend(final Iterator<NeedReplyMessage> it, final List<MessageReply> replies, final int num, final NoErrorCompletion completion) {
        NeedReplyMessage msg = null;
        synchronized (it) {
            if (!it.hasNext()) {
                return;
            }
            msg = it.next();
        }

        final NeedReplyMessage fmsg = msg;
        send(fmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                int replyNum;
                synchronized (replies) {
                    replies.add(reply);
                    replyNum = replies.size();
                }

                if (replyNum == num) {
                    completion.done();
                } else {
                    parallelSend(it, replies, num, completion);
                }
            }
        });
    }

    @Override
    public void send(final List<? extends NeedReplyMessage> msgs, final int parallelLevel, final CloudBusListCallBack callBack) {
        DebugUtils.Assert(!msgs.isEmpty(), "you cannot pass an empty message list to me");
        for (NeedReplyMessage msg : msgs) {
            evaluateMessageTimeout(msg);
        }

        List<NeedReplyMessage> copy = new ArrayList<NeedReplyMessage>();
        copy.addAll(msgs);

        int num = Math.min(parallelLevel, msgs.size());

        List<? extends NeedReplyMessage> sub = copy.subList(0, num);
        List<NeedReplyMessage> init = new ArrayList<NeedReplyMessage>();
        init.addAll(sub);
        sub.clear();

        final Iterator<NeedReplyMessage> it = copy.iterator();
        final List<MessageReply> replies = new ArrayList<>();
        final int retNum = msgs.size();
        for (NeedReplyMessage nmsg : init) {
            send(nmsg, new CloudBusCallBack(null) {

                private MessageReply findReply(final Message msg) {
                    return findOneOrNull(replies, arg -> arg.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID).equals(msg.getId()));
                }

                private List<MessageReply> sortReplies() {
                    List<MessageReply> ret = new ArrayList<MessageReply>();
                    for (final Message m : msgs) {
                        MessageReply r = findReply(m);
                        DebugUtils.Assert(r != null, String.format("cannot find reply for message:%s", wire.dumpMessage(m)));
                        ret.add(r);
                    }

                    return ret;
                }

                @Override
                public void run(MessageReply reply) {
                    synchronized (replies) {
                        replies.add(reply);
                        if (replies.size() == retNum) {
                            callBack.run(sortReplies());
                            return;
                        }

                        parallelSend(it, replies, retNum, new NoErrorCompletion() {
                            @Override
                            public void done() {
                                callBack.run(sortReplies());
                            }
                        });
                    }
                }
            });
        }
    }

    private void steppingSend(final Iterator<NeedReplyMessage> it, final CloudBusSteppingCallback callback) {
        NeedReplyMessage msg = null;
        synchronized (it) {
            if (!it.hasNext()) {
                return;
            }

            msg = it.next();
        }

        final NeedReplyMessage fmsg = msg;
        send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                try {
                    callback.run(fmsg, reply);
                } finally {
                    steppingSend(it, callback);
                }
            }
        });
    }

    @Override
    public void send(final List<? extends NeedReplyMessage> msgs, final int parallelLevel, final CloudBusSteppingCallback callback) {
        DebugUtils.Assert(!msgs.isEmpty(), "you can not pass an empty message list to me");
        for (NeedReplyMessage msg : msgs) {
            evaluateMessageTimeout(msg);
        }

        List<NeedReplyMessage> copy = new ArrayList<NeedReplyMessage>();
        copy.addAll(msgs);

        int num = Math.min(parallelLevel, msgs.size());

        List<? extends NeedReplyMessage> sub = copy.subList(0, num);
        List<NeedReplyMessage> init = new ArrayList<NeedReplyMessage>();
        init.addAll(sub);
        sub.clear();

        final Iterator<NeedReplyMessage> it = copy.iterator();
        for (final NeedReplyMessage msg : init) {
            send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    try {
                        steppingSend(it, callback);
                    } finally {
                        callback.run(msg, reply);
                    }
                }
            });
        }
    }

    @Override
    public void route(List<Message> msgs) {
        for (Message msg : msgs) {
            route(msg);
        }
    }

    @Override
    public void route(Message msg) {
        if (msg.getServiceId() == null) {
            throw new IllegalArgumentException(String.format("service id cannot be null: %s", msg.getClass().getName()));
        }

        if (msg instanceof NeedReplyMessage) {
            evaluateMessageTimeout((NeedReplyMessage) msg);
        }
        buildRequestMessageMetaData(msg);
        wire.send(msg);
    }

    private void callReplyPreSendingExtensions(Message msg, NeedReplyMessage msgReq) {
        List<MarshalReplyMessageExtensionPoint> exts = replyMessageMarshaller.get(msg.getClass());
        if (exts != null) {
            for (MarshalReplyMessageExtensionPoint ext : exts) {
                ext.marshalReplyMessageBeforeSending(msg, msgReq);
            }
        }
    }

    private void buildResponseMessageMetaData(Message msg) {
        if (!(msg instanceof APIEvent) && !(msg instanceof MessageReply)) {
            return;
        }

        ResponseMessageMetaData metaData = new ResponseMessageMetaData();
        metaData.isApiEvent = msg instanceof APIEvent;
        metaData.messageName = msg.getClass().getName();
        metaData.serviceId = metaData.isApiEvent ? null : msg.getServiceId();
        metaData.className = metaData.getClass().getName();
        metaData.correlationId = metaData.isApiEvent ? ((APIEvent)msg).getApiId() : (String) msg.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID);
        msg.getAMQPHeaders().put(CloudBus.HEADER_MESSAGE_META_DATA, JSONObjectUtil.toJsonString(metaData));
    }

    @Override
    public void reply(Message request, MessageReply reply) {
        if (Boolean.valueOf(request.getHeaderEntry(CloudBus.HEADER_NO_NEED_REPLY_MSG))) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("%s in message%s is set, drop reply%s", CloudBus.HEADER_NO_NEED_REPLY_MSG,
                        wire.dumpMessage(request), wire.dumpMessage(reply)));
            }

            return;
        }

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
        reply.setAMQPProperties(builder.deliveryMode(1).build());
        reply.getHeaders().put(CloudBus.HEADER_IS_MESSAGE_REPLY, Boolean.TRUE.toString());
        reply.putHeaderEntry(CloudBus.HEADER_CORRELATION_ID, request.getId());
        reply.setServiceId(request.getHeaderEntry(CloudBus.HEADER_REPLY_TO));

        buildResponseMessageMetaData(reply);
        if (request instanceof NeedReplyMessage) {
            callReplyPreSendingExtensions(reply, (NeedReplyMessage) request);
        }
        wire.send(reply, false);
    }

    @Override
    public void cancel(String correlationId, String error) {
        logger.warn("Unsupported cloudBus2");
    }

    @Override
    public void publish(List<Event> events) {
        for (Event e : events) {
            publish(e);
        }
    }

    @Override
    public void publish(Event event) {
        if (event instanceof APIEvent) {
            APIEvent aevt = (APIEvent) event;
            DebugUtils.Assert(aevt.getApiId() != null, String.format("apiId of %s cannot be null", aevt.getClass().getName()));
        }

        eventProperty(event);
        buildResponseMessageMetaData(event);
        callReplyPreSendingExtensions(event, null);

        BeforePublishEventInterceptor c = null;
        try {
            List<BeforePublishEventInterceptor> is = beforeEventPublishInterceptors.get(event.getClass());
            if (is != null) {
                for (BeforePublishEventInterceptor i : is) {
                    c = i;
                    i.beforePublishEvent(event);
                }
            }

            for (BeforePublishEventInterceptor i : beforeEventPublishInterceptorsForAll)  {
                c = i;
                i.beforePublishEvent(event);
            }
        } catch (StopRoutingException e) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("BeforePublishEventInterceptor[%s] stop publishing event: %s",
                        c == null ? "null" : c.getClass().getName(), JSONObjectUtil.toJsonString(event)));
            }

            return;
        }

        wire.publish(event);
    }

    @Override
    public MessageReply call(final NeedReplyMessage msg) {
        evaluateMessageTimeout(msg);

        final MessageReply[] replies = new MessageReply[1];
        replies[0] = null;

        Envelope e = new Envelope() {
            AtomicBoolean called = new AtomicBoolean(false);

            final Envelope self = this;

            @Override
            public synchronized void ack(MessageReply reply) {
                count(msg);

                envelopes.remove(msg.getId());

                if (!called.compareAndSet(false, true)) {
                    return;
                }

                replies[0] = reply;

                self.notify();
            }

            @Override
            public void timeout() {
                envelopes.remove(msg.getId());
                called.compareAndSet(false, true);
            }

            @Override
            List<Message> getRequests() {
                List<Message> requests = new ArrayList<Message>();
                requests.add(msg);
                return requests;
            }
        };

        envelopes.put(msg.getId(), e);
        send(msg, false);

        synchronized (e) {
            if (replies[0] == null) {
                try {
                    e.wait(msg.getTimeout());
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new CloudRuntimeException(e1);
                }
            }

            if (replies[0] == null) {
                e.timeout();
                return createTimeoutReply(msg);
            }
        }

        return replies[0];
    }

    @Override
    public <T extends NeedReplyMessage> List<MessageReply> call(final List<T> msgs) {
        DebugUtils.Assert(!msgs.isEmpty(), "cannot call empty messages");

        class Result {
            Map<String, MessageReply> replies;
        }

        final Result ret = new Result();

        final Envelope e = new Envelope() {
            AtomicBoolean isTimeout = new AtomicBoolean(false);

            Map<String, MessageReply> replies = new HashMap<String, MessageReply>(msgs.size());

            private void cleanup() {
                for (Message msg : msgs) {
                    envelopes.remove(msg.getId());
                }
            }

            private void doCount(MessageReply reply) {
                for (Message m : msgs) {
                    if (m.getId().equals(reply.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID))) {
                        count(m);
                        return;
                    }
                }
            }

            @Override
            public synchronized void ack(MessageReply reply) {
                if (isTimeout.get()) {
                    return;
                }

                doCount(reply);

                replies.put(reply.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID), reply);

                if (replies.size() == msgs.size()) {
                    cleanup();
                    ret.replies = replies;
                    notify();
                }
            }

            @Override
            public void timeout() {
                if (!isTimeout.compareAndSet(false, true)) {
                    return;
                }

                cleanup();
                ret.replies = replies;
            }

            @Override
            List<Message> getRequests() {
                List<Message> requests = new ArrayList<Message>();
                requests.addAll(msgs);
                return requests;
            }
        };

        long minTimeout = Long.MAX_VALUE;
        for (NeedReplyMessage msg : msgs) {
            evaluateMessageTimeout(msg);

            envelopes.put(msg.getId(), e);
            minTimeout = Math.min(msg.getTimeout(), minTimeout);
        }

        for (NeedReplyMessage msg : msgs) {
            send(msg, false);
        }

        List<MessageReply> res = new ArrayList<MessageReply>(msgs.size());
        synchronized (e) {
            if (ret.replies == null) {
                try {
                    e.wait(minTimeout);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    throw new CloudRuntimeException(e1);
                }
            }

            if (ret.replies != null) {
                Map<String, MessageReply> rmap = ret.replies;
                for (NeedReplyMessage msg : msgs) {
                    MessageReply r = rmap.get(msg.getId());
                    assert r != null;
                    res.add(r);
                }
            } else {
                e.timeout();
                for (NeedReplyMessage msg : msgs) {
                    MessageReply r = ret.replies.get(msg.getId());
                    if (r != null) {
                        res.add(r);
                    } else {
                        res.add(createTimeoutReply(msg));
                    }
                }
            }
        }

        return res;
    }

    private void setThreadLoggingContext(Message msg) {
        ThreadContext.clearAll();

        if (msg instanceof APIMessage) {
            ThreadContext.put(Constants.THREAD_CONTEXT_API, msg.getId());
            ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, msg.getClass().getName());
        } else {
            Map<String, String> ctx = msg.getHeaderEntry(CloudBus.HEADER_TASK_CONTEXT);
            if (ctx != null) {
                ThreadContext.putAll(ctx);
            }
        }

        if (msg.getHeaders().containsKey(CloudBus.HEADER_TASK_STACK)) {
            List<String> taskStack = msg.getHeaderEntry(CloudBus.HEADER_TASK_STACK);
            ThreadContext.setStack(taskStack);
        }
    }

    private void evalThreadContextToMessage(Message msg) {
        Map<String, String> ctx = ThreadContext.getImmutableContext();
        if (ctx != null) {
            msg.putHeaderEntry(CloudBus.HEADER_TASK_CONTEXT, ctx);
        }

        List<String> taskStack = ThreadContext.getImmutableStack().asList();
        if (taskStack != null && !taskStack.isEmpty()) {
            msg.putHeaderEntry(CloudBus.HEADER_TASK_STACK, taskStack);
        }
    }

    private void beforeDeliverMessage(Message msg) {
        List<BeforeDeliveryMessageInterceptor> is = beforeDeliveryMessageInterceptors.get(msg.getClass());
        if (is != null) {
            for (BeforeDeliveryMessageInterceptor i : is) {
                i.beforeDeliveryMessage(msg);
            }
        }

        for (BeforeDeliveryMessageInterceptor i : beforeDeliveryMessageInterceptorsForAll) {
            i.beforeDeliveryMessage(msg);
        }
    }

    @Override
    public void registerService(final Service serv) throws CloudConfigureFailException {
        final List<String> alias = serv.getAliasIds();
        final int syncLevel = serv.getSyncLevel();

        EndPoint e = new EndPoint() {
            Channel echan;
            Consumer handler;
            String baseName;
            List<String> aliasNames = new ArrayList<String>();

            {
                baseName = makeMessageQueueName(serv.getId());
                if (alias != null) {
                    for (String a : alias) {
                        aliasNames.add(makeMessageQueueName(a));
                    }
                }

                handler = new AbstractConsumer() {
                    @Override
                    public void handleDelivery(String s, com.rabbitmq.client.Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
                        try {
                            final Message msg = wire.toMessage(bytes, basicProperties);

                            if (logger.isTraceEnabled() && wire.logMessage(msg)) {
                                logger.trace(String.format("[msg received]: %s", wire.dumpMessage(msg)));
                            }

                            SyncTask<Void> task = new SyncTask<Void>() {
                                @Override
                                public String getSyncSignature() {
                                    return serv.getId();
                                }

                                @Override
                                public int getSyncLevel() {
                                    return syncLevel;
                                }

                                @Override
                                public String getName() {
                                    return String.format("CloudBus EndPoint[%s]", serv.getId());
                                }

                                @Override
                                public Void call() throws Exception {
                                    setThreadLoggingContext(msg);

                                    try {
                                        beforeDeliverMessage(msg);

                                        serv.handleMessage(msg);
                                    } catch (Throwable t) {
                                        logExceptionWithMessageDump(msg, t);

                                        if (t instanceof OperationFailureException) {
                                            replyErrorByMessageType(msg, ((OperationFailureException) t).getErrorCode());
                                        } else {
                                            replyErrorByMessageType(msg, inerr(t.getMessage()));
                                        }
                                    }

                                    return null;
                                }
                            };

                            if (syncLevel == 0) {
                                thdf.submit(task);
                            } else {
                                thdf.syncSubmit(task);
                            }
                        } catch (Throwable t) {
                            logger.warn("unhandled throwable", t);
                        }
                    }
                };
            }

            @Override
            public void active() {
                try {
                    echan = conn.createChannel();
                    echan.queueDeclare(baseName, false, false, true, null);
                    echan.basicConsume(baseName, true, handler);
                    echan.queueBind(baseName, BusExchange.P2P.toString(), baseName);

                    for (String aliasName : aliasNames) {
                        echan.queueDeclare(aliasName, false, false, true, null);
                        echan.basicConsume(aliasName, true, handler);
                        echan.queueBind(aliasName, BusExchange.P2P.toString(), aliasName);
                    }
                } catch (IOException e1) {
                    throw new CloudRuntimeException(e1);
                }
            }

            @Override
            public void inactive() {
                try {
                    echan.queueUnbind(baseName, BusExchange.P2P.toString(), baseName);
                    // consumer will not be removed
                    for (String aliasName: aliasNames) {
                        echan.queueUnbind(aliasName, BusExchange.P2P.toString(), aliasName);
                    }
                    echan.close();
                    echan = null;
                } catch (IOException | TimeoutException e1) {
                    try {
                        if (echan != null) {
                            echan.abort();
                        }
                    } catch (IOException e2) {
                        throw new CloudRuntimeException(e2);
                    }
                    throw new CloudRuntimeException(e1);
                }
            }

            @Override
            public void dismiss() {
                endpoints.remove(serv.getId());
            }
        };

        EndPoint s = endpoints.get(serv.getId());
        if (s != null) {
            throw new CloudRuntimeException(String.format("duplicate id[%s] for Service", serv.getId()));
        }

        endpoints.put(serv.getId(), e);
        activeService(serv);

        logger.debug(String.format("registered service[%s]", serv.getId()));

        tracker.trackService(serv.getId());
        if (alias != null) {
            for (String a : alias) {
                tracker.trackService(a);
            }
        }
    }

    @Override
    public void unregisterService(Service serv) {
        EndPoint e = endpoints.get(serv.getId());
        if (e == null) {
            logger.warn(String.format("cannot find endpoint for service[%s]", serv.getId()));
            return;
        }

        e.dismiss();
    }

    @Override
    public EventSubscriberReceipt subscribeEvent(final CloudBusEventListener listener, final Event... events) {
        final EventListenerWrapper wrapper = new EventListenerWrapper() {
            @Override
            public void callEventListener(Event e) {
                if (listener.handleEvent(e)) {
                    maid.unlisten(e, this);
                }
            }
        };

        for (Event e : events) {
            maid.listen(e, wrapper);
        }

        return new EventSubscriberReceipt() {
            @Override
            public void unsubscribe(Event e) {
                maid.unlisten(e, wrapper);
            }

            @Override
            public void unsubscribeAll() {
                for (Event e : events) {
                    maid.unlisten(e, wrapper);
                }
            }
        };
    }

    @Override
    public void dealWithUnknownMessage(Message msg) {
        String details = String.format("No service deals with message: %s", wire.dumpMessage(msg));
        if (msg instanceof APISyncCallMessage) {
            APIReply reply = new APIReply();
            reply.setError(err(SysErrors.UNKNOWN_MESSAGE_ERROR, details));
            reply.setSuccess(false);
            this.reply(msg, reply);
        } else if (msg instanceof APIMessage) {
            APIEvent evt = new APIEvent(msg.getId());
            evt.setError(err(SysErrors.UNKNOWN_MESSAGE_ERROR, details));
            this.publish(evt);
        } else if (msg instanceof NeedReplyMessage) {
            MessageReply reply = new MessageReply();
            reply.setError(err(SysErrors.UNKNOWN_MESSAGE_ERROR, details));
            reply.setSuccess(false);
            this.reply(msg, reply);
        }

        DebugUtils.dumpStackTrace("Dropped an unknown message, " + details);
    }

    private void replyErrorIfMessageNeedReply(Message msg, String errStr) {
        if (msg instanceof NeedReplyMessage) {
            ErrorCode err = inerr(errStr);
            replyErrorIfMessageNeedReply(msg, err);
        } else {
            DebugUtils.dumpStackTrace(String.format("An error happened when dealing with message[%s], because this message doesn't need a reply, we call it out loudly\nerror: %s\nmessage dump: %s", msg.getClass().getName(), errStr, wire.dumpMessage(msg)));
        }
    }

    private void replyErrorIfMessageNeedReply(Message msg, ErrorCode code) {
        if (msg instanceof NeedReplyMessage) {
            MessageReply reply = new MessageReply();
            reply.setError(code);
            reply.setSuccess(false);
            this.reply(msg, reply);
        }
    }

    private void replyErrorToApiMessage(APIMessage msg, String err) {
        replyErrorToApiMessage(msg, inerr(err));
    }

    private void replyErrorToApiMessage(APIMessage msg, ErrorCode err) {
        if (msg instanceof APISyncCallMessage) {
            APIReply reply = new APIReply();
            reply.setError(err);
            reply.setSuccess(false);
            this.reply(msg, reply);
        } else if (msg instanceof APISearchMessage) {
            APISearchReply reply = new APISearchReply();
            reply.setError(err);
            reply.setSuccess(false);
            this.reply(msg, reply);
        } else {
            APIEvent evt = new APIEvent(msg.getId());
            evt.setError(err);
            evt.setSuccess(false);
            this.publish(evt);
        }
    }

    @Override
    public void replyErrorByMessageType(Message msg, ErrorCode err) {
        if (msg instanceof APIMessage) {
            replyErrorToApiMessage((APIMessage) msg, err);
        } else {
            replyErrorIfMessageNeedReply(msg, err);
        }
    }

    @Override
    public void replyErrorByMessageType(Message msg, Exception e) {
        if (e instanceof OperationFailureException) {
            replyErrorByMessageType(msg, ((OperationFailureException) e).getErrorCode());
        } else {
            replyErrorByMessageType(msg, e.getMessage());
        }
    }

    @Override
    public void replyErrorByMessageType(Message msg, String err) {
        if (msg instanceof APIMessage) {
            replyErrorToApiMessage((APIMessage) msg, err);
        } else {
            replyErrorIfMessageNeedReply(msg, err);
        }
    }

    @Override
    public void logExceptionWithMessageDump(Message msg, Throwable e) {
        if (!(e instanceof OperationFailureException)) {
            logger.warn(String.format("unhandled throwable happened when dealing with message[%s], dump: %s", msg.getClass().getName(), wire.dumpMessage(msg)), e);
        }
    }

    @Override
    public String getServiceId(String targetServiceId) {
        return targetServiceId.split(".")[0];
    }

    @Override
    public String makeLocalServiceId(String serviceId) {
        return serviceId + "." + Platform.getManagementServerId();
    }

    @Override
    public void makeLocalServiceId(Message msg, String serviceId) {
        msg.setServiceId(makeLocalServiceId(serviceId));
    }

    @Override
    public String makeServiceIdByManagementNodeId(String serviceId, String managementNodeId) {
        return serviceId + "." + managementNodeId;
    }

    @Override
    public void makeServiceIdByManagementNodeId(Message msg, String serviceId, String managementNodeId) {
        msg.setServiceId(makeServiceIdByManagementNodeId(serviceId, managementNodeId));
    }

    @Override
    public String makeTargetServiceIdByResourceUuid(String serviceId, String resourceUuid) {
        DebugUtils.Assert(serviceId!=null, "serviceId cannot be null");
        DebugUtils.Assert(resourceUuid!=null, "resourceUuid cannot be null");
        String mgmtUuid = destMaker.makeDestination(resourceUuid);
        return serviceId + "." + mgmtUuid;
    }

    @Override
    public void makeTargetServiceIdByResourceUuid(Message msg, String serviceId, String resourceUuid) {
        String targetService = makeTargetServiceIdByResourceUuid(serviceId, resourceUuid);
        msg.setServiceId(targetService);
    }

    @Override
    public void installBeforeDeliveryMessageInterceptor(BeforeDeliveryMessageInterceptor interceptor, List<Class<? extends Message>> classes) {
        if (classes.size() == 0) {
            int order = 0;
            for (BeforeDeliveryMessageInterceptor i : beforeDeliveryMessageInterceptorsForAll) {
                if (i.orderOfBeforeDeliveryMessageInterceptor() <= interceptor.orderOfBeforeDeliveryMessageInterceptor()) {
                    order = beforeDeliveryMessageInterceptorsForAll.indexOf(i);
                    break;
                }
            }

            beforeDeliveryMessageInterceptorsForAll.add(order, interceptor);
            return;
        }

        for (Class clz : classes) {
            while (clz != Object.class) {
                List<BeforeDeliveryMessageInterceptor> is = beforeDeliveryMessageInterceptors.get(clz);
                if (is == null) {
                    is = new ArrayList<BeforeDeliveryMessageInterceptor>();
                    beforeDeliveryMessageInterceptors.put(clz, is);
                }

                synchronized (is) {
                    int order = 0;
                    for (BeforeDeliveryMessageInterceptor i : is) {
                        if (i.orderOfBeforeDeliveryMessageInterceptor() <= interceptor.orderOfBeforeDeliveryMessageInterceptor()) {
                            order = is.indexOf(i);
                            break;
                        }
                    }
                    is.add(order, interceptor);
                }

                clz = clz.getSuperclass();
            }
        }
    }

    @Override
    public void installBeforeDeliveryMessageInterceptor(BeforeDeliveryMessageInterceptor interceptor, Class<? extends Message>... classes) {
        installBeforeDeliveryMessageInterceptor(interceptor, Arrays.asList(classes));
    }

    @Override
    public void installBeforeSendMessageInterceptor(BeforeSendMessageInterceptor interceptor, Class<? extends Message>... classes) {
        if (classes.length == 0) {
            int order = 0;
            for (BeforeSendMessageInterceptor i : beforeSendMessageInterceptorsForAll) {
                if (i.orderOfBeforeSendMessageInterceptor() <= interceptor.orderOfBeforeSendMessageInterceptor()) {
                    order = beforeSendMessageInterceptorsForAll.indexOf(i);
                    break;
                }
            }

            beforeSendMessageInterceptorsForAll.add(order, interceptor);
            return;
        }

        for (Class clz : classes) {
            while (clz != Object.class) {
                List<BeforeSendMessageInterceptor> is = beforeSendMessageInterceptors.get(clz);
                if (is == null) {
                    is = new ArrayList<BeforeSendMessageInterceptor>();
                    beforeSendMessageInterceptors.put(clz, is);
                }

                synchronized (is) {
                    int order = 0;
                    for (BeforeSendMessageInterceptor i : is) {
                        if (i.orderOfBeforeSendMessageInterceptor() <= interceptor.orderOfBeforeSendMessageInterceptor()) {
                            order = is.indexOf(i);
                            break;
                        }
                    }
                    is.add(order, interceptor);
                }

                clz = clz.getSuperclass();
            }
        }
    }

    @Override
    public void installBeforePublishEventInterceptor(BeforePublishEventInterceptor interceptor, Class<? extends Event>... classes) {
        if (classes.length == 0) {
            int order = 0;
            for (BeforePublishEventInterceptor i : beforeEventPublishInterceptorsForAll) {
                if (i.orderOfBeforePublishEventInterceptor() <= interceptor.orderOfBeforePublishEventInterceptor()) {
                    order = beforeEventPublishInterceptorsForAll.indexOf(i);
                    break;
                }
            }

            beforeEventPublishInterceptorsForAll.add(order, interceptor);
            return;
        }

        for (Class clz : classes) {
            while (clz != Object.class) {
                List<BeforePublishEventInterceptor> is = beforeEventPublishInterceptors.get(clz);
                if (is == null) {
                    is = new ArrayList<BeforePublishEventInterceptor>();
                    beforeEventPublishInterceptors.put(clz, is);
                }

                synchronized (is) {
                    int order = 0;
                    for (BeforePublishEventInterceptor i : is) {
                        if (i.orderOfBeforePublishEventInterceptor() <= interceptor.orderOfBeforePublishEventInterceptor()) {
                            order = is.indexOf(i);
                            break;
                        }
                    }
                    is.add(order, interceptor);
                }

                clz = clz.getSuperclass();
            }
        }
    }

    private void populateExtension() {
        services = pluginRgty.getExtensionList(Service.class);
        for (MarshalReplyMessageExtensionPoint extp : pluginRgty.getExtensionList(MarshalReplyMessageExtensionPoint.class)) {
            List<Class> clazzs = extp.getReplyMessageClassForMarshalExtensionPoint();
            if (clazzs == null || clazzs.isEmpty()) {
                continue;
            }

            for (Class clz : clazzs) {
                if (!(APIEvent.class.isAssignableFrom(clz)) && !(MessageReply.class.isAssignableFrom(clz))) {
                    throw new CloudRuntimeException(String.format("ReplyMessagePreSendingExtensionPoint can only marshal APIEvent or MessageReply. %s claimed by %s is neither APIEvent nor MessageReply",
                            clz.getName(), extp.getClass().getName()));
                }

                List<MarshalReplyMessageExtensionPoint> exts = replyMessageMarshaller.get(clz);
                if (exts == null) {
                    exts = new ArrayList<MarshalReplyMessageExtensionPoint>();
                    replyMessageMarshaller.put(clz, exts);
                }
                exts.add(extp);
            }
        }
    }

    @Override
    public boolean start() {
        populateExtension();
        prepareStatistics();

        for (Service serv : services) {
            assert serv.getId() != null : String.format("service id can not be null[%s]", serv.getClass().getName());
            registerService(serv);
        }

        jmxf.registerBean("CloudBus", this);

        return true;
    }

    private void prepareStatistics() {
        List<Class> needReplyMsgs = new ArrayList<>(BeanUtils.reflections.getSubTypesOf(NeedReplyMessage.class));
        needReplyMsgs = transformAndRemoveNull(needReplyMsgs,
                arg -> !APIMessage.class.isAssignableFrom(arg) || APISyncCallMessage.class.isAssignableFrom(arg) ? arg : null);

        for (Class clz : needReplyMsgs) {
            MessageStatistic stat = new MessageStatistic();
            stat.setMessageClassName(clz.getName());
            statistics.put(stat.getMessageClassName(), stat);
        }
    }

    void destroy() {
        if (!stopped.compareAndSet(false, true)) {
            logger.debug(String.format("cloudbus has been stopped, ignore this call"));
            return;
        }

        for (final Service serv : services) {
            throwableSafe(new Runnable() {
                @Override
                public void run() {
                    unregisterService(serv);
                    logger.debug(String.format("unregistered service[%s]", serv.getId()));
                }
            });
        }

        tracker.destruct();

        throwableSafe(new Runnable() {
            @Override
            public void run() {
                try {
                    channelPool.destruct();
                } catch (IOException e) {
                    throw new CloudRuntimeException(e);
                }
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                maid.destruct();
            }
        }).throwableSafe(new Runnable() {
            @Override
            public void run() {
                noRouteEndPoint.destruct();
            }
        }).throwableSafe(new ExceptionDSL.RunnableWithThrowable() {
            @Override
            public void run() throws Throwable {
                conn.close();
            }
        });
    }

    @Override
    public boolean stop() {
        destroy();
        return true;
    }

    public List<String> getServerIps() {
        return serverIps;
    }

    public Map<String, MessageStatistic> getStatistics() {
        return statistics;
    }

    @Override
    public List<WaitingReplyMessageStatistic> getWaitingReplyMessageStatistic() {
        List<WaitingReplyMessageStatistic> ret = new ArrayList<WaitingReplyMessageStatistic>();

        long currentTime = System.currentTimeMillis();
        for (Envelope e : envelopes.values()) {
            for (Message msg : e.getRequests()) {
                WaitingReplyMessageStatistic statistic = new WaitingReplyMessageStatistic(
                        msg.getClass().getName(),
                        currentTime - msg.getCreatedTime(),
                        msg.getId(),
                        msg.getServiceId()
                );
                ret.add(statistic);
            }
        }
        return ret;
    }

    @Override
    public WaitingMessageSummaryStatistic getWaitingReplyMessageSummaryStatistic() {
        List<WaitingReplyMessageStatistic> ret = getWaitingReplyMessageStatistic();
        String mostWaitingMsgName = null;
        String longestWaitingMsgName = null;
        long most = 0;
        long longest = 0;
        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for (WaitingReplyMessageStatistic s : ret) {
            if (s.getWaitingTime() > longest) {
                longest = s.getWaitingTime();
                longestWaitingMsgName = s.getMessageName();
            }

            Integer count = countMap.get(s.getMessageName());
            count = count == null ? 1 : count ++;
            countMap.put(s.getMessageName(), count);
            if (count > most) {
                most = count;
                mostWaitingMsgName = s.getMessageName();
            }
        }

        return new WaitingMessageSummaryStatistic(
                ret.size(),
                countMap,
                mostWaitingMsgName,
                most,
                longestWaitingMsgName,
                longest
        );
    }

    private Map<String, Object> queueArguments() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("x-dead-letter-exchange", BusExchange.NO_ROUTE.toString());
        ret.put("x-dead-letter-routing-key", CloudBus.HEADER_DEAD_LETTER);
        ret.put("x-expires", TimeUnit.MINUTES.toMillis(5));
        return ret;
    }
}
