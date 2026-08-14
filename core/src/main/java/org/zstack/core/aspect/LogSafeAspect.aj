package org.zstack.core.aspect;

import org.zstack.core.log.LogSafeGson;
import org.zstack.header.message.Event;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.Utils;

import java.util.Collections;
import java.util.Map;

/**
 * Created by MaJin on 2019/9/23.
 */
public aspect LogSafeAspect {
    pointcut send() : execution(void org.zstack.core.cloudbus.CloudBus.send(Message));
    pointcut sendCallBack() : execution(void org.zstack.core.cloudbus.CloudBus.send(NeedReplyMessage, CloudBusCallBack));
    pointcut reply(MessageReply obj) : args(*, obj) && execution(void org.zstack.core.cloudbus.CloudBus.reply(Message, MessageReply));
    pointcut publish() : execution(void org.zstack.core.cloudbus.CloudBus.publish(Event));
    pointcut handle() : execution(void *.handle(Message+));

    void around(org.zstack.core.thread.ChainTask obj) : target(obj) && execution(void org.zstack.core.thread.ChainTask+.run(..)) {
        if (obj.maskWords.isEmpty()) {
            proceed(obj);
            return;
        }

        try (Utils.MaskWords h = new Utils.MaskWords(obj.maskWords)) {
            proceed(obj);
        }
    }

    Void around(org.zstack.core.thread.Task obj) : target(obj) && execution(Void org.zstack.core.thread.Task+.call()) {
        if (obj.maskWords.isEmpty()) {
            return proceed(obj);
        }

        try (Utils.MaskWords h = new Utils.MaskWords(obj.maskWords)) {
            return proceed(obj);
        }
    }

    Object around(org.zstack.core.thread.Task obj) : target(obj) && execution(Object org.zstack.core.thread.Task+.call()) {
        if (obj.maskWords.isEmpty()) {
            return proceed(obj);
        }

        try (Utils.MaskWords h = new Utils.MaskWords(obj.maskWords)) {
            return proceed(obj);
        }
    }

    void around() : send() || sendCallBack() || publish() || handle() {
        Object obj = thisJoinPoint.getArgs()[0];
        Map<String, String> maskWords = getValuesToMask(obj);
        if (maskWords.isEmpty()) {
            proceed();
            return;
        }

        boolean extend = !thisJoinPoint.getSignature().getName().equals("handle");
        try (Utils.MaskWords h = new Utils.MaskWords(maskWords, extend)) {
            proceed();
        }
    }

    void around(org.zstack.header.message.MessageReply obj) : reply(obj) {
        Map<String, String> maskWords = getValuesToMask(obj);
        if (maskWords.isEmpty()) {
            proceed(obj);
            return;
        }

        try (Utils.MaskWords h = new Utils.MaskWords(maskWords, true)) {
            proceed(obj);
        }
    }

    void around() : execution(void org.zstack.header.rest.RESTFacade+.asyncJsonPost(String, Object, java.util.Map, org.zstack.header.rest.AsyncRESTCallback, java.util.concurrent.TimeUnit, long)) {
        Object cmd = thisJoinPoint.getArgs()[1];
        Map<String, String> maskWords = getValuesToMask(cmd);
        if (maskWords.isEmpty()) {
            proceed();
            return;
        }

        try (Utils.MaskWords h = new Utils.MaskWords(maskWords, true)) {
            proceed();
        }
    }

    MessageReply around(org.zstack.header.message.Message obj) : args(obj, ..) && execution(MessageReply org.zstack.core.cloudbus.CloudBus.call(Message+, ..)) {
        Map<String, String> maskWords = getValuesToMask(obj);
        if (maskWords.isEmpty()) {
            return proceed(obj);
        }

        try (Utils.MaskWords h = new Utils.MaskWords(maskWords, true)) {
            return proceed(obj);
        }
    }

    private Map<String, String> getValuesToMask(Object obj) {
        try {
            return LogSafeGson.getValuesToMask(obj);
        } catch (Throwable t) {
            return Collections.emptyMap();
        }
    }
}
