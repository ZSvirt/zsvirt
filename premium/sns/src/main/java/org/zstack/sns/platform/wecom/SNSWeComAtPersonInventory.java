package org.zstack.sns.platform.wecom;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SNSWeComAtPersonVO.class)
public class SNSWeComAtPersonInventory {
    private String uuid;
    private String userId;
    private String endpointUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String remark;

    public static SNSWeComAtPersonInventory __example__() {
        SNSWeComAtPersonInventory inv = new SNSWeComAtPersonInventory();
        inv.uuid = DocUtils.createFixedUuid(SNSWeComAtPersonVO.class);
        inv.userId = "18988887777";
        inv.endpointUuid = DocUtils.createFixedUuid(SNSWeComEndpointVO.class);
        inv.remark = "jack";
        return inv;
    }

    public static SNSWeComAtPersonInventory valueOf(SNSWeComAtPersonVO vo) {
        SNSWeComAtPersonInventory inv = new SNSWeComAtPersonInventory();
        inv.uuid = vo.getUuid();
        inv.userId = vo.getUserId();
        inv.endpointUuid = vo.getEndpointUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.remark = vo.getRemark();
        return inv;
    }

    public static List<SNSWeComAtPersonInventory> valueOf(Collection<SNSWeComAtPersonVO> vos) {
        return vos.stream().map(SNSWeComAtPersonInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
