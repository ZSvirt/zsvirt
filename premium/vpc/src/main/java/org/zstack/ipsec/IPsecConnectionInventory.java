package org.zstack.ipsec;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.NormalIpRangeVO_;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.NormalIpRangeVO_;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterNicMetaData;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/11/3.
 */
@Inventory(mappingVOClass = IPsecConnectionVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = IPsecL3NetworkRefInventory.class,
                foreignKey = "l3NetworkRefs", expandedInventoryKey = "l3NetworkUuid"),
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
})
public class IPsecConnectionInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String peerAddress;
    private String authMode;
    private String authKey;
    private String vipUuid;
    private String ikeAuthAlgorithm;
    private String ikeEncryptionAlgorithm;
    private Integer ikeDhGroup;
    private String policyAuthAlgorithm;
    private String policyEncryptionAlgorithm;
    private String pfs;
    private String policyMode;
    private String transformProtocol;
    private String ikeVersion;
    private String idType;
    private String localId;
    private String remoteId;
    private String state;
    private String status;
    private int ikeLifeTime;
    private int lifeTime;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = IPsecPeerCidrInventory.class,
            joinColumn = @JoinColumn(name = "connectionUuid"))
    private List<IPsecPeerCidrInventory> peerCidrs;
    @Queryable(mappingClass = IPsecL3NetworkRefInventory.class,
            joinColumn = @JoinColumn(name = "connectionUuid"))
    private List<IPsecL3NetworkRefInventory> l3NetworkRefs;

    public static List<IPsecConnectionInventory> valueOf(Collection<IPsecConnectionVO> vos) {
        List<IPsecConnectionInventory> invs = new ArrayList<>();
        for (IPsecConnectionVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public static IPsecConnectionInventory valueOf(IPsecConnectionVO vo) {
        IPsecConnectionInventory inv = new IPsecConnectionInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setL3NetworkRefs(IPsecL3NetworkRefInventory.valueOf(vo.getL3Networks()));
        inv.setPeerAddress(vo.getPeerAddress());
        inv.setAuthMode(vo.getAuthMode());
        inv.setAuthKey(vo.getAuthKey());
        inv.setVipUuid(vo.getVipUuid());
        inv.setIkeAuthAlgorithm(vo.getIkeAuthAlgorithm());
        inv.setIkeEncryptionAlgorithm(vo.getIkeEncryptionAlgorithm());
        inv.setIkeDhGroup(vo.getIkeDhGroup());
        inv.setPolicyAuthAlgorithm(vo.getPolicyAuthAlgorithm());
        inv.setPolicyEncryptionAlgorithm(vo.getPolicyEncryptionAlgorithm());
        inv.setPfs(vo.getPfs());
        inv.setPolicyMode(vo.getPolicyMode());
        inv.setTransformProtocol(vo.getTransformProtocol());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPeerCidrs(IPsecPeerCidrInventory.valueOf(vo.getPeerCidrs()));
        inv.setState(vo.getState().toString());
        inv.setStatus(vo.getStatus().toString());
        inv.setIkeVersion(vo.getIkeVersion());
        inv.setIdType(vo.getIdType());
        inv.setLocalId(vo.getLocalId());
        inv.setRemoteId(vo.getRemoteId());
        inv.setIkeLifeTime(vo.getIkeLifeTime());
        inv.setLifeTime(vo.getLifeTime());
        return inv;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getIkeAuthAlgorithm() {
        return ikeAuthAlgorithm;
    }

    public void setIkeAuthAlgorithm(String ikeAuthAlgorithm) {
        this.ikeAuthAlgorithm = ikeAuthAlgorithm;
    }

    public String getIkeEncryptionAlgorithm() {
        return ikeEncryptionAlgorithm;
    }

    public void setIkeEncryptionAlgorithm(String ikeEncryptionAlgorithm) {
        this.ikeEncryptionAlgorithm = ikeEncryptionAlgorithm;
    }

    public List<IPsecL3NetworkRefInventory> getL3NetworkRefs() {
        return l3NetworkRefs;
    }

    public void setL3NetworkRefs(List<IPsecL3NetworkRefInventory> l3NetworkRefs) {
        this.l3NetworkRefs = l3NetworkRefs;
    }

    public Integer getIkeDhGroup() {
        return ikeDhGroup;
    }

    public void setIkeDhGroup(Integer ikeDhGroup) {
        this.ikeDhGroup = ikeDhGroup;
    }

    public String getPolicyAuthAlgorithm() {
        return policyAuthAlgorithm;
    }

    public void setPolicyAuthAlgorithm(String policyAuthAlgorithm) {
        this.policyAuthAlgorithm = policyAuthAlgorithm;
    }

    public String getPolicyEncryptionAlgorithm() {
        return policyEncryptionAlgorithm;
    }

    public void setPolicyEncryptionAlgorithm(String policyEncryptionAlgorithm) {
        this.policyEncryptionAlgorithm = policyEncryptionAlgorithm;
    }

    public String getPfs() {
        return pfs;
    }

    public void setPfs(String pfs) {
        this.pfs = pfs;
    }

    public String getPolicyMode() {
        return policyMode;
    }

    public void setPolicyMode(String policyMode) {
        this.policyMode = policyMode;
    }

    public String getTransformProtocol() {
        return transformProtocol;
    }

    public void setTransformProtocol(String transformProtocol) {
        this.transformProtocol = transformProtocol;
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

    public List<IPsecPeerCidrInventory> getPeerCidrs() {
        return peerCidrs;
    }

    public void setPeerCidrs(List<IPsecPeerCidrInventory> peerCidrs) {
        this.peerCidrs = peerCidrs;
    }

    public List<String> getPeerCidrSignatures() {
        return peerCidrs.stream().map(IPsecPeerCidrInventory::getCidr).collect(Collectors.toList());
    }

    public List<String> getLocalL3Networks() {
        return l3NetworkRefs.stream().map(IPsecL3NetworkRefInventory::getL3NetworkUuid).collect(Collectors.toList());
    }

    public String getIkeVersion() {
        return ikeVersion;
    }

    public void setIkeVersion(String ikeVersion) {
        this.ikeVersion = ikeVersion;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public int getIkeLifeTime() {
        return ikeLifeTime;
    }

    public void setIkeLifeTime(int ikeLifeTime) {
        this.ikeLifeTime = ikeLifeTime;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }
    /* get cidrs of all networks which is attached to vpc router */
    public Set<String> getLocalL3Cidrs() {
        Set<String> cidrs = new HashSet<>();
        if (l3NetworkRefs.isEmpty()) {
            return cidrs;
        }

        List<NormalIpRangeVO> iprVos = Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.l3NetworkUuid,
                l3NetworkRefs.stream().map(IPsecL3NetworkRefInventory::getL3NetworkUuid).collect(Collectors.toList())).eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
        for (NormalIpRangeVO vo : iprVos) {
            String cidr = NetworkUtils.getCanonicalNetworkCidr(vo.getGateway(), vo.getNetmask());
            if (!cidrs.contains(cidr)) {
                cidrs.add(cidr);
            }
        }

        return cidrs;
    }

    public String getVirtualRouter() {
        return new SQLBatchWithReturn<String>() {
            @Override
            protected String scripts() {
                if (l3NetworkRefs.isEmpty()) {
                    return null;
                }

                List<VmNicVO> nics = Q.New(VmNicVO.class).in(VmNicVO_.l3NetworkUuid,
                        l3NetworkRefs.stream().map(IPsecL3NetworkRefInventory::getL3NetworkUuid).collect(Collectors.toList()))
                        .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).list();
                if (nics == null || nics.isEmpty()) {
                    return null;
                }

                List<String> vrUuids = nics.stream().map(VmNicVO::getVmInstanceUuid).distinct().collect(Collectors.toList());
                if (vrUuids.isEmpty()) {
                    return null;
                }

                /* ipsecApiInterceptor make sure vrUuids.size == 0 */
                DebugUtils.Assert(vrUuids.size() == 1, String.format("L3Networks [uuids: %s] of ipsec [uuid: %s, name: %s] is attached to multiple routers [uuids: %s]",
                        getLocalL3Networks(), getUuid(), getName(), vrUuids));
                return vrUuids.get(0);
            }
        }.execute();
    }
}
