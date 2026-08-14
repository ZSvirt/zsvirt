package org.zstack.zwatch.alarm.sns;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.sns.*;
import org.zstack.zwatch.alarm.*;
import org.zstack.zwatch.alarm.sns.template.aliyunsms.*;
import org.zstack.zwatch.alarm.sns.template.dingtalk.DingTalkTextTemplate;
import org.zstack.zwatch.alarm.sns.template.email.EmailTextTemplate;

import static org.zstack.core.Platform.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SNSActionFactory extends AbstractService implements AlarmActionFactory, AfterDeleteSNSTopicExtensionPoint {
    public static AlarmActionType type = new AlarmActionType("sns");
    public static final String SERVICE_ID = "alarm.sns";

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    private Map<String, TextTemplateFactory> alarmTextTemplateFactories = new HashMap<>();


    @Override
    public String getActionType() {
        return type.toString();
    }

    @Override
    public AlarmAction createAlarmAction(String actionUuid) {
        SNSTopicVO vo = dbf.findByUuid(actionUuid, SNSTopicVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the topic[uuid:%s]", actionUuid));
        }

        return new SNSAction(vo);
    }

    @Override
    public TextTemplateFactory getTextTemplateFactory(String type) {
        TextTemplateFactory f = alarmTextTemplateFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find TextTemplateFactory with type[%s]", type));
        }

        return f;
    }

    @Override
    public Collection<TextTemplateFactory> getAllTextTemplateFactory() {
        return alarmTextTemplateFactories.values();
    }

    @Override
    public boolean isActionExists(String actionUuid) {
        return dbf.isExist(actionUuid, SNSTopicVO.class);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof SNSTextTemplateMessage) {
            passThrough((SNSTextTemplateMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(SNSTextTemplateMessage msg) {
        SNSTextTemplateVO vo = dbf.findByUuid(msg.getAlarmTextTemplateUuid(), SNSTextTemplateVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find SNSTextTemplate[uuid:%s], it may have been deleted", msg.getAlarmTextTemplateUuid()));
        }

        if (msg instanceof AliyunSmsSNSTextTemplateMessage) {
            new AliyunSmsSNSTextTemplate(vo).handleMessage((Message) msg);
        } else {
            new SNSTextTemplateBase(vo).handleMessage((Message) msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateAliyunSmsSNSTextTemplateMsg) {
            handle((APICreateAliyunSmsSNSTextTemplateMsg) msg);
        } else if (msg instanceof APICreateSNSTextTemplateMsg) {
            handle((APICreateSNSTextTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateAliyunSmsSNSTextTemplateMsg msg) {
        AliyunSmsSNSTextTemplateVO vo = new AliyunSmsSNSTextTemplateVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setApplicationPlatformType(AliyunSmsConstants.ALIYUN_SMS_SNS_ENDPOINT_TYPE);
        vo.setSubject(msg.getSubject());
        vo.setTemplate(msg.getTemplate());
        vo.setRecoverySubject(msg.getRecoverySubject());
        if (StringUtils.isNotEmpty(msg.getRecoveryTemplate())) {
            vo.setRecoveryTemplate(msg.getRecoveryTemplate());
        } else {
            vo.setRecoveryTemplate(getDefaultRecoveryTemplate(msg.getApplicationPlatformType()));
        }
        vo.setDefaultTemplate(msg.getDefaultTemplate() != null && msg.getDefaultTemplate());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setSign(msg.getSign());
        vo.setAlarmTemplateCode(msg.getAlarmTemplateCode());
        vo.setEventTemplate(msg.getEventTemplate());
        vo.setEventTemplateCode(msg.getEventTemplateCode());
        vo.setType(SNSTextTemplateType.get(msg.getType()));

        new SQLBatch() {

            @Override
            protected void scripts() {
                if (vo.isDefaultTemplate()) {
                    sql(AliyunSmsSNSTextTemplateVO.class).set(AliyunSmsSNSTextTemplateVO_.defaultTemplate, false)
                            .eq(AliyunSmsSNSTextTemplateVO_.applicationPlatformType, msg.getApplicationPlatformType())
                            .update();
                }

                persist(vo);
                reload(vo);
            }
        }.execute();

        APICreateSNSTextTemplateEvent event = new APICreateSNSTextTemplateEvent(msg.getId());
        event.setInventory(AliyunSmsSNSTextTemplateInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APICreateSNSTextTemplateMsg msg) {
        SNSTextTemplateVO vo = new SNSTextTemplateVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setApplicationPlatformType(msg.getApplicationPlatformType());
        vo.setSubject(msg.getSubject());
        vo.setRecoverySubject(msg.getRecoverySubject());
        vo.setTemplate(msg.getTemplate());
        if (StringUtils.isNotEmpty(msg.getRecoveryTemplate())) {
            vo.setRecoveryTemplate(msg.getRecoveryTemplate());
        } else {
            vo.setRecoveryTemplate(getDefaultRecoveryTemplate(msg.getApplicationPlatformType()));
        }
        vo.setDefaultTemplate(msg.getDefaultTemplate() != null && msg.getDefaultTemplate());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setType(SNSTextTemplateType.get(msg.getType()));

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (vo.isDefaultTemplate()) {
                    sql(SNSTextTemplateVO.class)
                            .set(SNSTextTemplateVO_.defaultTemplate, false)
                            .eq(SNSTextTemplateVO_.applicationPlatformType, msg.getApplicationPlatformType())
                            .eq(SNSTextTemplateVO_.type, vo.getType())
                            .update();
                }

                persist(vo);
                reload(vo);
            }
        }.execute();

        APICreateSNSTextTemplateEvent evt = new APICreateSNSTextTemplateEvent(msg.getId());
        evt.setInventory(SNSTextTemplateInventory.valueOf(vo));
        bus.publish(evt);
    }

    private String getDefaultRecoveryTemplate(String applicationPlatformType) {
        if (applicationPlatformType.equals(SNSConstants.DINGTALK_PLATFORM)) {
            return Platform.getLocale().equals(Locale.SIMPLIFIED_CHINESE) ? DingTalkTextTemplate.getRECOVERY_CHINESE_TEMPLATE()
                    : DingTalkTextTemplate.getRECOVERY_ENGLISH_TEMPLATE();
        }
        if (applicationPlatformType.equals(SNSConstants.EMAIL_PLATFORM)) {
            return Platform.getLocale().equals(Locale.SIMPLIFIED_CHINESE) ? EmailTextTemplate.getRECOVERY_CHINESE_TEMPLATE()
                    : EmailTextTemplate.getRECOVERY_ENGLISH_TEMPLATE();
        }
        return null;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        pluginRgty.getExtensionList(TextTemplateFactory.class).forEach(f -> {
            TextTemplateFactory old =  alarmTextTemplateFactories.get(f.getApplicationPlatformType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate TextTemplateFactory[%s,%s] with the same type[%s]", f.getClass(), old.getClass(), f.getApplicationPlatformType()));
            }

            alarmTextTemplateFactories.put(f.getApplicationPlatformType(), f);
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void afterDeleteSNSTopic(SNSTopicInventory topic) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(AlarmActionVO.class).eq(AlarmActionVO_.actionType, type.toString())
                        .eq(AlarmActionVO_.actionUuid, topic.getUuid()).hardDelete();
                sql(EventSubscriptionActionVO.class).eq(EventSubscriptionActionVO_.actionType, type.toString())
                        .eq(EventSubscriptionActionVO_.actionUuid, topic.getUuid()).hardDelete();
            }
        }.execute();
    }
}
