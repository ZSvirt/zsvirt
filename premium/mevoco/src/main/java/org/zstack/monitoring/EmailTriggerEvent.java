package org.zstack.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.monitoring.actions.EmailTriggerActionInventory;
import org.zstack.monitoring.actions.MonitorTriggerActionConstants;
import org.zstack.monitoring.actions.MonitorTriggerActionInventory;
import org.zstack.monitoring.media.EmailMediaVO;
import org.zstack.monitoring.media.MediaConstants;
import org.zstack.monitoring.media.MediaState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import static org.zstack.core.Platform.*;

import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;
import java.util.Properties;

/**
 * Created by xing5 on 2017/6/12.
 */
public class EmailTriggerEvent extends TextBasedTriggerEvent {
    private static final CLogger logger = Utils.getLogger(EmailTriggerEvent.class);

    private static final String EMAIL_SUBJECT_PREFIX = "ZStack-Monitor";

    @Autowired
    private DatabaseFacade dbf;

    @TriggerEventFactoryMethod(MonitorTriggerActionConstants.EMAIL_MONITOR_TRIGGER_ACTION_TYPE)
    public EmailTriggerEvent(MonitorTriggerInventory trigger, MonitorTriggerActionInventory action, MonitorTriggerContext context) {
        super(trigger, action, context);
    }

    private void sendEmail(String subject, String content) {
        EmailTriggerActionInventory einv = (EmailTriggerActionInventory) action;
        EmailMediaVO vo = dbf.findByUuid(einv.getMediaUuid(), EmailMediaVO.class);

        if(vo == null){
            logger.warn(String.format("Cannot send mail because the current email monitor trigger action[uuid:%s,name:%s] does not have a email media server", einv.getUuid(), einv.getName()));
            return;
        }

        if (vo.getState() == MediaState.Disabled) {
            logger.debug(String.format("email server[uuid:%s, server:%s] is disabled, no email will be sent for the action[uuid:%s, name:%s]",
                   vo.getUuid(), vo.getSmtpServer(), einv.getUuid(), einv.getName()));
            return;
        }

        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(vo.getSmtpServer());
            sender.setUsername(vo.getUsername());
            sender.setPassword(vo.getPassword());
            sender.setPort(vo.getSmtpPort());

            Properties javaMailProperties = new Properties();
            javaMailProperties.put(MediaConstants.Email.MAIL_SMTP_AUTH, vo.getPassword() != null);
            javaMailProperties.put(MediaConstants.Email.MAIL_SMTP_STARTTLS, true);
            javaMailProperties.put("mail.smtp.socketFactory.class", SSLSocketFactory.class.getName());
            sender.setJavaMailProperties(javaMailProperties);

            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(vo.getUsername());
            helper.setTo(einv.getEmail());
            helper.setText(content);
            helper.setSubject(subject);

            sender.send(message);

        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void sendEmail(String content){
        this.sendEmail(EMAIL_SUBJECT_PREFIX, content);
    }

    @Override
    public void triggerOK(Completion completion) {
        sendEmail(
                String.format("%s: A %s trigger recovered", EMAIL_SUBJECT_PREFIX, i18n(getItem().getReadableName())),
                createAlertTextWriter().writeOkAlertText(context)
        );
        completion.success();
    }

    @Override
    public void triggerProblem(Completion completion) {
        sendEmail(
                String.format("%s: A %s trigger fired", EMAIL_SUBJECT_PREFIX, i18n(getItem().getReadableName())),
                createAlertTextWriter().writeProblemAlertText(context)
        );
        completion.success();
    }
}
