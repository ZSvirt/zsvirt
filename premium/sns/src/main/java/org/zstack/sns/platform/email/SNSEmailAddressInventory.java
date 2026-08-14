package org.zstack.sns.platform.email;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSEmailAddressVO.class)
public class SNSEmailAddressInventory {
    private String uuid;
    private String emailAddress;
    private String endpointUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public static SNSEmailAddressInventory __example__() {
        SNSEmailAddressInventory inv = new SNSEmailAddressInventory();
        inv.uuid = DocUtils.createFixedUuid(SNSEmailAddressVO.class);
        inv.emailAddress = "example@zstack.io";
        inv.endpointUuid = DocUtils.createFixedUuid(SNSEmailEndpointVO.class);
        inv.createDate = DocUtils.timestamp();
        inv.lastOpDate = DocUtils.timestamp();
        return inv;
    }

    public static SNSEmailAddressInventory valueOf(SNSEmailAddressVO vo) {
        SNSEmailAddressInventory inv = new SNSEmailAddressInventory();
        inv.uuid = vo.getUuid();
        inv.emailAddress = vo.getEmailAddress();
        inv.endpointUuid = vo.getEndpointUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();

        return inv;
    }

    public static List<SNSEmailAddressInventory> valueOf(Collection<SNSEmailAddressVO> vos) {
        return vos.stream().map(SNSEmailAddressInventory::valueOf).collect(Collectors.toList());
    }
}
