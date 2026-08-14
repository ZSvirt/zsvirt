package org.zstack.zwatch.alarm.sns.template.aliyunsms;

import org.zstack.core.db.SQLBatch;
import org.zstack.header.message.APIMessage;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateBase;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateVO;
import org.zstack.zwatch.alarm.sns.SNSTextTemplateVO_;

/**
 * Created by Qi Le on 2019-07-15
 */
public class AliyunSmsSNSTextTemplate extends SNSTextTemplateBase {

    public AliyunSmsSNSTextTemplate(SNSTextTemplateVO self) {
        super(self);
    }

    protected AliyunSmsSNSTextTemplateVO getSelf() {
        return (AliyunSmsSNSTextTemplateVO) self;
    }

    protected AliyunSmsSNSTextTemplateInventory getInventory() {
        return AliyunSmsSNSTextTemplateInventory.valueOf(getSelf());
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateAliyunSmsSNSTextTemplateMsg) {
            handle((APIUpdateAliyunSmsSNSTextTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateAliyunSmsSNSTextTemplateMsg msg) {
        APIUpdateAliyunSmsSNSTextTemplateEvent event = new APIUpdateAliyunSmsSNSTextTemplateEvent(msg.getId());

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (msg.getName() != null) {
                    getSelf().setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    getSelf().setDescription(msg.getDescription());
                }

                if (msg.getSubject() != null) {
                    getSelf().setSubject(msg.getSubject());
                }

                if (msg.getRecoverySubject() != null) {
                    getSelf().setRecoverySubject(msg.getRecoverySubject());
                }

                if (msg.getTemplate() != null) {
                    getSelf().setTemplate(msg.getTemplate());
                }

                if (msg.getRecoveryTemplate() != null) {
                    getSelf().setRecoveryTemplate(msg.getRecoveryTemplate());
                }

                if (msg.getDefaultTemplate() != null) {
                    if (msg.getDefaultTemplate()) {
                        sql(SNSTextTemplateVO.class)
                                .set(SNSTextTemplateVO_.defaultTemplate, false)
                                .eq(SNSTextTemplateVO_.applicationPlatformType, self.getApplicationPlatformType())
                                .eq(SNSTextTemplateVO_.type, self.getType())
                                .update();
                    }

                    getSelf().setDefaultTemplate(msg.getDefaultTemplate());
                }

                if (msg.getAlarmTemplateCode() != null) {
                    getSelf().setAlarmTemplateCode(msg.getAlarmTemplateCode());
                }

                if (msg.getEventTemplate() != null) {
                    getSelf().setEventTemplate(msg.getEventTemplate());
                }

                if (msg.getEventTemplateCode() != null) {
                    getSelf().setEventTemplateCode(msg.getEventTemplateCode());
                }

                if (msg.getSign() != null) {
                    getSelf().setSign(msg.getSign());
                }

                reload(merge(getSelf()));
            }
        }.execute();

        event.setInventory(getInventory());
        bus.publish(event);
    }
}
