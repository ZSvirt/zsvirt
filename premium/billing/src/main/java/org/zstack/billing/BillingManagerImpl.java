package org.zstack.billing;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.billing.generator.*;
import org.zstack.billing.generator.pcidevice.PciDeviceBillingVO;
import org.zstack.billing.generator.pcidevice.PciDeviceUsageHistoryVO;
import org.zstack.billing.generator.pcidevice.PciDeviceUsageMaker;
import org.zstack.billing.generator.pubip.vip.PubIpVipBandwidthInBillingVO;
import org.zstack.billing.generator.pubip.vip.PubIpVipBandwidthOutBillingVO;
import org.zstack.billing.generator.pubip.vip.PubIpVipBandwidthUsageHistoryVO;
import org.zstack.billing.generator.pubip.vip.PubIpVipBandwidthUsageMaker;
import org.zstack.billing.generator.pubip.vmnic.PubIpVmNicBandwidthInBillingVO;
import org.zstack.billing.generator.pubip.vmnic.PubIpVmNicBandwidthOutBillingVO;
import org.zstack.billing.generator.pubip.vmnic.PubIpVmNicBandwidthUsageHistoryVO;
import org.zstack.billing.generator.pubip.vmnic.PubIpVmNicBandwidthUsageMaker;
import org.zstack.billing.generator.vm.VmUsageHistoryVO;
import org.zstack.billing.generator.vm.VmUsageMaker;
import org.zstack.billing.generator.vm.cpu.VmCPUBillingVO;
import org.zstack.billing.generator.vm.memory.VmMemoryBillingVO;
import org.zstack.billing.generator.volume.data.DataVolumeBillingVO;
import org.zstack.billing.generator.volume.data.DataVolumeUsageHistoryVO;
import org.zstack.billing.generator.volume.data.DataVolumeUsageMaker;
import org.zstack.billing.generator.volume.root.RootVolumeBillingVO;
import org.zstack.billing.generator.volume.root.RootVolumeUsageHistoryVO;
import org.zstack.billing.generator.volume.root.RootVolumeUsageMaker;
import org.zstack.billing.spendingcalculator.SpendingCalculator;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceSpending;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceUsageInventory;
import org.zstack.billing.spendingcalculator.pcidevice.PciDeviceUsageVO;
import org.zstack.billing.spendingcalculator.pcidevice.PricePciDeviceOfferingRefVO;
import org.zstack.billing.spendingcalculator.snapshot.SnapShotUsageVO;
import org.zstack.billing.spendingcalculator.snapshot.SnapshotUsageInventory;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthSpending;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageInventory;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageVO;
import org.zstack.billing.spendingcalculator.vip.PubIpVipBandwidthUsageVO_;
import org.zstack.billing.spendingcalculator.vm.VmSpending;
import org.zstack.billing.spendingcalculator.vm.VmUsageInventory;
import org.zstack.billing.spendingcalculator.vm.VmUsageVO;
import org.zstack.billing.spendingcalculator.vm.VmUsageVO_;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthSpending;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthUsageInventory;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthUsageVO;
import org.zstack.billing.spendingcalculator.vmnic.PubIpVmNicBandwidthUsageVO_;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeSpending;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageExtensionVO;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageInventory;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeUsageVO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeSpending;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageExtensionVO;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageInventory;
import org.zstack.billing.spendingcalculator.volume.root.RootVolumeUsageVO;
import org.zstack.billing.table.*;
import org.zstack.billing.userconfig.*;
import org.zstack.billing.userconfig.diskoffering.BillingDiskOfferingUserConfig;
import org.zstack.billing.userconfig.diskoffering.DiskOfferingPriceConfig;
import org.zstack.billing.userconfig.instanceoffering.BillingInstanceOfferingUserConfig;
import org.zstack.billing.userconfig.instanceoffering.InstanceOfferingPriceConfig;
import org.zstack.billing.userconfig.price.PriceUserConfig;
import org.zstack.billing.userconfig.price.ResourcePriceUserConfig;
import org.zstack.compute.vm.VmInstanceManager;
import org.zstack.compute.vm.VmNicQosConfigBackend;
import org.zstack.compute.vm.VmNicQosStruct;
import org.zstack.configuration.DiskOfferingSystemTags;
import org.zstack.configuration.InstanceOfferingSystemTags;
import org.zstack.configuration.OfferingUserConfigUtils;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.thread.*;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.billing.CalculateAccountSpendingMsg;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.userconfig.DiskOfferingUserConfigValidator;
import org.zstack.header.configuration.userconfig.InstanceOfferingUserConfigValidator;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l3.L3NetworkCategory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotStatus;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vipQos.VipQosCanonicalEvents;
import org.zstack.header.vipQos.VipQosVO;
import org.zstack.header.vipQos.VipQosVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.TakeOverResourceExtensionPoint;
import org.zstack.mevoco.MevocoGlobalProperty;
import org.zstack.network.service.vip.VipCanonicalEvents;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.pciDevice.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.SystemTagUtils;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.zstack.billing.BillingSystemTags.*;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.*;

/**
 * Created by frank on 2/23/2016.
 */
public class BillingManagerImpl extends AbstractService implements BillingManager,
        ResourceOwnerAfterChangeExtensionPoint, TakeOverResourceExtensionPoint, VmJustBeforeDeleteFromDbExtensionPoint,
        VmJustAfterDeleteFromDbExtensionPoint, Component, ManagementNodeReadyExtensionPoint,
        CreateDataVolumeExtensionPoint, InstanceOfferingUserConfigValidator, DiskOfferingUserConfigValidator {
    private static final CLogger logger = Utils.getLogger(BillingManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacade evf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private VmInstanceManager vrMgr;

    private String billingSyncControl = "billing-sync-control";

    private Map<String, EventCallback> callbacks = new HashMap<>();

    private List<SpendingCalculator> spendingCalculators;

    private List<BillingGenerator> billingGenerators;

    private List<BillingSpendingCalculator> billingSpendingCalculators;

    private List<ResourceCreateUsageExtensionPoint> createUsageExtensionPointList;

    private List<ResourceUsageMaker> resourceUsageMakers;

    @Autowired
    private ThreadFacade thdf;

    private Long intervalMinutes = null;
    private Future evaluationTask = null;
    private Lock taskLock = new ReentrantLock();

    private Future billingTask;

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
        if (msg instanceof GenerateAccountBillingMsg) {
            handle((GenerateAccountBillingMsg) msg);
        } else if (msg instanceof GenerateAccountBillingByPriceTableMsg) {
            handle((GenerateAccountBillingByPriceTableMsg) msg);
        } else if (msg instanceof CreateResourcePriceMsg) {
            handle((CreateResourcePriceMsg) msg);
        } else if (msg instanceof DeleteResourcePriceMsg) {
            handle((DeleteResourcePriceMsg) msg);
        } else if (msg instanceof CalculateAccountSpendingMsg) {
            handle((CalculateAccountSpendingMsg) msg);
        } else if (msg instanceof UpdateResourcePriceMsg) {
            handle((UpdateResourcePriceMsg) msg);
        } else if (msg instanceof EnableBillingMsg) {
            handle((EnableBillingMsg) msg);
        } else if (msg instanceof DisableBillingMsg) {
            handle((DisableBillingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateResourcePriceMsg) {
            handle((APICreateResourcePriceMsg) msg);
        } else if (msg instanceof APIUpdateResourcePriceMsg) {
            handle((APIUpdateResourcePriceMsg) msg);
        } else if (msg instanceof APICreatePriceTableMsg) {
            handle((APICreatePriceTableMsg) msg);
        } else if (msg instanceof APIAttachPriceTableToAccountMsg) {
            handle((APIAttachPriceTableToAccountMsg)msg);
        } else if (msg instanceof APIDetachPriceTableFromAccountMsg) {
            handle((APIDetachPriceTableFromAccountMsg) msg);
        } else if (msg instanceof APIUpdatePriceTableMsg) {
            handle((APIUpdatePriceTableMsg) msg);
        } else if (msg instanceof APIChangeAccountPriceTableBindingMsg) {
            handle((APIChangeAccountPriceTableBindingMsg) msg);
        } else if (msg instanceof APIDeletePriceTableMsg) {
            handle((APIDeletePriceTableMsg) msg);
        } else if (msg instanceof APIGetAccountPriceTableRefMsg) {
            handle((APIGetAccountPriceTableRefMsg) msg);
        } else if (msg instanceof APICalculateAccountSpendingMsg) {
            handle((APICalculateAccountSpendingMsg) msg);
        } else if (msg instanceof APICalculateAccountBillingSpendingMsg) {
            handle((APICalculateAccountBillingSpendingMsg) msg);
        } else if (msg instanceof APIGenerateAccountBillingMsg) {
            handle((APIGenerateAccountBillingMsg) msg);
        } else if (msg instanceof APICalculateResourceSpendingMsg) {
            handle((APICalculateResourceSpendingMsg) msg);
        } else if (msg instanceof APIDeleteResourcePriceMsg) {
            handle((APIDeleteResourcePriceMsg) msg);
        } else if (msg instanceof APIValidateInstanceOfferingUserConfigMsg) {
            handle((APIValidateInstanceOfferingUserConfigMsg) msg);
        } else if (msg instanceof APIValidateDiskOfferingUserConfigMsg) {
            handle((APIValidateDiskOfferingUserConfigMsg) msg);
        } else if (msg instanceof APIValidatePriceUserConfigMsg) {
            handle((APIValidatePriceUserConfigMsg) msg);
        } else if (msg instanceof APICleanupBillingUsageMsg) {
            handle((APICleanupBillingUsageMsg) msg);
        } else if (msg instanceof APIDeleteBillingMsg) {
            handle((APIDeleteBillingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(CalculateAccountSpendingMsg msg) {
        CalculateAccountSpendingReply reply = new CalculateAccountSpendingReply();

        AccountSpendingStruct struct = calculateAccountSpending(SpendingStruct.fromMessage(msg), msg.isTotalOnly());

        reply.setTotal(struct.getTotal());
        reply.setSpendings(struct.getSpendings());

        bus.reply(msg, reply);
    }

    private void handle(EnableBillingMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "generate-account-billing";
            }

            @Override
            public void run(SyncTaskChain chain) {
                generateUsageVOForAllResources();
                startBillingTask();
                bus.reply(msg, new MessageReply());
                chain.next();
            }

            @Override
            public String getName() {
                return "enable-billing";
            }
        });
    }

    private void handle(DisableBillingMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "generate-account-billing";
            }

            @Override
            public void run(SyncTaskChain chain) {
                truncateResourceUsageVO();
                stopBillingTask();
                bus.reply(msg, new MessageReply());
                chain.next();
            }

            @Override
            public String getName() {
                return "disable-billing";
            }
        });
    }

    private void handle(APIValidatePriceUserConfigMsg msg) {
        APIValidatePriceUserConfigEvent event = new APIValidatePriceUserConfigEvent(msg.getId());

        checkPriceUserConfig(msg.getConfig());

        bus.publish(event);
    }

    private void handle(APICleanupBillingUsageMsg msg) {
        APICleanupBillingUsageEvent event = new APICleanupBillingUsageEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "generate-account-billing";
            }

            @Override
            public void run(SyncTaskChain chain) {
                truncateResourceUsageVO();
                truncateResourceUsageHistoryVO();
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "truncate-billing-usage-table";
            }
        });
    }

    private void truncateResourceUsageHistoryVO() {
        truncateTable(VmUsageHistoryVO.class.getSimpleName());
        truncateTable(RootVolumeUsageHistoryVO.class.getSimpleName());
        truncateTable(DataVolumeUsageHistoryVO.class.getSimpleName());
        truncateTable(PciDeviceUsageHistoryVO.class.getSimpleName());
        truncateTable(PubIpVipBandwidthUsageHistoryVO.class.getSimpleName());
        truncateTable(PubIpVmNicBandwidthUsageHistoryVO.class.getSimpleName());
    }

    private void handle(APIDeleteBillingMsg msg) {
        APIDeleteBillingEvent event = new APIDeleteBillingEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "delete-billing";
            }

            @Override
            public void run(SyncTaskChain chain) {
                if (msg.getAccountUuid() == null &&
                        msg.getStartTime() == null &&
                        msg.getEndTime() == null) {
                    truncateBillingVO();
                    bus.publish(event);
                    chain.next();
                    return;
                }

                Q q = Q.New(BillingVO.class);
                if (msg.getAccountUuid() != null) {
                    q.eq(BillingVO_.accountUuid, msg.getAccountUuid());
                }
                if (msg.getStartTime() != null) {
                    q.gte(BillingVO_.createDate, new Timestamp(msg.getStartTime()));
                }
                if (msg.getEndTime() != null) {
                    q.lte(BillingVO_.createDate,  new Timestamp(msg.getEndTime()));
                }
                long count = q.count();

                q.select(BillingVO_.id);
                while (count > 0) {
                    List<String> ids = q.limit(5000).listValues();

                    SQL.New(BillingVO.class)
                            .in(BillingVO_.id, ids)
                            .delete();

                    count -= ids.size();
                }


                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "delete-billing";
            }
        });
    }

    private synchronized void truncateBillingVO() {
        truncateTable(VmCPUBillingVO.class.getSimpleName());
        truncateTable(VmMemoryBillingVO.class.getSimpleName());
        truncateTable(RootVolumeBillingVO.class.getSimpleName());
        truncateTable(DataVolumeBillingVO.class.getSimpleName());
        truncateTable(PciDeviceBillingVO.class.getSimpleName());
        truncateTable(PubIpVipBandwidthInBillingVO.class.getSimpleName());
        truncateTable(PubIpVipBandwidthOutBillingVO.class.getSimpleName());
        truncateTable(PubIpVmNicBandwidthInBillingVO.class.getSimpleName());
        truncateTable(PubIpVmNicBandwidthOutBillingVO.class.getSimpleName());
        truncateTable(BillingVO.class.getSimpleName());
    }

    private void checkPriceUserConfig(String configStr) {
        ResourcePriceUserConfig config = JSONObjectUtil.toObject(configStr, ResourcePriceUserConfig.class);

        if (config.getPriceUserConfig() == null) {
            return;
        }

        String priceConfig = config.getPriceUserConfig().getPriceKeyName();
        if (priceConfig == null) {
            throw new OperationFailureException(argerr("priceKeyName is null"));
        }
    }

    private void handle(APIValidateInstanceOfferingUserConfigMsg msg) {
        APIValidateDiskOfferingUserConfigEvent event = new APIValidateDiskOfferingUserConfigEvent(msg.getId());

        checkInstanceOfferingUserConfig(msg.getConfig());
        bus.publish(event);
    }

    private void checkInstanceOfferingUserConfig(String configStr) {
        for (InstanceOfferingUserConfigValidator ext : pluginRgty.getExtensionList(InstanceOfferingUserConfigValidator.class)) {
            ext.validateInstanceOfferingUserConfig(configStr, null);
        }
    }

    private void handle(APIValidateDiskOfferingUserConfigMsg msg) {
        APIValidateInstanceOfferingUserConfigEvent event = new APIValidateInstanceOfferingUserConfigEvent(msg.getId());

        checkDiskOfferingUserConfig(msg.getConfig());
        bus.publish(event);
    }

    private void checkDiskOfferingUserConfig(String configStr) {
        for (DiskOfferingUserConfigValidator ext : pluginRgty.getExtensionList(DiskOfferingUserConfigValidator.class)) {
            ext.validateDiskOfferingUserConfig(configStr, null);
        }
    }

    private void handle(APIDeleteResourcePriceMsg msg) {
        APIDeleteResourcePriceEvent event = new APIDeleteResourcePriceEvent(msg.getId());

        DeleteResourcePriceMsg localMsg = new DeleteResourcePriceMsg();
        localMsg.setUuid(msg.getUuid());
        localMsg.setTableUuid(msg.getTableUuid());
        localMsg.setCutoffPrice(msg.isCutoffPrice());
        bus.makeTargetServiceIdByResourceUuid(localMsg, BillingConstants.SERVICE_ID, msg.getTableUuid());

        bus.send(localMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }

                bus.publish(event);
            }
        });
    }

    private void handle(DeleteResourcePriceMsg msg) {
        PriceVO targetPriceVO = dbf.findByUuid(msg.getUuid(), PriceVO.class);
        List<GenerateAccountBillingMsg> msgs = new ArrayList<>();

        Set<String> accountUuids = new HashSet();
        List<String> uuids = Q.New(AccountPriceTableRefVO.class)
                .eq(AccountPriceTableRefVO_.tableUuid, msg.getTableUuid())
                .select(AccountPriceTableRefVO_.accountUuid)
                .listValues();
        accountUuids.addAll(uuids);
        if (BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID.equals(msg.getTableUuid())) {
            List<String> notAttachTableAccountUuids = SQL.New("select t0.uuid from AccountVO t0" +
                    " where t0.uuid not in (" +
                    " select ref.accountUuid from AccountPriceTableRefVO ref" +
                    " )", String.class)
                    .list();
            accountUuids.addAll(notAttachTableAccountUuids);
        }

        accountUuids.forEach(accountUuid -> {
            GenerateAccountBillingMsg generateAccountBillingMsg = new GenerateAccountBillingMsg();
            generateAccountBillingMsg.setAccountUuid(accountUuid);
            generateAccountBillingMsg.setSpendingType(targetPriceVO.getResourceName());
            bus.makeTargetServiceIdByResourceUuid(generateAccountBillingMsg, BillingConstants.SERVICE_ID, accountUuid);
            msgs.add(generateAccountBillingMsg);
        });

        if (msgs.isEmpty()) {
            thdf.chainSubmit(new ChainTask(msg) {
                @Override
                public String getSyncSignature() {
                    return getPriceTableSyncThreadName(msg.getTableUuid());
                }

                @Override
                public void run(SyncTaskChain chain) {
                    deletePriceVO(targetPriceVO, msg.isCutoffPrice());
                    bus.reply(msg, new DeleteResourcePriceReply());
                    chain.next();
                    return;
                }

                @Override
                public String getName() {
                    return "delete-price";
                }
            });
            return;
        }

        List<ErrorCode> erros = new ArrayList<>();
        new While<>(msgs).all((generateAccountBillingMsg, whileCompletion) -> {
            bus.send(generateAccountBillingMsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        erros.add(reply.getError());
                    }

                    whileCompletion.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                DeleteResourcePriceReply reply = new DeleteResourcePriceReply();
                if (!erros.isEmpty()) {
                    reply.setError(erros.get(0));
                    bus.reply(msg, reply);
                    return;
                }

                thdf.chainSubmit(new ChainTask(msg) {
                    @Override
                    public String getSyncSignature() {
                        return getPriceTableSyncThreadName(msg.getTableUuid());
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        deletePriceVO(targetPriceVO, msg.isCutoffPrice());
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public String getName() {
                        return "delete-price";
                    }
                });
            }
        });
    }

    private void deletePriceVO(PriceVO targetPriceVO, boolean cutoffPrice) {
        PriceVO lastPriceVO = null;
        List<PriceExtension> exts = pluginRgty.getExtensionList(PriceExtension.class);
        for (PriceExtension extension : exts) {
            if (!extension.getPriceResourceName().equals(targetPriceVO.getResourceName())) {
                continue;
            }

            String currentPriceUuid = extension.getLastPriceUuid(targetPriceVO.getUuid());
            if (currentPriceUuid != null) {
                lastPriceVO = dbf.findByUuid(currentPriceUuid, PriceVO.class);
            }
            break;
        }

        if (!cutoffPrice && lastPriceVO != null) {
            if (targetPriceVO.getEndDateInLong() == null) {
                lastPriceVO.setEndDateInLong(null);
            } else {
                lastPriceVO.setEndDateInLong(targetPriceVO.getDateInLong());
            }
        }
        PriceVO finalLastPriceVO = lastPriceVO;

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(PriceVO.class)
                        .eq(PriceVO_.uuid, targetPriceVO.getUuid())
                        .hardDelete();

                sql(ResourceVO.class)
                        .eq(ResourceVO_.uuid, targetPriceVO.getUuid())
                        .delete();

                if (finalLastPriceVO != null) {
                    merge(finalLastPriceVO);
                }
            }
        }.execute();
    }

    private AccountSpendingStruct calculateAccountSpending(SpendingStruct struct) {
        return calculateAccountSpending(struct, false);
    }

    private AccountSpendingStruct calculateAccountSpending(SpendingStruct struct, boolean totalOnly) {
        List<Spending> ret = new ArrayList<>();
        double total = 0;

        if (totalOnly) {
            Double sumFromBilling = SQL.New("select sum(bill.spending) from BillingVO bill where accountUuid = :accountUuid", Double.class)
                    .param("accountUuid", struct.getAccountUuid())
                    .find();

            if (sumFromBilling != null) {
                total += sumFromBilling;
            }
        } else {
            for (BillingSpendingCalculator cal : billingSpendingCalculators) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("ask %s to calculate spending", cal.getClass()));
                }

                Spending s = cal.calculate(struct);
                if (s != null) {
                    total += s.getSpending();
                    ret.add(s);
                }
            }
        }



        List<Spending> subSpendings = new ArrayList<>();
        for (SpendingCalculator cal : spendingCalculators) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("ask %s to calculate spending", cal.getClass()));
            }

            Spending s = cal.calculate(struct);
            if (s != null) {
                total += s.getSpending();
                subSpendings.add(s);
            }
        }
        logger.debug(String.format("ret num: %d, subSpendings num: %d", ret.size(), subSpendings.size()));

        mergeSpendingList(ret, subSpendings);

        AccountSpendingStruct accountSpendingStruct = new AccountSpendingStruct();
        accountSpendingStruct.setTotal(total);
        accountSpendingStruct.setSpendings(ret);

        return accountSpendingStruct;
    }

    private RunInQueue runInQueue(String syncSignature, int syncLevel) {
        return new RunInQueue(syncSignature, thdf, syncLevel);
    }

    private void handle(APICalculateAccountSpendingMsg msg) {
        APICalculateAccountSpendingReply reply = new APICalculateAccountSpendingReply();
        SpendingStruct structMsg = SpendingStruct.fromApiMessage(msg);

        String syncSignature = String.format("calculate-account-%s-spending", msg.getAccountUuid());
        runInQueue(syncSignature, 3)
                .name(syncSignature)
                .asyncBackup(msg)
                .run(outer -> runInQueue(billingSyncControl, BillingGlobalConfig.BILLING_SYNC_LEVEL.value(Integer.class))
                        .asyncBackup(outer)
                        .asyncBackup(msg)
                        .name(billingSyncControl).run(chain -> {
                            AccountSpendingStruct struct = calculateAccountSpending(structMsg);
                            reply.setSpending(struct.getSpendings());
                            reply.setTotal(struct.getTotal());
                            bus.reply(msg, reply);
                            chain.next();
                            outer.next();
                        }));
    }

    private void handle(APICalculateAccountBillingSpendingMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                APICalculateAccountBillingSpendingReply reply = new APICalculateAccountBillingSpendingReply();
                List<Spending> ret = new ArrayList<>();
                double total = 0;

                SpendingStruct struct = SpendingStruct.fromApiMessage(msg);
                for (BillingSpendingCalculator cal : billingSpendingCalculators) {
                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("ask %s to calculate spending", cal.getClass()));
                    }

                    Spending s = cal.calculate(struct);
                    if (s != null) {
                        total += s.getSpending();
                        if (s.getSpending() != 0) {
                            ret.add(s);
                        }
                    }
                }

                reply.setSpending(ret);
                reply.setTotal(total);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("calculate-account-%s-billing", msg.getAccountUuid());
            }

            @Override
            protected int getSyncLevel() {
                return 3;
            }
        });




    }

    private void handle(GenerateAccountBillingMsg msg) {
        GenerateAccountBillingReply reply = new GenerateAccountBillingReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "generate-account-billing";
            }

            @Override
            public void run(SyncTaskChain chain) {
                String tableUuid = Q.New(AccountPriceTableRefVO.class)
                        .select(AccountPriceTableRefVO_.tableUuid)
                        .eq(AccountPriceTableRefVO_.accountUuid, msg.getAccountUuid())
                        .findValue();
                if (tableUuid == null) {
                    tableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;
                }

                GenerateAccountBillingByPriceTableMsg generateAccountBillingMsg = new GenerateAccountBillingByPriceTableMsg();
                generateAccountBillingMsg.setSpendingType(msg.getSpendingType());
                generateAccountBillingMsg.setAccountUuid(msg.getAccountUuid());
                generateAccountBillingMsg.setPriceTableUuid(tableUuid);
                bus.makeTargetServiceIdByResourceUuid(generateAccountBillingMsg, BillingConstants.SERVICE_ID, generateAccountBillingMsg.getPriceTableUuid());

                bus.send(generateAccountBillingMsg, new CloudBusCallBack(msg) {
                    @Override
                    public void run(MessageReply rly) {
                        if (!rly.isSuccess()) {
                            reply.setError(rly.getError());
                        }
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("generate-account-%s-billing", msg.getAccountUuid());
            }
        });
    }

    private void handle(GenerateAccountBillingByPriceTableMsg msg) {
        GenerateAccountBillingByPriceTableReply reply = new GenerateAccountBillingByPriceTableReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getPriceTableSyncThreadName(msg.getPriceTableUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                for (BillingGenerator billingGenerator : billingGenerators) {
                    if (msg.getSpendingType() != null && !billingGenerator.getSpendingTypes().contains(msg.getSpendingType())) {
                        continue;
                    }

                    billingGenerator.generate(msg.getAccountUuid());
                }

                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "generate-account-billing-by-price-table";
            }
        });
    }

    private void handle(APIGenerateAccountBillingMsg msg) {
        APIGenerateAccountBillingEvent event = new APIGenerateAccountBillingEvent(msg.getId());

        GenerateAccountBillingMsg generateAccountBillingMsg = new GenerateAccountBillingMsg();
        generateAccountBillingMsg.setAccountUuid(msg.getAccountUuid());
        bus.makeTargetServiceIdByResourceUuid(generateAccountBillingMsg, BillingConstants.SERVICE_ID, msg.getAccountUuid());

        bus.send(generateAccountBillingMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }

                bus.publish(event);
            }
        });
    }

    private void mergeSpendingList(List<Spending> source, List<Spending> target) {
        if (target == null || target.isEmpty()) {
            return;
        }

        for (Spending targetSpending : target) {
            Optional<Spending> opt = source.stream().filter(s -> targetSpending.getSpendingType().equals(s.getSpendingType()))
                    .findAny();
            if (opt.isPresent()) {
                Spending s = opt.get();
                mergeSpending(s, targetSpending);
            } else {
                source.add(targetSpending);
            }
        }
    }

    private void mergeSpending(Spending source, Spending target) {
        if (target.getDetails() == null || target.getDetails().isEmpty()) {
            return;
        }

        if (source.getDetails() == null || source.getDetails().isEmpty()) {
            source.setDetails(target.getDetails());
            target.getDetails().forEach(source::addHypervisorTypeSpending);
            return;
        }

        List<SpendingDetails> tmps = new ArrayList<>();
        for (SpendingDetails targetDetails : target.getDetails()) {
            Optional<SpendingDetails> opt = source.getDetails().stream().filter(sourceDetails -> targetDetails.getResourceUuid().equals(sourceDetails.getResourceUuid()))
                    .findAny();
            if (opt.isPresent()) {
                SpendingDetails sourceDetails = opt.get();
                mergeSpendingDetails(sourceDetails, targetDetails);
            } else {
                tmps.add(targetDetails);
            }
            source.addHypervisorTypeSpending(targetDetails);
        }

        source.getDetails().addAll(tmps);
    }

    private void mergeSpendingDetails(SpendingDetails sourceSpending, SpendingDetails targetSpending) {
        sourceSpending.setSpending(sourceSpending.getSpending() + targetSpending.getSpending());
        sourceSpending.setHypervisorType(targetSpending.getHypervisorType());

        if (sourceSpending instanceof DataVolumeSpending) {
            mergeSpendingDetails((DataVolumeSpending) sourceSpending, (DataVolumeSpending) targetSpending);
        } else if (sourceSpending instanceof RootVolumeSpending) {
            mergeSpendingDetails((RootVolumeSpending) sourceSpending, (RootVolumeSpending) targetSpending);
        } else if (sourceSpending instanceof PciDeviceSpending) {
            mergeSpendingDetails((PciDeviceSpending) sourceSpending, (PciDeviceSpending) targetSpending);
        } else if (sourceSpending instanceof PubIpVipBandwidthSpending) {
            mergeSpendingDetails((PubIpVipBandwidthSpending) sourceSpending, (PubIpVipBandwidthSpending) targetSpending);
        } else if (sourceSpending instanceof PubIpVmNicBandwidthSpending) {
            mergeSpendingDetails((PubIpVmNicBandwidthSpending) sourceSpending, (PubIpVmNicBandwidthSpending) targetSpending);
        } else if (sourceSpending instanceof VmSpending) {
            mergeSpendingDetails((VmSpending) sourceSpending, (VmSpending) targetSpending);
        } else {
            DebugUtils.Assert(false, String.format("Unexpected SpendingDetails type: %s", sourceSpending.getClass().getName()));
        }
    }

    private void mergeSpendingDetails(DataVolumeSpending sourceSpending, DataVolumeSpending targetSpending) {
        sourceSpending.getSizeInventory().addAll(targetSpending.getSizeInventory());
    }

    private void mergeSpendingDetails(RootVolumeSpending sourceSpending, RootVolumeSpending targetSpending) {
        sourceSpending.getSizeInventory().addAll(targetSpending.getSizeInventory());
    }

    private void mergeSpendingDetails(PciDeviceSpending sourceSpending, PciDeviceSpending targetSpending) {
        sourceSpending.getSizeInventory().addAll(targetSpending.getSizeInventory());
    }

    private void mergeSpendingDetails(PubIpVipBandwidthSpending sourceSpending, PubIpVipBandwidthSpending targetSpending) {
        if (targetSpending.getBandwidthInInventory() != null) {
            if (sourceSpending.getBandwidthInInventory() == null) {
                sourceSpending.setBandwidthInInventory(targetSpending.getBandwidthInInventory());
            } else {
                sourceSpending.getBandwidthInInventory().addAll(targetSpending.getBandwidthInInventory());
            }
        }

        if (targetSpending.getBandwidthOutInventory() != null) {
            if (sourceSpending.getBandwidthOutInventory() == null) {
                sourceSpending.setBandwidthOutInventory(targetSpending.getBandwidthOutInventory());
            } else {
                sourceSpending.getBandwidthOutInventory().addAll(targetSpending.getBandwidthOutInventory());
            }
        }
    }

    private void mergeSpendingDetails(PubIpVmNicBandwidthSpending sourceSpending, PubIpVmNicBandwidthSpending targetSpending) {
        if (targetSpending.getBandwidthInInventory() != null) {
            if (sourceSpending.getBandwidthInInventory() == null) {
                sourceSpending.setBandwidthInInventory(targetSpending.getBandwidthInInventory());
            } else {
                sourceSpending.getBandwidthInInventory().addAll(targetSpending.getBandwidthInInventory());
            }
        }

        if (targetSpending.getBandwidthOutInventory() != null) {
            if (sourceSpending.getBandwidthOutInventory() == null) {
                sourceSpending.setBandwidthOutInventory(targetSpending.getBandwidthOutInventory());
            } else {
                sourceSpending.getBandwidthOutInventory().addAll(targetSpending.getBandwidthOutInventory());
            }
        }
    }

    private void mergeSpendingDetails(VmSpending sourceSpending, VmSpending targetSpending) {
        if (targetSpending.getCpuInventory() != null) {
            if (sourceSpending.getCpuInventory() == null) {
                sourceSpending.setCpuInventory(targetSpending.getCpuInventory());
            } else {
                sourceSpending.getCpuInventory().addAll(targetSpending.getCpuInventory());
            }
        }

        if (targetSpending.getMemoryInventory() != null) {
            if (sourceSpending.getMemoryInventory() == null) {
                sourceSpending.setMemoryInventory(targetSpending.getMemoryInventory());
            } else {
                sourceSpending.getMemoryInventory().addAll(targetSpending.getMemoryInventory());
            }
        }
    }

    private void handle(APICalculateResourceSpendingMsg msg) {
        Pair<List<ResourceSpending>, Pagination> spendings = ResourceSpendingHelper.getResourceSpendings(
                msg.getResourceType(), msg.getResourceUuid(),
                msg.getDateStart(), msg.getDateEnd(),
                msg.getStart(), msg.getLimit());

        APICalculateResourceSpendingReply reply = new APICalculateResourceSpendingReply();
        reply.setSpending(spendings.first());
        reply.setPagination(spendings.second());
        bus.reply(msg, reply);
    }

    private void handle(APICreateResourcePriceMsg msg) {
        APICreateResourcePriceEvent event = new APICreateResourcePriceEvent(msg.getId());

        CreateResourcePriceMsg localMsg = new CreateResourcePriceMsg();
        localMsg.setPriceTableUuid(msg.getTableUuid());
        localMsg.setPrice(msg.getPrice());
        localMsg.setDateInLong(msg.getDateInLong());
        localMsg.setResourceName(msg.getResourceName());
        localMsg.setResourceUnit(msg.getResourceUnit());
        localMsg.setTimeUnit(msg.getTimeUnit());
        localMsg.setSystemTags(msg.getSystemTags());
        bus.makeTargetServiceIdByResourceUuid(localMsg, BillingConstants.SERVICE_ID, msg.getTableUuid());

        bus.send(localMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                CreateResourcePriceReply rly = (CreateResourcePriceReply) reply;
                PriceInventory inventory = rly.getInventory();
                tagMgr.createTagsFromAPICreateMessage(msg, inventory.getUuid(), PriceVO.class.getSimpleName());
                event.setInventory(inventory);
                bus.publish(event);
            }
        });
    }

    private void handle(CreateResourcePriceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getPriceTableSyncThreadName(msg.getPriceTableUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                String tableUuid = msg.getPriceTableUuid();
                long priceDateInLong = msg.getDateInLong() == null ? System.currentTimeMillis() : msg.getDateInLong();

                final PriceVO vo = new PriceVO();
                vo.setUuid(Platform.getUuid());
                vo.setResourceName(msg.getResourceName());
                vo.setPrice(msg.getPrice());
                vo.setResourceUnit(msg.getResourceUnit());
                vo.setTimeUnit(msg.getTimeUnit());
                vo.setDateInLong(priceDateInLong);
                vo.setTableUuid(tableUuid);

                PriceVO currentPriceVO = null;
                List<PriceExtension> exts = pluginRgty.getExtensionList(PriceExtension.class);
                for (PriceExtension extension : exts) {
                    if (!extension.getPriceResourceName().equals(msg.getResourceName())) {
                        continue;
                    }

                    String currentPriceUuid = extension.getCurrentPriceUuid(tableUuid, msg.getSystemTags());
                    if (currentPriceUuid != null) {
                        currentPriceVO = dbf.findByUuid(currentPriceUuid, PriceVO.class);
                    }
                    break;
                }

                if (currentPriceVO != null) {
                    if (currentPriceVO.getDateInLong() > priceDateInLong) {
                        throw new OperationFailureException(argerr(
                                "dateInLong is less than %s", currentPriceVO.getDateInLong()));
                    }

                    if (currentPriceVO.getEndDateInLong() == null) {
                        currentPriceVO.setEndDateInLong(priceDateInLong);
                    }
                }

                PriceVO lastPriceVO = currentPriceVO;
                PriceVO finalVo = new SQLBatchWithReturn<PriceVO>() {
                    @Override
                    protected PriceVO scripts() {
                        persist(vo);

                        ResourceVO resourceVO = new ResourceVO(list(vo.getUuid(), vo.getResourceName(), PriceVO.class.getSimpleName()).toArray());
                        resourceVO.setConcreteResourceType(PriceVO.class.getName());
                        persist(resourceVO);

                        if (lastPriceVO != null) {
                            merge(lastPriceVO);
                        }

                        if (BillingConstants.SPENDING_PCI_DEVICE.equalsIgnoreCase(msg.getResourceName())) {
                            PricePciDeviceOfferingRefVO ref = new PricePciDeviceOfferingRefVO();
                            ref.setPriceUuid(vo.getUuid());
                            ref.setPciDeviceOfferingUuid(SystemTagUtils.findTagValue(msg.getSystemTags(), PRICE_GPU_OFFERING_UUID, PRICE_GPU_OFFERING_UUID_TOKEN));
                            persist(ref);
                        }

                        return reload(vo);
                    }
                }.execute();

                CreateResourcePriceReply reply = new CreateResourcePriceReply();
                reply.setInventory(PriceInventory.valueOf(finalVo));
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "create-price";
            }
        });
    }

    private void handle(APIUpdateResourcePriceMsg msg) {
        APIUpdateResourcePriceEvent event = new APIUpdateResourcePriceEvent(msg.getId());

        String tableUuid = Q.New(PriceVO.class)
                .select(PriceVO_.tableUuid)
                .eq(PriceVO_.uuid, msg.getUuid())
                .findValue();

        UpdateResourcePriceMsg localMsg = new UpdateResourcePriceMsg();
        localMsg.setUuid(msg.getUuid());
        localMsg.setEndDateInLong(msg.getEndDateInLong());
        localMsg.setSetEndDateInLongBaseOnCurrentTime(msg.isSetEndDateInLongBaseOnCurrentTime());
        bus.makeTargetServiceIdByResourceUuid(localMsg, BillingConstants.SERVICE_ID, tableUuid);

        bus.send(localMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                UpdateResourcePriceReply rly = (UpdateResourcePriceReply) reply;
                event.setInventory(rly.getInventory());
                bus.publish(event);
            }
        });
    }

    private void handle(UpdateResourcePriceMsg msg) {
        String tableUuid = Q.New(PriceVO.class)
                .select(PriceVO_.tableUuid)
                .eq(PriceVO_.uuid, msg.getUuid())
                .findValue();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getPriceTableSyncThreadName(tableUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                PriceVO priceVO = dbf.findByUuid(msg.getUuid(), PriceVO.class);
                boolean flag = false;
                if (msg.getEndDateInLong() != null) {
                    priceVO.setEndDateInLong(msg.getEndDateInLong());
                    flag = true;
                }

                if (flag) {
                    dbf.updateAndRefresh(priceVO);
                }

                UpdateResourcePriceReply reply = new UpdateResourcePriceReply();
                reply.setInventory(PriceInventory.valueOf(priceVO));
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return "update-price";
            }
        });
    }

    private void handle(APICreatePriceTableMsg msg) {
        APICreatePriceTableEvent event = new APICreatePriceTableEvent(msg.getId());

        String uuid = msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid();
        PriceTableVO priceTableVO = new PriceTableVO();
        priceTableVO.setUuid(uuid);
        priceTableVO.setName(msg.getName());
        priceTableVO.setDescription(msg.getDescription());
        List<PriceVO> priceVOS = new ArrayList<>();
        List<PricePciDeviceOfferingRefVO> pricePciDeviceOfferingRefVOS = new ArrayList<>();
        Map<String, List<String>> priceVOSystemTagMap = new HashMap<>();

        List<APICreatePriceTableMsg.Price> prices = msg.getPrices();
        for (APICreatePriceTableMsg.Price price : prices) {
            PriceVO vo = new PriceVO();
            vo.setUuid(Platform.getUuid());
            vo.setTableUuid(uuid);
            vo.setResourceName(price.getResourceName());
            vo.setPrice(price.getPrice());
            vo.setResourceUnit(price.getResourceUnit());
            vo.setTimeUnit(price.getTimeUnit());
            vo.setDateInLong(price.getDateInLong() == null ? System.currentTimeMillis() : price.getDateInLong());
            priceVOS.add(vo);

            if (BillingConstants.SPENDING_PCI_DEVICE.equalsIgnoreCase(price.getResourceName())) {
                PricePciDeviceOfferingRefVO ref = new PricePciDeviceOfferingRefVO();
                ref.setPriceUuid(vo.getUuid());
                ref.setPciDeviceOfferingUuid(SystemTagUtils.findTagValue(price.getSystemTags(), PRICE_GPU_OFFERING_UUID, PRICE_GPU_OFFERING_UUID_TOKEN));
                pricePciDeviceOfferingRefVOS.add(ref);
            }

            priceVOSystemTagMap.put(vo.getUuid(), price.getSystemTags());
        }

        PriceTableVO finalVo = new SQLBatchWithReturn<PriceTableVO>() {
            @Override
            protected PriceTableVO scripts() {
                persist(priceTableVO);

                for (PriceVO priceVO : priceVOS) {
                    persist(priceVO);

                    ResourceVO resourceVO = new ResourceVO(new String[]{priceVO.getUuid(), priceVO.getResourceName(), PriceVO.class.getSimpleName()});
                    resourceVO.setConcreteResourceType(PriceVO.class.getName());
                    persist(resourceVO);

                    tagMgr.createTags(priceVOSystemTagMap.get(priceVO.getUuid()),
                            null, priceVO.getUuid(),
                            PriceVO.class.getSimpleName());
                }

                for (PricePciDeviceOfferingRefVO refVO : pricePciDeviceOfferingRefVOS) {
                    persist(refVO);
                }

                return reload(priceTableVO);
            }
        }.execute();

        tagMgr.createTagsFromAPICreateMessage(msg, priceTableVO.getUuid(), PriceTableVO.class.getSimpleName());

        event.setInventory(PriceTableInventory.valueOf(finalVo));
        bus.publish(event);
    }

    private String getPriceTableSyncThreadName(String priceTableUuid) {
        return String.format("priceTable-%s", priceTableUuid);
    }

    private void handle(APIAttachPriceTableToAccountMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getPriceTableSyncThreadName(msg.getTableUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIAttachPriceTableToAccountEvent event = new APIAttachPriceTableToAccountEvent(msg.getId());

                AccountPriceTableRefVO refVO = new AccountPriceTableRefVO();
                refVO.setTableUuid(msg.getTableUuid());
                refVO.setAccountUuid(msg.getAccountUuid());
                dbf.persist(refVO);

                PriceTableVO tableVO = dbf.findByUuid(msg.getTableUuid(), PriceTableVO.class);
                event.setInventory(PriceTableInventory.valueOf(tableVO));
                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "attach-price-table-to-account";
            }
        });
    }

    private void handle(APIDetachPriceTableFromAccountMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getPriceTableSyncThreadName(msg.getTableUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIDetachPriceTableFromAccountEvent event = new APIDetachPriceTableFromAccountEvent(msg.getId());

                boolean exists = Q.New(AccountPriceTableRefVO.class)
                        .eq(AccountPriceTableRefVO_.accountUuid, msg.getAccountUuid())
                        .eq(AccountPriceTableRefVO_.tableUuid, msg.getTableUuid())
                        .isExists();
                if (!exists) {
                    bus.publish(event);
                    return;
                }

                SQL.New(AccountPriceTableRefVO.class)
                        .eq(AccountPriceTableRefVO_.accountUuid, msg.getAccountUuid())
                        .eq(AccountPriceTableRefVO_.tableUuid, msg.getTableUuid())
                        .hardDelete();

                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "detach-price-table-from-account";
            }
        });
    }

    private void handle(APIChangeAccountPriceTableBindingMsg msg) {
        APIChangeAccountPriceTableBindingEvent event = new APIChangeAccountPriceTableBindingEvent(msg.getId());

        GenerateAccountBillingMsg generateAccountBillingMsg = new GenerateAccountBillingMsg();
        generateAccountBillingMsg.setAccountUuid(msg.getAccountUuid());
        bus.makeTargetServiceIdByResourceUuid(generateAccountBillingMsg, BillingConstants.SERVICE_ID, msg.getAccountUuid());
        bus.send(generateAccountBillingMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                AccountPriceTableRefVO refVO = new AccountPriceTableRefVO();
                refVO.setTableUuid(msg.getTableUuid());
                refVO.setAccountUuid(msg.getAccountUuid());

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        sql(AccountPriceTableRefVO.class)
                                .eq(AccountPriceTableRefVO_.accountUuid, msg.getAccountUuid())
                                .hardDelete();

                        persist(refVO);
                    }
                }.execute();

                PriceTableVO tableVO = dbf.findByUuid(msg.getTableUuid(), PriceTableVO.class);
                event.setInventory(PriceTableInventory.valueOf(tableVO));
                bus.publish(event);
            }
        });
    }

    private void handle(APIUpdatePriceTableMsg msg) {
        APIUpdatePriceTableEvent event = new APIUpdatePriceTableEvent(msg.getId());

        PriceTableVO tableVO = dbf.findByUuid(msg.getUuid(), PriceTableVO.class);
        boolean flag = false;
        if (msg.getName() != null) {
            tableVO.setName(msg.getName());
            flag = true;
        }
        if (msg.getDescription() != null) {
            tableVO.setDescription(msg.getDescription());
            flag = true;
        }
        if (flag) {
            dbf.updateAndRefresh(tableVO);
        }

        event.setInventory(PriceTableInventory.valueOf(tableVO));
        bus.publish(event);
    }

    private void handle(APIDeletePriceTableMsg msg) {
        APIDeletePriceTableEvent event = new APIDeletePriceTableEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getPriceTableSyncThreadName(msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        List<String> priceUuids = q(PriceVO.class)
                                .select(PriceVO_.uuid)
                                .eq(PriceVO_.tableUuid, msg.getUuid())
                                .listValues();

                        if (!priceUuids.isEmpty()) {
                            sql(PriceVO.class)
                                    .in(PriceVO_.uuid, priceUuids)
                                    .delete();
                            sql(ResourceVO.class)
                                    .in(ResourceVO_.uuid, priceUuids)
                                    .delete();
                        }

                        sql(PriceTableVO.class)
                                .eq(PriceTableVO_.uuid, msg.getUuid())
                                .delete();
                    }
                }.execute();

                bus.publish(event);
                chain.next();
            }

            @Override
            public String getName() {
                return "delete-price-table";
            }
        });
    }

    private void handle(APIGetAccountPriceTableRefMsg msg) {
        APIGetAccountPriceTableRefReply reply = new APIGetAccountPriceTableRefReply();
        String tableUuid = msg.getTableUuid();
        String accountUuid = msg.getAccountUuid();

        if (tableUuid != null) {
            Set<String> accountUuids = new HashSet<>();
            List<String> uuids = Q.New(AccountPriceTableRefVO.class)
                    .select(AccountPriceTableRefVO_.accountUuid)
                    .eq(AccountPriceTableRefVO_.tableUuid, tableUuid)
                    .listValues();
            accountUuids.addAll(uuids);

            if (BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID.equals(tableUuid)) {
                uuids = SQL.New("select uuid from AccountVO where uuid not in (select accountUuid from AccountPriceTableRefVO)")
                        .list();
                accountUuids.addAll(uuids);
            }

            reply.setAccountUuids(new ArrayList<>(accountUuids));
        }

        if (msg.getAccountUuid() != null) {
            String refTableUuid = Q.New(AccountPriceTableRefVO.class)
                    .select(AccountPriceTableRefVO_.tableUuid)
                    .eq(AccountPriceTableRefVO_.accountUuid, accountUuid)
                    .findValue();
            if (refTableUuid == null) {
                refTableUuid = BillingConstants.GLOBAL_DEFAULT_PRICE_TABLE_UUID;
            }
            reply.setTableUuid(refTableUuid);
        }

        bus.reply(msg,reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BillingConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        spendingCalculators = pluginRgty.getExtensionList(SpendingCalculator.class);
        billingGenerators = pluginRgty.getExtensionList(BillingGenerator.class);
        billingSpendingCalculators = pluginRgty.getExtensionList(BillingSpendingCalculator.class);
        createUsageExtensionPointList = pluginRgty.getExtensionList(ResourceCreateUsageExtensionPoint.class);
        resourceUsageMakers = Arrays.asList(
                new VmUsageMaker(), new PciDeviceUsageMaker(),
                new PubIpVipBandwidthUsageMaker(), new PubIpVmNicBandwidthUsageMaker(),
                new RootVolumeUsageMaker(), new DataVolumeUsageMaker()
                );

        setupEvents();

        BillingGlobalConfig.BILLING_ENABLE.installLocalUpdateExtension(new GlobalConfigUpdateExtensionPoint(){
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (newConfig.value(Boolean.class)) {
                    EnableBillingMsg msg = new EnableBillingMsg();
                    bus.makeTargetServiceIdByResourceUuid(msg, BillingConstants.SERVICE_ID, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    bus.send(msg, new CloudBusCallBack(null) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.error(String.format("enable billing failed, %s", reply.getError().getDetails()));
                            }
                        }
                    });
                    return;
                }

                DisableBillingMsg msg = new DisableBillingMsg();
                bus.makeTargetServiceIdByResourceUuid(msg, BillingConstants.SERVICE_ID, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                bus.send(msg, new CloudBusCallBack(null) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.error(String.format("disable billing failed, %s", reply.getError().getDetails()));
                        }
                    }
                });
            }
        });

        BillingGlobalConfig.BILLING_ENABLE.updateValue(false);

        return true;
    }

    @Override
    public void managementNodeReady() {
        BillingGlobalConfig.BILLING_GENERATION_INTERVAL_DAY.installUpdateExtension((oldConfig, newConfig) -> startBillingTask());

        BillingGlobalConfig.BILLING_GENERATION_HOUR_POINT.installUpdateExtension((oldConfig, newConfig) -> startBillingTask());

        if (BillingGlobalConfig.BILLING_ENABLE.value(Boolean.class)) {
            startBillingTask();
        }

    }

    private synchronized void startBillingTask() {
        if (billingTask != null) {
            billingTask.cancel(true);
        }

        long interval = TimeUnit.DAYS.toMinutes(BillingGlobalConfig.BILLING_GENERATION_INTERVAL_DAY.value(Long.class));
        long delay = TimeUnit.MILLISECONDS.toMinutes(getBillingTaskDelayMills());
        billingTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "generate-bills-on-time";
            }

            @Override
            public void run() {
                generateBillings();
            }
        }, delay);
    }

    private void stopBillingTask() {
        if (billingTask != null) {
            billingTask.cancel(true);
        }
    }

    private synchronized void truncateResourceUsageVO() {
        truncateTable(VmUsageVO.class.getSimpleName());
        truncateTable(RootVolumeUsageExtensionVO.class.getSimpleName());
        truncateTable(DataVolumeUsageExtensionVO.class.getSimpleName());
        truncateTable(PciDeviceUsageVO.class.getSimpleName());
        truncateTable(SnapShotUsageVO.class.getSimpleName());
        truncateTable(PubIpVipBandwidthUsageVO.class.getSimpleName());
        truncateTable(PubIpVmNicBandwidthUsageVO.class.getSimpleName());

        //truncateTable(RootVolumeUsageVO.class.getSimpleName());
        nativeDeleteTable(RootVolumeUsageVO.class.getSimpleName());

        //truncateTable(DataVolumeUsageVO.class.getSimpleName());
        nativeDeleteTable(DataVolumeUsageVO.class.getSimpleName());
    }

    private void generateUsageVOForAllResources() {
        for (ResourceUsageMaker maker : resourceUsageMakers) {
            Long count = Q.New(maker.getResourceVOClass()).count();
            SQL.New(String.format("select vo.uuid from %s vo", maker.getResourceVOClass().getSimpleName()), String.class)
                    .limit(1000).paginate(count, (List<String> resourceUuids) -> {
                List<Usage> usages = maker.make(resourceUuids);
                dbf.persistCollection(usages);
            });
        }
    }

    @Transactional
    private void truncateTable(String tableName) {
        Query q = dbf.getEntityManager().createNativeQuery(String.format("truncate table %s", tableName));
        q.executeUpdate();
    }

    @Transactional
    private void nativeDeleteTable(String tableName) {
        Query q = dbf.getEntityManager().createNativeQuery(String.format("delete from %s", tableName));
        q.executeUpdate();
    }

    private long getBillingTaskDelayMills() {
        int hour = BillingGlobalConfig.BILLING_GENERATION_HOUR_POINT.value(Integer.class);

        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.add(Calendar.HOUR, hour);

        long current = System.currentTimeMillis();
        if (date.getTime().getTime() < current) {
            date.add(Calendar.DATE, 1);
        }

        long result = date.getTime().getTime() - current;
        return result;
    }

    private void generateBillings() {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "generate-account-billing";
            }

            @Override
            public void run(SyncTaskChain chain) {
                List<AccountVO> accountVOS = Q.New(AccountVO.class).list();

                for (AccountVO accountVO : accountVOS) {
                    if (!destinationMaker.isManagedByUs(accountVO.getUuid())) {
                        continue;
                    }

                    for (BillingGenerator billingGenerator : billingGenerators) {
                        billingGenerator.generate(accountVO.getUuid());
                    }
                }

                chain.next();
            }

            @Override
            public String getName() {
                return String.format("generate-account-billing");
            }
        });
    }

    private void withDrawEvents() {
        logger.debug(String.format("withdraw callbacks: %s", callbacks.keySet()));
        for(EventCallback c: callbacks.values()) {
            evf.off(c);
        }
    }

    private void setupEvents() {
        callbacks.put(VmCanonicalEvents.VM_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmCanonicalEvents.VmStateChangedData d = (VmCanonicalEvents.VmStateChangedData) data;
                if (destinationMaker.isManagedByUs(d.getVmUuid())) {
                    handleVmUsage(d);
                }
            }
        });

        callbacks.put(VmCanonicalEvents.VM_CONFIG_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmCanonicalEvents.VmConfigChangedData d = (VmCanonicalEvents.VmConfigChangedData) data;
                if (destinationMaker.isManagedByUs(d.getVmUuid())) {
                    handleVmConfigChanged(d);
                }
            }
        });

        callbacks.put(VolumeCanonicalEvents.VOLUME_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VolumeCanonicalEvents.VolumeStatusChangedData d = (VolumeCanonicalEvents.VolumeStatusChangedData) data;
                if (destinationMaker.isManagedByUs(d.getVolumeUuid())) {
                    handleVolumeUsage(d);
                }
            }
        });

        callbacks.put(VolumeCanonicalEvents.VOLUME_CONFIG_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VolumeCanonicalEvents.VolumeConfigChangedData d = (VolumeCanonicalEvents.VolumeConfigChangedData) data;
                if (destinationMaker.isManagedByUs(d.getInventory().getUuid())) {
                    handleVolumeConfigChanged(d);
                }
            }
        });

        callbacks.put(SnapShotCanonicalEvents.SNAPSHOT_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                SnapShotCanonicalEvents.SnapShotStatusChangedData d = (SnapShotCanonicalEvents.SnapShotStatusChangedData) data;
                if (destinationMaker.isManagedByUs(d.getSnapShotUuid())) {
                    handleSnapShotUsage(d);
                }
            }
        });

        callbacks.put(PciDeviceCanonicalEvents.PCIDEVICE_FULL_STATE_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                PciDeviceCanonicalEvents.PciDeviceStateChangedData d = (PciDeviceCanonicalEvents.PciDeviceStateChangedData) data;
                if (destinationMaker.isManagedByUs(d.getPciDeviceUuid())) {
                    handleGpuUsage(d);
                }
            }
        });

        callbacks.put(VipCanonicalEvents.VIP_CREATED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VipCanonicalEvents.VipEventData d = (VipCanonicalEvents.VipEventData) data;
                if (destinationMaker.isManagedByUs(d.getVipUuid())) {
                    handleVipBandwidthUsage(d);
                }
            }
        });

        callbacks.put(VipCanonicalEvents.VIP_DELETED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VipCanonicalEvents.VipEventData d = (VipCanonicalEvents.VipEventData) data;
                if (destinationMaker.isManagedByUs(d.getVipUuid())) {
                    handleVipBandwidthUsage(d);
                }
            }
        });

        callbacks.put(VipQosCanonicalEvents.VIP_QOS_CHANGE_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VipCanonicalEvents.VipEventData d = (VipCanonicalEvents.VipEventData) data;
                if (destinationMaker.isManagedByUs(d.getVipUuid())) {
                    handleVipBandwidthUsage(d);
                }
            }
        });

        callbacks.put(VmNicQosCanonicalEvents.VM_NIC_QOS_CHANGE_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmNicQosCanonicalEvents.VmNicQosEventData d = (VmNicQosCanonicalEvents.VmNicQosEventData) data;
                if (destinationMaker.isManagedByUs(d.getInventory().getUuid())) {
                    handleVmNicBandwidthUsage(d);
                }
            }
        });

        callbacks.put(VmNicCanonicalEvents.VM_NIC_CREATED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmNicCanonicalEvents.VmNicEventData d = (VmNicCanonicalEvents.VmNicEventData) data;
                if (destinationMaker.isManagedByUs(d.getInventory().getUuid())) {
                    handleVmNicBandwidthUsage(d);
                }
            }
        });

        callbacks.put(VmNicCanonicalEvents.VM_NIC_DELETED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VmNicCanonicalEvents.VmNicEventData d = (VmNicCanonicalEvents.VmNicEventData) data;
                if (destinationMaker.isManagedByUs(d.getInventory().getUuid())) {
                    handleVmNicBandwidthUsage(d);
                }
            }
        });

        callbacks.forEach((k, v) -> evf.on(k, v));
    }


    private void handleSnapShotUsage(SnapShotCanonicalEvents.SnapShotStatusChangedData d) {
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(d.getSnapShotUuid());
        VolumeSnapshotVO vo = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, d.getSnapShotUuid()).find();
        VolumeSnapshotInventory vsi = d.getInventory();
        SnapShotUsageVO ssVo = new SnapShotUsageVO();

        if (accountUuid == null) {
            logger.warn(String.format("Billing:cannot find account uuid for the volume[uuid:%s, name:%s]",
                    d.getSnapShotUuid(), d.getInventory().getName()));
            return;
        }
        if (vo == null) {
            ssVo.setAccountUuid(accountUuid);
            ssVo.setDateInLong(System.currentTimeMillis());
            ssVo.setSnapshotName(vsi.getName());
            ssVo.setVolumeUuid(vsi.getVolumeUuid());
            ssVo.setInventory(JSONObjectUtil.toJsonString(vsi));
            ssVo.setSnapshotSize(vsi.getSize());
            ssVo.setSnapshotStatus(VolumeSnapshotStatus.Deleted.toString());
            dbf.update(ssVo);
        } else if (!VolumeSnapshotStatus.Ready.toString().equals(d.getNewStatus())) {
            return;
        } else {
            ssVo.setSnapshotUuid(vo.getUuid());
            ssVo.setAccountUuid(accountUuid);
            ssVo.setDateInLong(System.currentTimeMillis());
            ssVo.setSnapshotName(vsi.getName());
            ssVo.setVolumeUuid(vsi.getVolumeUuid());
            ssVo.setInventory(JSONObjectUtil.toJsonString(vsi));
            ssVo.setSnapshotSize(vsi.getSize());
            ssVo.setSnapshotStatus(d.getNewStatus());
            persistUsage(ssVo);
        }
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[SnapShot Usage]: %s", JSONObjectUtil.toJsonString(SnapshotUsageInventory.valueOf(ssVo))));
        }

    }

    private void persistUsage(Usage usage) {
        if (!BillingGlobalConfig.BILLING_ENABLE.value(Boolean.class)) {
            return;
        }

        if (createUsageExtensionPointList != null) {
            for (ResourceCreateUsageExtensionPoint p : createUsageExtensionPointList) {
                Usage newUsage = p.makeUsage(usage);
                usage = newUsage != null ? newUsage : usage;
            }
        }

        Integer syncLevel = BillingGlobalConfig.PERSIST_RESOURCE_USAGE_SYNC_LEVEL.value(Integer.class);
        if (syncLevel <= -1) {
            return;
        }

        if (syncLevel == 0) {
            dbf.persist(usage);
            return;
        }

        Usage finalUsage = usage;
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return "persist-resource-UsageVO";
            }

            @Override
            protected int getSyncLevel() {
                return syncLevel;
            }

            @Override
            public void run(SyncTaskChain chain) {
                dbf.persist(finalUsage);
                chain.next();
            }

            @Override
            public String getName() {
                return "persist-resource-UsageVO";
            }
        });
    }

    private void handleVolumeConfigChanged(VolumeCanonicalEvents.VolumeConfigChangedData d) {
        String status = d.getInventory().getStatus();
        if (!VolumeStatus.Ready.toString().equals(status) && !VolumeStatus.Deleted.toString().equals(status)) {
            return;
        }

        if (StringUtils.isEmpty(d.getAccoutUuid())) {
            logger.warn(String.format("Billing:cannot find account uuid for the volume[uuid:%s, name:%s]",
                    d.getInventory().getUuid(), d.getInventory().getName()));
            return;
        }

        VolumeInventory inv = d.getInventory();
        if (VolumeType.Data.toString().equals(inv.getType())) {
            DataVolumeUsageVO vco = new DataVolumeUsageVO();
            vco.setAccountUuid(d.getAccoutUuid());
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(inv.getStatus());
            persistUsage(vco);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Volume Usage]: %s", JSONObjectUtil.toJsonString(DataVolumeUsageInventory.valueOf(vco))));
            }
        } else {
            RootVolumeUsageVO vco = new RootVolumeUsageVO();
            vco.setVmUuid(inv.getVmInstanceUuid());
            vco.setAccountUuid(d.getAccoutUuid());
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(inv.getStatus());
            persistUsage(vco);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Volume Usage]: %s", JSONObjectUtil.toJsonString(RootVolumeUsageInventory.valueOf(vco))));
            }
        }
    }

    private void handleVolumeUsage(VolumeCanonicalEvents.VolumeStatusChangedData d) {
        if (!VolumeStatus.Ready.toString().equals(d.getNewStatus()) && !VolumeStatus.Deleted.toString().equals(d.getNewStatus())) {
            return;
        }

        if (VolumeStatus.Deleted.toString().equals(d.getNewStatus())) {
            boolean exists = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, d.getVolumeUuid()).isExists();
            // Disks that have not been completely removed will continue to be charged
            if (exists) {
                return;
            }
        }

        // skip for vm creation rollback
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(d.getVolumeUuid());
        accountUuid = accountUuid != null ? accountUuid : d.getAccountUuid();
        if (accountUuid == null) {
            logger.warn(String.format("Billing:cannot find account uuid for the volume[uuid:%s, name:%s]",
                    d.getVolumeUuid(), d.getInventory().getName()));
            return;
        }


        VolumeInventory inv = d.getInventory();
        if (VolumeType.Data.toString().equals(inv.getType())) {
            DataVolumeUsageVO vco = new DataVolumeUsageVO();
            vco.setAccountUuid(accountUuid);
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(d.getNewStatus());
            persistUsage(vco);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Volume Usage]: %s", JSONObjectUtil.toJsonString(DataVolumeUsageInventory.valueOf(vco))));
            }
        } else {
            RootVolumeUsageVO vco = new RootVolumeUsageVO();
            vco.setVmUuid(inv.getVmInstanceUuid());
            vco.setAccountUuid(accountUuid);
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(d.getNewStatus());
            persistUsage(vco);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Volume Usage]: %s", JSONObjectUtil.toJsonString(RootVolumeUsageInventory.valueOf(vco))));
            }
        }
    }

    private void handleVmUsage(VmCanonicalEvents.VmStateChangedData d) {
        makeVmUsage(d);
        makePciDeviceUsage(d);
        makeVmNicBandwidthUsage(d);
    }

    private void handleVmConfigChanged(VmCanonicalEvents.VmConfigChangedData d) {
        String state = d.getInv().getState();
        if (!VmInstanceState.Running.toString().equals(state)
                && !VmInstanceState.Stopped.toString().equals(state)
                && !VmInstanceState.Destroyed.toString().equals(state)) {
            return;
        }

        if (d.getAccountUuid() == null) {
            logger.warn(String.format("Billing:cannot find account uuid for the vm[uuid:%s, name:%s]",
                    d.getVmUuid(), d.getInv().getName()));
            return;
        }

        VmUsageVO co = new VmUsageVO();
        co.setVmUuid(d.getVmUuid());
        co.setAccountUuid(d.getAccountUuid());
        co.setName(d.getInv().getName());
        co.setDateInLong(System.currentTimeMillis());
        co.setCpuNum(d.getInv().getCpuNum());
        co.setMemorySize(d.getInv().getMemorySize());
        co.setInventory(JSONObjectUtil.toJsonString(d));
        co.setState(state);
        if (d.getInv().getRootVolume() != null) {
            co.setRootVolumeSize(d.getInv().getRootVolume().getSize());
        }
        persistUsage(co);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[VM Usage]: %s", JSONObjectUtil.toJsonString(VmUsageInventory.valueOf(co))));
        }
    }

    private void makeVmUsage(VmCanonicalEvents.VmStateChangedData d) {
        List<String> states = Arrays.asList(VmInstanceState.Running.toString(),
                VmInstanceState.Stopped.toString(),
                VmInstanceState.Destroyed.toString(),
                VmInstanceState.Unknown.toString());

        if (!states.contains(d.getNewState())) {
            return;
        }

        // skip for vm creation rollback
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(d.getVmUuid());
        if (accountUuid == null) {
            logger.warn(String.format("Billing:cannot find account uuid for the vm[uuid:%s, name:%s]",
                    d.getVmUuid(), d.getInventory().getName()));
            return;
        }

        VmUsageVO co = new VmUsageVO();
        co.setVmUuid(d.getVmUuid());
        co.setAccountUuid(accountUuid);
        co.setName(d.getInventory().getName());
        co.setDateInLong(System.currentTimeMillis());
        co.setCpuNum(d.getInventory().getCpuNum());
        co.setMemorySize(d.getInventory().getMemorySize());
        co.setInventory(JSONObjectUtil.toJsonString(d));
        co.setState(d.getNewState());
        if (d.getInventory().getRootVolume() != null) {
            co.setRootVolumeSize(d.getInventory().getRootVolume().getSize());
        }
        persistUsage(co);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[VM Usage]: %s", JSONObjectUtil.toJsonString(VmUsageInventory.valueOf(co))));
        }
    }

    private void makePciDeviceUsage(VmCanonicalEvents.VmStateChangedData d) {
        if (!VmInstanceState.Running.toString().equals(d.getNewState())
                && !VmInstanceState.Stopped.toString().equals(d.getNewState())
                && !VmInstanceState.Destroyed.toString().equals(d.getNewState())) {
            return;
        }

        // skip for vm creation rollback
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(d.getVmUuid());
        if (accountUuid == null) {
            logger.warn(String.format("Billing:cannot find account uuid for the vm[uuid:%s, name:%s]",
                    d.getVmUuid(), d.getInventory().getName()));
            return;
        }

        /* even vm is stopped, pci device is still occupied by vm */
        if (VmInstanceState.Stopped.toString().equals(d.getNewState())) {
            return;
        }

        List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.vmInstanceUuid, d.getVmUuid()).list();
        if (pciDeviceVOS == null || pciDeviceVOS.isEmpty()) {
            return;
        }

        List<PciDeviceUsageVO> GpuUsageVos = new ArrayList<>();
        for (PciDeviceVO vo : pciDeviceVOS) {
            PciDeviceUsageVO pciVo = new PciDeviceUsageVO();
            pciVo.setPciDeviceUuid(vo.getUuid());
            pciVo.setVendorId(vo.getVendorId());
            pciVo.setDeviceId(vo.getDeviceId());
            pciVo.setSubvendorId(vo.getSubvendorId());
            pciVo.setSubdeviceId(vo.getSubdeviceId());
            pciVo.setVmUuid(d.getVmUuid());
            pciVo.setVmName(d.getInventory().getName());
            if (VmInstanceState.Destroyed.toString().equals(d.getNewState())) {
                pciVo.setStatus(PciDeviceStatus.System.toString());
            } else {
                pciVo.setStatus(vo.getStatus().toString());
            }
            pciVo.setAccountUuid(accountUuid);
            pciVo.setDateInLong(System.currentTimeMillis());
            pciVo.setDescription(vo.getDescription());
            pciVo.setInventory(JSONObjectUtil.toJsonString(PciDeviceInventory.valueOf(vo)));

            GpuUsageVos.add(pciVo);
            createPeiDeviceBillingResourceLabel(vo.getUuid(),d.getVmUuid());
        }
        for (PciDeviceUsageVO usageVO : GpuUsageVos) {
            persistUsage(usageVO);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PciDevice Usage]: %s", JSONObjectUtil.toJsonString(PciDeviceUsageInventory.valueOf(GpuUsageVos))));
        }
    }

    private void createPeiDeviceBillingResourceLabel(String pciDeviceUuid, String vmUuid){
        String hypervisorType = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
        createBillingResourceLabel(pciDeviceUuid, BillingResourceLabelKey.HYPERVISORTYPE.toString(), hypervisorType);
    }

    private void createBillingResourceLabel(String resourceUuid, String labelKey, String labelValue){
        boolean isLabelExist = Q.New(BillingResourceLabelVO.class)
                .eq(BillingResourceLabelVO_.resourceUuid, resourceUuid)
                .eq(BillingResourceLabelVO_.labelKey, labelKey)
                .isExists();
        if (!isLabelExist){
            BillingResourceLabelVO label = new BillingResourceLabelVO();
            label.setResourceUuid(resourceUuid);
            label.setLabelKey(labelKey);
            label.setLabelValue(labelValue);
            dbf.persist(label);
        }
    }

    private void makeVmNicBandwidthUsage(VmCanonicalEvents.VmStateChangedData d) {
        if (!VmInstanceState.Running.toString().equals(d.getNewState())
                && !VmInstanceState.Stopped.toString().equals(d.getNewState())
                && !VmInstanceState.Destroyed.toString().equals(d.getNewState())) {
            return;
        }

        String accountUuid = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, d.getVmUuid())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .findValue();
        if (accountUuid == null) {
            return;
        }

        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, d.getVmUuid())
                .list();

        if (vmNicVOS.isEmpty()) {
            return;
        }

        VmNicQosConfigBackend backend = vrMgr.getVmNicQosConfigBackend(d.getInventory().getType());
        for (VmNicVO vmNicVO : vmNicVOS) {
            String l3Uuid = vmNicVO.getL3NetworkUuid();
            boolean isPubL3 = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .eq(L3NetworkVO_.category, L3NetworkCategory.Public)
                    .isExists();
            if (!isPubL3) {
                continue;
            }

            if (skipSavePubIpVmNicBandwidthUsage(vmNicVO.getUuid())) {
                continue;
            }

            PubIpVmNicBandwidthUsageVO usageVO = new PubIpVmNicBandwidthUsageVO();
            usageVO.setVmNicUuid(vmNicVO.getUuid());
            usageVO.setAccountUuid(accountUuid);
            usageVO.setInventory(JSONObjectUtil.toJsonString(d));
            usageVO.setL3NetworkUuid(vmNicVO.getL3NetworkUuid());
            List<String> ips = VmNicHelper.getIpAddresses(vmNicVO);
            usageVO.setVmNicIp(StringUtils.join(ips, ","));
            usageVO.setVmInstanceUuid(vmNicVO.getVmInstanceUuid());
            usageVO.setVmNicStatus(d.getNewState());
            usageVO.setDateInLong(System.currentTimeMillis());

            VmNicQosStruct struct = backend.getNicQos(d.getInventory().getUuid(), vmNicVO.getUuid());
            Long inbound = struct.inboundBandwidth;
            Long outbound = struct.outboundBandwidth;
            usageVO.setBandwidthIn(0L);
            usageVO.setBandwidthOut(0L);
            if (outbound != -1L) {
                usageVO.setBandwidthOut(outbound);
            }
            if (inbound != -1L) {
                usageVO.setBandwidthIn(inbound);
            }
            persistUsage(usageVO);

            createVmNicBandwidthBillingResourceLabelVO(vmNicVO.getUuid(), d.getVmUuid());

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[PubIpVmNicBandwidth Usage]: %s", JSONObjectUtil.toJsonString(PubIpVmNicBandwidthUsageInventory.valueOf(usageVO))));
            }
        }
    }

    private void createVmNicBandwidthBillingResourceLabelVO(String vmNicUuid, String vmUuid) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
        createBillingResourceLabel(vmNicUuid, BillingResourceLabelKey.HYPERVISORTYPE.toString(), hypervisorType);
    }

    private boolean skipSavePubIpVmNicBandwidthUsage(String vmNicUuid) {
        long in = 0L;
        long out = 0L;

        VmNicVO vmNicVO = dbf.findByUuid(vmNicUuid, VmNicVO.class);
        if (vmNicVO != null) {
            String vmType = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmNicVO.getVmInstanceUuid()).select(VmInstanceVO_.type).findValue();
            VmNicQosConfigBackend backend = vrMgr.getVmNicQosConfigBackend(vmType);
            VmNicQosStruct struct = backend.getNicQos(vmNicVO.getVmInstanceUuid(), vmNicUuid);
            Long inbound = struct.inboundBandwidth;
            Long outbound = struct.outboundBandwidth;

            if (inbound != -1L) {
                in = inbound;
            }
            if (outbound != -1L) {
                out = outbound;
            }
        }

        if (in != 0 || out != 0) {
            return false;
        }

        boolean exists = Q.New(PubIpVmNicBandwidthUsageVO.class)
                .eq(PubIpVmNicBandwidthUsageVO_.vmNicUuid, vmNicUuid)
                .isExists();
        if (!exists) {
            return true;
        }

        return false;
    }

    private boolean skipSavePubIpVipBandwidthUsage(String vipUuid) {
        long in = 0L;
        long out = 0L;

        List<VipQosVO> vipQosVOS = Q.New(VipQosVO.class)
                .eq(VipQosVO_.vipUuid, vipUuid)
                .list();
        for (VipQosVO qosVO : vipQosVOS) {
            in += qosVO.getInboundBandwidth();
            out += qosVO.getOutboundBandwidth();
        }

        if (in != 0 || out != 0) {
            return false;
        }

        boolean exists = Q.New(PubIpVipBandwidthUsageVO.class)
                .eq(PubIpVipBandwidthUsageVO_.vipUuid, vipUuid)
                .isExists();
        if (!exists) {
            return true;
        }

        return false;
    }

    private void handleVipBandwidthUsage(VipCanonicalEvents.VipEventData data) {
        assert data.getInventory() != null;
        assert data.getVipUuid() != null;
        assert data.getCurrentStatus() != null;

        VipInventory inventory = data.getInventory();

        String accountUuid = Q.New(AccountResourceRefVO.class)
                .select(AccountResourceRefVO_.accountUuid)
                .eq(AccountResourceRefVO_.resourceUuid, data.getVipUuid())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .findValue();
        accountUuid = accountUuid != null ? accountUuid : data.getAccountUuid();
        if (accountUuid == null) {
            return;
        }

        String l3Uuid = inventory.getL3NetworkUuid();
        boolean isPubL3 = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, l3Uuid)
                .eq(L3NetworkVO_.category, L3NetworkCategory.Public)
                .isExists();
        if (!isPubL3) {
            return;
        }

        if (skipSavePubIpVipBandwidthUsage(inventory.getUuid())) {
            return;
        }

        PubIpVipBandwidthUsageVO usageVO = new PubIpVipBandwidthUsageVO();
        usageVO.setVipIp(inventory.getIp());
        usageVO.setVipName(inventory.getName());
        usageVO.setVipStatus(data.getCurrentStatus());
        usageVO.setVipUuid(data.getVipUuid());
        usageVO.setAccountUuid(accountUuid);
        usageVO.setDateInLong(System.currentTimeMillis());
        usageVO.setBandwidthIn(0L);
        usageVO.setBandwidthOut(0L);
        usageVO.setL3NetworkUuid(l3Uuid);
        usageVO.setInventory(JSONObjectUtil.toJsonString(data));

        List<VipQosVO> vipQosVOS = Q.New(VipQosVO.class)
                .eq(VipQosVO_.vipUuid, data.getVipUuid())
                .list();
        for (VipQosVO qosVO : vipQosVOS) {
            long bandwidthIn = usageVO.getBandwidthIn() + qosVO.getInboundBandwidth();
            long bandwidthOut = usageVO.getBandwidthOut() + qosVO.getOutboundBandwidth();
            usageVO.setBandwidthIn(bandwidthIn);
            usageVO.setBandwidthOut(bandwidthOut);
        }

        persistUsage(usageVO);

        addVipBandwidthBillingResourceLabel(data.getInventory().getUuid(), data.getInventory().getL3NetworkUuid());

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PubIpVipBandwidth Usage]: %s", JSONObjectUtil.toJsonString(PubIpVipBandwidthUsageInventory.valueOf(usageVO))));
        }
    }

    private void addVipBandwidthBillingResourceLabel(String resourceUuid, String l3NetworkUuid){
        String l2NetworkUuid = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, l3NetworkUuid)
                .select(L3NetworkVO_.l2NetworkUuid)
                .findValue();
        String clusterUuid = Q.New(L2NetworkClusterRefVO.class)
                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2NetworkUuid)
                .select(L2NetworkClusterRefVO_.clusterUuid)
                .findValue();
        if (clusterUuid == null) {
            return;
        }
        String hypervisorType = Q.New(ClusterVO.class)
                .eq(ClusterVO_.uuid, clusterUuid)
                .select(ClusterVO_.hypervisorType)
                .findValue();

        createBillingResourceLabel(resourceUuid, BillingResourceLabelKey.HYPERVISORTYPE.toString(), hypervisorType);

    }

    private void handleVmNicBandwidthUsage(VmNicQosCanonicalEvents.VmNicQosEventData data) {
        PubIpVmNicBandwidthUsageVO usageVO = makePubIpVmNicBandwidthUsageVO(data.getInventory(), data.getAccountUuid(), data.getCurrentStatus(), data.getDate().getTime());
        if (usageVO == null) {
            return;
        }

        Long inbound = data.getBandwidthIn();
        Long outbound = data.getBandwidthOut();
        usageVO.setBandwidthIn(0L);
        usageVO.setBandwidthOut(0L);
        if (outbound != null) {
            usageVO.setBandwidthOut(outbound == -1 ? 0 : outbound);
        }
        if (inbound != null) {
            usageVO.setBandwidthIn(inbound == -1 ? 0 : inbound);
        }
        persistUsage(usageVO);
        addVmNicBandwidthBillingResourceLabelVO(data.getInventory().getUuid(), data.getInventory().getVmInstanceUuid());
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PubIpVmNicBandwidth Usage]: %s", JSONObjectUtil.toJsonString(PubIpVmNicBandwidthUsageInventory.valueOf(usageVO))));
        }
    }

    private void handleVmNicBandwidthUsage(VmNicCanonicalEvents.VmNicEventData data) {
        PubIpVmNicBandwidthUsageVO usageVO = makePubIpVmNicBandwidthUsageVO(data.getInventory(), data.getAccountUuid(), data.getCurrentStatus(), data.getDate().getTime());
        if (usageVO == null) {
            return;
        }

        usageVO.setBandwidthIn(0L);
        usageVO.setBandwidthOut(0L);

        VmNicVO vmNicVO = dbf.findByUuid(data.getInventory().getUuid(), VmNicVO.class);
        if (vmNicVO != null) {
            String vmType = Q.New(VmInstanceVO.class).select(VmInstanceVO_.type).eq(VmInstanceVO_.uuid, vmNicVO.getVmInstanceUuid()).findValue();
            VmNicQosConfigBackend backend = vrMgr.getVmNicQosConfigBackend(vmType);
            VmNicQosStruct struct = backend.getNicQos(vmNicVO.getVmInstanceUuid(), vmNicVO.getUuid());

            Long inbound = struct.inboundBandwidth;
            Long outbound = struct.outboundBandwidth;
            if (outbound != -1L) {
                usageVO.setBandwidthOut(inbound);
            }
            if (inbound != -1L) {
                usageVO.setBandwidthIn(outbound);
            }
        }

        persistUsage(usageVO);

        addVmNicBandwidthBillingResourceLabelVO(data.getInventory().getUuid(), data.getInventory().getVmInstanceUuid());

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PubIpVmNicBandwidth Usage]: %s", JSONObjectUtil.toJsonString(PubIpVmNicBandwidthUsageInventory.valueOf(usageVO))));
        }
    }

    private PubIpVmNicBandwidthUsageVO makePubIpVmNicBandwidthUsageVO(VmNicInventory inventory, String accountUuid, String currentStatus, long dateInLong) {
        if (!VmInstanceState.Running.toString().equals(currentStatus)
                && !VmInstanceState.Stopped.toString().equals(currentStatus)
                && !VmInstanceState.Destroyed.toString().equals(currentStatus)) {
            return null;
        }

        if (accountUuid == null) {
            accountUuid = Q.New(AccountResourceRefVO.class)
                    .select(AccountResourceRefVO_.accountUuid)
                    .eq(AccountResourceRefVO_.resourceUuid, inventory.getUuid())
                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                    .findValue();
            if (accountUuid == null) {
                return null;
            }
        }

        String l3Uuid = inventory.getL3NetworkUuid();
        boolean isPubL3 = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, l3Uuid)
                .eq(L3NetworkVO_.category, L3NetworkCategory.Public)
                .isExists();
        if (!isPubL3) {
            return null;
        }

        if (skipSavePubIpVmNicBandwidthUsage(inventory.getUuid())) {
            return null;
        }

        PubIpVmNicBandwidthUsageVO usageVO = new PubIpVmNicBandwidthUsageVO();
        usageVO.setVmNicUuid(inventory.getUuid());
        usageVO.setAccountUuid(accountUuid);
        usageVO.setInventory(JSONObjectUtil.toJsonString(inventory));
        usageVO.setL3NetworkUuid(inventory.getL3NetworkUuid());
        List<String> ips = VmNicHelper.getIpAddresses(inventory);
        usageVO.setVmNicIp(StringUtils.join(ips, ","));
        usageVO.setVmInstanceUuid(inventory.getVmInstanceUuid());
        usageVO.setVmNicStatus(currentStatus);
        usageVO.setDateInLong(dateInLong);

        return usageVO;
    }

    private void addVmNicBandwidthBillingResourceLabelVO(String resourceUuid, String vmUuid) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
        createBillingResourceLabel(resourceUuid, BillingResourceLabelKey.HYPERVISORTYPE.toString(), hypervisorType);
    }

    @Override
    public boolean stop() {
        if (billingTask != null) {
            billingTask.cancel(true);
        }
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            withDrawEvents();
        }

        return true;
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (ref.getResourceType().equals(VmInstanceVO.class.getSimpleName())) {
            handleVmOwnerChange(ref, newOwnerUuid);
        } else if (ref.getResourceType().equals(VolumeVO.class.getSimpleName())) {
            handleVolumeOwnerChange(ref, newOwnerUuid);
        } else if (ref.getResourceType().equals(VipVO.class.getSimpleName())) {
            handleVipOwnerChange(ref, newOwnerUuid);
        }
    }

    @Transactional
    private void handleVolumeOwnerChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        VolumeVO vo = dbf.getEntityManager().find(VolumeVO.class, ref.getResourceUuid());
        if (vo == null) {
            return;
        }

        if (VolumeStatus.Ready != vo.getStatus() && VolumeStatus.Deleted != vo.getStatus()) {
            return;
        }

        VolumeInventory inv = VolumeInventory.valueOf(vo);
        if (VolumeType.Data.toString().equals(inv.getType())) {
            DataVolumeUsageVO vco = new DataVolumeUsageVO();
            vco.setAccountUuid(ref.getAccountUuid());
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(VolumeStatus.Deleted.toString());
            persistUsage(vco);

            vco = new DataVolumeUsageVO();
            vco.setAccountUuid(newOwnerUuid);
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(inv.getStatus());
            persistUsage(vco);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Volume Usage]: %s", JSONObjectUtil.toJsonString(DataVolumeUsageInventory.valueOf(vco))));
            }
        } else {
            RootVolumeUsageVO vco = new RootVolumeUsageVO();
            vco.setVmUuid(inv.getVmInstanceUuid());
            vco.setAccountUuid(ref.getAccountUuid());
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());
            vco.setVolumeStatus(VolumeStatus.Deleted.toString());
            persistUsage(vco);

            vco = new RootVolumeUsageVO();
            vco.setVmUuid(inv.getVmInstanceUuid());
            vco.setAccountUuid(newOwnerUuid);
            vco.setDateInLong(System.currentTimeMillis());
            vco.setVolumeName(inv.getName());
            vco.setVolumeUuid(inv.getUuid());
            vco.setInventory(JSONObjectUtil.toJsonString(inv));
            vco.setVolumeSize(inv.getSize());

            if (VolumeStatus.Deleted.toString().equals(inv.getStatus())) {
                boolean exists = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, inv.getUuid()).isExists();
                // Disks that have not been completely removed will continue to be charged
                if (exists) {
                    vco.setVolumeStatus(VolumeStatus.Ready.toString());
                } else {
                    vco.setVolumeStatus(inv.getStatus());
                }
            } else {
                vco.setVolumeStatus(inv.getStatus());
            }
            persistUsage(vco);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[Volume Usage]: %s", JSONObjectUtil.toJsonString(RootVolumeUsageInventory.valueOf(vco))));
            }
        }
    }

    @Transactional
    private void handleVmOwnerChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        VmInstanceVO vm = dbf.getEntityManager().find(VmInstanceVO.class, ref.getResourceUuid());
        if (vm == null) {
            return;
        }

        if (VmInstanceState.Running != vm.getState()
                && VmInstanceState.Stopped != vm.getState()
                && VmInstanceState.Destroyed != vm.getState()) {
            return;
        }

        VmInstanceInventory inv = VmInstanceInventory.valueOf(vm);

        VmUsageVO co = new VmUsageVO();
        co.setVmUuid(vm.getUuid());
        co.setAccountUuid(ref.getAccountUuid());
        co.setName(vm.getName());
        co.setDateInLong(System.currentTimeMillis());
        co.setCpuNum(vm.getCpuNum());
        co.setMemorySize(vm.getMemorySize());
        co.setInventory(JSONObjectUtil.toJsonString(inv));
        co.setState(VmInstanceState.Stopped.toString());
        if (inv.getRootVolume() != null) {
            co.setRootVolumeSize(inv.getRootVolume().getSize());
        }
        persistUsage(co);

        co = new VmUsageVO();
        co.setVmUuid(vm.getUuid());
        co.setAccountUuid(newOwnerUuid);
        co.setName(vm.getName());
        co.setDateInLong(System.currentTimeMillis());
        co.setCpuNum(vm.getCpuNum());
        co.setMemorySize(vm.getMemorySize());
        co.setInventory(JSONObjectUtil.toJsonString(inv));
        co.setState(vm.getState().toString());
        if (inv.getRootVolume() != null) {
            co.setRootVolumeSize(inv.getRootVolume().getSize());
        }
        persistUsage(co);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[VM Usage]: %s", JSONObjectUtil.toJsonString(VmUsageInventory.valueOf(co))));
        }

        handlePciDeviceOwnerChange(ref, newOwnerUuid, vm);
        handleVmNicOwnerChange(ref, newOwnerUuid, vm);
    }

    private void handlePciDeviceOwnerChange(AccountResourceRefInventory ref, String newOwnerUuid, VmInstanceVO vm) {
        List<PciDeviceVO> pciDeviceVOS = Q.New(PciDeviceVO.class).eq(PciDeviceVO_.vmInstanceUuid, vm.getUuid()).list();
        if (pciDeviceVOS == null || pciDeviceVOS.isEmpty()) {
            return;
        }

        List<PciDeviceUsageVO> PciUsageVos = new ArrayList<>();
        for (PciDeviceVO vo : pciDeviceVOS) {
            PciDeviceUsageVO pciVo = new PciDeviceUsageVO();
            pciVo.setPciDeviceUuid(vo.getUuid());
            pciVo.setVendorId(vo.getVendorId());
            pciVo.setDeviceId(vo.getDeviceId());
            pciVo.setSubvendorId(vo.getSubvendorId());
            pciVo.setSubdeviceId(vo.getSubdeviceId());
            pciVo.setDescription(vo.getDescription());
            pciVo.setVmUuid(vm.getUuid());
            pciVo.setVmName(vm.getName());
            pciVo.setStatus(PciDeviceStatus.System.toString());
            pciVo.setAccountUuid(ref.getAccountUuid());
            pciVo.setDateInLong(System.currentTimeMillis());
            pciVo.setInventory(JSONObjectUtil.toJsonString(PciDeviceInventory.valueOf(vo)));
            PciUsageVos.add(pciVo);

            pciVo = new PciDeviceUsageVO();
            pciVo.setPciDeviceUuid(vo.getUuid());
            pciVo.setVendorId(vo.getVendorId());
            pciVo.setDeviceId(vo.getDeviceId());
            pciVo.setSubvendorId(vo.getSubvendorId());
            pciVo.setSubdeviceId(vo.getSubdeviceId());
            pciVo.setDescription(vo.getDescription());
            pciVo.setVmUuid(vm.getUuid());
            pciVo.setVmName(vm.getName());
            pciVo.setStatus(vo.getStatus().toString());
            pciVo.setAccountUuid(newOwnerUuid);
            pciVo.setDateInLong(System.currentTimeMillis());
            pciVo.setInventory(JSONObjectUtil.toJsonString(PciDeviceInventory.valueOf(vo)));

            PciUsageVos.add(pciVo);
        }
        for (PciDeviceUsageVO usageVO : PciUsageVos) {
            persistUsage(usageVO);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PciDevice Usage]: %s", JSONObjectUtil.toJsonString(PciDeviceUsageInventory.valueOf(PciUsageVos))));
        }
    }

    private void handleVmNicOwnerChange(AccountResourceRefInventory ref, String newOwnerUuid, VmInstanceVO vm) {
        List<VmNicVO> vmNicVOS = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                .list();

        if (vmNicVOS.isEmpty()) {
            return;
        }

        VmNicQosConfigBackend backend = vrMgr.getVmNicQosConfigBackend(vm.getType());
        for (VmNicVO vmNicVO : vmNicVOS) {
            String l3Uuid = vmNicVO.getL3NetworkUuid();
            boolean isPubL3 = Q.New(L3NetworkVO.class)
                    .eq(L3NetworkVO_.uuid, l3Uuid)
                    .eq(L3NetworkVO_.category, L3NetworkCategory.Public)
                    .isExists();
            if (!isPubL3) {
                continue;
            }

            VmNicQosStruct struct = backend.getNicQos(vmNicVO.getVmInstanceUuid(), vmNicVO.getUuid());
            Long inbound = struct.inboundBandwidth;
            Long outbound = struct.outboundBandwidth;

            List<String> ips = VmNicHelper.getIpAddresses(vmNicVO);
            String nicIpStr = StringUtils.join(ips, ",");

            PubIpVmNicBandwidthUsageVO endUsageVO = new PubIpVmNicBandwidthUsageVO();
            endUsageVO.setVmNicUuid(vmNicVO.getUuid());
            // TODO: If account has been deleted, endUsageVO.accountUuid will be admin
            endUsageVO.setAccountUuid(ref.getAccountUuid());
            endUsageVO.setInventory(JSONObjectUtil.toJsonString(VmNicInventory.valueOf(vmNicVO)));
            endUsageVO.setL3NetworkUuid(vmNicVO.getL3NetworkUuid());
            endUsageVO.setVmNicIp(nicIpStr);
            endUsageVO.setVmInstanceUuid(vmNicVO.getVmInstanceUuid());
            endUsageVO.setVmNicStatus(VmInstanceState.Destroyed.toString());
            endUsageVO.setDateInLong(System.currentTimeMillis());
            endUsageVO.setBandwidthIn(0L);
            endUsageVO.setBandwidthOut(0L);
            if (outbound != -1L) {
                endUsageVO.setBandwidthOut(outbound);
            }
            if (inbound != -1L) {
                endUsageVO.setBandwidthIn(inbound);
            }
            persistUsage(endUsageVO);

            PubIpVmNicBandwidthUsageVO startUsageVO = new PubIpVmNicBandwidthUsageVO();
            startUsageVO.setVmNicUuid(vmNicVO.getUuid());
            startUsageVO.setAccountUuid(newOwnerUuid);
            startUsageVO.setInventory(JSONObjectUtil.toJsonString(VmNicInventory.valueOf(vmNicVO)));
            startUsageVO.setL3NetworkUuid(vmNicVO.getL3NetworkUuid());
            startUsageVO.setVmNicIp(nicIpStr);
            startUsageVO.setVmInstanceUuid(vmNicVO.getVmInstanceUuid());
            startUsageVO.setVmNicStatus(vm.getState().toString());
            startUsageVO.setDateInLong(System.currentTimeMillis());
            startUsageVO.setBandwidthIn(0L);
            startUsageVO.setBandwidthOut(0L);
            if (outbound != -1) {
                startUsageVO.setBandwidthOut(outbound);
            }
            if (inbound != -1) {
                startUsageVO.setBandwidthIn(inbound);
            }
            persistUsage(startUsageVO);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[PubIpVmNicBandwidth Usage]: %s", JSONObjectUtil.toJsonString(PubIpVmNicBandwidthUsageInventory.valueOf(endUsageVO))));
                logger.trace(String.format("[PubIpVmNicBandwidth Usage]: %s", JSONObjectUtil.toJsonString(PubIpVmNicBandwidthUsageInventory.valueOf(startUsageVO))));
            }
        }
    }

    @Transactional
    private void handleVipOwnerChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        VipVO vo = dbf.findByUuid(ref.getResourceUuid(), VipVO.class);
        if (vo == null) {
            return;
        }

        String l3Uuid = vo.getL3NetworkUuid();
        boolean isPubL3 = Q.New(L3NetworkVO.class)
                .eq(L3NetworkVO_.uuid, l3Uuid)
                .eq(L3NetworkVO_.category, L3NetworkCategory.Public)
                .isExists();
        if (!isPubL3) {
            return;
        }

        VipInventory inv = VipInventory.valueOf(vo);

        PubIpVipBandwidthUsageVO endUsageVO = new PubIpVipBandwidthUsageVO();
        endUsageVO.setAccountUuid(ref.getAccountUuid());
        endUsageVO.setDateInLong(System.currentTimeMillis());
        endUsageVO.setVipName(inv.getName());
        endUsageVO.setVipUuid(inv.getUuid());
        endUsageVO.setVipIp(inv.getIp());
        endUsageVO.setVipStatus(VipCanonicalEvents.VIP_STATUS_DELETED);
        endUsageVO.setL3NetworkUuid(l3Uuid);
        endUsageVO.setInventory(JSONObjectUtil.toJsonString(inv));
        endUsageVO.setBandwidthIn(0L);
        endUsageVO.setBandwidthOut(0L);
        List<VipQosVO> vipQosVOS = Q.New(VipQosVO.class)
                .eq(VipQosVO_.vipUuid, vo.getUuid())
                .list();
        for (VipQosVO qosVO : vipQosVOS) {
            long bandwidthIn = endUsageVO.getBandwidthIn() + qosVO.getInboundBandwidth();
            long bandwidthOut = endUsageVO.getBandwidthOut() + qosVO.getOutboundBandwidth();
            endUsageVO.setBandwidthIn(bandwidthIn);
            endUsageVO.setBandwidthOut(bandwidthOut);
        }
        persistUsage(endUsageVO);

        PubIpVipBandwidthUsageVO startUsageVO = new PubIpVipBandwidthUsageVO();
        startUsageVO.setAccountUuid(newOwnerUuid);
        startUsageVO.setDateInLong(System.currentTimeMillis());
        startUsageVO.setVipName(inv.getName());
        startUsageVO.setVipUuid(inv.getUuid());
        startUsageVO.setVipIp(inv.getIp());
        startUsageVO.setVipStatus(VipCanonicalEvents.VIP_STATUS_CREATED);
        startUsageVO.setL3NetworkUuid(l3Uuid);
        startUsageVO.setInventory(JSONObjectUtil.toJsonString(inv));
        startUsageVO.setBandwidthIn(0L);
        startUsageVO.setBandwidthOut(0L);
        vipQosVOS = Q.New(VipQosVO.class)
                .eq(VipQosVO_.vipUuid, vo.getUuid())
                .list();
        for (VipQosVO qosVO : vipQosVOS) {
            long bandwidthIn = startUsageVO.getBandwidthIn() + qosVO.getInboundBandwidth();
            long bandwidthOut = startUsageVO.getBandwidthOut() + qosVO.getOutboundBandwidth();
            startUsageVO.setBandwidthIn(bandwidthIn);
            startUsageVO.setBandwidthOut(bandwidthOut);
        }
        persistUsage(startUsageVO);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PubIpVipBandwidthUsage Usage]: %s", JSONObjectUtil.toJsonString(PubIpVipBandwidthUsageInventory.valueOf(endUsageVO))));
            logger.trace(String.format("[PubIpVipBandwidthUsage Usage]: %s", JSONObjectUtil.toJsonString(PubIpVipBandwidthUsageInventory.valueOf(startUsageVO))));
        }
    }

    private void handleGpuUsage(PciDeviceCanonicalEvents.PciDeviceStateChangedData d) {
        VmInstanceVO vmVo = dbf.findByUuid(d.getVmUuid(), VmInstanceVO.class);
        if (vmVo == null) {
            logger.warn(String.format("Billing:cannot find vm [uuid:%s] for gpu device [uuid:%s]",
                    d.getVmUuid(), d.getPciDeviceUuid()));
            return;
        }

        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(d.getVmUuid());
        if (accountUuid == null) {
            logger.warn(String.format("Billing:cannot find account uuid for gpu device [uuid:%s] on the vm[uuid:%s]",
                    d.getPciDeviceUuid(), d.getVmUuid()));
            return;
        }

        PciDeviceUsageVO pciVo = new PciDeviceUsageVO();
        pciVo.setPciDeviceUuid(d.getPciDeviceUuid());
        pciVo.setVendorId(d.getInventory().getVendorId());
        pciVo.setDeviceId(d.getInventory().getDeviceId());
        pciVo.setSubvendorId(d.getInventory().getSubvendorId());
        pciVo.setSubdeviceId(d.getInventory().getSubdeviceId());
        pciVo.setVmUuid(d.getVmUuid());
        pciVo.setVmName(vmVo.getName());
        pciVo.setStatus(d.getStatus());
        pciVo.setAccountUuid(accountUuid);
        pciVo.setDateInLong(System.currentTimeMillis());
        pciVo.setInventory(JSONObjectUtil.toJsonString(d.getInventory()));
        pciVo.setDescription(d.getDescription());
        persistUsage(pciVo);

        addGpuBillingResourceLabel(d.getPciDeviceUuid(), d.getVmUuid());
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[PciDevice Usage]: %s", JSONObjectUtil.toJsonString(PciDeviceUsageInventory.valueOf(pciVo))));
        }

    }

    private void addGpuBillingResourceLabel(String resourceUuid, String vmUuid) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.hypervisorType)
                .findValue();
        createBillingResourceLabel(resourceUuid, BillingResourceLabelKey.HYPERVISORTYPE.toString(), hypervisorType);
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        String lastState = Q.New(VmUsageVO.class)
                .select(VmUsageVO_.state)
                .eq(VmUsageVO_.vmUuid, inv.getUuid())
                .orderBy(VmUsageVO_.id, SimpleQuery.Od.DESC)
                .limit(1)
                .findValue();
        if (VmInstanceState.Stopped.toString().equals(lastState)
                || VmInstanceState.Destroyed.toString().equals(lastState)) {
            return;
        }
        VmCanonicalEvents.VmStateChangedData data = new VmCanonicalEvents.VmStateChangedData();
        data.setVmUuid(inv.getUuid());
        data.setOldState(null);
        data.setNewState(VmInstanceState.Destroyed.toString());
        data.setInventory(inv);

        handleVmUsage(data);

    }

    @Override
    public void vmJustAfterDeleteFromDbExtensionPoint(VmInstanceInventory inv, String accountUuid) {
        if (inv == null || accountUuid == null) {
            return;
        }
        VolumeInventory rootVolume = inv.getRootVolume();
        if (rootVolume == null) {
            return;
        }
        VolumeCanonicalEvents.VolumeStatusChangedData data = new VolumeCanonicalEvents.VolumeStatusChangedData();
        data.setOldStatus(rootVolume.getStatus());
        rootVolume.setStatus(VolumeStatus.Deleted.toString());
        data.setInventory(rootVolume);
        data.setNewStatus(VolumeStatus.Deleted.toString());
        data.setDate(new Date());
        data.setVolumeUuid(rootVolume.getUuid());
        data.setAccountUuid(accountUuid);
        evf.fire(VolumeCanonicalEvents.VOLUME_STATUS_CHANGED_PATH, data);
    }

    @Override
    public void beforeCreateVolume(VolumeInventory volume) {
        if (Objects.equals(volume.getType(), VolumeType.Data.toString())) {
            checkDataVolumeSystemTag(volume);
        } else {
            checkRootVolumeSystemTag(volume);
        }
    }

    private void checkDataVolumeSystemTag(VolumeInventory inv) {
        checkVolumeDiskOffering(inv);
    }

    private void checkRootVolumeSystemTag(VolumeInventory inv) {
        checkVolumeDiskOffering(inv);
        checkInstanceOffering(inv);
    }

    private void checkInstanceOffering(VolumeInventory inv) {
        VmInstanceVO vminstanceVO = dbf.findByUuid(inv.getVmInstanceUuid(), VmInstanceVO.class);
        String instanceOffering = vminstanceVO.getInstanceOfferingUuid();
        if (instanceOffering == null) {
            return;
        }
        if (!InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.hasTag(instanceOffering)) {
            return;
        }

        BillingInstanceOfferingUserConfig config = OfferingUserConfigUtils.getInstanceOfferingConfig(instanceOffering, BillingInstanceOfferingUserConfig.class);
        if (config.getPriceUserConfig() != null && config.getPriceUserConfig().getRootVolume() == null) {
            throw new OperationFailureException(operr("please set the correct priceUserConfig, for example: priceUserConfig:{\nrootVolume:{\npriceKeyName:\"priceKeyName\"}}"));
        }
    }

    private void checkVolumeDiskOffering(VolumeInventory inv) {
        String diskOffering = inv.getDiskOfferingUuid();
        if (diskOffering == null) {
            return;
        }
        if (!DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            return;
        }

        BillingDiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(diskOffering, BillingDiskOfferingUserConfig.class);
        if (config.getPriceUserConfig() != null && config.getPriceUserConfig().getVolume() == null) {
            throw new OperationFailureException(operr("please set the correct priceUserConfig, for example: priceUserConfig:{\nvolume:{\npriceKeyName:\"priceKeyName\"}}"));
        }
    }

    @Override
    public void preCreateVolume(VolumeCreateMessage msg) {
    }

    @Override
    public void afterCreateVolume(VolumeVO vo) {
        if (vo.getType() == VolumeType.Data) {
            makeDataVolumeSystemTag(VolumeInventory.valueOf(vo));
        } else {
            makeRootVolumeSystemTag(VolumeInventory.valueOf(vo));
        }
    }

    private void makeRootVolumeSystemTag(VolumeInventory inv) {
        VmInstanceVO vminstanceVO = dbf.findByUuid(inv.getVmInstanceUuid(), VmInstanceVO.class);

        String rootVolumeDiskOffering = inv.getDiskOfferingUuid();
        if (rootVolumeDiskOffering != null && DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(rootVolumeDiskOffering)) {
            BillingDiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(rootVolumeDiskOffering, BillingDiskOfferingUserConfig.class);
            String volumePriceConfig = null;
            if (config.getPriceUserConfig() != null) {
                volumePriceConfig = config.getPriceUserConfig().getVolume().getPriceKeyName();
            }
            Map<String, String> volumeDisplayAttribute = config.getDisplayAttribute() == null ? null : config.getDisplayAttribute().getVolume();
            createVolumeSystemTag(inv.getUuid(), volumePriceConfig, volumeDisplayAttribute);
            return;
        }

        String instanceOffering = vminstanceVO.getInstanceOfferingUuid();
        if (instanceOffering == null) {
            return;
        }

        if (InstanceOfferingSystemTags.INSTANCE_OFFERING_USER_CONFIG.hasTag(instanceOffering)) {
            BillingInstanceOfferingUserConfig config = OfferingUserConfigUtils.getInstanceOfferingConfig(instanceOffering, BillingInstanceOfferingUserConfig.class);
            String volumePriceConfig = null;
            if (config.getPriceUserConfig() != null) {
                volumePriceConfig = config.getPriceUserConfig().getRootVolume().getPriceKeyName();
            }

            Map<String, String> volumeDisplayAttribute = config.getDisplayAttribute() == null ? null : config.getDisplayAttribute().getRootVolume();
            createVolumeSystemTag(inv.getUuid(), volumePriceConfig, volumeDisplayAttribute);
        }
    }

    private void makeDataVolumeSystemTag(VolumeInventory inv) {
        String diskOffering = inv.getDiskOfferingUuid();
        String volumePriceConfig = null;

        if (!DiskOfferingSystemTags.DISK_OFFERING_USER_CONFIG.hasTag(diskOffering)) {
            return;
        }

        BillingDiskOfferingUserConfig config = OfferingUserConfigUtils.getDiskOfferingConfig(inv.getDiskOfferingUuid(), BillingDiskOfferingUserConfig.class);
        if (config.getPriceUserConfig() != null) {
            volumePriceConfig = config.getPriceUserConfig().getVolume().getPriceKeyName();
        }
        Map<String, String> volumeDisplayAttribute = config.getDisplayAttribute() == null ? null : config.getDisplayAttribute().getVolume();
        createVolumeSystemTag(inv.getUuid(), volumePriceConfig, volumeDisplayAttribute);
    }

    private void createVolumeSystemTag(String volumeUuid, String volumePriceConfig, Map<String, String> volumeDisplayAttribute) {
        SystemTagCreator creator;
        if(volumePriceConfig != null && !volumePriceConfig.isEmpty()){
            creator = BillingSystemTags.VOLUME_PRICE_USER_CONFIG.newSystemTagCreator(volumeUuid);
            creator.inherent = false;
            creator.unique = true;
            creator.setTagByTokens(map(e(BillingSystemTags.VOLUME_PRICE_USER_CONFIG_TOKEN, volumePriceConfig)));
            creator.create();
        }

        if(volumeDisplayAttribute != null){
            creator = VOLUME_ATTRIBUTE_USER_CONFIG.newSystemTagCreator(volumeUuid);
            creator.inherent = false;
            creator.unique = true;
            creator.setTagByTokens(map(e(BillingSystemTags.VOLUME_ATTRIBUTE_USER_CONFIG_TOKEN, JSONObjectUtil.toJsonString(volumeDisplayAttribute))));
            creator.create();
        }
    }

    @Override
    public void afterTakeOverResource(List<String> resourceUuids, String originAccountUuid, String newAccountUuid) {
        List<AccountResourceRefInventory> originInvs = resourceUuids.stream()
                        .map( uuid -> {
                            AccountResourceRefVO vo = Q.New(AccountResourceRefVO.class)
                                    .eq(AccountResourceRefVO_.resourceUuid, uuid)
                                    .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                                    .find();
                            vo.setAccountUuid(originAccountUuid);
                            return AccountResourceRefInventory.valueOf(vo);
                        }).collect(Collectors.toList());
        for (AccountResourceRefInventory originInv : originInvs) {
            resourceOwnerAfterChange(originInv, newAccountUuid);
        }
    }

    @Override
    public void validateInstanceOfferingUserConfig(String userConfig, String instanceOfferingUuid) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }

        BillingInstanceOfferingUserConfig config;

        try {
            config = OfferingUserConfigUtils.toObject(userConfig, BillingInstanceOfferingUserConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Syntax error(s) in billing instance offering user configuration, user configuration should write in json format.", e);
        }

        if (config.getPriceUserConfig() == null) {
            return;
        }

        InstanceOfferingPriceConfig priceConfig = config.getPriceUserConfig();
        if (priceConfig.getRootVolume() == null) {
            return;
        }

        PriceUserConfig rootVolumePriceConfig = priceConfig.getRootVolume();
        if (rootVolumePriceConfig.getPriceKeyName() == null) {
            throw new IllegalArgumentException("rootVolume price config is null");
        }
    }

    @Override
    public void validateDiskOfferingUserConfig(String userConfig, String diskOfferingUuid) {
        if (StringUtils.isBlank(userConfig)) {
            return;
        }

        BillingDiskOfferingUserConfig config;

        try {
            config = OfferingUserConfigUtils.toObject(userConfig, BillingDiskOfferingUserConfig.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Syntax error(s) in billing disk offering user configuration, user configuration should write in json format.", e);
        }

        if (config.getPriceUserConfig() == null) {
            return;
        }

        DiskOfferingPriceConfig priceConfig = config.getPriceUserConfig();
        if (priceConfig.getVolume() == null) {
            return;
        }

        PriceUserConfig volumePriceConfig = priceConfig.getVolume();
        if (volumePriceConfig.getPriceKeyName() == null) {
            throw new IllegalArgumentException("volume price config is null");
        }
    }
}
