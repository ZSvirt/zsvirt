package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.UpdateQuery;
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
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataManager;
import org.zstack.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by frank on 7/18/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmDetachNicFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private VmInstanceResourceMetadataManager vidm;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        final VmNicInventory nic = spec.getDestNics().get(0);
        boolean releaseNic = (boolean) data.get(VmInstanceConstant.Params.ReleaseNicAfterDetachNic.toString());
        if (!releaseNic) {
            detachLocalNic(trigger, spec, nic, false);
            return;
        }
        releaseNic(nic, new Completion(trigger) {
            @Override
            public void success() {
                detachLocalNic(trigger, spec, nic, true);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    private void releaseNic(VmNicInventory nic, Completion completion) {
        List<AfterAllocateExternalNetworkNicExtensionPoint> exts =
                pluginRgty.getExtensionList(AfterAllocateExternalNetworkNicExtensionPoint.class);
        new While<>(exts).each((ext, done) -> ext.releaseExternalNetworkNics(
                Collections.singletonList(nic), new Completion(done) {
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

    private void detachLocalNic(FlowTrigger trigger, VmInstanceSpec spec, VmNicInventory nic, boolean releaseNic) {
        String defaultL3Uuid = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class).getDefaultL3NetworkUuid();

        if (defaultL3Uuid != null && VmNicHelper.getL3Uuids(nic).contains(defaultL3Uuid)) {
            // reset the default L3 to a l3 network; if there is no other l3, the default l3 will be null
            VmNicInventory selected = CollectionUtils.findOneOrNull(
                    spec.getVmInventory().getVmNics(), arg -> !arg.getUuid().equals(nic.getUuid()));

            VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vm.setDefaultL3NetworkUuid(selected == null ? null : selected.getL3NetworkUuid());
            dbf.update(vm);
        }

        vidm.deleteVmResourceMetadata(nic.getUuid(), spec.getVmInventory().getUuid());

        if (!releaseNic) {
            UpdateQuery.New(VmNicVO.class)
                    .eq(VmNicVO_.uuid, nic.getUuid())
                    .set(VmNicVO_.vmInstanceUuid, null)
                    .set(VmNicVO_.deviceId, 1)
                    .set(VmNicVO_.internalName, null)
                    .update();
            trigger.next();
            return;
        }

        new While<>(nic.getUsedIps()).all((ip, comp) -> {
            ReturnIpMsg msg = new ReturnIpMsg();
            msg.setUsedIpUuid(ip.getUuid());
            msg.setL3NetworkUuid(ip.getL3NetworkUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, ip.getL3NetworkUuid());
            bus.send(msg, new CloudBusCallBack(comp) {
                @Override
                public void run(MessageReply reply) {
                    comp.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger){
            @Override
            public void done(ErrorCodeList errorCodeList) {
                dbf.removeByPrimaryKey(nic.getUuid(), VmNicVO.class);
                trigger.next();
            }
        });
    }
}
