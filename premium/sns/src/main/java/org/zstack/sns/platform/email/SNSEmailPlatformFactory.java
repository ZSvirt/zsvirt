package org.zstack.sns.platform.email;

import com.sun.mail.util.MailSSLSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.sns.*;
import org.zstack.tag.SystemTagCreator;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class SNSEmailPlatformFactory implements SNSApplicationPlatformFactory, SNSApplicationEndpointFactory {
    @Autowired
    private DatabaseFacade dbf;

    public static final SNSApplicationPlatformType type = new SNSApplicationPlatformType(SNSConstants.EMAIL_PLATFORM);
    public static final SNSApplicationEndpointType endpointType = new SNSApplicationEndpointType(SNSConstants.EMAIL_PLATFORM);

    private void validate(String username, String password, String smtpServer, int smtpPort, String encryptType){
        // skip this step when running some cases, because the cases have used invalid arguments
        if (CoreGlobalProperty.UNIT_TEST_ON){
            return;
        }

        try {
            Properties properties = new Properties();
            //if username or password is null, mail.smtp.auth set flase,
            //When the SMTP server sets anonymous mail, the client needs to close auth
            if (username != null && password != null) {
                properties.put("mail.smtp.auth", true);
            } else {
                properties.put("mail.smtp.auth", false);
            }

            properties.put("mail.smtp.connectiontimeout", EmailPlatformGlobalProperty.SNS_ADD_EMAIL_PLATFORM_CONNECT_TIMEOUT);
            properties.put("mail.smtp.timeout", EmailPlatformGlobalProperty.SNS_ADD_EMAIL_PLATFORM_READ_TIMEOUT);
            if (EmailServerEncryptType.STARTTLS.toString().equals(encryptType)) {
                properties.put("mail.smtp.starttls.enable", true);
            } else if (EmailServerEncryptType.SSL.toString().equals(encryptType)) {
                String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
                properties.put("mail.smtp.socketFactory.class", SSL_FACTORY);
                properties.put("mail.smtp.socketFactory.fallback", "false");
                properties.put("mail.smtp.socketFactory.port", smtpPort);
                MailSSLSocketFactory sf = new MailSSLSocketFactory();
                sf.setTrustAllHosts(true);
                properties.put("mail.smtp.ssl.socketFactory", sf);
            }
            Session session = Session.getInstance(properties, null);

            Transport transport = session.getTransport("smtp");
            transport.connect(smtpServer, smtpPort, username, password);
        } catch (AuthenticationFailedException e) {
            throw new OperationFailureException(operr("The problem may be caused by an incorrect user name or password or email permission denied"));
        } catch (MessagingException e) {
            throw new OperationFailureException(operr("Couldn't connect to host, port: %s, %d. The problem may be caused by an incorrect smtpServer or smtpPort", smtpServer, smtpPort));
        } catch (GeneralSecurityException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public SNSApplicationPlatformVO createApplicationPlatform(SNSApplicationPlatformVO vo, APICreateSNSApplicationPlatformMsg msg) {
        SNSEmailPlatformVO evo = new SNSEmailPlatformVO(vo);
        APICreateSNSEmailPlatformMsg emsg = (APICreateSNSEmailPlatformMsg) msg;

        String username = (emsg.getUsername() == null || emsg.getUsername().isEmpty())
                ? "anonymous" : emsg.getUsername();
        String password = emsg.getPassword();
        int smtpPort = emsg.getSmtpPort();
        String smtpServer = emsg.getSmtpServer();
        String encryptType = emsg.getEncryptType();

        validate(username, password, smtpServer, smtpPort, encryptType);

        evo.setPassword(password);
        evo.setUsername(username);
        evo.setSmtpPort(smtpPort);
        evo.setSmtpServer(smtpServer);
        return evo;
    }

    @Override
    public String getApplicationPlatformType() {
        return SNSConstants.EMAIL_PLATFORM;
    }

    @Override
    public SNSApplicationPlatformInventory getSNSApplicationPlatformInventory(SNSApplicationPlatformVO vo) {
        if (vo instanceof SNSEmailPlatformVO) {
            return SNSEmailPlatformInventory.valueOf((SNSEmailPlatformVO)vo);
        } else {
            return SNSEmailPlatformInventory.valueOf(dbf.findByUuid(vo.getUuid(), SNSEmailPlatformVO.class));
        }
    }

    @Override
    public SNSApplicationPlatform getSNSApplicationPlatform(String uuid) {
        if (StringUtils.isNotBlank(uuid)) {
            return new SNSEmailApplicationPlatform(dbf.findByUuid(uuid, SNSEmailPlatformVO.class));
        }
        return new SNSEmailApplicationPlatform();
    }

    @Override
    public SNSApplicationEndpointVO createApplicationEndpoint(SNSApplicationEndpointVO vo, APICreateSNSApplicationEndpointMsg msg) {
        SNSEmailEndpointVO evo = new SNSEmailEndpointVO(vo);
        APICreateSNSEmailEndpointMsg emsg = (APICreateSNSEmailEndpointMsg) msg;
        evo.setEmail(emsg.getEmail());

        if (emsg.getEmails() != null && !emsg.getEmails().isEmpty()) {
            evo.setEmailAddresses(emsg.getEmails().stream().map(address -> {
                SNSEmailAddressVO svo = new SNSEmailAddressVO();
                svo.setEndpointUuid(evo.getUuid());
                svo.setEmailAddress(address);
                svo.setUuid(Platform.getUuid());
                return svo;
            }).collect(Collectors.toSet()));
        }

        return evo;
    }

    @Override
    public String getApplicationEndpointType() {
        return getApplicationPlatformType();
    }

    @Override
    public SNSApplicationEndpointInventory getSNSApplicationEndpointInventory(SNSApplicationEndpointVO vo) {
        if (vo instanceof SNSEmailEndpointVO) {
            return SNSEmailEndpointInventory.valueOf((SNSEmailEndpointVO)vo);
        } else {
            return SNSEmailEndpointInventory.valueOf(dbf.findByUuid(vo.getUuid(), SNSEmailEndpointVO.class));
        }
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint(String uuid) {
        return new SNSEmailEndpoint(dbf.findByUuid(uuid, SNSEmailEndpointVO.class));
    }

    @Override
    public SNSApplicationEndpoint getSNSApplicationEndpoint() {
        return new SNSEmailEndpoint();
    }

    @Override
    public List<SNSApplicationEndpoint> getSNSApplicationEndpoints(List<String> uuids) {
        List<SNSEmailEndpointVO> vos = Q.New(SNSEmailEndpointVO.class).in(SNSEmailEndpointVO_.uuid, uuids).list();
        return vos.stream().map(SNSEmailEndpoint::new).collect(Collectors.toList());
    }
}
