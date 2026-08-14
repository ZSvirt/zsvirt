package org.zstack.zwatch.alarm.sns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.thread.SyncThread;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.sns.SNSConstants;
import org.zstack.sns.SNSPublishMsg;
import org.zstack.sns.SNSTopicVO;
import org.zstack.sns.SNSTopicVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.alarm.AlarmAction;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SNSAction implements AlarmAction {
    private static final CLogger logger = Utils.getLogger(SNSAction.class);
    private SNSTopicVO topicVO;

    @Autowired
    private SNSActionFactory factory;
    @Autowired
    private CloudBus bus;

    public SNSAction(SNSTopicVO topicVO) {
        this.topicVO = topicVO;
    }

    public static final String METADATA_EVENT_SUBSCRIPTION_UUID = "EventSubscriptionUUID";

    @Override
    @SyncThread(signature="zwatch-sns-action")
    public void takeAction(TakeAlarmActionParam param) {
        SNSPublishMsg msg = new SNSPublishMsg();
        Map<String, String> messages = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();

        String locale = Q.New(SNSTopicVO.class).select(SNSTopicVO_.locale).eq(SNSTopicVO_.uuid, topicVO.getUuid()).findValue();

        CollectionUtils.safeForEach(factory.getAllTextTemplateFactory(), new ForEachFunction<TextTemplateFactory>() {
            @Override
            public void run(TextTemplateFactory f) {
                SNSTextTemplateInventory inv = null;

                if (f.isSupportCustomTemplate()) {
                    SNSTextTemplateVO vo = Q.New(SNSTextTemplateVO.class)
                            .eq(SNSTextTemplateVO_.applicationPlatformType, f.getApplicationPlatformType())
                            .eq(SNSTextTemplateVO_.defaultTemplate, true)
                            .eq(SNSTextTemplateVO_.type, SNSTextTemplateType.ALARM)
                            .find();

                    if (vo != null) {
                        inv = SNSTextTemplateInventory.valueOf(vo);
                    }
                }

                TextTemplate template = f.createAlarmTemplate(inv, param);
                SNSTopicMessage s = template.createMessage(inv, param, locale);
                messages.put(f.getApplicationPlatformType(), s.getMessage());
                if (s.getMetadata() != null) {
                    metadata.put(f.getApplicationPlatformType(), s.getMetadata());
                }
            }
        });

        msg.setTopicUuid(topicVO.getUuid());
        msg.setMessage(messages);
        msg.setMetadata(metadata);
        bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, topicVO.getUuid());

        FutureCompletion sendConfirm = bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("fail to publish to a topic[uuid:%s], %s", topicVO.getUuid(), reply.getError()));
                }
            }
        });

        sendConfirm.await(TimeUnit.SECONDS.toMillis(30));
    }

    @Override
    @SyncThread(signature="zwatch-sns-action")
    public void takeAction(TakeEventSubscriptionActionParam param) {
        SNSPublishMsg msg = new SNSPublishMsg();
        Map<String, Object> metadata = new HashMap<>();
        Map<String, String> messages = new HashMap<>();

        String locale = Q.New(SNSTopicVO.class).select(SNSTopicVO_.locale).eq(SNSTopicVO_.uuid, topicVO.getUuid()).findValue();

        factory.getAllTextTemplateFactory().forEach(f -> {
            SNSTextTemplateInventory inv = null;

            if (f.isSupportCustomTemplate()) {
                SNSTextTemplateVO vo = Q.New(SNSTextTemplateVO.class)
                        .eq(SNSTextTemplateVO_.applicationPlatformType, f.getApplicationPlatformType())
                        .eq(SNSTextTemplateVO_.defaultTemplate, true)
                        .eq(SNSTextTemplateVO_.type, SNSTextTemplateType.EVENT)
                        .find();
                if (vo != null) {
                    inv = SNSTextTemplateInventory.valueOf(vo);
                }
            }

            TextTemplate template = f.createEventTemplate(inv, param);
            SNSTopicMessage s = template.createMessage(inv, param, locale);
            messages.put(f.getApplicationPlatformType(), s.getMessage());
            if (s.getMetadata() != null) {
                metadata.put(f.getApplicationPlatformType(), s.getMetadata());
            }
        });

        msg.setMetadata(metadata);
        msg.setTopicUuid(topicVO.getUuid());
        msg.setMessage(messages);
        bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, topicVO.getUuid());
        FutureCompletion sendConfirm = bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("fail to publish to a topic[uuid:%s], %s", topicVO.getUuid(), reply.getError()));
                }
            }
        });

        sendConfirm.await(TimeUnit.SECONDS.toMillis(30));
    }

    @Override
    @SyncThread(signature="zwatch-sns-action")
    public void takeActionForThirdpartyAlert(TakeEventSubscriptionActionParam param) {
        SNSPublishMsg msg = new SNSPublishMsg();
        Map<String, Object> metadata = new HashMap<>();
        Map<String, String> messages = new HashMap<>();

        String locale = Q.New(SNSTopicVO.class).select(SNSTopicVO_.locale).eq(SNSTopicVO_.uuid, topicVO.getUuid()).findValue();

        factory.getAllTextTemplateFactory().forEach(f -> {
            SNSTextTemplateInventory inv = null;


            TextTemplate template = f.createEventTemplateForThirdpartyAlert(inv, param);
            SNSTopicMessage s = template.createMessage(inv, param, locale);
            messages.put(f.getApplicationPlatformType(), s.getMessage());
            if (s.getMetadata() != null) {
                metadata.put(f.getApplicationPlatformType(), s.getMetadata());
            }
        });

        msg.setMetadata(metadata);
        msg.setTopicUuid(topicVO.getUuid());
        msg.setMessage(messages);
        bus.makeTargetServiceIdByResourceUuid(msg, SNSConstants.SERVICE_ID, topicVO.getUuid());
        FutureCompletion sendConfirm = bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("fail to publish to a topic[uuid:%s], %s", topicVO.getUuid(), reply.getError()));
                }
            }
        });

        sendConfirm.await(TimeUnit.SECONDS.toMillis(30));
    }
}
