package org.zstack.core.cloudbus;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.jmx.JmxFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.message.*;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.management.MXBean;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MXBean
public class CloudBusJMX implements Component, BeforeSendMessageInterceptor,
        BeforeDeliveryMessageInterceptor, BeforePublishEventInterceptor, CloudBusMXBean {
    private Map<String, MessageStatistic> statistics = new HashMap<>();
    private static final CLogger logger = Utils.getLogger(CloudBusJMX.class);

    @Autowired
    private CloudBus bus;

    @Autowired
    private JmxFacade jmxf;

    class Bundle {
        Long startTime;
        MessageStatistic statistic;
        Message msg;
    }

    private Cache<String, Bundle> messageStartTime = CacheBuilder.newBuilder()
            .maximumSize(30000)
            .build();

    @Override
    public boolean start() {
        BeanUtils.reflections.getSubTypesOf(NeedReplyMessage.class).stream().filter(clz -> !Modifier.isAbstract(clz.getModifiers()))
                .forEach(clz -> {
                    MessageStatistic stat = new MessageStatistic();
                    stat.setMessageClassName(clz.getName());
                    statistics.put(clz.getName(), stat);
                });

        bus.installBeforeDeliveryMessageInterceptor(this);
        bus.installBeforePublishEventInterceptor(this);
        bus.installBeforeSendMessageInterceptor(this);


        CloudBusGlobalConfig.STATISTICS_ON.installUpdateExtension((oldConfig, newConfig) -> {
            if (!newConfig.value(Boolean.class)) {
                messageStartTime.invalidateAll();
            }
        });

        jmxf.registerBean("CloudBus", this);

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 0;
    }

    private void collectStats(Message msg) {
        if (!CloudBusGlobalConfig.STATISTICS_ON.value(Boolean.class)) {
            return;
        }

        String msgId;
        if (msg instanceof MessageReply) {
            msgId = msg.getHeaderEntry(CloudBus.HEADER_CORRELATION_ID);
        } else {
            msgId = ((APIEvent) msg).getApiId();
        }

        Bundle bundle = messageStartTime.getIfPresent(msgId);
        if (bundle == null) {
            logger.warn(String.format("cannot find bundle for message[id:%s]", msg.getId()));
            return;
        }

        long cost = System.currentTimeMillis() - bundle.startTime;
        bundle.statistic.count(cost);
        messageStartTime.invalidate(msgId);
    }

    @Override
    public void beforeDeliveryMessage(Message msg) {
        if (msg instanceof MessageReply) {
            collectStats(msg);
        }
    }

    @Override
    public int orderOfBeforePublishEventInterceptor() {
        return 0;
    }

    @Override
    public void beforePublishEvent(Event evt) {
        if (evt instanceof APIEvent) {
            collectStats(evt);
        }
    }

    @Override
    public int orderOfBeforeSendMessageInterceptor() {
        return 0;
    }

    @Override
    public void beforeSendMessage(Message msg) {
        if (!CloudBusGlobalConfig.STATISTICS_ON.value(Boolean.class)) {
            return;
        }

        if (msg.getServiceId().equals(ApiMediatorConstant.SERVICE_ID)) {
            // the API message will be routed by ApiMediator,
            // filter out this message to avoid reporting the same
            // API message twice
            return;
        }

        if (!(msg instanceof APIMessage)) {
            return;
        }

        Bundle bundle = new Bundle();
        bundle.startTime = System.currentTimeMillis();
        bundle.msg = msg;
        bundle.statistic = statistics.get(msg.getClass().getName());

        messageStartTime.put(msg.getId(), bundle);
    }

    public Map<String, MessageStatistic> getStatistics() {
        return statistics;
    }

    @Override
    public List<WaitingReplyMessageStatistic> getWaitingReplyMessageStatistic() {
        List<WaitingReplyMessageStatistic> ret = new ArrayList<WaitingReplyMessageStatistic>();
        long currentTime = System.currentTimeMillis();
        messageStartTime.asMap().values().forEach(bundle -> {
            Message msg = bundle.msg;
            WaitingReplyMessageStatistic statistic = new WaitingReplyMessageStatistic(
                    msg.getClass().getName(),
                    currentTime - msg.getCreatedTime(),
                    msg.getId(),
                    msg.getServiceId()
            );
            ret.add(statistic);
        });

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
            count = count == null ? 1 : ++ count;
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
}
