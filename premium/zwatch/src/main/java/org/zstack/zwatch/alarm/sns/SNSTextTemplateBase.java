package org.zstack.zwatch.alarm.sns;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.access.method.P;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.zwatch.message.SNSTextTemplateDeletionMsg;
import org.zstack.zwatch.message.SNSTextTemplateDeletionReply;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SNSTextTemplateBase implements SNSTextTemplate {
    protected SNSTextTemplateVO self;
    
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;

    protected SNSTextTemplateInventory getInventory() {
        return SNSTextTemplateInventory.valueOf(self);
    }

    public SNSTextTemplateBase(SNSTextTemplateVO self) {
        this.self = self;
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

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SNSTextTemplateDeletionMsg) {
            handle((SNSTextTemplateDeletionMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void delete() {
        dbf.remove(self);
    }

    private void handle(SNSTextTemplateDeletionMsg msg) {
        delete();
        bus.reply(msg, new SNSTextTemplateDeletionReply());
    }

    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIUpdateSNSTextTemplateMsg) {
            handle((APIUpdateSNSTextTemplateMsg) msg);
        } else if (msg instanceof APIDeleteSNSTextTemplateMsg) {
            handle((APIDeleteSNSTextTemplateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDeleteSNSTextTemplateMsg msg) {
        delete();
        APIDeleteSNSTextTemplateEvent evt = new APIDeleteSNSTextTemplateEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIUpdateSNSTextTemplateMsg msg) {
        APIUpdateSNSTextTemplateEvent evt = new APIUpdateSNSTextTemplateEvent(msg.getId());

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (msg.getName() != null) {
                    self.setName(msg.getName());
                }

                if (msg.getDescription() != null) {
                    self.setDescription(msg.getDescription());
                }

                if (msg.getSubject() != null) {
                    self.setSubject(msg.getSubject());
                }

                if (msg.getRecoverySubject() != null) {
                    self.setRecoverySubject(msg.getRecoverySubject());
                }

                if (msg.getTemplate() != null) {
                    self.setTemplate(msg.getTemplate());
                }

                if (msg.getRecoveryTemplate() != null) {
                    self.setRecoveryTemplate(msg.getRecoveryTemplate());
                }

                if (msg.getDefaultTemplate() != null) {
                    if (msg.getDefaultTemplate()) {
                        sql(SNSTextTemplateVO.class)
                                .set(SNSTextTemplateVO_.defaultTemplate, false)
                                .eq(SNSTextTemplateVO_.applicationPlatformType, self.getApplicationPlatformType())
                                .eq(SNSTextTemplateVO_.type, self.getType())
                                .update();
                    }

                    self.setDefaultTemplate(msg.getDefaultTemplate());
                }

                reload(merge(self));
            }
        }.execute();

        evt.setInventory(getInventory());
        bus.publish(evt);
    }
}
