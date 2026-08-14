package org.zstack.core.cloudbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.webhook.WebhookCaller;
import org.zstack.header.core.webhooks.WebhookVO_;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.webhooks.APICreateWebhookMsg;
import org.zstack.header.core.webhooks.WebhookInventory;
import org.zstack.header.core.webhooks.WebhookVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Event;
import org.zstack.utils.TaskContext;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.zstack.core.Platform.argerr;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventFacadeImpl implements EventFacade, CloudBusEventListener, Component, GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    private final Map<String, CallbackWrapper> global = new ConcurrentHashMap<>();
    private final Map<String, CallbackWrapper> local =  new ConcurrentHashMap<>();

    private EventSubscriberReceipt unsubscriber;

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(APICreateWebhookMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateWebhookMsg) {
            validate((APICreateWebhookMsg) msg);
        }

        return msg;
    }

    private void validate(APICreateWebhookMsg msg) {
        if (!EventFacade.WEBHOOK_TYPE.equals(msg.getType())) {
            return;
        }

        if (msg.getOpaque() == null) {
            throw new ApiMessageInterceptionException(argerr("for webhooks with type[%s], the field opaque cannot be null", EventFacade.WEBHOOK_TYPE));
        }
    }

    private class CallbackWrapper {
        String path;
        String glob;
        AbstractEventFacadeCallback callback;
        AtomicBoolean hasRun;

        CallbackWrapper(String path, AbstractEventFacadeCallback callback) {
            this.path = path;
            this.glob = createRegexFromGlob(path.replaceAll("\\{.*\\}", ".*"));
            this.callback = callback;
            if (callback instanceof AutoOffEventCallback) {
                hasRun = new AtomicBoolean(false);
            }
        }

        Object getIdentity() {
            return callback;
        }

        public String getGlob() {
            return glob;
        }

        @AsyncThread
        void call(CanonicalEvent e) {
            TaskContext.removeTaskContext();

            if (callback instanceof EventRunnable) {
                ((EventRunnable) callback).run();
            } else {
                Map<String, String> tokens = tokenize(e.getPath(), path);
                tokens.put(EventFacade.META_DATA_MANAGEMENT_NODE_ID, e.getManagementNodeId());
                tokens.put(EventFacade.META_DATA_PATH, e.getPath());
                Object data = null;
                if (e.getContent() != null) {
                    data = e.getContent();
                }

                if (callback instanceof EventCallback) {
                    ((EventCallback)callback).run(tokens, data);
                } else if (callback instanceof AutoOffEventCallback) {

                    if (!hasRun.compareAndSet(false, true)) {
                        // the callback is being called
                        return;
                    }

                    if (((AutoOffEventCallback)callback).run(tokens, data)) {
                        off(callback);
                    } else {
                        hasRun.set(false);
                    }
                }
            }
        }
    }

    public String createRegexFromGlob(String glob) {
        String out = "^";
        for(int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
                case '*': out += ".*"; break;
                case '?': out += '.'; break;
                case '\\': out += "\\\\"; break;
                default: out += c;
            }
        }
        out += '$';
        return out;
    }

    private Map<String, String> tokenize(String str1, String str2) {
        StringTokenizer token = new StringTokenizer(str1, "/");
        List<String> origins =  new ArrayList<String>();
        while (token.hasMoreElements()) {
            origins.add(token.nextToken());
        }

        token = new StringTokenizer(str2, "/");
        List<String> t = new ArrayList<String>();
        while (token.hasMoreElements()) {
            t.add(token.nextToken());
        }

        Map ret = new HashMap();
        for (int i=0;i<t.size(); i++) {
            String key = t.get(i);
            if (!key.startsWith("{") || !key.endsWith("}")) {
                continue;
            }

            key = key.replaceAll("\\{", "").replaceAll("\\}", "");
            ret.put(key, origins.get(i));
        }

        return ret;
    }

    @Override
    public void on(String path, AutoOffEventCallback cb) {
        global.put(cb.uniqueIdentity, new CallbackWrapper(path, cb));
    }

    @Override
    public void on(String path, final EventCallback cb) {
        global.put(cb.uniqueIdentity, new CallbackWrapper(path, cb));
    }

    @Override
    public void on(String path, EventRunnable cb) {
        global.put(cb.uniqueIdentity, new CallbackWrapper(path, cb));
    }

    @Override
    public void off(AbstractEventFacadeCallback cb) {
        global.remove(cb.uniqueIdentity);
        local.remove(cb.uniqueIdentity);
    }

    @Override
    public void onLocal(String path, AutoOffEventCallback cb) {
        local.put(cb.uniqueIdentity, new CallbackWrapper(path, cb));
    }

    @Override
    public void onLocal(String path, EventCallback cb) {
        local.put(cb.uniqueIdentity, new CallbackWrapper(path, cb));
    }

    @Override
    public void onLocal(String path, EventRunnable cb) {
        local.put(cb.uniqueIdentity, new CallbackWrapper(path, cb));
    }

    @Override
    public void fire(String path, Object data) {
        assert path != null;
        CanonicalEvent evt = new CanonicalEvent();
        evt.setPath(path);
        evt.setManagementNodeId(Platform.getManagementServerId());
        if (data != null) {
            /*
            if (!TypeUtils.isPrimitiveOrWrapper(data.getClass()) && !data.getClass().isAnnotationPresent(NeedJsonSchema.class)) {
                throw new CloudRuntimeException(String.format("data[%s] passed to canonical event is not annotated by @NeedJsonSchema", data.getClass().getName()));
            }
            */

            evt.setContent(data);
        }
        
        fireLocal(evt);

        callWebhooks(evt);
        
        bus.publish(evt);
    }

    private void callWebhooks(CanonicalEvent event) {
        new WebhookCaller() {
            @Override
            public void call() {
                List<WebhookVO> vos = Q.New(WebhookVO.class).eq(WebhookVO_.type, EventFacade.WEBHOOK_TYPE).list();
                vos = vos.stream().filter(
                        vo -> event.getPath().matches(
                                createRegexFromGlob(vo.getOpaque().replaceAll("\\{.*\\}", ".*"))
                        )).collect(Collectors.toList());

                if (!vos.isEmpty()) {
                    postToWebhooks(WebhookInventory.valueOf(vos), JSONObjectUtil.toJsonString(event));
                }
            }
        }.call();
    }

    private void fireLocal(CanonicalEvent cevt) {
        Map<String, CallbackWrapper> wrappers = new HashMap<>();
        wrappers.putAll(local);

        for (CallbackWrapper w : wrappers.values()) {
            if (cevt.getPath().matches(w.getGlob())) {
                w.call(cevt);
            }
        }
    }

    @Override
    public boolean isFromThisManagementNode(Map tokens) {
        return Platform.getManagementServerId().equals(tokens.get(EventFacade.META_DATA_MANAGEMENT_NODE_ID));
    }

    @Override
    public boolean handleEvent(Event evt) {
        if (!(evt instanceof CanonicalEvent)) {
            return false;
        }

        CanonicalEvent cevt = (CanonicalEvent)evt;
        Map<String, CallbackWrapper> wrappers = new HashMap<>();
        wrappers.putAll(global);
        for (CallbackWrapper w : wrappers.values()) {
            if (cevt.getPath().matches(w.getGlob())) {
                w.call(cevt);
            }
        }

        return false;
    }

    @Override
    public boolean start() {
        unsubscriber =  bus.subscribeEvent(this, new CanonicalEvent());
        return true;
    }

    @Override
    public boolean stop() {
        if (unsubscriber != null) {
            unsubscriber.unsubscribeAll();
        }
        return true;
    }
}
