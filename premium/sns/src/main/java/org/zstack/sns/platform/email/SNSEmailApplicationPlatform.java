package org.zstack.sns.platform.email;

import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.sns.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18n;
import static org.zstack.core.Platform.operr;

public class SNSEmailApplicationPlatform extends SNSApplicationPlatformBase {
    private static CLogger logger = Utils.getLogger(SNSEmailApplicationPlatform.class);

    public SNSEmailApplicationPlatform() {}

    public SNSEmailApplicationPlatform(SNSApplicationPlatformVO self) {
        super(self);
    }

    @Override
    protected SNSEmailPlatformVO getSelf() {
        return (SNSEmailPlatformVO) self;
    }

    @Override
    protected SNSEmailPlatformInventory getInventory() {
        return SNSEmailPlatformInventory.valueOf(getSelf());
    }

    @Override
    protected void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIValidateSNSEmailPlatformMsg) {
            handle((APIValidateSNSEmailPlatformMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    private Properties getMailProperties(boolean auth, String uuid, String smtpServer, Integer smtpPort) {
        String encryptType = "";
        if (!StringUtils.isEmpty(uuid)) {
            encryptType = SNSApplicationPlatformSystemTags.SNS_ENCRYPTTYPE.getTokenByResourceUuid(uuid, SNSApplicationPlatformSystemTags.SNS_ENCRYPTTYPE_TOKEN);
        }
        encryptType = StringUtils.isEmpty(encryptType) ? EmailServerEncryptType.STARTTLS.toString() : encryptType;

        Properties props = new Properties();
        if (EmailServerEncryptType.STARTTLS.toString().equals(encryptType)) {
            props.put("mail.smtp.starttls.enable", true);
        } else if (EmailServerEncryptType.SSL.toString().equals(encryptType)) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            if (smtpPort != null) {
                props.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
            }
        }
        props.put("mail.smtp.auth", auth);
        props.put("mail.debug", "true");
        props.put("mail.smtp.host", smtpServer);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        return props;
    }

    private void handle(APIValidateSNSEmailPlatformMsg msg) {
        String smtpServer = msg.getSmtpServer();
        Integer smtpPort = msg.getSmtpPort();
        String username = msg.getUsername();
        String password = msg.getPassword();
        String uuid = null;

        if (!StringUtils.isEmpty(msg.getUuid()) && !msg.getUuid().equals("null")) {
            SNSEmailPlatformVO platformVO = dbf.findByUuid(msg.getUuid(), SNSEmailPlatformVO.class);
            smtpServer = platformVO.getSmtpServer();
            smtpPort = platformVO.getSmtpPort();
            username = platformVO.getUsername();
            password = platformVO.getPassword();
            uuid = msg.getUuid();
        }

        if (!NetworkUtils.isRemotePortOpen(smtpServer, smtpPort, (int) TimeUnit.SECONDS.toMillis(15))) {
            throw new OperationFailureException(operr("cannot connect SMTP server[server: %s, port: %s] in 15 seconds", smtpServer, smtpPort));
        }

        String finalUsername = username;
        String finalPassword = password;
        Session session = Session.getInstance(getMailProperties(password != null, uuid, smtpServer, smtpPort), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(finalUsername, finalPassword);
            }
        });

        Transport transport = null;
        try {
            transport = session.getTransport("smtp");
            transport.connect();
        } catch (MessagingException e) {
            logger.debug(e.getMessage(), e);
            throw new OperationFailureException(operr("SMTP server validation error: %s", e.getMessage()));
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    logger.warn(String.format("Transport close MessagingException：%s", e.getMessage()));
                }
            }
        }

        APIValidateSNSEmailPlatformEvent evt = new APIValidateSNSEmailPlatformEvent(msg.getId());
        bus.publish(evt);
    }

    @Override
    public void publish(SNSTopicInventory topic, MessageStruct message, ReturnValueCompletion<List<SNSPublishError>> completion) {
        List<SNSEmailEndpointVO> emailEndpointVOS = SQL.New("select e from SNSEmailEndpointVO e, SNSSubscriberVO s" +
                " where e.uuid = s.endpointUuid and s.topicUuid = :topicUuid").param("topicUuid", topic.getUuid()).list();

        if (emailEndpointVOS.isEmpty()) {
            completion.success(null);
            return;
        }

        List<SNSPublishError> errors = new ArrayList<>();

        List<SNSEmailEndpointVO> enabled = new ArrayList<>();
        List<SNSEmailEndpointVO> disabled = new ArrayList<>();
        emailEndpointVOS.forEach(vo -> {
            if (vo.getState() == SNSApplicationEndpointState.Enabled) {
                enabled.add(vo);
            } else {
                disabled.add(vo);
            }
        });

        disabled.forEach(vo -> {
            SNSPublishError error = new SNSPublishError();
            error.setEndpointUuid(vo.getUuid());
            error.setPlatformType(self.getType());
            error.setPlatformUuid(self.getUuid());
            error.setError(operr("the endpoint is disabled"));
            errors.add(error);
        });

        Set<String> receipts = new HashSet<>();
        enabled.forEach(endpoint -> {
            if (endpoint.getEmail() != null) {
                receipts.add(endpoint.getEmail());
            }

            if (endpoint.getEmailAddresses() != null && !endpoint.getEmailAddresses().isEmpty()) {
                receipts.addAll(endpoint.getEmailAddresses().stream().map(SNSEmailAddressVO::getEmailAddress).collect(Collectors.toList()));
            }
        });

        if (!receipts.isEmpty()) {
            JavaMailSenderImpl sender = Platform.New(JavaMailSenderImpl::new);
            sender.setHost(getSelf().getSmtpServer());
            sender.setUsername(getSelf().getUsername());
            sender.setPassword(getSelf().getPassword());
            sender.setPort(getSelf().getSmtpPort());
            sender.setJavaMailProperties(getMailProperties(getSelf().getPassword() != null,
                    getSelf().getUuid(),
                    getSelf().getSmtpServer(),
                    getSelf().getSmtpPort()));

            EmailMessageMetadata metadata = null;
            if (message.getMetadata() != null) {
                metadata = JSONObjectUtil.rehashObject(message.getMetadata(), EmailMessageMetadata.class);
            }

            String subject = metadata == null ? i18n("no subject") : metadata.getSubject();

            try {
                MimeMessage mail = sender.createMimeMessage();
                MimeMessageHelper helper = Platform.New(()-> new MimeMessageHelper(mail));
                helper.setFrom(getSelf().getUsername());
                helper.setTo(receipts.toArray(new String[receipts.size()]));
                helper.setText(message.getMessage());
                helper.setSubject(subject);

                sender.send(mail);
            } catch (MessagingException e) {
                enabled.forEach(vo -> {
                    SNSPublishError error = new SNSPublishError();
                    error.setEndpointUuid(vo.getUuid());
                    error.setPlatformType(self.getType());
                    error.setPlatformUuid(self.getUuid());
                    error.setError(operr(e.getMessage()));
                    errors.add(error);
                });
            }
        }

        completion.success(errors);
    }
}
