package org.zstack.header.network.service;

import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.*;

@Inventory(mappingVOClass = NetworkServiceProviderVO.class)
public class NetworkServiceProviderInventory {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = NetworkServiceTypeInventory.class,
            joinColumn = @JoinColumn(name = "networkServiceProviderUuid", referencedColumnName = "type"))
    private Set<String> networkServiceTypes;
    @Queryable(mappingClass = NetworkServiceProviderL2NetworkRefInventory.class,
            joinColumn = @JoinColumn(name = "networkServiceProviderUuid", referencedColumnName = "l2NetworkUuid"))
    private Set<String> attachedL2NetworkUuids;

    public NetworkServiceProviderInventory() {
    }

    protected NetworkServiceProviderInventory(NetworkServiceProviderVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setNetworkServiceTypes(vo.getNetworkServiceTypes());
        this.setType(vo.getType());
        this.attachedL2NetworkUuids = new HashSet<String>();
        for (NetworkServiceProviderL2NetworkRefVO ref : vo.getAttachedL2NetworkRefs()) {
            this.attachedL2NetworkUuids.add(ref.getL2NetworkUuid());
        }
    }

    public static NetworkServiceProviderInventory valueOf(NetworkServiceProviderVO vo) {
        NetworkServiceProviderInventory inv = new NetworkServiceProviderInventory(vo);
        return inv;
    }

    public static List<NetworkServiceProviderInventory> valueOf(Collection<NetworkServiceProviderVO> vos) {
        List<NetworkServiceProviderInventory> invs = new ArrayList<NetworkServiceProviderInventory>(vos.size());
        for (NetworkServiceProviderVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getNetworkServiceTypes() {
        return networkServiceTypes;
    }

    public void setNetworkServiceTypes(Set<String> networkServiceTypes) {
        this.networkServiceTypes = networkServiceTypes;
    }

    public Set<String> getAttachedL2NetworkUuids() {
        return attachedL2NetworkUuids;
    }

    public void setAttachedL2NetworkUuids(Set<String> attachedL2NetworkUuids) {
        this.attachedL2NetworkUuids = attachedL2NetworkUuids;
    }
}
