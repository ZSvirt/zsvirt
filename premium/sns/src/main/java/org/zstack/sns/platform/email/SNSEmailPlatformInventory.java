package org.zstack.sns.platform.email;

import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationPlatformInventory;
import org.zstack.sns.SNSApplicationPlatformVO;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSEmailPlatformVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "endpoints", inventoryClass = SNSEmailEndpointInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "platformUuid")
})
public class SNSEmailPlatformInventory extends SNSApplicationPlatformInventory implements Serializable {
    private String smtpServer;
    private int smtpPort;
    private String username;
    @APINoSee
    @NoLogging
    private String password;

    public SNSEmailPlatformInventory() {
    }

    public SNSEmailPlatformInventory(SNSApplicationPlatformVO vo) {
        super(vo);
    }

    public SNSEmailPlatformInventory(SNSApplicationPlatformInventory other) {
        super(other);
    }

    public static SNSEmailPlatformInventory valueOf(SNSEmailPlatformVO vo) {
        SNSEmailPlatformInventory inv = new SNSEmailPlatformInventory(vo);
        inv.smtpPort = vo.getSmtpPort();
        inv.smtpServer = vo.getSmtpServer();
        inv.username = vo.getUsername();
        inv.password = vo.getPassword();
        return inv;
    }

    public static List<SNSEmailPlatformInventory> valueOf1(Collection<SNSEmailPlatformVO> vos) {
        return vos.stream().map(SNSEmailPlatformInventory::valueOf).collect(Collectors.toList());
    }

    public String getSmtpServer() {

        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
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
