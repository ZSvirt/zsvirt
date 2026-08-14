package org.zstack.billing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.billing.generator.GenerateAccountBillingMsg;
import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefVO;
import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefVO_;
import org.zstack.billing.spendingcalculator.vm.VmUsageVO;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageVO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageVO;
import org.zstack.billing.userconfig.BillingUserConfigUtils;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.longjob.LongJobConstants;
import org.zstack.header.longjob.SubmitLongJobMsg;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2016/6/24.
 */
public class BillingUpgradeExtension implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(BillingUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private PluginRegistry pluginRgty;

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Autowired
    private CloudBus bus;

    @Autowired
    private ThreadFacade thdf;

    @Override
    public void managementNodeReady() {
        tapResourcesForBilling();

        generatePriceVOEndDateInLong();

        generateBillings();
    }

    private void tapResourcesForBilling() {
        if (!BillingGlobalProperty.TAP_RESOURCE_FOR_BILLING) {
            return;
        }

        logger.debug("tap resources for billing");

        long now = System.currentTimeMillis();

        Iterator<List<VmUsageVO>> vmIterator = new Iterator<List<VmUsageVO>>() {
            final int STEP = 1000;

            int offset = 0;

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            @Transactional(readOnly = true)
            public List<VmUsageVO> next() {
                String sql = "select vm, ref.accountUuid" +
                        " from VmInstanceVO vm, AccountResourceRefVO ref" +
                        " where vm.state = :state" +
                        " and vm.uuid = ref.resourceUuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.type = :type";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("state", VmInstanceState.Running);
                q.setParameter("rtype", VmInstanceVO.class.getSimpleName());
                q.setParameter("type", AccessLevel.Own);
                q.setFirstResult(offset);
                q.setMaxResults(STEP);
                List<Tuple> vms = q.getResultList();

                if (vms.isEmpty()) {
                    return null;
                }

                offset += STEP;

                return vms.stream().map(t -> {
                    VmInstanceVO vm = t.get(0, VmInstanceVO.class);
                    String accountUuid = t.get(1, String.class);

                    VmUsageVO co = new VmUsageVO();
                    co.setDateInLong(now);
                    co.setAccountUuid(accountUuid);

                    VolumeVO root = vm.getAllVolumes().stream().filter(vol -> vol.getType() == VolumeType.Root).findFirst().get();
                    co.setRootVolumeSize(root.getSize());

                    co.setCpuNum(vm.getCpuNum());
                    co.setMemorySize(vm.getMemorySize());
                    co.setName(vm.getName());
                    co.setState(vm.getState().toString());
                    co.setVmUuid(vm.getUuid());
                    co.setInventory(JSONObjectUtil.toJsonString(VmInstanceInventory.valueOf(vm)));

                    return co;
                }).collect(Collectors.toList());
            }
        };

        int vmNum = 0;
        List<VmUsageVO> vmUsageCOs;
        do {
            vmUsageCOs = vmIterator.next();
            if (vmUsageCOs != null) {
                for (VmUsageVO vmUsageCO : vmUsageCOs) {
                    dbf.persist(vmUsageCO);
                }

                vmNum += vmUsageCOs.size();
            }

        } while (vmUsageCOs != null);

        logger.debug(String.format("recorded %s VMs for billing", vmNum));

        Iterator<List> volIterator = new Iterator<List>() {
            final int STEP = 1000;

            int offset = 0;

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            @Transactional(readOnly = true)
            public List next() {
                String sql = "select vol, ref.accountUuid" +
                        " from VolumeVO vol, AccountResourceRefVO ref" +
                        " where vol.status = :status" +
                        " and vol.uuid = ref.resourceUuid" +
                        " and ref.resourceType = :rtype" +
                        " and ref.type = :type";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                q.setParameter("status", VolumeStatus.Ready);
                q.setParameter("rtype", VolumeVO.class.getSimpleName());
                q.setParameter("type", AccessLevel.Own);
                q.setFirstResult(offset);
                q.setMaxResults(STEP);
                List<Tuple> vols = q.getResultList();

                if (vols.isEmpty()) {
                    return null;
                }

                offset += STEP;

                return vols.stream().map(t -> {
                    VolumeVO vol = t.get(0, VolumeVO.class);
                    String accountUuid = t.get(1, String.class);

                    if (vol.getType() == VolumeType.Root) {
                        RootVolumeUsageVO co = new RootVolumeUsageVO();
                        co.setVmUuid(vol.getVmInstanceUuid());
                        co.setInventory(JSONObjectUtil.toJsonString(VolumeInventory.valueOf(vol)));
                        co.setAccountUuid(accountUuid);
                        co.setDateInLong(now);
                        co.setVolumeName(vol.getName());
                        co.setVolumeSize(vol.getSize());
                        co.setVolumeStatus(vol.getStatus().toString());
                        co.setVolumeUuid(vol.getUuid());

                        return co;
                    } else {
                        DataVolumeUsageVO co = new DataVolumeUsageVO();
                        co.setVolumeStatus(vol.getStatus().toString());
                        co.setVolumeUuid(vol.getUuid());
                        co.setVolumeSize(vol.getSize());
                        co.setAccountUuid(accountUuid);
                        co.setDateInLong(now);
                        co.setInventory(JSONObjectUtil.toJsonString(VolumeInventory.valueOf(vol)));
                        co.setVolumeName(vol.getName());

                        return co;
                    }
                }).collect(Collectors.toList());
            }
        };

        int volNum = 0;
        List volUsageCOs;
        do {
            volUsageCOs = volIterator.next();
            if (volUsageCOs != null) {
                for (Object obj : volUsageCOs) {
                    dbf.persist(obj);
                }

                volNum += volUsageCOs.size();
            }
        } while (volUsageCOs != null);

        logger.debug(String.format("recorded %s volumes for billing", volNum));
    }

    private void generateBillings() {
        if (!BillingGlobalProperty.GENERATE_BILLS_IMMEDIATELY) {
            return;
        }

        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        smsg.setJobName(GenerateAccountBillingMsg.class.getSimpleName());
        smsg.setJobRequestUuid(Platform.getUuid());
        smsg.setJobData("{}");
        smsg.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        bus.makeLocalServiceId(smsg, LongJobConstants.SERVICE_ID);
        bus.send(smsg);
    }

    private void generatePriceVOEndDateInLong() {
        if (!BillingGlobalProperty.GENERATE_PRICE_END_DATE_IN_LONG) {
            return;
        }

        String tableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;
        if (!destinationMaker.isManagedByUs(tableUuid)) {
            return;
        }

        long count = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, tableUuid)
                .count();
        if (count == 0) {
            return;
        }

        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return String.format("priceTable-%s", tableUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                List<PriceVO> priceVOS = new ArrayList<>();
                priceVOS.addAll(getPriceVOS(BillingConstants.SPENDING_CPU));
                priceVOS.addAll(getPriceVOS(BillingConstants.SPENDING_MEMORY));
                priceVOS.addAll(getPriceVOS(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_IN));
                priceVOS.addAll(getPriceVOS(BillingConstants.SPENDING_VM_NIC_BANDWIDTH_OUT));
                priceVOS.addAll(getPriceVOS(BillingConstants.SPENDING_VIP_BANDWIDTH_IN));
                priceVOS.addAll(getPriceVOS(BillingConstants.SPENDING_VIP_BANDWIDTH_OUT));
                priceVOS.addAll(getVolumePriceVOS(BillingConstants.SPENDING_TYPE_DATA_VOLUME));
                priceVOS.addAll(getVolumePriceVOS(BillingConstants.SPENDING_TYPE_ROOT_VOLUME));
                priceVOS.addAll(getPciDevicePriceVOS());

                dbf.updateCollection(priceVOS);
                chain.next();
            }

            @Override
            public String getName() {
                return "generate-price-endDate";
            }
        });
    }

    private List<PriceVO> getPriceVOS(String priceType) {
        String tableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;

        List<PriceVO> priceVOS = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, tableUuid)
                .eq(PriceVO_.resourceName, priceType)
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .list();

        return makeEndDateInLong(priceVOS);
    }

    private List<PriceVO> getVolumePriceVOS(String priceType) {
        String tableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;

        List<PriceVO> priceVOS = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, tableUuid)
                .eq(PriceVO_.resourceName, priceType)
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .list();

        Map<String, List<PriceVO>> priceListMap = new HashMap<>();
        for (PriceVO priceVO : priceVOS) {
            String priceUserConfigKey = getPriceUserConfigKey(priceVO.getUuid());
            List<PriceVO> subPriceVOS = priceListMap.computeIfAbsent(priceUserConfigKey, k -> new ArrayList<>());
            subPriceVOS.add(priceVO);
        }

        List<PriceVO> result = new ArrayList<>();
        for(List<PriceVO> subPriceVOS : priceListMap.values()){
            result.addAll(makeEndDateInLong(subPriceVOS));
        }

        return result;
    }

    private String getPriceUserConfigKey(String priceUuid) {
        String key = null;
        if (BillingSystemTags.PRICE_USER_CONFIG.hasTag(priceUuid)) {
            key = BillingUserConfigUtils.getResourcePriceConfig(priceUuid).getPriceUserConfig().getPriceKeyName();
        }

        return key;
    }

    private List<PriceVO> getPciDevicePriceVOS() {
        String tableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;

        List<PriceVO> priceVOS = Q.New(PriceVO.class)
                .eq(PriceVO_.tableUuid, tableUuid)
                .eq(PriceVO_.resourceName, BillingConstants.SPENDING_PCI_DEVICE)
                .orderBy(PriceVO_.dateInLong, SimpleQuery.Od.DESC)
                .list();

        Map<String, List<PriceVO>> priceListMap = new HashMap<>();
        for (PriceVO priceVO : priceVOS) {
            String pciDeviceOfferingUuid = getPciDeviceOfferingUuid(priceVO.getUuid());
            List<PriceVO> subPriceVOS = priceListMap.computeIfAbsent(pciDeviceOfferingUuid, k -> new ArrayList<>());
            subPriceVOS.add(priceVO);
        }

        List<PriceVO> result = new ArrayList<>();
        for(List<PriceVO> subPriceVOS : priceListMap.values()){
            result.addAll(makeEndDateInLong(subPriceVOS));
        }

        return result;
    }

    private String getPciDeviceOfferingUuid(String priceUuid) {
        String pciDeviceOfferingUuid = Q.New(PricePciDeviceOfferingRefVO.class)
                .select(PricePciDeviceOfferingRefVO_.pciDeviceOfferingUuid)
                .eq(PricePciDeviceOfferingRefVO_.priceUuid, priceUuid)
                .findValue();
        return pciDeviceOfferingUuid;
    }

    private List<PriceVO> makeEndDateInLong(List<PriceVO> priceVOS) {
        if (priceVOS == null || priceVOS.size() < 2) {
            return new ArrayList<>();
        }

        Long lastDateInLong = null;
        for (PriceVO priceVO : priceVOS) {
            if (lastDateInLong == null) {
                lastDateInLong = priceVO.getDateInLong();
                continue;
            }

            priceVO.setEndDateInLong(lastDateInLong);
            lastDateInLong = priceVO.getDateInLong();
        }

        return priceVOS;
    }
}
