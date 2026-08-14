package org.zstack.network.service.flat;

import com.googlecode.ipv6.IPv6Address;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.IpAllocatedReason;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmAbnormalLifeCycleStruct.VmAbnormalLifeCycleOperation;
import org.zstack.identity.AccountManager;
import org.zstack.kvm.*;
import org.zstack.kvm.KvmCommandSender.SteppingSendCallback;
import org.zstack.network.l3.CheckIpAddressAvailabilityExtensionPoint;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.DhcpExtension;
import org.zstack.network.service.NetworkProviderFinder;
import org.zstack.network.service.HostRouteUtils.HostRouteInfo;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.NetworkServiceProviderLookup;
import org.zstack.network.service.flat.IpStatisticConstants.VmType;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;
import static org.zstack.network.service.HostRouteUtils.getL3NetworkHostRoute;
import static org.zstack.network.service.flat.IpStatisticConstants.ResourceType;
import static org.zstack.network.service.flat.IpStatisticConstants.SortBy;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatDhcpBackend extends AbstractService implements NetworkServiceDhcpBackend, KVMHostConnectExtensionPoint,
        L3NetworkDeleteExtensionPoint, VmInstanceMigrateExtensionPoint, VmAbnormalLifeCycleExtensionPoint, IpRangeDeletionExtensionPoint,
        BeforeStartNewCreatedVmExtensionPoint, GlobalApiMessageInterceptor, AfterAddIpRangeExtensionPoint, DnsServiceExtensionPoint, CheckIpAddressAvailabilityExtensionPoint {
    private static final CLogger logger = Utils.getLogger(FlatDhcpBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DhcpExtension dhcpExtension;
    @Autowired
    private NetworkServiceManager nwServiceMgr;
    @Autowired
    protected L3NetworkManager l3NwMgr;

    private Map<String, L3NetworkGetIpStatisticExtensionPoint> getIpStatisticExts = new HashMap<>();

    public static final String APPLY_DHCP_PATH = "/flatnetworkprovider/dhcp/apply";
    public static final String BATCH_APPLY_DHCP_PATH = "/flatnetworkprovider/dhcp/batchApply";
    public static final String PREPARE_DHCP_PATH = "/flatnetworkprovider/dhcp/prepare";
    public static final String BATCH_PREPARE_DHCP_PATH = "/flatnetworkprovider/dhcp/batchPrepare";
    public static final String RELEASE_DHCP_PATH = "/flatnetworkprovider/dhcp/release";
    public static final String DHCP_CONNECT_PATH = "/flatnetworkprovider/dhcp/connect";
    public static final String RESET_DEFAULT_GATEWAY_PATH = "/flatnetworkprovider/dhcp/resetDefaultGateway";
    public static final String DHCP_DELETE_NAMESPACE_PATH = "/flatnetworkprovider/dhcp/deletenamespace";
    public static final String DHCP_FLUSH_NAMESPACE_PATH = "/flatnetworkprovider/dhcp/flush";
    public static final String ARPING_NAMESPACE_PATH = "/flatnetworkprovider/arping";

    public static String makeNamespaceName(String brName, String l3Uuid) {
        return String.format("%s_%s", brName, l3Uuid);
    }

    @Transactional(readOnly = true)
    private List<DhcpInfo> getDhcpInfoForConnectedKvmHost(HostInventory hostInv, String l3Uuid) {
        String sql = "select vm from VmInstanceVO vm where vm.hostUuid = :huuid and vm.state in (:states) and vm.type = :vtype";
        TypedQuery<VmInstanceVO> q = dbf.getEntityManager().createQuery(sql, VmInstanceVO.class);
        q.setParameter("huuid", hostInv.getUuid());
        q.setParameter("states", list(VmInstanceState.Running, VmInstanceState.Unknown, VmInstanceState.Starting,
                VmInstanceState.Rebooting, VmInstanceState.Resuming, VmInstanceState.Migrating, VmInstanceState.VolumeMigrating));
        q.setParameter("vtype", VmInstanceConstant.USER_VM_TYPE);
        List<VmInstanceVO> vmVos = q.getResultList();
        if (vmVos.isEmpty()) {
            return null;
        }

        List<String> vmUuids = vmVos.stream().map(VmInstanceVO::getUuid).collect(Collectors.toList());
        sql = "select nic.uuid from VmNicVO nic, L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider, UsedIpVO ip" +
                " where nic.uuid = ip.vmNicUuid and ip.l3NetworkUuid = l3.uuid" +
                " and ref.l3NetworkUuid = l3.uuid and ref.networkServiceProviderUuid = provider.uuid " +
                " and ref.networkServiceType = :dhcpType ";
        if (l3Uuid != null) {
            sql += " and l3.uuid = :l3Uuid ";
        }
        sql += " and provider.type = :ptype and nic.vmInstanceUuid in (:vmUuids) group by nic.uuid";

        TypedQuery<String> nq = dbf.getEntityManager().createQuery(sql, String.class);
        nq.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        nq.setParameter("dhcpType", NetworkServiceType.DHCP.toString());
        nq.setParameter("vmUuids", vmUuids);
        if (l3Uuid != null) {
            nq.setParameter("l3Uuid", l3Uuid);
        }
        List<String> nicUuids = nq.getResultList();
        if (nicUuids.isEmpty()) {
            return null;
        }

        List<DhcpInfo> dhcpInfoList = new ArrayList<DhcpInfo>();
        for (VmInstanceVO vm : vmVos) {
            List<VmNicVO> dhcpNics = vm.getVmNics().stream().filter(nic -> nicUuids.contains(nic.getUuid())).collect(Collectors.toList());

            List<VmInstanceSpec.HostName> hostNames = new ArrayList<>();
            String hostName = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
            if (hostName != null) {
                VmInstanceSpec.HostName hostNameSpec = new VmInstanceSpec.HostName();
                hostNameSpec.setL3NetworkUuid(vm.getDefaultL3NetworkUuid());
                hostNameSpec.setHostname(hostName);
                hostNames.add(hostNameSpec);
            }

            List<DhcpStruct> structs = dhcpExtension.makeDhcpStruct(VmInstanceInventory.valueOf(vm), hostNames, dhcpNics);
            dhcpInfoList.addAll(toDhcpInfo(structs, hostInv.getUuid()));
        }

        return dhcpInfoList;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetL3NetworkDhcpIpAddressMsg) {
            handle((APIGetL3NetworkDhcpIpAddressMsg) msg);
        } else if (msg instanceof APIGetL3NetworkIpStatisticMsg) {
            handle((APIGetL3NetworkIpStatisticMsg) msg);
        } else if (msg instanceof APIChangeL3NetworkDhcpIpAddressMsg) {
            handle((APIChangeL3NetworkDhcpIpAddressMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeL3NetworkDhcpIpAddressMsg msg) {
        APIChangeL3NetworkDhcpIpAddressEvent event = new APIChangeL3NetworkDhcpIpAddressEvent(msg.getId());
        L3NetworkVO l3VO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        List<IpRangeVO> ip4Ranges = l3VO.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeVO> ip6Ranges = l3VO.getIpRanges().stream().filter(
                ipr -> ipr.getIpVersion() == IPv6Constants.IPv6 && !ipr.getAddressMode().equals(IPv6Constants.SLAAC))
                .collect(Collectors.toList());

        /*
        * step #1, delete old dhcp server ip
        * step #2, allocate new dhcp server ip
        * step #3, flush new dhcp config to host
        * */
        FlowChain chain = new SimpleFlowChain();
        chain.setName(String.format("change-dhcp-server-ip-fo-l3-%s", msg.getL3NetworkUuid()));

        chain.then(new Flow() {
            String __name__ = "delete-old-dhcp-server-ip";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                Map<String, String> dhcpMap = getExistingDhcpServerIp(msg.getL3NetworkUuid(), IPv6Constants.DUAL_STACK);
                if (dhcpMap.isEmpty()) {
                    trigger.next();
                    return;
                }

                data.put("oldServerIp", dhcpMap);
                for (Map.Entry<String, String> e : dhcpMap.entrySet()) {
                    if (IPv6NetworkUtils.isValidIpv4(e.getKey()) && msg.getDhcpServerIp() != null) {
                        deleteDhcpServerIp(msg.getL3NetworkUuid(), e.getKey(), e.getValue());
                    } else if (IPv6NetworkUtils.isIpv6Address(e.getKey()) && msg.getDhcpv6ServerIp() != null) {
                        deleteDhcpServerIp(msg.getL3NetworkUuid(), e.getKey(), e.getValue());
                    }
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                Map<String, String> dhcpMap = (Map<String, String>) data.get("oldServerIp");
                if (dhcpMap == null || dhcpMap.isEmpty()) {
                    trigger.rollback();
                    return;
                }

                for (Map.Entry<String, String> e : dhcpMap.entrySet()) {
                    String dhcpIp = null;
                    if (IPv6NetworkUtils.isValidIpv4(e.getKey()) && msg.getDhcpServerIp() != null) {
                        boolean allocate_ip = false;
                        for (IpRangeVO ipr : ip4Ranges) {
                            if (NetworkUtils.isInRange(e.getKey(), ipr.getStartIp(), ipr.getEndIp())) {
                                allocate_ip = true;
                                break;
                            }
                        }
                        dhcpIp = allocateDhcpIp(msg.getL3NetworkUuid(), IPv6Constants.IPv4, allocate_ip, e.getKey(), null);
                    } else if (IPv6NetworkUtils.isIpv6Address(e.getKey()) && msg.getDhcpv6ServerIp() != null){
                        boolean allocate_ip = false;
                        for (IpRangeVO ipr : ip6Ranges) {
                            if (IPv6NetworkUtils.isIpv6InRange(e.getKey(), ipr.getStartIp(), ipr.getEndIp())) {
                                allocate_ip = true;
                                break;
                            }
                        }
                        dhcpIp = allocateDhcpIp(msg.getL3NetworkUuid(), IPv6Constants.IPv6, allocate_ip, e.getKey(), null);
                    }

                    if (dhcpIp == null || !dhcpIp.equals(e.getKey())) {
                        logger.error(String.format("roll back dhcp server ip to [%s], but got [%s]", e.getKey(), dhcpIp));
                    }
                }

                trigger.rollback();
            }
        }).then(new Flow() {
            String __name__ = "allocate-new-dhcp-server-ip";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (msg.getDhcpServerIp() != null) {
                    boolean allocate_ip = false;
                    for (IpRangeVO ipr : ip4Ranges) {
                        if (NetworkUtils.isInRange(msg.getDhcpServerIp(), ipr.getStartIp(), ipr.getEndIp())) {
                            allocate_ip = true;
                            break;
                        }
                    }

                    String dhcpIp = allocateDhcpIp(msg.getL3NetworkUuid(), IPv6Constants.IPv4, allocate_ip, msg.getDhcpServerIp(), null);
                    if (dhcpIp == null || !dhcpIp.equals(msg.getDhcpServerIp())) {
                        trigger.fail(operr("change dhcp server ip to [%s], but got [%s]", msg.getDhcpServerIp(), dhcpIp));
                        return;
                    }
                }

                if (msg.getDhcpv6ServerIp() != null) {
                    boolean allocate_ip = false;
                    for (IpRangeVO ipr : ip6Ranges) {
                        if (IPv6NetworkUtils.isIpv6InRange(msg.getDhcpv6ServerIp(), ipr.getStartIp(), ipr.getEndIp())) {
                            allocate_ip = true;
                            break;
                        }
                    }

                    String dhcpIp = allocateDhcpIp(msg.getL3NetworkUuid(), IPv6Constants.IPv6, allocate_ip, msg.getDhcpv6ServerIp(), null);
                    if (dhcpIp == null || !dhcpIp.equals(msg.getDhcpv6ServerIp())) {
                        trigger.fail(operr("change dhcp server ip to [%s], but got [%s]", msg.getDhcpv6ServerIp(), dhcpIp));
                    }
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (msg.getDhcpServerIp() != null) {
                    deleteDhcpServerIp(msg.getL3NetworkUuid(), msg.getDhcpServerIp());
                }

                if (msg.getDhcpv6ServerIp() != null) {
                    deleteDhcpServerIp(msg.getL3NetworkUuid(), msg.getDhcpv6ServerIp());
                }

                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh-dhcp-server-on-hosts";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                refreshDhcpInfoToHosts(l3VO, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                Map<String, String> dhcpMap = getExistingDhcpServerIp(msg.getL3NetworkUuid(), IPv6Constants.DUAL_STACK);
                for (Map.Entry<String, String> e : dhcpMap.entrySet()) {
                    if (IPv6NetworkUtils.isValidIpv4(e.getKey())) {
                        event.setDhcpServerIp(e.getKey());
                    } else {
                        event.setDhcpv6ServerIp(e.getKey());
                    }
                }
                bus.publish(event);
            }
        }).start();
    }

    private void handle(APIGetL3NetworkIpStatisticMsg msg) {
        APIGetL3NetworkIpStatisticReply reply = new APIGetL3NetworkIpStatisticReply();
        List<IpStatisticData> ipStatistics = doStatistic(msg);
        reply.setIpStatistics(ipStatistics);
        if (msg.isReplyWithCount()) {
            reply.setTotal(countResource(msg));
        }
        bus.reply(msg, reply);
    }

    private List<IpStatisticData> doStatistic(APIGetL3NetworkIpStatisticMsg msg) {
        String orderExpr;
        if (SortBy.IP.equals(msg.getSortBy())) {
            Integer ipVersion = Q.New(L3NetworkVO.class).select(L3NetworkVO_.ipVersion)
                    .eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).findValue();
            //when upgrade mysql to 5.6, both ipv4 and ipv6 can use INET6_ATON(ip) as order expression
            if (ipVersion == 4) {
                if (ResourceType.ALL.equals(msg.getResourceType())) {
                    orderExpr = "ipInLong";
                } else {
                    orderExpr = "INET_ATON(ip)";
                }
            } else {
                orderExpr = "ip";
            }
        } else {
            orderExpr = "createDate";
        }

        List<IpStatisticData> res = null;

        switch (msg.getResourceType()) {
            case ResourceType.ALL:
                res = ipStatisticAll(msg, orderExpr);
                break;
            case ResourceType.VIP:
                res = ipStatisticVip(msg, orderExpr);
                break;
            case ResourceType.VM:
                res = ipStatisticVm(msg, orderExpr);
                break;
            case ResourceType.KERNEL_INTERFACE:
                res = ipStatisticKernelInterface(msg, orderExpr);
                break;
        }

        return res != null ? res : new ArrayList<>();
    }

    private Long countResource(APIGetL3NetworkIpStatisticMsg msg) {
        Long res = null;
        switch (msg.getResourceType()) {
            case ResourceType.ALL:
                res = countUsedIp(msg);
                break;
            case ResourceType.VIP:
                res = countVip(msg);
                break;
            case ResourceType.VM:
                res = countVMNicIp(msg);
                break;
            case ResourceType.KERNEL_INTERFACE:
                res = countKernelInterface(msg);
                break;
        }
        return res;
    }

    private List<IpStatisticData> ipStatisticAll(APIGetL3NetworkIpStatisticMsg msg, String sortBy) {
        /*
        select uip.ip, vip.uuid as vipUuid, vip.name as vipName, it.uuid as vmInstanceUuid, it.name as vmInstanceName, it.type, uip.createDate, uip.uuid
        from (select uuid, ip, IpInLong, ipInBinary, createDate, vmNicUuid
            from UsedIpVO
            where l3NetworkUuid = '{uuid}' [and ip like '{ip}']
            order by LENGTH({sortBy}) {direction}, {sortBy} {direction}
            limit {limit} offset {start}) uip
                left join (select uuid, name, usedIpUuid from VipVO
                    where l3NetworkUuid = '{l3Uuid}') vip on uip.uuid = vip.usedIpUuid
                left join (select uuid, vmInstanceUuid from VmNicVO) nic on uip.vmNicUuid = nic.uuid
                left join (select uuid, name, type from VmInstanceVO) it on nic.vmInstanceUuid = it.uuid
        order by LENGTH({sortBy}) {direction}, {sortBy} {direction};
         */
        if (SortBy.IP.equals(msg.getSortBy())) {
            sortBy = "ipInBinary";
        }
        Map<String, String> dhcpMap = getExistingDhcpServerIp(msg.getL3NetworkUuid(), IPv6Constants.DUAL_STACK);
        Set<String> dhcp = dhcpMap.keySet();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select uip.ip, vip.uuid as vipUuid, vip.name as vipName, it.uuid as vmUuid, it.name as vmName, it.type, uip.createDate, uip.uuid ")
                .append("from (select uuid, ip, ipInLong, ipInBinary, createDate, vmNicUuid from UsedIpVO where l3NetworkUuid = '")
                .append(msg.getL3NetworkUuid()).append('\'');

        if (StringUtils.isNotEmpty(msg.getIp())) {
            sqlBuilder.append(" and ip like '").append(msg.getIp()).append('\'');
        }

        sqlBuilder.append(" order by LENGTH(").append(sortBy).append(") ").append(msg.getSortDirection())
                .append(", ").append(sortBy).append(" ").append(msg.getSortDirection()).append(" limit ")
                .append(msg.getLimit()).append(" offset ").append(msg.getStart()).append(") uip ")
                .append("left join ")
                .append("(select uuid, name, usedIpUuid from VipVO ")
                .append("where l3NetworkUuid = '").append(msg.getL3NetworkUuid())
                .append("') vip on uip.uuid = vip.usedIpUuid ")
                .append("left join ")
                .append("(select uuid, vmInstanceUuid from VmNicVO) nic on uip.vmNicUuid = nic.uuid ")
                .append("left join ")
                .append("(select uuid, name, type from VmInstanceVO) it on it.uuid = nic.vmInstanceUuid ")
                .append("order by LENGTH(").append(sortBy).append(") ").append(msg.getSortDirection())
                .append(", ").append(sortBy).append(" ").append(msg.getSortDirection());

        Query q = dbf.getEntityManager().createNativeQuery(sqlBuilder.toString());
        List<Object[]> results = q.getResultList();
        List<IpStatisticData> ipStatistics = new ArrayList<>();
        List<String> vmUuids = new ArrayList<>();

        boolean isAdmin = acntMgr.isAdmin(msg.getSession());

        Set<String> ownedVms = new HashSet<>();
        Set<String> ownedVips = new HashSet<>();
        if (!isAdmin) {
            ownedVms.addAll(acntMgr.getResourceUuidsCanAccessByAccount(msg.getSession().getAccountUuid(), VmInstanceVO.class));
            ownedVips.addAll(acntMgr.getResourceUuidsCanAccessByAccount(msg.getSession().getAccountUuid(), VipVO.class));
        }

        Map<String, String> ipToVip= new HashMap<>();
        for (Object[] result : results) {
            IpStatisticData element = new IpStatisticData();
            ipStatistics.add(element);
            element.setIp((String) result[0]);
            element.setUsedIpUuid((String) result[7]);
            if(result[1] != null) {
               ipToVip.put((String) result[0], (String) result[1]);
            }
            List<String> resourceTypes = new ArrayList<>();
            element.setResourceTypes(resourceTypes);
            if (dhcp.contains(element.getIp())) {
                resourceTypes.add(ResourceType.DHCP);
            }
            if (isAdmin) {
                element.setVipUuid((String) result[1]);
                element.setVipName((String) result[2]);
                element.setVmInstanceUuid((String) result[3]);
                element.setVmInstanceName((String) result[4]);
                element.setVmInstanceType((String) result[5]);
                if (result[3] != null) {
                    vmUuids.add((String) result[3]);
                }
                if (result[1] != null) {
                    resourceTypes.add(ResourceType.VIP);
                }
            } else {
                if (result[1] != null && ownedVips.contains(result[1])) {
                    element.setVipUuid((String) result[1]);
                    element.setVipName((String) result[2]);
                    resourceTypes.add(ResourceType.VIP);
                }

                if (result[3] != null && ownedVms.contains(result[3])) {
                    element.setVmInstanceUuid((String) result[3]);
                    element.setVmInstanceName((String) result[4]);
                    element.setVmInstanceType((String) result[5]);
                    vmUuids.add((String) result[3]);
                }
            }
            if (resourceTypes.isEmpty() && result[3] == null) {
                resourceTypes.add(ResourceType.OTHER);
            }
        }

        Map<String, Tuple> vrInfos = getApplianceVmInfo(vmUuids);

        List<IpStatisticData> copiedElements = new ArrayList<>();
        for (IpStatisticData element : ipStatistics) {
            if (element.getVmInstanceUuid() != null) {
                Tuple vrInfo = vrInfos.get(element.getVmInstanceUuid());
                if (vrInfo != null) {
                    element.setVmInstanceType(vrInfo.get(1, String.class));
                }
            }

            List<String> resourceTypes = element.getResourceTypes();
            if (element.getVmInstanceUuid() != null) {
                if (VmType.USER_VM.equals(element.getVmInstanceType())) {
                    resourceTypes.add(ResourceType.VM);
                } else if (VmType.VROUTER.equals(element.getVmInstanceType())) {
                    resourceTypes.add(ResourceType.VROUTER);
                } else if (VmType.VPC_VROUTER.equals(element.getVmInstanceType())) {
                    resourceTypes.add(ResourceType.VPC_VROUTER);
                }
            }

            L3NetworkGetIpStatisticExtensionPoint exp = getExtensionPointFactory(element.getVmInstanceType());
            if (exp != null) {
                List<String> ownerUuids = exp.getParentUuid(element.getVmInstanceUuid(), element.getVipUuid());
                if (ownerUuids.size() == 0) {
                    element.setApplianceVmOwnerUuid(element.getVmInstanceUuid());
                } else if (ownerUuids.size() == 1) {
                    element.setApplianceVmOwnerUuid(ownerUuids.get(0));
                } else {
                    element.setApplianceVmOwnerUuid(ownerUuids.get(0));
                    int cn = 1;
                    while(cn < ownerUuids.size()) {
                        IpStatisticData copy = new IpStatisticData();
                        copy.setIp(element.getIp());
                        copy.setVipUuid(element.getVipUuid());
                        copy.setVipName(element.getVipName());
                        copy.setVmInstanceUuid(element.getVmInstanceUuid());
                        copy.setVmInstanceName(element.getVmInstanceName());
                        copy.setVmInstanceType(element.getVmInstanceType());
                        copy.setResourceTypes(element.getResourceTypes());
                        copy.setState(element.getState());
                        copy.setUseFor(element.getUseFor());
                        copy.setCreateDate(element.getCreateDate());
                        copy.setOwnerName(element.getOwnerName());
                        copy.setVmDefaultIp(element.getVmDefaultIp());
                        copy.setApplianceVmOwnerUuid(ownerUuids.get(cn));
                        copiedElements.add(copy);
                        cn = cn + 1;
                    }
                }
            }

            if (resourceTypes.contains(ResourceType.OTHER)) {
                L3NetworkGetIpStatisticExtensionPoint ext = getExtensionPointFactory(ResourceType.KERNEL_INTERFACE);
                if (ext != null && element.getUsedIpUuid() != null) {
                    String hostKernelInterfaceUuid = ext.getResourceOwnerUuid(element.getUsedIpUuid());
                    if (hostKernelInterfaceUuid != null) {
                        element.setResourceTypes(Collections.singletonList(ResourceType.KERNEL_INTERFACE));
                        element.setResourceOwnerUuid(hostKernelInterfaceUuid);
                    }
                }
            }
        }
        ipStatistics.addAll(copiedElements);

        return ipStatistics;
    }

    private Long countKernelInterface(APIGetL3NetworkIpStatisticMsg msg) {
        L3NetworkGetIpStatisticExtensionPoint ext = getExtensionPointFactory(ResourceType.KERNEL_INTERFACE);
        if (ext == null) {
            return 0L;
        }

        return ext.countUsedIp(msg.getL3NetworkUuid(), msg.getIp());
    }

    private List<IpStatisticData> ipStatisticKernelInterface(APIGetL3NetworkIpStatisticMsg msg, String sortBy) {
        List<IpStatisticData> ipStatistics = ipStatisticAll(msg, sortBy);
        List<IpStatisticData> res = new ArrayList<>();
        for (IpStatisticData ipStatistic : ipStatistics) {
            if (ipStatistic.getResourceTypes().contains(ResourceType.KERNEL_INTERFACE)) {
                res.add(ipStatistic);
            }
        }

        return res;
    }

    private Long countUsedIp(APIGetL3NetworkIpStatisticMsg msg) {
        String sql = "select count(*) from UsedIpVO where l3NetworkUuid = :l3Uuid";
        if (StringUtils.isNotEmpty(msg.getIp())) {
            sql += " and ip like '" + msg.getIp() + '\'';
        }
        return SQL.New(sql, Long.class)
                .param("l3Uuid", msg.getL3NetworkUuid())
                .find();
    }

    private List<IpStatisticData> ipStatisticVip(APIGetL3NetworkIpStatisticMsg msg, String sortBy) {
        /*
        select ip, vip.uuid, vip.name as vipName, state, useFor, vip.createDate, ac.name as ownerName
        from (select ip, uuid, name, state, useFor, v.createDate, accountUuid
            from VipVO v,
                AccountResourceRefVO a
            where v.l3NetworkUuid = '{l3Uuid}'
                and a.resourceType = 'VipVO'
                and v.uuid = a.resourceUuid
                [and a.accountUuid = '{accUuid}']
                [and ip like '{ip}']
            order by {sortBy} {direction}
            limit {limit} offset {start}) vip
                left join (select uuid, name from AccountVO) ac on ac.uuid = vip.accountUuid
            order by {sortBy} {direction};
         */
        String accUuid = msg.getSession().getAccountUuid();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select ip, vip.uuid, vip.name as vipName, state, useFor, vip.createDate, ac.name as ownerName ")
                .append("from (select ip, uuid, name, state, useFor, v.createDate, accountUuid ")
                .append("from VipVO v, AccountResourceRefVO a where l3NetworkUuid = '")
                .append(msg.getL3NetworkUuid()).append('\'').append(" and a.resourceType = 'VipVO' and a.type = 'Own' ")
                .append("and v.uuid = a.resourceUuid");
        if (StringUtils.isNotEmpty(msg.getIp())) {
            sqlBuilder.append(" and ip like '").append(msg.getIp()).append('\'');
        }
        if (!acntMgr.isAdmin(msg.getSession())) {
            sqlBuilder.append(" and a.accountUuid = '").append(accUuid).append('\'');
        }
        sqlBuilder.append(" order by ").append(sortBy).append(' ').append(msg.getSortDirection())
                .append(" limit ").append(msg.getLimit()).append(" offset ").append(msg.getStart())
                .append(") vip ")
                .append("left join (select uuid, name from AccountVO) ac on ac.uuid = vip.accountUuid")
                .append(" order by ").append(sortBy).append(' ').append(msg.getSortDirection());

        Query q = dbf.getEntityManager().createNativeQuery(sqlBuilder.toString());
        List<Object[]> results = q.getResultList();
        List<IpStatisticData> ipStatistics = new ArrayList<>();

        for (Object[] result : results) {
            IpStatisticData element = new IpStatisticData();
            ipStatistics.add(element);
            element.setIp((String) result[0]);
            element.setVipUuid((String) result[1]);
            element.setVipName((String) result[2]);
            element.setState((String) result[3]);
            element.setUseFor((String) result[4]);
            element.setCreateDate((Timestamp) result[5]);
            element.setOwnerName((String) result[6]);
            element.setResourceTypes(Collections.singletonList(ResourceType.VIP));
        }

        return ipStatistics;
    }

    private Long countVip(APIGetL3NetworkIpStatisticMsg msg) {
        if (acntMgr.isAdmin(msg.getSession())) {
            String sql = "select count(*) from VipVO v where l3NetworkUuid = :l3Uuid";
            if (StringUtils.isNotEmpty(msg.getIp())) {
                sql += " and ip like '" + msg.getIp() + '\'';
            }
            return SQL.New(sql, Long.class)
                    .param("l3Uuid", msg.getL3NetworkUuid())
                    .find();
        } else {
            String sql = "select count(*) from VipVO v, AccountResourceRefVO a where a.accountUuid = :accUuid " +
                    "and v.l3NetworkUuid = :l3Uuid and v.uuid = a.resourceUuid and a.type = :type";
            if (StringUtils.isNotEmpty(msg.getIp())) {
                sql += " and ip like '" + msg.getIp() + '\'';
            }
            return SQL.New(sql, Long.class)
                    .param("accUuid", msg.getSession().getAccountUuid())
                    .param("l3Uuid", msg.getL3NetworkUuid())
                    .param("type", AccessLevel.Own)
                    .find();
        }
    }

    private List<IpStatisticData> ipStatisticVm(APIGetL3NetworkIpStatisticMsg msg, String sortBy) {
        /*
        select ip, vm.uuid, vm.name, vm.type, vm.state, vm.type, vm.createDate, ac.name as ownerName
        from (select n.vmInstanceUuid, u.ip, accountUuid
            from UsedIpVO u,
                 VmNicVO n,
                 AccountResourceRefVO a
            where u.l3NetworkUuid = '{l3Uuid}'
                and resourceType = 'VmNicVO'
                and n.ip is not null
                and n.metadata is null
                and u.vmNicUuid = n.uuid
                and n.uuid = a.resourceUuid
                [and a.accountUuid = '{accUuid}']
                [and u.ip like '{ip}']
            order by {sortBy} {direction}
            [limit {limit} offset {start}]
            ) nic
                left join (select uuid, name from AccountVO) ac on ac.uuid = nic.accountUuid
                left join (select uuid, name, state, type, createDate from VmInstanceVO) vm on nic.vmInstanceUuid = vm.uuid
            order by {sortBy} {direction}
            [limit {limit} offset {start}];
         */

        boolean byIp = SortBy.IP.equals(msg.getSortBy());
        String accUuid = msg.getSession().getAccountUuid();
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select ip, vm.uuid, vm.name, vm.type, vm.state, vm.createDate, ac.name as ownerName ")
                .append("from (select n.vmInstanceUuid, u.ip, accountUuid from UsedIpVO u, VmNicVO n, AccountResourceRefVO a ")
                .append("where u.l3NetworkUuid = '").append(msg.getL3NetworkUuid()).append('\'')
                .append(" and a.type = 'Own'");
        if (StringUtils.isNotEmpty(msg.getIp())) {
            sqlBuilder.append(" and u.ip like '").append(msg.getIp()).append('\'');
        }
        if (!acntMgr.isAdmin(msg.getSession())) {
            sqlBuilder.append(" and a.accountUuid = '").append(accUuid).append('\'');
        }
        sqlBuilder.append(" and resourceType = 'VmNicVO' and n.metadata is null and n.ip is not null and u.vmNicUuid = n.uuid and n.uuid = a.resourceUuid");
        if (byIp) {
            String sortByExpression;
            if ("INET_ATON(ip)".equals(sortBy)) {
                sortByExpression = "INET_ATON(u.ip)";
            } else {
                sortByExpression = sortBy;
            }
            sqlBuilder.append(" order by ").append(sortByExpression).append(' ').append(msg.getSortDirection())
                    .append(" limit ").append(msg.getLimit()).append(" offset ").append(msg.getStart());
        }

        sqlBuilder.append(") nic ")
                .append("left join (select uuid, name from AccountVO) ac on ac.uuid = nic.accountUuid ")
                .append("left join (select uuid, name, createDate, state, type from VmInstanceVO) vm ")
                .append("on vm.uuid = nic.vmInstanceUuid where vm.type = 'UserVm'")
                .append(" order by ").append(sortBy).append(' ').append(msg.getSortDirection());
        if (!byIp) {
            sqlBuilder.append(" limit ").append(msg.getLimit()).append(" offset ").append(msg.getStart());
        }

        Query q = dbf.getEntityManager().createNativeQuery(sqlBuilder.toString());
        List<Object[]> results = q.getResultList();
        List<IpStatisticData> ipStatistics = new ArrayList<>();
        List<String> vmUuids = new ArrayList<>();

        for (Object[] result : results) {
            IpStatisticData element = new IpStatisticData();
            ipStatistics.add(element);
            element.setIp((String) result[0]);
            String uuid = (String) result[1];
            element.setVmInstanceUuid(uuid);
            if (StringUtils.isNotEmpty(uuid)) {
                vmUuids.add(element.getVmInstanceUuid());
            }
            element.setVmInstanceName((String) result[2]);

            String type = (String) result[3];
            element.setVmInstanceType(type);
            element.setResourceTypes(Collections.singletonList(ResourceType.VM));

            element.setState((String) result[4]);
            element.setCreateDate((Timestamp) result[5]);
            element.setOwnerName((String) result[6]);
        }

        if (vmUuids.size() == 0) {
            return ipStatistics;
        }

        List<VmInstanceVO> vms = Q.New(VmInstanceVO.class).in(VmInstanceVO_.uuid, vmUuids).list();

        Map<String, VmInstanceVO> vmvos = vms.stream()
                .collect(Collectors.toMap(VmInstanceVO::getUuid, inv -> inv));
        Map<String, List<String>> vmToDefaultIpMap = new HashMap<>();

        for (IpStatisticData element : ipStatistics) {
            VmInstanceVO vmvo = vmvos.get(element.getVmInstanceUuid());
            if (vmvo == null) {
                continue;
            }
            if (!vmToDefaultIpMap.containsKey(vmvo.getUuid())) {
                List<VmNicVO> nics;
                nics = vmvo.getVmNics().stream()
                        .filter(vmNic -> vmNic.getL3NetworkUuid().equals(vmvo.getDefaultL3NetworkUuid()))
                        .collect(Collectors.toList());

                VmNicVO nic;
                if (nics.size() == 1) {
                    nic = nics.get(0);
                } else if (nics.size() > 1) {
                    nic = VmNicVO.findTheEarliestOne(nics);
                } else {
                    continue;
                }
                vmToDefaultIpMap.put(vmvo.getUuid(), new ArrayList<>());
                for (UsedIpVO usedIpVO : nic.getUsedIps()) {
                    vmToDefaultIpMap.get(vmvo.getUuid()).add(usedIpVO.getIp());
                }
                element.setVmDefaultIp(vmToDefaultIpMap.get(vmvo.getUuid()));
            } else {
                element.setVmDefaultIp(vmToDefaultIpMap.get(vmvo.getUuid()));
            }
        }

        return ipStatistics;
    }

    private Map<String, Tuple> getApplianceVmInfo(List<String> vmUuids) {
        Map<String, Tuple> vrInfos = new HashMap<>();
        if (vmUuids.size() == 0) {
            return vrInfos;
        }
        List<Tuple> vrs = SQL.New("select uuid, applianceVmType, defaultRouteL3NetworkUuid from ApplianceVmVO where uuid in (:vrUuids)",
                Tuple.class)
                .param("vrUuids", vmUuids)
                .list();
        for (Tuple t : vrs) {
            vrInfos.put(t.get(0, String.class), t);
        }
        return vrInfos;
    }

    private Long countVMNicIp(APIGetL3NetworkIpStatisticMsg msg) {
        if (acntMgr.isAdmin(msg.getSession())) {
            String sql = "select count(*) from UsedIpVO u, VmNicVO n, VmInstanceVO i " +
                    "where u.l3NetworkUuid = :l3Uuid and u.vmNicUuid = n.uuid and n.vmInstanceUuid = i.uuid " +
                    "and i.type = 'UserVm'";
            if (StringUtils.isNotEmpty(msg.getIp())) {
                sql += " and u.ip like '" + msg.getIp() + '\'';
            }
            return SQL.New(sql, Long.class)
                    .param("l3Uuid", msg.getL3NetworkUuid())
                    .find();
        } else {
            String sql = "select count(*) from UsedIpVO u, VmNicVO n, VmInstanceVO i, AccountResourceRefVO a " +
                    "where a.accountUuid = :accUuid and u.l3NetworkUuid = :l3Uuid and i.type = 'UserVm' and a.type = 'Own' " +
                    "and u.vmNicUuid = n.uuid and n.vmInstanceUuid = i.uuid and n.uuid = a.resourceUuid";
            if (StringUtils.isNotEmpty(msg.getIp())) {
                sql += " and u.ip like '" + msg.getIp() + '\'';
            }
            return SQL.New(sql, Long.class)
                    .param("accUuid", msg.getSession().getAccountUuid())
                    .param("l3Uuid", msg.getL3NetworkUuid())
                    .find();
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof FlatDhcpAcquireDhcpServerIpMsg) {
            handle((FlatDhcpAcquireDhcpServerIpMsg) msg);
        } else if (msg instanceof L3NetworkUpdateDhcpMsg) {
            handle((L3NetworkUpdateDhcpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetL3NetworkDhcpIpAddressMsg msg) {
        APIGetL3NetworkDhcpIpAddressReply reply = new APIGetL3NetworkDhcpIpAddressReply();

        if (msg.getL3NetworkUuid() == null) {
            reply.setError(argerr("l3 network uuid cannot be null"));
            bus.reply(msg, reply);
            return;
        }

        Map<String, String> dhcpServerMap = getExistingDhcpServerIp(msg.getL3NetworkUuid(), IPv6Constants.DUAL_STACK);
        for (Map.Entry<String, String> entry : dhcpServerMap.entrySet()) {
            String ip = entry.getKey();
            if (NetworkUtils.isIpv4Address(ip)) {
                reply.setIp(ip);
            } else {
                reply.setIp6(ip);
                int l3IpVersion = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).select(L3NetworkVO_.ipVersion).findValue();
                if (l3IpVersion == IPv6Constants.IPv6) {
                    /* to be compitable with old version, dhcp server address of ipv6 only l3 network is filled in this field */
                    reply.setIp(ip);
                }
            }
        }

        bus.reply(msg, reply);
    }

    String allocateDhcpIp(String l3Uuid, int ipVersion) {
        return allocateDhcpIp(l3Uuid,  ipVersion,null);
    }

    String allocateDhcpIp(String l3Uuid, int ipVersion, String excludedIp) {
        return allocateDhcpIp(l3Uuid, ipVersion, true, null, excludedIp);
    }

    private void doAllocateDhcpIp(String l3Uuid, int ipVersion) {
        AllocateIpMsg msg = new AllocateIpMsg();
        msg.setL3NetworkUuid(l3Uuid);
        if (ipVersion == IPv6Constants.IPv4) {
            msg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
        } else {
            msg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IPV6_ALLOCATOR_STRATEGY);
        }
        IpAllocatorStrategy ias = l3NwMgr.getIpAllocatorStrategy(IpAllocatorType.valueOf(msg.getAllocatorStrategy()));
        try {
            UsedIpInventory ip = ias.allocateIp(msg);
            if (ip == null) {
                logger.warn(String.format("failed to allocate DHCP IP for L3 network[%s]", l3Uuid));
                return;
            }

            SystemTagCreator creator = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.newSystemTagCreator(l3Uuid);
            creator.inherent = true;
            creator.setTagByTokens(
                    map(
                            e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN, IPv6NetworkUtils.ipv6AddessToTagValue(ip.getIp())),
                            e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN, ip.getUuid())
                    )
            );
            creator.create();

            for (DhcpServerExtensionPoint exp : pluginRgty.getExtensionList(DhcpServerExtensionPoint.class)) {
                exp.afterAllocateDhcpServerIP(l3Uuid, ip.getIp());
            }
        } catch (OperationFailureException e) {
            logger.warn(String.format("failed to allocate DHCP IP for L3 network[%s]: %s", l3Uuid, e.getMessage()));
        }
    }

    private static Map<String, String> getExistingDhcpServerIp(String l3Uuid, int ipVersion) {
        Map<String, String> ret = new HashMap<>();
        List<String> tags = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTags(l3Uuid);
        if (tags != null) {
            for (String tag: tags) {
                Map<String, String> tokens = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokensByTag(tag);
                String dhcpServerIp = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
                if (dhcpServerIp == null) {
                    continue;
                }
                String dhcpServerIpUuid = tokens.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN);
                dhcpServerIp = IPv6NetworkUtils.ipv6TagValueToAddress(dhcpServerIp);
                if (ipVersion == IPv6Constants.IPv4 && NetworkUtils.isIpv4Address(dhcpServerIp)) {
                    ret.put(dhcpServerIp, dhcpServerIpUuid);
                } else if (ipVersion == IPv6Constants.IPv6 && IPv6NetworkUtils.isIpv6Address(dhcpServerIp)) {
                    ret.put(dhcpServerIp, dhcpServerIpUuid);
                } else if (ipVersion == IPv6Constants.DUAL_STACK) {
                    ret.put(dhcpServerIp, dhcpServerIpUuid);
                }
            }
        }

        return ret;
    }

    @Deferred
    private String allocateDhcpIp(String l3Uuid, int ipVersion, boolean allocate_ip, String requiredIp, String excludedIp) {
        if (!isProvidedByMe(l3Uuid)) {
            return null;
        }

        Map<String, String> dhcpServerMap = getExistingDhcpServerIp(l3Uuid, ipVersion);
        if (!dhcpServerMap.isEmpty()) {
            return dhcpServerMap.keySet().iterator().next();
        }

        // TODO: static allocate the IP to avoid the lock
        GLock lock = new GLock(String.format("l3-%s-allocate-dhcp-ip", l3Uuid), TimeUnit.MINUTES.toSeconds(30));
        lock.lock();
        Defer.defer(lock::unlock);

        dhcpServerMap = getExistingDhcpServerIp(l3Uuid, ipVersion);
        if (!dhcpServerMap.isEmpty()) {
            return dhcpServerMap.keySet().iterator().next();
        }

        String dhcpServerIp = requiredIp;
        /* dhcp server IP uuid in L3_NETWORK_DHCP_IP is not used any more, to be compatible with old version,
         * keep the format of L3_NETWORK_DHCP_IP unchanged, so set it be null temporary, it will be optimized later */
        String dhcpServerIpUuid = "null";
        if (allocate_ip) {
            AllocateIpMsg amsg = new AllocateIpMsg();
            amsg.setL3NetworkUuid(l3Uuid);
            if (requiredIp != null) {
                amsg.setRequiredIp(requiredIp);
            }
            amsg.setIpVersion(ipVersion);
            if (ipVersion == IPv6Constants.IPv4) {
                amsg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY);
            } else {
                amsg.setAllocateStrategy(L3NetworkConstant.FIRST_AVAILABLE_IPV6_ALLOCATOR_STRATEGY);
            }
            amsg.setExcludedIp(excludedIp);
            bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
            MessageReply reply = bus.call(amsg);
            if (!reply.isSuccess()) {
                logger.warn(String.format("failed to allocate DHCP IP for L3 network[%s]: %s", l3Uuid, reply.getError()));
                return null;
            }

            AllocateIpReply r = reply.castReply();
            UsedIpInventory ip = r.getIpInventory();
            dhcpServerIp = ip.getIp();
            dhcpServerIpUuid = ip.getUuid();
        }

        SystemTagCreator creator = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.newSystemTagCreator(l3Uuid);
        creator.inherent = true;
        creator.setTagByTokens(
                map(
                        e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN, IPv6NetworkUtils.ipv6AddessToTagValue(dhcpServerIp)),
                        e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN, dhcpServerIpUuid)
                )
        );
        creator.create();

        logger.debug(String.format("allocated DHCP server IP[ip:%s, uuid:%s] for l3 network[uuid:%s]", dhcpServerIp, dhcpServerIpUuid, l3Uuid));
        for (DhcpServerExtensionPoint exp : pluginRgty.getExtensionList(DhcpServerExtensionPoint.class)) {
            exp.afterAllocateDhcpServerIP(l3Uuid, dhcpServerIp);
        }
        return dhcpServerIp;
    }

    private String allocateDhcpIp(String l3Uuid, int ipVersion, boolean allocate_ip, String requiredIp) {
        return allocateDhcpIp(l3Uuid, ipVersion, allocate_ip, requiredIp, null);
    }

    private void handle(final FlatDhcpAcquireDhcpServerIpMsg msg) {
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() {
                dealMessage(msg);
                return null;
            }

            @MessageSafe
            private void dealMessage(FlatDhcpAcquireDhcpServerIpMsg msg) {
                FlatDhcpAcquireDhcpServerIpReply reply = new FlatDhcpAcquireDhcpServerIpReply();
                L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
                for (int ipVersion : l3NetworkVO.getIpVersions()) {
                    String ip = allocateDhcpIp(msg.getL3NetworkUuid(), ipVersion);
                    if (ip != null) {
                        List<NormalIpRangeVO> iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                                .eq(NormalIpRangeVO_.ipVersion, ipVersion).list();
                        if (iprs == null || iprs.isEmpty()) {
                            logger.warn(String.format("there is no ip range for dhcp server ip [%s]", ip));
                            continue;
                        }

                        FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct struct = new FlatDhcpAcquireDhcpServerIpReply.DhcpServerIpStruct();
                        struct.setIp(ip);
                        struct.setNetmask(iprs.get(0).getNetmask());
                        struct.setIpr(IpRangeInventory.valueOf(iprs.get(0)));
                        struct.setIpVersion(iprs.get(0).getIpVersion());
                        reply.getDhcpServerList().add(struct);
                    }
                }
                bus.reply(msg, reply);
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return String.format("flat-dhcp-get-dhcp-ip-for-l3-network-%s", msg.getL3NetworkUuid());
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(FlatNetworkServiceConstant.SERVICE_ID);
    }

    public void upgradeFlatDhcpServerIp() {
        NetworkServiceProviderVO nsVO = Q.New(NetworkServiceProviderVO.class)
                .eq(NetworkServiceProviderVO_.type, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE.toString())
                .find();
        List<L3NetworkVO> l3NetworkVos = Q.New(L3NetworkVO.class).list();
        for (L3NetworkVO l3vo : l3NetworkVos) {
            List<NetworkServiceL3NetworkRefVO> dhcps = l3vo.getNetworkServices().stream()
                    .filter(ref -> ref.getNetworkServiceType().equals(NetworkServiceType.DHCP.toString())
                            && ref.getNetworkServiceProviderUuid().equals(nsVO.getUuid()))
                    .collect(Collectors.toList());
            if (dhcps.isEmpty()) {
                /* no dhcp service */
                continue;
            }

            List<Integer> ipVersions = l3vo.getIpVersions();
            if (ipVersions.size() == 1) {
                Integer ipVersion = ipVersions.get(0);
                Map<String, String> dhcpMap = getExistingDhcpServerIp(l3vo.getUuid(), ipVersion);
                if (dhcpMap.isEmpty()) {
                    doAllocateDhcpIp(l3vo.getUuid(), ipVersion);
                }
            } else if (ipVersions.size() == 2) {
                /* dual stack */
                Map<String, String> dhcpMap = getExistingDhcpServerIp(l3vo.getUuid(), IPv6Constants.DUAL_STACK);
                boolean hasIpv4 = false;
                boolean hasIpv6 = false;
                for (Map.Entry<String, String> e : dhcpMap.entrySet()) {
                    if (IPv6NetworkUtils.isValidIpv4(e.getKey())) {
                        hasIpv4 = true;
                    } else if (IPv6NetworkUtils.isIpv6Address(e.getKey())) {
                        hasIpv6 = true;
                    }
                }
                if (!hasIpv4) {
                    doAllocateDhcpIp(l3vo.getUuid(), IPv6Constants.IPv4);
                }
                if (!hasIpv6) {
                    doAllocateDhcpIp(l3vo.getUuid(), IPv6Constants.IPv6);
                }
            }
        }
    }

    @Override
    public boolean start() {
        for (L3NetworkGetIpStatisticExtensionPoint ext : pluginRgty.getExtensionList(L3NetworkGetIpStatisticExtensionPoint.class)) {
            getIpStatisticExts.put(ext.getType(), ext);
        }

        if (FlatDhcpGlobalProperty.UPGRADE_FLAT_DHCP_SERVER_IP) {
            upgradeFlatDhcpServerIp();
        }

        return true;
    }

    private L3NetworkGetIpStatisticExtensionPoint getExtensionPointFactory(String type) {
        return getIpStatisticExts.get(type);
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String preDeleteL3Network(L3NetworkInventory inventory) {
        return null;
    }

    @Override
    public void beforeDeleteL3Network(L3NetworkInventory inventory) {
    }

    private boolean isProvidedByMe(String l3Uuid) {
        String providerType = new NetworkProviderFinder().getNetworkProviderTypeByNetworkServiceType(l3Uuid, NetworkServiceType.DHCP.toString());
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING.equals(providerType);
    }

    @Override
    public void afterDeleteL3Network(L3NetworkInventory inventory) {
        if (!isProvidedByMe(inventory.getUuid())) {
            return;
        }

        Map<String, String> dhcpMap = getExistingDhcpServerIp(inventory.getUuid(), IPv6Constants.DUAL_STACK);
        for (Map.Entry<String, String> entry : dhcpMap.entrySet()) {
            String dhcpIp = entry.getKey();
            String dhcpIpUuid = entry.getValue();
            deleteDhcpServerIp(inventory.getUuid(), dhcpIp, dhcpIpUuid);
            logger.debug(String.format("delete DHCP server ip[%s] of the flat network[uuid:%s] as the L3 network is deleted",
                    dhcpIp, inventory.getUuid()));
        }

        deleteNameSpace(inventory, new NopeCompletion());
    }

    private void deleteNameSpace(L3NetworkInventory inventory, Completion completion) {
        deleteNameSpace(inventory, DHCP_DELETE_NAMESPACE_PATH, completion);
    }

    private void deleteNameSpace(L3NetworkInventory inventory, String path, Completion completion) {
        List<String> huuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select host.uuid from HostVO host, L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = host.clusterUuid and host.hypervisorType = :hType and l2.uuid = :uuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("uuid", inventory.getL2NetworkUuid());
                q.setParameter("hType", KVMConstant.KVM_HYPERVISOR_TYPE);
                return q.getResultList();
            }
        }.call();

        if (huuids.isEmpty()) {
            return;
        }

        String brName = new BridgeNameFinder().findByL3Uuid(inventory.getUuid());
        DeleteNamespaceCmd cmd = new DeleteNamespaceCmd();
        cmd.bridgeName = brName;
        cmd.namespaceName = makeNamespaceName(brName, inventory.getUuid());

        new While<>(huuids).step((huuid, comp) -> {
            new KvmCommandSender(huuid).send(cmd, path, wrapper -> {
                DeleteNamespaceRsp rsp = wrapper.getResponse(DeleteNamespaceRsp.class);
                if (rsp == null) {
                    return null;
                }
                return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
            }, new SteppingSendCallback<KvmResponseWrapper>() {
                @Override
                public void success(KvmResponseWrapper w) {
                    logger.debug(String.format("successfully deleted namespace for L3 network[uuid:%s, name:%s] on the " +
                            "KVM host[uuid:%s]", inventory.getUuid(), inventory.getName(), getHostUuid()));
                    comp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    if (!errorCode.isError(HostErrors.OPERATION_FAILURE_GC_ELIGIBLE)) {
                        comp.done();
                        return;
                    }

                    FlatDHCPDeleteNamespaceGC gc = new FlatDHCPDeleteNamespaceGC();
                    gc.path = path;
                    gc.hostUuid = getHostUuid();
                    gc.command = cmd;
                    gc.NAME = String.format("gc-namespace-on-host-%s", getHostUuid());
                    gc.submit();

                    comp.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(new NopeCompletion(completion)){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });

    }

    private List<DhcpInfo> getVmDhcpInfo(VmInstanceInventory vm) {
        return getVmDhcpInfo(vm, null);
    }

    @Transactional(readOnly = true)
    private List<DhcpInfo> getVmDhcpInfo(VmInstanceInventory vm, String l3Uuid) {
        List<DhcpInfo> dhcpInfoList = new ArrayList<>();
        if (!vm.getType().equals(VmInstanceConstant.USER_VM_TYPE)) {
            return dhcpInfoList;
        }

        String sql = "select nic from VmNicVO nic, L3NetworkVO l3, NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO provider, UsedIpVO ip" +
                " where nic.uuid = ip.vmNicUuid and ip.l3NetworkUuid = l3.uuid" +
                " and ref.l3NetworkUuid = l3.uuid and ref.networkServiceProviderUuid = provider.uuid " +
                " and ref.networkServiceType = :dhcpType " +
                " and provider.type = :ptype and nic.vmInstanceUuid = :vmUuid group by nic.uuid";

        TypedQuery<VmNicVO> nq = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        nq.setParameter("dhcpType", NetworkServiceType.DHCP.toString());
        nq.setParameter("ptype", FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);
        nq.setParameter("vmUuid", vm.getUuid());
        List<VmNicVO> nics = nq.getResultList();

        if (l3Uuid != null) {
            nics = nics.stream().filter(nic -> nic.getUsedIps().stream().map(UsedIpVO::getL3NetworkUuid).collect(Collectors.toList()).contains(l3Uuid)).collect(Collectors.toList());
        }

        if (nics.isEmpty()) {
            return dhcpInfoList;
        }

        String hostName = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
        List<VmInstanceSpec.HostName> hostNames = new ArrayList<>();
        if (hostName != null) {
            VmInstanceSpec.HostName hostNameSpec = new VmInstanceSpec.HostName();
            hostNameSpec.setL3NetworkUuid(vm.getDefaultL3NetworkUuid());
            hostNameSpec.setHostname(hostName);
            hostNames.add(hostNameSpec);
        }

        List<VmNicVO> dhcpNics = new ArrayList<>();
        for (VmNicVO nic : nics) {
            if (l3Uuid != null && !VmNicHelper.getL3Uuids(nic).contains(l3Uuid)) {
                continue;
            }

            dhcpNics.add(nic);
        }
        List<DhcpStruct> structs = dhcpExtension.makeDhcpStruct(vm, hostNames, dhcpNics);
        dhcpInfoList.addAll(toDhcpInfo(structs, vm.getHostUuid() != null ? vm.getHostUuid() : vm.getLastHostUuid()));

        return dhcpInfoList;
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null || info.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);
        applyDhcpToHosts(info, destHostUuid, false, completion);
        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot configure DHCP for vm[uuid:%s] on the destination host[uuid:%s]",
                            inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null || info.isEmpty()) {
            return;
        }

        releaseDhcpService(info, inv.getUuid(), srcHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                // ignore
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        if (destHostUuid == null) {
            return;
        }

        List<DhcpInfo> info = getVmDhcpInfo(inv);
        if (info == null || info.isEmpty()) {
            return;
        }

        releaseDhcpService(info, inv.getUuid(), destHostUuid, new NoErrorCompletion() {
            @Override
            public void done() {
                // ignore
            }
        });
    }

    @Override
    public Flow createVmAbnormalLifeCycleHandlingFlow(final VmAbnormalLifeCycleStruct struct) {
        return new Flow() {
            String __name__ = "flat-network-configure-dhcp";
            VmAbnormalLifeCycleOperation operation = struct.getOperation();
            VmInstanceInventory vm = struct.getVmInstance();
            List<DhcpInfo> info = getVmDhcpInfo(vm);

            String applyHostUuidForRollback;
            String releaseHostUuidForRollback;

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (info == null || info.isEmpty()) {
                    trigger.next();
                    return;
                }

                if (operation == VmAbnormalLifeCycleOperation.VmRunningOnTheHost) {
                    vmRunningOnTheHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmStoppedOnTheSameHost) {
                    vmStoppedOnTheSameHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostChanged) {
                    vmRunningFromUnknownStateHostChanged(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromUnknownStateHostNotChanged) {
                    vmRunningFromUnknownStateHostNotChanged(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmMigrateToAnotherHost) {
                    vmMigrateToAnotherHost(trigger);
                } else if (operation == VmAbnormalLifeCycleOperation.VmRunningFromIntermediateState) {
                    vmRunningFromIntermediateState(trigger);
                } else {
                    trigger.next();
                }
            }

            private void vmRunningFromIntermediateState(final FlowTrigger trigger) {
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmMigrateToAnotherHost(final FlowTrigger trigger) {
                releaseDhcpService(info, vm.getUuid(), struct.getOriginalHostUuid(), new NopeNoErrorCompletion());
                applyHostUuidForRollback = struct.getOriginalHostUuid();

                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmRunningFromUnknownStateHostNotChanged(final FlowTrigger trigger) {
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmRunningFromUnknownStateHostChanged(final FlowTrigger trigger) {
                releaseDhcpService(info, vm.getUuid(), struct.getOriginalHostUuid(), new NopeNoErrorCompletion());
                applyHostUuidForRollback = struct.getCurrentHostUuid();
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            private void vmStoppedOnTheSameHost(final FlowTrigger trigger) {
                releaseDhcpService(info, vm.getUuid(), struct.getCurrentHostUuid(), new NoErrorCompletion(trigger) {
                    @Override
                    public void done() {
                        applyHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }
                });
            }

            private void vmRunningOnTheHost(final FlowTrigger trigger) {
                applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(trigger) {
                    @Override
                    public void success() {
                        releaseHostUuidForRollback = struct.getCurrentHostUuid();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (info == null) {
                    trigger.rollback();
                    return;
                }

                if (releaseHostUuidForRollback != null) {
                    releaseDhcpService(info, vm.getUuid(), struct.getOriginalHostUuid(), new NopeNoErrorCompletion());
                }
                if (applyHostUuidForRollback != null) {
                    applyDhcpToHosts(info, struct.getCurrentHostUuid(), false, new Completion(null) {
                        @Override
                        public void success() {
                            //ignore
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("failed to re-apply DHCP configuration of" +
                                    " the vm[uuid:%s] to the host[uuid:%s], %s. You may need to reboot the VM to" +
                                    " make the DHCP works",  vm.getUuid(), applyHostUuidForRollback, errorCode));
                        }
                    });
                }

                trigger.rollback();
            }
        };
    }

    @Override
    public void preDeleteIpRange(IpRangeInventory ipRange) {

    }

    @Override
    public void beforeDeleteIpRange(IpRangeInventory ipRange) {

    }

    private void deleteDhcpServerIp(String l3Uuid, String dhcpServerIp) {
        FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.deleteInherentTag(l3Uuid);
        for (DhcpServerExtensionPoint exp : pluginRgty.getExtensionList(DhcpServerExtensionPoint.class)) {
            exp.afterRemoveDhcpServerIP(l3Uuid, dhcpServerIp);
        }
    }

    private void deleteDhcpServerIp(String l3Uuid, String dhcpServerIp, String dhcpServerIpUuid) {
        if (dhcpServerIpUuid == null) {
            dhcpServerIpUuid = Q.New(UsedIpVO.class).select(UsedIpVO_.uuid)
                    .eq(UsedIpVO_.l3NetworkUuid, l3Uuid)
                    .eq(UsedIpVO_.ip, dhcpServerIp).findValue();
        }
        ReturnIpMsg rmsg = new ReturnIpMsg();
        rmsg.setL3NetworkUuid(l3Uuid);
        rmsg.setUsedIpUuid(dhcpServerIpUuid);
        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, l3Uuid);
        MessageReply reply = bus.call(rmsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(reply.getError());
        }

        FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.deleteInherentTag(l3Uuid,
                FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.instantiateTag(map(
                        e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN, IPv6NetworkUtils.ipv6AddessToTagValue(dhcpServerIp)),
                        e(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_UUID_TOKEN, dhcpServerIpUuid))));

        for (DhcpServerExtensionPoint exp : pluginRgty.getExtensionList(DhcpServerExtensionPoint.class)) {
            exp.afterRemoveDhcpServerIP(l3Uuid, dhcpServerIp);
        }
    }

    @Override
    public void afterDeleteIpRange(IpRangeInventory ipRange) {
        Map<String, String> dhcpServerIpMap = getExistingDhcpServerIp(ipRange.getL3NetworkUuid(), ipRange.getIpVersion());

        boolean ipRangeExisted = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.l3NetworkUuid, ipRange.getL3NetworkUuid())
                .eq(NormalIpRangeVO_.ipVersion, ipRange.getIpVersion()).isExists();
        if (!ipRangeExisted && !dhcpServerIpMap.isEmpty()) {
            Map.Entry<String, String> entry = dhcpServerIpMap.entrySet().iterator().next();
            deleteDhcpServerIp(ipRange.getL3NetworkUuid(), entry.getKey(), entry.getValue());
            logger.debug(String.format("delete DHCP server ip[%s] of the flat network[uuid:%s] as the IP range[uuid:%s] is deleted",
                    entry.getKey(), ipRange.getL3NetworkUuid(), ipRange.getUuid()));
        }
    }

    @Override
    public void failedToDeleteIpRange(IpRangeInventory ipRange, ErrorCode errorCode) {

    }

    private void refreshDhcpInfoForConnectedHost(String hostUuid, List<DhcpInfo> dhcpInfoList, Completion completion) {
        // to flush ebtables
        ConnectCmd cmd = new ConnectCmd();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setHostUuid(hostUuid);
        msg.setCommand(cmd);
        msg.setNoStatusCheck(true);
        msg.setPath(DHCP_CONNECT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    applyDhcpToHosts(dhcpInfoList, hostUuid, true, new Completion(completion) {
                        @Override
                        public void success() {
                            completion.success();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            completion.fail(errorCode);
                        }
                    });
                }
            }
        });
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "prepare-flat-dhcp";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                if (!context.getInventory().getHypervisorType().equals(KVMConstant.KVM_HYPERVISOR_TYPE)) {
                    trigger.next();
                    return;
                }

                final List<DhcpInfo> dhcpInfoList = getDhcpInfoForConnectedKvmHost(context.getInventory(), null);
                if (dhcpInfoList == null) {
                    trigger.next();
                    return;
                }

                // to flush ebtables
                ConnectCmd cmd = new ConnectCmd();
                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setHostUuid(context.getInventory().getUuid());
                msg.setCommand(cmd);
                msg.setNoStatusCheck(true);
                msg.setPath(DHCP_CONNECT_PATH);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, context.getInventory().getUuid());
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                        } else {
                            applyDhcpToHosts(dhcpInfoList, context.getInventory().getUuid(), true, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    }
                });
            }
        };
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceSpec spec) {
        String providerUuid = new NetworkServiceProviderLookup().lookupUuidByType(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING);

        Map<String, List<String>> vmStaticIps = new StaticIpOperator().getStaticIpByVmUuid(spec.getVmInventory().getUuid());
        // make sure the Flat DHCP acquired DHCP server IP before starting VMs,
        // otherwise it may not be able to get IP when lots of VMs start concurrently
        // because the logic of VM acquiring IP is ahead flat DHCP acquiring IP
        for (L3NetworkInventory l3 :VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks())) {
            List<String> serviceTypes = l3.getNetworkServiceTypesFromProvider(providerUuid);
            if (serviceTypes.contains(NetworkServiceType.DHCP.toString())) {
                Map<Integer, String> staticIpMap = new StaticIpOperator().getNicStaticIpMap(vmStaticIps.get(l3.getUuid()));
                for (Integer ipversion : l3.getIpVersions()) {
                    allocateDhcpIp(l3.getUuid(), ipversion, staticIpMap.get(ipversion));
                }
            }
        }
    }

    public static class DhcpInfo {
        public int ipVersion;
        public String raMode;
        public boolean enableRa;
        public String ip;
        public String ip6;
        public String mac;
        public String netmask;
        public String firstIp;
        public String endIp;
        public Integer prefixLength;
        public String gateway;
        public String gateway6;
        public String hostname;
        public boolean isDefaultL3Network;
        public String dnsDomain;
        public List<String> dns;
        public List<String> dns6;
        public String bridgeName;
        public String namespaceName;
        public String l3NetworkUuid;
        public Integer mtu;
        public boolean vmMultiGateway;
        public List<HostRouteInfo> hostRoutes;
        public String nicType;
        public String virtualNetworkId;
    }

    public static class ApplyDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<DhcpInfo> dhcp;
        public boolean rebuild;
        public String l3NetworkUuid;
    }

    public static class BatchApplyDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<ApplyDhcpCmd> dhcpInfos;
    }

    public static class ApplyDhcpRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ReleaseDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<DhcpInfo> dhcp;
    }

    public static class ReleaseDhcpRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class PrepareDhcpCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String virtualNetworkId;
        public String dhcpServerIp;
        public String dhcpNetmask;
        public String namespaceName;
        public Integer ipVersion;
        public String dhcp6ServerIp;
        public Integer prefixLen;
        public String addressMode;
    }

    public static class BatchPrepareDhcpCmd extends KVMAgentCommands.AgentCommand {
        public List<PrepareDhcpCmd> dhcpInfos;
    }

    public static class PrepareDhcpRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ConnectCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class ConnectRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ResetDefaultGatewayCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeNameOfGatewayToRemove;
        public String namespaceNameOfGatewayToRemove;
        public String gatewayToRemove;
        public String macOfGatewayToRemove;
        public String gatewayToAdd;
        public String macOfGatewayToAdd;
        public String bridgeNameOfGatewayToAdd;
        public String namespaceNameOfGatewayToAdd;
    }

    public static class ResetDefaultGatewayRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class DeleteNamespaceCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String namespaceName;
    }

    public static class DeleteNamespaceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class FlushDhcpNamespaceCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String namespaceName;
    }

    public static class FlushDhcpNamespaceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ArpingCmd extends KVMAgentCommands.AgentCommand {
        public String bridgeName;
        public String vlanId;
        public String namespaceName;
        public List<String> targetIps;
    }

    public static class ArpingRsp extends KVMAgentCommands.AgentResponse {
        /* key: arping target ip:
        * value: arping result macs */
        public Map<String, List<String>> result;
    }

    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public List<String> getDnsAddress(L3NetworkInventory l3Inv) {
        List<String>  dns = new ArrayList<String>();

        String providerUuid = Q.New(NetworkServiceProviderVO.class)
                .select(NetworkServiceProviderVO_.uuid)
                .eq(NetworkServiceProviderVO_.type, FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)
                .findValue();
        if (l3Inv.getNetworkServiceTypesFromProvider(providerUuid).contains(NetworkServiceType.DHCP.toString())
                && FlatNetwordProviderGlobalConfig.ALLOW_DEFAULT_DNS.value(Boolean.class)
                && l3Inv.getIpVersions().contains(IPv6Constants.IPv4)){
            Map<String, String> dhcpIpMap = getExistingDhcpServerIp(l3Inv.getUuid(), IPv6Constants.IPv4);
            if (!dhcpIpMap.isEmpty()) {
                Map.Entry<String, String> entry = dhcpIpMap.entrySet().iterator().next();
                dns.add(entry.getKey());
            }
        }

        return dns;
    }

    private List<DhcpInfo> toDhcpInfo(List<DhcpStruct> structs, String hostUuid) {
        final List<String> l3Uuids = structs.stream().map(DhcpStruct::getL3Network)
                .map(L3NetworkInventory::getUuid).distinct().collect(Collectors.toList());
        final List<String> l2Uuids = structs.stream().map(DhcpStruct::getL3Network)
                .map(L3NetworkInventory::getL2NetworkUuid).distinct().collect(Collectors.toList());
        final Map<String, String> l3BridgeNameMap = new BridgeNameFinder().findByL3UuidsOnHost(l3Uuids, hostUuid);
        final Map<String, String> virtualNetworkIdMap = new BridgeVirtualNetworkIdFinder().findByL2Uuids(l2Uuids, hostUuid);

        return transformAndRemoveNull(structs, arg -> {
            if (arg.getIp() == null && arg.getIp6() == null) {
                return null;
            }

            if (l3BridgeNameMap.get(arg.getL3Network().getUuid()) == null) {
                return null;
            }

            if ((arg.getIpVersion() == IPv6Constants.IPv6) && (IPv6Constants.SLAAC.equals(arg.getRaMode()))) {
                return null;
            }

            String vmMultiGateway = VmSystemTags.MULTIPLE_GATEWAY.getTokenByResourceUuid(arg.getVmUuid(),
                    VmSystemTags.MULTIPLE_GATEWAY_TOKEN);
            boolean multiGateway = Boolean.parseBoolean(vmMultiGateway);

            DhcpInfo info = new DhcpInfo();
            info.ipVersion = arg.getIpVersion();
            info.raMode = arg.getRaMode();
            info.enableRa = arg.isEnableRa();
            info.dnsDomain = arg.getDnsDomain();
            info.gateway = arg.getGateway();
            info.hostname = arg.getHostname();
            info.isDefaultL3Network = arg.isDefaultL3Network();

            if (info.isDefaultL3Network) {
                if (info.hostname == null) {
                    /* ipVersion can be ipv4, ipv6, ip46. used ip address as hostName iif ipVersion is ipv6 */
                    if (info.ipVersion == IPv6Constants.IPv6 && arg.getIp6() != null) {
                        info.hostname = IPv6NetworkUtils.ipv6AddessToHostname(arg.getIp6());
                    } else if (arg.getIp() != null) {
                        info.hostname = arg.getIp().replaceAll("\\.", "-");
                    }
                }

                if (info.dnsDomain != null) {
                    info.hostname = String.format("%s.%s", info.hostname, info.dnsDomain);
                }
            }

            List<String> dns = new ArrayList<>();
            List<String> dns6 = new ArrayList<>();
            for (String dnsIp : nwServiceMgr.getL3NetworkDns(arg.getL3Network().getUuid())) {
                if (NetworkUtils.isIpv4Address(dnsIp)) {
                    dns.add(dnsIp);
                } else {
                    dns6.add(dnsIp);
                }
            }
            info.ip = arg.getIp();
            info.netmask = arg.getNetmask();
            info.mac = arg.getMac();
            info.dns = dns;
            info.l3NetworkUuid = arg.getL3Network().getUuid();
            info.bridgeName = l3BridgeNameMap.get(arg.getL3Network().getUuid());
            info.virtualNetworkId = virtualNetworkIdMap.get(info.bridgeName);
            info.namespaceName = makeNamespaceName(info.bridgeName, arg.getL3Network().getUuid());
            info.mtu = arg.getMtu();
            info.hostRoutes = getL3NetworkHostRoute(arg.getL3Network().getUuid());
            info.vmMultiGateway = multiGateway;
            if ((arg.getIpVersion() == IPv6Constants.DUAL_STACK  || arg.getIpVersion() == IPv6Constants.IPv6)
                    && !IPv6Constants.SLAAC.equals(arg.getRaMode())) {
                info.ip6 = arg.getIp6();
                info.gateway6 = arg.getGateway6();
                info.dns6 = dns6;
                info.firstIp = arg.getFirstIp();
                info.endIp = arg.getEndIP();
                info.prefixLength = arg.getPrefixLength();
            }
            info.nicType = arg.getNicType();
            return info;
        });
    }

    private void applyDhcpToHosts(List<DhcpInfo> dhcpInfo, final String hostUuid, final boolean rebuild, final Completion completion) {
        if (new FlatNetworkServiceValidator().validate(hostUuid)) {
            completion.success();
            return;
        }

        final Map<String, List<DhcpInfo>> l3DhcpMap = new HashMap<>();
        for (DhcpInfo d : dhcpInfo) {
            if (!isProvidedByMe(d.l3NetworkUuid)) {
                /* there are vm in this l3, but dhcp is disabled */
                continue;
            }

            List<DhcpInfo> lst = l3DhcpMap.get(d.l3NetworkUuid);
            if (lst == null) {
                lst = new ArrayList<>();
                l3DhcpMap.put(d.l3NetworkUuid, lst);
            }

            lst.add(d);
        }
        if (l3DhcpMap.isEmpty()) {
            completion.success();
            return;
        }

        DhcpApply dhcpApply = new DhcpApply();
        dhcpApply.bus = bus;
        dhcpApply.apply(l3DhcpMap, hostUuid, rebuild, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applyDhcpService(List<DhcpStruct> dhcpStructList, VmInstanceSpec spec, final Completion completion) {
        if (dhcpStructList.isEmpty()) {
            completion.success();
            return;
        }

        applyDhcpToHosts(toDhcpInfo(dhcpStructList, spec.getDestHost().getUuid()),
                spec.getDestHost().getUuid(), false, completion);
    }

    private void releaseDhcpService(List<DhcpInfo> info, final String vmUuid, final String hostUuid, final NoErrorCompletion completion) {
        if (new FlatNetworkServiceValidator().validate(hostUuid)) {
            completion.done();
            return;
        }

        final ReleaseDhcpCmd cmd = new ReleaseDhcpCmd();
        cmd.dhcp = info;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(RELEASE_DHCP_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO: Add GC and notification
                    logger.warn(String.format("failed to release dhcp%s for vm[uuid: %s] on the kvm host[uuid:%s]; %s",
                            cmd.dhcp, vmUuid, hostUuid, reply.getError()));
                    completion.done();
                    return;
                }

                KVMHostAsyncHttpCallReply r = reply.castReply();
                ReleaseDhcpRsp rsp = r.toResponse(ReleaseDhcpRsp.class);
                if (!rsp.isSuccess()) {
                    //TODO Add GC and notification
                    logger.warn(String.format("failed to release dhcp%s for vm[uuid: %s] on the kvm host[uuid:%s]; %s",
                            cmd.dhcp, vmUuid, hostUuid, rsp.getError()));
                    completion.done();
                    return;
                }

                completion.done();
            }
        });
    }

    @Override
    public void releaseDhcpService(List<DhcpStruct> dhcpStructsList, final VmInstanceSpec spec, final NoErrorCompletion completion) {
        if (dhcpStructsList.isEmpty()) {
            completion.done();
            return;
        }

        releaseDhcpService(toDhcpInfo(dhcpStructsList, spec.getVmInventory().getUuid()),
                spec.getVmInventory().getUuid(), spec.getDestHost().getUuid(), completion);
    }

    @Override
    public void vmDefaultL3NetworkChanged(VmInstanceInventory vm, String previousL3, String nowL3, final Completion completion) {
        DebugUtils.Assert(previousL3 != null || nowL3 != null, "why I get two NULL L3 networks!!!!");

        if (!VmInstanceState.Running.toString().equals(vm.getState())) {
            return;
        }

        VmNicInventory pnic = null;
        VmNicInventory nnic = null;

        for (VmNicInventory nic : vm.getVmNics()) {
            try {
                NetworkServiceProviderType pType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                        nic.getL3NetworkUuid(), NetworkServiceType.DHCP);
                if (pType != FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE) {
                    continue;
                }
            } catch (OperationFailureException exception) {
                logger.debug(String.format("dhcp is not enable on l3 network %s", nic.getL3NetworkUuid()));
                continue;
            }

            if (VmNicHelper.getL3Uuids(nic).contains(previousL3)) {
                pnic = nic;
            } else if (VmNicHelper.getL3Uuids(nic).contains(nowL3)) {
                nnic = nic;
            }
        }

        if (pnic == null && nnic == null) {
            completion.success();
            return;
        }

        ResetDefaultGatewayCmd cmd = new ResetDefaultGatewayCmd();
        String hostUuid = vm.getHostUuid();
        if (pnic != null) {
            cmd.gatewayToRemove = pnic.getGateway();
            cmd.macOfGatewayToRemove = pnic.getMac();
            cmd.bridgeNameOfGatewayToRemove = new BridgeNameFinder().findByL3UuidOnHost(previousL3, hostUuid);
            cmd.namespaceNameOfGatewayToRemove = makeNamespaceName(cmd.bridgeNameOfGatewayToRemove, previousL3);
        }
        if (nnic != null) {
            cmd.gatewayToAdd = nnic.getGateway();
            cmd.macOfGatewayToAdd = nnic.getMac();
            cmd.bridgeNameOfGatewayToAdd = new BridgeNameFinder().findByL3UuidOnHost(nowL3, hostUuid);
            cmd.namespaceNameOfGatewayToAdd = makeNamespaceName(cmd.bridgeNameOfGatewayToAdd, nowL3);
        }

        KvmCommandSender sender = new KvmCommandSender(hostUuid);
        sender.send(cmd, RESET_DEFAULT_GATEWAY_PATH, wrapper -> {
            ResetDefaultGatewayRsp rsp = wrapper.getResponse(ResetDefaultGatewayRsp.class);
            return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyDhcpToHosts(Iterator<Map.Entry<String, List<DhcpInfo>>> it, Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        Map.Entry<String, List<DhcpInfo>> e = it.next();
        final String hostUuid = e.getKey();
        final List<DhcpInfo> infos = e.getValue();
        if (infos == null || infos.isEmpty()) {
            applyDhcpToHosts(it, completion);
            return;
        }

        applyDhcpToHosts(infos, hostUuid, false, new Completion(completion) {
            @Override
            public void success() {
                applyDhcpToHosts(it, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                applyDhcpToHosts(it, completion);
            }
        });
    }

    private void handle(L3NetworkUpdateDhcpMsg msg) {
        L3NetworkUpdateDhcpReply reply = new L3NetworkUpdateDhcpReply();

        Map<String, List<DhcpInfo>> l3DhcpMap = new HashMap<String, List<DhcpInfo>>();

        List<String> vmUuids = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, msg.getL3NetworkUuid()).select(VmNicVO_.vmInstanceUuid)
                .groupBy(VmNicVO_.vmInstanceUuid).listValues();

        for (String uuid: vmUuids) {
            VmInstanceInventory vm = VmInstanceInventory.valueOf(dbf.findByUuid(uuid, VmInstanceVO.class));
            if (!vm.getState().equals(VmInstanceState.Running.toString()) || vm.getHostUuid() == null) {
                continue;
            }

            String hostUuid = vm.getHostUuid();
            List<DhcpInfo> hostInfo = l3DhcpMap.computeIfAbsent(hostUuid, k -> new ArrayList<>());
            hostInfo.addAll(getVmDhcpInfo(vm, msg.getL3NetworkUuid()));
        }

        applyDhcpToHosts(l3DhcpMap.entrySet().iterator(), new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<Class>();
        ret.add(APIAddIpRangeMsg.class);
        ret.add(APIAddIpRangeByNetworkCidrMsg.class);
        ret.add(APIAddIpv6RangeMsg.class);
        ret.add(APIAddIpv6RangeByNetworkCidrMsg.class);
        ret.add(APIDeleteIpRangeMsg.class);
        ret.add(APIChangeL3NetworkDhcpIpAddressMsg.class);
        ret.add(APIDetachNetworkServiceFromL3NetworkMsg.class);
        ret.add(APIAttachNetworkServiceToL3NetworkMsg.class);
        ret.add(APIDeleteIpAddressMsg.class);

        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    private void validateIpv6PrefixLength(IpRangeInventory inv) {
        NetworkServiceProviderType providerType = null;
        try {
            providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(
                    inv.getL3NetworkUuid(), NetworkServiceType.DHCP);
        } catch (Exception e) {
            return;
        }

        if (providerType == null || !providerType.toString().equals(FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING)) {
            return;
        }

        if (inv.getPrefixLen() < IPv6Constants.IPV6_PREFIX_LEN_MIN_DNSMASQ) {
            throw new ApiMessageInterceptionException(argerr("minimum ip range prefix length of flat network is %d",
                    IPv6Constants.IPV6_PREFIX_LEN_MIN_DNSMASQ));
        }
    }

    private void validateDhcpServerIp(IpRangeInventory inv, List<String> systemTags) {
        if (systemTags == null || systemTags.isEmpty()) {
            return;
        }

        for (String systemTag : systemTags) {
            if (!FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.isMatch(systemTag)) {
                continue;
            }

            Map<String, String> token = TagUtils.parse(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTagFormat(), systemTag);
            String dhcpServerIp = token.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
            dhcpServerIp = IPv6NetworkUtils.ipv6TagValueToAddress(dhcpServerIp);

            if (inv.getIpVersion() == IPv6Constants.IPv4) {
                if (!NetworkUtils.isIpv4Address(dhcpServerIp)) {
                    throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] is not a IPv4 address", dhcpServerIp));
                }

                if (!NetworkUtils.isIpv4InCidr(dhcpServerIp, inv.getNetworkCidr())) {
                    throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] is not in the cidr [%s]", dhcpServerIp, inv.getNetworkCidr()));
                }
            } else {
                if (!IPv6NetworkUtils.isIpv6Address(dhcpServerIp)) {
                    throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] is not a IPv6 address", dhcpServerIp));
                }

                if (!IPv6NetworkUtils.isIpv6InCidrRange(dhcpServerIp, inv.getNetworkCidr())) {
                    throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] is not in the cidr [%s]", dhcpServerIp, inv.getNetworkCidr()));
                }
            }

            Map<String, String> oldDhcpServerMap = getExistingDhcpServerIp(inv.getL3NetworkUuid(), inv.getIpVersion());
            if (!oldDhcpServerMap.isEmpty()) {
                Map.Entry<String, String> entry = oldDhcpServerMap.entrySet().iterator().next();
                throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] is already existed in l3 network [%s]",
                        entry.getKey(), inv.getL3NetworkUuid()));
            }

            if (dhcpServerIp.equals(inv.getGateway())) {
                throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] can not be equaled to gateway ip",
                        dhcpServerIp));
            }

            L3NetworkVO l3Vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, inv.getL3NetworkUuid()).find();
            if (l3Vo.isSystem()) {
                throw new ApiMessageInterceptionException(argerr("DHCP server ip [%s] can not be configured to system l3",
                        dhcpServerIp));
            }
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddIpRangeMsg) {
            IpRangeInventory inv = IpRangeInventory.fromMessage((APIAddIpRangeMsg)msg);
            validateDhcpServerIp(inv, msg.getSystemTags());
        } else if (msg instanceof APIAddIpRangeByNetworkCidrMsg) {
            List<IpRangeInventory> invs = IpRangeInventory.fromMessage((APIAddIpRangeByNetworkCidrMsg)msg);
            validateDhcpServerIp(invs.get(0), msg.getSystemTags());
        } else if (msg instanceof APIAddIpv6RangeMsg) {
            IpRangeInventory inv = IpRangeInventory.fromMessage((APIAddIpv6RangeMsg)msg);
            validateDhcpServerIp(inv, msg.getSystemTags());
            validateIpv6PrefixLength(inv);
        } else if (msg instanceof APIAddIpv6RangeByNetworkCidrMsg) {
            IpRangeInventory inv = IpRangeInventory.fromMessage((APIAddIpv6RangeByNetworkCidrMsg)msg);
            validateDhcpServerIp(inv, msg.getSystemTags());
            validateIpv6PrefixLength(inv);
        } else if (msg instanceof APIChangeL3NetworkDhcpIpAddressMsg) {
            validate((APIChangeL3NetworkDhcpIpAddressMsg) msg);
        } else if (msg instanceof APIDetachNetworkServiceFromL3NetworkMsg) {
            validate((APIDetachNetworkServiceFromL3NetworkMsg) msg);
        } else if (msg instanceof APIAttachNetworkServiceToL3NetworkMsg) {
            validate((APIAttachNetworkServiceToL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteIpAddressMsg) {
            validate((APIDeleteIpAddressMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteIpAddressMsg msg) {
        for (String uuid : msg.getUsedIpUuids()) {
            UsedIpVO vo = dbf.findByUuid(uuid, UsedIpVO.class);
            Map<String, String> dhcpMap = getExistingDhcpServerIp(vo.getL3NetworkUuid(), vo.getIpVersion());
            if (dhcpMap.containsKey(vo.getIp())) {
                throw new ApiMessageInterceptionException(argerr("could not delete ip address, " +
                        "because ip [%s] is dhcp server ip", vo.getIp()));
            }
        }
    }

    private void validate(APIAttachNetworkServiceToL3NetworkMsg msg) {
        String owner = acntMgr.getOwnerAccountUuidOfResource(msg.getL3NetworkUuid());
        if (!acntMgr.isAdmin(msg.getSession()) && !msg.getSession().getAccountUuid().equals(owner)) {
            throw new ApiMessageInterceptionException(argerr("could change dhcp server ip, because %s is not the owner of l3 network[uuid:%s]",
                    msg.getSession().getAccountUuid(), msg.getL3NetworkUuid()));
        }

        L3NetworkVO l3VO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        List<IpRangeVO> ipv4Ranges = l3VO.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeVO> ipv6Ranges = l3VO.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());

        String dhcpIp = null;
        String dhcp6Ip = null;
        if (msg.getSystemTags() != null) {
            for (String sysTag : msg.getSystemTags()) {
                if (!FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.isMatch(sysTag)) {
                    continue;
                }

                Map<String, String> token = TagUtils.parse(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTagFormat(), sysTag);
                String dhcpServerIp = token.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
                dhcpServerIp = IPv6NetworkUtils.ipv6TagValueToAddress(dhcpServerIp);
                if (NetworkUtils.isIpv4Address(dhcpServerIp)) {
                    dhcpIp = dhcpServerIp;
                } else if (IPv6NetworkUtils.isIpv6Address(dhcpServerIp)) {
                    dhcp6Ip = dhcpServerIp;
                }
            }
        }

        if (dhcpIp != null && ipv4Ranges.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not set dhcp v4 server ip, because there is no ipv4 range"));
        }

        if (dhcpIp != null) {
            if (!NetworkUtils.isIpv4InCidr(dhcpIp, ipv4Ranges.get(0).getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("could not set dhcp v4 server ip, because ip[%s] is not the cidr of l3 [%s]",
                dhcpIp, ipv4Ranges.get(0).getNetworkCidr()));
            }
        }

        if (dhcp6Ip != null && ipv6Ranges.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not set dhcp v6 server ip, because there is no ipv6 range"));
        }

        if (dhcp6Ip != null) {
            if (!IPv6NetworkUtils.isIpv6InCidrRange(dhcp6Ip, ipv6Ranges.get(0).getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("could not set dhcp v6 server ip, because ip[%s] is not the cidr of l3 [%s]",
                        dhcpIp, ipv6Ranges.get(0).getNetworkCidr()));
            }
        }
    }

    private void validate(APIDetachNetworkServiceFromL3NetworkMsg msg) {
        String owner = acntMgr.getOwnerAccountUuidOfResource(msg.getL3NetworkUuid());
        if (!acntMgr.isAdmin(msg.getSession()) && !msg.getSession().getAccountUuid().equals(owner)) {
            throw new ApiMessageInterceptionException(argerr("could change dhcp server ip, because %s is not the owner of l3 network[uuid:%s]",
                    msg.getSession().getAccountUuid(), msg.getL3NetworkUuid()));
        }

        if (msg.getService() == null && (MapUtils.isEmpty(msg.getNetworkServices()))) {
            throw new ApiMessageInterceptionException(argerr("either [service] or [networkServices] must be provided"));
        }

    }

    private void validate(APIChangeL3NetworkDhcpIpAddressMsg msg) {
        if (!isProvidedByMe(msg.getL3NetworkUuid())) {
            throw new ApiMessageInterceptionException(argerr("could change dhcp server ip, because flat dhcp is not enabled"));
        }

        String owner = acntMgr.getOwnerAccountUuidOfResource(msg.getL3NetworkUuid());
        if (!acntMgr.isAdmin(msg.getSession()) && !msg.getSession().getAccountUuid().equals(owner)) {
            throw new ApiMessageInterceptionException(argerr("could change dhcp server ip, because %s is not the owner of l3 network[uuid:%s]",
                    msg.getSession().getAccountUuid(), msg.getL3NetworkUuid()));
        }

        L3NetworkVO l3VO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        List<IpRangeVO> ipv4Ranges = l3VO.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeVO> ipv6Ranges = l3VO.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());

        if (msg.getDhcpServerIp() != null && ipv4Ranges.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could change dhcp v4 server ip, because there is no ipv4 range"));
        }

        if (msg.getDhcpServerIp() != null) {
            if (!NetworkUtils.isIpv4InCidr(msg.getDhcpServerIp(), ipv4Ranges.get(0).getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("could set dhcp v4 server ip, because ip[%s] is not the cidr of l3 [%s]",
                        msg.getDhcpServerIp(), ipv4Ranges.get(0).getNetworkCidr()));
            }
        }

        if (msg.getDhcpv6ServerIp() != null && ipv6Ranges.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could change dhcp v6 server ip, because there is no ipv6 range"));
        }

        if (msg.getDhcpv6ServerIp() != null) {
            if (!IPv6NetworkUtils.isIpv6InCidrRange(msg.getDhcpv6ServerIp(), ipv6Ranges.get(0).getNetworkCidr())) {
                throw new ApiMessageInterceptionException(argerr("could set dhcp v6 server ip, because ip[%s] is not the cidr of l3 [%s]",
                        msg.getDhcpv6ServerIp(), ipv6Ranges.get(0).getNetworkCidr()));
            }
        }

        if (msg.getDhcpServerIp() != null) {
            if (Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, msg.getDhcpServerIp())
                    .eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could set dhcp server ip, because ip[%s] is used",
                        msg.getDhcpServerIp()));
            }
        }

        if (msg.getDhcpv6ServerIp() != null) {
            if (Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, msg.getDhcpv6ServerIp())
                    .eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could set dhcp v6 server ip, because ip[%s] is used",
                        msg.getDhcpServerIp()));
            }
        }
    }

    /* when add an iprage, there are 2 cases:
     *  #1  include dhcp server ip, it means there is no dhcp server yet. and it include 2 sub-cases:
     *      $1.1 dhcp server ip is in this range, actions:
     *          a) allocate dhcp server ip in db
     *          b) create systemtag L3_NETWORK_DHCP_IP
     *      $1.2 dhcp server ip is not in this range, actions:
     *          b) create systemtag L3_NETWORK_DHCP_IP, but usedIp set to null
     *
     *  #2  doesn't include dhcp server ip, it include 2 sub-cases:
     *      $2.1 dhcp server ip is not config, actions: allocate dhcp server ip
     *      $2.2 dhcp server ip is configured, but no in this range, actions: None
     *      $2.3 dhcp server ip is configured and in this range, actions:
     *          a) allocate dhcp server ip in db
     *      */
    @Override
    public void afterAddIpRange(IpRangeInventory ipr, List<String> systemTags) {
        L3NetworkVO l3NetworkVO = dbf.findByUuid(ipr.getL3NetworkUuid(), L3NetworkVO.class);
        List<ReservedIpRangeVO> reservedIpRanges = null;
        if (ipr.getIpVersion() == IPv6Constants.IPv4) {
            reservedIpRanges = l3NetworkVO.getReservedIpRanges()
                    .stream().filter(r -> r.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
            for (ReservedIpRangeVO reservedIpRange : reservedIpRanges) {
                List<UsedIpVO> usedIpVOS = new ArrayList<>();
                if (NetworkUtils.isIpv4RangeOverlap(ipr.getStartIp(), ipr.getEndIp(),
                        reservedIpRange.getStartIp(), reservedIpRange.getEndIp())) {
                    long start = NetworkUtils.ipv4StringToLong(reservedIpRange.getStartIp());
                    long end  = NetworkUtils.ipv4StringToLong(reservedIpRange.getEndIp());

                    for (long i = start; i <= end; i++){
                        String newIp = NetworkUtils.longToIpv4String(i);
                        if (NetworkUtils.isInRange(newIp, ipr.getStartIp(), ipr.getEndIp())) {
                            UsedIpVO vo = new UsedIpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setIpRangeUuid(ipr.getUuid());
                            vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                            vo.setIpVersion(ipr.getIpVersion());
                            vo.setIp(newIp);
                            vo.setIpInLong(i);
                            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
                            vo.setNetmask(ipr.getNetmask());
                            vo.setGateway(ipr.getGateway());
                            vo.setUsedFor(IpAllocatedReason.Reserved.toString());
                            vo.setMetaData(reservedIpRange.getUuid());
                            usedIpVOS.add(vo);
                        }
                    }
                }
                if (!usedIpVOS.isEmpty()) {
                    dbf.persistCollection(usedIpVOS);
                }
            }
        } else {
            List<ReservedIpRangeVO> ipv6ReservedIpRange = l3NetworkVO.getReservedIpRanges()
                    .stream().filter(r -> r.getIpVersion() == IPv6Constants.IPv6).collect(Collectors.toList());
            for (ReservedIpRangeVO reservedIpRange : ipv6ReservedIpRange) {
                List<UsedIpVO> usedIpVOS = new ArrayList<>();
                if (IPv6NetworkUtils.isIpv6RangeOverlap(ipr.getStartIp(), ipr.getEndIp(),
                        reservedIpRange.getStartIp(), reservedIpRange.getEndIp())) {
                    BigInteger start = IPv6Address.fromString(reservedIpRange.getStartIp()).toBigInteger();
                    BigInteger end = IPv6Address.fromString(reservedIpRange.getEndIp()).toBigInteger();

                    for (BigInteger i = start; i.compareTo(end) <= 0; i = i.add(BigInteger.ONE)) {
                        String newIp = IPv6NetworkUtils.IPv6AddressToString(i);
                        if (IPv6NetworkUtils.isIpv6InRange(newIp, ipr.getStartIp(), ipr.getEndIp())) {
                            UsedIpVO vo = new UsedIpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setIpRangeUuid(ipr.getUuid());
                            vo.setL3NetworkUuid(ipr.getL3NetworkUuid());
                            //vo.setVmNicUuid(nic.getUuid());
                            vo.setIpVersion(ipr.getIpVersion());
                            vo.setIp(newIp);
                            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
                            vo.setNetmask(ipr.getNetmask());
                            vo.setGateway(ipr.getGateway());
                            vo.setUsedFor(IpAllocatedReason.Reserved.toString());
                            vo.setMetaData(reservedIpRange.getUuid());

                            usedIpVOS.add(vo);
                        }
                    }
                }
                if (!usedIpVOS.isEmpty()) {
                    dbf.persistCollection(usedIpVOS);
                }
            }
        }

        if (ipr.getIpRangeType() != IpRangeType.Normal) {
            return;
        }

        if (!Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, ipr.getUuid()).isExists()) {
            return;
        }

        String dhcpTag = null;
        String dhcpServerIp = null;
        if (systemTags != null) {
            for (String sysTag : systemTags) {
                if (!FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.isMatch(sysTag)) {
                    continue;
                }

                Map<String, String> token = TagUtils.parse(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTagFormat(), sysTag);
                dhcpServerIp = token.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
                if (dhcpServerIp == null) {
                    continue;
                }

                dhcpServerIp = IPv6NetworkUtils.ipv6TagValueToAddress(dhcpServerIp);
                dhcpTag = sysTag;
                break;
            }
        }

        if (dhcpServerIp != null) {
            if (NetworkUtils.isInRange(dhcpServerIp, ipr.getStartIp(), ipr.getEndIp())) {
                /* case #1.1 */
                allocateDhcpIp(ipr.getL3NetworkUuid(), ipr.getIpVersion(), true, dhcpServerIp);
            } else {
                /* case #1.2 */
                allocateDhcpIp(ipr.getL3NetworkUuid(), ipr.getIpVersion(),false, dhcpServerIp);
            }
            systemTags.remove(dhcpTag);
        } else {
            Map<String, String> oldDhcpServerIpMap = getExistingDhcpServerIp(ipr.getL3NetworkUuid(), ipr.getIpVersion());
            if (oldDhcpServerIpMap.isEmpty()) {
                /* case #2.1 */
                allocateDhcpIp(ipr.getL3NetworkUuid(), ipr.getIpVersion());
                return;
            }

            Map.Entry<String, String> entry = oldDhcpServerIpMap.entrySet().iterator().next();
            if (NetworkUtils.isInRange(entry.getKey(), ipr.getStartIp(), ipr.getEndIp())) {
                /* case #2.3 */
                deleteDhcpServerIp(ipr.getL3NetworkUuid(), entry.getKey(), entry.getValue());
                allocateDhcpIp(ipr.getL3NetworkUuid(), ipr.getIpVersion(), true, entry.getKey());
            }
        }
    }

    private void refreshDhcpInfoToHosts(L3NetworkVO l3VO, Completion completion) {
        L2NetworkVO l2VO = dbf.findByUuid(l3VO.getL2NetworkUuid(), L2NetworkVO.class);
        List<String> clusterUuids = l2VO.getAttachedClusterRefs().stream()
                .map(L2NetworkClusterRefVO::getClusterUuid).collect(Collectors.toList());
        if (clusterUuids.isEmpty()) {
            logger.debug(String.format("l2 network[uuid:%s] did not attach any cluster", l3VO.getL2NetworkUuid()));
            completion.success();
            return;
        }

        List<HostVO> hosts = Q.New(HostVO.class).eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .notIn(HostVO_.state,asList(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status,HostStatus.Connected)
                .in(HostVO_.clusterUuid, clusterUuids).list();
        if (hosts.isEmpty()) {
            logger.debug(String.format("there is no host connected for l3 network[uuid:%s]", l3VO.getUuid()));
            completion.success();
            return;
        }

        Map<String, List<DhcpInfo>> dhcpInfoMap = new HashMap<>();
        for (HostVO vo : hosts) {
            List<DhcpInfo> dhcpInfoList = getDhcpInfoForConnectedKvmHost(KVMHostInventory.valueOf(vo), l3VO.getUuid());
            if (dhcpInfoList != null) {
                dhcpInfoMap.put(vo.getUuid(), dhcpInfoList);
            }
        }
        if (dhcpInfoMap.isEmpty()) {
            logger.debug(String.format("there is no host has vm for l3 network[uuid:%s]", l3VO.getUuid()));
            completion.success();
            return;
        }

        
        new While<>(dhcpInfoMap.entrySet()).step((entry, wcomp) -> {
            String hostUuid = entry.getKey();
            List<DhcpInfo> dhcpInfos = entry.getValue();
            refreshDhcpInfoForConnectedHost(hostUuid, dhcpInfos, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.allDone();
                }
            });
        }, 10).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }

    /* TODO:
     *   1. disable/enable dhcp will disable ipam for flat, public network. VPC network will not
     *   2. if ipam is disabled, can not attach l3 network to applianceVm: vRouter, and SLB
     *   3. after enable/disable/enable dhcp, there are 3 kinds vmnic:
     *      3.a) vmnic without ip
     *      3.b) vmnic with ip specified by UI, this ip is not belong to any ip range
     *      3.c) vmnic with ip allocated from ip range
     *  4. 3.a, 3.b vmnic can not be eip/slb backend
     *  5. 3.a, 3.b vmnic will not has dhcp/userdata functionality
     * */
    @Override
    public void enableNetworkService(L3NetworkVO l3VO, List<String> systemTags, Completion completion) {
        List<IpRangeVO> ip4Ranges = l3VO.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == IPv6Constants.IPv4).collect(Collectors.toList());
        List<IpRangeVO> ip6Ranges = l3VO.getIpRanges().stream().filter(ipr ->
                ipr.getIpVersion() == IPv6Constants.IPv6 && !ipr.getAddressMode().equals(IPv6Constants.SLAAC))
                .collect(Collectors.toList());

        String dhcpIp = null;
        String dhcp6Ip = null;
        if (systemTags != null && !systemTags.isEmpty()){
            for (String sysTag : systemTags) {
                if (!FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.isMatch(sysTag)) {
                    continue;
                }

                Map<String, String> token = TagUtils.parse(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTagFormat(), sysTag);
                String dhcpServerIp = token.get(FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
                dhcpServerIp = IPv6NetworkUtils.ipv6TagValueToAddress(dhcpServerIp);
                if (NetworkUtils.isIpv4Address(dhcpServerIp)) {
                    dhcpIp = dhcpServerIp;
                } else if (IPv6NetworkUtils.isIpv6Address(dhcpServerIp)) {
                    dhcp6Ip = dhcpServerIp;
                }
            }
        }

        if (!ip4Ranges.isEmpty()) {
            boolean allocate_ip = false;
            if (dhcpIp != null) {
                for (IpRangeVO ipr : ip4Ranges) {
                    if (NetworkUtils.isInRange(dhcpIp, ipr.getStartIp(), ipr.getEndIp())) {
                        allocate_ip = true;
                        break;
                    }
                }
            } else {
                allocate_ip = true;
            }
            dhcpIp = allocateDhcpIp(l3VO.getUuid(), IPv6Constants.IPv4, allocate_ip, dhcpIp, null);
            if (dhcpIp == null) {
                completion.fail(operr("failed to allocate DHCP server IP for L3 network[uuid:%s]", l3VO.getUuid()));
                return;
            }
        }

        if (!ip6Ranges.isEmpty()) {
            boolean allocate_ip = false;
            if (dhcp6Ip != null) {
                for (IpRangeVO ipr : ip6Ranges) {
                    if (NetworkUtils.isInRange(dhcp6Ip, ipr.getStartIp(), ipr.getEndIp())) {
                        allocate_ip = true;
                        break;
                    }
                }
            } else {
                allocate_ip = true;
            }
            dhcp6Ip = allocateDhcpIp(l3VO.getUuid(), IPv6Constants.IPv6, allocate_ip, dhcp6Ip, null);
            if (dhcp6Ip == null) {
                completion.fail(operr("failed to allocate DHCP server IP for L3 network[uuid:%s]", l3VO.getUuid()));
                return;
            }
        }

        refreshDhcpInfoToHosts(l3VO, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void flushDhcpConfig(L3NetworkInventory inventory, Completion completion) {
        List<String> huuids = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select host.uuid from HostVO host, L2NetworkVO l2, L2NetworkClusterRefVO ref where l2.uuid = ref.l2NetworkUuid" +
                        " and ref.clusterUuid = host.clusterUuid and host.hypervisorType = :hType and l2.uuid = :uuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("uuid", inventory.getL2NetworkUuid());
                q.setParameter("hType", KVMConstant.KVM_HYPERVISOR_TYPE);
                return q.getResultList();
            }
        }.call();

        if (huuids.isEmpty()) {
            return;
        }

        String brName = new BridgeNameFinder().findByL3Uuid(inventory.getUuid());
        FlushDhcpNamespaceCmd cmd = new FlushDhcpNamespaceCmd();
        cmd.bridgeName = brName;
        cmd.namespaceName = makeNamespaceName(brName, inventory.getUuid());

        new While<>(huuids).step((huuid, comp) -> {
            new KvmCommandSender(huuid).send(cmd, DHCP_FLUSH_NAMESPACE_PATH, wrapper -> {
                FlushDhcpNamespaceRsp rsp = wrapper.getResponse(FlushDhcpNamespaceRsp.class);
                if (rsp == null) {
                    return null;
                }
                return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
            }, new SteppingSendCallback<KvmResponseWrapper>() {
                @Override
                public void success(KvmResponseWrapper w) {
                    logger.debug(String.format("successfully deleted namespace for L3 network[uuid:%s, name:%s] on the " +
                            "KVM host[uuid:%s]", inventory.getUuid(), inventory.getName(), getHostUuid()));
                    comp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.debug(String.format("delete namespace for L3 network[uuid:%s, name:%s] on the " +
                            "KVM host[uuid:%s], failed: %s", inventory.getUuid(),
                            inventory.getName(), getHostUuid(), errorCode.getDetails()));
                    comp.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(new NopeCompletion(completion)){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });

    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, Completion completion) {
        if (!isProvidedByMe(l3VO.getUuid())) {
            completion.success();
            return;
        }

        flushDhcpConfig(L3NetworkInventory.valueOf(l3VO), new Completion(completion) {
            @Override
            public void success() {
                Map<String, String> dhcpServerMap = getExistingDhcpServerIp(l3VO.getUuid(), IPv6Constants.DUAL_STACK);
                if (dhcpServerMap.isEmpty()) {
                    completion.success();
                    return;
                }

                for (Map.Entry<String, String> e : dhcpServerMap.entrySet()) {
                    deleteDhcpServerIp(l3VO.getUuid(), e.getKey(), e.getValue());
                }

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void doArpingOnHost(CheckIpAvailabilityStruct struct, HostInventory host, L2NetworkInventory l2Inv, ReturnValueCompletion<List<String>> completion) {
        ArpingCmd cmd = new ArpingCmd();
        cmd.bridgeName = new BridgeNameFinder().findByL3Uuid(struct.getL3NetworkUuid());
        if (StringUtils.isEmpty(cmd.bridgeName)) {
            completion.fail(operr("cannot find bridge name for L3 network[%s]", struct.getL3NetworkUuid()));
            return;
        }

        cmd.vlanId = new BridgeVirtualNetworkIdFinder().findByL2Uuid(l2Inv.getUuid(), host.getUuid(), false);
        cmd.namespaceName = makeNamespaceName(cmd.bridgeName, struct.getL3NetworkUuid());
        cmd.targetIps = Collections.singletonList(struct.getIp());

        KvmCommandSender sender = new KvmCommandSender(host.getUuid());
        sender.send(cmd, ARPING_NAMESPACE_PATH, wrapper -> {
            ArpingRsp rsp = wrapper.getResponse(ArpingRsp.class);
            return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper returnValue) {
                ArpingRsp rsp = returnValue.getResponse(ArpingRsp.class);
                List<String> conflictMacs = new ArrayList<>();
                if (rsp.result == null || rsp.result.isEmpty()) {
                    completion.success(conflictMacs);
                    return;
                }

                List<String> macs = rsp.result.get(struct.getIp());
                for (String mac : macs) {
                    boolean localMac = Q.New(VmNicVO.class)
                            .eq(VmNicVO_.l3NetworkUuid, struct.getL3NetworkUuid())
                            .eq(VmNicVO_.ip, struct.getIp())
                            .eq(VmNicVO_.mac, mac).isExists();
                    if (!localMac) {
                        conflictMacs.add(mac);
                    }
                }
                completion.success(conflictMacs);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void check(CheckIpAvailabilityStruct struct, ReturnValueCompletion<CheckIpAvailabilityResult> completion) {
        CheckIpAvailabilityResult result = new CheckIpAvailabilityResult();
        if (!struct.getArpCheck() || !NetworkUtils.isIpv4Address(struct.getIp())) {
            completion.success(result);
            return;
        }

        L3NetworkVO l3VO = dbf.findByUuid(struct.getL3NetworkUuid(), L3NetworkVO.class);
        L2NetworkVO l2VO = dbf.findByUuid(l3VO.getL2NetworkUuid(), L2NetworkVO.class);
        List<String> clusterUuids = l2VO.getAttachedClusterRefs().stream()
                .map(L2NetworkClusterRefVO::getClusterUuid).collect(Collectors.toList());
        if (clusterUuids.isEmpty()) {
            logger.debug(String.format("l2 network[uuid:%s] did not attach any cluster", l3VO.getL2NetworkUuid()));
            completion.success(result);
            return;
        }

        /* find 3 hosts to do arping */
        List<HostVO> hosts = Q.New(HostVO.class).eq(HostVO_.hypervisorType, KVMConstant.KVM_HYPERVISOR_TYPE)
                .notIn(HostVO_.state,asList(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status,HostStatus.Connected)
                .in(HostVO_.clusterUuid, clusterUuids).limit(3).list();
        if (hosts.isEmpty()) {
            logger.debug(String.format("there is no host connected for l3 network[uuid:%s]", l3VO.getUuid()));
            completion.success(result);
            return;
        }

        ConcurrentHashMap<String, List<String>> macMaps = new ConcurrentHashMap<String, List<String>>();
        new While<>(hosts).step((hostVO, wcomp) -> {
            doArpingOnHost(struct, HostInventory.valueOf(hostVO), L2NetworkInventory.valueOf(l2VO), new ReturnValueCompletion<List<String>>(wcomp) {
                @Override
                public void success(List<String> returnValue) {
                    if (returnValue != null && !returnValue.isEmpty()) {
                        macMaps.computeIfAbsent(struct.getIp(), k -> new CopyOnWriteArrayList<>());
                        macMaps.get(struct.getIp()).addAll(returnValue);
                        logger.debug(String.format("found arp conflict on host[uuid:%s], result:%s",
                                hostVO.getUuid(), returnValue));
                    }
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.done();
                }
            });
        }, 10).run(new WhileDoneCompletion(null) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (macMaps.get(struct.getIp()) != null) {
                    result.setAvailable(false);
                    result.setReason(String.format("%s", macMaps.get(struct.getIp())));
                }
                completion.success(result);
            }
        });
    }
}
