package org.zstack.sns.platform.email;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.sns.SNSApplicationEndpointBase;
import org.zstack.sns.SNSPublishError;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.zstack.core.Platform.operr;

public class SNSEmailEndpoint extends SNSApplicationEndpointBase {

    public SNSEmailEndpoint() {
    }

    public SNSEmailEndpoint(SNSEmailEndpointVO self) {
        super(self);
    }

    @Override
    protected SNSEmailEndpointVO getSelf() {
        return (SNSEmailEndpointVO) self;
    }

    @Override
    public SNSEmailEndpointInventory getInventory() {
        return SNSEmailEndpointInventory.valueOf(getSelf());
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddEmailAddressToSNSEmailEndpointMsg) {
            handle((APIAddEmailAddressToSNSEmailEndpointMsg) msg);
        } else if (msg instanceof APIDeleteEmailAddressOfSNSEmailEndpointMsg) {
            handle((APIDeleteEmailAddressOfSNSEmailEndpointMsg) msg);
        } else if (msg instanceof APIUpdateEmailAddressOfSNSEmailEndpointMsg) {
            handle((APIUpdateEmailAddressOfSNSEmailEndpointMsg) msg);
        } else if (msg instanceof APISNSEmailTestConnectionMsg) {
            handle((APISNSEmailTestConnectionMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Override
    protected void deleteHook() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(SNSEmailAddressVO.class).eq(SNSEmailAddressVO_.endpointUuid, self.getUuid()).hardDelete();
            }
        }.execute();
    }

    private void handle(APISNSEmailTestConnectionMsg msg) {
        SNSEmailPlatformVO platformVO;
        if (!StringUtils.isEmpty(msg.getPlatformUuid())) {
            platformVO = dbf.findByUuid(msg.getPlatformUuid(), SNSEmailPlatformVO.class);
        } else {
            SNSEmailEndpointVO endpointVO = dbf.findByUuid(msg.getEndpointUuid(), SNSEmailEndpointVO.class);
            platformVO = dbf.findByUuid(endpointVO.getPlatformUuid(), SNSEmailPlatformVO.class);
        }

        List<String> receipts = new ArrayList<>();
        if (!msg.getEmails().isEmpty()) {
            receipts.addAll(msg.getEmails());
        }
        if (!StringUtils.isEmpty(msg.getEndpointUuid())) {
            receipts.addAll(Q.New(SNSEmailAddressVO.class)
                    .select(SNSEmailAddressVO_.emailAddress)
                    .eq(SNSEmailAddressVO_.endpointUuid, msg.getEndpointUuid())
                    .listValues());
        }

        if (!receipts.isEmpty()) {
            JavaMailSenderImpl sender = Platform.New(JavaMailSenderImpl::new);
            sender.setHost(platformVO.getSmtpServer());
            sender.setUsername(platformVO.getUsername());
            sender.setPassword(platformVO.getPassword());
            sender.setPort(platformVO.getSmtpPort());
            sender.setJavaMailProperties(getMailProperties(platformVO.getPassword() != null, platformVO));

            try {
                MimeMessage mail = sender.createMimeMessage();
                MimeMessageHelper helper = Platform.New(() -> new MimeMessageHelper(mail));
                helper.setFrom(platformVO.getUsername());
                helper.setTo(receipts.toArray(new String[receipts.size()]));
                helper.setText(msg.getText());
                helper.setSubject(msg.getSubject());

                sender.send(mail);

                APISNSEmailTestConnectionEvent evt = new APISNSEmailTestConnectionEvent(msg.getId());
                evt.setConnected(true);
                bus.publish(evt);
            } catch (MessagingException e) {
                APISNSEmailTestConnectionEvent evt = new APISNSEmailTestConnectionEvent(msg.getId());
                evt.setConnected(false);
                bus.publish(evt);
                throw new CloudRuntimeException("send test email error");
            }
        }
    }

    private void handle(APIUpdateEmailAddressOfSNSEmailEndpointMsg msg) {
        SNSEmailAddressVO vo = dbf.findByUuid(msg.getEmailAddressUuid(), SNSEmailAddressVO.class);

        vo.setEmailAddress(msg.getEmailAddress());

        vo = dbf.updateAndRefresh(vo);

        APIUpdateEmailAddressOfSNSEmailEndpointEvent evt = new APIUpdateEmailAddressOfSNSEmailEndpointEvent(msg.getId());
        evt.setInventory(SNSEmailAddressInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIDeleteEmailAddressOfSNSEmailEndpointMsg msg) {
        SQL.New(SNSEmailAddressVO.class).eq(SNSEmailAddressVO_.endpointUuid, msg.getEndpointUuid())
                .eq(SNSEmailAddressVO_.uuid, msg.getEmailAddressUuid()).hardDelete();

        APIDeleteEmailAddressOfSNSEmailEndpointEvent evt = new APIDeleteEmailAddressOfSNSEmailEndpointEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAddEmailAddressToSNSEmailEndpointMsg msg) {
        SNSEmailAddressVO vo = new SQLBatchWithReturn<SNSEmailAddressVO>() {
            @Override
            protected SNSEmailAddressVO scripts() {
                SNSEmailAddressVO svo = new SNSEmailAddressVO();
                svo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
                svo.setEmailAddress(msg.getEmailAddress());
                svo.setEndpointUuid(msg.getEndpointUuid());
                persist(svo);

                return reload(svo);
            }
        }.execute();

        APIAddEmailAddressToSNSEmailEndpointEvent evt = new APIAddEmailAddressToSNSEmailEndpointEvent(msg.getId());
        evt.setInventory(SNSEmailAddressInventory.valueOf(vo));
        bus.publish(evt);
    }

    private Properties getMailProperties(boolean auth, SNSEmailPlatformVO platformVO) {
        String encryptType = SNSApplicationPlatformSystemTags.SNS_ENCRYPTTYPE.getTokenByResourceUuid(platformVO.getUuid(), SNSApplicationPlatformSystemTags.SNS_ENCRYPTTYPE_TOKEN);
        encryptType = StringUtils.isEmpty(encryptType) ? EmailServerEncryptType.STARTTLS.toString() : encryptType;

        Properties props = new Properties();
        if (EmailServerEncryptType.STARTTLS.toString().equals(encryptType)) {
            props.put("mail.smtp.starttls.enable", true);
        } else if (EmailServerEncryptType.SSL.toString().equals(encryptType)) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", String.valueOf(platformVO.getSmtpPort()));
        }
        props.put("mail.smtp.auth", auth);
        props.put("mail.debug", "true");
        props.put("mail.smtp.host", platformVO.getSmtpServer());
        props.put("mail.smtp.port", String.valueOf(platformVO.getSmtpPort()));
        return props;
    }
}
