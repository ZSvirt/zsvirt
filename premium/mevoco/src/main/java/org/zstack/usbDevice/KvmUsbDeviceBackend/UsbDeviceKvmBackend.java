package org.zstack.usbDevice.KvmUsbDeviceBackend;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.kvm.*;
import org.zstack.usbDevice.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by GuoYi on 10/21/17.
 */
public class UsbDeviceKvmBackend implements UsbDeviceBackend, KVMHostConnectExtensionPoint, HostConnectionReestablishExtensionPoint, HostAfterConnectedExtensionPoint {
    public static final String GET_USB_DEVICES_PATH = "/host/usbdevice/get";
    public static final String KVM_ATTACH_USB_DEVICE_PATH = "/vm/usbdevice/attach";
    public static final String KVM_DETACH_USB_DEVICE_PATH = "/vm/usbdevice/detach";
    public static final String HOST_START_USB_REDIRECT_PATH = "/host/usbredirect/start";
    public static final String HOST_STOP_USB_REDIRECT_PATH = "/host/usbredirect/stop";
    public static final String RELOAD_USB_REDIRECT_PATH = "/vm/usbdevice/reload";
    public static final String CHECK_USB_REDIRECT_PORT = "/host/usbredirect/check";
    private static final List<String> USB_DEVICE_CLASS_BLACKLIST = Arrays.asList("Hub");
    private static CLogger logger = Utils.getLogger(UsbDeviceKvmBackend.class);
    private static final long MAX_USB_SERVER_PORT = 64;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApiTimeoutManager apiTimeoutManager;

    @Override
    public void connectionReestablished(HostInventory inv) throws HostException {
        FutureCompletion completion = new FutureCompletion(null);
        SyncUsbDeviceMsg msg = new SyncUsbDeviceMsg();
        msg.setHostUuid(inv.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, UsbDeviceConstants.SERVICE_ID, inv.getUuid());
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
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Override
    public HypervisorType getHypervisorTypeForReestablishExtensionPoint() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public Flow createKvmHostConnectingFlow(final KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            String __name__ = "sync-usb-devices";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                String hostUuid = context.getInventory().getUuid();
                SyncUsbDeviceMsg msg = new SyncUsbDeviceMsg();
                msg.setHostUuid(hostUuid);
                bus.makeTargetServiceIdByResourceUuid(msg, UsbDeviceConstants.SERVICE_ID, hostUuid);
                bus.send(msg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        };
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.valueOf(KVMConstant.KVM_HYPERVISOR_TYPE);
    }

    @Override
    public void attachUsbDeviceToVm(UsbDeviceInventory usbInv, String vmUuid, String attachType, Completion completion) {
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Running) {
            logger.debug(String.format("attached usb device[uuid:%s] to a stopped vm instance[uuid:%s]",
                    usbInv.getUuid(), vmUuid));
            completion.success();
            return;
        }

        // vm is still running
        KvmAttachUsbDeviceCmd cmd = new KvmAttachUsbDeviceCmd();
        cmd.setBusNum(usbInv.getBusNum());
        cmd.setDevNum(usbInv.getDevNum());
        cmd.setIdVendor(usbInv.getIdVendor());
        cmd.setIdProduct(usbInv.getIdProduct());
        cmd.setUsbVersion(usbInv.getUsbVersion());
        cmd.setHostUuid(vm.getHostUuid());
        cmd.setAttachType(attachType);
        cmd.setVmUuid(vmUuid);
        if (UsbSystemTags.USB_REDIRECT_PORT.hasTag(usbInv.getUuid())) {
            String ip = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, usbInv.getHostUuid()).findValue();
            cmd.setPort(UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(usbInv.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN));
            cmd.setIp(ip);
        }
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(vm.getHostUuid());
        msg.setPath(KVM_ATTACH_USB_DEVICE_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    KvmAttachUsbDeviceRsp rsp = rly.toResponse(KvmAttachUsbDeviceRsp.class);
                    if (rsp.isSuccess()) {
                        completion.success();
                    } else {
                        completion.fail(Platform.operr("%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }


    public void closeRedirectPortIfNeeded(UsbDeviceInventory usbInv, Completion completion) {
        if (!usbInv.getAttachType().equals(UsbAttachType.Redirect.toString()) ||
                !UsbSystemTags.USB_REDIRECT_PORT.hasTag(usbInv.getUuid())) {
            completion.success();
            return;
        }

        HostVO host = dbf.findByUuid(usbInv.getHostUuid(), HostVO.class);
        if (!host.getStatus().equals(HostStatus.Connected)) {
            UsbSystemTags.USB_REDIRECT_PORT.delete(usbInv.getUuid());
            completion.success();
        } else {
            StopUsbServerCmd cmd = new StopUsbServerCmd();
            cmd.port = UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(usbInv.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN);
            cmd.busNum = usbInv.getBusNum();
            cmd.devNum = usbInv.getDevNum();
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setHostUuid(usbInv.getHostUuid());
            msg.setPath(HOST_STOP_USB_REDIRECT_PATH);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, usbInv.getHostUuid());
            bus.send(msg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (reply.isSuccess()) {
                        KVMHostAsyncHttpCallReply rly = reply.castReply();
                        KvmDetachUsbDeviceRsp rsp = rly.toResponse(KvmDetachUsbDeviceRsp.class);
                        if (rsp.isSuccess()) {
                            UsbSystemTags.USB_REDIRECT_PORT.delete(usbInv.getUuid());
                            completion.success();
                        } else {
                            completion.fail(Platform.operr("%s", rsp.getError()));
                        }
                    } else {
                        completion.fail(reply.getError());
                    }
                }
            });
        }
    }

    @Override
    public void detachUsbDeviceFromVm(UsbDeviceInventory usbInv, String vmUuid, Completion completion) {
        VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
        if (vm.getState() != VmInstanceState.Running) {
            closeRedirectPortIfNeeded(usbInv, completion);
            logger.debug(String.format("detach usb device[uuid:%s] from a stopped vm instance[uuid:%s]",
                    usbInv.getUuid(), vmUuid));
            return;
        }

        // vm is still running
        KvmDetachUsbDeviceCmd cmd = new KvmDetachUsbDeviceCmd();
        cmd.setBusNum(usbInv.getBusNum());
        cmd.setDevNum(usbInv.getDevNum());
        cmd.setIdVendor(usbInv.getIdVendor());
        cmd.setIdProduct(usbInv.getIdProduct());
        cmd.setVmUuid(vmUuid);
        cmd.setAttachType(usbInv.getAttachType());
        if (usbInv.getAttachType().equals(UsbAttachType.Redirect.toString())) {
            String ip = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, usbInv.getHostUuid()).findValue();
            cmd.setPort(UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(usbInv.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN));
            cmd.setIp(ip);
        }
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(vm.getHostUuid());
        msg.setPath(KVM_DETACH_USB_DEVICE_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    KvmDetachUsbDeviceRsp rsp = rly.toResponse(KvmDetachUsbDeviceRsp.class);
                    if (rsp.isSuccess()) {
                        closeRedirectPortIfNeeded(usbInv, completion);
                    } else {
                        completion.fail(Platform.operr("operation error, because:%s", rsp.getError()));
                    }
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void syncUsbDeviceFromHost(final String hostUuid, NoErrorCompletion completion) {
        syncUsbDeviceFromHost(hostUuid, true, completion);
    }

    @Override
    public void startUsbRedirectServer(UsbDeviceInventory usbInv, String port, ReturnValueCompletion<String> completion) {
        if (Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.hostUuid, usbInv.getHostUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                .count() > MAX_USB_SERVER_PORT) {
            completion.fail(Platform.operr("host[%s] has started more than 64 usb redirect port", usbInv.getHostUuid()));
            return;
        }
        HostVO host = dbf.findByUuid(usbInv.getHostUuid(), HostVO.class);
        if (host.getStatus() != HostStatus.Connected) {
            completion.fail(Platform.operr("unable to start usb server on host[%s], because host is not connected", host.getUuid()));
            return;
        }
        StartUsbServerCmd cmd = new StartUsbServerCmd();
        cmd.busNum = usbInv.getBusNum();
        cmd.devNum = usbInv.getDevNum();
        cmd.idProduct = usbInv.getIdProduct();
        cmd.idVendor = usbInv.getIdVendor();
        cmd.port = port;
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(usbInv.getHostUuid());
        msg.setPath(HOST_START_USB_REDIRECT_PATH);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, usbInv.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                KVMHostAsyncHttpCallReply rly = reply.castReply();
                StartUsbServerRsp rsp = rly.toResponse(StartUsbServerRsp.class);
                if (rsp.isSuccess()) {
                    completion.success(rsp.port);
                } else {
                    completion.fail(Platform.operr("failed to start usbredirect server from host[uuid:%s]", usbInv.getHostUuid()));
                }
            }
        });
    }

    @Override
    public void reloadRedirectUsb(VmInstanceInventory inv, String hostUuid) {
        if (!inv.getState().equals(VmInstanceState.Running.toString())) {
            return;
        }
        List<UsbDeviceVO> vos;
        if (hostUuid == null) {
            vos = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, inv.getUuid())
                    .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                    .list();
        } else {
            vos = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.vmInstanceUuid, inv.getUuid())
                    .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                    .eq(UsbDeviceVO_.hostUuid, hostUuid)
                    .list();
        }
        if (CollectionUtils.isEmpty(vos)) {
            return;
        }
        List<KVMHostAsyncHttpCallMsg> msgs = transformAndRemoveNull(vos, usb -> {
            ReloadRedirectUsbCmd cmd  = new ReloadRedirectUsbCmd();
            String ip = Q.New(HostVO.class).select(HostVO_.managementIp).eq(HostVO_.uuid, usb.getHostUuid()).findValue();
            cmd.setPort(UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(usb.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN));
            cmd.setIp(ip);
            cmd.setBusNum(usb.getBusNum());
            cmd.setDevNum(usb.getDevNum());
            cmd.setIdVendor(usb.getIdVendor());
            cmd.setIdProduct(usb.getIdProduct());
            cmd.setUsbVersion(usb.getUsbVersion());
            cmd.setHostUuid(inv.getHostUuid());
            cmd.setAttachType(UsbAttachType.Redirect.toString());
            cmd.setVmUuid(inv.getUuid());
            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
            msg.setCommand(cmd);
            msg.setHostUuid(inv.getHostUuid());
            msg.setPath(RELOAD_USB_REDIRECT_PATH);
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, inv.getHostUuid());
            return msg;
        });
        new While<>(msgs).all((msg, compl) -> bus.send(msg, new CloudBusCallBack(compl) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to reload vm[%s] redirect usb", inv.getUuid()));
                }
                compl.done();
            }
        })).run(new NopeWhileDoneCompletion());
    }

    private void syncUsbDeviceFromHost(final String hostUuid, boolean noStatusCheck, NoErrorCompletion completion) {
        GetUsbDevicesCmd cmd = new GetUsbDevicesCmd();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setHostUuid(hostUuid);
        msg.setPath(GET_USB_DEVICES_PATH);
        msg.setNoStatusCheck(noStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("failed to get usb device info from host[uuid:%s]", hostUuid));
                    completion.done();
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                GetUsbDevicesRsp rsp = rly.toResponse(GetUsbDevicesRsp.class);
                if (!rsp.isSuccess()) {
                    logger.debug(String.format("failed to get usb device info from host[uuid:%s]", hostUuid));
                    completion.done();
                    return;
                }

                syncUsbDeviceWithDb(rsp.usbDevicesInfo, hostUuid);
                completion.done();
            }
        });
    }

    private void syncUsbDeviceWithDb(List<UsbDeviceTO> tos, String hostUuid) {
        List<UsbDeviceTO> dbTos = UsbDeviceTO.valueOf(Q.New(UsbDeviceVO.class).eq(UsbDeviceVO_.hostUuid, hostUuid).list());
        //Fix ZSTAC-59333, UsbDeviceTO get from json may have wrong iSerial
        List<UsbDeviceTO> reTos = tos.stream().map(UsbDeviceTO::new).collect(Collectors.toList());
        createNewUsbDevices(reTos, dbTos, hostUuid);
        removeDeletedUsbDevices(reTos, dbTos, hostUuid);
    }

    private void createNewUsbDevices(List<UsbDeviceTO> tos, List<UsbDeviceTO> dbTos, String hostUuid) {
        List<UsbDeviceTO> tosToCreate = new ArrayList<>();
        tosToCreate.addAll(tos);
        tosToCreate.removeAll(dbTos);

        List<UsbDeviceVO> vosToCreate = new ArrayList<>();
        for (UsbDeviceTO to : tosToCreate) {
            UsbDeviceVO vo = new UsbDeviceVO();
            vo.setUuid(Platform.getUuid());
            vo.setName(String.format("%s-%s-%s", to.getiManufacturer(), to.getBusNum(), to.getDevNum()));
            vo.setHostUuid(hostUuid);
            vo.setState(UsbDeviceState.Enabled);
            vo.setBusNum(to.getBusNum());
            vo.setDevNum(to.getDevNum());
            vo.setIdVendor(to.getIdVendor());
            vo.setIdProduct(to.getIdProduct());
            vo.setiManufacturer(to.getiManufacturer());
            vo.setiProduct(to.getiProduct());
            vo.setiSerial(to.getiSerial());
            vo.setUsbVersion(to.getUsbVersion());
            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            vosToCreate.add(vo);
        }

        if (vosToCreate.isEmpty()) {
            return;
        }

        logger.debug(String.format("created new usb devices %s",
                vosToCreate.stream().map(UsbDeviceVO::getUuid).collect(Collectors.toList())));
        dbf.persistCollection(vosToCreate);
    }

    private void removeDeletedUsbDevices(List<UsbDeviceTO> tos, List<UsbDeviceTO> dbTos, String hostUuid) {
        List<UsbDeviceTO> tosToDelete = new ArrayList<>();
        tosToDelete.addAll(dbTos);
        tosToDelete.removeAll(tos);

        for (UsbDeviceTO to : tosToDelete) {
            // Fix ZSTAC-57532: fix same usb devices insert into table
            List<UsbDeviceVO> vos = Q.New(UsbDeviceVO.class)
                    .eq(UsbDeviceVO_.busNum, to.getBusNum())
                    .eq(UsbDeviceVO_.devNum, to.getDevNum())
                    .eq(UsbDeviceVO_.idVendor, to.getIdVendor())
                    .eq(UsbDeviceVO_.idProduct, to.getIdProduct())
                    .eq(UsbDeviceVO_.iManufacturer, to.getiManufacturer())
                    .eq(UsbDeviceVO_.iProduct, to.getiProduct())
                    .eq(UsbDeviceVO_.iSerial, to.getiSerial())
                    .eq(UsbDeviceVO_.usbVersion, to.getUsbVersion())
                    .eq(UsbDeviceVO_.hostUuid, hostUuid)
                    .list();

            if (vos.isEmpty()) {
                continue;
            }

            for (UsbDeviceVO vo : vos) {
                // remove usb device from database if it's not attached
                if (vo.getVmInstanceUuid() == null) {
                    removeUsbVO(vo);
                } else {
                    // detach usb device if it was attached to vm then remove it from database
                    detachUsbDeviceFromVm(UsbDeviceInventory.valueOf(vo), vo.getVmInstanceUuid(), new Completion(null) {
                        @Override
                        public void success() {
                            logger.debug(String.format("detached usb device[uuid:%s] from vm instance[uuid:%s]",
                                    vo.getUuid(), vo.getVmInstanceUuid()));
                            removeUsbVO(vo);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            // Fix ZSTAC-26263: if failed to `virsh detach-device`, then keep vo in database.
                            logger.warn(String.format("failed to detach usb device[uuid:%s] from vm instance[uuid:%s]",
                                    vo.getUuid(), vo.getVmInstanceUuid()));
                        }
                    });
                }
            }
        }
    }

    private void removeUsbVO(UsbDeviceVO vo) {
        // delete usb device from database
        dbf.remove(vo);
        logger.debug(String.format("deleted usb device[uuid:%s] that not existed in host[uuid:%s] any more", vo.getUuid(), vo.getHostUuid()));
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        List<UsbDeviceVO> vos = Q.New(UsbDeviceVO.class)
                .eq(UsbDeviceVO_.hostUuid, host.getUuid())
                .eq(UsbDeviceVO_.attachType, UsbAttachType.Redirect.toString())
                .list();
        if (vos.size() == 0) {
            return;
        }
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("check-usb-server-on-host-%s", host.getUuid()));
        chain.then(new ShareFlow() {
            List<UsbDeviceVO> vosToBeReload;

            //check all usb redirect server port
            //restart usb server if port closed
            //vm reload usb device
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-all-usb-redirect-server-port";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> portList = vos.stream()
                                .map(v -> {
                                    String port = UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(v.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN);

                                    return String.format("%s:%s", v.getUuid(), port);
                                })
                                .collect(Collectors.toList());
                        CheckUsbRedirectPortCmd cmd = new CheckUsbRedirectPortCmd();
                        cmd.portList = portList;
                        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                        msg.setCommand(cmd);
                        msg.setHostUuid(host.getUuid());
                        msg.setPath(CHECK_USB_REDIRECT_PORT);
                        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, host.getUuid());
                        bus.send(msg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (reply.isSuccess()) {
                                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                                    CheckUsbRedirectPortRsp rsp = rly.toResponse(CheckUsbRedirectPortRsp.class);
                                    vosToBeReload = vos.stream().filter(v -> rsp.uuids.contains(v.getUuid())).collect(Collectors.toList());
                                    trigger.next();
                                } else {
                                    trigger.fail(reply.getError());
                                }
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "restart-usb-server";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (vosToBeReload.size() == 0) {
                            trigger.next();
                        }

                        List<KVMHostAsyncHttpCallMsg> msgs = transformAndRemoveNull(vosToBeReload, arg -> {
                            StartUsbServerCmd cmd = new StartUsbServerCmd();
                            cmd.idVendor = arg.getIdVendor();
                            cmd.idProduct = arg.getIdProduct();
                            cmd.busNum = arg.getBusNum();
                            cmd.devNum = arg.getDevNum();
                            cmd.port = UsbSystemTags.USB_REDIRECT_PORT.getTokenByResourceUuid(arg.getUuid(), UsbSystemTags.USB_REDIRECT_PORT_TOKEN);
                            KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
                            msg.setCommand(cmd);
                            msg.setHostUuid(arg.getHostUuid());
                            msg.setPath(HOST_START_USB_REDIRECT_PATH);
                            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, arg.getHostUuid());
                            return msg;
                        });

                        new While<>(msgs).all((msg, compl) -> bus.send(msg, new CloudBusCallBack(compl) {
                            @Override
                            public void run(MessageReply reply) {
                                KVMHostAsyncHttpCallReply rly = reply.castReply();
                                StartUsbServerRsp rsp = rly.toResponse(StartUsbServerRsp.class);
                                if (!rsp.isSuccess()) {
                                    logger.warn(String.format("failed to start usb server on host[%s]", host.getUuid()));
                                }
                                compl.done();
                            }
                        })).run(new WhileDoneCompletion(trigger) {
                            @Override
                            public void done(ErrorCodeList errorCodeList) {
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "vm-reload-usb-device";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (vosToBeReload.size() == 0) {
                            trigger.next();
                            return;
                        }
                        vosToBeReload.forEach( usb -> {
                            VmInstanceInventory inv = VmInstanceInventory.valueOf(dbf.findByUuid(usb.getVmInstanceUuid(), VmInstanceVO.class));
                            reloadRedirectUsb(inv, usb.getHostUuid());
                        });
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {

                    }
                });

                error(new FlowErrorHandler(null) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        logger.error(String.format("failed to check usb server on host %s", host.getUuid()));
                    }
                });
            }
        }).start();
    }


    public static class GetUsbDevicesCmd extends KVMAgentCommands.AgentCommand {
        public List<String> blackList = USB_DEVICE_CLASS_BLACKLIST;
    }

    public static class StartUsbServerCmd extends KVMAgentCommands.AgentCommand {
        public String busNum;
        public String devNum;
        public String idVendor;
        public String idProduct;
        public String port;
    }

    public static class StartUsbServerRsp extends KVMAgentCommands.AgentResponse {
        public String port;
    }

    public static class StopUsbServerCmd  extends KVMAgentCommands.AgentCommand {
        public String port;
        public String busNum;
        public String devNum;
    }

    public static class StopUsbServerRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class GetUsbDevicesRsp extends KVMAgentCommands.AgentResponse {
        public List<UsbDeviceTO> usbDevicesInfo;
    }

    public static class ReloadRedirectUsbCmd extends KvmAttachUsbDeviceCmd {

    }

    public static class ReloadRedirectUsbRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class CheckUsbRedirectPortCmd extends KVMAgentCommands.AgentCommand {
        public List<String> portList;
    }

    public static class CheckUsbRedirectPortRsp extends KVMAgentCommands.AgentResponse {
        public List<String> uuids;
    }

    public static class KvmAttachUsbDeviceCmd extends KVMAgentCommands.AgentCommand {
        public String busNum;
        public String devNum;
        public String idVendor;
        public String idProduct;
        public String usbVersion;
        public String vmUuid;
        public String attachType;
        public String port;
        public String hostUuid;
        public String ip;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }
        public String getAttachType() {
            return attachType;
        }

        public void setAttachType(String attachType) {
            this.attachType = attachType;
        }

        public String getBusNum() {
            return busNum;
        }

        public void setBusNum(String busNum) {
            this.busNum = busNum;
        }

        public String getDevNum() {
            return devNum;
        }

        public void setDevNum(String devNum) {
            this.devNum = devNum;
        }

        public String getIdVendor() {
            return idVendor;
        }

        public void setIdVendor(String idVendor) {
            this.idVendor = idVendor;
        }

        public String getIdProduct() {
            return idProduct;
        }

        public void setIdProduct(String idProduct) {
            this.idProduct = idProduct;
        }

        public String getUsbVersion() {
            return usbVersion;
        }

        public void setUsbVersion(String usbVersion) {
            this.usbVersion = usbVersion;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class KvmAttachUsbDeviceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class KvmDetachUsbDeviceCmd extends KVMAgentCommands.AgentCommand {
        public String busNum;
        public String devNum;
        public String idVendor;
        public String idProduct;
        public String vmUuid;
        public String hostUuid;
        public String port;
        public String attachType;
        public String ip;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getAttachType() {
            return attachType;
        }

        public void setAttachType(String attachType) {
            this.attachType = attachType;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getBusNum() {
            return busNum;
        }

        public void setBusNum(String busNum) {
            this.busNum = busNum;
        }

        public String getDevNum() {
            return devNum;
        }

        public void setDevNum(String devNum) {
            this.devNum = devNum;
        }

        public String getIdVendor() {
            return idVendor;
        }

        public void setIdVendor(String idVendor) {
            this.idVendor = idVendor;
        }

        public String getIdProduct() {
            return idProduct;
        }

        public void setIdProduct(String idProduct) {
            this.idProduct = idProduct;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class KvmDetachUsbDeviceRsp extends KVMAgentCommands.AgentResponse {
    }
}
