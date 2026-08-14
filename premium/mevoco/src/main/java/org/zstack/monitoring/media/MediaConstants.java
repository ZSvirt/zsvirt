package org.zstack.monitoring.media;

/**
 * Created by xing5 on 2017/6/11.
 */
public interface MediaConstants {
    String EMAIL_MEDIA_TYPE = "Email";
    String NOTIFICATION_TYPE = "Notification";
    String SERVICE_ID = "media";

    interface Email {
        String PROTOCOL = "smtp";
        String MAIL_SMTP_AUTH = "mail.smtp.auth";
        String MAIL_SMTP_STARTTLS = "mail.smtp.starttls.enable";
    }
}
