package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.IsoOperator;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLocalPrimaryStorageMsg) {
            validate((APIAddLocalPrimaryStorageMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            validate((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APILocalStorageGetVolumeMigratableHostsMsg) {
            validate((APILocalStorageGetVolumeMigratableHostsMsg) msg);
        }

        return msg;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return asList(
                APIAddLocalPrimaryStorageMsg.class,
                APIGetLocalStorageHostDiskCapacityMsg.class,
                APIQueryLocalStorageResourceRefMsg.class,
                APILocalStorageMigrateVolumeMsg.class,
                APILocalStorageGetVolumeMigratableHostsMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }

    private void validate(APILocalStorageGetVolumeMigratableHostsMsg msg) {
        APILocalStorageGetVolumeMigratableReply reply = new APILocalStorageGetVolumeMigratableReply();

        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
        LocalStorageResourceRefVO ref = q.find();
        if (ref == null) {
            reply.setInventories(new ArrayList<HostInventory>());
            bus.reply(msg, reply);
            throw new StopRoutingException();
        }

        msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());
    }

    private void validate(APILocalStorageMigrateVolumeMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                //1.confirm that  volume is on local storage and  not on the dest Host.
                LocalStorageResourceRefVO ref = Q.New(LocalStorageResourceRefVO.class)
                        .eq(LocalStorageResourceRefVO_.resourceType,VolumeVO.class.getSimpleName())
                        .eq(LocalStorageResourceRefVO_.resourceUuid,msg.getVolumeUuid()).find();
                if (ref == null) {
                    throw new ApiMessageInterceptionException(argerr("the volume[uuid:%s] is not on any local primary storage", msg.getVolumeUuid()));
                }
                msg.setPrimaryStorageUuid(ref.getPrimaryStorageUuid());

                if (ref.getHostUuid().equals(msg.getDestHostUuid())) {
                    throw new ApiMessageInterceptionException(argerr("the volume[uuid:%s] is already on the host[uuid:%s]", msg.getVolumeUuid(), msg.getDestHostUuid()));
                }

                //2.confirm primary storage is available.
                PrimaryStorageVO vo = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid,ref.getPrimaryStorageUuid()).find();
                if (vo == null) {
                    throw new ApiMessageInterceptionException(argerr("the primary storage[uuid:%s] is not found", msg.getPrimaryStorageUuid()));
                }

                if (vo.getState() == PrimaryStorageState.Disabled || vo.getState() == PrimaryStorageState.Maintenance) {
                    throw new ApiMessageInterceptionException(argerr("the primary storage[uuid:%s] is disabled or maintenance cold migrate is not allowed", ref.getPrimaryStorageUuid()));
                }

                //3.confirm the dest host belong to the local storage where the volume locates and physical capacity is enough
                LocalStorageHostRefVO refVO = Q.New(LocalStorageHostRefVO.class)
                        .eq(LocalStorageHostRefVO_.hostUuid, msg.getDestHostUuid())
                        .eq(LocalStorageHostRefVO_.primaryStorageUuid,ref.getPrimaryStorageUuid())
                        .find();
                if (refVO == null) {
                    throw new ApiMessageInterceptionException(argerr("the dest host[uuid:%s] doesn't belong to the local primary storage[uuid:%s] where the" +
                            " volume[uuid:%s] locates", msg.getDestHostUuid(), ref.getPrimaryStorageUuid(), msg.getVolumeUuid()));
                }

                double physicalThreshold = physicalCapacityMgr.getRatio(msg.getPrimaryStorageUuid());
                if (!((refVO.getTotalPhysicalCapacity() * (1.0 - physicalThreshold)) <= refVO.getAvailablePhysicalCapacity())) {
                    throw new ApiMessageInterceptionException(argerr("the dest host[uuid:%s] doesn't have enough physical capacity due to the threshold of " +
                            "primary storage[uuid:%s] is %f but available physical capacity is %d", msg.getDestHostUuid(), msg.getPrimaryStorageUuid(), physicalThreshold, refVO.getAvailablePhysicalCapacity()));
                }

                //4.confirm primary storage is available.
                VolumeVO vol = Q.New(VolumeVO.class).eq(VolumeVO_.uuid,msg.getVolumeUuid()).find();
                if (VolumeStatus.Ready != vol.getStatus()) {
                    throw new ApiMessageInterceptionException(argerr("the volume[uuid:%s] is not in status of Ready, cannot migrate it", msg.getVolumeUuid()));
                }

                //5.confirm that the data volume and iso has detach the vm and the root volume will migrate to appropriate cluster.
                if (vol.getType() == VolumeType.Data && vol.getVmInstanceUuid() != null) {
                    throw new ApiMessageInterceptionException(argerr("the data volume[uuid:%s, name: %s] is still attached to the VM[uuid:%s]. Please detach" +
                            " it before migration", vol.getUuid(), vol.getName(), vol.getVmInstanceUuid()));
                } else if (vol.getType() == VolumeType.Root) {
                    msg.setVmInstanceUuid(vol.getVmInstanceUuid());
                    VmInstanceState vmstate = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.state)
                            .eq(VmInstanceVO_.uuid,vol.getVmInstanceUuid()).findValue();
                    if (VmInstanceState.Stopped != vmstate) {
                        throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm is in" +
                                " state of %s, please stop it before migration", vol.getUuid(), vol.getVmInstanceUuid(), vmstate));
                    }

                    long count = Q.New(VolumeVO.class)
                            .eq(VolumeVO_.type,VolumeType.Data)
                            .eq(VolumeVO_.vmInstanceUuid,vol.getVmInstanceUuid()).count();
                    if (count != 0) {
                        throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm still" +
                                " has %s data volumes attached, please detach them before migration", vol.getUuid(), vol.getVmInstanceUuid(), count));
                    }

                    if (IsoOperator.isIsoAttachedToVm(vol.getVmInstanceUuid())) {
                        throw new ApiMessageInterceptionException(operr("the volume[uuid:%s] is the root volume of the vm[uuid:%s]. Currently the vm still" +
                                " has ISO attached, please detach it before migration", vol.getUuid(), vol.getVmInstanceUuid()));

                    }

                    String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                            .eq(HostVO_.uuid,msg.getDestHostUuid()).findValue();
                    String originClusterUuid = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.clusterUuid)
                            .eq(VmInstanceVO_.uuid, vol.getVmInstanceUuid()).findValue();
                    if(originClusterUuid == null){
                        throw new ApiMessageInterceptionException(
                                err(SysErrors.INTERNAL,"The clusterUuid of vm[uuid:%s] cannot be null when migrate the root volume[uuid:%s, name: %s]",vol.getVmInstanceUuid(),vol.getUuid(),vol.getName()));
                    }

                    if(!originClusterUuid.equals(clusterUuid)){
                        List<String> originL2NetworkList  = sql("select l2NetworkUuid from L3NetworkVO" +
                                " where uuid in(select l3NetworkUuid from VmNicVO where vmInstanceUuid = :vmUuid)")
                                .param("vmUuid",vol.getVmInstanceUuid()).list();
                        List<String> l2NetworkList = sql("select l2NetworkUuid from L2NetworkClusterRefVO" +
                                " where clusterUuid = :clusterUuid")
                                .param("clusterUuid",clusterUuid).list();
                        for(String l2:originL2NetworkList){
                            if(!l2NetworkList.contains(l2)){
                                throw new ApiMessageInterceptionException(
                                        operr("The two clusters[uuid:%s,uuid:%s] cannot access each other in l2 network  when migrate the vm[uuid:%s] to another cluster", originClusterUuid, clusterUuid, vol.getVmInstanceUuid()));
                            }
                        }
                    }
                }

            }
        }.execute();

    }

    private void validate(APIAddLocalPrimaryStorageMsg msg) {
        String url = msg.getUrl();
        if (!url.startsWith("/")) {
            throw new ApiMessageInterceptionException(argerr("the url[%s] is not an absolute path starting with '/'", msg.getUrl()));
        }
        if (url.startsWith("/dev") || url.startsWith("/proc") || url.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }
    }
}
