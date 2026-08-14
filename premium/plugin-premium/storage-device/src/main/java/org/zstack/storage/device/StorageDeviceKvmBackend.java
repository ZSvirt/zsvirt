package org.zstack.storage.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SingleFlightTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storageDevice.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceType;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.*;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.storage.device.fibreChannel.FiberChannelLunStruct;
import org.zstack.storage.device.hba.HbaDeviceStruct;
import org.zstack.storage.device.iscsi.IscsiServerInventory;
import org.zstack.storage.device.iscsi.IscsiServerVO;
import org.zstack.storage.device.iscsi.IscsiTargetInventory;
import org.zstack.storage.device.localRaid.*;
import org.zstack.storage.device.multipath.DeviceTO;
import org.zstack.storage.device.nvme.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Create by weiwang at 2018/8/4
 */
public class StorageDeviceKvmBackend implements StorageDeviceBackend, KVMStartVmAddonExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(StorageDeviceKvmBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    ResourceConfigFacade rcf;
    @Autowired
    private ThreadFacade thdf;

    @Override
    public String getSupportHypervisorType() {
        return KVMConstant.KVM_HYPERVISOR_TYPE;
    }

    @Override
    public void loginIscsiServer(IscsiServerVO iscsiServerVO, HostInventory hostInventory, ReturnValueCompletion<IscsiServerInventory> completion) {
        StorageDeviceKvmCommands.IscsiLoginCmd cmd = new StorageDeviceKvmCommands.IscsiLoginCmd();
        cmd.setIscsiServerIp(iscsiServerVO.getIp());
        cmd.setIscsiServerPort(iscsiServerVO.getPort().toString());
        cmd.setIscsiChapUserName(iscsiServerVO.getChapUserName());
        cmd.setIscsiChapUserPassword(iscsiServerVO.getChapUserPassword());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.ISCSI_LOGIN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.IscsiLoginRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.IscsiLoginRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                IscsiServerInventory inventory = new IscsiServerInventory(iscsiServerVO);
                inventory.setIscsiTargets(IscsiTargetInventory.valueOf2(rsp.iscsiTargetStructList));
                completion.success(inventory);
            }
        });
    }

    @Override
    public void logoutIscsiServer(IscsiServerVO iscsiServerVO, HostInventory hostInventory, Completion completion) {
        StorageDeviceKvmCommands.IscsiLogoutCmd cmd = new StorageDeviceKvmCommands.IscsiLogoutCmd();
        cmd.setIscsiServerIp(iscsiServerVO.getIp());
        cmd.setIscsiServerPort(iscsiServerVO.getPort().toString());
        cmd.setIscsiChapUserName(iscsiServerVO.getChapUserName());
        cmd.setIscsiChapUserPassword(iscsiServerVO.getChapUserPassword());

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.ISCSI_LOGOUT_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.IscsiLogoutRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.IscsiLogoutRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void scanFcDevice(HostInventory hostInventory, boolean rescan, List<String> identifiers, ReturnValueCompletion<List<FiberChannelLunStruct>> completion) {
        StorageDeviceKvmCommands.FcScanCmd cmd = new StorageDeviceKvmCommands.FcScanCmd();
        cmd.rescan = rescan;
        cmd.identifiers = identifiers;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.FC_SCAN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.FcScanRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.FcScanRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success(rsp.fiberChannelLunStructs);
            }
        });
    }

    @Override
    public void scanNvmeDevice(HostInventory hostInventory, boolean rescan, List<String> identifiers, ReturnValueCompletion<List<NvmeLunStruct>> completion) {
        StorageDeviceKvmCommands.NvmeScanCmd cmd = new StorageDeviceKvmCommands.NvmeScanCmd();
        cmd.rescan = rescan;
        cmd.identifiers = identifiers;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.NVME_SCAN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.NvmeScanRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.NvmeScanRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success(rsp.nvmeLunStructs);
            }
        });
    }

    @Override
    public void scanHba(HostInventory hostInventory, ReturnValueCompletion<List<HbaDeviceStruct>> completion) {
        StorageDeviceKvmCommands.HbaScanCmd cmd = new StorageDeviceKvmCommands.HbaScanCmd();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.HBA_SCAN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.HbaScanRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.HbaScanRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("hba scan is error: %s", rsp.getError()));
                    return;
                }

                completion.success(rsp.hbaDeviceStructs);
            }
        });
    }

    @Override
    public void connectNvmeServer(NvmeServerVO nvmeServerVO, HostInventory hostInventory, ReturnValueCompletion<NvmeServerInventory> completion) {
        StorageDeviceKvmCommands.NvmeServerConnectCmd cmd = new StorageDeviceKvmCommands.NvmeServerConnectCmd();
        cmd.ip = nvmeServerVO.getIp();
        cmd.port = nvmeServerVO.getPort();
        cmd.transport = nvmeServerVO.getTransport();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.NVME_CONNECT_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.NvmeServerConnectRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.NvmeServerConnectRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                NvmeServerInventory nvmeServerInventory = new NvmeServerInventory(nvmeServerVO);
                nvmeServerInventory.setNvmeTargets(NvmeTargetInventory.valueOf2(rsp.nvmeLunStructs));
                completion.success(nvmeServerInventory);
            }
        });
    }

    @Override
    public void disconnectNvmeServer(NvmeServerVO nvmeServerVO, HostInventory hostInventory, Completion completion) {
        StorageDeviceKvmCommands.NvmeServerDisconnectCmd cmd = new StorageDeviceKvmCommands.NvmeServerDisconnectCmd();
        cmd.ip = nvmeServerVO.getIp();
        cmd.port = nvmeServerVO.getPort();
        cmd.transport = nvmeServerVO.getTransport();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.NVME_DISCONNECT_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.NvmeServerDisconnectRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.NvmeServerDisconnectRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void getPhysicalDriveSmartData(RaidPhysicalDriveInventory raidPhysicalDrive, RaidControllerInventory controller, ReturnValueCompletion<List<SmartDataStruct>> completion) {
        StorageDeviceKvmCommands.RaidPhysicalDriveSmartCmd cmd = new StorageDeviceKvmCommands.RaidPhysicalDriveSmartCmd();
        cmd.busNumber = controller.getAdapterNumber();
        cmd.deviceNumber = raidPhysicalDrive.getDeviceId();
        cmd.slotNumber = raidPhysicalDrive.getSlotNumber();
        cmd.wwn = raidPhysicalDrive.getWwn();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(controller.getHostUuid());
        msg.setPath(StorageDeviceKvmCommands.RAID_SMART_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.RaidPhysicalDriveSmartRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.RaidPhysicalDriveSmartRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success(rsp.smartDataStructs);
            }
        });
    }

    @Override
    public void localRaidSelfTest(RaidPhysicalDriveInventory raidPhysicalDrive, RaidControllerInventory controller, ReturnValueCompletion<SelfTestLocalRaidReply> completion) {
        StorageDeviceKvmCommands.RaidPhysicalDriveSmartTestCmd cmd = new StorageDeviceKvmCommands.RaidPhysicalDriveSmartTestCmd();
        cmd.busNumber = controller.getAdapterNumber();
        cmd.deviceNumber = raidPhysicalDrive.getDeviceId();
        cmd.slotNumber = raidPhysicalDrive.getSlotNumber();
        cmd.wwn = raidPhysicalDrive.getWwn();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(controller.getHostUuid());
        msg.setPath(StorageDeviceKvmCommands.RAID_SELF_TEST_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.RaidPhysicalDriveSmartTestRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.RaidPhysicalDriveSmartTestRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                SelfTestLocalRaidReply reply2 = new SelfTestLocalRaidReply();
                reply2.setResult(rsp.result);
                completion.success(reply2);
            }
        });
    }

    @Override
    public void getHostMultipathTopology(HostInventory hostInventory, List<String> wwids, ReturnValueCompletion<Map<String, List<DeviceTO>>> completion) {
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        StorageDeviceKvmCommands.GetMultipathTopologyCmd cmd = new StorageDeviceKvmCommands.GetMultipathTopologyCmd();
        cmd.wwids = wwids;
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.GET_MULTIPATH_TOPOLOGY_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.GetMultipathTopologyRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.GetMultipathTopologyRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                completion.success(rsp.devices);
            }
        });
    }

    @Override
    public void locateLocalRaidPhysicalDrive(RaidPhysicalDriveInventory raidPhysicalDrive, RaidControllerInventory controller, boolean locate, Completion completion) {
        StorageDeviceKvmCommands.RaidPhysicalDriveLocateCmd cmd = new StorageDeviceKvmCommands.RaidPhysicalDriveLocateCmd();
        cmd.raidControllerNumber = controller.getAdapterNumber();
        cmd.enclosureDeviceID = raidPhysicalDrive.getEnclosureDeviceId();
        cmd.slotNumber = raidPhysicalDrive.getSlotNumber();
        cmd.wwn = raidPhysicalDrive.getWwn();
        cmd.deviceNumber = raidPhysicalDrive.getDeviceId();
        cmd.busNumber = controller.getAdapterNumber();
        cmd.locate = locate;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(controller.getHostUuid());
        msg.setPath(StorageDeviceKvmCommands.RAID_LOCATE_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.RaidPhysicalDriveSmartRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.RaidPhysicalDriveSmartRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void scanLocalRaid(HostInventory hostInventory, ReturnValueCompletion<List<RaidPhysicalDriveStruct>> completion) {
        StorageDeviceKvmCommands.RaidScanCmd cmd = new StorageDeviceKvmCommands.RaidScanCmd();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostInventory.getUuid());
        msg.setPath(StorageDeviceKvmCommands.RAID_SCAN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.RaidScanRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.RaidScanRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success(rsp.raidPhysicalDriveStructs);
            }
        });
    }

    @Override
    public void enableMultipathDevice(HostInventory hostInventory, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {

            @Override
            public void run(SyncTaskChain chain) {
                StorageDeviceKvmCommands.MultipathEnableCmd cmd = new StorageDeviceKvmCommands.MultipathEnableCmd();

                String blacklist = rcf.getResourceConfigValue(StorageDeviceGlobalConfig.MULTIPATH_BLACKLIST, hostInventory.getClusterUuid(), String.class);
                if (!"".equals(blacklist)) {
                    cmd.blacklist = JSONObjectUtil.toObject(blacklist, ArrayList.class);
                }

                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(cmd);
                msg.setHostUuid(hostInventory.getUuid());
                msg.setPath(StorageDeviceKvmCommands.MULTIPATH_ENABLE_PATH);
                msg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        chain.next();
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                        StorageDeviceKvmCommands.MultipathEnableRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.MultipathEnableRsp.class);
                        if (!rsp.isSuccess()) {
                            logger.warn(String.format("enable multipath failed: %s", rsp.getError()));
                            completion.fail(operr("%s", rsp.getError()));
                            return;
                        }
                        completion.success();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("enable-multipath-on-host-%s", hostInventory.getUuid());
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    @Override
    public void disableMultipathDevice(HostInventory hostInventory, Completion completion) {
        thdf.singleFlightSubmit(new SingleFlightTask(completion)
            .setSyncSignature(String.format("disable-multipath-on-host-%s", hostInventory.getUuid()))
            .run(comp -> {
                KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                msg.setCommand(new KVMAgentCommands.AgentCommand());
                msg.setHostUuid(hostInventory.getUuid());
                msg.setPath(StorageDeviceKvmCommands.MULTIPATH_DISABLE_PATH);
                msg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            comp.fail(reply.getError());
                            return;
                        }

                        KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                        if (!reply1.isSuccess()) {
                            comp.fail(reply1.getError());
                            return;
                        }

                        KVMAgentCommands.AgentResponse rsp = reply1.toResponse(KVMAgentCommands.AgentResponse.class);
                        if (!rsp.isSuccess()) {
                            comp.fail(operr("%s", rsp.getError()));
                            return;
                        }
                        comp.success(null);
                    }
                });
            }).done(result -> {
                if (!result.isSuccess()) {
                    logger.warn(String.format("disable multipath failed: %s", result.getErrorCode()));
                    completion.fail(result.getErrorCode());
                    return;
                }
                completion.success();
            })
        );
    }

    private String getStorageDeviceInstallPath(String wwid, String type, Boolean isAttachMultipath) {
        if (type.equals(StorageDeviceConstants.MULTIPATH) && isAttachMultipath) {
            return String.format("/dev/disk/by-id/dm-uuid-mpath-%s", wwid);
        } else if (type.equals(StorageDeviceConstants.MULTIPATH) || type.equals(StorageDeviceConstants.DISK)) {
            return String.format("/dev/disk/by-id/scsi-%s", wwid);
        } else {
            return String.format("/dev/disk/by-id/%s", wwid);
        }
    }

    @Override
    public void attachScsiLunToVm(ScsiLunVO lunVO, VmInstanceVO vmInstanceVO, ScsiLunVmInstanceRefVO refVO, Completion completion) {
        StorageDeviceKvmCommands.AttachScsiLunToVmCmd cmd = new StorageDeviceKvmCommands.AttachScsiLunToVmCmd();
        cmd.vmInstanceUuid = vmInstanceVO.getUuid();
        cmd.wwid = lunVO.getWwid();
        cmd.multipath = refVO.isAttachMultipath();

        VolumeTO volume = new VolumeTO();
        volume.setVolumeUuid(lunVO.getUuid());
        volume.setDeviceType(VolumeTO.SCSILUN);
        volume.setDeviceId(refVO.getDeviceId());
        volume.setInstallPath(getStorageDeviceInstallPath(lunVO.getWwid(), lunVO.getType(), refVO.isAttachMultipath()));
        cmd.volume = volume;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(vmInstanceVO.getHostUuid());
        msg.setPath(StorageDeviceKvmCommands.ATTACH_SCSI_LUN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.AttachScsiLunToVmRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.AttachScsiLunToVmRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void detachScsiLunFromHost(ScsiLunVO lunVO, String hostUuid, Completion completion) {
        StorageDeviceKvmCommands.DetachScsiLunFromHostCmd cmd = new StorageDeviceKvmCommands.DetachScsiLunFromHostCmd();
        cmd.wwid = lunVO.getWwid();

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(StorageDeviceKvmCommands.DETACH_SCSI_DEV_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                KVMAgentCommands.AgentResponse rsp = reply1.toResponse(KVMAgentCommands.AgentResponse.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }

                completion.success();
            }
        });
    }

    @Override
    public void detachScsiLunFromVm(ScsiLunVO lunVO, VmInstanceVO vmInstanceVO, ScsiLunVmInstanceRefVO refVO, Completion completion) {
        StorageDeviceKvmCommands.DetachScsiLunFromVmCmd cmd = new StorageDeviceKvmCommands.DetachScsiLunFromVmCmd();
        cmd.vmInstanceUuid = vmInstanceVO.getUuid();
        cmd.wwid = lunVO.getWwid();

        VolumeTO volume = new VolumeTO();
        volume.setVolumeUuid(lunVO.getUuid());
        volume.setDeviceType(VolumeTO.SCSILUN);
        volume.setDeviceId(refVO.getDeviceId());
        volume.setInstallPath(getStorageDeviceInstallPath(lunVO.getWwid(), lunVO.getType(), refVO.isAttachMultipath()));
        cmd.volume = volume;

        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(vmInstanceVO.getHostUuid());
        msg.setPath(StorageDeviceKvmCommands.DETACH_SCSI_LUN_PATH);
        msg.setNoStatusCheck(true);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, msg.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                KVMHostAsyncHttpCallReply reply1 = reply.castReply();
                if (!reply1.isSuccess()) {
                    completion.fail(reply1.getError());
                    return;
                }

                StorageDeviceKvmCommands.DetachScsiLunFromVmRsp rsp = reply1.toResponse(StorageDeviceKvmCommands.DetachScsiLunFromVmRsp.class);
                if (!rsp.isSuccess()) {
                    completion.fail(operr("%s", rsp.getError()));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public VmInstanceType getVmTypeForAddonExtension() {
        return VmInstanceType.valueOf(VmInstanceConstant.USER_VM_TYPE);
    }

    @Override
    public void addAddon(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (spec.getVmInventory().getType().equals(ApplianceVmConstant.APPLIANCE_VM_TYPE)) {
            return;
        }

        List<ScsiLunVmInstanceRefVO> vos = Q.New(ScsiLunVmInstanceRefVO.class).eq(ScsiLunVmInstanceRefVO_.vmInstanceUuid, spec.getVmInventory().getUuid()).list();
        if (vos == null || vos.isEmpty()) {
            return;
        }

        List<VolumeTO> volumes = new ArrayList<>();
        for (ScsiLunVmInstanceRefVO refVO : vos) {
            VolumeTO volume = new VolumeTO();
            volume.setVolumeUuid(refVO.getScsiLunUuid());
            volume.setDeviceType(VolumeTO.SCSILUN);
            volume.setDeviceId(refVO.getDeviceId());

            ScsiLunVO scsiLunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, refVO.getScsiLunUuid()).find();

            volume.setInstallPath(getStorageDeviceInstallPath(scsiLunVO.getWwid(), scsiLunVO.getType(), refVO.isAttachMultipath()));
            volumes.add(volume);
        }

        cmd.getAddons().put(StorageDeviceConstants.SERVICE_ID, volumes);
        logger.debug(String.format("put scsi lun %s to vm instance[uuid:%s]", volumes.stream().map(VolumeTO::getInstallPath).collect(Collectors.toList()), spec.getVmInventory().getUuid()));
    }
}
