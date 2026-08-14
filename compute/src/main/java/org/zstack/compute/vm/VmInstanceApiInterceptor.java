package org.zstack.compute.vm;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.configuration.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.*;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterAO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vm.cdrom.*;
import org.zstack.header.vm.APIRegisterVmInstanceFromMetadataMsg;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataGroupVO;
import org.zstack.header.vm.devices.VmInstanceResourceMetadataGroupVO_;
import org.zstack.header.vm.metadata.VmMetadataPathBuildExtensionPoint;
import org.zstack.header.volume.*;
import org.zstack.network.l2.L2NetworkHostUtils;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.network.NicIpAddressInfo;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.isEmpty;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("vmInstance")
public class VmInstanceApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VmInstanceApiInterceptor.class);
    private static final VmInstanceHelper vmInstanceHelper = new VmInstanceHelper();
    private static final StaticIpOperator ipOperator = new StaticIpOperator();
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private VmNicManager nicManager;
    @Autowired
    private PluginRegistry pluginRgty;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VmInstanceMessage) {
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmsg.getVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDestroyVmInstanceMsg) {
            validate((APIDestroyVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeMsg) {
            validate((APICreateVmInstanceFromVolumeMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICreateVmInstanceFromVolumeSnapshotGroupMsg) {
            validate((APICreateVmInstanceFromVolumeSnapshotGroupMsg) msg);
        } else if (msg instanceof APIGetVmAttachableDataVolumeMsg) {
            validate((APIGetVmAttachableDataVolumeMsg) msg);
        } else if (msg instanceof APIDetachL3NetworkFromVmMsg) {
            validate((APIDetachL3NetworkFromVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicStateMsg) {
            validate((APIChangeVmNicStateMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmMsg) {
            validate((APIAttachL3NetworkToVmMsg) msg);
        } else if (msg instanceof APIChangeVmNicNetworkMsg) {
            validate((APIChangeVmNicNetworkMsg) msg);
        } else if (msg instanceof APIGetCandidateL3NetworksForChangeVmNicNetworkMsg)
            validate((APIGetCandidateL3NetworksForChangeVmNicNetworkMsg) msg);
        else if (msg instanceof APIAttachVmNicToVmMsg) {
            validate((APIAttachVmNicToVmMsg) msg);
        } else if (msg instanceof APICreateVmNicMsg) {
            validate((APICreateVmNicMsg) msg);
        } else if (msg instanceof APIAttachIsoToVmInstanceMsg) {
            validate((APIAttachIsoToVmInstanceMsg) msg);
        } else if (msg instanceof APIDetachIsoFromVmInstanceMsg) {
            validate((APIDetachIsoFromVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmBootOrderMsg) {
            validate((APISetVmBootOrderMsg) msg);
        } else if (msg instanceof APISetVmBootVolumeMsg) {
            validate((APISetVmBootVolumeMsg) msg);
        } else if (msg instanceof APIDeleteVmStaticIpMsg) {
            validate((APIDeleteVmStaticIpMsg) msg);
        } else if (msg instanceof APISetVmStaticIpMsg) {
            validate((APISetVmStaticIpMsg) msg);
        } else if (msg instanceof APIGetVmDnsMsg) {
            validate((APIGetVmDnsMsg) msg);
        } else if (msg instanceof APISetVmDnsMsg) {
            validate((APISetVmDnsMsg) msg);
        } else if (msg instanceof APIStartVmInstanceMsg) {
            validate((APIStartVmInstanceMsg) msg);
        } else if (msg instanceof APIGetInterdependentL3NetworksBackupStoragesMsg) {
            validate((APIGetInterdependentL3NetworksBackupStoragesMsg) msg);
        } else if (msg instanceof APIUpdateVmInstanceMsg) {
            validate((APIUpdateVmInstanceMsg) msg);
        } else if (msg instanceof APISetVmConsolePasswordMsg) {
            validate((APISetVmConsolePasswordMsg) msg);
        } else if (msg instanceof APIChangeInstanceOfferingMsg) {
            validate((APIChangeInstanceOfferingMsg) msg);
        } else if (msg instanceof APIMigrateVmMsg) {
            validate((APIMigrateVmMsg) msg);
        } else if (msg instanceof APIGetCandidatePrimaryStoragesForCreatingVmMsg) {
            validate((APIGetCandidatePrimaryStoragesForCreatingVmMsg) msg);
        } else if (msg instanceof APIAttachL3NetworkToVmNicMsg) {
            validate((APIAttachL3NetworkToVmNicMsg) msg);
        } else if (msg instanceof APIDeleteVmCdRomMsg) {
            validate((APIDeleteVmCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmCdRomMsg) {
            validate((APIUpdateVmCdRomMsg) msg);
        } else if (msg instanceof APISetVmInstanceDefaultCdRomMsg) {
            validate((APISetVmInstanceDefaultCdRomMsg) msg);
        } else if (msg instanceof APICreateVmCdRomMsg) {
            validate((APICreateVmCdRomMsg) msg);
        } else if (msg instanceof APIUpdateVmNicDriverMsg) {
            validate((APIUpdateVmNicDriverMsg) msg);
        } else if (msg instanceof APIGetCandidateZonesClustersHostsForCreatingVmMsg) {
            validate((APIGetCandidateZonesClustersHostsForCreatingVmMsg) msg);
        } else if (msg instanceof APIFstrimVmMsg) {
            validate((APIFstrimVmMsg) msg);
        } else if (msg instanceof APITakeVmConsoleScreenshotMsg) {
            validate((APITakeVmConsoleScreenshotMsg) msg);
        } else if (msg instanceof APIGetVmUptimeMsg) {
            validate((APIGetVmUptimeMsg) msg);
        } else if (msg instanceof APIConvertVmInstanceToTemplatedVmInstanceMsg) {
            validate((APIConvertVmInstanceToTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APIConvertTemplatedVmInstanceToVmInstanceMsg) {
            validate((APIConvertTemplatedVmInstanceToVmInstanceMsg) msg);
        } else if (msg instanceof APIDeleteTemplatedVmInstanceMsg) {
            validate((APIDeleteTemplatedVmInstanceMsg) msg);
        } else if (msg instanceof APIRegisterVmInstanceFromMetadataMsg) {
            validate((APIRegisterVmInstanceFromMetadataMsg) msg);
        }

        if (msg instanceof NewVmInstanceMessage2) {
            vmInstanceHelper.validate((NewVmInstanceMessage2) msg);
        } else if (msg instanceof NewVmInstanceMessage) {
            vmInstanceHelper.validate((NewVmInstanceMessage) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIDeleteTemplatedVmInstanceMsg msg) {
        if (!dbf.isExist(msg.getUuid(), TemplatedVmInstanceVO.class)) {
            APIDeleteTemplatedVmInstanceEvent evt = new APIDeleteTemplatedVmInstanceEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIConvertTemplatedVmInstanceToVmInstanceMsg msg) {
        if (msg.getVmInstanceUuid() == null) {
            msg.setVmInstanceUuid(msg.getTemplatedVmInstanceUuid());
        }
    }

    private void validate(APIConvertVmInstanceToTemplatedVmInstanceMsg msg) {
        TemplatedVmInstanceVO templatedVm = Q.New(TemplatedVmInstanceVO.class)
                .eq(TemplatedVmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .find();
        if (templatedVm != null) {
            APIConvertVmInstanceToTemplatedVmInstanceEvent event = new APIConvertVmInstanceToTemplatedVmInstanceEvent(msg.getId());
            event.setInventory(TemplatedVmInstanceInventory.valueOf(templatedVm));
            bus.publish(event);
            throw new StopRoutingException();
        }

        boolean isTemplatedCache = Q.New(TemplatedVmInstanceCacheVO.class).eq(TemplatedVmInstanceCacheVO_.cacheVmInstanceUuid, msg.getVmInstanceUuid()).isExists();
        if (isTemplatedCache) {
            throw new ApiMessageInterceptionException(operr("templated vm cache[uuid:%s] cannot be convert to templated vm",
                    msg.getVmInstanceUuid()));
        }
    }

    private void validate(APIGetVmUptimeMsg msg) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!vm.getState().equals(VmInstanceState.Running)) {
            throw new ApiMessageInterceptionException(operr(
                    "can not take vm pid createTime for vm[uuid:%s] which is not Running", msg.getVmInstanceUuid()));
        }
    }

    private void validate(APITakeVmConsoleScreenshotMsg msg) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
        if (!vm.getState().equals(VmInstanceState.Running)) {
            throw new ApiMessageInterceptionException(operr(
                    "can not take vm console screenshot for vm[uuid:%s] which is not Running", msg.getVmInstanceUuid()));
        }
    }

    private void validate(APIGetInterdependentL3NetworksBackupStoragesMsg msg) {
        if (msg.getL3NetworkUuids() == null && msg.getBackupStorageUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("either l3NetworkUuids or backupStorageUuid must be set"));
        }
    }

    private void validate(APIGetCandidateL3NetworksForChangeVmNicNetworkMsg msg) {
        String vmUuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).select(VmNicVO_.vmInstanceUuid).findValue();
        msg.setVmInstanceUuid(vmUuid);
    }

    private void validate(APIChangeVmNicNetworkMsg msg) {
        List<Map<String, String>> networkServices = new ArrayList<>();
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();

        //l2 must use same vswitch
        long count  = SQL.New("select count(distinct l2.vSwitchType) from L2NetworkVO l2, L3NetworkVO l3 where l2.uuid = l3.l2NetworkUuid" +
                " and l3.uuid in :l3Uuids")
                .param("l3Uuids", Arrays.asList(nicVO.getL3NetworkUuid(), msg.getDestL3NetworkUuid()))
                .find();
        if (count > 1) {
            throw new ApiMessageInterceptionException(operr("could not change to L3 network, the l2 of l3[uuid:%s, %s] use different vswitch",
                    nicVO.getL3NetworkUuid(), msg.getDestL3NetworkUuid()));
        }
        for (VmNicChangeNetworkExtensionPoint extension : pluginRgty.getExtensionList(VmNicChangeNetworkExtensionPoint.class)) {
            Map<String, String> ret = extension.getVmNicAttachedNetworkService(VmNicInventory.valueOf(nicVO));
            if (ret == null) {
                continue;
            }
            networkServices.add(ret);
        }

        if (!networkServices.isEmpty()) {
            String error = "vm nic [%s] attached network services, please detach manually/n" + networkServices.toString();
            throw new ApiMessageInterceptionException(operr(error, msg.getVmNicUuid()));
        }

        String sql = "select vm.uuid, vm.state, vm.type, vm.hostUuid, vm.lastHostUuid, vm.platform, vm.defaultL3NetworkUuid" +
                " from VmInstanceVO vm, VmNicVO nic" +
                " where vm.uuid = nic.vmInstanceUuid and nic.uuid = :uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuid", msg.getVmNicUuid());
        Tuple t = q.getSingleResult();
        String vmUuid = t.get(0, String.class);
        VmInstanceState state = t.get(1, VmInstanceState.class);
        String type = t.get(2, String.class);
        String hostUuid = t.get(3, String.class) != null ? t.get(3, String.class) : t.get(4, String.class);
        String platform = t.get(5, String.class);
        String defaultL3Uuid = t.get(6, String.class);
        msg.setVmInstanceUuid(vmUuid);

        if (!VmInstanceState.Stopped.equals(state) && !VmInstanceState.Running.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    msg.getVmInstanceUuid(), state));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getDestL3NetworkUuid(), L3NetworkVO.class);
        if (l3NetworkVO.getEnableIPAM() && l3NetworkVO.getIpRanges().isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] doesn't has have ip range",
                    msg.getDestL3NetworkUuid()));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l3NetworkVO.getL2NetworkUuid())
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] are belonged to l2 network[uuid:%s] that have not been attached to any cluster",
                    msg.getDestL3NetworkUuid(), l3NetworkVO.getL2NetworkUuid()));
        }

         boolean attached = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmNicVO_.l3NetworkUuid, msg.getDestL3NetworkUuid())
                .count() > 0;
        if (attached) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        msg.getDestL3NetworkUuid(), msg.getVmInstanceUuid()));
            }

            if (!L3NetworkCategory.Private.equals(l3NetworkVO.getCategory())) {
                throw new ApiMessageInterceptionException(operr("unable to change to a non-guest L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        msg.getDestL3NetworkUuid(), msg.getVmInstanceUuid()));
            }
        }

        if (l3NetworkVO.getState() == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] is disabled", l3NetworkVO.getUuid()));
        }
        if (VmInstanceConstant.USER_VM_TYPE.equals(type) && l3NetworkVO.isSystem()) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] is a system network and vm is a user vm",
                    l3NetworkVO.getUuid()));
        }

        VmNicParam vmNicParam = null;
        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            try {
                vmNicParam = JSONObjectUtil.toObject(msg.getVmNicParams(), VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }
        }

        if (msg.getStaticIp() != null) {
            if (vmNicParam == null) {
                vmNicParam = new VmNicParam();
                vmNicParam.setL3NetworkUuid(msg.getDestL3NetworkUuid());
            }

            String ip = IPv6NetworkUtils.ipv6TagValueToAddress(msg.getStaticIp());
            if (NetworkUtils.getIpversion(ip) == IPv6Constants.IPv4) {
                vmNicParam.setIp(ip);
            } else {
                vmNicParam.setIp6(ip);
            }
        }

        if (vmNicParam != null) {
            new VmNicParamValidator().withVmNicParam(vmNicParam)
                    .withL3Uuid(msg.getDestL3NetworkUuid())
                    .withDefaultL3Uuid(defaultL3Uuid)
                    .withSupportNicDriverTypes(nicManager.getSupportNicDriverTypes())
                    .withVmType(type)
                    .isWindowsVm(ImagePlatform.Windows.toString().equals(platform))
                    .validate();
        }

        Map<String, NicIpAddressInfo> infoMap = ipOperator.validateStaticIpTagsInApiMessage(msg, msg.getVmInstanceUuid(),
                vmNicParam != null ? Collections.singletonList(vmNicParam) : null);
        msg.setRequiredIpMap(ipOperator.getStaticIpByNicIpAddressInfo(infoMap));

        L2NetworkType l2Type = L2NetworkType.valueOf(Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid())
                .select(L2NetworkVO_.type).findValue());
        if (!l2Type.isAttachToAllHosts() && !L2NetworkHostUtils.checkIfL2AttachedToHost(l3NetworkVO.getL2NetworkUuid(), hostUuid)) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network[uuid:%s]" +
                            " whose l2Network is not attached to the host[uuid:%s]", msg.getDestL3NetworkUuid(), hostUuid));
        }

    }

    private void validate(APIGetCandidateZonesClustersHostsForCreatingVmMsg msg) {
        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();

        if (instanceOfferingUuid == null) {
            if (msg.getCpuNum() == null || msg.getMemorySize() == null) {
                throw new ApiMessageInterceptionException(operr("Missing CPU/memory settings"));
            }
        }

        ImageVO image = dbf.findByUuid(msg.getImageUuid(), ImageVO.class);
        if (image != null && image.getMediaType() == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null || msg.getRootDiskSize() <= 0) {
                    throw new OperationFailureException(argerr("the image[name:%s, uuid:%s] is an ISO, rootDiskSize must be set",
                            image.getName(), image.getUuid()));
                }
            }
        }
    }

    private void validate(final APICreateVmCdRomMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (!vo.getState().equals(VmInstanceState.Stopped)) {
            throw new ApiMessageInterceptionException(argerr(
                    "Can not create CD-ROM for vm[uuid:%s] which is in state[%s] ", msg.getVmInstanceUuid(), vo.getState().toString()));
        }
    }

    private void validate(final APIUpdateVmNicDriverMsg msg) {
        VmInstanceVO vo = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        if (vo.getPlatform().equals(ImagePlatform.Other.toString())) {
            throw new ApiMessageInterceptionException(argerr(
                    "Current platform %s not support update nic driver yet", vo.getPlatform()));
        }

        List<String> supportNicDriverTypes = nicManager.getSupportNicDriverTypes();
        if (!supportNicDriverTypes.contains(msg.getDriverType())) {
            throw new ApiMessageInterceptionException(argerr(
                    "Nic driver %s not support yet", msg.getDriverType()));
        }

        if (vo.getState() != VmInstanceState.Stopped) {
            throw new ApiMessageInterceptionException(argerr("vm nic driver type can be updated only when the vm is stopped"));
        }
    }

    private void validate(final APIGetCandidatePrimaryStoragesForCreatingVmMsg msg) {
        ImageMediaType mediaType = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).select(ImageVO_.mediaType).findValue();
        if (ImageMediaType.ISO == mediaType) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null || msg.getRootDiskSize() <= 0) {
                    throw new ApiMessageInterceptionException(argerr("rootDiskSize is needed when image media type is ISO"));
                }
            }
        }
    }

    private void validate(APIMigrateVmMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
                if (vo.getState().equals(VmInstanceState.Running) && vo.getHostUuid().equals(msg.getHostUuid())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the vm[uuid:%s] is already on host[uuid:%s]", msg.getVmInstanceUuid(), msg.getHostUuid()
                    ));
                }

                if (vo.getState() == VmInstanceState.Paused && VmSystemTags.VM_STATE_PAUSED_AFTER_MIGRATE.hasTag(msg.getVmInstanceUuid())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the vm[uuid:%s] is still paused after the last migration, please resume it before migrate.", msg.getVmInstanceUuid()));
                }
            }
        }.execute();
    }

    private void validate(APIChangeInstanceOfferingMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                InstanceOfferingVO instanceOfferingVO = Q.New(InstanceOfferingVO.class).eq(InstanceOfferingVO_.uuid, msg.getInstanceOfferingUuid()).find();

                boolean numa = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, msg.getVmInstanceUuid(), Boolean.class);
                if (!numa && !VmInstanceState.Stopped.equals(vo.getState())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the VM cannot do online cpu/memory update because of disabling Instance Offering Online Modification. Please stop the VM then do the cpu/memory update again"
                    ));
                }

                if (!VmInstanceState.Stopped.equals(vo.getState()) && !VmInstanceState.Running.equals(vo.getState())) {
                    throw new OperationFailureException(operr("The state of vm[uuid:%s] is %s. Only these state[%s] is allowed to update cpu or memory.",
                            vo.getUuid(), vo.getState(),
                            StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ",")));
                }

                if (VmInstanceState.Stopped.equals(vo.getState())) {
                    return;
                }

                if (instanceOfferingVO.getCpuNum() < vo.getCpuNum() || instanceOfferingVO.getMemorySize() < vo.getMemorySize()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "can't decrease capacity when vm[uuid:%s] is running", vo.getUuid()
                    ));
                }
            }
        }.execute();
    }


    private void validate(APIUpdateVmInstanceMsg msg) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VmInstanceVO vo = q(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).find();
                if (msg.getReservedMemorySize() != null) {
                    Long memorySize = msg.getMemorySize() == null ? vo.getMemorySize() : msg.getMemorySize();
                    if (msg.getReservedMemorySize() > memorySize) {
                        throw new ApiMessageInterceptionException(argerr(
                                "reservedMemorySize[%s] is greater than memorySize[%s]", msg.getReservedMemorySize(), memorySize
                        ));
                    }
                }

                if (msg.getCpuNum() == null && msg.getMemorySize() == null) {
                    return;
                }

                Integer cpuSum = msg.getCpuNum();
                Long memorySize = msg.getMemorySize();

                VmInstanceState vmState = q(VmInstanceVO.class).select(VmInstanceVO_.state).eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue();
                boolean numa = rcf.getResourceConfigValue(VmGlobalConfig.NUMA, msg.getUuid(), Boolean.class);
                if (!numa && !VmInstanceState.Stopped.equals(vmState)) {
                    if (cpuSum != null && cpuSum != vo.getCpuNum()) {
                        throw new ApiMessageInterceptionException(argerr(
                                "the VM cannot do cpu hot plug because of disabling cpu hot plug. Please stop the VM then do the cpu hot plug again"
                        ));
                    }

                    if (memorySize != null && memorySize != vo.getMemorySize()) {
                        throw new ApiMessageInterceptionException(argerr(
                                "the VM cannot do memory hot plug because of disabling memory hot plug. Please stop the VM then do the memory hot plug again"
                        ));
                    }
                }

                if (!VmInstanceState.Stopped.equals(vo.getState()) && !VmInstanceState.Running.equals(vo.getState())) {
                    throw new OperationFailureException(operr("The state of vm[uuid:%s] is %s. Only these state[%s] is allowed to update cpu or memory.",
                            vo.getUuid(), vo.getState(),
                            StringUtils.join(list(VmInstanceState.Running, VmInstanceState.Stopped), ",")));
                }

                if (VmInstanceState.Stopped.equals(vmState)) {
                    return;
                }

                if (msg.getCpuNum() != null && msg.getCpuNum() < vo.getCpuNum()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "can't decrease cpu of vm[uuid:%s] when it is running", vo.getUuid()
                    ));
                }

                if (msg.getMemorySize() != null && msg.getMemorySize() < vo.getMemorySize()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "can't decrease memory size of vm[uuid:%s] when it is running", vo.getUuid()
                    ));
                }
            }
        }.execute();

        if (msg.getAllocatorStrategy() != null && !HostAllocatorStrategyType.hasType(msg.getAllocatorStrategy())) {
            throw new ApiMessageInterceptionException(
                    argerr("unsupported host allocation strategy[%s]", msg.getAllocatorStrategy()));
        }
    }

    private void validate(APIStartVmInstanceMsg msg) {
        // host uuid overrides cluster uuid
        if (msg.getHostUuid() != null) {
            msg.setClusterUuid(null);
        }
    }

    private void validate(APISetVmStaticIpMsg msg) {
        L3NetworkVO l3NetworkVO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, msg.getL3NetworkUuid()).find();
        if (msg.getIp() == null && msg.getIp6() == null) {
            throw new ApiMessageInterceptionException(argerr("could not set ip address, due to no ip address is specified"));
        }

        if (l3NetworkVO.getEnableIPAM()) {
            if (msg.getIp() != null && msg.getIp().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("ipv4 address cannot be empty when l3 is IPAM enabled"));
            } else if (msg.getIp6() != null && msg.getIp6().isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("ipv6 address cannot be empty when l3 is IPAM enabled"));
            }
        }

        NicIpAddressInfo info = new NicIpAddressInfo();
        info.ipv4Address = msg.getIp();
        info.ipv4Netmask = msg.getNetmask();
        info.ipv4Gateway = msg.getGateway();
        info.ipv6Address = msg.getIp6();
        if (msg.getIpv6Prefix() != null) {
            try {
                info.ipv6Prefix = Integer.valueOf(msg.getIpv6Prefix());
            } catch (NumberFormatException e) {
                throw new ApiMessageInterceptionException(argerr("ipv6 prefix must be a number, but got [%s]", msg.getIpv6Prefix()));
            }
        }
        info.ipv6Gateway = msg.getIpv6Gateway();

        ipOperator.validateStaticIp(info, l3NetworkVO, new ArrayList<>());
        msg.setNetmask(info.ipv4Netmask);
        msg.setGateway(info.ipv4Gateway);
        msg.setIp6(info.ipv6Address);
        if (info.ipv6Prefix != null) {
            msg.setIpv6Prefix(info.ipv6Prefix.toString());
        }
        msg.setIpv6Gateway(info.ipv6Gateway);
    }

    private void validate(APIDeleteVmStaticIpMsg msg) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.vmInstanceUuid, Op.EQ, msg.getVmInstanceUuid());
        q.add(VmNicVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("the VM[uuid:%s] has no nic on the L3 network[uuid:%s]", msg.getVmInstanceUuid(),
                            msg.getL3NetworkUuid()));
        }

        if (msg.getStaticIp() != null) {
            if (!Q.New(UsedIpVO.class).eq(UsedIpVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(UsedIpVO_.ip, msg.getStaticIp()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could not delete static ip [%s] for vm [uuid:%s] " +
                                "because it does not exist", msg.getStaticIp(), msg.getVmInstanceUuid()));
            }
        }
    }

    private void validate(APIGetVmDnsMsg msg) {
        boolean isWindowsVm = ImagePlatform.Windows.toString().equals(Q.New(VmInstanceVO.class).select(VmInstanceVO_.platform)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue());

        validateDnsMsg(msg.getVmInstanceUuid(), msg.getVmNicUuid(), msg.getIpVersion(), isWindowsVm);
    }

    private void validate(APISetVmDnsMsg msg) {
        boolean isWindowsVm = ImagePlatform.Windows.toString().equals(Q.New(VmInstanceVO.class).select(VmInstanceVO_.platform)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid()).findValue());

        validateDnsMsg(msg.getVmInstanceUuid(), msg.getVmNicUuid(), msg.getIpVersion(), isWindowsVm);

        if (isWindowsVm) {
            if (msg.getDnsList().size() > 2) {
                throw new ApiMessageInterceptionException(argerr("size of dns list for Windows vm should not exceed 2"));
            }

            for (String dns: msg.getDnsList()) {
                if (!Objects.equals(NetworkUtils.getIpversion(dns), msg.getIpVersion())) {
                    throw new ApiMessageInterceptionException(argerr("dns[%s] should be ipv%s address", dns, msg.getIpVersion()));
                }
            }
        } else {
            if (msg.getDnsList().size() > 3) {
                throw new ApiMessageInterceptionException(argerr("size of dns list should not exceed 3"));
            }

            for (String dns: msg.getDnsList()) {
                if (NetworkUtils.isNotIpAddress(dns)) {
                    throw new ApiMessageInterceptionException(argerr("dns[%s] is not a IP address", dns));
                }
            }
        }

        if (msg.getDnsList().size() > msg.getDnsList().stream().distinct().count()) {
            throw new ApiMessageInterceptionException(argerr("duplicate dns in dns list %s", msg.getDnsList()));
        }
    }

    private void validateDnsMsg(String vmInstanceUuid, String vmNicUuid, Integer ipVersion, boolean isWindowsVm) {
        if (isWindowsVm) {
            if (vmNicUuid == null) {
                throw new ApiMessageInterceptionException(argerr("vmNicUuid should be set for Windows vm"));
            }
            if (ipVersion == null) {
                throw new ApiMessageInterceptionException(argerr("ip version should be set for Windows vm"));
            }

            boolean isConsistent = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, vmNicUuid)
                    .eq(VmNicVO_.vmInstanceUuid, vmInstanceUuid).isExists();
            if (!isConsistent) {
                throw new ApiMessageInterceptionException(argerr("vmNicUuid[%s] is not consistent with vmInstanceUuid[%s]",
                        vmNicUuid, vmInstanceUuid));
            }
        } else {
            if (vmNicUuid != null) {
                throw new ApiMessageInterceptionException(argerr("vmNicUuid should not be set for non-Windows vm"));
            }
            if (ipVersion != null) {
                throw new ApiMessageInterceptionException(argerr("ip version should not be set for non-Windows vm"));
            }
        }
    }

    private void validate(APISetVmBootOrderMsg msg) {
        if (msg.getBootOrder() != null) {
            for (String o : msg.getBootOrder()) {
                try {
                    VmBootDevice.valueOf(o);
                } catch (IllegalArgumentException e) {
                    throw new ApiMessageInterceptionException(argerr("invalid boot device[%s] in boot order%s", o, msg.getBootOrder()));
                }
            }
        }
    }

    private boolean isVmHasMemorySnapshotGroup(String vmUuid) {
        List<String> snapShotGroupUuids = Q.New(VmInstanceResourceMetadataGroupVO.class)
                .select(VmInstanceResourceMetadataGroupVO_.resourceUuid)
                .eq(VmInstanceResourceMetadataGroupVO_.vmInstanceUuid, vmUuid)
                .listValues();
        if (snapShotGroupUuids.isEmpty()) {
            return false;
        }
        return Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid)
                .in(VolumeSnapshotGroupVO_.uuid, snapShotGroupUuids)
                .isExists();
    }

    private void validate(APISetVmBootVolumeMsg msg) {
        VolumeVO volume = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, msg.getVolumeUuid()).find();
        if (volume.isShareable()) {
            throw new ApiMessageInterceptionException(argerr("boot volume cannot be shareable."));
        }

        if (!msg.getVmInstanceUuid().equals(volume.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(argerr("volume[uuid:%s] must be attached to vm[uuid:%s]",
                    msg.getVolumeUuid(), msg.getVmInstanceUuid()));
        }

        if (isVmHasMemorySnapshotGroup(msg.getVmInstanceUuid())) {
            throw new ApiMessageInterceptionException(argerr("the vm %s with memory snapshots do not support setting boot volume", msg.getVmInstanceUuid()));
        }
    }


    private void validate(APIAttachIsoToVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        if (isoUuids.contains(msg.getIsoUuid())) {
            throw new ApiMessageInterceptionException(operr("VM[uuid:%s] already has an ISO[uuid:%s] attached", msg.getVmInstanceUuid(), msg.getIsoUuid()));
        }

        ImageMediaType type = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getIsoUuid()).select(ImageVO_.mediaType).findValue();
        if (type != ImageMediaType.ISO) {
            throw new ApiMessageInterceptionException(argerr("Unsupported Image Media Type: [%s] ", type));
        }

        validateCdRomUuid(msg);
    }

    private void validateCdRomUuid(APIAttachIsoToVmInstanceMsg msg) {
        if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        String cdRomUuid = SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.CD_ROM, VmSystemTags.CD_ROM_UUID_TOKEN);
        if (cdRomUuid != null) {
            VmCdRomVO cdRomVO = dbf.findByUuid(cdRomUuid, VmCdRomVO.class);
            if (cdRomVO == null) {
                throw new ApiMessageInterceptionException(operr("The cdRom[uuid:%s] does not exist", cdRomUuid));
            }

            if (StringUtils.isNotEmpty(cdRomVO.getIsoUuid())){
                throw new ApiMessageInterceptionException(operr("VM[uuid:%s] cdRom[uuid:%s] has mounted the ISO", msg.getVmInstanceUuid(), cdRomUuid));
            }

            msg.setCdRomUuid(cdRomUuid);
        }
    }

    private void fillIsoUuid(APIDetachIsoFromVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());
        if(isoUuids.size() == 1) {
            msg.setIsoUuid(isoUuids.get(0));
        }
    }

    private void validate(APIDetachIsoFromVmInstanceMsg msg) {
        List<String> isoUuids = IsoOperator.getIsoUuidByVmUuid(msg.getVmInstanceUuid());

        if (isoUuids.size() > 1 && msg.getIsoUuid() == null) {
            throw new ApiMessageInterceptionException(operr("VM[uuid:%s] has multiple ISOs attached, specify the isoUuid when detaching", msg.getVmInstanceUuid()));
        }

        if (msg.getIsoUuid() == null) {
            fillIsoUuid(msg);
        }
    }

    private void validate(APICreateVmNicMsg msg) {
        L3NetworkVO l3 = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (l3.getState() == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is disabled", msg.getL3NetworkUuid()));
        }

        if (msg.getIp() != null) {
            ipOperator.checkIpAvailability(l3, msg.getIp());
        }
    }

    private void validate(APIAttachL3NetworkToVmMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.type, VmInstanceVO_.state, VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid,
                VmInstanceVO_.platform, VmInstanceVO_.defaultL3NetworkUuid);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        Tuple t = q.findTuple();
        String type = t.get(0, String.class);
        VmInstanceState state = t.get(1, VmInstanceState.class);
        String hostUuid = t.get(2, String.class) != null ? t.get(2, String.class) : t.get(3, String.class);
        String platform = t.get(4, String.class);
        String defaultL3Uuid = t.get(5, String.class);

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                            msg.getVmInstanceUuid(), state));
        }

        L3NetworkVO l3NetworkVO = dbf.findByUuid(msg.getL3NetworkUuid(), L3NetworkVO.class);
        if (l3NetworkVO.getEnableIPAM() && l3NetworkVO.getIpRanges().isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] doesn't has have ip range",
                    msg.getL3NetworkUuid()));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l3NetworkVO.getL2NetworkUuid())
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to change to L3 network. The L3 network[uuid:%s] are belonged to l2 network[uuid:%s] that have not been attached to any cluster",
                    msg.getL3NetworkUuid(), l3NetworkVO.getL2NetworkUuid()));
        }

        boolean attached = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .eq(VmNicVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                .count() > 0;
        if (attached) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        msg.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }

            if (!L3NetworkCategory.Private.equals(l3NetworkVO.getCategory())) {
                throw new ApiMessageInterceptionException(operr("unable to attach a non-guest L3 network. The L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        msg.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }
        }

        if (l3NetworkVO.getState() == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is disabled", l3NetworkVO.getUuid()));
        }
        if (VmInstanceConstant.USER_VM_TYPE.equals(type) && l3NetworkVO.isSystem()) {
            throw new ApiMessageInterceptionException(operr("unable to attach a L3 network. The L3 network[uuid:%s] is a system network and vm is a user vm",
                    l3NetworkVO.getUuid()));
        }

        VmNicParam vmNicParam = null;
        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            try {
                vmNicParam = JSONObjectUtil.toObject(msg.getVmNicParams(), VmNicParam.class);
                if (msg.getDriverType() == null) {
                    msg.setDriverType(vmNicParam.getDriverType());
                } else if (vmNicParam.getDriverType() == null) {
                    vmNicParam.setDriverType(msg.getDriverType());
                    msg.setVmNicParams(JSONObjectUtil.toJsonString(vmNicParam));
                }
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }
        }

        if (msg.getStaticIp() != null) {
            if (vmNicParam == null) {
                vmNicParam = new VmNicParam();
                vmNicParam.setL3NetworkUuid(msg.getL3NetworkUuid());
            }

            String ip = IPv6NetworkUtils.ipv6TagValueToAddress(msg.getStaticIp());
            if (NetworkUtils.getIpversion(ip) == IPv6Constants.IPv4) {
                vmNicParam.setIp(ip);
            } else {
                vmNicParam.setIp6(ip);
            }
        }

        if (vmNicParam != null) {
            List<VmNicParam> vmNicParams = new ArrayList<>(Collections.singletonList(vmNicParam));
            List<String> l3Uuids = new ArrayList<>(Collections.singletonList(msg.getL3NetworkUuid()));
            List<VmNicVO> attachedNics = Q.New(VmNicVO.class)
                    .eq(VmNicVO_.l3NetworkUuid, msg.getL3NetworkUuid())
                    .eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid()).list();
            for (VmNicVO nic : attachedNics) {
                if (VmNicType.valueOf(nic.getType()).isUseSRIOV()){
                    VmNicParam attachedNicParam = new VmNicParam();
                    attachedNicParam.setL3NetworkUuid(nic.getL3NetworkUuid());
                    attachedNicParam.setDriverType(nic.getDriverType());
                    vmNicParams.add(attachedNicParam);
                    l3Uuids.add(nic.getL3NetworkUuid());
                }
            }

            new VmNicParamValidator().withVmNicParams(vmNicParams)
                    .withL3Uuids(l3Uuids)
                    .withDefaultL3Uuid(defaultL3Uuid)
                    .withSupportNicDriverTypes(nicManager.getSupportNicDriverTypes())
                    .withVmType(type)
                    .isWindowsVm(ImagePlatform.Windows.toString().equals(platform))
                    .validate();
        }

        Map<String, NicIpAddressInfo> infoMap = ipOperator.validateStaticIpTagsInApiMessage(msg, msg.getVmInstanceUuid(),
                vmNicParam != null ? Collections.singletonList(vmNicParam) : null);

        msg.setNicNetworkInfo(infoMap);
        msg.setStaticIpMap(ipOperator.getStaticIpByNicIpAddressInfo(infoMap));

        L2NetworkType l2Type = L2NetworkType.valueOf(Q.New(L2NetworkVO.class)
                .eq(L2NetworkVO_.uuid, l3NetworkVO.getL2NetworkUuid())
                .select(L2NetworkVO_.type).findValue());
        if (!l2Type.isAttachToAllHosts() && !L2NetworkHostUtils.checkIfL2AttachedToHost(l3NetworkVO.getL2NetworkUuid(), hostUuid)) {
            throw new ApiMessageInterceptionException(operr("unable to attach L3 network[uuid:%s] to VM[uuid:%s]" +
                    " whose l2Network is not attached to the host[uuid:%s]",
                    msg.getL3NetworkUuid(), msg.getVmInstanceUuid(), hostUuid));
        }
    }

    private void validate(APIAttachVmNicToVmMsg msg) {
        VmInstanceVO vmInstanceVO = dbf.findByUuid(msg.getVmInstanceUuid(), VmInstanceVO.class);
        String type = vmInstanceVO.getType();
        VmInstanceState state = vmInstanceVO.getState();

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    msg.getVmInstanceUuid(), state));
        }

        VmNicVO vmNicVO = dbf.findByUuid(msg.getVmNicUuid(), VmNicVO.class);

        if (vmNicVO.getVmInstanceUuid() != null) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. The nic has been attached with vm[uuid: %s]", vmNicVO.getVmInstanceUuid()));
        }

        boolean exist = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, vmNicVO.getL3NetworkUuid())
                .eq(VmNicVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .isExists();
        L3NetworkVO l3NetworkVO = dbf.findByUuid(vmNicVO.getL3NetworkUuid(), L3NetworkVO.class);
        if (exist) {
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class)
                    || !VmInstanceConstant.USER_VM_TYPE.equals(type)) {
                throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        vmNicVO.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }

            if (!L3NetworkCategory.Private.equals(l3NetworkVO.getCategory())) {
                throw new ApiMessageInterceptionException(operr("unable to attach the nic with a non-guest L3 network. Its L3 network[uuid:%s] is already attached to the vm[uuid: %s]",
                        vmNicVO.getL3NetworkUuid(), msg.getVmInstanceUuid()));
            }
        }

        L3NetworkState l3state = l3NetworkVO.getState();
        boolean system = l3NetworkVO.isSystem();

        if (l3state == L3NetworkState.Disabled) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its L3 network[uuid:%s] is disabled", l3NetworkVO.getUuid()));
        }
        if (VmInstanceConstant.USER_VM_TYPE.equals(type) && system) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its L3 network[uuid:%s] is a system network and vm is a user vm",
                    l3NetworkVO.getUuid()));
        }

        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, l3NetworkVO.getL2NetworkUuid())
                                     .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("unable to attach the nic. Its l2 network [uuid:%s] that have not been attached to any cluster",
                    l3NetworkVO.getL2NetworkUuid()));
        }
    }

    @Transactional(readOnly = true)
    private void validate(APIChangeVmNicStateMsg msg) {
        VmNicVO nicVO = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        if (msg.getState().equals(VmNicState.enable.toString())) {
            MacOperator mo = new MacOperator();
            if (mo.checkDuplicateMac(nicVO.getHypervisorType(), nicVO.getMac())) {
                throw new ApiMessageInterceptionException(argerr("Duplicate mac address [%s]", nicVO.getMac()));
            }
        }

        if (!nicVO.getType().equals(VmInstanceConstant.VIRTUAL_NIC_TYPE)) {
            throw new ApiMessageInterceptionException(operr("could not update nic[uuid: %s] state, due to nic type[%s] not support",
                    msg.getVmNicUuid(), nicVO.getType()));
        }
        msg.setVmInstanceUuid(nicVO.getVmInstanceUuid());
        msg.l3Uuid = nicVO.getL3NetworkUuid();
    }

    @Transactional(readOnly = true)
    private void validate(APIDetachL3NetworkFromVmMsg msg) {
        String sql = "select vm.uuid, vm.state from VmInstanceVO vm, VmNicVO nic where vm.uuid = nic.vmInstanceUuid and nic.uuid = :uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("uuid", msg.getVmNicUuid());
        Tuple t = q.getSingleResult();
        String vmUuid = t.get(0, String.class);
        VmInstanceState state = t.get(1, VmInstanceState.class);

        if (!VmInstanceState.Running.equals(state) && !VmInstanceState.Stopped.equals(state)) {
            throw new ApiMessageInterceptionException(operr("unable to detach a L3 network. The vm[uuid: %s] is not Running or Stopped; the current state is %s",
                    vmUuid, state));
        }

        msg.setVmInstanceUuid(vmUuid);

        msg.l3Uuid = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).select(VmNicVO_.l3NetworkUuid).findValue();
    }

    private void validate(APIGetVmAttachableDataVolumeMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (state != VmInstanceState.Stopped && state != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr("vm[uuid:%s] can only attach volume when state is Running or Stopped, current state is %s", msg.getVmInstanceUuid(), state));
        }
    }

    private void validateRootDiskOffering(ImageMediaType imgFormat, APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (imgFormat == ImageMediaType.ISO) {
            if (msg.getRootDiskOfferingUuid() == null) {
                if (msg.getRootDiskSize() == null) {
                    throw new ApiMessageInterceptionException(argerr("image mediaType is ISO but missing root disk settings"));
                }

                if (msg.getRootDiskSize() <= 0) {
                    throw new ApiMessageInterceptionException(operr("Unexpected root disk settings")
                            .withException("DiskAO[0].size is mandatory when image format is ISO"));
                }
            }
        }
    }

    private void validatePsWhetherSameCluster(APICreateVmInstanceMsg msg) {
        if (msg.getPrimaryStorageUuidForRootVolume() == null || msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        String primaryStorageUuidForDataVolume = SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
        if (primaryStorageUuidForDataVolume == null) {
            return;
        }

        List<String> clusterUuidsForRootVolume = Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.clusterUuid).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, msg.getPrimaryStorageUuidForRootVolume()).listValues();
        List<String> clusterUuidsForDataVolume = Q.New(PrimaryStorageClusterRefVO.class).select(PrimaryStorageClusterRefVO_.clusterUuid).eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, primaryStorageUuidForDataVolume).listValues();

        clusterUuidsForRootVolume.retainAll(clusterUuidsForDataVolume);
        if (clusterUuidsForRootVolume.isEmpty()) {
            throw new ApiMessageInterceptionException(operr("the primary storage[%s] of the root volume and the primary storage[%s] of the data volume are not in the same cluster", msg.getPrimaryStorageUuidForRootVolume(), primaryStorageUuidForDataVolume));
        }
    }

    private void validateDataDiskSizes(APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (CollectionUtils.isEmpty(msg.getDataDiskSizes())) {
            return;
        }
        msg.getDataDiskSizes().forEach(dataDiskSize -> {
            if (dataDiskSize <= 0) {
                throw new ApiMessageInterceptionException(operr("Unexpected data disk settings. dataDiskSizes need to be greater than 0"));
            }
        });
    }

    private void validate(APICreateVmInstanceMsg msg) {
        boolean virtIOTagExists = (isEmpty(msg.getSystemTags())) ? false :
                msg.getSystemTags().contains(VmSystemTags.VIRTIO.getTagFormat());
        String platform = msg.getPlatform(), guestOsType = msg.getGuestOsType(), architecture = msg.getArchitecture();
        long rootDiskSize = msg.getRootDiskSize() != null ? msg.getRootDiskSize() : 0L;

        if (CollectionUtils.isNotEmpty(msg.getDiskAOs())) {
            DiskAO rootDiskAO = isEmpty(msg.getDiskAOs()) ? null : findOneOrNull(msg.getDiskAOs(), DiskAO::isBoot);
            if (rootDiskAO == null) {
                throw new ApiMessageInterceptionException(argerr("missing root disk"));
            }

            platform = platform == null ? rootDiskAO.getPlatform() : platform;
            guestOsType = guestOsType == null ? rootDiskAO.getGuestOsType() : guestOsType;
            architecture = architecture == null ? rootDiskAO.getArchitecture() : architecture;
            rootDiskSize = rootDiskSize == 0L ? rootDiskAO.getSize() : rootDiskSize;
            msg.setRootDiskSize(rootDiskSize);

            if (!virtIOTagExists && CollectionUtils.isNotEmpty(rootDiskAO.getSystemTags())) {
                // "driver::virtio" is tag for VmInstanceVO (not for VolumeVO)
                virtIOTagExists = rootDiskAO.getSystemTags().remove(VmSystemTags.VIRTIO.getTagFormat());
            }

            // root disk must be the first
            msg.getDiskAOs().remove(rootDiskAO);
            msg.getDiskAOs().add(0, rootDiskAO);
        }

        if (virtIOTagExists && msg.getVirtio() == Boolean.FALSE) {
            throw new ApiMessageInterceptionException(argerr("virtio tag is not allowed when virtio is false"));
        } else if (virtIOTagExists) {
            msg.setVirtio(true);
        }

        ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).find();
        if (image == null) {
            List<String> errorList = new ArrayList<>();
            if (platform == null) {
                errorList.add(Platform.missingVariables("platform"));
            }

            if (guestOsType == null) {
                errorList.add(Platform.missingVariables("guestOsType"));
            }

            if (architecture == null) {
                errorList.add(Platform.missingVariables("architecture"));
            }

            if (msg.getRootDiskOfferingUuid() == null && rootDiskSize <= 0) {
                errorList.add("rootDiskOfferingUuid or rootDiskSize cannot be all null");
            }

            if (!errorList.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr(
                        String.format("when imageUuid is null, %s", String.join(", ", errorList))));
            }
        } else {
            ImageState imgState = image.getState();
            if (imgState == ImageState.Disabled) {
                throw new ApiMessageInterceptionException(operr("image[uuid:%s] is Disabled, can't create vm from it", msg.getImageUuid()));
            }

            ImageStatus imgStatus = image.getStatus();
            if (imgStatus != ImageStatus.Ready) {
                throw new ApiMessageInterceptionException(operr("image[uuid:%s] is not ready yet, can't create vm from it", msg.getImageUuid()));
            }

            ImageMediaType imgFormat = image.getMediaType();
            if (imgFormat != ImageMediaType.RootVolumeTemplate && imgFormat != ImageMediaType.ISO) {
                throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is of mediaType: %s, only RootVolumeTemplate and ISO can be used to create vm", msg.getImageUuid(), imgFormat));
            }

            boolean isSystemImage = image.isSystem();
            if (isSystemImage && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                throw new ApiMessageInterceptionException(argerr("image[uuid:%s] is system image, can't be used to create user vm", msg.getImageUuid()));
            }

            if (platform == null && image.getPlatform() == null) {
                throw new ApiMessageInterceptionException(operr("at least one of field platform in msg or image[uuid:%s] should be set", msg.getImageUuid()));
            } else if (platform == null) {
                platform = image.getPlatform().name();
            }

            if (guestOsType == null && image.getGuestOsType() == null) {
                throw new ApiMessageInterceptionException(operr("at least one of field guestOsType in msg or image[uuid:%s] should be set", msg.getImageUuid()));
            } else if (guestOsType == null) {
                guestOsType = image.getGuestOsType();
            }

            if (architecture == null && image.getArchitecture() == null) {
                throw new ApiMessageInterceptionException(operr("at least one of field architecture in msg or image[uuid:%s] should be set", msg.getImageUuid()));
            } else if (architecture == null) {
                architecture = image.getArchitecture();
            }

            validateRootDiskOffering(imgFormat, msg);
        }

        msg.setPlatform(platform);
        msg.setGuestOsType(guestOsType);
        msg.setArchitecture(architecture);
        if (msg.getVirtio() == null) {
            msg.setVirtio(ImagePlatform.Linux.name().equals(platform));
        }

        validateDataDiskSizes(msg);

        List<String> allDiskOfferingUuids = new ArrayList<String>();
        if (msg.getRootDiskOfferingUuid() != null) {
            allDiskOfferingUuids.add(msg.getRootDiskOfferingUuid());
        }
        if (msg.getDataDiskOfferingUuids() != null) {
            allDiskOfferingUuids.addAll(msg.getDataDiskOfferingUuids());
        }

        if (!allDiskOfferingUuids.isEmpty()) {
            SimpleQuery<DiskOfferingVO> dq = dbf.createQuery(DiskOfferingVO.class);
            dq.select(DiskOfferingVO_.uuid);
            dq.add(DiskOfferingVO_.state, Op.EQ, DiskOfferingState.Disabled);
            dq.add(DiskOfferingVO_.uuid, Op.IN, allDiskOfferingUuids);
            List<String> diskUuids = dq.listValue();
            if (!diskUuids.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("disk offerings[uuids:%s] are Disabled, can not create vm from it", diskUuids));
            }
        }

        validatePsWhetherSameCluster(msg);
        validateDataDiskAOs(msg);

        if (msg.getAllocatorStrategy() != null && !HostAllocatorStrategyType.hasType(msg.getAllocatorStrategy())) {
            throw new ApiMessageInterceptionException(
                    argerr("unsupported host allocation strategy[%s]", msg.getAllocatorStrategy()));
        }
    }

    private void validateDataDiskAOs(APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }
        for (DiskAO diskAO : msg.getDiskAOs()) {
            if (diskAO.isBoot()) {
                continue;
            }
            checkMutualExclusion(diskAO);
        }
    }

    public void checkMutualExclusion(DiskAO diskAO) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("size", diskAO.getSize() > 0);
        map.put("templateUuid", diskAO.getTemplateUuid() != null);
        map.put("diskOfferingUuid", diskAO.getDiskOfferingUuid() != null);
        map.put("sourceUuid", diskAO.getSourceUuid() != null);
        List<String> invalidProperties = map.entrySet().stream()
                .filter(entry -> entry.getValue() == Boolean.TRUE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (invalidProperties.size() == 1) {
            return;
        } else if (invalidProperties.size() > 1) {
            String invalidPropertiesText = String.join(", ", invalidProperties);
            throw new ApiMessageInterceptionException(operr("cannot set the following properties at the same time: %s", invalidPropertiesText));
        }

        StringJoiner properties = new StringJoiner(", ");
        for (String key : map.keySet()) {
            properties.add(key);
        }
        throw new ApiMessageInterceptionException(operr("Need to set one of the following properties, and can only be one of them: %s", properties));
    }

    private void validate(APICreateVmInstanceFromVolumeMsg msg) {
        VolumeVO volume = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        if (volume.isShareable()) {
            throw new ApiMessageInterceptionException(operr("cannot create vm instance from a shareable volume."));
        }

        if (volume.isAttached()) {
            throw new ApiMessageInterceptionException(operr("could not create vm instance from a attached volume."));
        }

        if (volume.getStatus() != VolumeStatus.Ready || volume.getState() != VolumeState.Enabled) {
            throw new ApiMessageInterceptionException(operr("volume[uuid:%s] could not satisfy conditions[state:Enabled status:Ready]", msg.getVolumeUuid()));
        }

        if (msg.getPlatform() == null) {
            String vmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid)
                    .eq(VolumeVO_.uuid, msg.getVolumeUuid()).findValue();
            String platform = Q.New(VmInstanceVO.class).select(VmInstanceVO_.platform)
                    .eq(VmInstanceVO_.uuid, vmUuid).findValue();
            msg.setPlatform(platform);
        }
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotMsg msg) {
        if (msg.getPlatform() == null) {
            String volumeUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.volumeUuid)
                    .eq(VolumeSnapshotVO_.uuid, msg.getVolumeSnapshotUuid()).findValue();
            String vmUuid = Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid)
                    .eq(VolumeVO_.uuid, volumeUuid).findValue();
            String platform = Q.New(VmInstanceVO.class).select(VmInstanceVO_.platform)
                    .eq(VmInstanceVO_.uuid, vmUuid).findValue();
            msg.setPlatform(platform);
        }
    }

    private void validate(APICreateVmInstanceFromVolumeSnapshotGroupMsg msg) {
        String vmInstanceUuid = Q.New(VolumeSnapshotGroupVO.class).select(VolumeSnapshotGroupVO_.vmInstanceUuid)
                .eq(VolumeSnapshotGroupVO_.uuid, msg.getVolumeSnapshotGroupUuid()).findValue();
        if (vmInstanceUuid == null) {
            return;
        }

        String platform = Q.New(VmInstanceVO.class).select(VmInstanceVO_.platform)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid).findValue();

        msg.setPlatform(platform);
    }

    private void validate(APIDestroyVmInstanceMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VmInstanceVO.class)) {
            APIDestroyVmInstanceEvent evt = new APIDestroyVmInstanceEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APISetVmConsolePasswordMsg msg) {
        String pwd = msg.getConsolePassword();
        if (pwd.startsWith("password")){
            throw new ApiMessageInterceptionException(argerr("The console password cannot start with 'password' which may trigger a VNC security issue"));
        }
    }

    private void validate(APIAttachL3NetworkToVmNicMsg msg) {
        throw new ApiMessageInterceptionException(argerr("can not call this api because it's Deprecated"));
    }

    private void validate(APIDeleteVmCdRomMsg msg) {
        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);
        msg.setVmInstanceUuid(vmCdRomVO.getVmInstanceUuid());
    }

    private void validate(APIUpdateVmCdRomMsg msg) {
        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);
        msg.setVmInstanceUuid(vmCdRomVO.getVmInstanceUuid());
    }

    private void validate(APISetVmInstanceDefaultCdRomMsg msg) {
        VmCdRomVO vmCdRomVO = dbf.findByUuid(msg.getUuid(), VmCdRomVO.class);

        if (vmCdRomVO.getDeviceId() == 0) {
            throw new ApiMessageInterceptionException(argerr("The CdRom[%s] Already the default", vmCdRomVO.getUuid()));
        }
    }

    private void validate(APIFstrimVmMsg msg) {
        Tuple t = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, msg.getUuid())
                .select(VmInstanceVO_.state, VmInstanceVO_.hostUuid)
                .findTuple();
        VmInstanceState state = t.get(0, VmInstanceState.class);

        if (state != VmInstanceState.Running) {
            throw new ApiMessageInterceptionException(operr(
                    "vm[uuid:%s] can only fstrim when state is Running, current state is %s", msg.getUuid(), state));
        }
        msg.setHostUuid(t.get(1, String.class));
    }

    private void validate(APIRegisterVmInstanceFromMetadataMsg msg) {
        String path = msg.getMetadataPath();
        if (StringUtils.isEmpty(path)) {
            throw new ApiMessageInterceptionException(argerr("metadataPath cannot be empty or null"));
        }

        // Delegate path validation to the storage-type-specific extension
        String psUuid = msg.getPrimaryStorageUuid();
        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, psUuid).findValue();
        if (psType == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "primary storage[uuid:%s] not found", psUuid));
        }

        VmMetadataPathBuildExtensionPoint ext = pluginRgty.getExtensionFromMap(psType, VmMetadataPathBuildExtensionPoint.class);
        if (ext == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "primary storage[uuid:%s, type:%s] does not support vm metadata", psUuid, psType));
        }

        String error = ext.validateMetadataPath(psUuid, path);
        if (error != null) {
            throw new ApiMessageInterceptionException(argerr("%s", error));
        }

        // Validate cluster belongs to the specified zone
        if (msg.getZoneUuid() != null) {
            String clusterZoneUuid = Q.New(ClusterVO.class).select(ClusterAO_.zoneUuid)
                    .eq(ClusterAO_.uuid, msg.getClusterUuid()).findValue();
            if (!msg.getZoneUuid().equals(clusterZoneUuid)) {
                throw new ApiMessageInterceptionException(argerr(
                        "cluster[uuid:%s] does not belong to zone[uuid:%s]",
                        msg.getClusterUuid(), msg.getZoneUuid()));
            }
        }

        boolean psAttachedToCluster = Q.New(PrimaryStorageClusterRefVO.class)
                .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuid)
                .eq(PrimaryStorageClusterRefVO_.clusterUuid, msg.getClusterUuid())
                .isExists();
        if (!psAttachedToCluster) {
            throw new ApiMessageInterceptionException(argerr(
                    "primary storage[uuid:%s] is not attached to cluster[uuid:%s]",
                    psUuid, msg.getClusterUuid()));
        }

        if (msg.getHostUuid() != null) {
            boolean hostAvailable = Q.New(HostVO.class)
                    .eq(HostVO_.uuid, msg.getHostUuid())
                    .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .isExists();
            if (!hostAvailable) {
                throw new ApiMessageInterceptionException(argerr(
                        "host[uuid:%s] is not in cluster[uuid:%s] or not Enabled/Connected", msg.getHostUuid(), msg.getClusterUuid()));
            }
        } else {
            boolean hasHost = Q.New(HostVO.class)
                    .eq(HostVO_.clusterUuid, msg.getClusterUuid())
                    .eq(HostVO_.state, HostState.Enabled)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .isExists();
            if (!hasHost) {
                throw new ApiMessageInterceptionException(argerr(
                        "no available host found in cluster[uuid:%s], " +
                                "please specify hostUuid or ensure there is at least one connected host in the cluster",
                        msg.getClusterUuid()));
            }
        }
    }
}
