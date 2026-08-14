package org.zstack.header.vipQos;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VipQosVO.class)
public class VipQosInventory {
    private String     uuid;
    private String     vipUuid;
    private Integer    port;
    private Long       inboundBandwidth;
    private Long       outboundBandwidth;
    private String     type;
    private Timestamp  createDate;
    private Timestamp  lastOpDate;

    public static VipQosInventory valueOf (VipQosVO vo) {
        VipQosInventory inv = new VipQosInventory();

        inv.setUuid(vo.getUuid());
        inv.setVipUuid(vo.getVipUuid());
        inv.setPort(vo.getPort());
        inv.setInboundBandwidth(vo.getInboundBandwidth());
        inv.setOutboundBandwidth(vo.getOutboundBandwidth());
        inv.setType(vo.getType());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        return inv;
    }

    public static List<VipQosInventory> valueOf (Collection<VipQosVO> vos) {
        List<VipQosInventory> invs = new ArrayList<>();
        for (VipQosVO vo : vos) {
            invs.add(VipQosInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
