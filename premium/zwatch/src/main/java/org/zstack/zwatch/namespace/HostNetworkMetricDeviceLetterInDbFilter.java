package org.zstack.zwatch.namespace;

import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l2.L2VlanNetworkVO;
import org.zstack.header.network.l2.L2VlanNetworkVO_;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zwatch.datatype.HostNetworkMetricFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HostNetworkMetricDeviceLetterInDbFilter implements HostNetworkMetricFilter{
    public static String filterName = "NetworkDeviceLetterInDb";
    private static CLogger logger = Utils.getLogger(HostNetworkMetricDeviceLetterInDbFilter.class);

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public boolean test(Object o) {
        Map m = (Map) o;

        /* this filter ONLY works on NetworkDeviceLetter */
        String name = (String)m.get(HostNamespace.LabelNames.NetworkDeviceLetter.toString());
        if (name == null) {
            return false;
        }

        String hostUuid = (String)m.get(HostNamespace.LabelNames.HostUuid.toString());

        if (name.startsWith("vxlan")) { /* vxlan interface */
            int vxlanId;
            try {
                String vxlan = name.replace("vxlan", "");
                vxlanId = Integer.valueOf(vxlan);
            } catch (Exception e) {
                return false;
            }

            if (hostUuid == null) {
                /* vxlan must has been bond to some clusters, and specified host has been added to the cluster */
                String sql = "select host.uuid from VxlanNetworkVO vxlan, L2NetworkVO l2, L2NetworkClusterRefVO ref, HostVO host " +
                        "where vxlan.vni = :vni and vxlan.uuid=l2.uuid and ref.l2NetworkUuid = l2.uuid and ref.clusterUuid = host.clusterUuid";
                List<String> hostUuids = SQL.New(sql, String.class)
                        .param("vni", vxlanId)
                        .list();
                if (hostUuids != null && !hostUuids.isEmpty()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                /* vxlan must has been bond to some clusters, and specified host has been added to the cluster */
                String sql = "select host.uuid from VxlanNetworkVO vxlan, L2NetworkVO l2, L2NetworkClusterRefVO ref, HostVO host " +
                        "where vxlan.vni = :vni and vxlan.uuid=l2.uuid and ref.l2NetworkUuid = l2.uuid and ref.clusterUuid = host.clusterUuid and host.uuid=:hostUuid";
                List<String> hostUuids = SQL.New(sql, String.class)
                        .param("vni", vxlanId)
                        .param("hostUuid", hostUuid)
                        .list();
                if (hostUuids != null && !hostUuids.isEmpty() && hostUuids.contains(hostUuid)) {
                    return true;
                } else {
                    return false;
                }
            }
        } else if (name.contains(".")){   /* vlan sub interface */
            String[] fields = name.split("\\.");
            if (fields.length != 2) {
                return true;
            }

            int vlanId;
            try {
                vlanId = Integer.valueOf(fields[1]);
            } catch (Exception e) {
                return false;
            }

            if (hostUuid == null) {
                /* vlan must has been bond to some clusters, and specified host has been added to the cluster */
                String sql = "select host.uuid from L2VlanNetworkVO vlan, L2NetworkVO l2, L2NetworkClusterRefVO ref, HostVO host " +
                        "where vlan.vlan = :vlanId and vlan.uuid=l2.uuid and l2.physicalInterface= :interfacename " +
                        "and ref.l2NetworkUuid = l2.uuid and ref.clusterUuid = host.clusterUuid";
                List<String> hostUuids = SQL.New(sql, String.class)
                        .param("vlanId", vlanId)
                        .param("interfacename", fields[0])
                        .list();
                if (hostUuids != null && !hostUuids.isEmpty()) {
                    return true;
                } else {
                    return false;
                }
            } else {
                /* vlan must has been bond to some clusters, and specified host has been added to the cluster */
                String sql = "select host.uuid from L2VlanNetworkVO vlan, L2NetworkVO l2, L2NetworkClusterRefVO ref, HostVO host " +
                        "where vlan.vlan = :vlanId and vlan.uuid=l2.uuid and l2.physicalInterface= :interfacename " +
                        "and ref.l2NetworkUuid = l2.uuid and ref.clusterUuid = host.clusterUuid and host.uuid = :hostUuid";
                List<String> hostUuids = SQL.New(sql, String.class)
                        .param("vlanId", vlanId)
                        .param("interfacename", fields[0])
                        .param("hostUuid", hostUuid)
                        .list();
                if (hostUuids != null && !hostUuids.isEmpty() && hostUuids.contains(hostUuid)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        /* for unknown type will not filter */
        return true;
    }
}
