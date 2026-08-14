package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NicIpAddressInfo;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateNicFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateNicFlow.class);
    private static final StaticIpOperator ipOperator = new StaticIpOperator();

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected L3NetworkManager l3nm;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    protected VmInstanceManager vmMgr;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Autowired
    protected ResourceConfigFacade rcf;

    @Override
    public String name() {
        return "create-nics";
    }

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        Boolean allowDuplicatedMac = (Boolean) data.get(VmInstanceConstant.Params.VmAllocateNicFlow_allowDuplicatedMac.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicSpec> l3Networks = spec.getL3Networks();
        for (VmNicSpec l3Network : l3Networks) {
            for (VmPreAttachL3NetworkExtensionPoint ext : pluginRgty.getExtensionList(VmPreAttachL3NetworkExtensionPoint.class)) {
                for (L3NetworkInventory l3Inv : l3Network.getL3Invs()) {
                    ext.vmPreAttachL3Network(spec.getVmInventory(), l3Inv);
                }
            }
        }
        final Map<String, NicIpAddressInfo> nicNetworkInfoMap =
                Optional.ofNullable(data.get(VmInstanceConstant.Params.VmAllocateNicFlow_nicNetworkInfo.toString()))
                .map(obj -> (Map<String, NicIpAddressInfo>) obj)
                .orElse(ipOperator.getNicNetworkInfoByVmUuid(spec.getVmInventory().getUuid()));
        ipOperator.updateNicNetworkInfoByVmNicParam(spec.getVmInventory().getUuid(), nicNetworkInfoMap, VmNicSpec.getVmNicParamsOfSpec(spec.getL3Networks()));

        final List<String> disableL3Networks = new ArrayList<>();
        if (spec.getDisableL3Networks() != null && !spec.getDisableL3Networks().isEmpty()) {
            disableL3Networks.addAll(spec.getDisableL3Networks());
        }

        // it's unlikely a vm having more than 512 nics
        final BitSet deviceIdBitmap = new BitSet(512);
        for (VmNicInventory nic : spec.getVmInventory().getVmNics()) {
            deviceIdBitmap.set(nic.getDeviceId());
        }

        List<VmNicInventory> nics = new ArrayList<>();
        data.put(VmInstanceConstant.Params.VmAllocateNicFlow_nics.toString(), nics);

        new While<>(VmNicSpec.getFirstL3NetworkInventoryOfSpec(spec.getL3Networks())).each((nicSpec, wcomp) -> {
            L3NetworkInventory nw = nicSpec.getL3Invs().get(0);
            VmNicParam nicParam = nicSpec.getVmNicParam();
            int deviceId = deviceIdBitmap.nextClearBit(0);
            deviceIdBitmap.set(deviceId);
            MacOperator mo = new MacOperator();
            String customMac = mo.getMac(spec.getVmInventory().getUuid(), nw.getUuid());
            if (customMac != null){
                mo.deleteCustomMacSystemTag(spec.getVmInventory().getUuid(), nw.getUuid(), customMac);
                customMac = customMac.toLowerCase();
            } else if (nicParam.getMac() != null) {
                customMac = nicParam.getMac().toLowerCase();
            } else {
                customMac = NetworkUtils.generateMacWithDeviceId((short) deviceId);
            }
            final String mac = customMac;

            // choose vnic factory based on nicParams of nicSpec & enableVhostUser globalConfig
            VmNicType type = nicManager.getVmNicType(spec.getVmInventory().getUuid(), nw, nicParam.isSriovEnabled());
            if (type == null) {
                wcomp.addError(Platform.operr("there is no available nicType on L3 network [%s]", nw.getUuid()));
                wcomp.allDone();
                return;
            }
            VmInstanceNicFactory vnicFactory = vmMgr.getVmInstanceNicFactory(type);

            CustomNicOperator nicOperator = new CustomNicOperator(spec.getVmInventory().getUuid(),nw.getUuid());
            final String customNicUuid = nicOperator.getCustomNicId();
            VmNicInventory nic = new VmNicInventory();
            if (customNicUuid != null) {
                nic.setUuid(customNicUuid);
            } else {
                nic.setUuid(Platform.getUuid());
            }
            /* the first ip is ipv4 address for dual stack nic */
            nic.setVmInstanceUuid(spec.getVmInventory().getUuid());
            nic.setL3NetworkUuid(nw.getUuid());
            nic.setMac(mac);
            nic.setHypervisorType(spec.getDestHost() == null ?
                    spec.getVmInventory().getHypervisorType() : spec.getDestHost().getHypervisorType());

            if (allowDuplicatedMac == null || !allowDuplicatedMac) {
                if (mo.checkDuplicateMac(nic.getHypervisorType(), nic.getMac())) {
                    wcomp.addError(Platform.operr("Duplicate mac address [%s]", nic.getMac()));
                    wcomp.allDone();
                    return;
                }
            }

            if (!StringUtils.isEmpty(nicSpec.getNicDriverType())) {
                nic.setDriverType(nicSpec.getNicDriverType());
            } else {
                boolean vmHasVirtio = VmSystemTags.VIRTIO.hasTag(spec.getVmInventory().getUuid());
                nicManager.setNicDriverType(nic, vmHasVirtio,
                        ImagePlatform.valueOf(spec.getVmInventory().getPlatform()).isParaVirtualization(),
                        spec.getVmInventory());
            }

            nic.setDeviceId(deviceId);
            nic.setInternalName(VmNicVO.generateNicInternalName(spec.getVmInventory().getInternalId(), nic.getDeviceId()));
            nic.setState(
                    disableL3Networks.contains(nic.getL3NetworkUuid()) || VmNicState.disable.toString().equals(nicParam.getState())
                            ? VmNicState.disable.toString()
                            : VmNicState.enable.toString()
            );
            final String vmNicUuid = new SQLBatchWithReturn<String>() {
                @Override
                protected String scripts() {
                    VmNicVO nicVO = vnicFactory.createVmNic(nic, spec);
                    if (!nw.enableIpAllocation() && nicNetworkInfoMap != null
                            && nicNetworkInfoMap.containsKey(nw.getUuid())
                            && spec.getVmInventory().getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
                        NicIpAddressInfo nicIpAddressInfo = nicNetworkInfoMap.get(nic.getL3NetworkUuid());
                        if (!StringUtils.isEmpty(nicIpAddressInfo.ipv6Address)) {
                            UsedIpVO vo = new UsedIpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setIp(nicIpAddressInfo.ipv6Address);
                            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
                            vo.setNetmask(IPv6NetworkUtils.getFormalNetmaskOfNetworkCidr(nicIpAddressInfo.ipv6Address+"/"+ nicIpAddressInfo.ipv6Prefix));
                            vo.setGateway(nicIpAddressInfo.ipv6Gateway);
                            vo.setIpVersion(IPv6Constants.IPv6);
                            vo.setVmNicUuid(nic.getUuid());
                            vo.setL3NetworkUuid(nic.getL3NetworkUuid());
                            vo.setIpRangeUuid(IpRangeHelper.getIpRangeUuid(nic.getL3NetworkUuid(), vo.getIp()));
                            nic.setUsedIpUuid(vo.getUuid());
                            nicVO.setUsedIpUuid(vo.getUuid());
                            nicVO.setIp(vo.getIp());
                            nicVO.setIpVersion(vo.getIpVersion());
                            nicVO.setNetmask(vo.getNetmask());
                            nicVO.setGateway(vo.getGateway());
                            persist(vo);
                        }
                        if (!StringUtils.isEmpty(nicIpAddressInfo.ipv4Address)) {
                            UsedIpVO vo = new UsedIpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setIp(nicIpAddressInfo.ipv4Address);
                            vo.setIpInLong(NetworkUtils.ipv4StringToLong(vo.getIp()));
                            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
                            vo.setIpRangeUuid(IpRangeHelper.getIpRangeUuid(nic.getL3NetworkUuid(), vo.getIp()));
                            vo.setGateway(nicIpAddressInfo.ipv4Gateway);
                            vo.setNetmask(nicIpAddressInfo.ipv4Netmask);
                            vo.setIpVersion(IPv6Constants.IPv4);
                            vo.setVmNicUuid(nic.getUuid());
                            vo.setL3NetworkUuid(nic.getL3NetworkUuid());
                            nic.setUsedIpUuid(vo.getUuid());
                            nicVO.setUsedIpUuid(vo.getUuid());
                            nicVO.setIp(vo.getIp());
                            nicVO.setIpVersion(vo.getIpVersion());
                            nicVO.setNetmask(vo.getNetmask());
                            nicVO.setGateway(vo.getGateway());
                            persist(vo);
                        }
                    }
                    nics.add(nic);
                    nicVO = merge(nicVO);
                    return nicVO.getUuid();
                }
            }.execute();
            addVmNicConfig(vmNicUuid, spec, nicParam);
            wcomp.done();

        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }

    private void addVmNicConfig(String vmNicUuid, VmInstanceSpec vmSpec, VmNicParam vmNicParam) {
        if (vmNicParam == null) {
            return;
        }

        // add vmnic bandwidth systemtag
        if (vmNicParam.getInboundBandwidth() != null || vmNicParam.getOutboundBandwidth() != null) {
            VmNicQosConfigBackend backend = vmMgr.getVmNicQosConfigBackend(vmSpec.getVmInventory().getType());
            backend.addNicQos(vmSpec.getVmInventory().getUuid(), vmNicUuid, vmNicParam.getOutboundBandwidth(), vmNicParam.getInboundBandwidth());
        }

        //add vmnic multiqueue config
        if (vmNicParam.getMultiQueueNum() != null) {
            ResourceConfig multiQueues = rcf.getResourceConfig(VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM.getIdentity());
            Integer queues = vmNicParam.getMultiQueueNum();
            multiQueues.updateValue(vmNicUuid, queues.toString());
        }

        boolean isWindowsVm = ImagePlatform.Windows.toString().equals(vmSpec.getVmInventory().getPlatform());
        VmDnsBackend bkd = vmMgr.getVmDnsBackend(vmSpec.getVmInventory().getType());
        if (bkd == null) {
            logger.debug(String.format("no dns backend found for vm type[%s], skip setting dns", vmSpec.getVmInventory().getType()));
            return;
        }

        if (isWindowsVm) {
            bkd.setNicDns(vmSpec.getVmInventory().getUuid(), vmNicUuid, vmNicParam.getDnsList(), IPv6Constants.IPv4);
            bkd.setNicDns(vmSpec.getVmInventory().getUuid(), vmNicUuid, vmNicParam.getDns6List(), IPv6Constants.IPv6);
        } else {
            List<String> dnsList = vmNicParam.getDnsList() == null ? new ArrayList<>() : vmNicParam.getDnsList();
            List<String> dns6List = vmNicParam.getDns6List() == null ? new ArrayList<>() : vmNicParam.getDns6List();
            dnsList.addAll(dns6List);
            // assign dns list to null to skip setting
            bkd.setVmDns(vmSpec.getVmInventory().getUuid(), dnsList.isEmpty() ? null : dnsList);
        }
    }

    @Override
    public void rollback(final FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final List<VmNicInventory> destNics = spec.getDestNics();
        if (destNics == null || destNics.isEmpty()) {
            chain.rollback();
            return;
        }
        logger.debug(String.format("%s nic need for delete", destNics.size()));
        for (VmNicInventory vmNic : destNics) {
            for (VmDetachNicExtensionPoint ext : pluginRgty.getExtensionList(VmDetachNicExtensionPoint.class)) {
                ext.afterDetachNic(vmNic);
            }

            VmNicType type = VmNicType.valueOf(vmNic.getType());
            VmInstanceNicFactory vnicFactory = vmMgr.getVmInstanceNicFactory(type);
            vnicFactory.releaseVmNic(vmNic);
        }
        dbf.removeByPrimaryKeys(destNics.stream().map(VmNicInventory::getUuid).collect(Collectors.toList()), VmNicVO.class);
        chain.rollback();
        return;
    }
}
