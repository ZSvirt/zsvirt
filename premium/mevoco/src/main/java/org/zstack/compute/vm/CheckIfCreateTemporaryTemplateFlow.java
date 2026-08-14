package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.AllocateHostDryRunReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicParam;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2021/3/15.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CheckIfCreateTemporaryTemplateFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(CheckIfCreateTemporaryTemplateFlow.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @SuppressWarnings("unchecked")
    @Override
    public void run(FlowTrigger trigger, Map data) {
        CreateVmFromVolumeResourceSpec spec = (CreateVmFromVolumeResourceSpec) data.get(PremiumVmInstanceConstant.VM_INSTANCE_FROM_VOLUME_SPEC);
        NewVmInstanceMessage2 apiMsg = spec.getApiMsg();

        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setUuid(Platform.FAKE_UUID);

        List<VmNicParam> vmNicParams = new ArrayList<>();
        if (!StringUtils.isEmpty(apiMsg.getVmNicParams())) {
            vmNicParams.addAll(JSONObjectUtil.toCollection(apiMsg.getVmNicParams(), ArrayList.class, VmNicParam.class));
        }

        DesignatedAllocateHostMsg amsg = new DesignatedAllocateHostMsg();
        amsg.setL3NetworkUuids(apiMsg.getL3NetworkUuids());
        amsg.setVmNicParams(vmNicParams);
        amsg.setVmOperation(VmInstanceConstant.VmOperation.NewCreate.toString());
        amsg.setHostUuid(apiMsg.getHostUuid());
        amsg.setClusterUuid(apiMsg.getClusterUuid());
        amsg.setZoneUuid(apiMsg.getZoneUuid());
        amsg.setRequiredPrimaryStorageUuids(spec.getDataVolumeRequiredPrimaryStorageUuids());
        if (apiMsg.getInstanceOfferingUuid() != null) {
            InstanceOfferingVO instanceOffering = dbf.findByUuid(apiMsg.getInstanceOfferingUuid(), InstanceOfferingVO.class);
            amsg.setCpuCapacity(instanceOffering.getCpuNum());
            amsg.setMemoryCapacity(instanceOffering.getMemorySize());

            vm.setCpuNum(instanceOffering.getCpuNum());
            vm.setMemorySize(instanceOffering.getMemorySize());
            vm.setInstanceOfferingUuid(apiMsg.getInstanceOfferingUuid());
        } else {
            amsg.setCpuCapacity(apiMsg.getCpuNum());
            amsg.setMemoryCapacity(apiMsg.getMemorySize());

            vm.setCpuNum(apiMsg.getCpuNum());
            vm.setMemorySize(apiMsg.getMemorySize());
        }

        if (!CollectionUtils.isEmpty(apiMsg.getL3NetworkUuids())) {
            vm.setDefaultL3NetworkUuid(apiMsg.getDefaultL3NetworkUuid() == null ? apiMsg.getL3NetworkUuids().get(0) : apiMsg.getDefaultL3NetworkUuid());
        }
        amsg.setAllowNoL3Networks(true);
        vm.setName("for-getting-candidates-zones-clusters-hosts");
        amsg.setVmInstance(vm);

        amsg.setDryRun(true);
        amsg.setListAllHosts(true);
        bus.makeLocalServiceId(amsg, HostAllocatorConstant.SERVICE_ID);
        bus.send(amsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    trigger.fail(reply.getError());
                    return;
                }

                AllocateHostDryRunReply r = reply.castReply();

                if (!spec.getDataVolumeRequiredHostUuids().isEmpty()) {
                    r.getHosts().removeIf(it -> !spec.getDataVolumeRequiredHostUuids().contains(it.getUuid()));
                }

                if (r.getHosts().isEmpty()) {
                    trigger.fail(operr("only host(s)[uuid(s): %s] can access data volume.",
                            spec.getDataVolumeRequiredHostUuids()));
                    return;
                }

                spec.setPreAllocateClusterUuids(r.getHosts().stream().map(HostInventory::getClusterUuid).collect(Collectors.toSet()));
                filterVolumeAccessibleHosts(spec.getOriginVolumeUuidForRootVolume(), spec.getPrimaryStorageUuidForRootVolume(),
                        r.getHosts(), new ReturnValueCompletion<List<HostInventory>>(trigger) {
                            @Override
                            public void success(List<HostInventory> hosts) {
                                boolean accessible = !hosts.isEmpty();
                                logger.debug(String.format("create temporary root image: %s", String.valueOf(accessible)));
                                spec.getRootVolumeImage().setTemporary(accessible);
                                if (accessible) {
                                    spec.setPreAllocateClusterUuids(hosts.stream().map(HostInventory::getClusterUuid).collect(Collectors.toSet()));
                                }

                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
            }
        });
    }

    private void filterVolumeAccessibleHosts(String volumeUuid, String dstPrimaryStorageUuid,
                                             List<HostInventory> dstHosts, ReturnValueCompletion<List<HostInventory>> completion) {
        String volumePsUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid)
                .select(VolumeVO_.primaryStorageUuid)
                .findValue();

        // check if specific primary storage is volume location.
        if (dstPrimaryStorageUuid != null && !dstPrimaryStorageUuid.equals(volumePsUuid)) {
            completion.success(Collections.emptyList());
            return;
        }

        List<String> volAccessibleClusterUuids = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, volumePsUuid)
                .select(PrimaryStorageClusterRefVO_.clusterUuid)
                .listValues();

        dstHosts.removeIf(host -> !volAccessibleClusterUuids.contains(host.getClusterUuid()));
        if (dstHosts.isEmpty()) {
            completion.success(dstHosts);
            return;
        }

        GetPrimaryStorageResourceLocationMsg gmsg = new GetPrimaryStorageResourceLocationMsg();
        gmsg.setPrimaryStorageUuid(volumePsUuid);
        gmsg.setResourceUuid(volumeUuid);
        gmsg.setResourceType(VolumeVO.class.getSimpleName());
        bus.makeLocalServiceId(gmsg, PrimaryStorageConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                GetPrimaryStorageResourceLocationReply r = reply.castReply();
                // TODO find more accurate way when volume locate more than one host.
                if (r.getHostUuids() != null && !r.getHostUuids().isEmpty()) {
                    dstHosts.removeIf(it -> !r.getHostUuids().contains(it.getUuid()));
                }

                completion.success(dstHosts);
            }
        });
    }
}
