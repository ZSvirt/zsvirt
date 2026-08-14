package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-12.
 *
 * create pxelinux.cfg and ks.cfg for baremetal instance
 */
public class CreateBaremetalInstanceConfigsMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String pxeServerUuid;
    private String imageUuid;
    private String bmUuid;
    private String pxeNicMac;
    private String username;
    @NoLogging
    private String password;
    private String templateUuid;

    @Override
    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getBmUuid() {
        return bmUuid;
    }

    public void setBmUuid(String bmUuid) {
        this.bmUuid = bmUuid;
    }

    public String getPxeNicMac() {
        return pxeNicMac;
    }

    public void setPxeNicMac(String pxeNicMac) {
        this.pxeNicMac = pxeNicMac;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }
}
