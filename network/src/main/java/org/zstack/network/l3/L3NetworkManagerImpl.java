package org.zstack.network.l3;

import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.identity.*;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.l3.datatypes.IpCapacityData;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.AccountManager;
import org.zstack.network.service.MtuGetter;
import org.zstack.network.service.NetworkServiceSystemTag;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigUpdateExtensionPoint;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Callable;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.*;

public class L3NetworkManagerImpl extends AbstractService implements L3NetworkManager, ReportQuotaExtensionPoint,
        ResourceOwnerPreChangeExtensionPoint, PrepareDbInitialValueExtensionPoint, CheckIpAddressAvailabilityExtensionPoint {
    private static final CLogger logger = Utils.getLogger(L3NetworkManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    protected L3NetworkExtensionPointEmitter extpEmitter;

    private Map<String, IpRangeFactory> ipRangeFactories = Collections.synchronizedMap(new HashMap<String, IpRangeFactory>());
    private Map<String, L3NetworkFactory> l3NetworkFactories = Collections.synchronizedMap(new HashMap<String, L3NetworkFactory>());
    private Map<String, IpAllocatorStrategy> ipAllocatorStrategies = Collections.synchronizedMap(new HashMap<String, IpAllocatorStrategy>());
    private Set<String> notAccountMetaDatas = Collections.synchronizedSet(new HashSet<>());

    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(L3NetworkDeletionMsg.class);
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

    private void handleLocalMessage(Message msg) {
        if (msg instanceof L3NetworkMessage) {
            passThrough((L3NetworkMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }


    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateL3NetworkMsg) {
            handle((APICreateL3NetworkMsg) msg);
        } else if (msg instanceof APISetL3NetworkMtuMsg) {
            handle((APISetL3NetworkMtuMsg) msg);
        } else if (msg instanceof APIGetL3NetworkMtuMsg) {
            handle((APIGetL3NetworkMtuMsg) msg);
        } else if (msg instanceof L3NetworkMessage) {
            passThrough((L3NetworkMessage) msg);
        } else if (msg instanceof APIGetL3NetworkTypesMsg) {
            handle((APIGetL3NetworkTypesMsg) msg);
        } else if (msg instanceof APIGetIpAddressCapacityMsg) {
            handle((APIGetIpAddressCapacityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APISetL3NetworkMtuMsg msg) {
        final APISetL3NetworkMtuEvent evt = new APISetL3NetworkMtuEvent(msg.getId());

        NetworkServiceSystemTag.L3_MTU.delete(msg.getL3NetworkUuid());
        SystemTagCreator creator = NetworkServiceSystemTag.L3_MTU.newSystemTagCreator(msg.getL3NetworkUuid());
        creator.ignoreIfExisting = true;
        creator.inherent = false;
        creator.setTagByTokens(
                map(
                        e(NetworkServiceSystemTag.MTU_TOKEN, msg.getMtu()),
                        e(NetworkServiceSystemTag.L3_UUID_TOKEN, msg.getL3NetworkUuid())
                )
        );
        creator.create();

        bus.publish(evt);
    }

    private void handle(final APIGetL3NetworkMtuMsg msg) {
        APIGetL3NetworkMtuReply reply = new APIGetL3NetworkMtuReply();

        reply.setMtu(new MtuGetter().getMtu(msg.getL3NetworkUuid()));
        bus.reply(msg, reply);
    }

    private void handle(final APIGetIpAddressCapacityMsg msg) {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();

        class IpCapacity {
            Map<String, IpCapacity> elements;
            long total;
            long avail;
            long ipv4TotalCapacity;
            long ipv4AvailableCapacity;
            long ipv4UsedIpAddressNumber;
            long ipv6TotalCapacity;
            long ipv6AvailableCapacity;
            long ipv6UsedIpAddressNumber;
            long used = 0L;
        }

        IpCapacity ret = new Callable<IpCapacity>() {
            private long calcTotalIp(List<Tuple> ts) {
                long total = 0;
                for (Tuple t : ts) {
                    String sip = t.get(0, String.class);
                    String eip = t.get(1, String.class);
                    int ipVersion = t.get(2, Integer.class);
                    if (ipVersion == IPv6Constants.IPv4) {
                        total = total + NetworkUtils.getTotalIpInRange(sip, eip);
                    } else {
                        total += IPv6NetworkUtils.getIpv6RangeSize(sip, eip);
                        if (total > Integer.MAX_VALUE) {
                            total = Integer.MAX_VALUE;
                        }
                    }
                }

                return total;
            }

            private void calcElementTotalIp(List<Tuple> tuples, IpCapacity capacity) {
                if (capacity.elements == null) {
                    capacity.elements = new HashMap<>();
                }
                Map<String, IpCapacity> elements = capacity.elements;
                long total = 0;
                long ipv4TotalCapacity = 0;
                long ipv6TotalCapacity = 0;

                for (Tuple tuple : tuples) {
                    String sip = tuple.get(0, String.class);
                    String eip = tuple.get(1, String.class);
                    int ipVersion = tuple.get(3, Integer.class);
                    String elementUuid = tuple.get(4, String.class);

                    IpCapacity element = elements.getOrDefault(elementUuid, new IpCapacity());
                    elements.put(elementUuid, element);
                    if (ipVersion == IPv6Constants.IPv4) {
                        if (NetworkUtils.isValidIpv4Range(sip, eip)) {
                            int t = NetworkUtils.getTotalIpInRange(sip, eip);
                            element.total += t;
                            element.total = Math.min(element.total, Integer.MAX_VALUE);
                            element.avail = element.total;
                            element.ipv4TotalCapacity += t;
                            element.ipv4TotalCapacity = Math.min(element.ipv4TotalCapacity, Integer.MAX_VALUE);
                            element.ipv4AvailableCapacity = element.ipv4TotalCapacity;
                            ipv4TotalCapacity += t;
                            total += t;
                            ipv4TotalCapacity = Math.min(ipv4TotalCapacity, (long) Integer.MAX_VALUE);
                            total = Math.min(total, (long) Integer.MAX_VALUE);
                        }
                    } else {
                        long t = IPv6NetworkUtils.getIpv6RangeSize(sip, eip);
                        element.total += t;
                        element.total = Math.min(element.total, Integer.MAX_VALUE);
                        element.avail = element.total;
                        element.ipv6TotalCapacity += t;
                        element.ipv6TotalCapacity = Math.min(element.ipv6TotalCapacity, Integer.MAX_VALUE);
                        element.ipv6AvailableCapacity = element.ipv6TotalCapacity;
                        ipv6TotalCapacity += t;
                        total += t;
                        ipv6TotalCapacity = Math.min(ipv6TotalCapacity, (long)Integer.MAX_VALUE);
                        total = Math.min(total, (long)Integer.MAX_VALUE);
                    }

                }
                capacity.ipv4TotalCapacity = ipv4TotalCapacity;
                capacity.ipv4AvailableCapacity = ipv4TotalCapacity;
                capacity.ipv6AvailableCapacity = ipv6TotalCapacity;
                capacity.ipv6TotalCapacity = ipv6TotalCapacity;
                capacity.total = total;
                capacity.avail = total;
            }

            private void calcElementUsedIp(List<Tuple> tuples, IpCapacity capacity) {
                if (capacity == null) {
                    return;
                }
                if (capacity.elements == null) {
                    capacity.elements = new HashMap<>();
                }
                Map<String, IpCapacity> elements = capacity.elements;
                long total = 0;
                long ipv4UsedIpAddressNumber = 0;
                long ipv6UsedIpAddressNumber = 0;
                for (Tuple tuple : tuples) {
                    long used = tuple.get(0, Long.class);
                    String elementUuid = tuple.get(1, String.class);
                    int ipVersion = tuple.get(2, Integer.class);

                    IpCapacity element = elements.getOrDefault(elementUuid, new IpCapacity());
                    elements.put(elementUuid, element);
                    if (ipVersion == IPv6Constants.IPv4) {
                       element.ipv4UsedIpAddressNumber += used;
                       element.ipv4AvailableCapacity -= used;
                       ipv4UsedIpAddressNumber += used;
                       ipv4UsedIpAddressNumber = Math.min(ipv4UsedIpAddressNumber, Integer.MAX_VALUE);
                    } else {
                        element.ipv6UsedIpAddressNumber += used;
                        element.ipv6AvailableCapacity -= used;
                        ipv6UsedIpAddressNumber += used;
                        ipv6UsedIpAddressNumber = Math.min(ipv6UsedIpAddressNumber, Integer.MAX_VALUE);
                    }
                    element.used += used;
                    element.avail -= used;
                    total += used;
                    total = Math.min(total, Integer.MAX_VALUE);
                }
                capacity.ipv4AvailableCapacity -= ipv4UsedIpAddressNumber;
                capacity.ipv4UsedIpAddressNumber = ipv4UsedIpAddressNumber;
                capacity.ipv6AvailableCapacity -= ipv6UsedIpAddressNumber;
                capacity.ipv6UsedIpAddressNumber = ipv6UsedIpAddressNumber;
                capacity.used = total;
                capacity.avail -= total;
            }

            @Override
            @Transactional(readOnly = true)
            public IpCapacity call() {
                IpCapacity ret = new IpCapacity();
                if (notAccountMetaDatas.isEmpty()) {
                    notAccountMetaDatas.add(""); // Avoid NULL
                }

                if (msg.getIpRangeUuids() != null && !msg.getIpRangeUuids().isEmpty()) {
                    reply.setResourceType(IpRangeVO.class.getSimpleName());
                    String sql = "select ipr.startIp, ipr.endIp, ipr.netmask, ipr.ipVersion, ipr.uuid from IpRangeVO ipr where ipr.uuid in (:uuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", msg.getIpRangeUuids());
                    List<Tuple> ts = q.getResultList();
                    ts = IpRangeHelper.stripNetworkAndBroadcastAddress(ts);
                    calcElementTotalIp(ts, ret);

                    sql = "select count(distinct uip.ip), uip.ipRangeUuid, uip.ipVersion from UsedIpVO uip where uip.ipRangeUuid in (:uuids) and (uip.metaData not in (:notAccountMetaData) or uip.metaData IS NULL) group by uip.ipRangeUuid, uip.ipVersion";
                    TypedQuery<Tuple> cq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    cq.setParameter("uuids", msg.getIpRangeUuids());
                    cq.setParameter("notAccountMetaData", notAccountMetaDatas);
                    List<Tuple> uts = cq.getResultList();
                    calcElementUsedIp(uts, ret);
                    return ret;
                } else if (msg.getL3NetworkUuids() != null && !msg.getL3NetworkUuids().isEmpty()) {
                    reply.setResourceType(L3NetworkVO.class.getSimpleName());
                    String sql = "select ipr.startIp, ipr.endIp, ipr.netmask, ipr.ipVersion, l3.uuid from IpRangeVO ipr, L3NetworkVO l3 where ipr.l3NetworkUuid = l3.uuid and l3.uuid in (:uuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", msg.getL3NetworkUuids());
                    List<Tuple> ts = q.getResultList();
                    ts = IpRangeHelper.stripNetworkAndBroadcastAddress(ts);
                    calcElementTotalIp(ts, ret);

                    sql = "select count(distinct uip.ip), uip.l3NetworkUuid, uip.ipVersion from UsedIpVO uip where uip.l3NetworkUuid in (:uuids) and (uip.metaData not in (:notAccountMetaData) or uip.metaData IS NULL) group by uip.l3NetworkUuid, uip.ipVersion";
                    TypedQuery<Tuple> cq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    cq.setParameter("uuids", msg.getL3NetworkUuids());
                    cq.setParameter("notAccountMetaData", notAccountMetaDatas);
                    List<Tuple> uts = cq.getResultList();
                    calcElementUsedIp(uts, ret);
                    return ret;
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    reply.setResourceType(ZoneVO.class.getSimpleName());
                    String sql = "select ipr.startIp, ipr.endIp, ipr.netmask, ipr.ipVersion, zone.uuid from IpRangeVO ipr, L3NetworkVO l3, ZoneVO zone where ipr.l3NetworkUuid = l3.uuid and l3.enableIPAM = true and l3.zoneUuid = zone.uuid and zone.uuid in (:uuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("uuids", msg.getZoneUuids());
                    List<Tuple> ts = q.getResultList();
                    ts = IpRangeHelper.stripNetworkAndBroadcastAddress(ts);
                    calcElementTotalIp(ts, ret);

                    sql = "select count(distinct uip.ip), zone.uuid, uip.ipVersion from UsedIpVO uip, L3NetworkVO l3, ZoneVO zone where uip.l3NetworkUuid = l3.uuid and l3.enableIPAM = true and l3.zoneUuid = zone.uuid and zone.uuid in (:uuids) and (uip.metaData not in (:notAccountMetaData) or uip.metaData IS NULL) group by zone.uuid, uip.ipVersion";
                    TypedQuery<Tuple> cq = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    cq.setParameter("uuids", msg.getZoneUuids());
                    cq.setParameter("notAccountMetaData", notAccountMetaDatas);
                    List<Tuple> uts = cq.getResultList();
                    calcElementUsedIp(uts, ret);
                    return ret;
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        if (ret.elements != null) {
            List<IpCapacityData> capacityData = new ArrayList<>();
            ret.elements.forEach((uuid, element) -> {
                IpCapacityData data = new IpCapacityData();
                capacityData.add(data);
                data.setResourceUuid(uuid);
                data.setTotalCapacity(element.total);
                data.setAvailableCapacity(element.avail);
                data.setUsedIpAddressNumber(element.used);
                data.setIpv4TotalCapacity(element.ipv4TotalCapacity);
                data.setIpv4AvailableCapacity(element.ipv4AvailableCapacity);
                data.setIpv4UsedIpAddressNumber(element.ipv4UsedIpAddressNumber);
                data.setIpv6TotalCapacity(element.ipv6TotalCapacity);
                data.setIpv6AvailableCapacity(element.ipv6AvailableCapacity);
                data.setIpv6UsedIpAddressNumber(element.ipv6UsedIpAddressNumber);
            });
            reply.setCapacityData(capacityData);
        }

        reply.setIpv4TotalCapacity(ret.ipv4TotalCapacity);
        reply.setIpv4UsedIpAddressNumber(ret.ipv4UsedIpAddressNumber);
        reply.setIpv4AvailableCapacity(ret.ipv4AvailableCapacity);
        reply.setIpv6TotalCapacity(ret.ipv6TotalCapacity);
        reply.setIpv6UsedIpAddressNumber(ret.ipv6UsedIpAddressNumber);
        reply.setIpv6AvailableCapacity(ret.ipv6AvailableCapacity);
        reply.setTotalCapacity(ret.total);
        reply.setAvailableCapacity(ret.avail);
        reply.setUsedIpAddressNumber(ret.used);
        bus.reply(msg, reply);
    }

    private void handle(APIGetL3NetworkTypesMsg msg) {
        APIGetL3NetworkTypesReply reply = new APIGetL3NetworkTypesReply();
        List<String> lst = new ArrayList<String>(L3NetworkType.getAllTypeNames());
        reply.setL3NetworkTypes(lst);
        bus.reply(msg, reply);
    }

    private void passThrough(String l3NetworkUuid, Message msg) {
        L3NetworkVO vo = dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            L3NetworkEO eo = dbf.findByUuid(l3NetworkUuid, L3NetworkEO.class);
            vo = ObjectUtils.newAndCopy(eo, L3NetworkVO.class);
        }

        if (vo == null) {
            ErrorCode err = err(SysErrors.RESOURCE_NOT_FOUND,
                    "Unable to find L3Network[uuid:%s], it may have been deleted", l3NetworkUuid);
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        L3NetworkFactory factory = getL3NetworkFactory(L3NetworkType.valueOf(vo.getType()));
        L3Network nw = factory.getL3Network(vo);
        nw.handleMessage(msg);
    }

    private void passThrough(L3NetworkMessage msg) {
        passThrough(msg.getL3NetworkUuid(), (Message) msg);
    }

    private void syncManagementServiceTypeWhileCreate(ServiceTypeExtensionPoint ext, L2NetworkVO l2NetworkVO, List<String> hostUuids) {
        String l2NetworkType = l2NetworkVO.getType();
        switch (l2NetworkType) {
            case L2NetworkConstant.VXLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, "vxlan" + l2NetworkVO.getVirtualNetworkId(), null, false);
                break;
            case L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE:
            case L2NetworkConstant.HARDWARE_VXLAN_NETWORK_TYPE:
            case L2NetworkConstant.L2_VLAN_NETWORK_TYPE:
                ext.syncManagementServiceTypeExtensionPoint(hostUuids, l2NetworkVO.getPhysicalInterface(), l2NetworkVO.getVirtualNetworkId(), false);
                break;

            default:
                break;
        }
    }

    private void handle(APICreateL3NetworkMsg msg) {
        String zoneUuid = null;
        if (msg.getL2NetworkUuid() != null) {
            zoneUuid = Q.New(L2NetworkVO.class).select(L2NetworkVO_.zoneUuid).eq(L2NetworkVO_.uuid, msg.getL2NetworkUuid()).findValue();
        }

        L3NetworkVO vo = new L3NetworkVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setDescription(msg.getDescription());
        vo.setDnsDomain(msg.getDnsDomain());
        vo.setL2NetworkUuid(msg.getL2NetworkUuid());
        vo.setName(msg.getName());
        vo.setSystem(msg.isSystem());
        vo.setZoneUuid(zoneUuid);
        vo.setState(L3NetworkState.Enabled);
        vo.setCategory(L3NetworkCategory.valueOf(msg.getCategory()));
        vo.setEnableIPAM(msg.getEnableIPAM());
        if (msg.getIpVersion() != null) {
            vo.setIpVersion(msg.getIpVersion());
        } else {
            vo.setIpVersion(IPv6Constants.IPv4);
        }
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        APICreateL3NetworkEvent evt = new APICreateL3NetworkEvent(msg.getId());
        L3NetworkFactory factory = getL3NetworkFactory(L3NetworkType.valueOf(msg.getType()));
        factory.createL3Network(vo, msg, new ReturnValueCompletion<L3NetworkInventory>(msg) {
            @Override
            public void success(L3NetworkInventory returnValue) {
                tagMgr.createTagsFromAPICreateMessage(msg, returnValue.getUuid(), L3NetworkVO.class.getSimpleName());

                if (msg.isSystem()) {
                    L2NetworkVO l2NetworkVO = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
                    List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).select(L2NetworkClusterRefVO_.clusterUuid)
                            .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkVO.getUuid()).listValues();
                    if (clusterUuids != null && !clusterUuids.isEmpty()) {
                        List<String> hostUuids = Q.New(HostVO.class).select(HostVO_.uuid)
                                .in(HostVO_.clusterUuid, clusterUuids).listValues();
                        for (ServiceTypeExtensionPoint ext : pluginRgty.getExtensionList(ServiceTypeExtensionPoint.class)) {
                            syncManagementServiceTypeWhileCreate(ext, l2NetworkVO, hostUuids);
                        }
                    }
                }

                extpEmitter.afterCreate(evt.getInventory());

                evt.setInventory(returnValue);
                logger.debug(String.format("Successfully created L3Network[name:%s, uuid:%s]",
                        returnValue.getName(), returnValue.getUuid()));
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(L3NetworkConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        installResourceConfigExtensions();
        return true;
    }

    @Override
    public boolean applyNetworkServiceWhenVmStateChange(String type) {
        L3NetworkFactory factory = l3NetworkFactories.get(type);
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find L3NetworkFactory for type(%s)", type));
        }
        return factory.applyNetworkServiceWhenVmStateChange();
    }

    private void populateExtensions() {
        for (L3NetworkFactory f : pluginRgty.getExtensionList(L3NetworkFactory.class)) {
            L3NetworkFactory old = l3NetworkFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate L3NetworkFactory[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getType()));
            }
            l3NetworkFactories.put(f.getType().toString(), f);
        }

        for (IpAllocatorStrategy f : pluginRgty.getExtensionList(IpAllocatorStrategy.class)) {
            IpAllocatorStrategy old = ipAllocatorStrategies.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate IpAllocatorStrategy[%s, %s] for type[%s]", f.getClass().getName(),
                        old.getClass().getName(), f.getType()));
            }
            ipAllocatorStrategies.put(f.getType().toString(), f);
        }

        for (UsedIpNotAccountMetaDataExtensionPoint f : pluginRgty.getExtensionList(UsedIpNotAccountMetaDataExtensionPoint.class)) {
            notAccountMetaDatas.add(f.usedIpNotAccountMetaData());
        }

        for (IpRangeFactory f : pluginRgty.getExtensionList(IpRangeFactory.class)) {
            IpRangeFactory old = ipRangeFactories.get(f.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate L2NetworkFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getType()));
            }
            ipRangeFactories.put(f.getType().toString(), f);
        }
    }

    private void installResourceConfigExtensions() {
        ResourceConfig resourceConfig = rcf.getResourceConfig(L3NetworkGlobalConfig.IP_ALLOCATE_STRATEGY.getIdentity());
        resourceConfig.installUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                cleanUpIpCursorOfAcsStrategy(resourceUuid, oldValue, newValue);
            }
        });
    }

    private void cleanUpIpCursorOfAcsStrategy(String resourceUuid, String oldValue, String newValue) {
        if (oldValue.equals(L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY) &&
        !newValue.equals(L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY)) {
            PatternedSystemTag pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_NORMAL_NEXT_IP;
            pst.delete(resourceUuid);
            pst = L3NetworkSystemTags.NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP;
            pst.delete(resourceUuid);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public IpRangeFactory getIpRangeFactory(IpRangeType type) {
        IpRangeFactory factory = ipRangeFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find IpRangeFactory for type(%s)", type));
        }

        return factory;
    }

    @Override
    public L3NetworkFactory getL3NetworkFactory(L3NetworkType type) {
        L3NetworkFactory factory = l3NetworkFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find L3NetworkFactory for type(%s)", type));
        }

        return factory;
    }

    @Override
    public IpAllocatorStrategy  getIpAllocatorStrategy(IpAllocatorType type) {
        IpAllocatorStrategy factory = ipAllocatorStrategies.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("Cannot find IpAllocatorStrategy for type(%s)", type));
        }

        return factory;
    }

    private UsedIpInventory reserveIpv6(IpRangeVO ipRange, String ip, boolean allowDuplicatedAddress) {
        try {
            UsedIpVO vo = new UsedIpVO();
            //vo.setIpInLong(NetworkUtils.ipv4StringToLong(ip));
            String uuid;
            if (allowDuplicatedAddress) {
                uuid = Platform.getUuid();
            } else {
                uuid = ipRange.getUuid() + ip;
            }
            uuid = UUID.nameUUIDFromBytes(uuid.getBytes()).toString().replaceAll("-", "");
            vo.setUuid(uuid);
            vo.setIpRangeUuid(ipRange.getUuid());
            vo.setIp(IPv6NetworkUtils.getIpv6AddressCanonicalString(ip));
            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
            vo.setL3NetworkUuid(ipRange.getL3NetworkUuid());
            vo.setNetmask(ipRange.getNetmask());
            vo.setGateway(ipRange.getGateway());
            vo.setIpVersion(IPv6Constants.IPv6);
            vo = dbf.persistAndRefresh(vo);
            return UsedIpInventory.valueOf(vo);
        } catch (PersistenceException e) {
            if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class)) {
                logger.debug(String.format("Concurrent ip allocation. " +
                        "Ip[%s] in ip range[uuid:%s] has been allocated, try allocating another one. " +
                        "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                        "we will try finding another ip", ip, ipRange.getUuid()));
                logger.trace("", e);
            } else {
                throw e;
            }
            return null;
        }
    }

    private UsedIpInventory reserveIpv4(IpRangeVO ipRange, String ip, boolean allowDuplicatedAddress) {
        try {
            UsedIpVO vo = new UsedIpVO(ipRange.getUuid(), ip);
            vo.setIpInLong(NetworkUtils.ipv4StringToLong(ip));
            vo.setIpInBinary(NetworkUtils.ipStringToBytes(vo.getIp()));
            String uuid;
            if (allowDuplicatedAddress) {
                uuid = Platform.getUuid();
            } else {
                uuid = ipRange.getUuid() + ip;
                uuid = UUID.nameUUIDFromBytes(uuid.getBytes()).toString().replaceAll("-", "");
            }
            vo.setUuid(uuid);
            vo.setL3NetworkUuid(ipRange.getL3NetworkUuid());
            vo.setNetmask(ipRange.getNetmask());
            vo.setGateway(ipRange.getGateway());
            vo.setIpVersion(IPv6Constants.IPv4);
            vo = dbf.persistAndRefresh(vo);
            return UsedIpInventory.valueOf(vo);
        } catch (PersistenceException e) {
            if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class)) {
                logger.debug(String.format("Concurrent ip allocation. " +
                        "Ip[%s] in ip range[uuid:%s] has been allocated, try allocating another one. " +
                        "The error[Duplicate entry] printed by jdbc.spi.SqlExceptionHelper is no harm, " +
                        "we will try finding another ip", ip, ipRange.getUuid()));
                logger.trace("", e);
            } else {
                throw e;
            }
            return null;
        }
    }

    @Override
    public UsedIpInventory reserveIp(IpRangeVO ipRange, String ip) {
        return reserveIp(ipRange, ip, false);
    }

    @Override
    public UsedIpInventory reserveIp(IpRangeVO ipRange, String ip, boolean allowDuplicatedAddress) {
        if (NetworkUtils.isIpv4Address(ip)) {
            return reserveIpv4(ipRange, ip, allowDuplicatedAddress);
        } else if (IPv6NetworkUtils.isIpv6Address(ip)) {
            return reserveIpv6(ipRange, ip, allowDuplicatedAddress);
        } else {
            return null;
        }
    }

    @Override
    public boolean isIpRangeFull(IpRangeVO vo) {
        List<BigInteger> used = getUsedIpInRange(vo);

        if (vo.getIpVersion() == IPv6Constants.IPv4) {
            int total = NetworkUtils.getTotalIpInRange(vo.getStartIp(), vo.getEndIp());
            return used.size() >= total;
        } else {
            return IPv6NetworkUtils.isIpv6RangeFull(vo.getStartIp(), vo.getEndIp(), used.size());
        }
    }

    @Override
    public List<BigInteger> getUsedIpInRange(IpRangeVO vo) {
        return IpRangeHelper.getUsedIpInRange(vo.getUuid(), vo.getIpVersion());
    }

    @Override
    public void updateIpAllocationMsg(AllocateIpMsg msg, String mac) {
        if (msg.getRequiredIp() != null) {
            return;
        }

        List<NormalIpRangeVO> iprs = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.ipVersion, IPv6Constants.IPv6)
                .eq(NormalIpRangeVO_.l3NetworkUuid, msg.getL3NetworkUuid()).list();
        if (iprs.isEmpty()) {
            return;
        }

        if (!iprs.get(0).getAddressMode().equals(IPv6Constants.Stateful_DHCP)) {
            msg.setRequiredIp(IPv6NetworkUtils.getIPv6AddresFromMac(iprs.get(0).getNetworkCidr(), mac));
            msg.setIpVersion(IPv6Constants.IPv6);
        }
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new L3NumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateL3NetworkMsg.class)
                .addCounterQuota(L3NetworkQuotaConstant.L3_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(L3NetworkVO.class)
                        .eq(L3NetworkVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(L3NetworkQuotaConstant.L3_NUM));

        return list(quota);
    }

    @Override
    @Transactional(readOnly = true)
    public void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid) {
    }

    @Override
    public void prepareDbInitialValue() {
        List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class).isNull(IpRangeVO_.prefixLen).list();
        for (IpRangeVO ipr : ipRangeVOS) {
            ipr.setPrefixLen(NetworkUtils.getPrefixLengthFromNetmask(ipr.getNetmask()));
        }
        if (!ipRangeVOS.isEmpty()) {
            dbf.updateCollection(ipRangeVOS);
        }

        List<VmNicVO> nics = Q.New(VmNicVO.class).notNull(VmNicVO_.usedIpUuid).list();
        List<UsedIpVO> ips = new ArrayList<>();
        for (VmNicVO nic : nics) {
            UsedIpVO ip = Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, nic.getUsedIpUuid()).isNull(UsedIpVO_.vmNicUuid).find();
            if (ip != null) {
                ip.setVmNicUuid(nic.getUuid());
                ips.add(ip);
            }
        }

        if (!ips.isEmpty()) {
            dbf.updateCollection(ips);
        }
    }

    @Override
    public List<VmNicInventory> filterVmNicByIpVersion(List<VmNicInventory> vmNics, int ipVersion) {
        List<VmNicInventory> ret = new ArrayList<>();
        for (VmNicInventory nic : vmNics) {
            if (ipVersion == IPv6Constants.IPv4) {
                if (!nic.isIpv6OnlyNic()) {
                    ret.add(nic);
                }
            } else if (ipVersion == IPv6Constants.IPv6) {
                if (!nic.isIpv4OnlyNic()) {
                    ret.add(nic);
                }
            }
        }

        return ret;
    }

    @Override
    public void check(CheckIpAvailabilityStruct struct, ReturnValueCompletion<CheckIpAvailabilityResult> completion) {
        L3NetworkVO vo = dbf.findByUuid(struct.getL3NetworkUuid(), L3NetworkVO.class);
        L3NetworkFactory factory = getL3NetworkFactory(L3NetworkType.valueOf(vo.getType()));
        L3Network nw = factory.getL3Network(vo);
        completion.success(nw.checkIpAvailability(struct));
    }
}
