package org.zstack.sns.platform.dingtalk;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSDingTalkAtPersonVO.class)
public class SNSDingTalkAtPersonInventory {
    private String uuid;
    private String phoneNumber;
    private String endpointUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String remark;

    public static SNSDingTalkAtPersonInventory __example__() {
        SNSDingTalkAtPersonInventory inv = new SNSDingTalkAtPersonInventory();
        inv.uuid = DocUtils.createFixedUuid(SNSDingTalkAtPersonVO.class);
        inv.phoneNumber = "18988887777";
        inv.endpointUuid = DocUtils.createFixedUuid(SNSDingTalkEndpointVO.class);
        inv.createDate = DocUtils.timestamp();
        inv.lastOpDate = DocUtils.timestamp();
        inv.remark = "jack";
        return inv;
    }

    public static SNSDingTalkAtPersonInventory valueOf(SNSDingTalkAtPersonVO vo) {
        SNSDingTalkAtPersonInventory inv = new SNSDingTalkAtPersonInventory();
        inv.uuid = vo.getUuid();
        inv.phoneNumber = vo.getPhoneNumber();
        inv.endpointUuid = vo.getEndpointUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.remark = vo.getRemark();
        return inv;
    }

    public static List<SNSDingTalkAtPersonInventory> valueOf(Collection<SNSDingTalkAtPersonVO> vos) {
        return vos.stream().map(SNSDingTalkAtPersonInventory::valueOf).collect(Collectors.toList());
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
