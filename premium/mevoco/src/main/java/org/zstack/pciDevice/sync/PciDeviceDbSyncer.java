package org.zstack.pciDevice.sync;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.pciDevice.PciDeviceManager;
import org.zstack.pciDevice.PciDeviceTOHelper;
import org.zstack.pciDevice.entity.PciDeviceOfferingVO;
import org.zstack.pciDevice.entity.PciDevicePciDeviceOfferingRefVO;
import org.zstack.pciDevice.entity.PciDeviceVO;
import org.zstack.pciDevice.entity.PciDeviceVO_;
import org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecVO;
import org.zstack.pciDevice.entity.specification.mdev.MdevDeviceSpecVO_;
import org.zstack.pciDevice.entity.specification.mdev.PciDeviceMdevSpecRefVO;
import org.zstack.pciDevice.entity.specification.mdev.PciDeviceMdevSpecRefVO_;
import org.zstack.pciDevice.entity.specification.mdev.VmInstanceMdevDeviceSpecRefVO;
import org.zstack.pciDevice.entity.specification.mdev.VmInstanceMdevDeviceSpecRefVO_;
import org.zstack.pciDevice.entity.specification.pci.PciDeviceSpecVO;
import org.zstack.pciDevice.entity.specification.pci.PciDeviceSpecVO_;
import org.zstack.pciDevice.entity.specification.pci.VmInstancePciDeviceSpecRefVO;
import org.zstack.pciDevice.entity.specification.pci.VmInstancePciDeviceSpecRefVO_;
import org.zstack.pciDevice.entity.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.entity.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.pciDevice.header.PciDeviceConstants;
import org.zstack.pciDevice.header.PciDeviceStatus;
import org.zstack.pciDevice.header.PciDeviceType;
import org.zstack.pciDevice.header.specification.mdev.MdevDeviceSpecState;
import org.zstack.pciDevice.header.virtual.PciDeviceVirtStatus;
import org.zstack.pciDevice.header.virtual.vfio_mdev.MdevDeviceType;
import org.zstack.pciDevice.message.PciDeviceTO;
import org.zstack.pciDevice.message.SyncPciDeviceInDbMsg;
import org.zstack.pciDevice.spi.AfterSyncPciDeviceExtensionPoint;
import org.zstack.pciDevice.spi.PciDeviceTypeFactory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.NO)
public class PciDeviceDbSyncer {
    private static CLogger logger = Utils.getLogger(PciDeviceDbSyncer.class);

    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private PciDeviceManager pciDeviceMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;

    final String hostUuid;
    final List<PciDeviceTO> tos;
    final List<PciDeviceVO> vos = new ArrayList<>();
    final List<PciDeviceSpecVO> pciSpecs = new ArrayList<>();
    final List<PciDeviceOfferingVO> offerings = new ArrayList<>();

    public PciDeviceDbSyncer(String hostUuid, List<PciDeviceTO> tos) {
        this.hostUuid = hostUuid;
        this.tos = tos;
        this.tos.forEach(to -> to.setHostUuid(hostUuid));
    }

    void prepareSyncPicDevice() {
        vos.clear();
        vos.addAll(Q.New(PciDeviceVO.class).eq(PciDeviceVO_.hostUuid, hostUuid).list());
        pciSpecs.clear();
        pciSpecs.addAll(Q.New(PciDeviceSpecVO.class).list());
        offerings.clear();
        offerings.addAll(Q.New(PciDeviceOfferingVO.class).list());
    }

    void doSyncPciDevice() {
        List<PciDeviceTO> dbTos = PciDeviceTO.valueOf(vos);
        tos.forEach(to -> dbTos.stream().filter(dt -> dt.equals(to)).findFirst().ifPresent(dto -> to.setUuid(dto.getUuid())));
        List<PciDeviceTO> newTos = tos.stream().filter(to -> !dbTos.contains(to)).collect(Collectors.toList());
        List<PciDeviceTO> oldTos = tos.stream().filter(to -> to.getUuid() != null).collect(Collectors.toList());
        List<PciDevicePciDeviceOfferingRefVO> refVOS = new ArrayList<>();

        // 1. create new pci device. we suppose that vf device will not be parents of another vf
        List<PciDeviceTO> parentNewTos = newTos.stream()
                .filter(t -> !StringUtils.isNotBlank(t.getParentAddress()))
                .collect(Collectors.toList());
        List<PciDeviceTO> childNewTos = newTos.stream()
                .filter(t -> StringUtils.isNotBlank(t.getParentAddress()))
                .collect(Collectors.toList());

        HashMap<String, String> pciAddressUuidMap = new HashMap<>();
        HashMap<PciDeviceType, List<PciDeviceVO>> pciDeviceTypeMap = new HashMap<>();
        List<PciDeviceVO> newVOs = new ArrayList<>();

        for (PciDeviceTO to : parentNewTos) {
            PciDeviceVO pciVo = PciDeviceTOHelper.getPciDeviceVO(to, pciSpecs, offerings, refVOS);

            pciAddressUuidMap.put(pciVo.getPciDeviceAddress(), pciVo.getUuid());
            newVOs.add(pciVo);
            pciDeviceTypeMap.computeIfAbsent(pciVo.getType(), k -> new ArrayList<>()).add(pciVo);
        }

        for (PciDeviceTO to : childNewTos) {
            PciDeviceVO pciVo = PciDeviceTOHelper.getPciDeviceVO(to, pciSpecs, offerings, refVOS);

            String parentUuid = pciAddressUuidMap.get(to.getParentAddress());
            if (parentUuid == null) {
                parentUuid = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.hostUuid, hostUuid)
                        .eq(PciDeviceVO_.pciDeviceAddress, to.getParentAddress())
                        .select(PciDeviceVO_.uuid)
                        .limit(1)
                        .findValue();
                if (parentUuid == null) {
                    logger.error(String.format("virtual pci device[pci:%s, parent pci:%s] can not " +
                            " find parent pci device", pciVo.getPciDeviceAddress(), to.getParentAddress()));
                } else {
                    /* parent is not new device */
                    pciAddressUuidMap.put(to.getParentAddress(), parentUuid);
                }
            }
            pciVo.setParentUuid(parentUuid);

            newVOs.add(pciVo);
            pciDeviceTypeMap.computeIfAbsent(pciVo.getType(), k -> new ArrayList<>()).add(pciVo);
        }

        for (Map.Entry<PciDeviceType, List<PciDeviceVO>> e : pciDeviceTypeMap.entrySet()) {
            PciDeviceType pciDeviceType = e.getKey();
            List<PciDeviceVO> pciDeviceVos = e.getValue();

            PciDeviceTypeFactory f = pciDeviceMgr.getPciDeviceTypeFactory(pciDeviceType);
            f.createPciDevices(pciDeviceVos);
        }

        if (!refVOS.isEmpty()) {
            dbf.persistCollection(refVOS);
        }

        // 2. update existing pci devices
        Map<String, PciDeviceVO> vosMap = vos.stream().collect(Collectors.toMap(PciDeviceVO::getUuid, vo -> vo));
        HashMap<PciDeviceType, List<PciDeviceVO>> updatePciDeviceTypeMap = new HashMap<>();
        for (PciDeviceTO to : oldTos) {
            PciDeviceVO pci = vosMap.get(to.getUuid());
            if (pci == null) {
                logger.warn(String.format("cannot update pci device[uuid:%s] because it doesn't exist", to.getUuid()));
                continue;
            }

            PciDeviceTOHelper.updatePciDeviceVO(pci, to);
            pciSpecs.stream().filter(s -> s.matchPciDevice(to)).findFirst().ifPresent(spec -> pci.setPciSpecUuid(spec.getUuid()));
            if (pci.getVmInstanceUuid() != null && !pci.getVmInstanceUuid().isEmpty()) {
                pci.setStatus(PciDeviceStatus.Attached);
            } else if (offerings.stream().anyMatch(offeringVO -> offeringVO.matchPciDevice(pci))) {
                pci.setStatus(PciDeviceStatus.Active);
            } else {
                pci.setStatus(PciDeviceStatus.System);
            }

            if (StringUtils.isEmpty(pci.getDevice())) {
                pci.setDevice(to.getDevice());
            }

            if (StringUtils.isEmpty(pci.getVendor())) {
                pci.setVendor(to.getVendor());
            }

            pci.setAddonInfo(to.getAddonInfo());
            updatePciDeviceTypeMap.computeIfAbsent(pci.getType(), k -> new ArrayList<>()).add(pci);
            logger.debug(String.format("updated existing pci device[uuid:%s, name:%s]", pci.getUuid(), pci.getName()));
        }


        for (Map.Entry<PciDeviceType, List<PciDeviceVO>> e : updatePciDeviceTypeMap.entrySet()) {
            PciDeviceType pciDeviceType = e.getKey();
            List<PciDeviceVO> pciDeviceVos = e.getValue();

            PciDeviceTypeFactory f = pciDeviceMgr.getPciDeviceTypeFactory(pciDeviceType);
            f.updatePciDevices(pciDeviceVos);
        }

        // 3. delete garbage pci device
        List<PciDeviceTO> garbageTos = new ArrayList<>(dbTos);
        garbageTos.removeAll(tos);
        HashMap<PciDeviceType, List<PciDeviceTO>> garbagePciDeviceTypeMap = new HashMap<>();
        List<String> garbageUuids = garbageTos.stream().map(PciDeviceTO::getUuid).collect(Collectors.toList());
        for (PciDeviceTO pciTo :garbageTos) {
            garbagePciDeviceTypeMap.computeIfAbsent(PciDeviceType.valueOf(pciTo.getType()),
                    k -> new ArrayList<>()).add(pciTo);
        }

        for (Map.Entry<PciDeviceType, List<PciDeviceTO>> e : garbagePciDeviceTypeMap.entrySet()) {
            PciDeviceType pciDeviceType = e.getKey();
            List<PciDeviceTO> pciDeviceTos = e.getValue();
            PciDeviceTypeFactory f = pciDeviceMgr.getPciDeviceTypeFactory(pciDeviceType);
            f.removePciDevices(pciDeviceTos);
        }

        /* 4. move to step #1
         link virtual pci devices with physical pci devices
        List<PciDeviceTO> vTos = newTos.stream()
                .filter(t -> PciDeviceVirtStatus.SRIOV_VIRTUAL.isEqual(t.getVirtStatus())
                        && StringUtils.isNotBlank(t.getParentAddress()))
                .collect(Collectors.toList());

        List<PciDeviceVO> virtualPcis = new ArrayList<>();
        Map<String, String> physicalPcis = new HashMap<>();
        Map<String, PciDeviceVO> newVOsMap = newVOs.stream().collect(Collectors.toMap(PciDeviceVO::getUuid, vo -> vo));
        for (PciDeviceTO vTo : vTos) {
            String parentUuid;
            PciDeviceVO virtual = newVOsMap.get(vTo.getUuid());
            if (physicalPcis.containsKey(vTo.getParentAddress())) {
                parentUuid = physicalPcis.get(vTo.getParentAddress());
            } else {
                parentUuid = Q.New(PciDeviceVO.class)
                        .eq(PciDeviceVO_.hostUuid, hostUuid)
                        .eq(PciDeviceVO_.pciDeviceAddress, vTo.getParentAddress())
                        .select(PciDeviceVO_.uuid)
                        .limit(1)
                        .findValue();
                physicalPcis.put(vTo.getParentAddress(), parentUuid);
            }

            if (StringUtils.equals(parentUuid, virtual.getParentUuid())) {
                continue;
            }

            virtual.setParentUuid(parentUuid);
            virtualPcis.add(virtual);
            logger.debug(String.format("update parent uuid of pci device[uuid:%s] to [%s]", vTo.getUuid(), parentUuid));
        }

        if (!virtualPcis.isEmpty()) {
            dbf.updateCollection(virtualPcis);
        }*/

        // 5. clean up virtual pci devices that are created bypass zstack
        List<String> pfOutZstack = Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.VIRTUALIZED_BYPASS_ZSTACK)
                .select(PciDeviceVO_.uuid).listValues();

        if (!pfOutZstack.isEmpty()) {
            SQL.New(PciDeviceVO.class)
                    .eq(PciDeviceVO_.virtStatus, PciDeviceVirtStatus.SRIOV_VIRTUAL)
                    .in(PciDeviceVO_.parentUuid, pfOutZstack)
                    .hardDelete();
            logger.debug(String.format("virtual pci devices whose parent uuid in [uuids:%s] are deleted, " +
                    "because they are generated bypass zstack.", pfOutZstack));
        }

        // 6. after sync pci device
        vos.removeIf(vo -> garbageUuids.contains(vo.getUuid()));
    }

    void doCreateOrUpdatePciDeviceSpec() {
        List<PciDeviceTO> distinctTOs = tos.stream().collect(Collectors.collectingAndThen(
                Collectors.toCollection(() -> new TreeSet<>(Comparator
                        .comparing(PciDeviceTO::getVendorId)
                        .thenComparing(PciDeviceTO::getDeviceId)
                        .thenComparing(PciDeviceTO::getSubvendorId, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(PciDeviceTO::getSubdeviceId, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(PciDeviceTO::getMaxPartNum))),
                ArrayList::new));

        List<PciDeviceSpecVO> newSpecs = new ArrayList<>();
        List<PciDeviceSpecVO> oldSpecs = new ArrayList<>();
        for (PciDeviceTO to : distinctTOs) {
            PciDeviceSpecVO op = pciSpecs.stream().filter(spec -> spec.matchPciDevice(to)).findFirst().orElse(null);
            if (op == null) {
                // 1. create new pci device spec
                newSpecs.add(PciDeviceTOHelper.createPciDeviceSpec(to));
                logger.debug(String.format("create new pci device spec[%s:%s:%s:%s]",
                        to.getVendorId(), to.getDeviceId(), to.getSubvendorId(), to.getSubdeviceId()));
            } else {
                // 2. update existing pci device spec
                PciDeviceTOHelper.updatePciDeviceSpec(op, to);
                oldSpecs.add(op);
                logger.debug(String.format("updated existing pci device spec[%s:%s:%s:%s]",
                        to.getVendorId(), to.getDeviceId(), to.getSubvendorId(), to.getSubdeviceId()));

                // when upgrade from 3.3.1, there may be some orphan pci device specs
                if (op.getAccountUuid() == null) {
                    dbf.persist(AccountResourceRefVO.forOwner(
                            AccountConstant.INITIAL_SYSTEM_ADMIN_UUID,
                            op.getUuid(),
                            PciDeviceSpecVO.class
                    ));
                }
            }
        }

        if (!newSpecs.isEmpty()) {
            dbf.persistCollection(newSpecs);
            pciSpecs.addAll(newSpecs);
        }

        if (!oldSpecs.isEmpty()) {
            dbf.updateCollection(oldSpecs);
        }
    }

    void doDeleteGarbagePciDeviceSpec() {
        List<String> garbagePciSpecUuids;
        List<String> pciSpecUuids = Q.New(PciDeviceVO.class)
                .notNull(PciDeviceVO_.pciSpecUuid)
                .select(PciDeviceVO_.pciSpecUuid)
                .groupBy(PciDeviceVO_.pciSpecUuid)
                .listValues();

        if (pciSpecUuids.isEmpty()) {
            garbagePciSpecUuids = Q.New(PciDeviceSpecVO.class).select(PciDeviceSpecVO_.uuid).listValues();
        } else {
            garbagePciSpecUuids = Q.New(PciDeviceSpecVO.class)
                    .select(PciDeviceSpecVO_.uuid)
                    .notIn(PciDeviceSpecVO_.uuid, pciSpecUuids)
                    .listValues();
        }

        if (!garbagePciSpecUuids.isEmpty()) {
            List<String> garbagePciSpecNames = Q.New(PciDeviceSpecVO.class)
                    .in(PciDeviceSpecVO_.uuid, garbagePciSpecUuids)
                    .select(PciDeviceSpecVO_.name)
                    .listValues();
            SQL.New(PciDeviceSpecVO.class).in(PciDeviceSpecVO_.uuid, garbagePciSpecUuids).hardDelete();
            logger.debug(String.format("deleted pci specs[uuids:%s, names:%s] because they are not existing any more",
                    garbagePciSpecUuids, garbagePciSpecNames));

            // delete VmInstancePciDeviceSpecRef
            SQL.New(VmInstancePciDeviceSpecRefVO.class).in(VmInstancePciDeviceSpecRefVO_.pciSpecUuid, garbagePciSpecUuids).hardDelete();
            logger.debug(String.format("clean up VmInstancePciDeviceSpecRefVO because pci specs[uuids:%s] vanished", garbagePciSpecUuids));
        }
    }

    void doSyncMdevDeviceSpecs() {
        final String key = "Name";
        List<MdevDeviceSpecVO> mdevSpecs = Q.New(MdevDeviceSpecVO.class).list();
        tos.stream().filter(to -> !CollectionUtils.isEmpty(to.getMdevSpecifications())).forEach(to -> {
            logger.debug("pci device mdev specification: " + to.getMdevSpecifications());
            for (Map<String, String> specItem : to.getMdevSpecifications()) {
                MdevDeviceSpecVO dbSpec = null;
                for (MdevDeviceSpecVO spec : mdevSpecs) {
                    HashMap specMap = JSONObjectUtil.toObject(spec.getSpecification(), HashMap.class);
                    if (specItem.containsKey(key) && specMap.containsKey(key) && specItem.get(key).equals(specMap.get(key))) {
                        dbSpec = spec;
                        break;
                    }
                }

                if (dbSpec == null) {
                    // 1. create new mdev specs
                    dbSpec = new MdevDeviceSpecVO();
                    dbSpec.setUuid(Platform.getUuid());
                    dbSpec.setName(specItem.getOrDefault(key, "UNKNOWN"));
                    dbSpec.setSpecification(JSONObjectUtil.toJsonString(specItem));
                    dbSpec.setType(MdevDeviceType.valueOf(to.getType().toString()));
                    dbSpec.setState(MdevDeviceSpecState.Enabled);
                    dbSpec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    dbf.persist(dbSpec);
                    logger.debug(String.format("create new mdev device spec[uuid:%s, name:%s]",
                            dbSpec.getUuid(), dbSpec.getName()));
                } else {
                    // 2. update existing mdev specs
                    dbSpec.setSpecification(JSONObjectUtil.toJsonString(specItem));
                    dbSpec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    dbf.update(dbSpec);
                    logger.debug(String.format("updated existing mdev device spec[uuid:%s, name:%s]",
                            dbSpec.getUuid(), dbSpec.getName()));
                }

                // 3. create PciDeviceMdevSpecRefVO if not exist
                boolean exists = Q.New(PciDeviceMdevSpecRefVO.class)
                        .eq(PciDeviceMdevSpecRefVO_.pciDeviceUuid, to.getUuid())
                        .eq(PciDeviceMdevSpecRefVO_.mdevSpecUuid, dbSpec.getUuid())
                        .isExists();

                if (!exists) {
                    PciDeviceMdevSpecRefVO ref = new PciDeviceMdevSpecRefVO();
                    ref.setMdevSpecUuid(dbSpec.getUuid());
                    ref.setPciDeviceUuid(to.getUuid());
                    dbf.persist(ref);
                    logger.debug(String.format("create PciDeviceMdevSpecRef for pci device[uuid:%s] and mdev spec[uuid:%s]",
                            ref.getPciDeviceUuid(), ref.getMdevSpecUuid()));
                }
            }
        });

        // 4. delete garbage mdev specs
        cleanGarbageMdevSpecInDB();
    }

    void cleanGarbageMdevSpecInDB() {
        List<String> garbageMdevSpecUuids;
        List<String> mdevSpecUuids = Q.New(PciDeviceMdevSpecRefVO.class)
                .notNull(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                .select(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                .groupBy(PciDeviceMdevSpecRefVO_.mdevSpecUuid)
                .listValues();

        if (mdevSpecUuids.isEmpty()) {
            garbageMdevSpecUuids = Q.New(MdevDeviceSpecVO.class).select(MdevDeviceSpecVO_.uuid).listValues();
        } else {
            garbageMdevSpecUuids = Q.New(MdevDeviceSpecVO.class)
                    .select(MdevDeviceSpecVO_.uuid)
                    .notIn(MdevDeviceSpecVO_.uuid, mdevSpecUuids)
                    .listValues();
        }

        if (!garbageMdevSpecUuids.isEmpty()) {
            List<String> garbageMdevSpecNames = Q.New(MdevDeviceSpecVO.class)
                    .in(MdevDeviceSpecVO_.uuid, garbageMdevSpecUuids)
                    .select(MdevDeviceSpecVO_.name)
                    .listValues();
            SQL.New(AccountResourceRefVO.class).in(AccountResourceRefVO_.resourceType, garbageMdevSpecUuids).hardDelete();
            SQL.New(MdevDeviceSpecVO.class).in(MdevDeviceSpecVO_.uuid, garbageMdevSpecUuids).hardDelete();
            logger.debug(String.format("deleted mdev specs[uuids:%s, names:%s] because they are not existing any more",
                    garbageMdevSpecUuids, garbageMdevSpecNames));

            // delete VmInstanceMdevDeviceSpecRef
            SQL.New(VmInstanceMdevDeviceSpecRefVO.class).in(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, garbageMdevSpecUuids).hardDelete();
            logger.debug(String.format("clean up VmInstanceMdevDeviceSpecRefVO because mdev specs[uuids:%s] vanished", garbageMdevSpecUuids));
        }
    }

    void afterSyncPciWithDb() {
        List<AfterSyncPciDeviceExtensionPoint> exps = pluginRgty.getExtensionList(AfterSyncPciDeviceExtensionPoint.class);
        for (AfterSyncPciDeviceExtensionPoint exp : exps) {
            exp.afterSyncPciDeviceVO(this.hostUuid, this.tos);
        }
    }

    public void syncPciWithDb(Completion completion) {
        if (CollectionUtils.isEmpty(tos)) {
            completion.success();
            return;
        }

        sendSyncPciDeviceInDbMsg(hostUuid, tos, null, SyncPciDeviceInDbMsg.SyncMode.Full, completion);
    }

    public void doSyncPciWithDb() {
        // order matters
        prepareSyncPicDevice();
        doCreateOrUpdatePciDeviceSpec();
        doSyncPciDevice();
        doDeleteGarbagePciDeviceSpec();
        doSyncMdevDeviceSpecs();
        afterSyncPciWithDb();
    }

    void syncEthPciWithDb(Completion completion) {
        sendSyncPciDeviceInDbMsg(hostUuid, tos, null, SyncPciDeviceInDbMsg.SyncMode.Ethernet, completion);
    }

    public void doSyncEthPciWithDb() {
        cleanPciWhenHostIommuDisabled();
        if (CollectionUtils.isEmpty(tos)) {
            return;
        }

        tos.removeIf(to -> !PciDeviceType.Ethernet_Controller.isEqual(to.getType()));

        if (CollectionUtils.isEmpty(tos)) {
            return;
        }

        doSyncPciWithDb();
    }

    void cleanMdevDevices() {
        boolean mDevExists = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.hostUuid, hostUuid)
                .isExists();
        if (!mDevExists) {
            return;
        }

        SQL.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.hostUuid, hostUuid)
                .hardDelete();
        logger.debug(String.format("delete all mdev devices in host[uuid:%s] due to iommu-disabled state", hostUuid));
        cleanGarbageMdevSpecInDB();
    }

    boolean anyPciDevicesExistsInDB() {
        return Q.New(PciDeviceVO.class)
                .eq(PciDeviceVO_.hostUuid, hostUuid)
                .isExists();
    }

    void cleanPciWhenHostIommuDisabled() {
        if (anyPciDevicesExistsInDB()) {
            cleanMdevDevices();
        }
    }

    private void sendSyncPciDeviceInDbMsg(String hostUuid, List<PciDeviceTO> pciDevices,
                                           List<String> pciDeviceAddresses,
                                           SyncPciDeviceInDbMsg.SyncMode syncMode,
                                           Completion completion) {
        SyncPciDeviceInDbMsg msg = new SyncPciDeviceInDbMsg();
        msg.setHostUuid(hostUuid);
        msg.setPciDevices(pciDevices);
        msg.setPciDeviceAddresses(pciDeviceAddresses);
        msg.setSyncMode(syncMode);
        bus.makeTargetServiceIdByResourceUuid(msg, PciDeviceConstants.SERVICE_ID,
                PciDeviceConstants.SYNC_PCI_DEVICE_IN_DB_SIGNATURE);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }
}
