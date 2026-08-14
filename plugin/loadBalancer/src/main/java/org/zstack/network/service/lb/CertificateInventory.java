package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.vip.VipInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by shixin on 03/22/2018.
 */
@Inventory(mappingVOClass = CertificateVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "listener", inventoryClass = LoadBalancerListenerCertificateRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "certificateUuid"),
})
public class CertificateInventory {
    private String name;
    private String uuid;
    private String certificate;
    private String description;
    private List<LoadBalancerListenerCertificateRefInventory> listeners;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static CertificateInventory valueOf(CertificateVO vo) {
        CertificateInventory inv = new CertificateInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setCertificate(vo.getCertificate());
        inv.setDescription(vo.getDescription());
        inv.setListeners(LoadBalancerListenerCertificateRefInventory.valueOf(vo.getListeners()));
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<CertificateInventory> valueOf(Collection<CertificateVO> vos) {
        List<CertificateInventory> invs = new ArrayList<CertificateInventory>();
        for (CertificateVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }


    public List<LoadBalancerListenerCertificateRefInventory> getListeners() {
        return listeners;
    }

    public void setListeners(List<LoadBalancerListenerCertificateRefInventory> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
