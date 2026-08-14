package org.zstack.sns;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Qi Le on 2019-07-10
 */
@Inventory(mappingVOClass = SNSSmsReceiverVO.class)
public class SNSSmsReceiverInventory {
    private String uuid;
    private String phoneNumber;
    private String endpointUuid;
    private SmsReceiverType type;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SNSSmsReceiverInventory __example__() {
        SNSSmsReceiverInventory inv = new SNSSmsReceiverInventory();
        inv.uuid = DocUtils.createFixedUuid(SNSSmsReceiverVO.class);
        inv.phoneNumber = "18912345678";
        inv.endpointUuid = DocUtils.createFixedUuid(SNSSmsEndpointVO.class);
        inv.type = SmsReceiverType.AliyunSms;
        inv.description = "description";
        inv.createDate = DocUtils.timestamp();
        inv.lastOpDate = DocUtils.timestamp();
        return inv;
    }

    public static SNSSmsReceiverInventory valueOf(SNSSmsReceiverVO vo) {
        SNSSmsReceiverInventory inv = new SNSSmsReceiverInventory();
        inv.uuid = vo.getUuid();
        inv.phoneNumber = vo.getPhoneNumber();
        inv.endpointUuid = vo.getEndpointUuid();
        inv.type = vo.getType();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<SNSSmsReceiverInventory> valueOf(Collection<SNSSmsReceiverVO> vos) {
        return vos.stream().map(SNSSmsReceiverInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }

    public SmsReceiverType getType() {
        return type;
    }

    public void setType(SmsReceiverType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
