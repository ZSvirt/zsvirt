package org.zstack.monitoring.media;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.OperationFailureException;

import javax.mail.*;
import java.util.Properties;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/6/11.
 */
public class EmailMediaFactory implements MediaFactory {
    @Autowired
    private DatabaseFacade dbf;

    public void validate(String username, String password, String smtpServer, int smtpPort){
        // skip this step when running some cases, because the cases have used invalid arguments
        if (CoreGlobalProperty.UNIT_TEST_ON){
            return;
        }

        try {
            Properties properties = new Properties();
            properties.put(MediaConstants.Email.MAIL_SMTP_AUTH, true);
            properties.put(MediaConstants.Email.MAIL_SMTP_STARTTLS, true);
            Session session = Session.getInstance(properties, null);

            Transport transport = session.getTransport(MediaConstants.Email.PROTOCOL);
            transport.connect(smtpServer, smtpPort, username, password);
        } catch (AuthenticationFailedException e) {
            throw new OperationFailureException(operr("The problem may be caused by an incorrect user name or password or email permission denied"));
        } catch (MessagingException e) {
            throw new OperationFailureException(operr("Couldn't connect to host, port: %s, %d. The problem may be caused by an incorrect smtpServer or smtpPort", smtpServer, smtpPort));
        }
    }

    @Override
    @Transactional
    public MediaVO createMedia(MediaVO vo, APICreateMediaMsg msg) {
        APICreateEmailMediaMsg emsg = (APICreateEmailMediaMsg) msg;

        String username = emsg.getUsername();
        String password = emsg.getPassword();
        int smtpPort = emsg.getSmtpPort();
        String smtpServer = emsg.getSmtpServer();

        validate(username, password, smtpServer, smtpPort);

        EmailMediaVO evo = new EmailMediaVO(vo);
        evo.setPassword(password);
        evo.setUsername(username);
        evo.setSmtpPort(smtpPort);
        evo.setSmtpServer(smtpServer);
        dbf.getEntityManager().persist(evo);
        dbf.getEntityManager().flush();
        dbf.getEntityManager().refresh(evo);

        return evo;
    }

    @Override
    public MediaInventory getMediaInventory(MediaVO vo) {
        return EmailMediaInventory.valueOf((EmailMediaVO) vo);
    }

    @Override
    public MediaInventory getMediaInventory(String uuid) {
        return getMediaInventory(dbf.findByUuid(uuid, EmailMediaVO.class));
    }

    @Override
    public String getMediaType() {
        return MediaConstants.EMAIL_MEDIA_TYPE;
    }
}
