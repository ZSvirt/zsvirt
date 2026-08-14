package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.network.l2.vxlan.vtep.RemoteVtepInventory;
import org.zstack.network.l2.vxlan.vtep.RemoteVtepVO;
import org.zstack.core.db.Q;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.network.l2.vxlan.vtep.VtepInventory;
import org.zstack.network.l2.vxlan.vtep.VtepVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;

import javax.persistence.JoinColumn;
import java.util.*;
import java.util.regex.Pattern;

@PythonClassInventory
@Inventory(mappingVOClass = VxlanNetworkPoolVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = L2NetworkInventory.class, type = VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vniRange", inventoryClass = VniRangeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l2NetworkUuid"),
        @ExpandedQuery(expandedField = "l2VxlanNetwork", inventoryClass = L2VxlanNetworkInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "poolUuid"),
        @ExpandedQuery(expandedField = "remotevtep", inventoryClass = RemoteVtepInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "poolUuid"),
        @ExpandedQuery(expandedField = "vtep", inventoryClass = VtepInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "poolUuid")
})
public class L2VxlanNetworkPoolInventory extends L2NetworkInventory {
    @Queryable(mappingClass = VtepInventory.class,
            joinColumn = @JoinColumn(name = "poolUuid"))
    private List<VtepInventory> attachedVtepRefs;

    @Queryable(mappingClass = RemoteVtepInventory.class,
            joinColumn = @JoinColumn(name = "poolUuid"))
    private List<RemoteVtepInventory> remoteVteps;

    @Queryable(mappingClass = L2VxlanNetworkInventory.class,
            joinColumn = @JoinColumn(name = "poolUuid"))
    private List<L2VxlanNetworkInventory> attachedVxlanNetworkRefs;

    @Queryable(mappingClass = VniRangeInventory.class,
            joinColumn = @JoinColumn(name = "l2NetworkUuid"))
    private List<VniRangeInventory> attachedVniRanges;

    @NoJsonSchema
    private Map<String, String> attachedCidrs;

    public L2VxlanNetworkPoolInventory() {
    }

    protected L2VxlanNetworkPoolInventory(VxlanNetworkPoolVO vo) {
        super(vo);
        String patern = "\\{.*\\}";
        attachedCidrs = new HashMap<>();
        for (Map<String, String> tag : VxlanSystemTags.VXLAN_POOL_CLUSTER_VTEP_CIDR.getTokensOfTagsByResourceUuid(vo.getUuid())) {
            if (!Pattern.matches(patern, tag.get(VxlanSystemTags.VTEP_CIDR_TOKEN))) {
                continue;
            }
            attachedCidrs.put(tag.get(VxlanSystemTags.CLUSTER_UUID_TOKEN),
                    tag.get(VxlanSystemTags.VTEP_CIDR_TOKEN).split("[{}]")[1]);
        }
        setAttachedCidrs(attachedCidrs);
        setAttachedVniRanges(VniRangeInventory.valueOf(vo.getAttachedVniRanges()));
        setRemoteVteps(RemoteVtepInventory.valueOf(vo.getRemoteVteps()));
        setAttachedVtepRefs(VtepInventory.valueOf(vo.getAttachedVtepRefs()));
        List<VxlanNetworkVO> networkVOS = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, vo.getUuid()).list();
        setAttachedVxlanNetworkRefs(L2VxlanNetworkInventory.valueOf1(networkVOS));
    }

    public static L2VxlanNetworkPoolInventory valueOf(VxlanNetworkPoolVO vo) {
        return new L2VxlanNetworkPoolInventory(vo);
    }

    public static List<L2VxlanNetworkPoolInventory> valueOf1(Collection<VxlanNetworkPoolVO> vos) {
        List<L2VxlanNetworkPoolInventory> invs = new ArrayList<L2VxlanNetworkPoolInventory>(vos.size());
        for (VxlanNetworkPoolVO vo : vos) {
            invs.add(new L2VxlanNetworkPoolInventory(vo));
        }
        return invs;
    }

    public List<RemoteVtepInventory> getRemoteVteps() {
        return remoteVteps;
    }

    public void setRemoteVteps(List<RemoteVtepInventory> remoteVteps) {
        this.remoteVteps = remoteVteps;
    }

    public List<VtepInventory> getAttachedVtepRefs() {
        return attachedVtepRefs;
    }

    public void setAttachedVtepRefs(List<VtepInventory> attachedVtepRefs) {
        this.attachedVtepRefs = attachedVtepRefs;
    }

    public List<L2VxlanNetworkInventory> getAttachedVxlanNetworkRefs() {
        return attachedVxlanNetworkRefs;
    }

    public void setAttachedVxlanNetworkRefs(List<L2VxlanNetworkInventory> attachedVxlanNetworkRefs) {
        this.attachedVxlanNetworkRefs = attachedVxlanNetworkRefs;
    }

    public List<VniRangeInventory> getAttachedVniRanges() {
        return attachedVniRanges;
    }

    public void setAttachedVniRanges(List<VniRangeInventory> attachedVniRanges) {
        this.attachedVniRanges = attachedVniRanges;
    }

    public Map<String, String> getAttachedCidrs() {
        return attachedCidrs;
    }

    public void setAttachedCidrs(Map<String, String> attachedCidrs) {
        this.attachedCidrs = attachedCidrs;
    }
}
