package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.BootstrapParams;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkCreateExtensionPoint;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.service.NetworkServiceProviderL2NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.network.service.eip.EipConstant;
import org.zstack.network.service.lb.LoadBalancerConstants;
import org.zstack.network.service.virtualrouter.VirtualRouterApplianceVmFactory;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalConfig;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/10/31.
 */
public class VyosVmBaseFactory extends VirtualRouterApplianceVmFactory implements Component,
        PrepareDbInitialValueExtensionPoint, L2NetworkCreateExtensionPoint, ApplianceVmPrepareBootstrapInfoExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VyosVmBaseFactory.class);

    @Autowired
    private AnsibleFacade asf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    protected PluginRegistry pluginRgty;
    @Autowired
    private ResourceConfigFacade rcf;

    private List<String> vyosPostCreateFlows;
    private List<String> vyosPostStartFlows;
    private List<String> vyosPostRebootFlows;
    private List<String> vyosPostDestroyFlows;
    private List<String> vyosReconnectFlows;
    private List<String> vyosProvisionConfigFlows;
    private FlowChainBuilder postCreateFlowsBuilder;
    private FlowChainBuilder postStartFlowsBuilder;
    private FlowChainBuilder postRebootFlowsBuilder;
    private FlowChainBuilder postDestroyFlowsBuilder;
    private FlowChainBuilder reconnectFlowsBuilder;
    private FlowChainBuilder provisionConfigFlows;
    private NetworkServiceProviderVO providerVO;

    private List<VyosPostCreateFlowExtensionPoint> postCreateFlowExtensionPoints;
    private List<VyosPostDestroyFlowExtensionPoint> postDestroyFlowExtensionPoints;
    private List<VyosPostRebootFlowExtensionPoint> postRebootFlowExtensionPoints;
    private List<VyosPostReconnectFlowExtensionPoint> postReconnectFlowExtensionPoints;
    private List<VyosPostStartFlowExtensionPoint> postStartFlowExtensionPoints;
    private List<VyosPostMigrateFlowExtensionPoint> postMigrateFlowExtensionPoints;
    private List<VyosProvisionConfigFlowExtensionPoint> provisionConfigFlowExtensionPoints;

    private final static List<String> supportedL2NetworkTypes = new ArrayList<>();

    static {
        supportedL2NetworkTypes.add(L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE);
        supportedL2NetworkTypes.add(L2NetworkConstant.L2_VLAN_NETWORK_TYPE);
    }

    public void setVyosPostCreateFlows(List<String> vyosPostCreateFlows) {
        this.vyosPostCreateFlows = vyosPostCreateFlows;
    }

    public void setVyosPostStartFlows(List<String> vyosPostStartFlows) {
        this.vyosPostStartFlows = vyosPostStartFlows;
    }

    public void setVyosPostRebootFlows(List<String> vyosPostRebootFlows) {
        this.vyosPostRebootFlows = vyosPostRebootFlows;
    }

    public void setVyosPostDestroyFlows(List<String> vyosPostDestroyFlows) {
        this.vyosPostDestroyFlows = vyosPostDestroyFlows;
    }

    public void setVyosReconnectFlows(List<String> vyosReconnectFlows) {
        this.vyosReconnectFlows = vyosReconnectFlows;
    }

    public void setVyosProvisionConfigFlows(List<String> vyosProvisionConfigFlows) {
        this.vyosProvisionConfigFlows = vyosProvisionConfigFlows;
    }

    public List<Flow> getPostCreateFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postCreateFlowsBuilder.getFlows());
        flows.addAll(postCreateFlowExtensionPoints.stream().map(VyosPostCreateFlowExtensionPoint::vyosPostCreateFlow).collect(Collectors.toList()));
        return flows;
    }

    public List<Flow> getPostStartFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postStartFlowsBuilder.getFlows());
        flows.addAll(postStartFlowExtensionPoints.stream().map(VyosPostStartFlowExtensionPoint::vyosPostStartFlow).collect(Collectors.toList()));
        return flows;
    }

    public List<Flow> getPostRebootFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postRebootFlowsBuilder.getFlows());
        flows.addAll(postRebootFlowExtensionPoints.stream().map(VyosPostRebootFlowExtensionPoint::vyosPostRebootFlow).collect(Collectors.toList()));
        return flows;
    }

    public List<Flow> getPostStopFlows() {
        return null;
    }

    public List<Flow> getPostMigrateFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postMigrateFlowExtensionPoints.stream().map(VyosPostMigrateFlowExtensionPoint::vyosPostMigrateFlow).collect(Collectors.toList()));
        return flows;
    }

    public List<Flow> getPostDestroyFlows() {
        List<Flow> flows = new ArrayList<>();
        flows.addAll(postDestroyFlowsBuilder.getFlows());
        flows.addAll(postDestroyFlowExtensionPoints.stream().map(VyosPostDestroyFlowExtensionPoint::vyosPostDestroyFlow).collect(Collectors.toList()));
        return flows;
    }

    public FlowChain getReconnectFlowChain() {
        FlowChain c = reconnectFlowsBuilder.build();
        for (VyosPostReconnectFlowExtensionPoint ext : postReconnectFlowExtensionPoints) {
            c.then(ext.vyosPostReconnectFlow());
        }

        /* flush config is part of reconnect flow */
        for (VyosProvisionConfigFlowExtensionPoint ext : provisionConfigFlowExtensionPoints) {
            c.then(ext.vyosProvisionConfigFlow());
        }
        return c;
    }

    public FlowChain getFlushConfigChain() {
        FlowChain c = provisionConfigFlows.build();
        for (VyosProvisionConfigFlowExtensionPoint ext : provisionConfigFlowExtensionPoints) {
            c.then(ext.vyosProvisionConfigFlow());
        }
        return c;
    }

    @Override
    public ApplianceVm getSubApplianceVm(ApplianceVmVO apvm) {
        VirtualRouterVmVO vr = dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class);
        return new VyosVm(vr);
    }

    protected void buildWorkFlowBuilder() {
        postCreateFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostCreateFlows).construct();
        postStartFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostStartFlows).construct();
        postRebootFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostRebootFlows).construct();
        postDestroyFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosPostDestroyFlows).construct();
        reconnectFlowsBuilder = FlowChainBuilder.newBuilder().setFlowClassNames(vyosReconnectFlows).construct();
        provisionConfigFlows = FlowChainBuilder.newBuilder().setFlowClassNames(vyosProvisionConfigFlows).construct();
    }


    @Override
    public boolean start() {
        buildWorkFlowBuilder();
        populateExtensions();

        VirtualRouterGlobalConfig.VYOS_PASSWORD.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!newValue.matches("^[a-zA-Z0-9][A-Za-z0-9-_#]*")) {
                    throw new GlobalConfigException("the vrouter password's first character must be a letter or number, besides no characters other than letters, numbers and these special characters: -_#");
                }
            }
        });

        return true;
    }

    protected void populateExtensions() {
        postCreateFlowExtensionPoints = pluginRgty.getExtensionList(VyosPostCreateFlowExtensionPoint.class);
        postDestroyFlowExtensionPoints = pluginRgty.getExtensionList(VyosPostDestroyFlowExtensionPoint.class);
        postRebootFlowExtensionPoints = pluginRgty.getExtensionList(VyosPostRebootFlowExtensionPoint.class);
        postReconnectFlowExtensionPoints = pluginRgty.getExtensionList(VyosPostReconnectFlowExtensionPoint.class);
        postStartFlowExtensionPoints = pluginRgty.getExtensionList(VyosPostStartFlowExtensionPoint.class);
        postMigrateFlowExtensionPoints = pluginRgty.getExtensionList(VyosPostMigrateFlowExtensionPoint.class);
        provisionConfigFlowExtensionPoints = pluginRgty.getExtensionList(VyosProvisionConfigFlowExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void prepareDbInitialValue() {
        SimpleQuery<NetworkServiceProviderVO> query = dbf.createQuery(NetworkServiceProviderVO.class);
        query.add(NetworkServiceProviderVO_.type, Op.EQ, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        providerVO = query.find();
        if (providerVO != null) {
            return;
        }

        NetworkServiceProviderVO vo = new NetworkServiceProviderVO();
        vo.setUuid(Platform.getUuid());
        vo.setName(VyosConstants.VYOS_VM_TYPE);
        vo.setDescription("zstack vrouter network service provider");
        vo.getNetworkServiceTypes().add(NetworkServiceType.DHCP.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.DNS.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.SNAT.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.PortForwarding.toString());
        vo.getNetworkServiceTypes().add(NetworkServiceType.Centralized_DNS.toString());
        vo.getNetworkServiceTypes().add(EipConstant.EIP_NETWORK_SERVICE_TYPE);
        vo.getNetworkServiceTypes().add(LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING);
        //hard code for the premium plugin
        vo.getNetworkServiceTypes().add("IPsec");
        vo.getNetworkServiceTypes().add("VRouterRoute");
        vo.getNetworkServiceTypes().add("VipQos");
        vo.setType(VyosConstants.VYOS_ROUTER_PROVIDER_TYPE);
        providerVO = dbf.persistAndRefresh(vo);
    }

    @Override
    public void beforeCreateL2Network(APICreateL2NetworkMsg msg) throws NetworkException {

    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
        if (!supportedL2NetworkTypes.contains(l2Network.getType())) {
            return;
        }

        NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
        ref.setNetworkServiceProviderUuid(providerVO.getUuid());
        ref.setL2NetworkUuid(l2Network.getUuid());
        dbf.persist(ref);
        String info = String.format("successfully attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
                providerVO.getUuid(), providerVO.getName(), providerVO.getType(), l2Network.getUuid(), l2Network.getName(), l2Network.getType());
        logger.debug(info);
    }

    public String getNetworkServiceProviderUuid() {
        return providerVO.getUuid();
    }

    @Override
    public void applianceVmPrepareBootstrapInfo(VmInstanceSpec spec, Map<String, Object> info) {
        SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
        q.add(ApplianceVmVO_.applianceVmType, Op.EQ, VyosConstants.VYOS_VM_TYPE);
        q.add(ApplianceVmVO_.uuid, Op.EQ, spec.getVmInventory().getUuid());
        if (!q.isExists()) {
            return;
        }
        logger.debug("add vyos password to vrouter");
        info.put(VyosConstants.BootstrapInfoKey.vyosPassword.toString(), VirtualRouterGlobalConfig.VYOS_PASSWORD.value());
        info.put(BootstrapParams.sshPort.toString(), VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class));

        /* vrouter only has 1 private network */
        List<String> l3Uuids = (List<String>)info.get(ApplianceVmConstant.BootstrapParams.additionalL3Uuids.toString());
        if (rcf.getResourceConfigValue(VyosGlobalConfig.CONFIG_FIREWALL_WITH_IPTABLES, l3Uuids.get(0), Boolean.class)) {
            info.put(VyosConstants.REPLACE_FIREWALL_WITH_IPTBALES, true);
        }

        if (rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_VYOS_CMD, spec.getVmInventory().getUuid(), Boolean.class)) {
            info.put(VyosConstants.CONFIG_ENABLE_VYOS, true);
        } else {
            info.put(VyosConstants.CONFIG_ENABLE_VYOS, false);
        }
        
        info.put(VirtualRouterConstant.TC_FOR_VIPQOS, rcf.getResourceConfigValue(VirtualRouterGlobalConfig.TC_FOR_VIPQOS, spec.getVmInventory().getUuid(), Boolean.class));
        info.put(ApplianceVmConstant.ABNORMAL_FILE_MAX_SIZE, ApplianceVmGlobalConfig.ABNORMAL_FILE_MAX_SIZE.value(Integer.class));
    }
}
