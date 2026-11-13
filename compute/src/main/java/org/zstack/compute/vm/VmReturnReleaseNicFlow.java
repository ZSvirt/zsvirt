package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.ReturnIpMsg;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmReturnReleaseNicFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmReturnReleaseNicFlow.class);
    public static final String EXPUNGE_EXTERNAL_NICS = VmReturnReleaseNicFlow.class.getName() + ".expungeExternalNics";

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected VmInstanceDeletionPolicyManager deletionPolicyMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        boolean expunge = spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.Expunge;
        final List<VmNicInventory> nics;
        if (expunge) {
            nics = new ArrayList<>();
            for (AfterAllocateExternalNetworkNicExtensionPoint ext :
                    pluginRgty.getExtensionList(AfterAllocateExternalNetworkNicExtensionPoint.class)) {
                nics.addAll(ext.getExternalNetworkNicsToRelease(spec.getVmInventory().getVmNics()));
            }
        } else {
            nics = spec.getVmInventory().getVmNics();
        }
        if (nics.isEmpty()) {
            chain.next();
            return;
        }
        if (expunge) {
            data.put(EXPUNGE_EXTERNAL_NICS, nics);
        }

        Completion releaseCompletion = new Completion(chain) {
            @Override
            public void success() {
                releaseLocalNics(chain, spec, spec.getVmInventory().getVmNics(), data);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        };
        if (expunge) {
            expungeNic(nics, releaseCompletion);
        } else {
            releaseNic(nics, releaseCompletion);
        }
    }

    private void expungeNic(List<VmNicInventory> nics, Completion completion) {
        List<AfterAllocateExternalNetworkNicExtensionPoint> exts =
                pluginRgty.getExtensionList(AfterAllocateExternalNetworkNicExtensionPoint.class);
        new While<>(exts).each((ext, done) -> {
            List<VmNicInventory> selected = ext.getExternalNetworkNicsToRelease(nics);
            if (selected.isEmpty()) {
                done.done();
                return;
            }
            ext.expungeExternalNetworkNics(selected, new Completion(done) {
                @Override
                public void success() {
                    done.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    done.addError(errorCode);
                    done.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.hasError()) {
                    completion.fail(errors.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    private void releaseNic(List<VmNicInventory> nics, Completion completion) {
        List<AfterAllocateExternalNetworkNicExtensionPoint> exts =
                pluginRgty.getExtensionList(AfterAllocateExternalNetworkNicExtensionPoint.class);
        new While<>(exts).each((ext, done) -> ext.releaseExternalNetworkNics(nics, new Completion(done) {
            @Override
            public void success() {
                done.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                done.addError(errorCode);
                done.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.hasError()) {
                    completion.fail(errors.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
    }

    private void releaseLocalNics(FlowTrigger chain, VmInstanceSpec spec, List<VmNicInventory> nics, Map data) {
        List<ReturnIpMsg> msgs = new ArrayList<>(nics.size());
        for (VmNicInventory nic : nics) {
            if (nic.getUsedIps() == null) {
                continue;
            }
            for (UsedIpInventory ip : nic.getUsedIps()) {
                ReturnIpMsg msg = new ReturnIpMsg();
                msg.setL3NetworkUuid(ip.getL3NetworkUuid());
                msg.setUsedIpUuid(ip.getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
                msgs.add(msg);
            }
        }

        new While<>(msgs).each((returnIpMsg, completion) -> bus.send(returnIpMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to release ip[usedIpUuid:%s] for vm[uuid:%s], but continue anyway",
                            returnIpMsg.getUsedIpUuid(), spec.getVmInventory().getUuid()));
                }
                completion.done();
            }
        })).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                removeLocalNics(spec, nics, data);
                chain.next();
            }
        });
    }

    private void removeLocalNics(VmInstanceSpec spec, List<VmNicInventory> nics, Map data) {
        for (VmNicInventory nic : nics) {
            VmNicVO vo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
            if (VmInstanceConstant.USER_VM_TYPE.equals(spec.getVmInventory().getType())) {
                VmInstanceDeletionPolicy deletionPolicy = getDeletionPolicy(spec, data);
                if (deletionPolicy == VmInstanceDeletionPolicy.Direct) {
                    dbf.remove(vo);
                } else {
                    vo.removeIp();
                    dbf.update(vo);
                }
            } else {
                dbf.remove(vo);
            }
        }
    }

    private VmInstanceDeletionPolicy getDeletionPolicy(VmInstanceSpec spec, Map data) {
        if (data.containsKey(VmInstanceConstant.Params.DeletionPolicy)) {
            return (VmInstanceDeletionPolicy) data.get(VmInstanceConstant.Params.DeletionPolicy);
        }

        return deletionPolicyMgr.getDeletionPolicy(spec.getVmInventory().getUuid());
    }
}
