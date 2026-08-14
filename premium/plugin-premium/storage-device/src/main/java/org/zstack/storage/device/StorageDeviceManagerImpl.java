package org.zstack.storage.device;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.NextVolumeDeviceIdGetter;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storageDevice.*;
import org.zstack.header.vm.BeforeGetNextVolumeDeviceIdExtensionPoint;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.GetVmStartingCandidateClustersHostsMsg;
import org.zstack.header.vm.GetVmStartingCandidateClustersHostsReply;
import org.zstack.header.vm.VmAttachOtherDiskExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMPingAgentNoFailureExtensionPoint;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.device.common.header.LunLocate;
import org.zstack.storage.device.fibreChannel.APIRefreshFiberChannelStorageEvent;
import org.zstack.storage.device.fibreChannel.APIRefreshFiberChannelStorageMsg;
import org.zstack.storage.device.fibreChannel.FiberChannelLunStruct;
import org.zstack.storage.device.fibreChannel.FiberChannelLunVO;
import org.zstack.storage.device.fibreChannel.FiberChannelLunVO_;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageInventory;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageVO;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageVO_;
import org.zstack.storage.device.fibreChannel.RefreshFiberChannelMsg;
import org.zstack.storage.device.fibreChannel.RefreshFiberChannelReply;
import org.zstack.storage.device.hba.*;
import org.zstack.storage.device.iscsi.APIAddIscsiServerEvent;
import org.zstack.storage.device.iscsi.APIAddIscsiServerMsg;
import org.zstack.storage.device.iscsi.APIAttachIscsiServerToClusterEvent;
import org.zstack.storage.device.iscsi.APIAttachIscsiServerToClusterMsg;
import org.zstack.storage.device.iscsi.APIDeleteIscsiServerEvent;
import org.zstack.storage.device.iscsi.APIDeleteIscsiServerMsg;
import org.zstack.storage.device.iscsi.APIDetachIscsiServerFromClusterEvent;
import org.zstack.storage.device.iscsi.APIDetachIscsiServerFromClusterMsg;
import org.zstack.storage.device.iscsi.APIRefreshIscsiServerEvent;
import org.zstack.storage.device.iscsi.APIRefreshIscsiServerMsg;
import org.zstack.storage.device.iscsi.APIUpdateIscsiServerEvent;
import org.zstack.storage.device.iscsi.APIUpdateIscsiServerMsg;
import org.zstack.storage.device.iscsi.IscsiLunInventory;
import org.zstack.storage.device.iscsi.IscsiLunVO;
import org.zstack.storage.device.iscsi.IscsiLunVO_;
import org.zstack.storage.device.iscsi.IscsiServerClusterRefVO;
import org.zstack.storage.device.iscsi.IscsiServerClusterRefVO_;
import org.zstack.storage.device.iscsi.IscsiServerInventory;
import org.zstack.storage.device.iscsi.IscsiServerVO;
import org.zstack.storage.device.iscsi.IscsiServerVO_;
import org.zstack.storage.device.iscsi.IscsiTargetInventory;
import org.zstack.storage.device.iscsi.IscsiTargetVO;
import org.zstack.storage.device.iscsi.IscsiTargetVO_;
import org.zstack.storage.device.iscsi.RefreshIscsiServerMsg;
import org.zstack.storage.device.iscsi.RefreshIscsiServerReply;
import org.zstack.storage.device.localRaid.APIGetLocalRaidPhysicalDriveSmartMsg;
import org.zstack.storage.device.localRaid.APIGetLocalRaidPhysicalDriveSmartReply;
import org.zstack.storage.device.localRaid.APILocateLocalRaidPhysicalDriveEvent;
import org.zstack.storage.device.localRaid.APILocateLocalRaidPhysicalDriveMsg;
import org.zstack.storage.device.localRaid.APIRefreshLocalRaidEvent;
import org.zstack.storage.device.localRaid.APIRefreshLocalRaidMsg;
import org.zstack.storage.device.localRaid.APISelfTestLocalRaidEvent;
import org.zstack.storage.device.localRaid.APISelfTestLocalRaidMsg;
import org.zstack.storage.device.localRaid.LocateLocalRaidPhysicalDriveMsg;
import org.zstack.storage.device.localRaid.LocateLocalRaidPhysicalDriveReply;
import org.zstack.storage.device.localRaid.LocateStatus;
import org.zstack.storage.device.localRaid.PhysicalDriveSmartSelfTestHistoryVO;
import org.zstack.storage.device.localRaid.RaidControllerInventory;
import org.zstack.storage.device.localRaid.RaidControllerVO;
import org.zstack.storage.device.localRaid.RaidControllerVO_;
import org.zstack.storage.device.localRaid.RaidPhysicalDriveInventory;
import org.zstack.storage.device.localRaid.RaidPhysicalDriveStruct;
import org.zstack.storage.device.localRaid.RaidPhysicalDriveVO;
import org.zstack.storage.device.localRaid.RaidPhysicalDriveVO_;
import org.zstack.storage.device.localRaid.RefreshLocalRaidMsg;
import org.zstack.storage.device.localRaid.RefreshLocalRaidReply;
import org.zstack.storage.device.localRaid.RunningState;
import org.zstack.storage.device.localRaid.SelfTestLocalRaidMsg;
import org.zstack.storage.device.localRaid.SelfTestLocalRaidReply;
import org.zstack.storage.device.localRaid.SmartDataStruct;
import org.zstack.storage.device.multipath.APIGetHostMultipathTopologyMsg;
import org.zstack.storage.device.multipath.APIGetHostMultipathTopologyReply;
import org.zstack.storage.device.multipath.DeviceTO;
import org.zstack.storage.device.multipath.MultipathTopologyStruct;
import org.zstack.storage.device.nvme.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.PersistenceException;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.storageDevice.StorageDeviceConstants.SERVICE_ID;
import static org.zstack.header.storageDevice.StorageDeviceConstants.allowedVmOperationStates;
import static org.zstack.storage.primary.sharedblock.SharedBlockKvmFactory.checkSharedBlockGroupPrimaryStorageDisconnectedOnHost;

/**
 * Create by weiwang at 2018/8/3
 */
public class StorageDeviceManagerImpl extends AbstractService implements StorageDeviceManager,
        HostConnectionReestablishExtensionPoint, KVMHostConnectExtensionPoint, KVMPingAgentNoFailureExtensionPoint,
        BeforeGetNextVolumeDeviceIdExtensionPoint, KVMStartVmExtensionPoint,
        PrepareDbInitialValueExtensionPoint, HostAfterConnectedExtensionPoint, VmAttachOtherDiskExtensionPoint {
    private static final CLogger logger = Utils.getLogger(StorageDeviceManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    ResourceConfigFacade rcf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private RESTFacade restf;


    private final Map<String, StorageDeviceBackend> backends = new HashMap<>();

    @Override
    public boolean start() {
        populateExtensions();
        installGlobalConfigValidator();

        restf.registerSyncHttpCallHandler(KVMConstant.HOST_STORAGEDEVICE_HBA_STATE_EVENT, KVMAgentCommands.HostStorageDeviceHbaStateEventCmd.class, cmd -> {
            HostCanonicalEvents.HostPhysicalHbaPortStateAbnormalData cdata = new HostCanonicalEvents.HostPhysicalHbaPortStateAbnormalData();
            FcHbaDeviceVO hba = Q.New(FcHbaDeviceVO.class)
                    .eq(FcHbaDeviceVO_.hostUuid, cmd.host)
                    .eq(FcHbaDeviceVO_.name, cmd.name)
                    .eq(FcHbaDeviceVO_.portName, cmd.portName)
                    .find();

            if (hba == null) {
                return null;
            }

            if (hba.getPortState().equals(cmd.portState)) {
                return null;
            }

            cdata.setHostUuid(cmd.host);
            cdata.setName(cmd.name);
            cdata.setPortName(cmd.portName);
            cdata.setOldPortState(hba.getPortState());
            cdata.setNewPortState(cmd.portState);
            evtf.fire(HostCanonicalEvents.HOST_PHYSICAL_HBA_STATE_ABNORMAL, cdata);
            return null;
        });

        evtf.on(HostCanonicalEvents.HOST_PHYSICAL_HBA_STATE_ABNORMAL, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                HostCanonicalEvents.HostPhysicalHbaPortStateAbnormalData cdata = (HostCanonicalEvents.HostPhysicalHbaPortStateAbnormalData) data;
                FcHbaDeviceVO hba = Q.New(FcHbaDeviceVO.class)
                        .eq(FcHbaDeviceVO_.hostUuid, cdata.getHostUuid())
                        .eq(FcHbaDeviceVO_.name, cdata.getName())
                        .eq(FcHbaDeviceVO_.portName, cdata.getPortName())
                        .find();
                if (hba != null && !hba.getPortState().equals(cdata.getNewPortState())) {
                    hba.setPortState(cdata.getNewPortState());
                    dbf.update(hba);
                }
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(StorageDeviceConstants.SERVICE_ID);
    }

    private void installGlobalConfigValidator() {
        StorageDeviceGlobalConfig.MULTIPATH_BLACKLIST.installValidateExtension((category, name, oldValue, newValue) -> {
            if ("".equals(newValue)) {
                return;
            }
            Set<String> vaildKeys = new HashSet<>(Arrays.asList("wwid", "device", "devnode", "property", "protocol"));
            Set<String> deviceVaildKeys = new HashSet<>(Arrays.asList("vendor", "product"));
            try {
                DebugUtils.Assert(StringUtils.isNotBlank(newValue), "");
                ArrayList blacklist = JSONObjectUtil.toObject(newValue, ArrayList.class);
                for (Object item : blacklist) {
                    Map<String, Object> itemMap = (Map) item;
                    DebugUtils.Assert(itemMap.size() == 1, "");
                    String itemKey = (String)(itemMap.keySet().toArray()[0]);
                    DebugUtils.Assert(vaildKeys.contains(itemKey), "");
                    Object itemValue = itemMap.values().toArray()[0];
                    if (itemKey.equals("device")) {
                        DebugUtils.Assert(itemValue instanceof ArrayList, "");
                        ArrayList device = (ArrayList) itemValue;
                        for (Object subitem : device) {
                            Map<String, String> subMap = (Map) subitem;
                            DebugUtils.Assert(subMap.size() == 1, "");
                            DebugUtils.Assert(deviceVaildKeys.contains(subMap.keySet().toArray()[0]), "");
                        }
                    } else {
                        DebugUtils.Assert(itemValue instanceof String, "");
                    }
                }
            } catch (Exception e) {
                throw new GlobalConfigException("invaild multipath blacklist config, it must be a array, for example: " +
                        "[{device:[{vendor:\"IBM\"}, {product:\"3S42\"}]}, {wwid:\"36001405913ad48768b84db39bbcc5cb0\"}]");
            }
        });
    }

    private void populateExtensions() {
        for (StorageDeviceBackend ext : pluginRgty.getExtensionList(StorageDeviceBackend.class)) {
            StorageDeviceBackend old = backends.get(ext.getSupportHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate StorageDeviceBackend[%s,%s] for type[%s]", old.getClass().getName(),
                        ext.getClass().getName(), ext.getSupportHypervisorType()));
            }
            backends.put(ext.getSupportHypervisorType(), ext);
        }
    }

    @Override
    public StorageDeviceBackend getStorageDeviceBackend(String hypervisorType) {
        StorageDeviceBackend bkd = backends.get(hypervisorType);
        if (bkd == null) {
            throw new CloudRuntimeException(String.format("cannot find StorageDeviceBackend for type[%s]", hypervisorType));
        }

        return bkd;
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
        if (msg instanceof RefreshIscsiServerMsg) {
            handle((RefreshIscsiServerMsg) msg);
        } else if (msg instanceof RefreshFiberChannelMsg) {
            handle((RefreshFiberChannelMsg) msg);
        } else if (msg instanceof RefreshNvmeMsg) {
            handle((RefreshNvmeMsg) msg);
        } else if (msg instanceof ConnectNvmeServerMsg) {
            handle((ConnectNvmeServerMsg) msg);
        } else if (msg instanceof RefreshLocalRaidMsg) {
            handle((RefreshLocalRaidMsg) msg);
        } else if (msg instanceof SelfTestLocalRaidMsg) {
            handle((SelfTestLocalRaidMsg) msg);
        } else if (msg instanceof LocateLocalRaidPhysicalDriveMsg) {
            handle((LocateLocalRaidPhysicalDriveMsg) msg);
        } else if (msg instanceof RefreshHbaDeviceMsg) {
            handle((RefreshHbaDeviceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(LocateLocalRaidPhysicalDriveMsg msg) {
        LocateLocalRaidPhysicalDriveReply reply = new LocateLocalRaidPhysicalDriveReply();

        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getControllerInventory().getHostUuid()).find();
        StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
        bkd.locateLocalRaidPhysicalDrive(msg.getPhysicalDriveInventory(), msg.getControllerInventory(), msg.getLocate(), new Completion(msg) {
            @Override
            public void success() {
                RaidPhysicalDriveVO physicalDriveVO = Q.New(RaidPhysicalDriveVO.class).eq(RaidPhysicalDriveVO_.uuid, msg.getPhysicalDriveInventory().getUuid()).find();
                if (msg.getLocate()) {
                    physicalDriveVO.setLocateStatus(LocateStatus.Enabled);
                } else {
                    physicalDriveVO.setLocateStatus(LocateStatus.Disabled);
                }
                reply.setInventory(RaidPhysicalDriveInventory.valueOf(dbf.updateAndRefresh(physicalDriveVO)));
                reply.setSuccess(true);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(SelfTestLocalRaidMsg msg) {
        PhysicalDriveSmartSelfTestHistoryVO historyVO = new PhysicalDriveSmartSelfTestHistoryVO();
        historyVO.setRaidPhysicalDriveUuid(msg.getUuid());
        historyVO.setRunningState(RunningState.Running);
        dbf.persist(historyVO);

        RaidPhysicalDriveVO vo = Q.New(RaidPhysicalDriveVO.class).eq(RaidPhysicalDriveVO_.uuid, msg.getUuid()).find();
        RaidControllerVO controllerVO = Q.New(RaidControllerVO.class).eq(RaidControllerVO_.uuid, vo.getRaidControllerUuid()).find();
        String hypervisorType = Q.New(HostVO.class).select(HostVO_.hypervisorType).eq(HostVO_.uuid, controllerVO.getHostUuid()).findValue();
        StorageDeviceBackend bkd = backends.get(hypervisorType);
        bkd.localRaidSelfTest(RaidPhysicalDriveInventory.valueOf(vo), RaidControllerInventory.valueOf(controllerVO), new ReturnValueCompletion<SelfTestLocalRaidReply>(msg) {
            @Override
            public void success(SelfTestLocalRaidReply returnValue) {
                historyVO.setRunningState(RunningState.Success);
                historyVO.setTestResult(returnValue.getResult());
                dbf.updateAndRefresh(historyVO);
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                historyVO.setRunningState(RunningState.Failed);
                historyVO.setTestResult(errorCode.getDetails());
                dbf.updateAndRefresh(historyVO);

                SelfTestLocalRaidReply reply = new SelfTestLocalRaidReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(RefreshHbaDeviceMsg msg) {
        RefreshHbaDeviceReply reply = new RefreshHbaDeviceReply();
        HostVO hostVO = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .find();

        StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
        bkd.scanHba(HostInventory.valueOf(hostVO), new ReturnValueCompletion<List<HbaDeviceStruct>>(msg) {

            @Override
            public void success(List<HbaDeviceStruct> hbaDeviceStructs) {
                syncHbaDevicesFromAgent(hbaDeviceStructs, msg.getHostUuid(), new Completion(msg) {

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
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handle(RefreshLocalRaidMsg msg) {
        RefreshLocalRaidReply reply = new RefreshLocalRaidReply();
        HostVO hostVO = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .find();

        StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
        bkd.scanLocalRaid(HostInventory.valueOf(hostVO), new ReturnValueCompletion<List<RaidPhysicalDriveStruct>>(msg) {
            @Override
            public void success(List<RaidPhysicalDriveStruct> returnValue) {
                syncLocalRaidDbFromAgent(returnValue, msg.getHostUuid(), new Completion(msg) {
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
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private List<String> getDiskIdentifiers(List<String> scsiLunUuids) {
        if (scsiLunUuids == null || scsiLunUuids.isEmpty()) {
            return null;
        }

        return Q.New(ScsiLunVO.class)
                .in(ScsiLunVO_.uuid, scsiLunUuids)
                .select(ScsiLunVO_.wwid)
                .listValues();
    }

    private void handle(RefreshFiberChannelMsg msg) {
        RefreshFiberChannelReply reply = new RefreshFiberChannelReply();
        thdf.singleFlightSubmit(new SingleFlightTask(msg)
                .setSyncSignature(String.format("refresh-fiber-channel-on-host-%s", msg.getHostUuid()))
                .run(completion -> {
                    HostVO hostVO = Q.New(HostVO.class)
                            .eq(HostVO_.uuid, msg.getHostUuid())
                            .find();

                    List<String> identifiers = getDiskIdentifiers(msg.getScsiLunUuids());
                    StorageDeviceBackend bkd = getStorageDeviceBackend(hostVO.getHypervisorType());
                    bkd.scanFcDevice(HostInventory.valueOf(hostVO), msg.isRescan(), identifiers, new ReturnValueCompletion<List<FiberChannelLunStruct>>(completion) {
                        @Override
                        public void success(List<FiberChannelLunStruct> structs) {
                            List<FiberChannelLunStruct> res = structs == null ? Collections.emptyList() :
                                    structs.stream().filter(FiberChannelLunStruct::isValid).collect(Collectors.toList());

                            syncFiberChannelDbFromAgent(res, hostVO.getUuid(), new Completion(completion) {
                                @Override
                                public void success() {
                                    completion.success(null);
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    completion.fail(errorCode);
                                }
                            });
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("scan fc device failed: %s", errorCode));
                            completion.fail(errorCode);
                        }
                    });
                }).done(result -> {
                    if (!result.isSuccess()) {
                        reply.setError(result.getErrorCode());
                    }
                    bus.reply(msg, reply);
                })
        );
    }

    private void handle(RefreshNvmeMsg msg) {
        logger.debug(String.format("now refresh nvme targets on host %s", msg.getHostUuid()));
        RefreshNvmeReply reply = new RefreshNvmeReply();

        HostVO hostVO = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .find();

        List<String> identifiers = getDiskIdentifiers(msg.getNvmeLunUuids());
        StorageDeviceBackend bkd = getStorageDeviceBackend(hostVO.getHypervisorType());
        bkd.scanNvmeDevice(HostInventory.valueOf(hostVO), msg.isRescan(), identifiers, new ReturnValueCompletion<List<NvmeLunStruct>>(msg) {
            @Override
            public void success(List<NvmeLunStruct> structs) {
                List<NvmeLunStruct> res = structs == null ? Collections.emptyList() :
                        structs.stream().filter(NvmeLunStruct::isValid).collect(Collectors.toList());

                syncNvmeDbFromAgent(res, hostVO.getUuid(), new Completion(msg) {
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
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("scan nvme device failed: %s", errorCode));
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(ConnectNvmeServerMsg msg) {
        logger.debug(String.format("now connect nvme server %s", msg.getNvmeServerUuid()));
        ConnectNvmeServerReply reply = new ConnectNvmeServerReply();

        HostVO hostVO = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .find();

        NvmeServerVO nvmeServerVO = Q.New(NvmeServerVO.class)
                    .eq(NvmeServerVO_.uuid, msg.getNvmeServerUuid())
                    .find();

        StorageDeviceBackend bkd = getStorageDeviceBackend(hostVO.getHypervisorType());
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setChainName(String.format("connect-nvme-server-%s:%s", nvmeServerVO.getIp(), nvmeServerVO.getPort()));
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                thdf.singleFlightSubmit(new SingleFlightTask(trigger)
                        .setSyncSignature(String.format("connect-nvme-server-%s-on-host-%s", nvmeServerVO.getUuid(), msg.getHostUuid()))
                        .run(completion -> {
                            bkd.connectNvmeServer(nvmeServerVO, HostInventory.valueOf(hostVO), new ReturnValueCompletion<NvmeServerInventory>(completion) {
                                @Override
                                public void success(NvmeServerInventory serverInventory) {
                                    reply.setNvmeTargetInventories(serverInventory.getNvmeTargets());
                                    completion.success(serverInventory.getNvmeTargets());
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    completion.fail(errorCode);
                                }
                            });
                        }).done(result -> {
                            if (result.isSuccess()) {
                                trigger.next();
                            } else {
                                logger.warn(String.format("connect nvme device failed: %s", result.getErrorCode().getDetails()));
                                trigger.fail(result.getErrorCode());
                            }
                        }));
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                RefreshNvmeMsg refreshNvmeMsg = new RefreshNvmeMsg();
                refreshNvmeMsg.setHostUuid(hostVO.getUuid());
                bus.makeTargetServiceIdByResourceUuid(refreshNvmeMsg, SERVICE_ID, hostVO.getZoneUuid());
                bus.send(refreshNvmeMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }
                        updateTargetServerUuid();
                        trigger.next();
                    }
                    private void updateTargetServerUuid() {
                        List<String> nqns = reply.getNvmeTargetInventories().stream().map(NvmeTargetInventory::getNqn)
                                .collect(Collectors.toList());
                        if (!nqns.isEmpty()) {
                            SQL.New(NvmeTargetVO.class).in(NvmeTargetVO_.nqn, nqns)
                                    .set(NvmeTargetVO_.nvmeServerUuid, nvmeServerVO.getUuid())
                                    .update();
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void doSyncFiberChannelDbFromAgent(List<FiberChannelLunStruct> structs, String hostUuid) {
        if (structs == null || structs.isEmpty()) {
            SQL.New("delete from ScsiLunHostRefVO ref where ref.hostUuid = :hostUuid " +
                    "and ref.scsiLunUuid in (select lun.uuid from ScsiLunVO lun where " +
                    "lun.source = :source)")
                    .param("hostUuid", hostUuid)
                    .param("source", StorageDeviceConstants.FIBER_CHANNEL)
                    .execute();
            doClearStaleFiberChannel();
            return;
        }

        Set<Object> seen = new HashSet<>();
        Set<String> validSerials = new HashSet<>(), validWwids = new HashSet<>(), validStorageWwnn = new HashSet<>();
        for (FiberChannelLunStruct s : structs) {
            if (seen.add(s.getWwid() + s.storageWwnn)) {
                syncSingleFiberChannelDbFromAgent(s, hostUuid);
            }

            validSerials.add(s.serial);
            validWwids.addAll(s.wwids);
            validStorageWwnn.add(s.storageWwnn);
        }

        List<String> validateFCUuids = Q.New(FiberChannelStorageVO.class).select(FiberChannelStorageVO_.uuid)
                .in(FiberChannelStorageVO_.wwnn, validStorageWwnn)
                .listValues();
        List<ScsiLunHostRefVO> refs = Q.New(ScsiLunHostRefVO.class).eq(ScsiLunHostRefVO_.hostUuid, hostUuid).list();
        List<FiberChannelLunVO> luns = Q.New(FiberChannelLunVO.class)
                .in(FiberChannelLunVO_.uuid, refs.stream().map(ScsiLunHostRefVO::getScsiLunUuid).collect(Collectors.toList()))
                .eq(FiberChannelLunVO_.source, StorageDeviceConstants.FIBER_CHANNEL)
                .list();

        Set<String> invalidLunUuids = luns.stream().filter(v -> !validSerials.contains(v.getSerial())
                || !validateFCUuids.contains(v.getFiberChannelStorageUuid())
                || !validWwids.contains(v.getWwid()))
                .map(FiberChannelLunVO::getUuid)
                .collect(Collectors.toSet());

        logger.debug(String.format("scsi lun %s no longer at host %s", invalidLunUuids, hostUuid));

        dbf.removeCollection(
                refs.stream().filter(r -> invalidLunUuids.contains(r.getScsiLunUuid())).collect(Collectors.toList()), ScsiLunHostRefVO.class);

        doClearStaleFiberChannel();
    }

    private void doClearStaleRaidController(String hostUuid) {
        List<String> controllerUuids = Q.New(RaidControllerVO.class)
                .select(RaidControllerVO_.uuid).eq(RaidControllerVO_.hostUuid, hostUuid).listValues();
        if (controllerUuids == null || controllerUuids.isEmpty()) {
            return;
        }

        for (String controllerUuid : controllerUuids) {
            Long count = Q.New(RaidPhysicalDriveVO.class).eq(RaidPhysicalDriveVO_.raidControllerUuid, controllerUuid).count();
            if (count != 0) {
                continue;
            }

            dbf.removeByPrimaryKey(controllerUuid, RaidControllerVO.class);
        }
    }

    private void doSyncHbaDeviceDbFromAgent(List<HbaDeviceStruct> structs, String hostUuid) {
        if (structs == null || structs.isEmpty()) {
            SQL.New(HbaDeviceVO.class).eq(HbaDeviceVO_.hostUuid, hostUuid).delete();
            return;
        }

        List<String> portNames = structs.stream().map(HbaDeviceStruct::getPortName)
                .collect(Collectors.toList());
        List<String> hbaUuids = Q.New(FcHbaDeviceVO.class)
                .select(FcHbaDeviceVO_.uuid)
                .eq(FcHbaDeviceVO_.hostUuid, hostUuid)
                .notIn(FcHbaDeviceVO_.portName, portNames)
                .listValues();
        dbf.removeByPrimaryKeys(hbaUuids, HbaDeviceVO.class);

        for (HbaDeviceStruct s : structs) {
            syncSingleHBADeviceDbFromAgent(s, hostUuid);
        }
    }
    private void syncSingleHBADeviceDbFromAgent(HbaDeviceStruct s, String hostUuid) {
        FcHbaDeviceVO hba = Q.New(FcHbaDeviceVO.class)
                .eq(FcHbaDeviceVO_.hostUuid, hostUuid)
                .eq(FcHbaDeviceVO_.portName, s.getPortName())
                .find();

        if (hba == null) {
            hba = new FcHbaDeviceVO();
            hba.setHostUuid(hostUuid);
            hba.setHbaType(HbaType.FC);
            hba.setName(s.getName());
            hba.setPortName(s.getPortName());
            hba.setPortState(s.getPortState());
            hba.setSpeed(s.getSpeed());
            hba.setSymbolicName(s.getSymbolicName());
            hba.setSupportedSpeeds(s.getSupportedSpeeds());
            hba.setSupportedClasses(s.getSupportedClasses());
            hba.setNodeName(s.getNodeName());
            hba.setUuid(Platform.getUuid());
            dbf.persist(hba);
            return;
        }

        if (!hba.getPortState().toString().equals(s.getPortState())) {
            hba.setPortState(s.getPortState());
            hba.setSpeed(s.getSpeed());
            dbf.update(hba);
        }
    }

    private void doSyncLocalRaidDbFromAgent(List<RaidPhysicalDriveStruct> structs, String hostUuid) {
        List<RaidControllerVO> controllerVOList = Q.New(RaidControllerVO.class).eq(RaidControllerVO_.hostUuid, hostUuid).list();
        Map<String, RaidControllerVO> controllerVOS = controllerVOList.stream().collect(Collectors.toMap(RaidControllerVO::getSasAddress, v -> v));
        List<String> controllerUuids = controllerVOS.values().stream().map(RaidControllerVO::getUuid).collect(Collectors.toList());
        Map<String, RaidPhysicalDriveVO> driveVOS = new HashMap<>();

        if (!controllerVOS.isEmpty()) {
            driveVOS = Maps.uniqueIndex(Q.New(RaidPhysicalDriveVO.class)
                    .in(RaidPhysicalDriveVO_.raidControllerUuid, controllerUuids).list(), vo -> {
                return vo.getWwn();
            });
        }

        if (structs == null || structs.isEmpty()) {
            if (controllerUuids.isEmpty()) {
                return;
            }

            if (driveVOS != null && !driveVOS.isEmpty()) {
                dbf.removeCollection(driveVOS.values(), RaidControllerVO.class);
            }
            dbf.removeByPrimaryKeys(controllerUuids, RaidControllerVO.class);
            return;
        }

        List<String> agentWwns = structs.stream().map(RaidPhysicalDriveStruct::getWwn).collect(Collectors.toList());

        for (RaidPhysicalDriveStruct s : structs) {
            if (s.getWwn() == null || s.getRaidControllerSasAddreess() == null) {
                logger.warn(String.format("illegal raid physical drive struct: %s", s));
                continue;
            }

            if (!controllerVOS.containsKey(s.getRaidControllerSasAddreess())) {
                RaidControllerVO controllerVO = new RaidControllerVO();
                controllerVO.setHostUuid(hostUuid);
                controllerVO.setName(String.format("raid-controller-%s", s.getRaidControllerProductName())
                        .replace(" ", "-").toLowerCase());
                controllerVO.setProductName(s.getRaidControllerProductName());
                controllerVO.setAdapterNumber(s.getRaidControllerNumber());
                controllerVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
                controllerVO.setLastOpDate(new Timestamp(System.currentTimeMillis()));
                controllerVO.setSasAddress(s.getRaidControllerSasAddreess());
                controllerVO.setUuid(Platform.getUuid());

                dbf.persist(controllerVO);
                controllerVOS.put(controllerVO.getSasAddress(), controllerVO);
            }

            RaidPhysicalDriveVO physicalDriveVO = driveVOS.get(s.getWwn());

            if (physicalDriveVO == null) {
                dbf.persist(s.toVO(controllerVOS.get(s.getRaidControllerSasAddreess()).getUuid()));
                continue;
            } else if (!physicalDriveVO.getRaidControllerUuid().equals(
                    controllerVOS.get(s.getRaidControllerSasAddreess()).getUuid())) {
                physicalDriveVO.setRaidControllerUuid(controllerVOS.get(s.getRaidControllerSasAddreess()).getUuid());
                dbf.update(physicalDriveVO);
            } else if (s.compareToVO(physicalDriveVO)) {
                continue;
            }

            physicalDriveVO.updateFromStruct(s);
            dbf.update(physicalDriveVO);
        }

        for (RaidPhysicalDriveVO driveVO : driveVOS.values()) {
            if (!agentWwns.contains(driveVO.getWwn())) {
                dbf.remove(driveVO);
            }
        }

        doClearStaleRaidController(hostUuid);
    }

    private void syncHbaDevicesFromAgent(List<HbaDeviceStruct> structs, String hostUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-hba-device-on-host-%s", hostUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncHbaDeviceDbFromAgent(structs, hostUuid);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void syncLocalRaidDbFromAgent(List<RaidPhysicalDriveStruct> struct, String hostUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-local-raid-on-host-%s", hostUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncLocalRaidDbFromAgent(struct, hostUuid);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void syncFiberChannelDbFromAgent(List<FiberChannelLunStruct> struct, String hostUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "clear-stale-fiber-channel";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncFiberChannelDbFromAgent(struct, hostUuid);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return "sync-fiber-channel";
            }
        });
    }

    private void doClearStaleFiberChannel() {
        logger.debug("clearing fiber channel db");
        List<FiberChannelLunVO> luns = Q.New(FiberChannelLunVO.class)
                .eq(FiberChannelLunVO_.source, StorageDeviceConstants.FIBER_CHANNEL)
                .list();

        for (FiberChannelLunVO vo : luns) {
            if (vo.getScsiLunHostRefs() == null || vo.getScsiLunHostRefs().isEmpty()) {
                SQL.New(ScsiLunVmInstanceRefVO.class).eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, vo.getUuid()).delete();
                vo.setScsiLunVmInstanceRefs(null);
                dbf.remove(vo);
                logger.debug(String.format("cleared staled fiber channel lun %s", vo.getUuid()));
            }
        }

        List<FiberChannelStorageVO> storageVOS = Q.New(FiberChannelStorageVO.class).list();
        for (FiberChannelStorageVO vo : storageVOS) {
            if (vo.getFiberChannelLuns() == null || vo.getFiberChannelLuns().isEmpty()) {
                dbf.remove(vo);
                logger.debug(String.format("cleared staled fiber channel storage %s", vo.getUuid()));
            }
        }
    }

    private void syncSingleFiberChannelDbFromAgent(FiberChannelLunStruct struct, String hostUuid) {
        logger.debug(String.format("syncing fiber channel lun: %s from host %s", struct.getWwid(), hostUuid));
        FiberChannelStorageVO storageVO = Q.New(FiberChannelStorageVO.class).eq(FiberChannelStorageVO_.wwnn, struct.getStorageWwnn()).find();
        if (storageVO == null) {
            storageVO = new FiberChannelStorageVO(struct.getStorageWwnn());
            dbf.persist(storageVO);
        }

        FiberChannelLunVO lunVO = Q.New(FiberChannelLunVO.class)
                .eq(FiberChannelLunVO_.serial, struct.getSerial())
                .eq(FiberChannelLunVO_.wwid, struct.getWwid())
                .eq(FiberChannelLunVO_.fiberChannelStorageUuid, storageVO.getUuid()).find();
        if (lunVO == null) {
            lunVO = new FiberChannelLunVO(struct, storageVO.getUuid());
            dbf.persist(lunVO);
        } else if (!lunVO.getWwid().equals(struct.getWwid()) || !lunVO.getSize().equals(struct.getSize()) || !lunVO.getType().equals(struct.getType())) {
            lunVO.setWwid(struct.getWwid());
            lunVO.setSize(struct.getSize());
            lunVO.setType(struct.getType());
            dbf.update(lunVO);
        }


        ScsiLunHostRefVO refVO = Q.New(ScsiLunHostRefVO.class).eq(ScsiLunHostRefVO_.scsiLunUuid, lunVO.getUuid())
                .eq(ScsiLunHostRefVO_.hostUuid, hostUuid).find();
        if (refVO == null) {
            refVO = new ScsiLunHostRefVO();
            refVO.setScsiLunUuid(lunVO.getUuid());
            refVO.setHostUuid(hostUuid);
            refVO.setPath(struct.getPath());
            refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
            refVO.setLastOpDate(new Timestamp(System.currentTimeMillis()));
            dbf.persist(refVO);
        } else if (!Objects.equals(refVO.getPath(), struct.path)) {
            refVO.setPath(struct.path);
            dbf.update(refVO);
        }
    }

    private void syncNvmeDbFromAgent(List<NvmeLunStruct> structs, String hostUuid, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "sync-nvme-devices";
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncNvmeDbFromAgent(structs, hostUuid);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return "sync-nvme";
            }
        });
    }

    private void doSyncNvmeDbFromAgent(List<NvmeLunStruct> structs, String hostUuid) {
        if (CollectionUtils.isEmpty(structs)) {
            SQL.New(NvmeLunHostRefVO.class).eq(NvmeLunHostRefVO_.hostUuid, hostUuid).delete();
            doClearStaleNvme();
            return;
        }

        Set<Object> seen = new HashSet<>();
        Set<String> validSerials = new HashSet<>(), validWwids = new HashSet<>(), validNqn = new HashSet<>();
        for (NvmeLunStruct s : structs) {
            if (seen.add(s.getWwid() + s.nqn)) {
                syncSingleNvmeDbFromAgent(s, hostUuid);
            }

            validSerials.add(s.serial);
            validWwids.addAll(s.wwids);
            validNqn.add(s.nqn);
        }

        List<String> validateNVMeUuids = validNqn.isEmpty() ? Collections.emptyList() : Q.New(NvmeTargetVO.class).select(NvmeTargetVO_.uuid)
                .in(NvmeTargetVO_.nqn, validNqn)
                .listValues();
        List<NvmeLunHostRefVO> refs = Q.New(NvmeLunHostRefVO.class).eq(NvmeLunHostRefVO_.hostUuid, hostUuid).list();
        List<NvmeLunVO> luns = refs.isEmpty() ? Collections.emptyList() : Q.New(NvmeLunVO.class)
                .in(NvmeLunVO_.uuid, refs.stream().map(NvmeLunHostRefVO::getNvmeLunUuid).collect(Collectors.toList()))
                .list();

        Set<String> invalidLunUuids = luns.stream().filter(v -> !validSerials.contains(v.getSerial())
                        || !validateNVMeUuids.contains(v.getNvmeTargetUuid())
                        || !validWwids.contains(v.getWwid()))
                .map(NvmeLunVO::getUuid)
                .collect(Collectors.toSet());

        logger.debug(String.format("nvme lun %s no longer at host %s", invalidLunUuids, hostUuid));

        dbf.removeCollection(
                refs.stream().filter(r -> invalidLunUuids.contains(r.getNvmeLunUuid())).collect(Collectors.toList()), NvmeLunHostRefVO.class);

        doClearStaleNvme();
    }

    private void syncSingleNvmeDbFromAgent(NvmeLunStruct struct, String hostUuid) {
        logger.debug(String.format("syncing NVMe lun: %s from host %s", struct.getWwid(), hostUuid));
        NvmeTargetVO storageVO = Q.New(NvmeTargetVO.class).eq(NvmeTargetVO_.nqn, struct.getNqn()).find();
        if (storageVO == null) {
            storageVO = new NvmeTargetVO(struct.getNqn());
            dbf.persist(storageVO);
            logger.debug(String.format("found new nvme target[nqn:%s] on host[uuid:%s]", struct.getNqn(), hostUuid));
        }

        NvmeLunVO lunVO = Q.New(NvmeLunVO.class)
                .eq(NvmeLunVO_.serial, struct.getSerial())
                .eq(NvmeLunVO_.wwn, struct.wwn)
                .eq(NvmeLunVO_.nvmeTargetUuid, storageVO.getUuid()).find();
        if (lunVO == null) {
            lunVO = new NvmeLunVO(struct, storageVO.getUuid());
            dbf.persist(lunVO);
            logger.debug(String.format("found new nvme lun[wwid:%s] on host[uuid:%s]", lunVO.getWwid(), hostUuid));
        } else if (!lunVO.getWwid().equals(struct.getWwid()) || !lunVO.getSize().equals(struct.getSize()) || !lunVO.getType().equals(struct.getType())) {
            lunVO.setWwid(struct.getWwid());
            lunVO.setSize(struct.getSize());
            lunVO.setType(struct.getType());
            dbf.update(lunVO);
        }

        NvmeLunHostRefVO refVO = Q.New(NvmeLunHostRefVO.class)
                .eq(NvmeLunHostRefVO_.nvmeLunUuid, lunVO.getUuid())
                .eq(NvmeLunHostRefVO_.hostUuid, hostUuid)
                .find();
        if (refVO == null) {
            refVO = new NvmeLunHostRefVO();
            refVO.setNvmeLunUuid(lunVO.getUuid());
            refVO.setHostUuid(hostUuid);

            String transport = struct.getTransport() == null ? "" : struct.getTransport().toUpperCase();
            LunLocate locate = LunLocate.fromTransport(struct.transport);
            locate = locate == null ? LunLocate.Remote : locate; // remote by default

            refVO.setTransport(transport);
            refVO.setLocate(locate);
            refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
            refVO.setLastOpDate(new Timestamp(System.currentTimeMillis()));
            dbf.persist(refVO);
        } else {
            boolean updated = false;

            LunLocate locate = LunLocate.fromTransport(struct.transport);
            locate = locate == null ? LunLocate.Remote : locate;
            String transport = struct.transport == null ? null : struct.transport.toUpperCase();
            if (!Objects.equals(locate, refVO.getLocate())) {
                refVO.setLocate(locate);
                updated = true;
            }
            if (!Objects.equals(transport, refVO.getTransport())) {
                refVO.setTransport(transport);
                updated = true;
            }

            if (updated) {
                dbf.update(refVO);
            }
        }
    }

    private void doClearStaleNvme() {
        logger.debug("clearing NVMe db");
        List<NvmeLunVO> luns = Q.New(NvmeLunVO.class).list();

        for (NvmeLunVO vo : luns) {
            if (CollectionUtils.isEmpty(vo.getNvmeLunHostRefs())) {
                dbf.remove(vo);
                logger.debug(String.format("cleared staled NVMe lun[uuid:%s, wwid:%s]", vo.getUuid(), vo.getWwid()));
            }
        }

        List<NvmeTargetVO> storageVOS = Q.New(NvmeTargetVO.class).list();
        for (NvmeTargetVO vo : storageVOS) {
            if (CollectionUtils.isEmpty(vo.getNvmeLuns())) {
                dbf.remove(vo);
                logger.debug(String.format("cleared staled NVMe target[uuid:%s, nqn:%s]", vo.getUuid(), vo.getNqn()));
            }
        }
    }

    private void handle(RefreshIscsiServerMsg msg) {
        RefreshIscsiServerReply reply = new RefreshIscsiServerReply();
        IscsiServerVO iscsiServerVO = Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.uuid, msg.getIscsiServerUuid())
                .find();
        logger.debug(String.format("now refesh iscsi server %s", msg.getIscsiServerUuid()));

        HostVO hostVO = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .find();

        StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
        thdf.singleFlightSubmit(new SingleFlightTask(msg)
                .setSyncSignature(String.format("refresh-iscsi-server-%s-on-host-%s", iscsiServerVO.getUuid(), hostVO.getUuid()))
                .run(completion -> {
                    bkd.loginIscsiServer(iscsiServerVO, HostInventory.valueOf(hostVO), new ReturnValueCompletion<IscsiServerInventory>(completion) {
                        @Override
                        public void success(IscsiServerInventory inventory) {
                            syncIscsiServerDbFromAgent(inventory, msg.getHostUuid(), new NoErrorCompletion(completion) {
                                @Override
                                public void done() {
                                    reply.setIscsiServerInventory(inventory);
                                    completion.success(null);
                                }
                            });
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            reply.setError(errorCode);
                            completion.fail(errorCode);
                        }
                    });
                })
                .done(result -> {
                    bus.reply(msg, reply);
                })
        );
    }

    private void syncIscsiServerDbFromAgent(IscsiServerInventory inventory, String hostUuid, NoErrorCompletion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-iscsi-%s", inventory.getIp());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doSyncIscsiServerDbFromAgent(inventory, hostUuid);
                completion.done();
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doSyncIscsiServerDbFromAgent(IscsiServerInventory inventory, String hostUuid) {
        IscsiServerVO serverVO = Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.ip, inventory.getIp())
                .eq(IscsiServerVO_.port, inventory.getPort())
                .find();

        if (serverVO == null) {
            IscsiServerVO serverVO1 = new IscsiServerVO();
            serverVO1.setUuid(Platform.getUuid());
            serverVO1.setResourceName(String.format("iscsi-server-%s:%s", inventory.getIp(), inventory.getPort()));
            serverVO1.setChapUserName(inventory.getChapUserName());
            serverVO1.setChapUserPassword(inventory.getChapUserPassword());
            serverVO1.setState(StorageDeviceState.Enabled.toString());
            serverVO1.setCreateDate(new Timestamp(System.currentTimeMillis()));
            dbf.persistAndRefresh(serverVO1);

            if (inventory.getIscsiTargets() == null || inventory.getIscsiTargets().isEmpty()) {
                return;
            }

            for (IscsiTargetInventory i : inventory.getIscsiTargets()) {
                i.setIscsiServerUuid(serverVO1.getUuid());
                syncIscsiTargetDbFromAgent(i, hostUuid);
            }
            return;
        }
        if (StorageDeviceGlobalConfig.ALLOW_DIFFERENT_ISCSI_CONFIG.value(Boolean.class)) {
            SQL.New(ScsiLunHostRefVO.class).eq(ScsiLunHostRefVO_.hostUuid, hostUuid).hardDelete();
        }

        for (IscsiTargetInventory i : inventory.getIscsiTargets()) {
            i.setIscsiServerUuid(serverVO.getUuid());
            syncIscsiTargetDbFromAgent(i, hostUuid);
        }

        IscsiServerInventory serverInventory = IscsiServerInventory.valueOf(serverVO);
        List<String> existsIqns = inventory.getIscsiTargets().stream().map(IscsiTargetInventory::getIqn).collect(Collectors.toList());
        List<IscsiTargetInventory> needDelete = serverInventory.getIscsiTargets();

        if (StorageDeviceGlobalConfig.ALLOW_DIFFERENT_ISCSI_CONFIG.value(Boolean.class)) {
            needDelete = IscsiServerInventory.valueOf(dbf.findByUuid(serverVO.getUuid(), IscsiServerVO.class)).getIscsiTargets();
            needDelete.removeIf(target -> target.getIscsiLuns().stream().anyMatch(lun -> !lun.getScsiLunHostRefs().isEmpty()));
        } else {
            needDelete = needDelete.stream().filter(i -> !existsIqns.contains(i.getIqn())).collect(Collectors.toList());
        }

        removeIscsiTarget(needDelete, serverInventory);
    }

    private void removeIscsiTarget(List<IscsiTargetInventory> needDelete, IscsiServerInventory serverInventory) {
        for (IscsiTargetInventory t : needDelete) {
            logger.debug(String.format("remove iscsi target: %s-%s", serverInventory.getName(), t.getIqn()));
            removeIscsiLun(t.getIscsiLuns(), t);
            try {
                dbf.removeByPrimaryKey(t.getUuid(), IscsiTargetVO.class);
            } catch (ObjectOptimisticLockingFailureException exception) {
                logger.debug(String.format("remove lun %s failed because another transaction, not matter", t.getUuid()));
            }
        }
    }

    private void removeIscsiLun(List<IscsiLunInventory> needDelete, IscsiTargetInventory iscsiTargetInventory) {
        for (IscsiLunInventory l : needDelete) {
            logger.debug(String.format("remove iscsi lun: %s-%s", iscsiTargetInventory.getIqn(), l.getWwid()));
            List<String> refIds = Q.New(ScsiLunHostRefVO.class)
                    .select(ScsiLunHostRefVO_.id)
                    .eq(ScsiLunHostRefVO_.scsiLunUuid, l.getUuid()).listValues();
            dbf.removeByPrimaryKeys(refIds, ScsiLunHostRefVO.class);

            List<String> vmRefIds = Q.New(ScsiLunVmInstanceRefVO.class)
                    .select(ScsiLunVmInstanceRefVO_.id)
                    .eq(ScsiLunHostRefVO_.scsiLunUuid, l.getUuid()).listValues();
            dbf.removeByPrimaryKeys(vmRefIds, ScsiLunVmInstanceRefVO.class);
            dbf.removeByPrimaryKey(l.getUuid(), IscsiLunVO.class);
        }
    }

    private void cleanStaleLunHostRef(IscsiLunVO lunVO, String iscsiServerUuid) {
        List<HostVO> hosts = SQL.New("select distinct h from HostVO h, IscsiServerClusterRefVO r " +
                "where r.iscsiServerUuid = :serverUuid " +
                "and r.clusterUuid = h.clusterUuid", HostVO.class)
                .param("serverUuid", iscsiServerUuid)
                .list();

        List<ScsiLunHostRefVO> refVOS = Q.New(ScsiLunHostRefVO.class)
                .eq(ScsiLunHostRefVO_.scsiLunUuid, lunVO.getUuid())
                .list();

        if (hosts == null || hosts.isEmpty()) {
            if (refVOS == null || refVOS.isEmpty()) {
                return;
            }
            dbf.removeCollection(refVOS, ScsiLunHostRefVO.class);
            return;
        }

        for (ScsiLunHostRefVO refVO : refVOS) {
            if (hosts.stream().anyMatch(h -> h.getUuid().equals(refVO.getHostUuid()))) {
                continue;
            }
            logger.debug(String.format("remove scsiLunHostRef[lun: %s, host: %s]", refVO.getScsiLunUuid(), refVO.getHostUuid()));
            dbf.remove(refVO);
        }
    }

    private void syncIscsiTargetDbFromAgent(IscsiTargetInventory iscsiTargetInventory, String hostUuid) {
        IscsiTargetVO targetVO = Q.New(IscsiTargetVO.class)
                .eq(IscsiTargetVO_.iscsiServerUuid, iscsiTargetInventory.getIscsiServerUuid())
                .eq(IscsiTargetVO_.iqn, iscsiTargetInventory.getIqn())
                .find();

        if (targetVO == null) {
            targetVO = new IscsiTargetVO();
            targetVO.setResourceName(String.format("iscsi-target-%s", iscsiTargetInventory.getIqn()));
            targetVO.setUuid(Platform.getUuid());
            targetVO.setIscsiServerUuid(iscsiTargetInventory.getIscsiServerUuid());
            targetVO.setIqn(iscsiTargetInventory.getIqn());
            targetVO.setState(StorageDeviceState.Enabled.toString());
            targetVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
            dbf.persistAndRefresh(targetVO);

            if (iscsiTargetInventory.getIscsiLuns() == null || iscsiTargetInventory.getIscsiLuns().isEmpty()) {
                return;
            }

            for (IscsiLunInventory i : iscsiTargetInventory.getIscsiLuns()) {
                i.setIscsiTargetUuid(targetVO.getUuid());
                syncIscsiLunDbFromAgent(i, targetVO, hostUuid);
            }
            return;
        }

        for (IscsiLunInventory i : iscsiTargetInventory.getIscsiLuns()) {
            i.setIscsiTargetUuid(targetVO.getUuid());
            syncIscsiLunDbFromAgent(i, targetVO, hostUuid);
        }

        IscsiTargetInventory targetInventory = IscsiTargetInventory.valueOf(targetVO);
        Set<IscsiLunInventory> needDelete = new HashSet<>(targetInventory.getIscsiLuns());
        if (StorageDeviceGlobalConfig.ALLOW_DIFFERENT_ISCSI_CONFIG.value(Boolean.class)) {
            needDelete = new HashSet<>(IscsiTargetInventory.valueOf(dbf.findByUuid(targetVO.getUuid(), IscsiTargetVO.class)).getIscsiLuns());
            needDelete.removeIf(lun -> !lun.getScsiLunHostRefs().isEmpty());
        } else {
            needDelete.removeAll(new HashSet<>(iscsiTargetInventory.getIscsiLuns()));
        }

        removeIscsiLun(new ArrayList<>(needDelete), targetInventory);
    }

    private void syncIscsiLunDbFromAgent(IscsiLunInventory iscsiLunInventory, IscsiTargetVO iscsiTargetVO, String hostUuid) {
        IscsiLunVO lunVO = Q.New(IscsiLunVO.class)
                .eq(IscsiLunVO_.iscsiTargetUuid, iscsiLunInventory.getIscsiTargetUuid())
                .eq(IscsiLunVO_.serial, iscsiLunInventory.getSerial())
                .find();

        if (lunVO == null) {
            lunVO = new IscsiLunVO();
            lunVO.setUuid(Platform.getUuid());
            lunVO.setResourceName(String.format("iscsi-lun-%s", iscsiLunInventory.getPath()));
            lunVO.setIscsiTargetUuid(iscsiLunInventory.getIscsiTargetUuid());
            lunVO.setHctl(iscsiLunInventory.getHctl());
            lunVO.setVendor(iscsiLunInventory.getVendor());
            lunVO.setModel(iscsiLunInventory.getModel());
            lunVO.setPath(iscsiLunInventory.getPath());
//            lunVO.setMultipathDeviceUuid(iscsiLunInventory.getMultipathDeviceUuid());
            lunVO.setSerial(iscsiLunInventory.getSerial());
            lunVO.setWwn(iscsiLunInventory.getWwn());
            lunVO.setWwid(iscsiLunInventory.getWwid());
            lunVO.setType(iscsiLunInventory.getType());
            lunVO.setName(String.format("iscsi-lun-%s",iscsiLunInventory.getWwid()));
            lunVO.setSource(StorageDeviceConstants.ISCSI);
            lunVO.setState(StorageDeviceState.Enabled.toString());
            lunVO.setSize(iscsiLunInventory.getSize());
            lunVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
            dbf.persistAndRefresh(lunVO);
        } else if (!IscsiLunInventory.valueOf(lunVO).equals(iscsiLunInventory) || !lunVO.getWwid().equals(iscsiLunInventory.getWwid()) || !lunVO.getSize().equals(iscsiLunInventory.getSize()) || !lunVO.getType().equals(iscsiLunInventory.getType())) {
            lunVO.setWwid(iscsiLunInventory.getWwid());
            lunVO.setSize(iscsiLunInventory.getSize());
            lunVO.setType(iscsiLunInventory.getType());
//            lunVO.setMultipathDeviceUuid(iscsiLunInventory.getMultipathDeviceUuid());
            dbf.updateAndRefresh(lunVO);
        }

        if (hostUuid == null) {
            persistScsiLunHostRef(lunVO, iscsiTargetVO.getIscsiServerUuid());
        } else {
            logger.debug(String.format("sync host %s lun: %s", hostUuid, iscsiLunInventory.getWwid()));
            persistScsiLunHostRefIfNotExists(lunVO, hostUuid);
        }
    }

    private void persistScsiLunHostRefIfNotExists(IscsiLunVO lunVO, String hostUuid) {
        if (!Q.New(ScsiLunHostRefVO.class)
                .eq(ScsiLunHostRefVO_.hostUuid, hostUuid)
                .eq(ScsiLunHostRefVO_.scsiLunUuid, lunVO.getUuid()).isExists()) {
            logger.debug(String.format("create scsiLunHostRef[lun: %s, host: %s]", lunVO.getUuid(), hostUuid));
            ScsiLunHostRefVO refVO = new ScsiLunHostRefVO();
            refVO.setScsiLunUuid(lunVO.getUuid());
            refVO.setHostUuid(hostUuid);
            // for iscsi, path and hctl are the same on all hosts.
            refVO.setHctl(lunVO.getHctl());
            refVO.setPath(lunVO.getPath());
            refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
            dbf.persist(refVO);
        }
    }

    private void persistScsiLunHostRef(IscsiLunVO lunVO, String iscsiServerUuid) {
        List<HostVO> hosts = SQL.New("select distinct h from HostVO h, IscsiServerClusterRefVO r " +
                "where r.iscsiServerUuid = :serverUuid " +
                "and r.clusterUuid = h.clusterUuid", HostVO.class)
                .param("serverUuid", iscsiServerUuid)
                .list();

        for (HostVO hostVO : hosts) {
            persistScsiLunHostRefIfNotExists(lunVO, hostVO.getUuid());
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddIscsiServerMsg) {
            handle((APIAddIscsiServerMsg) msg);
        } else if (msg instanceof APIAttachIscsiServerToClusterMsg) {
            handle((APIAttachIscsiServerToClusterMsg) msg);
        } else if (msg instanceof APIDeleteIscsiServerMsg) {
            handle((APIDeleteIscsiServerMsg) msg);
        } else if (msg instanceof APIDetachIscsiServerFromClusterMsg) {
            handle((APIDetachIscsiServerFromClusterMsg) msg);
        } else if (msg instanceof APIRefreshIscsiServerMsg) {
            handle((APIRefreshIscsiServerMsg) msg);
        } else if (msg instanceof APIUpdateIscsiServerMsg) {
            handle((APIUpdateIscsiServerMsg) msg);
        } else if (msg instanceof APIUpdateScsiLunMsg) {
            handle((APIUpdateScsiLunMsg) msg);
        } else if (msg instanceof APIDetachScsiLunFromHostMsg) {
            handle((APIDetachScsiLunFromHostMsg) msg);
        } else if (msg instanceof APIRefreshFiberChannelStorageMsg) {
            handle((APIRefreshFiberChannelStorageMsg) msg);
        } else if (msg instanceof APIUpdateNvmeServerMsg) {
            handle((APIUpdateNvmeServerMsg) msg);
        } else if (msg instanceof APIRefreshNvmeServerMsg) {
            handle((APIRefreshNvmeServerMsg) msg);
        } else if (msg instanceof APIRefreshNvmeTargetMsg) {
            handle((APIRefreshNvmeTargetMsg) msg);
        } else if (msg instanceof APIAddNvmeServerMsg) {
            handle((APIAddNvmeServerMsg) msg);
        } else if (msg instanceof APIAttachNvmeServerToClusterMsg) {
            handle((APIAttachNvmeServerToClusterMsg) msg);
        } else if (msg instanceof APIDetachNvmeServerFromClusterMsg) {
            handle((APIDetachNvmeServerFromClusterMsg) msg);
        } else if(msg instanceof APIDeleteNvmeServerMsg) {
            handle((APIDeleteNvmeServerMsg) msg);
        } else if (msg instanceof APIAttachScsiLunToVmInstanceMsg) {
            handle((APIAttachScsiLunToVmInstanceMsg) msg);
        } else if (msg instanceof APIDetachScsiLunFromVmInstanceMsg) {
            handle((APIDetachScsiLunFromVmInstanceMsg) msg);
        } else if (msg instanceof APIGetScsiLunCandidatesForAttachingVmMsg) {
            handle((APIGetScsiLunCandidatesForAttachingVmMsg) msg);
        } else if (msg instanceof APICheckScsiLunClusterStatusMsg) {
            handle((APICheckScsiLunClusterStatusMsg) msg);
        } else if (msg instanceof APIRefreshLocalRaidMsg) {
            handle((APIRefreshLocalRaidMsg) msg);
        } else if (msg instanceof APILocateLocalRaidPhysicalDriveMsg) {
            handle((APILocateLocalRaidPhysicalDriveMsg) msg);
        } else if (msg instanceof APIGetLocalRaidPhysicalDriveSmartMsg) {
            handle((APIGetLocalRaidPhysicalDriveSmartMsg) msg);
        } else if (msg instanceof APISelfTestLocalRaidMsg) {
            handle((APISelfTestLocalRaidMsg) msg);
        } else if (msg instanceof APIGetHostMultipathTopologyMsg) {
            handle((APIGetHostMultipathTopologyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateNvmeServerMsg msg) {
        APIUpdateNvmeServerEvent event = new APIUpdateNvmeServerEvent(msg.getId());

        NvmeServerVO vo = Q.New(NvmeServerVO.class)
                .eq(NvmeServerVO_.uuid, msg.getUuid()).find();

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        vo = dbf.updateAndRefresh(vo);
        event.setInventory(NvmeServerInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(APIRefreshNvmeServerMsg msg) {
        APIRefreshNvmeServerEvent event = new APIRefreshNvmeServerEvent(msg.getId());

        NvmeServerVO vo = Q.New(NvmeServerVO.class)
                .eq(NvmeServerVO_.uuid, msg.getUuid())
                .find();

        List<String> attachedClusterUuids = Q.New(NvmeServerClusterRefVO.class)
                .select(NvmeServerClusterRefVO_.clusterUuid)
                .eq(NvmeServerClusterRefVO_.nvmeServerUuid, msg.getUuid())
                .listValues();

        if (CollectionUtils.isEmpty(attachedClusterUuids)) {
            for (NvmeLunVO nvmeLunVO : vo.getNvmeLuns()) {
                cleanStaleNvmeLunHostRef(nvmeLunVO, vo.getUuid());
            }
            vo = Q.New(NvmeServerVO.class)
                    .eq(NvmeServerVO_.uuid, msg.getUuid())
                    .find();

            event.setInventory(NvmeServerInventory.valueOf(vo));
            bus.publish(event);
            return;
        }

        List<String> attachedHostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, attachedClusterUuids)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();

        if (attachedHostUuids.isEmpty()) {
            event.setInventory(NvmeServerInventory.valueOf(vo));
            bus.publish(event);
            return;
        }

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("refresh nvme server %s", msg.getUuid()));
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "select one host refresh first";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ConnectNvmeServerMsg rmsg = new ConnectNvmeServerMsg();
                rmsg.setNvmeServerUuid(msg.getUuid());
                rmsg.setHostUuid(attachedHostUuids.get(0));
                bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, rmsg.getNvmeServerUuid());
                bus.send(rmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        ConnectNvmeServerReply r = reply.castReply();
                        if (!r.isSuccess()) {
                            trigger.fail(r.getError());
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh other hosts";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                attachedHostUuids.remove(attachedHostUuids.get(0));
                new While<>(attachedHostUuids).step((hostUuid, whileCompletion) -> {
                    ConnectNvmeServerMsg rmsg = new ConnectNvmeServerMsg();
                    rmsg.setNvmeServerUuid(msg.getUuid());
                    rmsg.setHostUuid(hostUuid);
                    bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, rmsg.getNvmeServerUuid());
                    bus.send(rmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                            }

                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            NvmeServerVO vo = Q.New(NvmeServerVO.class)
                                    .eq(NvmeServerVO_.uuid, msg.getUuid())
                                    .find();
                            event.setInventory(NvmeServerInventory.valueOf(vo));
                            trigger.next();
                        }
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
                bus.publish(event);
            }
        }).start();
    }

    private void cleanStaleNvmeLunHostRef(NvmeLunVO lunVO, String nvmeServerUuid) {
        List<HostVO> hosts = SQL.New("select distinct h from HostVO h, NvmeServerClusterRefVO r " +
                        "where r.nvmeServerUuid = :serverUuid " +
                        "and r.clusterUuid = h.clusterUuid", HostVO.class)
                .param("serverUuid", nvmeServerUuid)
                .list();

        List<NvmeLunHostRefVO> refVOS = Q.New(NvmeLunHostRefVO.class)
                .eq(NvmeLunHostRefVO_.nvmeLunUuid, lunVO.getUuid())
                .list();

        if (hosts == null || hosts.isEmpty()) {
            if (refVOS == null || refVOS.isEmpty()) {
                return;
            }
            dbf.removeCollection(refVOS, NvmeLunHostRefVO.class);
            return;
        }

        List<NvmeLunHostRefVO> toRemove = refVOS.stream()
                .filter(refVO -> hosts.stream().anyMatch(h -> h.getUuid().equals(refVO.getHostUuid())))
                .collect(Collectors.toList());
        dbf.removeCollection(toRemove, NvmeLunHostRefVO.class);
    }

    private void handle(APISelfTestLocalRaidMsg msg) {
        APISelfTestLocalRaidEvent event = new APISelfTestLocalRaidEvent(msg.getId());

        SelfTestLocalRaidMsg smsg = new SelfTestLocalRaidMsg();
        smsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, StorageDeviceConstants.SERVICE_ID, msg.getUuid());
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                } else {
                    SelfTestLocalRaidReply reply1 = reply.castReply();
                    event.setResult(reply1.getResult());
                }
                bus.publish(event);
            }
        });
    }

    private String getLunTarget(String lunUuid, String source) {
        switch (source) {
            case StorageDeviceConstants.FIBER_CHANNEL:
                return SQL.New("select wwnn from FiberChannelStorageVO where uuid = " +
                        "(select fiberChannelStorageUuid from FiberChannelLunVO where uuid = :lunUuid)", String.class)
                        .param("lunUuid", lunUuid)
                        .find();
            case StorageDeviceConstants.ISCSI:
                return SQL.New("select iqn from IscsiTargetVO where uuid = " +
                                "(select iscsiTargetUuid from IscsiLunVO where uuid = :lunUuid)", String.class)
                        .param("lunUuid", lunUuid)
                        .find();
            case StorageDeviceConstants.NVME:
                return SQL.New("select nqn from NvmeTargetVO where uuid = " +
                                "(select nvmeTargetUuid from NvmeLunVO where uuid = :lunUuid)", String.class)
                        .param("lunUuid", lunUuid)
                        .find();
            default:
                return null;
        }
    }

    private void handle(APIGetHostMultipathTopologyMsg msg) {
        APIGetHostMultipathTopologyReply reply = new APIGetHostMultipathTopologyReply();
        List<MultipathTopologyStruct> results = new ArrayList<>();

        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, msg.getHostUuid()).find();
        StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
        List<Tuple> tuples = Q.New(LunVO.class).select(LunVO_.wwid, LunVO_.uuid, LunVO_.source).in(LunVO_.uuid, msg.getLunUuids()).listTuple();
        Map<String, String> wwidUuidMap = new HashMap<>();
        Map<String, String> lunSourceMap = new HashMap<>();
        for (Tuple tuple : tuples) {
            String uuid = tuple.get(1, String.class);
            wwidUuidMap.put(tuple.get(0, String.class), uuid);
            lunSourceMap.put(uuid, tuple.get(2, String.class));
        }

        bkd.getHostMultipathTopology(HostInventory.valueOf(hostVO), new ArrayList<>(wwidUuidMap.keySet()), new ReturnValueCompletion<Map<String, List<DeviceTO>>>(msg) {
            @Override
            public void success(Map<String, List<DeviceTO>> devices) {
                devices.keySet().forEach(wwid -> {
                    String lunUuid = wwidUuidMap.get(wwid);
                    String target = getLunTarget(lunUuid, lunSourceMap.get(lunUuid));
                    devices.get(wwid).forEach(d -> {
                        d.setTarget(target);
                        d.setTargetType(lunSourceMap.get(lunUuid));
                    });
                    MultipathTopologyStruct s = new MultipathTopologyStruct();
                    s.setLunUuid(lunUuid);
                    s.setDevices(devices.get(wwid));
                    results.add(s);
                });
                reply.setResults(results);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetLocalRaidPhysicalDriveSmartMsg msg) {
        APIGetLocalRaidPhysicalDriveSmartReply reply = new APIGetLocalRaidPhysicalDriveSmartReply();

        RaidPhysicalDriveVO physicalDriveVO = Q.New(RaidPhysicalDriveVO.class).eq(RaidPhysicalDriveVO_.uuid, msg.getUuid()).find();
        RaidControllerVO controllerVO = Q.New(RaidControllerVO.class).eq(RaidControllerVO_.uuid, physicalDriveVO.getRaidControllerUuid()).find();
        HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, controllerVO.getHostUuid()).find();
        StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
        bkd.getPhysicalDriveSmartData(RaidPhysicalDriveInventory.valueOf(physicalDriveVO), RaidControllerInventory.valueOf(controllerVO), new ReturnValueCompletion<List<SmartDataStruct>>(msg) {
            @Override
            public void success(List<SmartDataStruct> returnValue) {
                reply.setResult(returnValue);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APILocateLocalRaidPhysicalDriveMsg msg) {
        APILocateLocalRaidPhysicalDriveEvent event = new APILocateLocalRaidPhysicalDriveEvent(msg.getId());

        RaidPhysicalDriveVO physicalDriveVO = Q.New(RaidPhysicalDriveVO.class).eq(RaidPhysicalDriveVO_.uuid, msg.getUuid()).find();
        RaidControllerVO controllerVO = Q.New(RaidControllerVO.class).eq(RaidControllerVO_.uuid, physicalDriveVO.getRaidControllerUuid()).find();

        LocateLocalRaidPhysicalDriveMsg lmsg = new LocateLocalRaidPhysicalDriveMsg();
        lmsg.setControllerInventory(RaidControllerInventory.valueOf(controllerVO));
        lmsg.setPhysicalDriveInventory(RaidPhysicalDriveInventory.valueOf(physicalDriveVO));
        lmsg.setLocate(msg.getLocate());
        bus.makeTargetServiceIdByResourceUuid(lmsg, StorageDeviceConstants.SERVICE_ID, controllerVO.getHostUuid());
        bus.send(lmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                }

                LocateLocalRaidPhysicalDriveReply rly = (LocateLocalRaidPhysicalDriveReply) reply;
                event.setInventory(rly.getInventory());
                bus.publish(event);
            }
        });
    }

    private void handle(APIRefreshLocalRaidMsg msg) {
        APIRefreshLocalRaidEvent event = new APIRefreshLocalRaidEvent(msg.getId());

        RefreshLocalRaidMsg rmsg = new RefreshLocalRaidMsg();
        rmsg.setHostUuid(msg.getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, rmsg.getHostUuid());
        bus.send(rmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                List<RaidControllerVO> controllerVOS = Q.New(RaidControllerVO.class).eq(RaidControllerVO_.hostUuid, msg.getHostUuid()).list();
                event.setInventories(RaidControllerInventory.valueOf(controllerVOS));
                bus.publish(event);
            }
        });
    }

    private void handle(APICheckScsiLunClusterStatusMsg msg) {
        APICheckScsiLunClusterStatusReply reply = new APICheckScsiLunClusterStatusReply();

        List<HostVO> hostVOS = Q.New(HostVO.class).eq(HostVO_.clusterUuid, msg.getClusterUuid()).list();
        if (hostVOS == null || hostVOS.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }
        List<String> refHostUuids = Q.New(ScsiLunHostRefVO.class)
                .select(ScsiLunHostRefVO_.hostUuid)
                .in(ScsiLunHostRefVO_.hostUuid, hostVOS.stream().map(HostVO::getUuid).collect(Collectors.toList()))
                .eq(ScsiLunHostRefVO_.scsiLunUuid, msg.getUuid())
                .listValues();
        if (refHostUuids == null || refHostUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        ScsiLunClusterStatusInventory inventory = new ScsiLunClusterStatusInventory();
        inventory.setAttachedHosts(new ArrayList<>());
        inventory.setUnattachedHosts(new ArrayList<>());

        for (HostVO hostVO : hostVOS) {
            if (refHostUuids.contains(hostVO.getUuid())) {
                inventory.getAttachedHosts().add(HostInventory.valueOf(hostVO));
            } else {
                inventory.getUnattachedHosts().add(HostInventory.valueOf(hostVO));
            }
        }
        inventory.setAllHostsAttached(inventory.getUnattachedHosts().isEmpty());

        reply.setInventory(inventory);
        bus.reply(msg, reply);
    }

    private void handle(APIGetScsiLunCandidatesForAttachingVmMsg msg) {
        APIGetScsiLunCandidatesForAttachingVmReply reply = new APIGetScsiLunCandidatesForAttachingVmReply();
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();

        if (vmInstanceVO.getHostUuid() != null) {
            List<ScsiLunVmInstanceRefVO> refVOS = Q.New(ScsiLunVmInstanceRefVO.class)
                    .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .list();
            List<ScsiLunVO> scsiLunVOS;
            if (refVOS != null && !refVOS.isEmpty()) {
                scsiLunVOS = SQL.New("select l from ScsiLunVO l, ScsiLunHostRefVO r " +
                        "where l.uuid = r.scsiLunUuid " +
                        "and l.state = :allowedState " +
                        "and l.wwid not in (select s.wwid from ScsiLunVO s where s.uuid in :attachedUuids) " +
                        "and r.hostUuid = :vmHostUuid", ScsiLunVO.class)
                        .param("attachedUuids", refVOS.stream().map(ScsiLunVmInstanceRefVO::getScsiLunUuid).collect(Collectors.toList()))
                        .param("allowedState", StorageDeviceState.Enabled.toString())
                        .param("vmHostUuid", vmInstanceVO.getHostUuid())
                        .list();
            } else {
                scsiLunVOS = SQL.New("select l from ScsiLunVO l, ScsiLunHostRefVO r " +
                        "where l.uuid = r.scsiLunUuid " +
                        "and l.state = :allowedState " +
                        "and r.hostUuid = :vmHostUuid", ScsiLunVO.class)
                        .param("allowedState", StorageDeviceState.Enabled.toString())
                        .param("vmHostUuid", vmInstanceVO.getHostUuid())
                        .list();
            }

            for (GetScsiLunCandidatesExtensionPoint ext : pluginRgty.getExtensionList(GetScsiLunCandidatesExtensionPoint.class)) {
                scsiLunVOS = ext.filterGetScsiLunCandidates(VmInstanceInventory.valueOf(vmInstanceVO), scsiLunVOS);
            }
            reply.setInventories(ScsiLunInventory.valueOf1(scsiLunVOS));
            bus.reply(msg, reply);
        } else {
            getScsiLunCandidatesForStoppedVm(vmInstanceVO.getUuid(), new ReturnValueCompletion<List<ScsiLunVO>>(msg) {
                @Override
                public void success(List<ScsiLunVO> returnValue) {
                    for (GetScsiLunCandidatesExtensionPoint ext : pluginRgty.getExtensionList(GetScsiLunCandidatesExtensionPoint.class)) {
                        returnValue = ext.filterGetScsiLunCandidates(VmInstanceInventory.valueOf(vmInstanceVO), returnValue);
                    }
                    reply.setInventories(ScsiLunInventory.valueOf1(returnValue));
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setSuccess(false);
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private void getScsiLunCandidatesForStoppedVm(String vmInstanceUuid, ReturnValueCompletion<List<ScsiLunVO>> completion) {
        List<ScsiLunVmInstanceRefVO> refVOS = Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, vmInstanceUuid)
                .list();
        logger.debug(String.format("there are scsi luns[%s] attached in vm[uuid: %s]", refVOS.stream().map(ScsiLunVmInstanceRefVO::getScsiLunUuid).collect(Collectors.toList()), vmInstanceUuid));
        if (refVOS.isEmpty()) {
            GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
            gmsg.setUuid(vmInstanceUuid);
            bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
            bus.send(gmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply re) {
                    if (!re.isSuccess()) {
                        completion.fail(re.getError());
                    } else {
                        GetVmStartingCandidateClustersHostsReply greply = (GetVmStartingCandidateClustersHostsReply) re;

                        if (greply.getHostInventories() == null || greply.getHostInventories().isEmpty()) {
                            completion.success(new ArrayList<>());
                            return;
                        }

                        List<ScsiLunVO> scsiLunVOS = SQL.New("select distinct l from ScsiLunVO l, ScsiLunHostRefVO r, HostVO h " +
                                "where l.uuid = r.scsiLunUuid " +
                                "and h.state = :allowedHostState " +
                                "and l.state = :allowedState " +
                                "and r.hostUuid in :hostUuids", ScsiLunVO.class)
                                .param("allowedHostState", HostState.Enabled)
                                .param("allowedState", StorageDeviceState.Enabled.toString())
                                .param("hostUuids", greply.getHostInventories().stream()
                                        .map(HostInventory::getUuid).collect(Collectors.toList()))
                                .list();

                        completion.success(scsiLunVOS);
                    }
                }
            });
            return;
        }

        List<String> hosts = Q.New(ScsiLunHostRefVO.class)
                .select(ScsiLunHostRefVO_.hostUuid)
                .eq(ScsiLunHostRefVO_.scsiLunUuid, refVOS.get(0).getScsiLunUuid())
                .listValues();
        if (hosts == null || hosts.isEmpty()) {
            logger.debug(String.format("can not find any host for scsi lun[uuid: %s]", refVOS.get(0).getScsiLunUuid()));
            completion.success(new ArrayList<>());
            return;
        }

        for (ScsiLunVmInstanceRefVO refVO : refVOS) {
            List<String> h = Q.New(ScsiLunHostRefVO.class)
                    .select(ScsiLunHostRefVO_.hostUuid)
                    .eq(ScsiLunHostRefVO_.scsiLunUuid, refVO.getScsiLunUuid())
                    .listValues();
            hosts.retainAll(h);
            if (hosts.isEmpty()) {
                logger.debug(String.format("can not find any common host for scsi lun between [uuid: %s] and [uuid: %s]",
                        refVOS.get(0).getScsiLunUuid(), refVO.getScsiLunUuid()));
                completion.success(new ArrayList<>());
                return;
            }
        }
        logger.debug(String.format("find common hosts[%s] for scsi lun on vm[uuid: %s]", hosts, vmInstanceUuid));

        List<ScsiLunVO> scsiLunVOS = SQL.New("select distinct l from ScsiLunVO l, ScsiLunHostRefVO r, HostVO h " +
                "where l.uuid = r.scsiLunUuid " +
                "and h.state = :allowedHostState " +
                "and l.state = :allowedState " +
                "and l.wwid not in (select s.wwid from ScsiLunVO s where s.uuid in :attachedUuids) " +
                "and r.hostUuid in :hostUuids", ScsiLunVO.class)
                .param("attachedUuids", refVOS.stream().map(ScsiLunVmInstanceRefVO::getScsiLunUuid).collect(Collectors.toList()))
                .param("allowedHostState", HostState.Enabled)
                .param("allowedState", StorageDeviceState.Enabled.toString())
                .param("hostUuids", hosts)
                .list();
        completion.success(scsiLunVOS);
    }

    private void handle(APIAttachScsiLunToVmInstanceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("scsi-lun-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                dohandleInVmQueue(msg, new ReturnValueCompletion<APIAttachScsiLunToVmInstanceEvent>(chain) {
                    @Override
                    public void success(APIAttachScsiLunToVmInstanceEvent returnValue) {
                        bus.publish(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        APIAttachScsiLunToVmInstanceEvent event = new APIAttachScsiLunToVmInstanceEvent(msg.getId());
                        event.setSuccess(false);
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void dohandleInVmQueue(APIAttachScsiLunToVmInstanceMsg msg, ReturnValueCompletion<APIAttachScsiLunToVmInstanceEvent> completion) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("scsi-lun-to-vm-%s", msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                dohandle(msg, new ReturnValueCompletion<APIAttachScsiLunToVmInstanceEvent>(chain) {
                    @Override
                    public void success(APIAttachScsiLunToVmInstanceEvent returnValue) {
                        completion.success(returnValue);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private boolean hasAttachSameWwidScsiLun(String vmUuid, ScsiLunVO lunVO) {
        if (lunVO == null) {
            return false;
        }
        String sql = "select s from ScsiLunVO s, ScsiLunVmInstanceRefVO ref " +
                "where ref.vmInstanceUuid = :vmUuid " +
                "and ref.scsiLunUuid = s.uuid " +
                "and s.wwid = :wwid";
        TypedQuery<ScsiLunVO> q = dbf.getEntityManager().createQuery(sql, ScsiLunVO.class);
        q.setParameter("vmUuid", vmUuid);
        q.setParameter("wwid", lunVO.getWwid());
        return !q.getResultList().isEmpty();
    }

    private void dohandle(APIAttachScsiLunToVmInstanceMsg msg, ReturnValueCompletion<APIAttachScsiLunToVmInstanceEvent> completion) {
        APIAttachScsiLunToVmInstanceEvent event = new APIAttachScsiLunToVmInstanceEvent(msg.getId());
        ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();

        if (Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, msg.getUuid())
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, msg.getVmInstanceUuid()).isExists()) {
            event.setInventory(ScsiLunInventory.valueOf(lunVO));
            completion.success(event);
            return;
        }

        if(hasAttachSameWwidScsiLun(msg.getVmInstanceUuid(), lunVO)) {
            completion.fail(operr("scsi lun[wwid:%s] has been attached into the vm[%s]", lunVO.getWwid(), msg.getVmInstanceUuid()));
            return;
        }

        //TODO(weiw): move these to interceptor
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!allowedVmOperationStates.contains(vmInstanceVO.getState())) {
            completion.fail(argerr(
                    "vm instance[%s] state [%s] not in allowed state[%s] for operation", msg.getUuid(), vmInstanceVO.getState(), allowedVmOperationStates));
            return;
        }

        if (vmInstanceVO.getHostUuid() != null) {
            if (!Q.New(ScsiLunHostRefVO.class)
                    .eq(ScsiLunHostRefVO_.scsiLunUuid, msg.getUuid())
                    .eq(ScsiLunHostRefVO_.hostUuid, vmInstanceVO.getHostUuid()).isExists()) {
                completion.fail(argerr(
                        "vm instance[uuid: %s] host[uuid: %s] not attached scsi lun[uuid: %s]", msg.getUuid(), vmInstanceVO.getHostUuid(), msg.getUuid()));
                return;
            }
        }
        ClusterVO clusterVO = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, vmInstanceVO.getClusterUuid()).find();

        ScsiLunVmInstanceRefVO refVO = new ScsiLunVmInstanceRefVO();
        refVO.setScsiLunUuid(msg.getUuid());
        refVO.setVmInstanceUuid(msg.getVmInstanceUuid());
        refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        refVO.setDeviceId(new NextVolumeDeviceIdGetter().getNextVolumeDeviceId(msg.getVmInstanceUuid()));
        refVO.setAttachMultipath(!msg.isDisableMultiPathAttach());
        dbf.persist(refVO);

        if (VmInstanceState.Stopped.equals(vmInstanceVO.getState())) {
            lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
            event.setInventory(ScsiLunInventory.valueOf(lunVO));
            completion.success(event);
            return;
        }

        StorageDeviceBackend bkd = backends.get(clusterVO.getHypervisorType());
        bkd.attachScsiLunToVm(lunVO, vmInstanceVO, refVO, new Completion(msg) {
            @Override
            public void success() {
                ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
                event.setInventory(ScsiLunInventory.valueOf(lunVO));
                event.setSuccess(true);
                completion.success(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(refVO);
                ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
                event.setInventory(ScsiLunInventory.valueOf(lunVO));
                event.setError(errorCode);
                event.setSuccess(false);
                completion.success(event);
            }
        });
    }

    private void handle(APIDetachScsiLunFromVmInstanceMsg msg) {
        APIDetachScsiLunFromVmInstanceEvent event = new APIDetachScsiLunFromVmInstanceEvent(msg.getId());
        ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();

        if (!Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, msg.getUuid())
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, msg.getVmInstanceUuid()).isExists()) {
            event.setInventory(ScsiLunInventory.valueOf(lunVO));
            bus.publish(event);
            return;
        }

        //TODO(weiw): move these to interceptor
        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!allowedVmOperationStates.contains(vmInstanceVO.getState())) {
            throw new ApiMessageInterceptionException(argerr(
                    "vm instance[%s] state[%s] not in allowed state[%s] for operation", msg.getVmInstanceUuid(), vmInstanceVO.getState(), allowedVmOperationStates));
        }

        if (vmInstanceVO.getHostUuid() != null) {
            HostVO hostVO = Q.New(HostVO.class).eq(HostVO_.uuid, vmInstanceVO.getHostUuid()).find();
            if (!Q.New(ScsiLunHostRefVO.class)
                    .eq(ScsiLunHostRefVO_.scsiLunUuid, msg.getUuid())
                    .eq(ScsiLunHostRefVO_.hostUuid, vmInstanceVO.getHostUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr(
                        "vm instance[%s] host[uuid: %s] not attached scsi lun[uuid: %s]", msg.getVmInstanceUuid(), hostVO.getUuid(), msg.getUuid()));
            }
        }

        ScsiLunVmInstanceRefVO refVO = Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, msg.getUuid())
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .find();

        if (VmInstanceState.Stopped.equals(vmInstanceVO.getState())) {
            dbf.remove(refVO);

            lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
            event.setInventory(ScsiLunInventory.valueOf(lunVO));
            bus.publish(event);
            return;
        }

        ClusterVO clusterVO = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, vmInstanceVO.getClusterUuid()).find();
        StorageDeviceBackend bkd = backends.get(clusterVO.getHypervisorType());
        bkd.detachScsiLunFromVm(lunVO, vmInstanceVO, refVO, new Completion(msg) {
            @Override
            public void success() {
                dbf.remove(refVO);
                ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
                event.setInventory(ScsiLunInventory.valueOf(lunVO));
                event.setSuccess(true);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
                event.setInventory(ScsiLunInventory.valueOf(lunVO));
                event.setSuccess(false);
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private List<String> getHostFCLunUuids(String hostUuid) {
        final String sql = "select r.scsiLunUuid from ScsiLunHostRefVO r " +
                "where r.hostUuid = :huuid " +
                "and r.scsiLunUuid in (select l.uuid from FiberChannelLunVO l)";
        return SQL.New(sql, String.class)
                .param("huuid", hostUuid)
                .list();
    }

    private void handle(APIRefreshFiberChannelStorageMsg msg) {
        APIRefreshFiberChannelStorageEvent event = new APIRefreshFiberChannelStorageEvent(msg.getId());

        List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.zoneUuid, msg.getZoneUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        if (hosts == null || hosts.isEmpty()) {
            bus.publish(event);
            return;
        }

        new While<>(hosts).step((hostVO, whileCompletion) -> {
            RefreshFiberChannelMsg rmsg = new RefreshFiberChannelMsg();
            rmsg.setRescan(true);
            rmsg.setHostUuid(hostVO.getUuid());
            if (msg.getScsiLunUuids() == null) {
                rmsg.setScsiLunUuids(getHostFCLunUuids(hostVO.getUuid()));
            } else {
                rmsg.setScsiLunUuids(msg.getScsiLunUuids());
            }
            bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, msg.getZoneUuid());
            bus.send(rmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    whileCompletion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                List<FiberChannelStorageVO> vos = SQL.New("select distinct s " +
                        "from FiberChannelStorageVO s, FiberChannelLunVO l, ScsiLunHostRefVO r, HostVO h " +
                        "where l.fiberChannelStorageUuid = s.uuid " +
                        "and r.scsiLunUuid = l.uuid " +
                        "and r.hostUuid = h.uuid " +
                        "and h.zoneUuid = :msgZoneUuid", FiberChannelStorageVO.class)
                        .param("msgZoneUuid", msg.getZoneUuid())
                        .list();
                event.setInventories(FiberChannelStorageInventory.valueOf1(vos));
                bus.publish(event);
            }
        });
    }

    private void handle(APIAddNvmeServerMsg msg) {
        APIAddNvmeServerEvent event = new APIAddNvmeServerEvent(msg.getId());

        NvmeServerVO nvmeServerVO = new NvmeServerVO();
        nvmeServerVO.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        nvmeServerVO.setIp(msg.getIp());
        nvmeServerVO.setName(msg.getName());
        nvmeServerVO.setPort(msg.getPort());
        nvmeServerVO.setTransport(msg.getTransport());
        nvmeServerVO.setState(StorageDeviceState.Enabled.toString());
        nvmeServerVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

        if (nvmeServerVO.getName() == null) {
            nvmeServerVO.setName(String.format("nvme-server-%s:%s",
                    msg.getIp(), msg.getPort()));
        }

        dbf.persistAndRefresh(nvmeServerVO);
        event.setInventory(NvmeServerInventory.valueOf(nvmeServerVO));
        bus.publish(event);
    }

    private void handle(APIAttachNvmeServerToClusterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIAttachNvmeServerToClusterEvent event = new APIAttachNvmeServerToClusterEvent(msg.getId());
                doAttachNvmeServerToCluster(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        event.setInventory(NvmeServerInventory.valueOf(dbf.findByUuid(msg.getUuid(), NvmeServerVO.class)));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("attach-nvme-server-%s-into-or-detach-from-a-cluster", msg.getUuid());
            }

            @Override
            public String getName() {
                return String.format("attach-nvme-server-%s-into-cluster", msg.getUuid());
            }
        });
    }

    protected void doAttachNvmeServerToCluster(APIAttachNvmeServerToClusterMsg msg, Completion completion) {
        List<String> attachedClusterUuids = Q.New(NvmeServerClusterRefVO.class)
                .select(NvmeServerClusterRefVO_.clusterUuid)
                .eq(NvmeServerClusterRefVO_.nvmeServerUuid, msg.getUuid())
                .listValues();

        List<String> attachedHostUuids = (attachedClusterUuids == null || attachedClusterUuids.isEmpty()) ? new ArrayList<>() : Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, attachedClusterUuids)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();

        NvmeServerClusterRefVO refVO = new NvmeServerClusterRefVO();
        refVO.setNvmeServerUuid(msg.getUuid());
        refVO.setClusterUuid(msg.getClusterUuid());
        refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

        List<HostVO> hostVOS = Q.New(HostVO.class)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .eq(HostVO_.status, HostStatus.Connected).list();

        if (hostVOS.isEmpty()) {
            dbf.persistAndRefresh(refVO);
            completion.success();
            return;
        }

        AttachNvmeServerContext context = new AttachNvmeServerContext();
        context.hostVOS = hostVOS;
        context.attachedHostUuids = attachedHostUuids;
        context.msg = msg;
        context.refVO = refVO;

        attachNvmeServerToClusterAndRetry(
                context,
                StorageDeviceGlobalConfig.ATTACH_TO_CLUSTER_RETRY_TIMES.value(Integer.class),
                completion);
    }

    static class AttachNvmeServerContext {
        // parameters
        List<HostVO> hostVOS;
        List<String> attachedHostUuids;
        APIAttachNvmeServerToClusterMsg msg;
        NvmeServerClusterRefVO refVO;

        // locks
        final Object lock = new Object();

        // results
        /**
         * The host first successfully connected NVMe server
         */
        NvmeServerInventory nvmeServerChecker;
        String checkerHostUuid;
        final HashSet<String> hostsConnected = new HashSet<>();
    }

    /**
     * Note: retry only applies to flow "connect-nvme-server"
     */
    @SuppressWarnings("rawtypes")
    private void attachNvmeServerToClusterAndRetry(AttachNvmeServerContext context, int retryTimes, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-nvme-server-to-cluster-%s", context.msg.getClusterUuid()));
        chain.then(new Flow() {
            String __name__ = "connect-nvme-server-and-retry-" + retryTimes + "-times";
            AtomicInteger remainTimes = new AtomicInteger(retryTimes);

            @Override
            public void run(FlowTrigger trigger, Map data) {
                nextRetry(trigger);
            }

            public void nextRetry(FlowTrigger trigger) {
                attachNvmeServerToCluster(context, createCompletion(trigger));
            }

            private Completion createCompletion(FlowTrigger trigger) {
                return new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (remainTimes.decrementAndGet() > 0) {
                            logger.debug(String.format("failed to connect nvme server[%s], retry (remain %d times)",
                                    errorCode, remainTimes.get()));
                            nextRetry(trigger);
                            return;
                        }

                        trigger.fail(errorCode);
                    }
                };
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                cleanNvmeServer(context, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.debug(String.format("detach nvme server failed but still continue: %s", errorCode));
                        trigger.rollback();
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = "persist-nvme-server-cluster-refs";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                dbf.persistAndRefresh(context.refVO);
                trigger.next();
            }

            public void rollback(FlowRollback trigger, Map data) {
                SQL.New(NvmeServerClusterRefVO.class)
                        .eq(NvmeServerClusterRefVO_.nvmeServerUuid, context.refVO.getNvmeServerUuid())
                        .eq(NvmeServerClusterRefVO_.clusterUuid, context.refVO.getClusterUuid())
                        .delete();
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh-nvme-on-host-attached-nvme-server";

            @Override
            public boolean skip(Map data) {
                return context.hostsConnected.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String zoneUuid = Q.New(HostVO.class).eq(HostVO_.uuid, context.hostVOS.get(0).getUuid())
                        .select(HostVO_.zoneUuid)
                        .findValue();
                new While<>(context.hostVOS).each((hostVO, whileCompletion) -> {
                    RefreshNvmeMsg cmsg = new RefreshNvmeMsg();
                    cmsg.setHostUuid(hostVO.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, SERVICE_ID, zoneUuid);
                    bus.send(cmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                whileCompletion.addError(reply.getError());
                                whileCompletion.allDone();
                            } else {
                                whileCompletion.done();
                            }
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                            return;
                        }
                        updateTargetServerUuid();
                        trigger.next();
                    }
                });
            }

            private void updateTargetServerUuid() {
                NvmeServerInventory foundNvmeServer = context.nvmeServerChecker;
                List<String> nqns = CollectionUtils.transform(foundNvmeServer.getNvmeTargets(), NvmeTargetInventory::getNqn);
                if (nqns.isEmpty()) {
                    return;
                }

                SQL.New(NvmeTargetVO.class).in(NvmeTargetVO_.nqn, nqns)
                        .set(NvmeTargetVO_.nvmeServerUuid, context.msg.getUuid())
                        .update();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh-nvme-on-other-host-related-nvme-server";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.attachedHostUuids.isEmpty()) {
                    trigger.next();
                    return;
                }
                String zoneUuid = Q.New(HostVO.class).eq(HostVO_.uuid, context.attachedHostUuids.get(0))
                        .select(HostVO_.zoneUuid)
                        .findValue();
                for (String hostUuid : context.attachedHostUuids) {
                    RefreshNvmeMsg cmsg = new RefreshNvmeMsg();
                    logger.debug(String.format("refresh nvme server[uuid: %s] on host %s", context.msg.getUuid(), hostUuid));
                    cmsg.setHostUuid(hostUuid);
                    bus.makeTargetServiceIdByResourceUuid(cmsg, SERVICE_ID, zoneUuid);
                    bus.send(cmsg);
                }
                trigger.next();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private void attachNvmeServerToCluster(AttachNvmeServerContext context, Completion completion) {
        AtomicBoolean skipNextConnectRequest = new AtomicBoolean(false);
        NvmeServerVO nvmeServerVO = Q.New(NvmeServerVO.class)
                .eq(NvmeServerVO_.uuid, context.msg.getUuid())
                .find();

        new While<>(context.hostVOS).step((hostVO, whileCompletion) -> {
            if (context.hostsConnected.contains(hostVO.getUuid()) || skipNextConnectRequest.get()) {
                whileCompletion.done();
                return;
            }

            StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
            bkd.connectNvmeServer(nvmeServerVO, HostInventory.valueOf(hostVO), new ReturnValueCompletion<NvmeServerInventory>(whileCompletion) {
                @Override
                public void success(NvmeServerInventory serverInventory) {
                    onConnectSuccess(serverInventory, hostVO, whileCompletion);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    whileCompletion.addError(errorCode);
                    // DO NOT use whileCompletion.allDone(): we should wait for all tasks to be completed
                    whileCompletion.done();
                    skipNextConnectRequest.set(true);
                }

                void onConnectSuccess(NvmeServerInventory serverInventory, HostVO hostVO, WhileCompletion whileCompletion) {
                    synchronized (context.lock) {
                        context.hostsConnected.add(hostVO.getUuid());
                        if (context.nvmeServerChecker == null) {
                            context.nvmeServerChecker = serverInventory;
                            context.checkerHostUuid = hostVO.getUuid();
                        }
                    }

                    if (context.nvmeServerChecker == serverInventory) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(String.format("first nvme server found in host %s: %s",
                                    hostVO.getUuid(), JSONObjectUtil.toJsonString(serverInventory)));
                        }
                        whileCompletion.done();
                        return;
                    }

                    if (!new HashSet<>(context.nvmeServerChecker.getNvmeTargets()).equals(new HashSet<>(serverInventory.getNvmeTargets()))) {
                        whileCompletion.addError(operr(
                                "different nvme targets were found on host[%s] and host[%s]",
                                hostVO.getUuid(),
                                context.checkerHostUuid)
                                        .withOpaque("host1.uuid", hostVO.getUuid())
                                        .withOpaque("host2.uuid", context.checkerHostUuid)
                                        .withOpaque("host1.nvmeTargets", serverInventory.getNvmeTargets())
                                        .withOpaque("host2.nvmeTargets", context.nvmeServerChecker.getNvmeTargets())
                        );
                        // DO NOT use whileCompletion.allDone(): we should wait for all tasks to be completed
                        whileCompletion.done();
                        skipNextConnectRequest.set(true);
                        return;
                    }
                    whileCompletion.done();
                }
            });
        }, 15).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                    return;
                }

                completion.fail(errorCodeList.getCauses().get(0));
            }
        });
    }

    private void cleanNvmeServer(AttachNvmeServerContext context, Completion completion) {
        if (context.nvmeServerChecker == null) {
            completion.success();
            return;
        }

        NvmeServerInventory serverInventory = context.nvmeServerChecker;
        NvmeServerVO server = new NvmeServerVO();
        server.setUuid(context.msg.getUuid());
        server.setIp(serverInventory.getIp());
        server.setPort(serverInventory.getPort());
        server.setTransport(serverInventory.getTransport());

        Map<String, HostVO> uuidHostMap = CollectionUtils.toMap(context.hostVOS, HostVO::getUuid, Function.identity());
        List<HostVO> connectedHosts = CollectionUtils.transform(context.hostsConnected, uuidHostMap::get);
        detachNvmeServerFromCluster(server, connectedHosts, new Completion(completion) {
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

    private void handle(APIDetachNvmeServerFromClusterMsg msg) {
        APIDetachNvmeServerFromClusterEvent event = new APIDetachNvmeServerFromClusterEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                NvmeServerClusterRefVO refVO = Q.New(NvmeServerClusterRefVO.class).eq(NvmeServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                        .eq(NvmeServerClusterRefVO_.nvmeServerUuid, msg.getUuid())
                        .find();
                if (refVO == null) {
                    bus.publish(event);
                    chain.next();
                    return;
                }
                detachNvmeServerFromCluster(msg.getUuid(), msg.getClusterUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        dbf.remove(refVO);
                        NvmeServerVO serverVO = Q.New(NvmeServerVO.class).eq(NvmeServerVO_.uuid, msg.getUuid()).find();
                        event.setInventory(NvmeServerInventory.valueOf(serverVO));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("attach-nvme-server-%s-into-or-detach-from-a-cluster", msg.getUuid());
            }

            @Override
            public String getName() {
                return String.format("detach-nvme-server-%s-from-cluster", msg.getUuid());
            }
        });
    }

    private void detachNvmeServerFromCluster(String nvmeServerUuid, String clusterUuid, Completion completion) {
        List<HostVO> attachedHosts = Q.New(HostVO.class)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .list();

        NvmeServerVO nvmeServerVO = Q.New(NvmeServerVO.class)
                .eq(NvmeServerVO_.uuid, nvmeServerUuid)
                .find();

        detachNvmeServerFromCluster(nvmeServerVO, attachedHosts, completion);
    }

    @SuppressWarnings("rawtypes")
    private void detachNvmeServerFromCluster(NvmeServerVO nvmeServerVO, List<HostVO> attachedHosts, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setChainName(String.format("detach-nvme-server-%s-from-cluster", nvmeServerVO.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "disconnect-nvme-server";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(attachedHosts).step((hostVO, whileCompletion) -> {
                    StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
                    bkd.disconnectNvmeServer(nvmeServerVO, HostInventory.valueOf(hostVO), new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.done();
                        }
                    });
                }, 15).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh nvme devices";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (HostVO host : attachedHosts) {
                    RefreshNvmeMsg refreshNvmeMsg = new RefreshNvmeMsg();
                    refreshNvmeMsg.setHostUuid(host.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(refreshNvmeMsg, SERVICE_ID, host.getZoneUuid());
                    bus.send(refreshNvmeMsg);
                }
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(APIDeleteNvmeServerMsg msg) {
        APIDeleteNvmeServerEvent event = new APIDeleteNvmeServerEvent(msg.getId());
        SQL.New(NvmeServerVO.class)
                .eq(NvmeServerVO_.uuid, msg.getUuid())
                .delete();
        bus.publish(event);
    }

    private void handle(APIRefreshNvmeTargetMsg msg) {
        APIRefreshNvmeTargetEvent event = new APIRefreshNvmeTargetEvent(msg.getId());

        List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.zoneUuid, msg.getZoneUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .list();
        if (hosts.isEmpty()) {
            bus.publish(event);
            return;
        }

        new While<>(hosts).step((hostVO, whileCompletion) -> {
            RefreshNvmeMsg rmsg = new RefreshNvmeMsg();
            rmsg.setRescan(true);
            rmsg.setHostUuid(hostVO.getUuid());
            if (msg.getNvmeLunUuids() == null) {
                List<String> nvmeUuids = Q.New(NvmeLunHostRefVO.class)
                        .eq(NvmeLunHostRefVO_.hostUuid, hostVO.getUuid())
                        .select(NvmeLunHostRefVO_.nvmeLunUuid)
                        .listValues();
                rmsg.setNvmeLunUuids(nvmeUuids);
            } else {
                rmsg.setNvmeLunUuids(msg.getNvmeLunUuids());
            }
            bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, msg.getZoneUuid());
            bus.send(rmsg, new CloudBusCallBack(whileCompletion) {
                @Override
                public void run(MessageReply reply) {
                    whileCompletion.done();
                }
            });
        }, 5).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                List<NvmeTargetVO> vos = SQL.New("select distinct s " +
                        "from NvmeTargetVO s, NvmeLunVO l, NvmeLunHostRefVO r, HostVO h " +
                        "where l.nvmeTargetUuid = s.uuid " +
                        "and r.nvmeLunUuid = l.uuid " +
                        "and r.hostUuid = h.uuid " +
                        "and h.zoneUuid = :msgZoneUuid", NvmeTargetVO.class)
                        .param("msgZoneUuid", msg.getZoneUuid())
                        .list();
                event.setInventories(NvmeTargetInventory.valueOf(vos));
                bus.publish(event);
            }
        });
    }

    //the LUN should not have been attached to any VM
    private void handleDetach(APIDetachScsiLunFromHostMsg msg, NoErrorCompletion completion) {
        APIDetachScsiLunFromHostEvent event = new APIDetachScsiLunFromHostEvent(msg.getId());
        final List<String> vmUuids = Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, msg.getUuid())
                .select(ScsiLunVmInstanceRefVO_.vmInstanceUuid)
                .listValues();
        if (!vmUuids.isEmpty()) {
            event.setError(operr("SCSI LUN[%s] is attached to VM [%s]", msg.getUuid(), vmUuids));
            bus.publish(event);
            completion.done();
            return;
        }

        if (!Q.New(ScsiLunHostRefVO.class)
                .eq(ScsiLunHostRefVO_.scsiLunUuid, msg.getUuid())
                .eq(ScsiLunHostRefVO_.hostUuid, msg.getHostUuid())
                .isExists()) {
            event.setError(operr("SCSI LUN[%s] record not found on host [%s]", msg.getUuid(), msg.getHostUuid()));
            bus.publish(event);
            completion.done();
            return;
        }

        String hvType = Q.New(HostVO.class)
                .eq(HostVO_.uuid, msg.getHostUuid())
                .select(HostVO_.hypervisorType)
                .findValue();
        StorageDeviceBackend bkd = backends.get(hvType);
        if (bkd == null) {
            event.setError(operr("unexpected hypervisor type[%s] for host [%s]", hvType, msg.getHostUuid()));
            bus.publish(event);
            completion.done();
            return;
        }

        bkd.detachScsiLunFromHost(dbf.findByUuid(msg.getUuid(), ScsiLunVO.class), msg.getHostUuid(),
                new Completion(completion) {
                    @Override
                    public void success() {
                        UpdateQuery.New(ScsiLunHostRefVO.class)
                                .eq(ScsiLunHostRefVO_.scsiLunUuid, msg.getUuid())
                                .eq(ScsiLunHostRefVO_.hostUuid, msg.getHostUuid())
                                .delete();
                        event.setInventory(ScsiLunInventory.valueOf(dbf.findByUuid(msg.getUuid(), ScsiLunVO.class)));
                        bus.publish(event);
                        completion.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        completion.done();
                    }
                }
        );
    }

    private void handle(APIDetachScsiLunFromHostMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("scsi-lun-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleDetach(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });

    }

    private void handleUpdate(APIUpdateScsiLunMsg msg) {
        boolean changed = false;
        ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, msg.getUuid()).find();
        APIUpdateScsiLunEvent event = new APIUpdateScsiLunEvent(msg.getId());

        if (msg.getName() != null) {
            lunVO.setName(msg.getName());
            changed = true;
        }

        if (msg.getState() != null) {
            lunVO.setState(StorageDeviceState.valueOf(msg.getState()).toString());
            changed = true;
        }

        if (changed) {
            dbf.update(lunVO);
        }

        event.setInventory(ScsiLunInventory.valueOf(lunVO));
        bus.publish(event);
    }

    private void handle(APIUpdateScsiLunMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("scsi-lun-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                handleUpdate(msg);
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APIAddIscsiServerMsg msg) {
        APIAddIscsiServerEvent event = new APIAddIscsiServerEvent(msg.getId());

        IscsiServerVO iscsiServerVO = new IscsiServerVO();
        iscsiServerVO.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        iscsiServerVO.setIp(msg.getIp());
        iscsiServerVO.setName(msg.getName());
        iscsiServerVO.setPort(msg.getPort());
        iscsiServerVO.setChapUserName(msg.getChapUserName());
        iscsiServerVO.setChapUserPassword(msg.getChapUserPassword());
        iscsiServerVO.setState(StorageDeviceState.Enabled.toString());
        iscsiServerVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

        if (iscsiServerVO.getName() == null) {
            iscsiServerVO.setName(String.format("iscsi-server-%s:%s",
                    msg.getIp(), msg.getPort()));
        }

        dbf.persistAndRefresh(iscsiServerVO);
        event.setInventory(IscsiServerInventory.valueOf(iscsiServerVO));
        bus.publish(event);
    }

    protected void handle(APIAttachIscsiServerToClusterMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                APIAttachIscsiServerToClusterEvent event = new APIAttachIscsiServerToClusterEvent(msg.getId());
                doAttachIscsiServerToCluster(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        event.setInventory(IscsiServerInventory.valueOf(dbf.findByUuid(msg.getUuid(), IscsiServerVO.class)));
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("attach-and-detach-iscsi-server-%s-from-cluster", msg.getUuid());
            }

            @Override
            public String getName() {
                return String.format("attach-iscsi-server-%s-from-cluster", msg.getUuid());
            }
        });
    }

    @Transactional
    protected void doAttachIscsiServerToCluster(APIAttachIscsiServerToClusterMsg msg, Completion completion) {
        List<String> attachedClusterUuids = Q.New(IscsiServerClusterRefVO.class)
                .select(IscsiServerClusterRefVO_.clusterUuid)
                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                .listValues();

        List<String> attachedHostUuids = (attachedClusterUuids == null || attachedClusterUuids.isEmpty()) ? new ArrayList<>() : Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, attachedClusterUuids)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();

        IscsiServerClusterRefVO refVO = new IscsiServerClusterRefVO();
        refVO.setIscsiServerUuid(msg.getUuid());
        refVO.setClusterUuid(msg.getClusterUuid());
        refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));

        List<HostVO> hostVOS = Q.New(HostVO.class)
                .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                .eq(HostVO_.status, HostStatus.Connected).list();

        if (hostVOS.isEmpty()) {
            dbf.persistAndRefresh(refVO);
        }
        if (hostVOS.isEmpty()) {
            completion.success();
            return;
        }

        AtomicInteger retryTimes = new AtomicInteger(StorageDeviceGlobalConfig.ATTACH_TO_CLUSTER_RETRY_TIMES.value(Integer.class));

        attachIscsiServerToCluster(hostVOS, attachedHostUuids, msg, refVO, retryTimes, completion);
    }

    private void attachIscsiServerToCluster(List<HostVO> hostVOS, List<String> attachedHostUuids,
                                            APIAttachIscsiServerToClusterMsg msg, IscsiServerClusterRefVO refVO, AtomicInteger retryTimes, Completion completion) {
        IscsiServerVO vo = Q.New(IscsiServerVO.class).eq(IscsiServerVO_.uuid, msg.getUuid()).find();

        ErrorCodeList errList = new ErrorCodeList();
        Map<String, IscsiServerInventory> hostIscsiServerMap = Maps.newConcurrentMap();

        boolean allowDiffConfig = StorageDeviceGlobalConfig.ALLOW_DIFFERENT_ISCSI_CONFIG.value(Boolean.class);
        new While<>(hostVOS).step((hostVO, whileCompletion) -> {
            StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
            bkd.loginIscsiServer(vo, HostInventory.valueOf(hostVO), new ReturnValueCompletion<IscsiServerInventory>(whileCompletion) {
                @Override
                public void success(IscsiServerInventory returnValue) {
                    synchronized (hostIscsiServerMap) {
                        if (hostIscsiServerMap.isEmpty() || allowDiffConfig) {
                            hostIscsiServerMap.putIfAbsent(hostVO.getUuid(), returnValue);
                            whileCompletion.done();
                            return;
                        }
                    }

                    Map.Entry<String, IscsiServerInventory> scannedServer = hostIscsiServerMap.entrySet().iterator().next();
                    List<IscsiTargetInventory> scannedTargets = scannedServer.getValue().getIscsiTargets();

                    if (!new HashSet<>(scannedTargets).equals(
                            new HashSet<>(returnValue.getIscsiTargets()))) {
                        errList.getCauses().add(operr("different iscsi configuration were found on host[uuid:%s, targets:%s]" +
                                "and host[uuid:%s, targets:%s]", scannedServer.getKey(), JSONObjectUtil.toJsonString(scannedTargets), hostVO.getUuid(), JSONObjectUtil.toJsonString(returnValue.getIscsiTargets())));
                    }
                    whileCompletion.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    errList.getCauses().add(errorCode);
                    whileCompletion.done();
                }
            });
        }, 15).run(new WhileDoneCompletion(msg) {
            private boolean iscsiServerChanged = false;

            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    if (retryTimes.decrementAndGet() == 0) {
                        completion.fail(errList.getCauses().get(0));
                        return;
                    }

                    logger.warn(String.format("scan host iscsi target failed, remaining scan times %s", retryTimes.get()));
                    attachIscsiServerToCluster(hostVOS, attachedHostUuids, msg, refVO, retryTimes, completion);
                    return;
                }

                dbf.persistAndRefresh(refVO);
                FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
                chain.setName(String.format("sync-iscsi-%s-%s", msg.getUuid(), vo.getIp()));
                chain.then(new NoRollbackFlow() {
                    String __name__ = "pre-check-iscsi-config-before-sync";
                    @Override
                    public boolean skip(Map data) {
                        return !allowDiffConfig;
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<IscsiTargetInventory> allscannedTargets = hostIscsiServerMap.values().stream().flatMap(serverInventory ->
                                serverInventory.getIscsiTargets().stream()).collect(Collectors.toList());

                        Map<String, List<IscsiLunInventory>> serialLunsMap = allscannedTargets.stream().flatMap(target -> target.getIscsiLuns().stream())
                                .collect(Collectors.groupingBy(IscsiLunInventory::getSerial));

                        for (String serial : serialLunsMap.keySet()) {
                            Map<String, List<IscsiLunInventory>> types = serialLunsMap.get(serial).stream().collect(Collectors.groupingBy(IscsiLunInventory::getType));
                            if (types.size() > 1) {
                                trigger.fail(operr("different disk types are found in different hosts for lun[serial:%s], " +
                                        "unable to attach it to cluster", serial));
                                return;
                            }
                        }
                        trigger.next();
                    }
                }).then(new NoRollbackFlow() {

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (hostIscsiServerMap.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        if (hostIscsiServerMap.keySet().stream().allMatch(hostUuid -> new HashSet<>(hostIscsiServerMap.get(hostUuid).getIscsiTargets())
                                .equals(new HashSet<>(IscsiServerInventory.valueOf(vo).getIscsiTargets())))) {
                            for (IscsiLunVO iscsiLunVO : dbf.reload(vo).getIscsiLuns()) {
                                persistScsiLunHostRef(iscsiLunVO, vo.getUuid());
                            }
                            trigger.next();
                            return;
                        }

                        iscsiServerChanged = true;
                        logger.debug(String.format("found iscsi[uuid: %s] changed ", msg.getUuid()));
                        if (allowDiffConfig) {
                            new While<>(hostIscsiServerMap.entrySet()).step((entry, whileCompletion) -> {
                                syncIscsiServerDbFromAgent(entry.getValue(), entry.getKey(), new NoErrorCompletion(msg) {
                                    @Override
                                    public void done() {
                                        whileCompletion.done();
                                    }
                                });
                            }, 5).run(new WhileDoneCompletion(trigger) {
                                @Override
                                public void done(ErrorCodeList errorCodeList) {
                                    trigger.next();
                                }
                            });
                            return;
                        }
                        IscsiServerInventory foundedServer = hostIscsiServerMap.entrySet().iterator().next().getValue();
                        syncIscsiServerDbFromAgent(foundedServer, null, new NoErrorCompletion(msg) {
                            @Override
                            public void done() {
                                trigger.next();
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        for (IscsiLunVO iscsiLunVO : dbf.reload(vo).getIscsiLuns()) {
                            cleanStaleLunHostRef(iscsiLunVO, vo.getUuid());
                        }
                        if (!iscsiServerChanged || attachedHostUuids == null || attachedHostUuids.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        for (String hostUuid : attachedHostUuids) {
                            RefreshIscsiServerMsg msg1 = new RefreshIscsiServerMsg();
                            msg1.setIscsiServerUuid(msg.getUuid());
                            logger.debug(String.format("refresh iscsi[uuid: %s] on host %s", msg.getUuid(), hostUuid));
                            msg1.setHostUuid(hostUuid);
                            bus.makeTargetServiceIdByResourceUuid(msg1, SERVICE_ID, msg1.getIscsiServerUuid());
                            bus.send(msg1);
                        }
                        trigger.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        dbf.remove(refVO);
                        completion.fail(errCode);
                    }
                }).done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                }).start();
            }
        });
    }

    private void handle(APIDetachIscsiServerFromClusterMsg msg) {
        APIDetachIscsiServerFromClusterEvent event = new APIDetachIscsiServerFromClusterEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                detachIscsiServerFromCluster(msg.getUuid(), msg.getClusterUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        done();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        IscsiServerVO iscsiServerVO = Q.New(IscsiServerVO.class)
                                .eq(IscsiServerVO_.uuid, msg.getUuid())
                                .find();

                        event.setInventory(IscsiServerInventory.valueOf(iscsiServerVO));
                        event.setError(errorCode);
                        event.setSuccess(false);
                        bus.publish(event);
                        chain.next();
                    }

                    private void done() {
                        IscsiServerClusterRefVO refVO = Q.New(IscsiServerClusterRefVO.class)
                                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                                .eq(IscsiServerClusterRefVO_.clusterUuid, msg.getClusterUuid())
                                .find();
                        dbf.remove(refVO);

                        IscsiServerVO iscsiServerVO = Q.New(IscsiServerVO.class)
                                .eq(IscsiServerVO_.uuid, msg.getUuid())
                                .find();

                        if (iscsiServerVO.getIscsiLuns() == null || iscsiServerVO.getIscsiLuns().isEmpty()) {
                            event.setInventory(IscsiServerInventory.valueOf(iscsiServerVO));
                            bus.publish(event);

                            return;
                        }
                        for (IscsiLunVO iscsiLunVO : iscsiServerVO.getIscsiLuns()) {
                            cleanStaleLunHostRef(iscsiLunVO, iscsiServerVO.getUuid());
                        }
                        iscsiServerVO = Q.New(IscsiServerVO.class)
                                .eq(IscsiServerVO_.uuid, msg.getUuid())
                                .find();

                        event.setInventory(IscsiServerInventory.valueOf(iscsiServerVO));
                        bus.publish(event);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("attach-and-detach-iscsi-server-%s-from-cluster", msg.getUuid());
            }

            @Override
            public String getName() {
                return String.format("detach-iscsi-server-%s-from-cluster", msg.getUuid());
            }
        });
    }

    private void detachIscsiServerFromCluster(String iscsiServeruuid, String clusterUuid, Completion completion) {
        List<HostVO> attachedHosts = Q.New(HostVO.class)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.clusterUuid, clusterUuid)
                .list();

        IscsiServerVO iscsiServerVO = Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.uuid, iscsiServeruuid)
                .find();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-iscsi-server-%s-%s-from-cluster-%s", iscsiServeruuid, iscsiServerVO.getIp(), clusterUuid));
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-and-detach-scsi-lun-from-vm";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(iscsiServerVO.getIscsiLuns()).step((iscsiLunVO, whileCompletion) -> {
                    if (iscsiLunVO.getScsiLunVmInstanceRefs() == null || iscsiLunVO.getScsiLunVmInstanceRefs().isEmpty()) {
                        whileCompletion.done();
                        return;
                    }
                    new While<>(iscsiLunVO.getScsiLunVmInstanceRefs()).step((vmInstanceRefVO, whileCompletion1) -> {
                        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class)
                                .eq(VmInstanceVO_.uuid, vmInstanceRefVO.getVmInstanceUuid())
                                .find();
                        if ((vmInstanceVO.getState().equals(VmInstanceState.Stopped) ||
                                vmInstanceVO.getState().equals(VmInstanceState.Destroyed)) &&
                                iscsiServerVO.getIscsiClusterRefs().size() == 1 &&
                                iscsiServerVO.getIscsiClusterRefs().iterator().next().getClusterUuid().equals(clusterUuid)) {
                            dbf.remove(vmInstanceRefVO);
                            whileCompletion1.done();
                            return;
                        }
                        if (!vmInstanceVO.getClusterUuid().equals(clusterUuid)) {
                            whileCompletion1.done();
                            return;
                        }
                        StorageDeviceBackend bkd = backends.get(vmInstanceVO.getHypervisorType());
                        bkd.detachScsiLunFromVm(iscsiLunVO, vmInstanceVO, vmInstanceRefVO, new Completion(whileCompletion1) {
                            @Override
                            public void success() {
                                dbf.remove(vmInstanceRefVO);
                                whileCompletion1.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errList.getCauses().add(errorCode);
                                whileCompletion1.done();
                            }
                        });
                    }, 5).run(new WhileDoneCompletion(whileCompletion) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errList.getCauses().isEmpty()) {
                            trigger.fail(errList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "logout-iscsi-from-cluster";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                new While<>(attachedHosts).step((hostVO, whileCompletion) -> {
                    StorageDeviceBackend bkd = backends.get(hostVO.getHypervisorType());
                    bkd.logoutIscsiServer(iscsiServerVO, HostInventory.valueOf(hostVO), new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            errList.getCauses().add(errorCode);
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errList.getCauses().get(0));
                        }
                    }
                });
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

    private void handle(APIDeleteIscsiServerMsg msg) {
        APIDeleteIscsiServerEvent event = new APIDeleteIscsiServerEvent(msg.getId());

        IscsiServerVO iscsiServerVO = Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.uuid, msg.getUuid())
                .find();

        List<IscsiServerClusterRefVO> refVOS = Q.New(IscsiServerClusterRefVO.class)
                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                .list();

        dbf.removeCollection(refVOS, IscsiServerClusterRefVO.class);

        List<String> targetUuids = Q.New(IscsiTargetVO.class)
                .select(IscsiTargetVO_.uuid)
                .eq(IscsiTargetVO_.iscsiServerUuid, msg.getUuid())
                .listValues();

        if (targetUuids == null || targetUuids.isEmpty()) {
            dbf.removeByPrimaryKey(iscsiServerVO.getUuid(), IscsiServerVO.class);
            bus.publish(event);
            return;
        }

        List<String> lunUuids = Q.New(IscsiLunVO.class)
                .select(IscsiLunVO_.uuid)
                .in(IscsiLunVO_.iscsiTargetUuid, targetUuids)
                .listValues();

        if (lunUuids == null || lunUuids.isEmpty()) {
            dbf.removeByPrimaryKeys(targetUuids, IscsiTargetVO.class);
            dbf.removeByPrimaryKey(iscsiServerVO.getUuid(), IscsiServerVO.class);
            bus.publish(event);
            return;
        }

        try {
            removeScsiLunHostRefAndIscsiLun(lunUuids);
        } catch (DataIntegrityViolationException | PersistenceException e) {
            if (!ExceptionDSL.isCausedBy(e, ConstraintViolationException.class) && !ExceptionDSL.isCausedBy(e, DataIntegrityViolationException.class)) {
                throw e;
            }
            removeScsiLunHostRefAndIscsiLun(lunUuids);
        }
        dbf.removeByPrimaryKeys(targetUuids, IscsiTargetVO.class);
        dbf.removeByPrimaryKey(iscsiServerVO.getUuid(), IscsiServerVO.class);
        bus.publish(event);
    }

    private void removeScsiLunHostRefAndIscsiLun(List<String> lunUuids) {
        SQL.New(ScsiLunHostRefVO.class)
                .in(ScsiLunHostRefVO_.scsiLunUuid, lunUuids)
                .hardDelete();
        dbf.removeByPrimaryKeys(lunUuids, IscsiLunVO.class);
    }

    private void handle(APIRefreshIscsiServerMsg msg) {
        APIRefreshIscsiServerEvent event = new APIRefreshIscsiServerEvent(msg.getId());

        IscsiServerVO vo = Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.uuid, msg.getUuid())
                .find();

        List<String> attachedClusterUuids = Q.New(IscsiServerClusterRefVO.class)
                .select(IscsiServerClusterRefVO_.clusterUuid)
                .eq(IscsiServerClusterRefVO_.iscsiServerUuid, msg.getUuid())
                .listValues();

        if (attachedClusterUuids == null || attachedClusterUuids.isEmpty()) {
            for (IscsiLunVO iscsiLunVO : vo.getIscsiLuns()) {
                cleanStaleLunHostRef(iscsiLunVO, vo.getUuid());
            }
            vo = Q.New(IscsiServerVO.class)
                    .eq(IscsiServerVO_.uuid, msg.getUuid())
                    .find();

            event.setInventory(IscsiServerInventory.valueOf(vo));
            bus.publish(event);
            return;
        }

        List<String> attachedHostUuids = Q.New(HostVO.class)
                .select(HostVO_.uuid)
                .in(HostVO_.clusterUuid, attachedClusterUuids)
                .eq(HostVO_.status, HostStatus.Connected)
                .listValues();


        if (attachedHostUuids.isEmpty()) {
            event.setInventory(IscsiServerInventory.valueOf(vo));
            bus.publish(event);
            return;
        }

        FlowChain flowChain = FlowChainBuilder.newSimpleFlowChain();
        flowChain.setName(String.format("refresh iscsi server %s", msg.getUuid()));
        flowChain.then(new NoRollbackFlow() {
            String __name__ = "select one host refresh first";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                RefreshIscsiServerMsg rmsg = new RefreshIscsiServerMsg();
                rmsg.setIscsiServerUuid(msg.getUuid());
                rmsg.setHostUuid(attachedHostUuids.get(0));
                bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, rmsg.getIscsiServerUuid());
                bus.send(rmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        RefreshIscsiServerReply rreply = reply.castReply();
                        if (!rreply.isSuccess()) {
                            trigger.fail(rreply.getError());
                            return;
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "refresh other hosts";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                ErrorCodeList errList = new ErrorCodeList();
                attachedHostUuids.remove(attachedHostUuids.get(0));
                new While<>(attachedHostUuids).step((hostUuid, whileCompletion) -> {
                    RefreshIscsiServerMsg rmsg = new RefreshIscsiServerMsg();
                    rmsg.setIscsiServerUuid(msg.getUuid());
                    rmsg.setHostUuid(hostUuid);
                    bus.makeTargetServiceIdByResourceUuid(rmsg, StorageDeviceConstants.SERVICE_ID, rmsg.getIscsiServerUuid());
                    bus.send(rmsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                errList.getCauses().add(reply.getError());
                                whileCompletion.done();
                                return;
                            }

                            RefreshIscsiServerReply reply1 = reply.castReply();
                            if (!reply1.isSuccess()) {
                                errList.getCauses().add(reply1.getError());
                            }
                            whileCompletion.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errList.getCauses().isEmpty()) {
                            trigger.fail(errList.getCauses().get(0));
                        } else {
                            IscsiServerVO vo = Q.New(IscsiServerVO.class)
                                    .eq(IscsiServerVO_.uuid, msg.getUuid())
                                    .find();
                            event.setInventory(IscsiServerInventory.valueOf(vo));
                            trigger.next();
                        }
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
                bus.publish(event);
            }
        }).start();
    }

    private void handle(APIUpdateIscsiServerMsg msg) {
        APIUpdateIscsiServerEvent event = new APIUpdateIscsiServerEvent(msg.getId());

        IscsiServerVO vo = Q.New(IscsiServerVO.class)
                .eq(IscsiServerVO_.uuid, msg.getUuid()).find();
        if (msg.getState() != null) {
            vo.setState(StorageDeviceState.valueOf(msg.getState()).toString());
        }

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getChapUserName() != null) {
            vo.setChapUserName(msg.getChapUserName());
        }

        if (msg.getChapUserPassword() != null) {
            vo.setChapUserPassword(msg.getChapUserPassword());
        }

        vo = dbf.updateAndRefresh(vo);
        event.setInventory(IscsiServerInventory.valueOf(vo));
        bus.publish(event);
    }

    public List<IscsiServerVO> getClusterEnabledIscsiServer(String clusterUuid) {
        return SQL.New("select server " +
                "from IscsiServerVO server, IscsiServerClusterRefVO ref " +
                "where server.uuid = ref.iscsiServerUuid " +
                "and server.state = :stateEnabled " +
                "and ref.clusterUuid = :clusterUuid")
                .param("stateEnabled", StorageDeviceState.Enabled.toString())
                .param("clusterUuid", clusterUuid).list();
    }

    public List<NvmeServerVO> getClusterEnabledNvmeServer(String clusterUuid) {
        return SQL.New("select server " +
                        "from NvmeServerVO server, NvmeServerClusterRefVO ref " +
                        "where server.uuid = ref.nvmeServerUuid " +
                        "and server.state = :stateEnabled " +
                        "and ref.clusterUuid = :clusterUuid")
                .param("stateEnabled", StorageDeviceState.Enabled.toString())
                .param("clusterUuid", clusterUuid).list();
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void connectionReestablished(HostInventory inv) {
        List<IscsiServerVO> serverVOS = getClusterEnabledIscsiServer(inv.getClusterUuid());
        if (serverVOS == null || serverVOS.isEmpty()) {
            return;
        }
        for (IscsiServerVO s : serverVOS) {
            RefreshIscsiServerMsg msg = new RefreshIscsiServerMsg();
            msg.setHostUuid(inv.getUuid());
            msg.setIscsiServerUuid(s.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, StorageDeviceConstants.SERVICE_ID, msg.getIscsiServerUuid());
            bus.send(msg);
        }
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "login-storage-devices";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                // Not care about results, primary storage will check more strict
                StorageDeviceBackend bkd = backends.get(context.getInventory().getHypervisorType());
                refreshStorageDevice(context.getInventory(), context.isNewAddedHost());
                String enableMultipath = rcf.getResourceConfigValue(StorageDeviceGlobalConfig.ENABLE_MULTIPATH, context.getInventory().getClusterUuid(), String.class);
                if ("enable".equals(enableMultipath)) {
                    bkd.enableMultipathDevice(context.getInventory(), new NopeCompletion());
                } else if ("disable".equals(enableMultipath)) {
                    bkd.disableMultipathDevice(context.getInventory(), new NopeCompletion());
                }

                if (StorageDeviceGlobalConfig.ENABLE_LOCALRAID.value(Boolean.class)) {
                    RefreshLocalRaidMsg msg = new RefreshLocalRaidMsg();
                    msg.setHostUuid(context.getInventory().getUuid());
                    bus.makeLocalServiceId(msg, StorageDeviceConstants.SERVICE_ID);
                    bus.send(msg);
                }

                List<IscsiServerVO> serverVOS = getClusterEnabledIscsiServer(context.getInventory().getClusterUuid());
                if (serverVOS == null || serverVOS.isEmpty()) {
                    trigger.next();
                    return;
                }

                List<RefreshIscsiServerMsg> refreshMsgs = serverVOS.stream().map(serverVO -> {
                    RefreshIscsiServerMsg msg = new RefreshIscsiServerMsg();
                    msg.setHostUuid(context.getInventory().getUuid());
                    msg.setIscsiServerUuid(serverVO.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, SERVICE_ID, msg.getIscsiServerUuid());
                    return msg;
                }).collect(Collectors.toList());

                if (context.isNewAddedHost()) {
                    new While<>(refreshMsgs).step((msg, completion) -> {
                        bus.send(msg, new CloudBusCallBack(completion) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    logger.warn(String.format("host[%s] login iscsi server[%s] failed, because %s", context.getInventory().getUuid(), msg.getIscsiServerUuid(), reply.getError().getDetails()));
                                }
                                completion.done();
                            }
                        });
                    }, 5).run(new WhileDoneCompletion(trigger) {
                        @Override
                        public void done(ErrorCodeList errorCodeList) {
                            trigger.next();
                        }
                    });
                } else {
                    refreshMsgs.forEach(msg -> bus.send(msg));
                    trigger.next();
                }
            }
        };
    }

    @Override
    public void kvmPingAgentNoFailure(KVMHostInventory host, NoErrorCompletion completion) {
        Set<String> disconnectedPrimaryStorages = checkSharedBlockGroupPrimaryStorageDisconnectedOnHost(host);
        if (disconnectedPrimaryStorages == null || disconnectedPrimaryStorages.size() == 0) {
            completion.done();
            return;
        }

        refreshStorageDevice(host, false);
        completion.done();
    }

    private void refreshStorageDevice(HostInventory host, boolean newAdd) {
        StorageDeviceBackend bkd = backends.get(host.getHypervisorType());
        if (StorageDeviceGlobalConfig.ENABLE_FC_DEVICE_SCAN.value(Boolean.class) || newAdd) {
            RefreshFiberChannelMsg msg = new RefreshFiberChannelMsg();
            msg.setHostUuid(host.getUuid());
            bus.makeLocalServiceId(msg, StorageDeviceConstants.SERVICE_ID);
            bus.send(msg);
        }

        if (StorageDeviceGlobalConfig.ENABLE_NVME_DEVICE_SCAN.value(Boolean.class)) {
            List<NvmeServerVO> serverVOS = getClusterEnabledNvmeServer(host.getClusterUuid());
            serverVOS.forEach(s -> {
                ConnectNvmeServerMsg cmsg = new ConnectNvmeServerMsg();
                cmsg.setHostUuid(host.getUuid());
                cmsg.setNvmeServerUuid(s.getUuid());
                bus.makeTargetServiceIdByResourceUuid(cmsg, StorageDeviceConstants.SERVICE_ID, host.getUuid());
                bus.send(cmsg);
            });
        }

        RefreshHbaDeviceMsg hbaMsg = new RefreshHbaDeviceMsg();
        hbaMsg.setHostUuid(host.getUuid());
        bus.makeLocalServiceId(hbaMsg, StorageDeviceConstants.SERVICE_ID);
        bus.send(hbaMsg);

        String enableMultipath = rcf.getResourceConfigValue(StorageDeviceGlobalConfig.ENABLE_MULTIPATH, host.getClusterUuid(), String.class);
        if ("enable".equals(enableMultipath)) {
            bkd.enableMultipathDevice(host, new NopeCompletion());
        } else if ("disable".equals(enableMultipath)) {
            bkd.disableMultipathDevice(host, new NopeCompletion());
        }

        List<IscsiServerVO> serverVOS = getClusterEnabledIscsiServer(host.getClusterUuid());
        if (serverVOS == null || serverVOS.isEmpty()) {
            return;
        }
        RefreshIscsiServerMsg msg = new RefreshIscsiServerMsg();
        msg.setHostUuid(host.getUuid());
        for (IscsiServerVO s : serverVOS) {
            msg.setIscsiServerUuid(s.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, StorageDeviceConstants.SERVICE_ID, msg.getIscsiServerUuid());
            bus.send(msg);
        }
        
    }

    @Override
    public void beforeGetNextVolumeDeviceId(String vmUuid, List<Integer> devIds) {
        List<Integer> deviceIds = Q.New(ScsiLunVmInstanceRefVO.class)
                .select(ScsiLunVmInstanceRefVO_.deviceId)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, vmUuid)
                .listValues();

        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }

        devIds.addAll(deviceIds);
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        List<ScsiLunVmInstanceRefVO> refVOS = Q.New(ScsiLunVmInstanceRefVO.class).eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, spec.getVmInventory().getUuid()).list();
        if (refVOS == null || refVOS.isEmpty()) {
            return;
        }

        for (ScsiLunVmInstanceRefVO refVO : refVOS) {
            ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, refVO.getScsiLunUuid()).find();
            if (lunVO == null || StorageDeviceState.Disabled.toString().equals(lunVO.getState())) {
                throw new OperationFailureException(operr("specified scsi lun[wwid: %s] not exists or disabled", refVO.getScsiLunUuid()));
            }
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
    }

    @Override
    public void prepareDbInitialValue() {
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        List<RaidControllerVO> controllerVOS = Q.New(RaidControllerVO.class).eq(RaidControllerVO_.hostUuid, host.getUuid()).list();
        if (controllerVOS.isEmpty()){
            return;
        }

        for (RaidControllerVO raidControllerVO : controllerVOS) {
            List<RaidPhysicalDriveVO> physicalDriveVOS = Q.New(RaidPhysicalDriveVO.class).eq(RaidPhysicalDriveVO_.raidControllerUuid, raidControllerVO.getUuid()).list();

            for (RaidPhysicalDriveVO physicalDriveVO : physicalDriveVOS) {
                LocateLocalRaidPhysicalDriveMsg msg = new LocateLocalRaidPhysicalDriveMsg();
                if (physicalDriveVO.getLocateStatus() == LocateStatus.Enabled) {
                    msg.setLocate(true);
                } else {
                    msg.setLocate(false);
                }
                msg.setControllerInventory(RaidControllerInventory.valueOf(raidControllerVO));
                msg.setPhysicalDriveInventory(RaidPhysicalDriveInventory.valueOf(physicalDriveVO));
                bus.makeTargetServiceIdByResourceUuid(msg, StorageDeviceConstants.SERVICE_ID, raidControllerVO.getHostUuid());
                bus.send(msg);
            }
        }
    }

    @Override
    public void attachOtherDiskToVm(DiskAO diskAO, String vmInstanceUuid, Completion completion) {
        String resourceUuid = diskAO.getSourceUuid();
        List<String> systemTags = diskAO.getSystemTags() == null ? new ArrayList<>() : diskAO.getSystemTags();

        ScsiLunVO lunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, resourceUuid).find();

        if (Q.New(ScsiLunVmInstanceRefVO.class)
                .eq(ScsiLunVmInstanceRefVO_.scsiLunUuid, resourceUuid)
                .eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, vmInstanceUuid).isExists()) {
            completion.success();
            return;
        }

        if (hasAttachSameWwidScsiLun(vmInstanceUuid, lunVO)) {
            completion.fail(operr("scsi[%s] lun[wwid:%s] has been attached into the vm[%s]",
                    resourceUuid, lunVO.getWwid(), vmInstanceUuid));
            return;
        }

        VmInstanceVO vmInstanceVO = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid).find();
        if (!allowedVmOperationStates.contains(vmInstanceVO.getState())) {
            completion.fail(argerr("vm instance[%s] state [%s] not in allowed state[%s] for operation",
                    vmInstanceVO.getUuid(), vmInstanceVO.getState(), allowedVmOperationStates));
            return;
        }

        if (vmInstanceVO.getHostUuid() != null) {
            if (!Q.New(ScsiLunHostRefVO.class)
                    .eq(ScsiLunHostRefVO_.scsiLunUuid, resourceUuid)
                    .eq(ScsiLunHostRefVO_.hostUuid, vmInstanceVO.getHostUuid()).isExists()) {
                completion.fail(argerr("vm instance[uuid: %s] host[uuid: %s] not attached scsi lun[uuid: %s]",
                        vmInstanceVO.getUuid(), vmInstanceVO.getHostUuid(), resourceUuid));
                return;
            }
        }
        ClusterVO clusterVO = Q.New(ClusterVO.class).eq(ClusterVO_.uuid, vmInstanceVO.getClusterUuid()).find();

        ScsiLunVmInstanceRefVO refVO = new ScsiLunVmInstanceRefVO();
        refVO.setScsiLunUuid(resourceUuid);
        refVO.setVmInstanceUuid(vmInstanceUuid);
        refVO.setCreateDate(new Timestamp(System.currentTimeMillis()));
        refVO.setDeviceId(new NextVolumeDeviceIdGetter().getNextVolumeDeviceId(vmInstanceUuid));
        if (!CollectionUtils.isEmpty(systemTags)) {
            refVO.setAttachMultipath(!systemTags.contains(StorageDeviceSsytemTags.DISABLE_ATTACH_MULTIPATH.getTagFormat()));
        }
        dbf.persist(refVO);

        if (VmInstanceState.Stopped.equals(vmInstanceVO.getState())) {
            completion.success();
            return;
        }

        StorageDeviceBackend bkd = backends.get(clusterVO.getHypervisorType());
        bkd.attachScsiLunToVm(lunVO, vmInstanceVO, refVO, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(refVO);
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public String getDiskType() {
        return LunVO.class.getSimpleName();
    }
}
