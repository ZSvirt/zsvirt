package org.zstack.network.service.eip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.StaticIpOperator;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.network.l3.L3NetworkManager;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.network.service.NetworkServiceManager;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.findOneOrNull;

/**
 */
public class EipExtension extends AbstractNetworkServiceExtension implements Component, ReleaseNetworkServiceOnDeletingNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(EipExtension.class);

    @Autowired
    private EipManager eipMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    L3NetworkManager l3Mgr;
    @Autowired
    private NetworkServiceManager nwServiceMgr;

    private static final String SUCCESS = EipExtension.class.getName();

    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return EipConstant.EIP_TYPE;
    }

    @Transactional
    private EipStruct eipVOtoEipStruct(EipVO vo) {
        String sql = "select nic from VmNicVO nic where nic.uuid = :uuid";
        TypedQuery<VmNicVO> q = dbf.getEntityManager().createQuery(sql, VmNicVO.class);
        q.setParameter("uuid", vo.getVmNicUuid());
        VmNicVO nicvo = q.getSingleResult();

        sql = "select vip from VipVO vip where vip.uuid = :uuid";
        TypedQuery<VipVO> vq = dbf.getEntityManager().createQuery(sql, VipVO.class);
        vq.setParameter("uuid", vo.getVipUuid());
        VipVO vipvo = vq.getSingleResult();

        UsedIpInventory guestIp = eipMgr.getEipGuestIp(vo.getUuid());
        EipStruct struct = eipMgr.generateEipStruct(VmNicInventory.valueOf(nicvo), VipInventory.valueOf(vipvo), EipInventory.valueOf(vo), guestIp);

        struct.setSnatInboundTraffic(EipGlobalConfig.SNAT_INBOUND_TRAFFIC.value(Boolean.class));
        return struct;
    }

    private List<EipStruct> workOutEipStruct(VmNicInventory vmNic) {
        List<EipVO> vos = Q.New(EipVO.class).eq(EipVO_.vmNicUuid, vmNic.getUuid()).list();
        if (vos == null || vos.isEmpty()) {
            return null;
        }

        return vos.stream().map(eipVO -> eipVOtoEipStruct(eipVO)).collect(Collectors.toList());
    }

    private boolean isEipShouldBeAttachedToBackend(String vmUuid, String l3Uuid, VmOperation operation) {
        boolean ipChanged = new StaticIpOperator().isIpChange(vmUuid, l3Uuid);

        L3NetworkVO l3Vo = dbf.findByUuid(l3Uuid, L3NetworkVO.class);
        boolean l3Need = l3Mgr.applyNetworkServiceWhenVmStateChange(l3Vo.getType());

        /* when vm is destroyed, eip configure will be deleted */
        boolean stateNeed = EipConstant.vmOperationForDetachEip.contains(operation);

        logger.debug(String.format("eip modified for vm [vmUuid:%s] while [ipChanged:%s] | [l3Need:%s] | [stateNeed:%s]", vmUuid, ipChanged, l3Need, stateNeed));
        return ipChanged || l3Need || stateNeed;
    }

    private Map<String, List<EipStruct>> workOutEipStruct(VmInstanceSpec spec) {
        Map<NetworkServiceProviderType, List<L3NetworkInventory>> map = getNetworkServiceProviderMap(EipConstant.EIP_TYPE,
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()));
        Map<String, List<EipStruct>> ret = new HashMap<String, List<EipStruct>>();
        for (Map.Entry<NetworkServiceProviderType, List<L3NetworkInventory>> e : map.entrySet()) {
            List<EipStruct> structs = new ArrayList<EipStruct>();
            for (final L3NetworkInventory l3 : e.getValue()) {
                if (!isEipShouldBeAttachedToBackend(spec.getVmInventory().getUuid(), l3.getUuid(), spec.getCurrentVmOperation())) {
                    continue;
                }

                final VmNicInventory nic = findOneOrNull(spec.getDestNics(), arg -> {
                    if (arg.getUsedIps() != null && !arg.getUsedIps().isEmpty()) {
                        for (UsedIpInventory ip : arg.getUsedIps()) {
                            if (ip.getL3NetworkUuid().equals(l3.getUuid())) {
                                return true;
                            }
                        }
                    } else {
                        VmNicVO nicVO = dbf.findByUuid(arg.getUuid(), VmNicVO.class);
                        for (UsedIpVO ip : nicVO.getUsedIps()) {
                            if (ip.getL3NetworkUuid().equals(l3.getUuid())) {
                                return true;
                            }
                        }
                    }
                    return false;
                });

                if (nic == null) {
                    continue;
                }

                List<EipVO> evos = new Supplier<List<EipVO>>() {
                    @Override
                    @Transactional
                    public List<EipVO> get() {
                        String sql = "select eip from EipVO eip, VmNicVO nic, UsedIpVO ip where nic.uuid = ip.vmNicUuid and ip.l3NetworkUuid = :l3uuid and nic.uuid = eip.vmNicUuid and nic.uuid = :nicUuid";
                        Query q = dbf.getEntityManager().createQuery(sql);
                        q.setParameter("l3uuid", l3.getUuid());
                        q.setParameter("nicUuid", nic.getUuid());
                        return q.getResultList();
                    }
                }.get();

                if (evos.isEmpty()) {
                    continue;
                }

                for (EipVO evo : evos) {
                    structs.add(eipVOtoEipStruct(evo));
                }
            }
            ret.put(e.getKey().toString(), structs);
        }

        for (WorkOutEipStructureExtensionPoint ext : pluginRgty.getExtensionList(WorkOutEipStructureExtensionPoint.class)) {
            ext.afterWorkOutEipStruct(spec, ret);
        }

        return ret;
    }

    private void applyNetworkService(final String providerType, final Iterator<EipStruct> it, final Map<String, List<EipStruct>> applieds, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        final EipStruct struct = it.next();
        eipMgr.attachEip(struct, providerType, new Completion(completion) {
            private void addAppliedStruct() {
                List<EipStruct> s = applieds.computeIfAbsent(providerType, k -> new ArrayList<EipStruct>());
                s.add(struct);
                logger.debug(String.format("successfully applied eip[uuid:%s, ip:%s] for vm nic[uuid:%s]", struct.getEip().getUuid(),
                        struct.getVip().getIp(), struct.getNic().getUuid()));
            }

            @Override
            public void success() {
                addAppliedStruct();
                applyNetworkService(providerType, it, applieds, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void applyNetworkService(final Iterator<Map.Entry<String, List<EipStruct>>> it, final Map<String, List<EipStruct>> applieds, final Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        Map.Entry<String, List<EipStruct>> e = it.next();
        String providerType = e.getKey();
        applyNetworkService(providerType, e.getValue().iterator(), applieds, new Completion(completion) {
            @Override
            public void success() {
                applyNetworkService(it, applieds, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void applyNetworkService(VmInstanceSpec spec, Map<String, Object> data, Completion completion) {
        // For new created vm, there is no eip
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            completion.success();
            return;
        }

        Map<String, List<EipStruct>> map = workOutEipStruct(spec);
        Map<String, List<EipStruct>> applieds = new HashMap<String, List<EipStruct>>();
        data.put(SUCCESS, applieds);
        applyNetworkService(map.entrySet().iterator(), applieds, completion);
    }


    private void releaseNetworkService(final Iterator<Map.Entry<String, List<EipStruct>>> it, final boolean detachInDb, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        Map.Entry<String, List<EipStruct>> e = it.next();
        releaseNetworkService(e.getValue().iterator(), e.getKey(), detachInDb, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                releaseNetworkService(it, detachInDb, completion);
            }
        });
    }

    private void releaseNetworkService(final Iterator<EipStruct> it, final String providerType, final boolean detachInDb, final NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        final EipStruct struct = it.next();
        Completion cop = new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("successfully released eip[uuid:%s, ip:%s] for vm[uuid:%s, nic uuid:%s]",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getVmInstanceUuid(), struct.getNic().getUuid()));
                releaseNetworkService(it, providerType, detachInDb, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                String err = String.format("failed to release eip[uuid:%s, ip:%s, vm nic uuid:%s] on service provider[%s], service provider should handle the failure, %s",
                        struct.getEip().getUuid(), struct.getVip().getIp(), struct.getNic().getUuid(), providerType, errorCode);
                logger.warn(err);
                releaseNetworkService(it, providerType, detachInDb, completion);
            }
        };

        if (detachInDb) {
            eipMgr.detachEipAndUpdateDb(struct, providerType, DetachEipOperation.FORCE_DB_UPDATE, cop);
        } else {
            eipMgr.detachEip(struct, providerType, cop);
        }
    }

    @Override
    public void releaseNetworkService(VmInstanceSpec spec, Map<String, Object> data, NoErrorCompletion completion) {
        // For new created vm, there is no eip
        if (spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate) {
            completion.done();
            return;
        }

        Map<String, List<EipStruct>> map;
        if (data.containsKey(SUCCESS)) {
            map = (Map<String, List<EipStruct>>) data.get(SUCCESS);
        } else {
            map = workOutEipStruct(spec);
            if (!Optional.ofNullable(spec.getDestHost()).isPresent()){
                completion.done();
                return;
            }
            for (Map.Entry<String, List<EipStruct>> e : map.entrySet()) {
                for (EipStruct struct : e.getValue()) {
                    struct.setHostUuid(spec.getDestHost().getUuid());
                }
            }
        }

        if (map.isEmpty()) {
            completion.done();
            return;
        }

        boolean updateDb = spec.getCurrentVmOperation() == VmOperation.Destroy || spec.getCurrentVmOperation() == VmOperation.DetachNic;
        releaseNetworkService(map.entrySet().iterator(), updateDb, completion);
    }

    @Override
    public void releaseNetworkServiceOnDeletingNic(VmNicInventory nic, NoErrorCompletion completion) {
        List<EipStruct> structs = workOutEipStruct(nic);
        if (structs == null) {
            logger.debug(String.format("vmNic[%s] does not need release eip",nic.getUuid()));
            completion.done();
            return;
        }
        final NetworkServiceProviderType providerType = nwServiceMgr.getTypeOfNetworkServiceProviderForService(nic.getL3NetworkUuid(),
                EipConstant.EIP_TYPE);
        Map<String, List<EipStruct>> map = new HashMap<String, List<EipStruct>>();

        map.put(providerType.toString(), structs);
        releaseNetworkService(map.entrySet().iterator(), true, completion);
    }

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
