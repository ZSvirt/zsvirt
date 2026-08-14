package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by shixin on 03/22/2018.
 */
@Inventory(mappingVOClass = LoadBalancerListenerCertificateRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "certificate", inventoryClass = CertificateInventory.class,
                foreignKey = "certificateUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "listener", inventoryClass = LoadBalancerListenerInventory.class,
                foreignKey = "listenerUuid", expandedInventoryKey = "uuid")
})
public class LoadBalancerListenerCertificateRefInventory {
    private Long id;
    private String listenerUuid;
    private String certificateUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerListenerCertificateRefInventory valueOf(LoadBalancerListenerCertificateRefVO vo) {
        LoadBalancerListenerCertificateRefInventory inv = new LoadBalancerListenerCertificateRefInventory();
        inv.setId(vo.getId());
        inv.setListenerUuid(vo.getListenerUuid());
        inv.setCertificateUuid(vo.getCertificateUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<LoadBalancerListenerCertificateRefInventory> valueOf(Collection<LoadBalancerListenerCertificateRefVO> vos) {
        List<LoadBalancerListenerCertificateRefInventory> invs = new ArrayList<LoadBalancerListenerCertificateRefInventory>();
        for (LoadBalancerListenerCertificateRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
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

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }
}
