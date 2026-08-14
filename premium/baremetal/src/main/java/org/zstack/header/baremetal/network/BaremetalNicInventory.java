package org.zstack.header.baremetal.network;

import org.zstack.header.baremetal.instance.BaremetalInstanceInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 7/5/18.
 */
@Inventory(mappingVOClass = BaremetalNicVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "baremetalInstance", inventoryClass = BaremetalInstanceInventory.class,
                foreignKey = "baremetalInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
})
public class BaremetalNicInventory implements Serializable {
    private String uuid;
    private String baremetalInstanceUuid;
    private String l3NetworkUuid;
    @APINoSee
    private String usedIpUuid;
    private String baremetalBondingUuid;
    private String mac;
    private String ip;
    private String netmask;
    private String gateway;
    private String metadata;
    private Boolean pxe;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public BaremetalNicInventory(){

    }

    public BaremetalNicInventory(BaremetalNicVO vo) {
        this.setUuid(vo.getUuid());
        this.setBaremetalInstanceUuid(vo.getBaremetalInstanceUuid());
        this.setL3NetworkUuid(vo.getL3NetworkUuid());
        this.setUsedIpUuid(vo.getUsedIpUuid());
        this.setBaremetalBondingUuid(vo.getBaremetalBondingUuid());
        this.setMac(vo.getMac());
        this.setIp(vo.getIp());
        this.setNetmask(vo.getNetmask());
        this.setGateway(vo.getGateway());
        this.setMetadata(vo.getMetaData());
        this.setPxe(vo.getPxe());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static BaremetalNicInventory valueOf(BaremetalNicVO vo) {
        return new BaremetalNicInventory(vo);
    }

    public static List<BaremetalNicInventory> valueOf(Collection<BaremetalNicVO> vos) {
        List<BaremetalNicInventory> nicCfgs = new ArrayList<>();
        for (BaremetalNicVO vo: vos) {
            nicCfgs.add(valueOf(vo));
        }
        return nicCfgs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBaremetalInstanceUuid() {
        return baremetalInstanceUuid;
    }

    public void setBaremetalInstanceUuid(String baremetalInstanceUuid) {
        this.baremetalInstanceUuid = baremetalInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getBaremetalBondingUuid() {
        return baremetalBondingUuid;
    }

    public void setBaremetalBondingUuid(String baremetalBondingUuid) {
        this.baremetalBondingUuid = baremetalBondingUuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean getPxe() {
        return pxe;
    }

    public void setPxe(Boolean pxe) {
        this.pxe = pxe;
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
