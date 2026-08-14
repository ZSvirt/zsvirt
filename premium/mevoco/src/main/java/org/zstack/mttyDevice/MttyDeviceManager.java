package org.zstack.mttyDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.pciDevice.virtual.vfio_mdev.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * @author yu.sun
 * @date 2022/11/17 13:19
 **/
public class MttyDeviceManager extends AbstractService implements
        PostHostConnectExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        PostVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MttyDeviceManager.class);
    
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    
    private final Map<HypervisorType, MttyDeviceBackend> backends = new HashMap<>();
    
    private void populateExtensions() {
        for (MttyDeviceBackend bkd : pluginRgty.getExtensionList(MttyDeviceBackend.class)) {
            MttyDeviceBackend old = backends.get(bkd.getHypervisorType());
            if (old != null) {
                throw new CloudRuntimeException(String.format(
                        "duplicate MttyDeviceBackend[%s, %s] for type[%s]",
                        old.getClass(), bkd.getClass(), bkd.getHypervisorType()
                ));
            }
            backends.put(bkd.getHypervisorType(), bkd);
        }
    }

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {
            String __name__ = "sync-mtty-devices";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String hostUuid = host.getUuid();
                HostVO host = dbf.findByUuid(hostUuid, HostVO.class);
                String hostArch = host.getArchitecture();
                if (!CpuArchitecture.loongarch64.toString().equals(hostArch)) {
                    trigger.next();
                    return;
                }

                SyncMttyDeviceMsg smsg = new SyncMttyDeviceMsg();
                smsg.setHostUuid(hostUuid);
                bus.makeTargetServiceIdByResourceUuid(smsg, MttyDeviceConstants.SERVICE_ID, hostUuid);
                bus.send(smsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(reply.getError());
                            return;
                        }

                        SyncMttyDeviceReply srly = reply.castReply();
                        if (!srly.isSuccess()) {
                            trigger.fail(srly.getError());
                        } else {
                            trigger.next();
                        }
                    }
                });
            }
        };
    }
    
    public MttyDeviceBackend getMttyDeviceBackendByHostUuid(String hostUuid) {
        HostVO hvo = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).find();
        return backends.get(HypervisorType.valueOf(hvo.getHypervisorType()));
    }
    
    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }
    
    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleAPIMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }
    
    private void handleLocalMessage(Message msg) {
        if (msg instanceof SyncMttyDeviceMsg) {
            handle((SyncMttyDeviceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }
    
    private void handle(SyncMttyDeviceMsg msg) {
        SyncMttyDeviceReply reply = new SyncMttyDeviceReply();
        MttyDeviceBackend bkd = getMttyDeviceBackendByHostUuid(msg.getHostUuid());
        if (bkd == null) {
            logger.debug("no mtty device hypervisor backend found, no need to sync mtty devices from host");
            bus.reply(msg, reply);
            return;
        }
        
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                bkd.syncMttyDeviceFromHost(msg.getHostUuid(), new Completion(chain) {
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
            public String getSyncSignature() {
                return String.format("sync-mtty-from-host-%s", msg.getHostUuid());
            }
            
            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
    
    private void handleAPIMessage(APIMessage msg) {
        if (msg instanceof APIGenerateSeMdevDevicesMsg) {
            handle((APIGenerateSeMdevDevicesMsg) msg);
        } else if (msg instanceof APIUngenerateSeMdevDevicesMsg) {
            handle((APIUngenerateSeMdevDevicesMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }
    
    private void handle(APIUngenerateSeMdevDevicesMsg msg) {
        APIUngenerateSeMdevDevicesEvent event = new APIUngenerateSeMdevDevicesEvent(msg.getId());
        MttyDeviceInventory mttyDevice = MttyDeviceInventory.valueOf(dbf.findByUuid(msg.getMttyDeviceUuid(), MttyDeviceVO.class));
        MttyDeviceBackend bkd = getMttyDeviceBackendByHostUuid(mttyDevice.getHostUuid());
        if (bkd == null) {
            logger.debug("no mtty device hypervisor backend found, no need to ungenerate se mdev devices from host");
            bus.publish(event);
            return;
        }
    
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                bkd.ungenerateSeMdevDevices(mttyDevice.getHostUuid(), mttyDevice, msg.getMdevUuids(), new Completion(chain) {
                    @Override
                    public void success() {
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
                return String.format("ungenerate-se-mdev-devices-from-mtty-%s", msg.getMttyDeviceUuid());
            }
    
            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
    
    private void handle(APIGenerateSeMdevDevicesMsg msg) {
        APIGenerateSeMdevDevicesEvent event = new APIGenerateSeMdevDevicesEvent(msg.getId());
        MttyDeviceInventory mttyDevice = MttyDeviceInventory.valueOf(dbf.findByUuid(msg.getMttyDeviceUuid(), MttyDeviceVO.class));
        MttyDeviceBackend bkd = getMttyDeviceBackendByHostUuid(mttyDevice.getHostUuid());
        if (bkd == null) {
            logger.debug("no mtty device hypervisor backend found, no need to generate se mdev devices from host");
            bus.publish(event);
            return;
        }
        
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                List<String> mdevUuids = new ArrayList<>();
                for (int i = 0; i < msg.getVirtPartNum(); i++) {
                    mdevUuids.add(Platform.getUuid());
                }
                bkd.generateSeMdevDevices(mttyDevice.getHostUuid(), mttyDevice, mdevUuids, false, new Completion(chain) {
                    @Override
                    public void success() {
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
                return String.format("generate-se-mdev-devices-from-mtty-%s", msg.getMttyDeviceUuid());
            }
    
            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }
    
    @Override
    public String getId() {
        return bus.makeLocalServiceId(MttyDeviceConstants.SERVICE_ID);
    }
    
    @Override
    public void postBeforeInstantiateVmResource(VmInstanceSpec spec) {
    
    }
    
    @Override
    public void postInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        String vmUuid = spec.getVmInventory().getUuid();
        if (VmSystemTags.SECURITY_ELEMENT_ENABLE.hasTag(vmUuid)) {
            VmSystemTags.SECURITY_ELEMENT_ENABLE.delete(vmUuid);
        }
        completion.success();
    }
    
    @Override
    public void postReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }
    
    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
    
    }
    
    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        if (!spec.isEnableSecurityElement()) {
            completion.success();
            return;
        }
        String vmUuid = spec.getVmInventory().getUuid();
        String hostUuid = spec.getDestHost().getUuid();
        String mttyDeviceUuid = Q.New(MttyDeviceVO.class).select(MttyDeviceVO_.uuid)
                .eq(MttyDeviceVO_.hostUuid, hostUuid).findValue();
        String mdevUuid = Q.New(MdevDeviceVO.class).select(MdevDeviceVO_.uuid).eq(MdevDeviceVO_.type, MdevDeviceType.SE_Controller)
                .eq(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .eq(MdevDeviceVO_.mttyUuid, mttyDeviceUuid).findValue();
        if (mdevUuid != null && mdevUuid.length() != 0) {
            MdevDeviceVO mdev = dbf.findByUuid(mdevUuid, MdevDeviceVO.class);
            mdev.setVmInstanceUuid(vmUuid);
            mdev.setStatus(MdevDeviceStatus.Attached);
            dbf.update(mdev);
            completion.success();
            return;
        }
        String vifoMdevUuid = Platform.getUuid();
        MttyDeviceInventory mttyDevice = MttyDeviceInventory.valueOf(dbf.findByUuid(mttyDeviceUuid, MttyDeviceVO.class));
        MttyDeviceBackend bkd = getMttyDeviceBackendByHostUuid(mttyDevice.getHostUuid());
        bkd.generateSeMdevDevices(mttyDevice.getHostUuid(), mttyDevice, Arrays.asList(vifoMdevUuid), false, new Completion(completion) {
            @Override
            public void success() {
                logger.debug(String.format("tried to re-splited mtty device[uuid:%s] into mdev devices", mttyDevice.getUuid()));
                MdevDeviceVO mdev = dbf.findByUuid(vifoMdevUuid, MdevDeviceVO.class);
                mdev.setVmInstanceUuid(vmUuid);
                mdev.setStatus(MdevDeviceStatus.Attached);
                dbf.update(mdev);
                completion.success();
            }
            
            @Override
            public void fail(ErrorCode errorCode) {
                logger.error(String.format("failed to re-splited mtty device[uuid:%s] into mdev devices", mttyDevice.getUuid()));
                completion.fail(errorCode);
            }
        });
    }
    
    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }
}