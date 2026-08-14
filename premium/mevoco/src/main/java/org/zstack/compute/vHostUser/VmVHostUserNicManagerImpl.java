package org.zstack.compute.vHostUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vHostUser.*;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class VmVHostUserNicManagerImpl extends AbstractService implements
        KVMStartVmExtensionPoint,
        VmInstanceMigrateExtensionPoint {

    private static final CLogger logger = Utils.getLogger(VmVHostUserNicManagerImpl.class);
    private Map<String, VmVHostUserNicHypervisorBackend> vmVHostUserNicHypervisorBackend = new HashMap<>();

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    @Qualifier("KVMHostFactory")
    protected KVMHostFactory factory;

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateExtensions() {
        for (VmVHostUserNicHypervisorBackend bkd : pluginRgty.getExtensionList(VmVHostUserNicHypervisorBackend.class)) {
            String type = bkd.getHypervisorType().toString();
            VmVHostUserNicHypervisorBackend old = vmVHostUserNicHypervisorBackend.get(type);
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VmVHostUserNicHypervisorBackend[%s, %s] for type[%s]",
                        bkd.getClass().getName(), old.getClass().getName(), type));
            }
            vmVHostUserNicHypervisorBackend.put(type, bkd);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof DeleteVHostUserResourceMsg) {
            handle((DeleteVHostUserResourceMsg) msg);
        } else if (msg instanceof GenerateVHostUserResourceMsg) {
            handle((GenerateVHostUserResourceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(DeleteVHostUserResourceMsg msg) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hypervisorType)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        final DeleteVHostUserResourceReply reply = new DeleteVHostUserResourceReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("expunge-vHost-User-Resource-for-vm-%s", msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmVHostUserNicHypervisorBackend bkd = vmVHostUserNicHypervisorBackend.get(hypervisorType);
                bkd.expungeVHostUserResource(hostUuid, msg, new Completion(msg) {
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
                return DeleteVHostUserResourceMsg.class.getName();
            }
        });
    }

    private void handle(GenerateVHostUserResourceMsg msg) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hypervisorType)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        String hostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.lastHostUuid)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .findValue();

        final GenerateVHostUserResourceReply reply = new GenerateVHostUserResourceReply();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("attach-vhost-user-client-for-vm-%s", msg.getVmInstanceUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                VmVHostUserNicHypervisorBackend bkd = vmVHostUserNicHypervisorBackend.get(hypervisorType);
                bkd.generateVHostUserResource(hostUuid, msg, new Completion(msg) {
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
                return GenerateVHostUserResourceMsg.class.getName();
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmVHostUserNicConstant.SERVICE_ID);
    }

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {

    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {

    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        String vmUuid = spec.getVmInventory().getUuid();
        List<String> vHostUserNicUuids = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmNicVO_.type, VmOvsNicConstant.ACCEL_TYPE_VHOST_USER_SPACE)
                .select(VmNicVO_.uuid).listValues();

        if (vHostUserNicUuids.isEmpty()) {
            return;
        }

        DeleteVHostUserResourceMsg msg = new DeleteVHostUserResourceMsg();
        msg.setVmInstanceUuid(vmUuid);
        VmVHostUserNicHypervisorBackend bkd = vmVHostUserNicHypervisorBackend.get(host.getHypervisorType());
        bkd.expungeVHostUserResource(host.getUuid(), msg, new Completion(msg) {
            @Override
            public void success() {
                logger.debug(String.format("expunge vhost user nic resource for vm[uuid:%s] in host[uuid:%s]", vmUuid, host.getUuid()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to expunge vhost user nic resource for vm[uuid:%s] in host[uuid:%s]", vmUuid, host.getUuid()));
            }
        });
    }

    private List<KVMAgentCommands.NicTO> getNicTos(String vmUuid) {
        List<VmNicVO> nics = Q.New(VmNicVO.class).eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmNicVO_.type, VmOvsNicConstant.ACCEL_TYPE_VHOST_USER_SPACE)
                .list();
        return VmNicInventory.valueOf(nics).stream().map(this::completeVHostUserNicInfo).collect(Collectors.toList());
    }

    private List<String> getCleanTrafficIp(VmNicInventory nic) {
        boolean isUserVm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, nic.getVmInstanceUuid()).select(VmInstanceVO_.type)
                .findValue().equals(VmInstanceConstant.USER_VM_TYPE);

        if (!isUserVm) {
            return null;
        }

        String tagValue = VmSystemTags.CLEAN_TRAFFIC.getTokenByResourceUuid(nic.getVmInstanceUuid(), VmSystemTags.CLEAN_TRAFFIC_TOKEN);
        if (Boolean.parseBoolean(tagValue) || (tagValue == null && VmGlobalConfig.VM_CLEAN_TRAFFIC.value(Boolean.class))) {
            return VmNicHelper.getIpAddresses(nic);
        }

        return null;
    }

    private L2NetworkInventory getL2NetworkTypeFromL3NetworkUuid(String l3NetworkUuid) {
        String sql = "select l2 from L2NetworkVO l2 where l2.uuid = (select l3.l2NetworkUuid from L3NetworkVO l3 where l3.uuid = :l3NetworkUuid)";
        TypedQuery<L2NetworkVO> query = dbf.getEntityManager().createQuery(sql, L2NetworkVO.class);
        query.setParameter("l3NetworkUuid", l3NetworkUuid);
        L2NetworkVO l2vo = query.getSingleResult();
        return L2NetworkInventory.valueOf(l2vo);
    }

    @Transactional(readOnly = true)
    private KVMAgentCommands.NicTO completeVHostUserNicInfo(VmNicInventory nic) {
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class));
        L2NetworkInventory l2inv = getL2NetworkTypeFromL3NetworkUuid(nic.getL3NetworkUuid());
        KVMCompleteNicInformationExtensionPoint extp = factory.getCompleteNicInfoExtension(L2NetworkType.valueOf(l2inv.getType()));
        KVMAgentCommands.NicTO to = extp.completeNicInformation(l2inv, l3Inv, nic);

        if (to.getUseVirtio() == null) {
            to.setUseVirtio(VmSystemTags.VIRTIO.hasTag(nic.getVmInstanceUuid()));
            to.setIps(getCleanTrafficIp(nic));
        }

        return to;
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<VmNicInventory> vmNics = inv.getVmNics().stream()
                .filter(vn -> vn.getType().equals(VmOvsNicConstant.ACCEL_TYPE_VHOST_USER_SPACE))
                .collect(Collectors.toList());

        if (vmNics.isEmpty()) {
            return;
        }

        FutureCompletion completion = new FutureCompletion(null);

        List<KVMAgentCommands.NicTO> nicTos = getNicTos(inv.getUuid());
        if (nicTos.isEmpty()) {
            return;
        }
        GenerateVHostUserResourceMsg msg = new GenerateVHostUserResourceMsg();
        msg.setVmInstanceUuid(inv.getUuid());
        msg.setNics(nicTos);
        VmVHostUserNicHypervisorBackend bkd = vmVHostUserNicHypervisorBackend.get(inv.getHypervisorType());

        bkd.generateVHostUserResource(destHostUuid, msg, completion);
        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot generate vhost user client for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
        List<KVMAgentCommands.NicTO> nicTos = getNicTos(inv.getUuid());
        if (nicTos.isEmpty()) {
            return;
        }

        DeleteVHostUserResourceMsg msg = new DeleteVHostUserResourceMsg();
        msg.setVmInstanceUuid(inv.getUuid());
        VmVHostUserNicHypervisorBackend bkd = vmVHostUserNicHypervisorBackend.get(inv.getHypervisorType());
        bkd.expungeVHostUserResource(srcHostUuid, msg, new Completion(msg) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to release vhost user nic for vm[uuid:%s] after migrate : %s", inv.getUuid(), errorCode));
            }
        });
    }

    @Override
    public void failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
        afterMigrateVm(inv, destHostUuid);
    }

}
