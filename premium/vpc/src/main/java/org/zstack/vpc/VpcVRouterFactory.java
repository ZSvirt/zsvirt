package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.*;
import org.zstack.appliancevm.ApplianceVmConstant.BootstrapParams;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.header.vpc.ha.VpcHaGroupApplianceVmRefVO;
import org.zstack.network.l3.IpRangeHelper;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.network.service.virtualrouter.vyos.VyosGlobalConfig;
import org.zstack.network.service.virtualrouter.vyos.VyosVmBaseFactory;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.vpc.ha.VpcHaGroupGlobalConfig;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by weiwang on 18/09/2017
 */
public class VpcVRouterFactory extends VyosVmBaseFactory implements ApplianceVmPrepareBootstrapInfoExtensionPoint,
        ApvmCascadeFilterExtensionPoint, IpRangeDeletionExtensionPoint, VmPreAttachL3NetworkExtensionPoint,
        FilterAttachableL3NetworkExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VpcVRouterFactory.class);
    public static ApplianceVmType applianceVmType = new ApplianceVmType(VpcConstants.VPC_VROUTER_VM_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public ApplianceVmType getApplianceVmType() {
        return applianceVmType;
    }

    @Override
    public void applianceVmPrepareBootstrapInfo(VmInstanceSpec spec, Map<String, Object> info) {
        SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
        q.add(ApplianceVmVO_.applianceVmType, SimpleQuery.Op.EQ, VpcConstants.VPC_VROUTER_VM_TYPE);
        q.add(ApplianceVmVO_.uuid, SimpleQuery.Op.EQ, spec.getVmInventory().getUuid());
        ApplianceVmVO applianceVmVO = q.find();
        if (applianceVmVO == null) {
            return;
        }

        info.put(VyosConstants.HA_STATUS, applianceVmVO.getHaStatus().toString());

        logger.debug("add vpc password to vpc vrouter");
        info.put(VyosConstants.BootstrapInfoKey.vyosPassword.toString(), VirtualRouterGlobalConfig.VYOS_PASSWORD.value());
        info.put(BootstrapParams.sshPort.toString(), VirtualRouterGlobalConfig.SSH_PORT.value(Integer.class));

        if (rcf.getResourceConfigValue(VpcGlobalConfig.CONFIG_FIREWALL_WITH_IPTABLES, spec.getVmInventory().getUuid(), Boolean.class)) {
            info.put(VyosConstants.REPLACE_FIREWALL_WITH_IPTBALES, true);
        }

        if (rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_VYOS_CMD, spec.getVmInventory().getUuid(), Boolean.class)) {
            info.put(VyosConstants.CONFIG_ENABLE_VYOS, true);
        } else {
            info.put(VyosConstants.CONFIG_ENABLE_VYOS, false);
        }

        if (applianceVmVO.getHaStatus() != ApplianceVmHaStatus.NoHa) {
            VpcHaGroupApplianceVmRefVO ref = dbf.findByUuid(applianceVmVO.getUuid(), VpcHaGroupApplianceVmRefVO.class);
            if (rcf.getResourceConfigValue(VpcHaGroupGlobalConfig.CONFIG_FIREWALL_WITH_IPTABLES, ref.getVpcHaRouterUuid(), Boolean.class)) {
                info.put(VyosConstants.REPLACE_FIREWALL_WITH_IPTBALES, true);
            }
            if (rcf.getResourceConfigValue(VyosGlobalConfig.ENABLE_VYOS_CMD, ref.getVpcHaRouterUuid(), Boolean.class)) {
                info.put(VyosConstants.CONFIG_ENABLE_VYOS, true);
            } else {
                info.put(VyosConstants.CONFIG_ENABLE_VYOS, false);
            }
            info.put(VpcConstants.TC_FOR_VIPQOS, rcf.getResourceConfigValue(VpcHaGroupGlobalConfig.TC_FOR_VIPQOS, ref.getVpcHaRouterUuid(), Boolean.class));
        } else {
            info.put(VpcConstants.TC_FOR_VIPQOS, rcf.getResourceConfigValue(VpcGlobalConfig.TC_FOR_VIPQOS, spec.getVmInventory().getUuid(), Boolean.class));
            info.put(ApplianceVmConstant.ABNORMAL_FILE_MAX_SIZE, ApplianceVmGlobalConfig.ABNORMAL_FILE_MAX_SIZE.value(Integer.class));
        }
    }

    private void deleteVpcNicForVmCascade(String vpcUuid, List<VmNicVO> deleletedNics) {
        List<ErrorCode> errs = new ArrayList<>();
        FutureCompletion completion = new FutureCompletion(null);

        new While<>(deleletedNics).each((VmNicVO nic, WhileCompletion completion1) -> {
            if (!dbf.isExist(nic.getUuid(), VmNicVO.class)) {
                logger.debug(String.format("nic[uuid:%s] not exists, skip", nic.getUuid()));
                completion1.done();
                return;
            }
            
            DetachNicFromVmMsg msg = new DetachNicFromVmMsg();
            msg.setVmNicUuid(nic.getUuid());
            msg.setVmInstanceUuid(nic.getVmInstanceUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, nic.getVmInstanceUuid());
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        if (!dbf.isExist(nic.getUuid(), VmNicVO.class)) {
                            logger.info(String.format("nic[uuid:%s] not exists, mark it as success", nic.getUuid()));
                        } else {
                            errs.add(reply.getError());
                            logger.error(String.format("detach nic[uuid: %s] for " +
                                    "delete l3[uuid: %s] failed", nic.getUuid(), nic.getL3NetworkUuid()));
                        }
                    } else {
                        logger.debug(String.format("detach nic[uuid: %s] for " +
                                "delete l3[uuid: %s] success", nic.getUuid(), nic.getL3NetworkUuid()));
                    }
                    completion1.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errs.isEmpty()) {
                    completion.fail(errs.get(0));
                } else {
                    completion.success();
                }
            }
        });

        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("can not detach nic from vpc vr[uuid:%s]",
                    vpcUuid).causedBy(completion.getErrorCode()));
        }
    }

    boolean updateNicIp(VmNicVO nic, List<String> parentIssuerUuids) {
        /* update nic ip */
        UsedIpVO ip4 = null, ip6 = null;
        for (UsedIpVO ip : nic.getUsedIps()) {
            if (ip.getIpVersion() == IPv6Constants.IPv6) {
                ip6 = ip;
            }
            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                ip4 = ip;
            }
        }

        boolean deleteIp4 = false, hasIp6 = false;
        if (ip4 != null && parentIssuerUuids.contains(ip4.getIpRangeUuid())) {
            deleteIp4 = true;
        }
        if (ip6 != null && !parentIssuerUuids.contains(ip6.getIpRangeUuid())) {
            hasIp6 = true;
        }

        /* ipv4 is deleted and ipv6 is not deleted, move ipv6 address to nic ip */
        if (deleteIp4 && hasIp6) {
            SQL.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.getUuid())
                    .set(VmNicVO_.ip, ip6.getIp()).set(VmNicVO_.gateway, ip6.getGateway()).update();
        }

        return true;
    }

    @Override
    public List<ApplianceVmVO> filterApplianceVmCascade(List<ApplianceVmVO> vmVOS, CascadeAction action,
                                                        String parentIssuer, List<String> parentIssuerUuids,
                                                        List<VmNicInventory> toDeleteNics,
                                                        List<UsedIpInventory> toDeleteIps) {
        logger.debug(String.format("filter appliance vm type %s for [%s], with parentIssuer [type: %s, uuids: %s]",
                applianceVmType, serializeAppVms(vmVOS), parentIssuer, parentIssuerUuids));
        List<ApplianceVmVO> vpcs = vmVOS.stream()
                .filter(apvmInv -> apvmInv.getApplianceVmType().equals(VpcConstants.VPC_VROUTER_VM_TYPE))
                .collect(Collectors.toList());
        if (vpcs.isEmpty()) {
            logger.debug("there is no vpc to be deleted");
            return vmVOS;
        }

        List<ApplianceVmVO> notDeleteVpcs = new ArrayList<>();
        if (parentIssuer.equals(L3NetworkVO.class.getSimpleName())
                || (parentIssuer.equals(L2NetworkVO.class.getSimpleName()) && action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE))) {
            List<String> processedUuids = new ArrayList<>();
            for (ApplianceVmVO vpc : vpcs) {
                if (!dbf.isExist(vpc.getUuid(), VirtualRouterVmVO.class) || processedUuids.contains(vpc.getUuid())) {
                    continue;
                }
                processedUuids.add(vpc.getUuid());

                List<VmNicVO> vpcNicsInParent = vpc.getVmNics().stream()
                        .filter(nic -> parentIssuerUuids.contains(nic.getL3NetworkUuid()))
                        .collect(Collectors.toList());
                if (vpcNicsInParent.isEmpty()) {
                    continue;
                }

                /* if delete l3 is not mgt, public network, default route network, only delete nic, will not delete vpc router */
                boolean deleteVpc = false;
                for (VmNicVO nic : vpcNicsInParent) {
                    if (VirtualRouterNicMetaData.isPublicNic(nic) || VirtualRouterNicMetaData.isManagementNic(nic)) {
                        deleteVpc = true;
                        break;
                    }
                    if (nic.getL3NetworkUuid().equals(vpc.getDefaultRouteL3NetworkUuid())) {
                        deleteVpc = true;
                        break;
                    }
                }

                if (!deleteVpc) {
                    logger.debug(String.format("parent issuers all are guest l3 networks[%s], skip to delete.", vpcNicsInParent));
                    toDeleteNics.addAll(VmNicInventory.valueOf(vpcNicsInParent));
                    notDeleteVpcs.add(vpc);
                }
            }
        } else if (parentIssuer.equals(IpRangeVO.class.getSimpleName())) {
            List<String> processedUuids = new ArrayList<>();
            for (ApplianceVmVO vpc : vpcs) {
                if (processedUuids.contains(vpc.getUuid())) {
                    continue;
                }
                processedUuids.add(vpc.getUuid());

                boolean toBeDelete = false;
                List<VmNicVO> deleletedNics = new ArrayList<>();
                List<UsedIpVO> deleletedIps = new ArrayList<>();
                List<UsedIpVO> updatedIps = new ArrayList<>();
                for (VmNicVO nic : vpc.getVmNics()) {
                    /* if there is no ip for mgt nic, vpc can not be managed, delete it
                      if there is no ip for public nic, vpc can not route packet, delete it
                      if there is no ip for default route nic and not delete vpc, it will break fkApplianceVmVOL3NetworkEO1
                     * */
                    if (VirtualRouterNicMetaData.isManagementNic(nic)
                            || VirtualRouterNicMetaData.isPublicNic(nic)
                            || nic.getL3NetworkUuid().equals(vpc.getDefaultRouteL3NetworkUuid())) {
                        if (nic.getUsedIps().stream().allMatch(ip -> parentIssuerUuids.contains(ip.getIpRangeUuid()))) {
                            toBeDelete = true;
                            break;
                        }
                        /* delete ip if nic ip is in the range to be deleted */
                        for (UsedIpVO ip : nic.getUsedIps()) {
                            if (parentIssuerUuids.contains(ip.getIpRangeUuid())) {
                                VipVO vip = Q.New(VipVO.class)
                                        .eq(VipVO_.ipRangeUuid, ip.getIpRangeUuid()).eq(VipVO_.ip, ip.getIp())
                                        .eq(VipVO_.system, Boolean.TRUE).find();
                                if (vip == null) {
                                    deleletedIps.add(ip);
                                }
                            }
                        }
                        updateNicIp(nic, parentIssuerUuids);
                    } else if (VirtualRouterNicMetaData.isAddinitionalPublicNic(nic)) {
                        /* additional public nic: detach nic if and only if all ipranges of nic ip are deleted */
                        if (nic.getUsedIps().stream().allMatch(ip -> parentIssuerUuids.contains(ip.getIpRangeUuid()))) {
                            deleletedNics.add(nic);
                        } else {
                            for (UsedIpVO ip : nic.getUsedIps()) {
                                if (parentIssuerUuids.contains(ip.getIpRangeUuid())) {
                                    VipVO vip = Q.New(VipVO.class)
                                            .eq(VipVO_.ipRangeUuid, ip.getIpRangeUuid()).eq(VipVO_.ip, ip.getIp())
                                            .eq(VipVO_.system, Boolean.TRUE).find();
                                    if (vip == null) {
                                        deleletedIps.add(ip);
                                    }
                                }
                            }
                            updateNicIp(nic, parentIssuerUuids);
                        }
                    } else {
                        /* guest nic: guest nic ip(gw) is not in any ip range,
                            detach nic if and only if there is no normal ip range in l3 network */
                        List<String> currentIp4Uuids = Q.New(NormalIpRangeVO.class).select(NormalIpRangeVO_.uuid)
                                .eq(NormalIpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv4).listValues();
                        List<String> currentIp6Uuids = Q.New(NormalIpRangeVO.class).select(NormalIpRangeVO_.uuid)
                                .eq(NormalIpRangeVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                                .eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6).listValues();
                        UsedIpVO ip4 = null, ip6 = null;
                        for (UsedIpVO ip : nic.getUsedIps()) {
                            if (ip.getIpVersion() == IPv6Constants.IPv4) {
                                ip4 = ip;
                            } else if (ip.getIpVersion() == IPv6Constants.IPv6) {
                                ip6 = ip;
                            }
                        }
                        HashSet<String> ipRangeUuidSet = new HashSet<>(parentIssuerUuids);
                        /* both ipv4 and ipv6 range are deleted at same time, delete nic */
                        if ((currentIp4Uuids.isEmpty() || ipRangeUuidSet.containsAll(currentIp4Uuids))
                                && (currentIp6Uuids.isEmpty() || ipRangeUuidSet.containsAll(currentIp6Uuids))) {
                            deleletedNics.add(nic);
                        } else {
                            if (ip4 != null && ipRangeUuidSet.contains(ip4.getIpRangeUuid())) {
                                /* nic is not deleted, but delete ip address */
                                if (ipRangeUuidSet.containsAll(currentIp4Uuids)) {
                                    deleletedIps.add(ip4);
                                } else {
                                    currentIp4Uuids.remove(ip4.getIpRangeUuid());
                                    ip4.setIpRangeUuid(currentIp4Uuids.get(0));
                                    updatedIps.add(ip4);
                                }
                            }
                            if (ip6 != null && ipRangeUuidSet.contains(ip6.getIpRangeUuid())) {
                                /* nic is not deleted, but delete ip address */
                                if (ipRangeUuidSet.containsAll(currentIp6Uuids)) {
                                    deleletedIps.add(ip6);
                                } else {
                                    currentIp6Uuids.remove(ip6.getIpRangeUuid());
                                    ip6.setIpRangeUuid(currentIp4Uuids.get(0));
                                    updatedIps.add(ip6);
                                }
                            }
                        }
                    }
                }

                if (!updatedIps.isEmpty()) {
                    dbf.updateCollection(updatedIps);
                }

                if (!deleletedNics.isEmpty()) {
                    toDeleteNics.addAll(VmNicInventory.valueOf(deleletedNics));
                }

                if (!deleletedIps.isEmpty()) {
                    toDeleteIps.addAll(UsedIpInventory.valueOf(deleletedIps));
                }

                if (!toBeDelete) {
                    notDeleteVpcs.add(vpc);
                }
            }
        }

        vmVOS.removeAll(notDeleteVpcs);
        logger.debug(String.format("need to delete vms after filter: %s", serializeAppVms(vmVOS)));
        return vmVOS;
    }

    private String serializeAppVms(List<ApplianceVmVO> vmVOS) {
        StringBuilder result = new StringBuilder();
        for (ApplianceVmVO vo : vmVOS) {
            result.append(String.format("[uuid: %s, type: %s] ", vo.getUuid(), vo.getApplianceVmType()));
        }
        return result.toString();
    }

    @Override
    public void afterCreateL2Network(L2NetworkInventory l2Network) {
    }

    @Override
    public ApplianceVm getSubApplianceVm(ApplianceVmVO apvm) {
        VirtualRouterVmVO vr = dbf.findByUuid(apvm.getUuid(), VirtualRouterVmVO.class);
        return new VpcVyosVm(vr);
    }

    @Override
    public void preDeleteIpRange(IpRangeInventory ipRange) {

    }

    @Override
    public void beforeDeleteIpRange(IpRangeInventory ipRange) {}

    @Override
    public void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode) {}

    @Override
    public void afterDeleteIpRange(IpRangeInventory ipRange) {}

    @Override
    public void vmPreAttachL3Network(VmInstanceInventory vm, L3NetworkInventory l3){
        boolean isVpcNetwork = VpcConstants.VPC_L3_NETWORK_TYPE.equals(l3.getType());
        if (!vm.getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)){
            return;
        } else {
            VirtualRouterVmVO virtualRouterVmVO = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vm.getUuid()).find();
            boolean isVpcVrouterVm = VpcConstants.VPC_VROUTER_VM_TYPE.equals(virtualRouterVmVO.getApplianceVmType());
            if (!isVpcNetwork && !isVpcVrouterVm) {
                return;
            }
        }

        ErrorCode err = checkIpRangeOfL3(l3);
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }

        err = checkNicGatewayIpOccupied(vm, l3, true);
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<L3NetworkInventory> filterAttachableL3Network(VmInstanceInventory vm, List<L3NetworkInventory> l3s) {
        // find attachable vpc L3 network
        Set<String> l3Uuids = SQL.New("select l3.uuid from L3NetworkVO l3 " +
                        "left join VmNicVO nic on l3.uuid = nic.l3NetworkUuid " +
                        "left join ApplianceVmVO vm on nic.vmInstanceUuid = vm.uuid " +
                        "where vm.applianceVmType = :applianceVmType")
                .param("applianceVmType", VpcConstants.VPC_VROUTER_VM_TYPE)
                .list()
                .stream().map(Objects::toString).collect(Collectors.toSet());
    
        List<L3NetworkInventory> rets = new ArrayList<>(l3s);
        ErrorCode err;

        for (Iterator<L3NetworkInventory> it = rets.iterator(); it.hasNext();) {
            L3NetworkInventory l3 = it.next();

            if (!VpcConstants.VPC_L3_NETWORK_TYPE.equals(l3.getType())) {
                continue; // ignore other type
            }

            if (!l3Uuids.contains(l3.getUuid())) {
                it.remove();
                continue;
            }

            err = checkIpRangeOfL3(l3);
            if (err != null) {
                it.remove();
                continue;
            }

            err = checkNicGatewayIpOccupied(vm, l3, false);
            if (err != null) {
                it.remove();
            }
        }
        return rets;
    }

    private ErrorCode checkIpRangeOfL3(L3NetworkInventory l3) {
        if (IpRangeHelper.getNormalIpRanges(l3).isEmpty()) {
            return operr("there is no ip range for l3 network[uuid:%s]", l3.getUuid());
        }
        return null;
    }

    private ErrorCode checkNicGatewayIpOccupied(VmInstanceInventory vm, L3NetworkInventory l3, boolean needRefreshNics) {
        List<IpRangeInventory> iprs = IpRangeHelper.getNormalIpRanges(l3);
    
        String gateway = null;
        String gateway6 = null;
        List<IpRangeInventory> iprs4 = iprs.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        if (!iprs4.isEmpty()) {
            gateway = iprs4.get(0).getGateway();
        }

        List<IpRangeInventory> iprs6 = iprs.stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
        if (!iprs6.isEmpty()) {
            gateway6 = iprs6.get(0).getGateway();
        }

        List<VmNicInventory> nics = (needRefreshNics) ?
                VmNicInventory.valueOf(Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vm.getUuid()).list()) :
                vm.getVmNics();
        for (VmNicInventory nic : nics) {
            List<UsedIpInventory> usedIps = nic.getUsedIps();
            if (!usedIps.isEmpty()) {
                for (UsedIpInventory ip : usedIps) {
                    if (ip.getGateway() != null && (ip.getGateway().equals(gateway) || ip.getGateway().equals(gateway6))) {
                        return operr("the gateway[ip:%s] of l3[uuid:%s] has been occupied on vpc vr[uuid: %s]",
                            ip.getGateway(), l3.getUuid(), vm.getUuid());
                    }
                }
            }
        }

        return null;
    }

    @Override
    @Transactional
    public ApplianceVmVO persistApplianceVm(ApplianceVmSpec spec, ApplianceVmVO apvm) {
        VirtualRouterVmVO vr = new VirtualRouterVmVO(apvm);
        VirtualRouterOfferingInventory offering = (VirtualRouterOfferingInventory) spec.getInstanceOffering();
        vr.setPublicNetworkUuid(offering.getPublicNetworkUuid());
        VpcRouterVmVO vpcVr = new VpcRouterVmVO(vr);
        dbf.getEntityManager().persist(vpcVr);
        return vpcVr;
    }

    @Override
    @Transactional
    public void removeApplianceVm(ApplianceVmSpec spec, ApplianceVmVO apvm) {
        dbf.removeByPrimaryKey(apvm.getUuid(), VpcRouterVmVO.class);
    }

}
