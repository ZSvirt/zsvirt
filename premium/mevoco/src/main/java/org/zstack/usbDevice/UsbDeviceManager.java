package org.zstack.usbDevice;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.compute.vm.VmCapabilitiesExtensionPoint;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.RunInQueue;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.ha.HaHostDeviceExtensionPoint;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.ReportHostDeviceEventCmd;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmAddonExtensionPoint;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVmMsg;
import org.zstack.storage.migration.primary.APIPrimaryStorageMigrateVolumeMsg;
import org.zstack.storage.primary.local.APILocalStorageMigrateVolumeMsg;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by GuoYi on 10/21/17.
 */
public class UsbDeviceManager extends AbstractService implements KVMStartVmAddonExtensionPoint, GlobalApiMessageInterceptor,
        VmCapabilitiesExtensionPoint, VmInstanceMigrateExtensionPoint, HaHostDeviceExtensionPoint, PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(UsbDeviceManager.class);
    private Map<HypervisorType, UsbDeviceBackend> backends = new HashMap<>();

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private CloudBus bus;

    @Autowired
    private ThreadFacade thdf;

    @Autowired
    private RESTFacade restf;

    @Autowired
    private PluginRegistry pluginRgty;

    private void populateExtensions() {
        for (UsbDeviceBackend bkd : pluginRgty.getExtensionList(UsbDeviceBackend.class)) {
            UsbDeviceBackend old = backends.get(bkd.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format(
                        "duplicate UsbDeviceBackend[%s, %s] for type[%s]",
                        old.getClass(), bkd.getClass(), bkd.getHypervisorType()
                ));
            }
            backends.put(bkd.getHypervisorType(), bkd);
        }
    }

    private UsbDeviceBackend getUsbDeviceBackendByVmUuid(String vmUuid) {
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        return backends.get(new HypervisorType(vm.getHypervisorType()));
    }

    private UsbDeviceBackend getUsbDeviceBackendByHostUuid(String hostUuid) {
        HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
        return backends.get(new HypervisorType(host.getHypervisorType()));
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIAttachUsbDeviceToVmMsg) {
            handle((APIAttachUsbDeviceToVmMsg) msg);
        } else if (msg instanceof APIDetachUsbDeviceFromVmMsg) {
            handle((APIDetachUsbDeviceFromVmMsg) msg);
        } else if (msg instanceof APIGetUsbDeviceCandidatesForAttachingVmMsg) {
            handle((APIGetUsbDeviceCandidatesForAttachingVmMsg) msg);
        } else if (msg instanceof APIUpdateUsbDeviceMsg) {
            handle((APIUpdateUsbDeviceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DetachUsbDeviceMsg) {
            handle((DetachUsbDeviceMsg) msg);
        } else if (msg instanceof SyncUsbDeviceMsg) {
            handle((SyncUsbDeviceMsg) msg);
        } else if (msg instanceof CheckAndReserveUsbDeviceMsg) {
            handle((CheckAndReserveUsbDeviceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected RunInQueue operationUsbDeviceInQueue(String usbDeviceUuid) {
        return new RunInQueue(usbDeviceUuid, thdf, 3);
    }

    private void handle(APIAttachUsbDeviceToVmMsg msg) {
        APIAttachUsbDeviceToVmEvent evt = new APIAttachUsbDeviceToVmEvent(msg.getId());

        operationUsbDeviceInQueue(msg.getUsbDeviceUuid())
                .asyncBackup(msg)
                .name(String.format("attach-usb-device-to-vm-%s", msg.getVmInstanceUuid()))
                .run((outer) -> doAttachUsbDeviceToVm(msg, new ReturnValueCompletion<UsbDeviceInventory>(outer) {
                    @Override
                    public void success(UsbDeviceInventory returnValue) {
                        evt.setInventory(returnValue);
                        bus.publish(evt);
                        outer.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        outer.next();
                    }
                }));
    }

    private void doAttachUsbDeviceToVm(APIAttachUsbDeviceToVmMsg msg, ReturnValueCompletion<UsbDeviceInventory> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("attach-usb-device-to-vm-%s", msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                UsbDeviceVO usb = dbf.findByUuid(msg.getUsbDeviceUuid(), UsbDeviceVO.class);
                UsbDeviceInventory usbInv = UsbDeviceInventory.valueOf(usb);

                if (msg.getVmInstanceUuid().equals(usb.getVmInstanceUuid())) {
                    throw new OperationFailureException(Platform.argerr(
                            "the usb device[uuid:%s] has already been attached to same vm[uuid:%s]",
                            msg.getUsbDeviceUuid(), usb.getVmInstanceUuid()
                    ));
                }

                if (usb.getVmInstanceUuid() != null) {
                    throw new OperationFailureException(Platform.argerr(
                            "the usb device[uuid:%s] has already been attached to another vm[uuid:%s]",
                            msg.getUsbDeviceUuid(), usb.getVmInstanceUuid()
                    ));
                }

                UsbDeviceBackend bkd = getUsbDeviceBackendByVmUuid(msg.getVmInstanceUuid());
                FlowChain fchain = FlowChainBuilder.newSimpleFlowChain();
                fchain.setName(String.format("attach-usb-device-%s-to-vm-%s", msg.getUsbDeviceUuid(), msg.getVmInstanceUuid()));
                fchain.then(new NoRollbackFlow() {
                    String __name__ = String.format("check-usb-device-%s-attachable-if-vm-%s-stopped", msg.getUsbDeviceUuid(), msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!vm.getState().equals(VmInstanceState.Stopped)) {
                            if (msg.getAttachType().equals(UsbAttachType.PassThrough.toString())
                                    && !usb.getHostUuid().equals(vm.getHostUuid())) {
                                trigger.fail(Platform.operr(
                                        "PassThrough only support use on vm running host"));
                                return;
                            }
                            trigger.next();
                            return;
                        }

                        if (msg.getAttachType().equals(UsbAttachType.Redirect.toString())) {
                            trigger.next();
                            return;
                        }

                        getUsbDeviceCandidatesForStoppedVm(msg.getVmInstanceUuid(), new ReturnValueCompletion<List<UsbDeviceInventory>>(trigger) {
                            @Override
                            public void success(List<UsbDeviceInventory> returnValue) {
                                if (!returnValue.stream().anyMatch(inv -> inv.getUuid().equals(msg.getUsbDeviceUuid()))) {
                                    trigger.fail(Platform.operr("cannot attach the usb device[uuid:%s] to vm[uuid:%s]," +
                                                    " possibly reasons include: the device is not enabled or had been attached to a vm," +
                                                    " or the device and the vm are not on same host.",
                                            msg.getUsbDeviceUuid(), msg.getVmInstanceUuid()));
                                } else {
                                    trigger.next();
                                }
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = "check-number-of-usb-devices-already-attached-to-vm";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        // attach at most 1 USB 1.0 devices to one VM
                        ErrorCode errorCode = UsbDeviceUtils.checkNumberOfUsbDeviceAlreadyAttachedTOVm(usb.getUsbVersion(), vm.getUuid());
                        if (errorCode != null) {
                            trigger.fail(errorCode);
                            return;
                        }

                        trigger.next();
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = String.format("start-usb-redirect-server-in-host-%s", usb.getUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (!msg.getAttachType().equals(UsbAttachType.Redirect.toString())) {
                            trigger.next();
                            return;
                        }
                        bkd.startUsbRedirectServer(usbInv, null, new ReturnValueCompletion<String>(trigger) {
                            @Override
                            public void success(String port) {
                                updateUsbPortTag(usb.getUuid(), port);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                }).then(new NoRollbackFlow() {
                    String __name__ = String.format("attach-usb-device-%s-to-vm-%s-in-backend", msg.getUsbDeviceUuid(), msg.getVmInstanceUuid());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        bkd.attachUsbDeviceToVm(usbInv, msg.getVmInstanceUuid(), msg.getAttachType(), new Completion(trigger) {
                            @Override
                            public void success() {
                                usb.setVmInstanceUuid(msg.getVmInstanceUuid());
                                usb.setAttachType(msg.getAttachType());
                                dbf.update(usb);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                }).error(new FlowErrorHandler(chain) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        usbInv.setAttachType(msg.getAttachType());
                        bkd.closeRedirectPortIfNeeded(usbInv, new Completion(msg) {
                            @Override
                            public void success() {
                                completion.fail(errCode);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                completion.fail(errorCode);
                            }
                        });
                    }
                }).done(new FlowDoneHandler(chain) {
                    @Override
                    public void handle(Map data) {
                        completion.success(UsbDeviceInventory.valueOf(usb));
                    }
                }).Finally(new FlowFinallyHandler(chain) {
                    @Override
                    public void Finally() {
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });

    }

    private void cleanUsbSystemTag(String usbUuid) {
        if (UsbSystemTags.USB_REDIRECT_PORT.hasTag(usbUuid)) {
            UsbSystemTags.USB_REDIRECT_PORT.delete(usbUuid);
        }
    }

    private void handle(APIDetachUsbDeviceFromVmMsg msg) {
        APIDetachUsbDeviceFromVmEvent evt = new APIDetachUsbDeviceFromVmEvent(msg.getId());
        DetachUsbDeviceMsg localMsg = new DetachUsbDeviceMsg();
        localMsg.setUsbDeviceUuid(msg.getUsbDeviceUuid());
        bus.makeTargetServiceIdByResourceUuid(localMsg, UsbDeviceConstants.SERVICE_ID, msg.getUsbDeviceUuid());
        bus.send(localMsg, new CloudBusCallBack(evt) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    bus.publish(evt);
                } else {
                    evt.setError(reply.getError());
                    bus.publish(evt);
                }
            }
        });
    }

    private void detachUsbDevice(String usbDeviceUuid, Completion completion) {
        UsbDeviceVO usb = dbf.findByUuid(usbDeviceUuid, UsbDeviceVO.class);
        UsbDeviceInventory usbInv = UsbDeviceInventory.valueOf(usb);
        UsbDeviceBackend bkd = getUsbDeviceBackendByVmUuid(usb.getVmInstanceUuid());

        bkd.detachUsbDeviceFromVm(usbInv, usb.getVmInstanceUuid(), new Completion(completion) {
            @Override
            public void success() {
                usb.setVmInstanceUuid(null);
                usb.setAttachType(null);
                dbf.update(usb);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void handle(APIGetUsbDeviceCandidatesForAttachingVmMsg msg) {
        final APIGetUsbDeviceCandidatesForAttachingVmReply reply = new APIGetUsbDeviceCandidatesForAttachingVmReply();
        VmInstanceVO vm = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);

        if (msg.getAttachType().equals(UsbAttachType.Redirect.toString())) {
            List<String> hosts = Q.New(HostVO.class)
                    .select(HostVO_.uuid)
                    .eq(HostVO_.zoneUuid, vm.getZoneUuid())
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .listValues();
            List<UsbDeviceVO> usbs = Q.New(UsbDeviceVO.class)
                    .isNull(UsbDeviceVO_.vmInstanceUuid)
                    .in(UsbDeviceVO_.hostUuid, hosts)
                    .eq(UsbDeviceVO_.state, UsbDeviceState.Enabled)
                    .list();
            reply.setInventories(UsbDeviceInventory.valueOf(usbs));
            bus.reply(msg, reply);
            return;
        }

        if (vm.getState().equals(VmInstanceState.Running)) {
            List<UsbDeviceVO> usbs = getEnabledUnattachedUsbDevices(Arrays.asList(vm.getHostUuid()));
            reply.setInventories(UsbDeviceInventory.valueOf(usbs));
            bus.reply(msg, reply);
        } else if (vm.getState().equals(VmInstanceState.Stopped)) {
            getUsbDeviceCandidatesForStoppedVm(vm.getUuid(), new ReturnValueCompletion<List<UsbDeviceInventory>>(msg) {
                @Override
                public void success(List<UsbDeviceInventory> returnValue) {
                    reply.setInventories(returnValue);
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }


    private void updateUsbPortTag(String uuid, String port) {
        if (!UsbSystemTags.USB_REDIRECT_PORT.hasTag(uuid)) {
            SystemTagCreator creator = UsbSystemTags.USB_REDIRECT_PORT.newSystemTagCreator(uuid);
            creator.setTagByTokens(map(e(UsbSystemTags.USB_REDIRECT_PORT_TOKEN, port.split("\\.")[0])));
            creator.inherent = false;
            creator.ignoreIfExisting = true;
            creator.create();
            return;
        }
        UsbSystemTags.USB_REDIRECT_PORT.update(uuid, UsbSystemTags.USB_REDIRECT_PORT.instantiateTag(
                map(e(UsbSystemTags.USB_REDIRECT_PORT_TOKEN, port.split("\\.")[0]))
        ));
    }

    private void getUsbDeviceCandidatesForStoppedVm(String vmInstanceUuid, ReturnValueCompletion<List<UsbDeviceInventory>> completion) {
        List<UsbDeviceVO> usbs = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, vmInstanceUuid)
                .list();
        if (usbs != null && !usbs.isEmpty()) {
            List<UsbDeviceVO> candidates = getEnabledUnattachedUsbDevices(Arrays.asList(usbs.get(0).getHostUuid()));
            logger.debug(String.format("get usb device candidates[%s] for vm instance[uuid:%s]",
                    candidates.stream().map(usb -> usb.getUuid()).collect(Collectors.toList()), vmInstanceUuid));
            completion.success(UsbDeviceInventory.valueOf(candidates));
            return;
        }

        GetVmStartingCandidateClustersHostsMsg gmsg = new GetVmStartingCandidateClustersHostsMsg();
        gmsg.setUuid(vmInstanceUuid);
        bus.makeLocalServiceId(gmsg, VmInstanceConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    GetVmStartingCandidateClustersHostsReply greply = reply.castReply();
                    List<UsbDeviceVO> candidates = getEnabledUnattachedUsbDevices(
                            greply.getHostInventories().stream().map(host -> host.getUuid()).collect(Collectors.toList())
                    );
                    logger.debug(String.format("get usb device candidates[%s] for vm instance[uuid:%s]",
                            candidates.stream().map(usb -> usb.getUuid()).collect(Collectors.toList()), vmInstanceUuid));
                    completion.success(UsbDeviceInventory.valueOf(candidates));
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    private List<UsbDeviceVO> getEnabledUnattachedUsbDevices(List<String> hostUuids) {
        return Q.New(UsbDeviceVO.class)
                .in(UsbDeviceVO_.hostUuid, hostUuids)
                .isNull(UsbDeviceVO_.vmInstanceUuid)
                .eq(UsbDeviceVO_.state, UsbDeviceState.Enabled)
                .list();
    }


    private void handle(APIUpdateUsbDeviceMsg msg) {
        APIUpdateUsbDeviceEvent event = new APIUpdateUsbDeviceEvent(msg.getId());
        UsbDeviceVO vo = dbf.findByUuid(msg.getUuid(), UsbDeviceVO.class);

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        if (msg.getState() != null) {
            vo.setState(UsbDeviceState.valueOf(msg.getState()));
        }

        dbf.updateAndRefresh(vo);
        event.setInventory(UsbDeviceInventory.valueOf(vo));
        bus.publish(event);
    }

    private void handle(DetachUsbDeviceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                String vmUuid = Q.New(UsbDeviceVO.class)
                        .select(UsbDeviceVO_.vmInstanceUuid)
                        .eq(UsbDeviceVO_.uuid, msg.getUsbDeviceUuid())
                        .findValue();
                return String.format("detach-usb-device-from-vm-%s", vmUuid);
            }

            @Override
            public void run(SyncTaskChain chain) {
                DetachUsbDeviceReply reply = new DetachUsbDeviceReply();
                detachUsbDevice(msg.getUsbDeviceUuid(), new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
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

    private void handle(CheckAndReserveUsbDeviceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                CheckAndReserveUsbDeviceReply reply = new CheckAndReserveUsbDeviceReply();
                checkAndReserveUsbDevice(msg, new ReturnValueCompletion<List<String>>(chain) {
                    @Override
                    public void success(List<String> usbUuids) {
                        reply.setRevertUsbUuids(usbUuids);
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return "check-and-reserve-usb-device";
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void checkAndReserveUsbDevice(CheckAndReserveUsbDeviceMsg msg, ReturnValueCompletion<List<String>> completion) {
        List<Map<String, String>> tokens = VmSystemTags.VM_ATTACH_USB.getTokensOfTagsByResourceUuid(msg.getVmUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> usbUuids = new ArrayList<>();
                for(Map<String, String> token : tokens) {
                    String usbDeviceUuid = token.get(VmSystemTags.USBDEVICE_UUID_TOKEN);

                    UsbDeviceVO deviceVO = q(UsbDeviceVO.class).eq(UsbDeviceVO_.uuid, usbDeviceUuid).find();

                    if (!StringUtils.isEmpty(deviceVO.getVmInstanceUuid())) {
                        ErrorCode errorCode = operr("usb is already bound to vm[uuid:%s] and cannot be bound to other vm",
                                deviceVO.getVmInstanceUuid());
                        sql(UsbDeviceVO.class)
                                .eq(UsbDeviceVO_.vmInstanceUuid, msg.getVmUuid())
                                .set(UsbDeviceVO_.vmInstanceUuid, null)
                                .set(UsbDeviceVO_.attachType, null)
                                .update();

                        completion.fail(errorCode);
                        return;
                    }

                    ErrorCode errorCode = UsbDeviceUtils.checkNumberOfUsbDeviceAlreadyAttachedTOVm(deviceVO.getUsbVersion(), msg.getVmUuid());

                    if (errorCode != null) {
                        sql(UsbDeviceVO.class)
                                .eq(UsbDeviceVO_.vmInstanceUuid, msg.getVmUuid())
                                .set(UsbDeviceVO_.vmInstanceUuid, null)
                                .set(UsbDeviceVO_.attachType, null)
                                .update();
                        completion.fail(errorCode);
                        return;
                    }
                    deviceVO.setVmInstanceUuid(msg.getVmUuid());
                    deviceVO.setAttachType(token.get(VmSystemTags.usbDevice_attach_type_token));
                    merge(deviceVO);
                    usbUuids.add(deviceVO.getUuid());
                }

                VmSystemTags.VM_ATTACH_USB.delete(msg.getVmUuid());
                completion.success(usbUuids);

            }
        }.execute();
    }

    private void handle(SyncUsbDeviceMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("sync-usb-device-info-from-host-%s", msg.getHostUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                UsbDeviceBackend bkd = getUsbDeviceBackendByHostUuid(msg.getHostUuid());
                if (bkd == null) {
                    logger.debug(String.format("cannot get UsbDeviceBackend for host[uuid:%s]", msg.getHostUuid()));
                    chain.next();
                    return;
                }

                bkd.syncUsbDeviceFromHost(msg.getHostUuid(), new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        pluginRgty.getExtensionList(SyncUsbDeviceExtensionPoint.class)
                                .forEach(it -> it.afterSyncUsbDevice(msg.getHostUuid()));
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });

        SyncUsbDeviceReply reply = new SyncUsbDeviceReply();
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(UsbDeviceConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(KVMConstant.KVM_HOST_REPORT_DEVICE_EVENT_PATH, ReportHostDeviceEventCmd.class, cmd -> {
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                public String getSyncSignature() {
                    return String.format("handle-device-event-of-host-%s", cmd.hostUuid);
                }

                @Override
                public void run(SyncTaskChain chain) {
                    SyncUsbDeviceMsg msg = new SyncUsbDeviceMsg();
                    msg.setHostUuid(cmd.hostUuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, UsbDeviceConstants.SERVICE_ID, cmd.hostUuid);
                    bus.send(msg, new CloudBusCallBack(chain) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                logger.debug(String.format("synced usb device events of host %s", cmd.hostUuid));
                            } else {
                                logger.warn(String.format("failed to sync usb device events of host %s", cmd.hostUuid));
                            }
                            chain.next();
                        }
                    });
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });

            return null;
        });

        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(APIMigrateVmMsg.class,
                APIPrimaryStorageMigrateVolumeMsg.class,
                APIPrimaryStorageMigrateVmMsg.class,
                APIStartVmInstanceMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            validate((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVolumeMsg) {
            validate((APIPrimaryStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APIPrimaryStorageMigrateVmMsg) {
            validate((APIPrimaryStorageMigrateVmMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            validate((APIStartVmInstanceMsg) msg);
        }
        return msg;
    }

    private boolean checkUsbRedirectHostState(String vmUuid) {
        List<String> hostUuids = Q.New(UsbDeviceVO.class)
                .select(UsbDeviceVO_.hostUuid)
                .eq(UsbDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                .listValues();
        if (CollectionUtils.isEmpty(hostUuids)) {
            return true;
        }
        if (Q.New(HostVO.class)
                .notEq(HostVO_.status, HostStatus.Connected)
                .in(HostVO_.uuid, hostUuids)
                .isExists()) {
            logger.warn(String.format("vm[%s] cannot start because usb redirect host is not connected", vmUuid));
            return false;
        }
        return true;
    }

    private void validate(APIStartVmInstanceMsg msg) {
        if (!checkUsbRedirectHostState(msg.getUuid())) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "vm[%s] cannot start because usb redirect host is not connected",
                    msg.getVmInstanceUuid()));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIMigrateVmMsg msg) {
        boolean usbAttached = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.PassThrough.toString())
                .isExists();
        if (usbAttached) {
            throw new ApiMessageInterceptionException(Platform.operr(
                    "cannot migrate vm[uuid:%s] because there are usb devices attached by passthrough",
                    msg.getVmInstanceUuid()
            ));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APILocalStorageMigrateVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume != null && volume.getType() == VolumeType.Root) {
            boolean usbAttached = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, volume.getVmInstanceUuid())
                    .isExists();

            if (usbAttached) {
                throw new ApiMessageInterceptionException(Platform.operr(
                        "cannot migrate root volume[uuid:%s] because there are usb devices attached",
                        msg.getVolumeUuid()
                ));
            }
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIPrimaryStorageMigrateVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume != null && volume.getType() == VolumeType.Root) {
            boolean usbAttached = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, volume.getVmInstanceUuid())
                    .isExists();

            if (usbAttached) {
                throw new ApiMessageInterceptionException(Platform.operr(
                        "cannot migrate root volume[uuid:%s] because there are usb devices attached",
                        msg.getVolumeUuid()
                ));
            }
        }
    }


    @Transactional(readOnly = true)
    private void validate(APIPrimaryStorageMigrateVmMsg msg) {
        if (msg.getVmInstanceUuid() != null) {
            boolean pciAttached = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                    .eq(UsbDeviceVO_.attachType, UsbAttachType.PassThrough.toString())
                    .isExists();

            if (pciAttached) {
                throw new ApiMessageInterceptionException(Platform.operr(
                        "cannot migrate vm[uuid:%s] because there are usb devices attached",
                        msg.getVmInstanceUuid()
                ));
            }
        }
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

        List<UsbDeviceVO> usbs = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                .list();
        List<String> usbDevices = new ArrayList<>();
        for (UsbDeviceVO usb : usbs) {
            String ip = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, usb.getHostUuid()).findValue();
            String port = "-1";
            if (usb.getAttachType().equals(UsbAttachType.Redirect.toString())) {
                port = UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(
                        usb.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN);
            }
            String usbDevice = String.format("%s:%s:%s:%s:%s:%s:%s:%s", usb.getBusNum(), usb.getDevNum(), usb.getIdVendor(), usb.getIdProduct(), usb.getUsbVersion(), usb.getAttachType(), port, ip);
            usbDevices.add(usbDevice);
        }
        cmd.getAddons().put(UsbDeviceConstants.SERVICE_ID, usbDevices);
        logger.debug(String.format("put usb device %s to vm instance[uuid:%s]",
                usbs.stream().map(usb -> usb.getUuid()).collect(Collectors.toList()), spec.getVmInventory().getUuid()
        ));
    }

    @Override
    public void checkVmCapability(VmInstanceInventory inv, VmCapabilities capabilities) {
        if (!capabilities.isSupportLiveMigration()) {
            return;
        }

        if (Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, inv.getUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.PassThrough.toString())
                .isExists()) {
            capabilities.setSupportLiveMigration(false);
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        UsbDeviceBackend bkd = getUsbDeviceBackendByVmUuid(inv.getUuid());
        bkd.reloadRedirectUsb(inv, null);
    }

    @Override
    public boolean canDoVmHa(String vmUuid) {
        return checkUsbRedirectHostState(vmUuid);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {

    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.NewCreate) {
            completion.success();
            return;
        }
        List<UsbDeviceVO> usbDeviceVOS = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                .list();
        List<UsbDeviceVO> filterUsbDevices = usbDeviceVOS.stream()
                .filter(usb -> !UsbSystemTags.USB_REDIRECT_PORT.hasTag(usb.getUuid()))
                .collect(Collectors.toList());

        if (org.zstack.utils.CollectionUtils.isEmpty(filterUsbDevices)) {
            completion.success();
            return;
        }
        UsbDeviceBackend bkd = getUsbDeviceBackendByVmUuid(spec.getVmInventory().getUuid());

        new While<>(UsbDeviceInventory.valueOf(filterUsbDevices))
                .each((usb, compl) -> bkd.startUsbRedirectServer(usb, null, new ReturnValueCompletion<String>(compl) {
                    @Override
                    public void success(String port) {
                        updateUsbPortTag(usb.getUuid(), port);
                        compl.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        compl.addError(errorCode);
                        compl.allDone();

                    }
                })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.hasError()) {
                    completion.fail(multiErr(errorCodeList));
                    return;
                }
                completion.success();
            }
        });
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.NewCreate) {
            completion.success();
            return;
        }

        List<UsbDeviceVO> usbDeviceVOS = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                .list();

        List<UsbDeviceVO> filterUsbDevices = usbDeviceVOS.stream()
                .filter(usb -> !UsbSystemTags.USB_REDIRECT_PORT.hasTag(usb.getUuid()))
                .collect(Collectors.toList());

        if (org.zstack.utils.CollectionUtils.isEmpty(filterUsbDevices)) {
            completion.success();
            return;
        }
        
        UsbDeviceBackend bkd = getUsbDeviceBackendByVmUuid(spec.getVmInventory().getUuid());
        new While<>(UsbDeviceInventory.valueOf(filterUsbDevices))
                .each((usb, compl) -> bkd.closeRedirectPortIfNeeded(usb, new Completion(compl) {
                    @Override
                    public void success() {
                        compl.done();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        compl.addError(errorCode);

                    }
                })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.hasError()) {
                    completion.fail(multiErr(errorCodeList));
                    return;
                }
                completion.success();
            }
        });
    }
}
