package org.zstack.vpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.Platform;
import org.zstack.core.db.*;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vpc.VpcConstants;
import org.zstack.header.vpc.VpcRouterDnsVO;
import org.zstack.header.vpc.VpcSnatStateVO;
import org.zstack.header.vpc.VpcSnatStateVO_;
import org.zstack.header.vpc.ha.*;
import org.zstack.ipsec.IPSecStatus;
import org.zstack.ipsec.IPsecConnectionVO;
import org.zstack.ipsec.IpSecconnectionSystemTags;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.eip.EipVO_;
import org.zstack.network.service.flat.FlatNetworkServiceConstant;
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO;
import org.zstack.network.service.lb.LoadBalancerListenerVmNicRefVO_;
import org.zstack.network.service.lb.LoadBalancerVmNicStatus;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_;
import org.zstack.network.service.userdata.UserdataConstant;
import org.zstack.network.service.vip.*;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.network.service.virtualrouter.eip.EipConfigProxy;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO_;
import org.zstack.network.service.virtualrouter.portforwarding.PortForwardingConfigProxy;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.VipUseForList;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class VpcUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(VpcUpgradeExtension.class);
    @Autowired
    EipConfigProxy eipProxy;
    @Autowired
    VipConfigProxy vipProxy;
    @Autowired
    PortForwardingConfigProxy pfProxy;
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    protected TagManager tagMgr;

    private void upgradeEip() {
        List<VipPeerL3NetworkRefVO> peerL3NetworkRefVOS = new ArrayList<>();
        List<EipVO> eipVos = Q.New(EipVO.class).notNull(EipVO_.vmNicUuid).list();
        for (EipVO eipVo : eipVos) {
            VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, eipVo.getVmNicUuid()).find();
            /* only handle eip in vpc network */
            L3NetworkVO guestL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).find();
            if (!guestL3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)) {
                continue;
            }

            SQL.New(VipVO.class).eq(VipVO_.uuid, eipVo.getVipUuid()).set(VipVO_.serviceProvider, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE).update();

            if (!Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, eipVo.getVipUuid())
                    .eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, guestL3.getUuid()).isExists()) {
                VipPeerL3NetworkRefVO ref = new VipPeerL3NetworkRefVO();
                ref.setVipUuid(eipVo.getVipUuid());
                ref.setL3NetworkUuid(guestL3.getUuid());
                peerL3NetworkRefVOS.add(ref);
            }

            /* skip if eip guest nic has not attached to vpc router */
            String vrUuid = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid).eq(VmNicVO_.l3NetworkUuid, guestL3.getUuid())
                    .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).limit(1).findValue();
            if (vrUuid == null) {
                continue;
            }

            /* skip if  eip already attached to virtual router */
            List<String> eipsAttachedToVr = eipProxy.getServiceUuidsByRouterUuid(vrUuid, EipVO.class.getSimpleName());
            if (eipsAttachedToVr == null || eipsAttachedToVr.contains(eipVo.getUuid())) {
                continue;
            }

            /* attach eip to virtual router */
            eipProxy.attachNetworkService(vrUuid, EipVO.class.getSimpleName(), asList(eipVo.getUuid()));
            vipProxy.attachNetworkService(vrUuid, VipVO.class.getSimpleName(), asList(eipVo.getVipUuid()));
        }

        if (!peerL3NetworkRefVOS.isEmpty()) {
            dbf.persistCollection(peerL3NetworkRefVOS);
        }
    }

    private void upgradePf() {
        List<VipPeerL3NetworkRefVO> peerL3NetworkRefVOS = new ArrayList<>();
        List<PortForwardingRuleVO> pfVos = Q.New(PortForwardingRuleVO.class).notNull(PortForwardingRuleVO_.vmNicUuid).list();
        for (PortForwardingRuleVO pfVo : pfVos) {
            VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, pfVo.getVmNicUuid()).find();
            /* only handle eip in vpc network */
            L3NetworkVO guestL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).find();
            if (!guestL3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)) {
                continue;
            }

            SQL.New(VipVO.class).eq(VipVO_.uuid, pfVo.getVipUuid()).set(VipVO_.serviceProvider, VyosConstants.VYOS_ROUTER_PROVIDER_TYPE).update();

            if (!Q.New(VipPeerL3NetworkRefVO.class).eq(VipPeerL3NetworkRefVO_.vipUuid, pfVo.getVipUuid())
                    .eq(VipPeerL3NetworkRefVO_.l3NetworkUuid, guestL3.getUuid()).isExists()) {
                VipPeerL3NetworkRefVO ref = new VipPeerL3NetworkRefVO();
                ref.setVipUuid(pfVo.getVipUuid());
                ref.setL3NetworkUuid(guestL3.getUuid());
                peerL3NetworkRefVOS.add(ref);
            }

            /* skip if eip guest nic has not attached to vpc router */
            String vrUuid = Q.New(VmNicVO.class).select(VmNicVO_.vmInstanceUuid).eq(VmNicVO_.l3NetworkUuid, guestL3.getUuid())
                    .in(VmNicVO_.metaData, VirtualRouterNicMetaData.GUEST_NIC_MASK_STRING_LIST).limit(1).findValue();
            if (vrUuid == null) {
                continue;
            }

            /* skip if pf already attached to virtual router */
            List<String> pfsAttachedToVr = pfProxy.getServiceUuidsByRouterUuid(vrUuid, PortForwardingRuleVO.class.getSimpleName());
            if (pfsAttachedToVr == null || pfsAttachedToVr.contains(pfVo.getUuid())) {
                continue;
            }

            pfProxy.attachNetworkService(vrUuid, PortForwardingRuleVO.class.getSimpleName(), asList(pfVo.getUuid()));
            vipProxy.attachNetworkService(vrUuid, VipVO.class.getSimpleName(), asList(pfVo.getVipUuid()));
        }

        if (!peerL3NetworkRefVOS.isEmpty()) {
            dbf.persistCollection(peerL3NetworkRefVOS);
        }
    }

    private void upgradeLb() {
        List<LoadBalancerListenerVmNicRefVO> refs = Q.New(LoadBalancerListenerVmNicRefVO.class).notNull(LoadBalancerListenerVmNicRefVO_.vmNicUuid).list();
        for (LoadBalancerListenerVmNicRefVO ref : refs) {
            VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, ref.getVmNicUuid()).find();
            /* only handle eip in vpc network */
            L3NetworkVO guestL3 = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, nic.getL3NetworkUuid()).find();
            if (!guestL3.getType().equals(VpcConstants.VPC_L3_NETWORK_TYPE)) {
                continue;
            }

            SQL.New(LoadBalancerListenerVmNicRefVO.class).eq(LoadBalancerListenerVmNicRefVO_.id, ref.getId())
                    .set(LoadBalancerListenerVmNicRefVO_.status, LoadBalancerVmNicStatus.Active).update();
        }
    }

    private void upgradeL3Network() {
        List<NetworkServiceL3NetworkRefVO> refs = new ArrayList<>();
        NetworkServiceProviderVO nspVo = Q.New(NetworkServiceProviderVO.class)
                .eq(NetworkServiceProviderVO_.type, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING).find();
        List<L3NetworkVO> l3Vos = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.ipVersion, IPv6Constants.IPv6)
                .eq(L3NetworkVO_.type, L3NetworkConstant.L3_BASIC_NETWORK_TYPE).list();
        for (L3NetworkVO l3Vo : l3Vos) {
            List<String> services = l3Vo.getNetworkServices().stream().map(NetworkServiceL3NetworkRefVO::getNetworkServiceType).collect(Collectors.toList());
            if (!services.contains(UserdataConstant.USERDATA_TYPE_STRING)) {
                NetworkServiceL3NetworkRefVO ref = new NetworkServiceL3NetworkRefVO();
                ref.setL3NetworkUuid(l3Vo.getUuid());
                ref.setNetworkServiceProviderUuid(nspVo.getUuid());
                ref.setNetworkServiceType(UserdataConstant.USERDATA_TYPE_STRING);
                refs.add(ref);
            }
            if (l3Vo.getCategory().equals(L3NetworkCategory.Private)) {
                if (!services.contains(NetworkServiceType.HostRoute.toString())) {
                    NetworkServiceL3NetworkRefVO ref = new NetworkServiceL3NetworkRefVO();
                    ref.setL3NetworkUuid(l3Vo.getUuid());
                    ref.setNetworkServiceProviderUuid(nspVo.getUuid());
                    ref.setNetworkServiceType(NetworkServiceType.HostRoute.toString());
                    refs.add(ref);
                }
            }
        }
        if (!refs.isEmpty()) {
            dbf.persistCollection(refs);
        }
    }

    @Transactional
    private void upgradeVrToVpc() {
        /*
        * 1.find l3
        * 2.upgrade l3 type to VpcNetwork
        * 3.delete dns networkservice ref
        * 4.find virtual router by l3
        * 5.update vr to vpc and add dns
        * 6.add vpc vm
        * */

        logger.debug("start to upgrade vr to vpc");

        List<L3NetworkVO> l3s = SQL.New("SELECT l3" +
                " FROM L3NetworkVO l3 LEFT JOIN NetworkServiceL3NetworkRefVO ref on l3.uuid = ref.l3NetworkUuid" +
                " where l3.category = 'Private' AND l3.type = 'L3BasicNetwork' AND ref.networkServiceType = 'SNAT'").list();

        //1.find l3
        if (l3s.isEmpty()) {
            logger.debug("no vr network will upgrade to vpc network");
            return;
        }
        logger.debug(String.format("%d vr network will upgrade to vpc network", l3s.size()));

        for (L3NetworkVO l3 : l3s) {
            //2.upgrade l3 type to VpcNetwork
            l3.setType(VpcConstants.VPC_L3_NETWORK_TYPE);
            dbf.updateAndRefresh(l3);

            //3.delete dns networkservice ref
            NetworkServiceL3NetworkRefVO networkServiceL3NetworkRefVO = Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.getUuid())
                    .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, "DNS").find();
            if (networkServiceL3NetworkRefVO != null) {
                dbf.remove(networkServiceL3NetworkRefVO);
            }

            //4.find virtual router by l3
            ApplianceVmVO vm = SQL.New("SELECT vm from ApplianceVmVO vm, VmNicVO nic" +
                    " where nic.l3NetworkUuid = :l3Uuid and nic.metaData in ('4', '5', '6', '7')" +
                    " and vm.uuid = nic.vmInstanceUuid")
                    .param("l3Uuid", l3.getUuid())
                    .find();

            if (vm == null) {
                SQL.New("DELETE FROM L3NetworkDnsVO WHERE l3NetworkUuid = :l3Uuid")
                        .param("l3Uuid", l3.getUuid())
                        .execute();
                continue;
            }

            //5.update vr to vpc and add dns
            vm.setApplianceVmType(VpcConstants.VPC_VROUTER_VM_TYPE);
            dbf.updateAndRefresh(vm);
            //6.add vpc vm
            dbf.getEntityManager().createNativeQuery(
                    String.format("insert into VpcRouterVmVO (uuid)" +
                            "values ('%s')", vm.getUuid()))
                    .executeUpdate();


            SQL.New("UPDATE ResourceVO set resourceType='VpcRouterVmVO' where uuid  = :vrUuid")
                    .param("vrUuid", vm.getUuid())
                    .execute();

            List<L3NetworkDnsVO> l3NetworkDnsVOs = Q.New(L3NetworkDnsVO.class).eq(L3NetworkDnsVO_.l3NetworkUuid, l3.getUuid()).list();

            for (L3NetworkDnsVO l3NetworkDnsVO : l3NetworkDnsVOs) {
                VpcRouterDnsVO vpcRouterDnsVO = new VpcRouterDnsVO();
                vpcRouterDnsVO.setVpcRouterUuid(vm.getUuid());
                vpcRouterDnsVO.setDns(l3NetworkDnsVO.getDns());
                dbf.persistAndRefresh(vpcRouterDnsVO);
                dbf.remove(l3NetworkDnsVO);
            }

            List<ClusterVO> clusterVOs = SQL.New("select cluster from ClusterVO cluster, L2NetworkClusterRefVO ref " +
                    "where ref.l2NetworkUuid = :l2NetworkUuid and ref.clusterUuid=cluster.uuid " +
                    "and cluster.type = :type")
                    .param("l2NetworkUuid", l3.getL2NetworkUuid())
                    .param("type", "vmware")
                    .list();
            if (clusterVOs != null && clusterVOs.size() > 0) {
                NetworkServiceL3NetworkRefVO netServiceL3RefVO = Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, l3.getUuid())
                        .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, "SecurityGroup").find();
                if (netServiceL3RefVO != null) {
                    dbf.remove(netServiceL3RefVO);
                }
            }
        }

        logger.debug("end to upgrade vr to vpc");
    }

    private void addUsedIpForVpcGateway() {
        List<VmNicVO> vmNicVOS = SQL.New("select nic from L3NetworkVO l3, ApplianceVmVO vm, VmNicVO nic " +
                "where l3.type='L3VpcNetwork' and l3.uuid=nic.l3NetworkUuid and nic.vmInstanceUuid=vm.uuid and nic.usedIpUuid is null").list();

        logger.debug(String.format("%d vmnic have to add used ip for gateway", vmNicVOS.size()));
        if (vmNicVOS.isEmpty()) {
            return;
        }

        for (VmNicVO nicVO : vmNicVOS) {
            UsedIpVO vo = new UsedIpVO();
            vo.setUuid(Platform.getUuid());
            List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).eq(IpRangeVO_.l3NetworkUuid, nicVO.getL3NetworkUuid()).list();
            if (ipRangeVOS.isEmpty()) {
                continue;
            }
            IpRangeVO ipRangeVO = ipRangeVOS.get(0);
            vo.setIp(ipRangeVO.getGateway());
            vo.setIpInLong(NetworkUtils.ipv4StringToLong(ipRangeVO.getGateway()));
            vo.setIpInBinary(NetworkUtils.ipStringToBytes(ipRangeVO.getGateway()));
            vo.setIpRangeUuid(ipRangeVO.getUuid());
            vo.setGateway(ipRangeVO.getGateway());
            vo.setNetmask(ipRangeVO.getNetmask());
            vo.setIpVersion(ipRangeVO.getIpVersion());
            vo.setVmNicUuid(nicVO.getUuid());
            vo.setL3NetworkUuid(nicVO.getL3NetworkUuid());
            dbf.persist(vo);

            nicVO.setUsedIpUuid(vo.getUuid());
            dbf.update(nicVO);
        }
    }

    private void upgradeSystemVipNetworkServiceRef() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<VipVO> systemVips = q(VipVO.class).eq(VipVO_.system, true).list();
                for (VipVO vipVO: systemVips) {
                    String uuid = q(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vipVO.getUuid())
                            .eq(VipNetworkServicesRefVO_.serviceType, VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)
                            .select(VipNetworkServicesRefVO_.uuid).findValue();
                    if (uuid == null || !uuid.equals(vipVO.getUuid())) {
                        continue;
                    }

                    String vrUuid = q(VirtualRouterVipVO.class).eq(VirtualRouterVipVO_.uuid, vipVO.getUuid())
                            .select(VirtualRouterVipVO_.virtualRouterVmUuid).findValue();
                    if (vrUuid == null) {
                        vrUuid = q(VpcHaGroupNetworkServiceRefVO.class)
                                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, vipVO.getUuid())
                                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid).findValue();
                    }

                    if (vrUuid == null) {
                        continue;
                    }

                    sql(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vipVO.getUuid())
                            .eq(VipNetworkServicesRefVO_.serviceType, VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE)
                            .set(VipNetworkServicesRefVO_.uuid, vrUuid).update();
                }
            }
        }.execute();
    }

    private void upgradeVipOwner() {
        String sql = "select v.uuid, a.accountUuid from EipVO e, VipVO v, AccountResourceRefVO a, AccountResourceRefVO b "
                    + "where e.vipUuid = v.uuid and a.resourceUuid = e.uuid and b.resourceUuid = v.uuid and (a.accountUuid != b.accountUuid or a.accountUuid != b.accountUuid)"
                    + " and a.type = 'Own' and b.type = 'Own'";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        List<Tuple> rvos = q.getResultList();
        if (rvos.isEmpty()) {
            return;
        }

        for (Tuple r : rvos) {
            String vipUuid = r.get(0, String.class);
            String eipAccountUuid = r.get(1, String.class);
            acntMgr.changeResourceOwner(vipUuid, eipAccountUuid);
        }
    }

    private void cleanUpVirtualRouterEipRefVO() {
        /* eip that attached to vpcha group, VirtualRouterEipRefVO should be cleanup */
        List<String> eipUuids = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, EipVO.class.getSimpleName())
                .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid).listValues();

        if (eipUuids.isEmpty()) {
            return;
        }

        SQL.New(VirtualRouterEipRefVO.class).in(VirtualRouterEipRefVO_.eipUuid, eipUuids).delete();
    }

    private void upgradeVpcHaL3NetworkCheck() {
        //due to ZSTAC-48197, prefer detect this problem when management node ready
        logger.debug("Start virtual router ha group upgrade detection : ");
        List<String> vpcHaGroups = Q.New(VpcHaGroupVO.class)
                .select(VpcHaGroupVO_.uuid)
                .listValues();
        vpcHaGroups.forEach(vpcHaGroup -> {
            String defaultNetworkUuid = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                    .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VirtualRouterConstant.VR_DEFAULT_ROUTE_NETWORK)
                    .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaGroup)
                    .findValue();
            if (Q.New(VpcHaGroupNetworkServiceRefVO.class)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VipVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaGroup)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid, defaultNetworkUuid).isExists()) {
                logger.warn(String.format("Result: There is an error virtual router ha group [uuid:%s]: multi snat upgrade problem",
                        vpcHaGroup));
            } else {
                logger.debug(String.format("Result: There is an virtual router ha group [uuid:%s]: no multi snat upgrade problem",
                        vpcHaGroup));
            }

            //due to ZSTAC-25985, prefer auto repair VpcHaGroupNetworkServiceRefVO L3NetworkVO record
            List<VpcHaGroupNetworkServiceRefVO> vos = new ArrayList<>();
            List<String> vpcs = Q.New(VpcHaGroupApplianceVmRefVO.class)
                    .select(VpcHaGroupApplianceVmRefVO_.uuid)
                    .eq(VpcHaGroupApplianceVmRefVO_.vpcHaRouterUuid, vpcHaGroup)
                    .limit(2).listValues();

            String vpcMasterUuid, vpcBackupUuid = new String();
            if (vpcs.size() == 1) {
                vpcMasterUuid = vpcs.get(0);
            } else if (vpcs.size() == 2) {
                vpcMasterUuid = vpcs.get(0);
                vpcBackupUuid = vpcs.get(1);
            } else {
                return;
            }
            List<String> vpc_1_nicl3s = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, vpcMasterUuid).orderBy(VmNicVO_.l3NetworkUuid, SimpleQuery.Od.ASC).listValues();
            List<String> vpc_2_nicl3s = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, vpcBackupUuid).orderBy(VmNicVO_.l3NetworkUuid, SimpleQuery.Od.ASC).listValues();
            if (vpcs.size() != 1 && !vpc_1_nicl3s.equals(vpc_2_nicl3s)) {
                logger.warn(String.format("Result: There is an error virtual router ha group [uuid:%s]: " +
                                "nic of vpc [uuid:%s] is different from nic of vpc [uuid:%s]",
                        vpcHaGroup, vpcMasterUuid, vpcBackupUuid));
                return;
            }

            List<String> vpc_nicl3s = (vpc_1_nicl3s.size()>vpc_2_nicl3s.size()) ? new ArrayList<>(vpc_1_nicl3s) : new ArrayList<>(vpc_2_nicl3s);
            String vpcUuid = (vpc_1_nicl3s.size()>vpc_2_nicl3s.size()) ? vpcMasterUuid : vpcBackupUuid;
            String pub = Q.New(VirtualRouterVmVO.class)
                    .select(VirtualRouterVmVO_.publicNetworkUuid)
                    .eq(VirtualRouterVmVO_.uuid, vpcUuid)
                    .findValue();
            vpc_nicl3s.remove(pub);
            List<String> l3_ref = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                    .select(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid)
                    .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, L3NetworkVO.class.getSimpleName())
                    .eq(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, vpcHaGroup)
                    .groupBy(VpcHaGroupNetworkServiceRefVO_.networkServiceUuid)
                    .listValues();
            vpc_nicl3s.removeAll(l3_ref);

            vpc_nicl3s.forEach(uuid -> {
                VmNicVO l3vo = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, uuid).eq(VmNicVO_.vmInstanceUuid, vpcUuid).limit(1).find();
                if (l3vo != null && !VirtualRouterNicMetaData.isManagementNic(l3vo)) {
                    logger.warn(String.format("Result: Fix an error virtual router ha group [uuid:%s]: " +
                                    "l3 network [uuid:%s] of vpc [uuid:%s] is not in vpc ha group record",
                            vpcHaGroup, uuid, vpcUuid));
                    VpcHaGroupNetworkServiceRefVO vo = new VpcHaGroupNetworkServiceRefVO();
                    vo.setVpcHaRouterUuid(vpcHaGroup);
                    vo.setNetworkServiceName(L3NetworkVO.class.getSimpleName());
                    vo.setNetworkServiceUuid(uuid);
                    vos.add(vo);
                }
            });
            //Repair if ha networkService record are difference
            if(!vos.isEmpty()) {
                dbf.persistCollection(vos);
            }
        });
    }

    private void upgradeVpcIpsecVersionCheck() {
        // use sublist due to VirtualRouterSoftwareVersionVO doesn`t exist when mn version < 4.4.24
        List<String> vpcOldIpsecList = Q.New(ApplianceVmVO.class)
                .eq(ApplianceVmVO_.applianceVmType, VpcConstants.VPC_VROUTER_VM_TYPE)
                .select(ApplianceVmVO_.uuid)
                .listValues();
        List<String> vpcNewIpsecList = Q.New(VirtualRouterSoftwareVersionVO.class)
                .select(VirtualRouterSoftwareVersionVO_.uuid)
                .eq(VirtualRouterSoftwareVersionVO_.softwareName, VirtualRouterConstant.IPSEC_NETWORK_SERVICE_TYPE)
                .eq(VirtualRouterSoftwareVersionVO_.currentVersion, VirtualRouterConstant.IPSEC_NETWORK_SERVICE_LATEST_VERSION)
                .listValues();
        vpcOldIpsecList.removeAll(vpcNewIpsecList);
        if (vpcOldIpsecList.isEmpty()) {
            return;
        }
        List<IPsecConnectionVO> allIpsecList = new ArrayList<>();
        vpcOldIpsecList.forEach( vpcUuid -> {
            VirtualRouterVmVO vr = dbf.findByUuid(vpcUuid, VirtualRouterVmVO.class);
            VirtualRouterVmInventory vrInv = VirtualRouterVmInventory.valueOf(vr);
            List<String> l3Uuids = vrInv.getGuestL3Networks();
            if (l3Uuids.isEmpty()) {
                return;
            }
            List<IPsecConnectionVO> vpcIpsecList = SQL.New("select distinct ipsec from IPsecConnectionVO ipsec, IPsecL3NetworkRefVO ref " +
                            "where ipsec.uuid=ref.connectionUuid and ref.l3NetworkUuid in (:l3Uuids)", IPsecConnectionVO.class)
                    .param("l3Uuids", l3Uuids).list();
            vpcIpsecList.forEach(ipsec -> {
                if (IpSecconnectionSystemTags.IPSEC_LOW_VERSION.getTag(ipsec.getUuid(), IPsecConnectionVO.class) == null) {
                    //vpcHa only need tag once
                    ipsec.setStatus(IPSecStatus.Ready); // old ipsec status always Ready due to it doesn`t support monitor state when change
                    allIpsecList.add(ipsec);
                    tagMgr.createInherentSystemTag(ipsec.getUuid(), IpSecconnectionSystemTags.IPSEC_LOW_VERSION.getTagFormat(), IPsecConnectionVO.class.getSimpleName());
                }
            });
        });
        dbf.updateCollection(allIpsecList);
    }

    //this upgrade will fix snat VipNetworkServiceRef record which error or missing
    private void upgradeVipSnatNetworkServiceRefRecord() {
        logger.debug("Start vip snat network service ref fix : ");
        List<VipNetworkServicesRefVO> vipRefResultList = new ArrayList<>();

        //record normal vpc snat ref
        List<Tuple> vpcEnableSnatList = Q.New(VpcSnatStateVO.class)
                .select(VpcSnatStateVO_.vpcUuid, VpcSnatStateVO_.l3NetworkUuid)
                .eq(VpcSnatStateVO_.state, VpcStateEvent.enable.toString()).listTuple();
        vpcEnableSnatList.forEach( t -> {
            String vpcUuid = t.get(0, String.class);
            String vpcEnableSnatL3Network = t.get(1, String.class);
            VmNicVO nic = Q.New(VmNicVO.class)
                    .eq(VmNicVO_.vmInstanceUuid, vpcUuid)
                    .eq(VmNicVO_.l3NetworkUuid, vpcEnableSnatL3Network).limit(1).find();
            VipVO vip = Q.New(VipVO.class).eq(VipVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                    .eq(VipVO_.ip, nic.getIp())
                    .eq(VipVO_.system, Boolean.TRUE).limit(1).find();
            makeVipSnatNetworkServicesRef(vipRefResultList, vpcUuid, vip);
        });

        //record ha group vpc snat ref
        List<Tuple> vpcHaGroupEnableSnatList = Q.New(VpcHaGroupNetworkServiceRefVO.class)
                .select(VpcHaGroupNetworkServiceRefVO_.vpcHaRouterUuid, VpcHaGroupNetworkServiceRefVO_.networkServiceUuid)
                .eq(VpcHaGroupNetworkServiceRefVO_.networkServiceName, VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE).listTuple();

        vpcHaGroupEnableSnatList.stream().forEach(t -> {
            String vpcHaUuid = t.get(0, String.class);
            String vpcHaEnableSnatL3Network = t.get(1, String.class);
            List<String> haVpc = VpcHaGroupOperator.getVpcUuidByVpcHaRouterUuid(vpcHaUuid);
            if (haVpc == null || haVpc.isEmpty()) {
                return;
            }
            VirtualRouterVmVO haVpcVO = Q.New(VirtualRouterVmVO.class).in(VirtualRouterVmVO_.uuid, haVpc).limit(1).find();
            List<String> vpcHaVipList = new VpcHaGroupOperator().getVpcHaGroupSystemVipUuids(vpcHaUuid);
            if (vpcHaEnableSnatL3Network.equals(haVpcVO.getPublicNetworkUuid()) && vpcHaEnableSnatL3Network.equals(haVpcVO.getManagementNetworkUuid())) {
                List<String> ips = Q.New(VmNicVO.class).select(VmNicVO_.ip).in(VmNicVO_.vmInstanceUuid, haVpc).eq(VmNicVO_.l3NetworkUuid, vpcHaEnableSnatL3Network).listValues();
                // ha origin default pub l3 vip is different from nic ip
                if (ips.isEmpty() || vpcHaVipList.isEmpty()) {
                    return;
                }
                VipVO pubVip = Q.New(VipVO.class).notIn(VipVO_.ip, ips).in(VipVO_.uuid, vpcHaVipList).eq(VipVO_.l3NetworkUuid, vpcHaEnableSnatL3Network).eq(VipVO_.system, Boolean.TRUE).limit(1).find();
                makeVipSnatNetworkServicesRef(vipRefResultList, vpcHaUuid, pubVip);
            } else {
                List<String> ips = Q.New(VmNicVO.class).select(VmNicVO_.ip).in(VmNicVO_.vmInstanceUuid, haVpc).eq(VmNicVO_.l3NetworkUuid, vpcHaEnableSnatL3Network).listValues();
                // ha additional pub l3 vip same with nic ip
                if (ips.isEmpty() || vpcHaVipList.isEmpty()) {
                    return;
                }
                VipVO pubVip = Q.New(VipVO.class).in(VipVO_.ip, ips).in(VipVO_.uuid, vpcHaVipList).eq(VipVO_.l3NetworkUuid, vpcHaEnableSnatL3Network).eq(VipVO_.system, Boolean.TRUE).limit(1).find();
                makeVipSnatNetworkServicesRef(vipRefResultList, vpcHaUuid, pubVip);
            }
        });
        new SQLBatch() {
            @Override
            protected void scripts() {
                //clean vpc and ha group snat ref
                List<VipNetworkServicesRefVO> oldList = Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.serviceType, VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE).list();
                dbf.removeCollection(oldList, VipNetworkServicesRefVO.class);
                //persist vpc and ha group snat ref
                dbf.persistCollection(vipRefResultList);
            }
        }.execute();
    }

    private void makeVipSnatNetworkServicesRef(List<VipNetworkServicesRefVO> vipRefResultList, String vpc, VipVO vip) {
        if (vipRefResultList == null || vpc == null || vip == null) {
            return;
        }
        VipNetworkServicesRefVO vipRef = new VipNetworkServicesRefVO();
        vipRef.setVipUuid(vip.getUuid());
        vipRef.setUuid(vpc);
        vipRef.setServiceType(VirtualRouterConstant.SNAT_NETWORK_SERVICE_TYPE);
        if (!vipRefResultList.contains(vipRef)) {
            vipRefResultList.add(vipRef);
        }
    }

    @Override
    public boolean start() {
        if (VpcGlobalProperty.UPGRADE_VPC_NETWORK_SERVICE) {
            upgradeEip();
            upgradePf();
            upgradeLb();
            upgradeL3Network();
        }
        logger.debug(String.format("upgrade vr to vpc is %s", String.valueOf(VpcGlobalProperty.UPGRADE_VR_TO_VPC)));
        if (VpcGlobalProperty.UPGRADE_VR_TO_VPC) {
            upgradeVrToVpc();
            addUsedIpForVpcGateway();
            upgradeEip();
            upgradePf();
            upgradeLb();
            upgradeL3Network();
        }

        if (VpcGlobalProperty.UPGRADE_SYSTEM_VIP_SERVICE_REF) {
            upgradeSystemVipNetworkServiceRef();
        }

        if (VpcGlobalProperty.UPGRADE_VIP_OWNER) {
            upgradeVipOwner();
        }

        cleanUpVirtualRouterEipRefVO();

        if (VpcGlobalProperty.UPGRADE_VPC_HA_L3NETWORK_CHECK) {
            upgradeVpcHaL3NetworkCheck();
        }

        if (VpcGlobalProperty.UPGRADE_VPC_IPSEC_VERSION_CHECK) {
            upgradeVpcIpsecVersionCheck();
        }

        if (VpcGlobalProperty.UPGRADE_VIP_SNAT_NETWORK_SERVICE_REF_RECORD) {
            upgradeVipSnatNetworkServiceRefRecord();
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
