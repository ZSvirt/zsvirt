package org.zstack.monitoring.media;

import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/6/11.
 */
@Inventory(mappingVOClass = EmailMediaVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = MediaInventory.class,  type = MediaConstants.EMAIL_MEDIA_TYPE)}
)
public class EmailMediaInventory extends MediaInventory implements Serializable {
    private String smtpServer;
    private Integer smtpPort;
    private String username;

    @APINoSee
    @NoLogging
    private String password;

    public EmailMediaInventory() {
    }

    protected EmailMediaInventory(EmailMediaVO vo) {
        super(vo);
        password = vo.getPassword();
        smtpPort = vo.getSmtpPort();
        smtpServer = vo.getSmtpServer();
        username = vo.getUsername();
        password = vo.getPassword();
    }

    public static EmailMediaInventory valueOf(EmailMediaVO vo) {
        return new EmailMediaInventory(vo);
    }

    public static List<EmailMediaInventory> valueOf1(Collection<EmailMediaVO> vos) {
        List<EmailMediaInventory> invs = new ArrayList<>();
        for (EmailMediaVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
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
}
