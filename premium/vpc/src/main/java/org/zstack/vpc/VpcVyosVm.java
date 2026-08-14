package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.DetachNicExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterMetadataOperator;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.vyos.*;
import org.zstack.utils.VersionComparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by weiwang on 15/11/2017
 */
public class VpcVyosVm extends VyosVm {
    @Autowired
    SnatConfigProxy snatProxy;

    public VpcVyosVm(VirtualRouterVmVO vo) {
        super(vo);
    }

    @Override
    public List<String> getSnatL3NetworkOnRouter(String vrUuid) {
        return snatProxy.getServiceUuidsByRouterUuid(vrUuid, NetworkServiceType.SNAT.toString());
    }

    @Override
    public void attachNetworkService(String vrUuid, String networkServiceType,String l3NetworkUuid){
        snatProxy.attachNetworkService(vrUuid, networkServiceType, Arrays.asList(l3NetworkUuid));
    }

    @Override
    public void detachNetworkService(String vrUuid, String networkServiceType,String l3NetworkUuid){
        snatProxy.detachNetworkService(vrUuid, networkServiceType, Arrays.asList(l3NetworkUuid));
    }

    protected List<Flow> createBootstrapFlows(HypervisorType hvType) {
        List<Flow> flows = new ArrayList<>();

        flows.add(apvmf.createBootstrapFlow(hvType));
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            flows.add(new VyosGetVersionFlow());
            flows.add(new VyosWaitAgentStartFlow());
            flows.add(new VyosDeployAgentFlow());
            //flows.add(new VpcVyosDeployZsnAgentFlow());
        }

        return flows;
    }

    protected ErrorCode validateOperationByVmTypeAndL3Type(String l3Uuid) {
        L3NetworkVO l3NetworkVO = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
        L3NetworkInventory l3 = L3NetworkInventory.valueOf(l3NetworkVO);
        if (!l3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)) {
            return null;
        }

        String zvrVersion = new VirtualRouterMetadataOperator().getZvrVersion(self.getUuid());
        if (zvrVersion == null) {
            return null;
        }
        VersionComparator allow_version = new VersionComparator(VyosConstants.VYOS_VMWARE_ALLOW_NIC_HOT_PLUGIN_VERSION);

        ErrorCode code = null;
        if (allow_version.compare(zvrVersion) > 0) {
            for (DetachNicExtensionPoint ext : pluginRgty.getExtensionList(DetachNicExtensionPoint.class)) {
                code = ext.validateDetachNicByDriverTypeAndClusterType(l3, VmInstanceInventory.valueOf(self));
            }
        }
        return code;
    }
}
