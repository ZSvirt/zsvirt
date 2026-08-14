package org.zstack.storage.primary.block;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KvmSetupSelfFencerExtensionPoint;
import org.zstack.storage.primary.block.message.*;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/14 15:22
 */
public class BlockPrimaryStorageManagerImpl extends AbstractService implements Component {
    private static final CLogger logger = Utils.getLogger(BlockPrimaryStorageManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private BlockPrimaryStorageFactory blockPrimaryStorageFactory;

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
        if (msg instanceof DeleteBlockPrimaryStorageHostMsg) {
            handle((DeleteBlockPrimaryStorageHostMsg) msg);
        } else if (msg instanceof InitBlockPrimaryStorageOnHostConnectedMsg) {
            handle((InitBlockPrimaryStorageOnHostConnectedMsg) msg);
        } else if (msg instanceof DeleteVolumeLunOnPrimaryStorageMsg) {
            handle((DeleteVolumeLunOnPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetBlockPrimaryStorageMetadataMsg) {
            handle((APIGetBlockPrimaryStorageMetadataMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected void handle(DeleteVolumeLunOnPrimaryStorageMsg msg) {
        DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
        if (StringUtils.isEmpty(msg.getPrimaryStorageUuid())) {
            reply.setError(operr("primary storage uuid is mandatory when delete lun"));
            bus.reply(msg, reply);
            return;
        }
        deleteVolumeLunOnPrimaryStorage(msg.getLunName(), msg.getLunId(), msg.getPrimaryStorageUuid(), new Completion(reply) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();
                blockScsiLunVO.setName(msg.getLunName());
                blockScsiLunVO.setId(msg.getLunId());

                VolumeInventory volume = new VolumeInventory();
                volume.setUuid(msg.getBitsUuid());
                BlockPrimaryStorageDeleteVolumeGC gc = new BlockPrimaryStorageDeleteVolumeGC();
                gc.blockScsiLunVO = blockScsiLunVO;
                gc.volume = volume;
                gc.deduplicateSubmit(BlockPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);
                bus.reply(msg, reply);
            }
        });
    }

    private void deleteVolumeLunOnPrimaryStorage(String lunName, Integer lunId, String primaryStorageUuid, Completion completion) {

        BlockPrimaryStorageDeviceBackend bkd = blockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(primaryStorageUuid);
        final String skipNextFlowsToken = "skipNextFlows";
        Map data = new HashMap();
        data.put(skipNextFlowsToken, false);
        List<String> hostUuids = new ArrayList<>();
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setData(data);
        chain.setName("delete-volume-lun-on-block-primary-storage");
        chain.then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.getLunByName(lunName, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        data.put(BlockPrimaryStorageConstants.Params.BlockScsiLun, returnValue);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if(errorCode.isError(LunErrors.LUN_CAN_NOT_BE_FOUND)) {
                            data.put(skipNextFlowsToken, true);
                            trigger.next();
                            return;
                        }
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                bkd.getLunMappedHosts(blockScsiLunVO, new ReturnValueCompletion<List<String>>(trigger) {
                    @Override
                    public void success(List<String> returnValue) {
                        hostUuids.addAll(returnValue);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public boolean skip(Map map) {
                return (boolean) map.get(skipNextFlowsToken);
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.logoutTarget(hostUuids.get(0), blockScsiLunVO, bkd.getIscsiServer(blockScsiLunVO), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
            @Override
            public boolean skip(Map map) {
                if (hostUuids.isEmpty()) {
                    return true;
                }
                return (boolean) map.get(skipNextFlowsToken);
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockScsiLunVO blockScsiLunVO = (BlockScsiLunVO) data.get(BlockPrimaryStorageConstants.Params.BlockScsiLun);
                bkd.deleteLun(blockScsiLunVO.getId(), new Completion( trigger) {
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

            @Override
            public boolean skip(Map map) {
                return (boolean) map.get(skipNextFlowsToken);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map map) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map map) {
                if (errorCode.isError(LunErrors.LUN_CAN_NOT_BE_FOUND)) {
                    completion.success();
                    return;
                }
                completion.fail(errorCode);
            }
        }).start();
    }

    private void handle(InitBlockPrimaryStorageOnHostConnectedMsg msg) {
        final InitBlockPrimaryStorageOnHostConnectedReply reply = new InitBlockPrimaryStorageOnHostConnectedReply();
        final BlockScsiLunVO heartbeatLun = blockPrimaryStorageFactory.generateHeartbeatLun(msg.getPrimaryStorageUuid());

        BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO = new BlockPrimaryStorageHostRefVO();
        blockPrimaryStorageHostRefVO.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        blockPrimaryStorageHostRefVO.setHostUuid(msg.getHostUuid());
        blockPrimaryStorageHostRefVO.setMetadata(msg.getMetadata());
        if (StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getMetadata()) && msg.getNewAdded().equals(false)) {
            String metadata = Q.New(BlockPrimaryStorageHostRefVO.class)
                    .select(BlockPrimaryStorageHostRefVO_.metadata)
                    .eq(BlockPrimaryStorageHostRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                    .eq(BlockPrimaryStorageHostRefVO_.hostUuid, msg.getHostUuid())
                    .findValue();
            blockPrimaryStorageHostRefVO.setMetadata(metadata);
        }
        blockPrimaryStorageHostRefVO.setInitiatorName(msg.getInitiatorName());

        BlockPrimaryStorageDeviceBackend bkd = blockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(msg.getPrimaryStorageUuid());
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        final Boolean[] psIsAlreadyMounted = {false};
        chain.setName(String.format("init block primary storage %s on connected host %s", msg.getPrimaryStorageUuid(), msg.getHostUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = String.format("ping host %s agent first", msg.getHostUuid());
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.pingAgentNoFailure(msg.getHostUuid(), Collections.singletonList(heartbeatLun.getInstallPath()), new ReturnValueCompletion<BlockPrimaryStorageKvmCommandDispatcher.PingAgentNoFailureRsp>(trigger) {
                    @Override
                    public void success(BlockPrimaryStorageKvmCommandDispatcher.PingAgentNoFailureRsp returnValue) {
                        psIsAlreadyMounted[0] = true;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("get initiator name of host %s", msg.getHostUuid());
            @Override
            public void run(FlowTrigger trigger, Map data) {
                getHostInitiatorName(blockPrimaryStorageHostRefVO, new ReturnValueCompletion<String>(trigger) {
                    @Override
                    public void success(String returnValue) {
                        blockPrimaryStorageHostRefVO.setInitiatorName(returnValue);
                        SQL.New(BlockPrimaryStorageHostRefVO.class)
                                .eq(BlockPrimaryStorageHostRefVO_.hostUuid, blockPrimaryStorageHostRefVO.getHostUuid())
                                .set(BlockPrimaryStorageHostRefVO_.initiatorName, returnValue)
                                .update();
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new Flow() {
            String __name__ = String.format("add host:%s into block storage", msg.getHostUuid());
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.addHost(blockPrimaryStorageHostRefVO, new ReturnValueCompletion<BlockPrimaryStorageHostRefVO>(trigger) {
                    @Override
                    public void success(BlockPrimaryStorageHostRefVO returnValue) {
                        blockPrimaryStorageHostRefVO.setMetadata(returnValue.getMetadata());
                        SQL.New(BlockPrimaryStorageHostRefVO.class)
                                .eq(BlockPrimaryStorageHostRefVO_.hostUuid, blockPrimaryStorageHostRefVO.getHostUuid())
                                .eq(BlockPrimaryStorageHostRefVO_.primaryStorageUuid, blockPrimaryStorageHostRefVO.getPrimaryStorageUuid())
                                .set(BlockPrimaryStorageHostRefVO_.metadata, returnValue.getMetadata())
                                .update();
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
                //Only newAdded host needs to be deleted.
                if (!msg.getNewAdded()) {
                    trigger.rollback();
                    return;
                }
                bkd.deleteHost(blockPrimaryStorageHostRefVO.getMetadata(), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }

            @Override
            public boolean skip(Map map) {
                if (!msg.getNewAdded()) {
                    return true;
                }
                return false;
            }
        }).then(new Flow() {
            String __name__ = String.format("map heart beat lun to host:%s", msg.getHostUuid());
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.createLunMap(heartbeatLun, blockPrimaryStorageHostRefVO, new ReturnValueCompletion<BlockScsiLunVO>(trigger) {
                    @Override
                    public void success(BlockScsiLunVO returnValue) {
                        heartbeatLun.setTarget(returnValue.getTarget());
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
                //Only newAdded host needs to be deleted lun map.
                if (!msg.getNewAdded()) {
                    trigger.rollback();
                    return;
                }
                bkd.deleteLunMap(heartbeatLun, blockPrimaryStorageHostRefVO, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.rollback();
                    }
                });
            }

            @Override
            public boolean skip(Map map) {
                if (psIsAlreadyMounted[0]) {
                    return true;
                }
                return false;
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("create heartbeat on host:%s", msg.getHostUuid());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HostVO hostVO = Q.New(HostVO.class)
                        .eq(HostVO_.uuid, msg.getHostUuid())
                        .find();
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.createHeartbeat(hostVO.getUuid(), heartbeatLun, bkd.getIscsiServer(heartbeatLun), new Completion(trigger) {
                    @Override
                    public void success() {
                        blockPrimaryStorageFactory.changeBlockPrimaryStorageHostRefStatus(msg.getPrimaryStorageUuid(), hostVO.getUuid(), PrimaryStorageHostStatus.Connected);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public boolean skip(Map map) {
                return psIsAlreadyMounted[0];
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                blockPrimaryStorageFactory.changeBlockPrimaryStorageHostRefStatus(msg.getPrimaryStorageUuid(), msg.getHostUuid(), PrimaryStorageHostStatus.Connected);
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                blockPrimaryStorageFactory.changeBlockPrimaryStorageHostRefStatus(msg.getPrimaryStorageUuid(), msg.getHostUuid(), PrimaryStorageHostStatus.Disconnected);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void deleteBlockPrimaryStorageHost(String hostUuid, String psUuid, String hostMetadata, Completion completion) {
        BlockPrimaryStorageVO blockPrimaryStorageVO = Q.New(BlockPrimaryStorageVO.class)
                .eq(BlockPrimaryStorageVO_.uuid, psUuid)
                .find();

        if (blockPrimaryStorageVO == null) {
            completion.success();
            return;
        }

        BlockPrimaryStorageDeviceBackend bkd = blockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(blockPrimaryStorageVO);
        if (bkd == null) {
            completion.success();
            return;
        }
        PrimaryStorageInventory primaryStorageInventory = blockPrimaryStorageVO.toInventory();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName("delete host from block primary storage");
        chain.then(new NoRollbackFlow() {
            String __name__ = "cancel self fencer on host";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                CancelSelfFencerOnKvmHostMsg cancelSelfFencerOnKvmHostMsg = new CancelSelfFencerOnKvmHostMsg();
                KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam param = new KvmSetupSelfFencerExtensionPoint.KvmCancelSelfFencerParam();
                param.setHostUuid(hostUuid);
                param.setPrimaryStorage(primaryStorageInventory);
                cancelSelfFencerOnKvmHostMsg.setParam(param);
                bus.makeTargetServiceIdByResourceUuid(cancelSelfFencerOnKvmHostMsg, PrimaryStorageConstant.SERVICE_ID, hostUuid);
                bus.send(cancelSelfFencerOnKvmHostMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete heart beat on host";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.deleteHeartbeat(hostUuid, psUuid, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                BlockScsiLunVO heartbeatLun = Q.New(BlockScsiLunVO.class)
                        .eq(BlockScsiLunVO_.name, blockPrimaryStorageFactory.generateHeartbeatLunName(psUuid))
                        .find();

                if (heartbeatLun == null) {
                    trigger.next();
                    return;
                }
                BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageKvmCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
                blockPrimaryStorageKvmCommandDispatcher.logoutTarget(hostUuid, heartbeatLun, bkd.getIscsiServer(heartbeatLun), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete lun map on storage";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                bkd.deleteHost(hostMetadata, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
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

    private void handle(DeleteBlockPrimaryStorageHostMsg msg) {
        DeleteBlockPrimaryStorageHostReply reply = new DeleteBlockPrimaryStorageHostReply();

        if (StringUtils.isEmpty(msg.getPrimaryStorageUuid())) {
            bus.reply(msg, reply);
            return;
        }
        deleteBlockPrimaryStorageHost(msg.getHostUuid(), msg.getPrimaryStorageUuid(), msg.getMetadata(), new Completion(msg) {
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

    private void handle(APIGetBlockPrimaryStorageMetadataMsg msg) {
        BlockPrimaryStorageDeviceBackend blockPrimaryStorageDeviceBackend = blockPrimaryStorageFactory.getBlockPrimaryStorageDeviceBackend(msg.getVendorName(), msg.getMetadata());
        String metadata = blockPrimaryStorageDeviceBackend.syncMetadata();
        APIQueryBlockPrimaryStorageReply reply = new APIQueryBlockPrimaryStorageReply();
        List<BlockPrimaryStorageInventory> blockPrimaryStorageInventories = new ArrayList<>();
        BlockPrimaryStorageInventory blockPrimaryStorageInventory = new BlockPrimaryStorageInventory();
        blockPrimaryStorageInventory.setVendorName(msg.getVendorName());
        blockPrimaryStorageInventory.setMetadata(metadata);
        blockPrimaryStorageInventories.add(blockPrimaryStorageInventory);
        reply.setInventories(blockPrimaryStorageInventories);
        bus.reply(msg, reply);
        return;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(BlockPrimaryStorageConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    public void getHostInitiatorName(BlockPrimaryStorageHostRefVO blockPrimaryStorageHostRefVO, ReturnValueCompletion<String> completion) {
        BlockPrimaryStorageKvmCommandDispatcher blockPrimaryStorageCommandDispatcher = new BlockPrimaryStorageKvmCommandDispatcher();
        if (StringUtils.isNotEmpty(blockPrimaryStorageHostRefVO.getInitiatorName())) {
            completion.success(blockPrimaryStorageHostRefVO.getInitiatorName());
            return;
        }
        if (StringUtils.isEmpty(blockPrimaryStorageHostRefVO.getHostUuid())) {
            completion.fail(operr(String.format("hostUuid is mandatory when get host initiator name")));
            return;
        }
        blockPrimaryStorageCommandDispatcher.getInitiatorName(blockPrimaryStorageHostRefVO.getHostUuid(),  new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String returnValue) {
                Boolean initiatorOccupiedByOtherHost = Q.New(BlockPrimaryStorageHostRefVO.class)
                        .eq(BlockPrimaryStorageHostRefVO_.initiatorName, returnValue)
                        .notEq(BlockPrimaryStorageHostRefVO_.hostUuid, blockPrimaryStorageHostRefVO.getHostUuid())
                        .isExists();
                if (initiatorOccupiedByOtherHost) {
                    completion.fail(operr("initiatorName %s is occupied by other host, please regenerate initiator of host %s"
                            , blockPrimaryStorageHostRefVO.getInitiatorName(), blockPrimaryStorageHostRefVO.getHostUuid()));
                    return;
                }
                completion.success(returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}