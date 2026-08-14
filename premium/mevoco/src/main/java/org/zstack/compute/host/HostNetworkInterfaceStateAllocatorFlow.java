package org.zstack.compute.host;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.bonding.HostNetworkBondingConstant;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.ha.HaConstants;
import org.zstack.ha.HaStrategyConditionVO;
import org.zstack.ha.HaStrategyConditionVO_;
import org.zstack.ha.HaStrategyState;
import org.zstack.header.allocator.HostAllocatorFilterExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.network.l2.L2NetworkGetInterfaceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.i18m;

/**
 * @Author: DaoDao
 * @Date: 2023/4/18
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostNetworkInterfaceStateAllocatorFlow implements HostAllocatorFilterExtensionPoint {
    @Autowired
    private PluginRegistry pluginRgty;

    private static final CLogger logger = Utils.getLogger(HostNetworkInterfaceStateAllocatorFlow.class);

    private boolean isSkipAllocate(HostAllocatorSpec spec) {
        if (spec.getVmInstance() != null && !VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(spec.getVmInstance().getHypervisorType())) {
            return true;
        }

        if (spec.getImage() != null && !ImageConstant.ZSTACK_IMAGE_TYPE.equals(spec.getImage().getType())) {
            return true;
        }
        return false;
    }

    public void allocate(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (isSkipAllocate(spec)) {
            return;
        }

        if(!Q.New(HaStrategyConditionVO.class)
                .eq(HaStrategyConditionVO_.fencerName, HaConstants.HOST_BUSINESS_NIC)
                .eq(HaStrategyConditionVO_.state, HaStrategyState.Enable)
                .isExists()) {
            logger.debug("business nic is disable, skip HostNetworkInterfaceStateAllocatorFlow");
            return;
        }

        if (CollectionUtils.isEmpty(spec.getL3NetworkUuids())) {
            return;
        }

        List<String> hostUuids = candidates.stream().map(HostCandidate::getUuid).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hostUuids)) {
            return;
        }

        if (isVxlanNetwork(spec.getL3NetworkUuids())) {
            return;
        }

        List<Tuple> tuples = SQL.New("select distinct l2.uuid, l2.type, l2.physicalInterface" +
                        " from L2NetworkVO l2, L3NetworkVO l3" +
                        " where l3.l2NetworkUuid = l2.uuid" +
                        " and l3.uuid in (:l3uuids)" +
                        " and l2.type != :l2Type", Tuple.class)
                .param("l3uuids", spec.getL3NetworkUuids())
                .param("l2Type", VxlanNetworkConstant.VXLAN_NETWORK_TYPE)
                .list();
        for (HostCandidate candidate : candidates) {
            List<String> interfaceNames = new ArrayList<>();
            for (Tuple tuple : tuples) {
                String l2Uuid = tuple.get(0, String.class);
                String l2Type = tuple.get(1, String.class);
                String interfaceName = tuple.get(2, String.class);
                boolean added = false;

                for (L2NetworkGetInterfaceExtensionPoint ext : pluginRgty.getExtensionList(L2NetworkGetInterfaceExtensionPoint.class)) {
                    if (Objects.equals(ext.getType().toString(), l2Type)) {
                        String ifName = ext.getPhysicalInterfaceName(l2Uuid, candidate.getUuid());
                        if (!StringUtils.isEmpty(ifName)) {
                            interfaceNames.add(ifName.split("\\.")[0]);
                            added = true;
                        }
                    }
                }

                if (!added && !StringUtils.isEmpty(interfaceName)) {
                    interfaceNames.add(interfaceName.split("\\.")[0]);
                }
            }

            if (interfaceNames.isEmpty()) {
                candidate.markAsRejected(getClass(),
                        i18m("no available network interface on the host to start the vm"));
                continue;
            }

            List<String> uniqueInterfaceNames = interfaceNames.stream().distinct().collect(Collectors.toList());

            Long activateInterfaceNum = Q.New(HostNetworkBondingVO.class)
                    .eq(HostNetworkBondingVO_.hostUuid, candidate.getUuid())
                    .eq(HostNetworkBondingVO_.miiStatus, HostNetworkBondingConstant.MII_STATUS_UP)
                    .in(HostNetworkBondingVO_.bondingName, uniqueInterfaceNames)
                    .count();

            activateInterfaceNum += Q.New(HostNetworkInterfaceVO.class)
                    .eq(HostNetworkInterfaceVO_.hostUuid, candidate.getUuid())
                    .eq(HostNetworkInterfaceVO_.carrierActive, Boolean.TRUE)
                    .in(HostNetworkInterfaceVO_.interfaceName, uniqueInterfaceNames)
                    .count();

            if (activateInterfaceNum < uniqueInterfaceNames.size()) {
                candidate.markAsRejected(getClass(),
                        i18m("no available network interface on the host to start the vm"));
            }
        }
    }

    private boolean isVxlanNetwork(List<String> l3NetworkUuids) {
        List<String> physicalInterfaces = SQL.New("select l2 from L2NetworkVO l2, L3NetworkVO l3 " +
                "where l3.l2NetworkUuid = l2.uuid and l3.uuid in (:l3uuids) and l2.type = 'VxlanNetwork'")
               .param("l3uuids", l3NetworkUuids)
               .list();

        return !CollectionUtils.isEmpty(physicalInterfaces) && physicalInterfaces.size() == l3NetworkUuids.size();
    }

    @Override
    public void filter(List<HostCandidate> candidates, HostAllocatorSpec spec) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        allocate(candidates, spec);
    }
}
