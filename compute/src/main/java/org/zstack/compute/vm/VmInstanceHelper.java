package org.zstack.compute.vm;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.configuration.InstanceOfferingState;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.NewVmInstanceMessage;
import org.zstack.header.vm.NewVmInstanceMessage2;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicParam;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionUtils.getDuplicateElementsOfList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstanceHelper {
    private static final CLogger logger = Utils.getLogger(VmInstanceHelper.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private VmNicManager nicManager;

    public void validate(NewVmInstanceMessage2 msg) {
        validateInstanceSettings(msg);

        Set<String> macs = new HashSet<>();
        if (null != msg.getSystemTags()) {
            Optional<String> duplicateMac = msg.getSystemTags().stream()
                    .filter(t -> VmSystemTags.CUSTOM_MAC.isMatch(t))
                    .map(t -> t.split("::")[2].toLowerCase())
                    .filter(t -> !macs.add(t))
                    .findAny();
            if (duplicateMac.isPresent()){
                throw new ApiMessageInterceptionException(operr(
                        "Not allowed same mac [%s]", duplicateMac.get()));
            }
        }

        if (!CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
            SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
            l3q.select(L3NetworkVO_.uuid, L3NetworkVO_.system, L3NetworkVO_.state, L3NetworkVO_.l2NetworkUuid);
            List<String> uuids = new ArrayList<>(msg.getL3NetworkUuids());
            List<String> duplicateElements = getDuplicateElementsOfList(uuids);
            if (!VmGlobalConfig.MULTI_VNIC_SUPPORT.value(Boolean.class) && !duplicateElements.isEmpty()) {
                throw new ApiMessageInterceptionException(operr("Can't add same uuid in the l3Network,uuid: %s", duplicateElements.get(0)));
            }

            l3q.add(L3NetworkVO_.uuid, SimpleQuery.Op.IN, msg.getL3NetworkUuids());
            List<Tuple> l3ts = l3q.listTuple();
            for (Tuple t : l3ts) {
                String l3Uuid = t.get(0, String.class);
                Boolean system = t.get(1, Boolean.class);
                L3NetworkState state = t.get(2, L3NetworkState.class);
                String l2Uuid = t.get(3, String.class);
                if (state != L3NetworkState.Enabled) {
                    throw new ApiMessageInterceptionException(operr("l3Network[uuid:%s] is Disabled, can not create vm on it", l3Uuid));
                }
                if (system && (msg.getType() == null || VmInstanceConstant.USER_VM_TYPE.equals(msg.getType()))) {
                    throw new ApiMessageInterceptionException(operr("l3Network[uuid:%s] is system network, can not create user vm on it", l3Uuid));
                }
                L2NetworkVO l2NetworkVO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2Uuid).find();
                if (l2NetworkVO.getAttachedClusterRefs() == null || l2NetworkVO.getAttachedClusterRefs().isEmpty()) {
                    throw new ApiMessageInterceptionException(argerr(String.format("l2 network[uuid: %s] of l3 network[uuid: %s] not attached to cluster",
                            l2NetworkVO.getUuid(), l3Uuid)));
                } else if (msg.getClusterUuid() != null && l2NetworkVO.getAttachedClusterRefs().stream().noneMatch(c -> c.getClusterUuid().equals(msg.getClusterUuid()))) {
                    throw new ApiMessageInterceptionException(argerr(String.format("l2 network[uuid: %s] of l3 network[uuid: %s] not attached to cluster[uuid: %s]",
                            l2NetworkVO.getUuid(), l3Uuid, msg.getClusterUuid())));
                }
            }
        }

        if (msg.getHostUuid() != null) {
            final Tuple tuple = Q.New(HostVO.class)
                    .eq(HostVO_.uuid, msg.getHostUuid())
                    .select(HostVO_.clusterUuid, HostVO_.zoneUuid, HostVO_.state, HostVO_.status)
                    .findTuple();
            String expectClusterUuid = tuple.get(0, String.class);
            String expectZoneUuid = tuple.get(1, String.class);
            if (msg.getClusterUuid() != null && !Objects.equals(msg.getClusterUuid(), expectClusterUuid)) {
                throw new ApiMessageInterceptionException(
                        argerr("host[uuid:%s] is specified but it is not in cluster[uuid:%s], can not create vm from it",
                        msg.getHostUuid(), expectClusterUuid)
                        .withOpaque("host.uuid", msg.getHostUuid())
                        .withOpaque("expect.cluster.uuid", expectClusterUuid)
                        .withOpaque("actual.cluster.uuid", msg.getClusterUuid()));
            }

            if (msg.getZoneUuid() != null && !Objects.equals(msg.getZoneUuid(), expectZoneUuid)) {
                throw new ApiMessageInterceptionException(
                        argerr("host[uuid:%s] is specified but it is not in zone[uuid:%s], can not create vm from it",
                        msg.getHostUuid(), expectZoneUuid)
                        .withOpaque("host.uuid", msg.getHostUuid())
                        .withOpaque("expect.zone.uuid", expectZoneUuid)
                        .withOpaque("actual.zone.uuid", msg.getZoneUuid()));
            }

            HostState hostState = tuple.get(2, HostState.class);
            if (hostState == HostState.Disabled) {
                throw new ApiMessageInterceptionException(
                        argerr("host[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getHostUuid())
                        .withOpaque("host.uuid", msg.getHostUuid()));
            }

            HostStatus connectionState = tuple.get(3, HostStatus.class);
            if (connectionState != HostStatus.Connected) {
                throw new ApiMessageInterceptionException(
                        argerr("host[uuid:%s] is specified but its connection status is %s, can not create vm from it",
                        msg.getHostUuid(), connectionState)
                        .withOpaque("host.uuid", msg.getHostUuid()));
            }

            msg.setClusterUuid(expectClusterUuid);
            msg.setZoneUuid(expectZoneUuid);
        }

        if (msg.getClusterUuid() != null) {
            final Tuple tuple = Q.New(ClusterVO.class)
                    .eq(ClusterVO_.uuid, msg.getClusterUuid())
                    .select(ClusterVO_.zoneUuid, ClusterVO_.state)
                    .findTuple();
            String expectZoneUuid = tuple.get(0, String.class);
            if (msg.getZoneUuid() != null && !Objects.equals(msg.getZoneUuid(), expectZoneUuid)) {
                throw new ApiMessageInterceptionException(
                        argerr("cluster[uuid:%s] is specified but it's not in zone[uuid:%s], can not create vm from it",
                        msg.getClusterUuid(), expectZoneUuid)
                        .withOpaque("cluster.uuid", msg.getClusterUuid())
                        .withOpaque("expect.zone.uuid", expectZoneUuid)
                        .withOpaque("actual.zone.uuid", msg.getZoneUuid()));
            }

            ClusterState clusterState = tuple.get(1, ClusterState.class);
            if (clusterState == ClusterState.Disabled) {
                throw new ApiMessageInterceptionException(
                        argerr("cluster[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getClusterUuid())
                        .withOpaque("cluster.uuid", msg.getClusterUuid()));
            }

            msg.setZoneUuid(expectZoneUuid);
        }

        if (msg.getZoneUuid() != null) {
            ZoneState zoneState = Q.New(ZoneVO.class)
                    .eq(ZoneVO_.uuid, msg.getZoneUuid())
                    .select(ZoneVO_.state)
                    .findValue();
            if (zoneState == ZoneState.Disabled) {
                throw new ApiMessageInterceptionException(
                        argerr("zone[uuid:%s] is specified but it's Disabled, can not create vm from it", msg.getZoneUuid())
                        .withOpaque("zone.uuid", msg.getZoneUuid()));
            }
        }

        if (msg.getType() == null) {
            msg.setType(VmInstanceConstant.USER_VM_TYPE);
        }

        if (VmInstanceConstant.USER_VM_TYPE.equals(msg.getType())) {
            String defaultL3Uuid = msg.getDefaultL3NetworkUuid();
            List<String> l3UuidList = msg.getL3NetworkUuids() == null ? new ArrayList<>() : msg.getL3NetworkUuids();

            if (defaultL3Uuid == null) {
                if (l3UuidList.size() == 1) {
                    defaultL3Uuid = l3UuidList.get(0);
                } else if (l3UuidList.size() > 1) {
                    throw new ApiMessageInterceptionException(
                            argerr("there are more than one L3 network specified in l3NetworkUuids, but defaultL3NetworkUuid is null"));
                }
            } else {
                if (!l3UuidList.contains(defaultL3Uuid)) {
                    throw new ApiMessageInterceptionException(
                            argerr("defaultL3NetworkUuid[uuid:%s] is not in l3NetworkUuids%s", defaultL3Uuid, l3UuidList));
                }
            }

            msg.setDefaultL3NetworkUuid(defaultL3Uuid);
        }

        if (msg instanceof APIMessage && !(msg instanceof APICreateMessage)) {
            new StaticIpOperator().validateStaticIpTagsInApiMessage((APIMessage) msg);
        }

        validateCdRomsTag(msg);
        validateZoneOrClusterOrHostOrL3Exist(msg);
        validate((NewVmInstanceMessage) msg);
    }

    public void validate(NewVmInstanceMessage msg) {
        validateVmNicParams(msg);
        validateL3Networks(msg.getL3NetworkUuids());
    }

    public void validateL3Networks(List<String> l3NetworkUuids) {
        if (CollectionUtils.isEmpty(l3NetworkUuids)) {
            return;
        }

        List<String> IPAMEnabledL3Uuids = Q.New(L3NetworkVO.class)
                .select(L3NetworkVO_.uuid)
                .eq(L3NetworkVO_.enableIPAM, Boolean.TRUE)
                .in(L3NetworkVO_.uuid, l3NetworkUuids)
                .listValues();

        if (CollectionUtils.isEmpty(IPAMEnabledL3Uuids)) {
            return;
        }

        Set<String> hasIpRangeL3Set = new HashSet<>(Q.New(IpRangeVO.class)
                .select(IpRangeVO_.l3NetworkUuid)
                .in(IpRangeVO_.l3NetworkUuid, IPAMEnabledL3Uuids)
                .listValues());

        for (String l3Uuid : IPAMEnabledL3Uuids) {
            if (!hasIpRangeL3Set.contains(l3Uuid)) {
                throw new ApiMessageInterceptionException(operr("there is no available ipRange on L3 network [%s]", l3Uuid));
            }
        }
    }

    private void validateInstanceSettings(NewVmInstanceMessage2 msg) {
        final String instanceOfferingUuid = msg.getInstanceOfferingUuid();

        if (instanceOfferingUuid == null) {
            if (msg.getCpuNum() == null || msg.getMemorySize() == null) {
                throw new ApiMessageInterceptionException(operr("Missing CPU/memory settings"));
            }

            if (msg.getCpuNum() <= 0 || msg.getMemorySize() <= 0) {
                throw new ApiMessageInterceptionException(operr("Unexpected CPU/memory settings"));
            }

            if (msg.getReservedMemorySize() != null) {
                if (msg.getReservedMemorySize() > msg.getMemorySize()) {
                    throw new ApiMessageInterceptionException(operr("reserved memory[%s] is greater than memory size[%s]", msg.getReservedMemorySize(), msg.getMemorySize()));
                }
            } else {
                msg.setReservedMemorySize(0L);
            }

            return;
        }

        // InstanceOffering takes precedence over CPU/memory settings.
        InstanceOfferingVO ivo = dbf.findByUuid(instanceOfferingUuid, InstanceOfferingVO.class);
        if (ivo.getState() == InstanceOfferingState.Disabled) {
            throw new ApiMessageInterceptionException(operr("instance offering[uuid:%s] is Disabled, can't create vm from it", instanceOfferingUuid));
        }

        if (!ivo.getType().equals(VmInstanceConstant.USER_VM_TYPE)){
            throw new ApiMessageInterceptionException(operr("instance offering[uuid:%s, type:%s] is not UserVm type, can't create vm from it", instanceOfferingUuid, ivo.getType()));
        }

        msg.setCpuNum(ivo.getCpuNum());
        msg.setMemorySize(ivo.getMemorySize());
        // reserved memory should support customize
        if (msg.getReservedMemorySize() == null) {
            msg.setReservedMemorySize(ivo.getReservedMemorySize());
        }
    }

    private void validateCdRomsTag(NewVmInstanceMessage msg) {
        if (msg.getSystemTags() == null || msg.getSystemTags().isEmpty()) {
            return;
        }

        String tagValue = SystemTagUtils.findTagValue(msg.getSystemTags(), VmSystemTags.CREATE_VM_CD_ROM_LIST);
        if (tagValue == null) {
            return;
        }

        Map<String, String> tokens = VmSystemTags.CREATE_VM_CD_ROM_LIST.getTokensByTag(tagValue);
        List<String> cdRoms = new ArrayList<>();
        cdRoms.add(tokens.get(VmSystemTags.CD_ROM_0));
        cdRoms.add(tokens.get(VmSystemTags.CD_ROM_1));
        cdRoms.add(tokens.get(VmSystemTags.CD_ROM_2));
        cdRoms = cdRoms.stream().filter(i -> i != null && !VmInstanceConstant.NONE_CDROM.equalsIgnoreCase(i) && !VmInstanceConstant.EMPTY_CDROM.equalsIgnoreCase(i)).collect(Collectors.toList());
        if (cdRoms == null || cdRoms.isEmpty()) {
            return;
        }

        for (String cdRomIsoUuid : cdRoms) {
            ImageVO imageVO = dbf.findByUuid(cdRomIsoUuid, ImageVO.class);
            if (imageVO == null) {
                throw new ApiMessageInterceptionException(argerr("The image[uuid:%s] does not exist", cdRomIsoUuid));
            }
        }

        if (cdRoms.size() != new HashSet<>(cdRoms).size()) {
            throw new ApiMessageInterceptionException(argerr("Do not allow to mount duplicate ISO"));
        }
    }

    private void validateZoneOrClusterOrHostOrL3Exist(NewVmInstanceMessage2 msg) {
        if (CollectionUtils.isEmpty(msg.getL3NetworkUuids()) && StringUtils.isEmpty(msg.getZoneUuid())
                && StringUtils.isEmpty(msg.getClusterUuid()) && StringUtils.isEmpty(msg.getHostUuid())) {
            throw new ApiMessageInterceptionException(operr("could not create vm, because at least one of field (l3NetworkUuids,zoneUuid,clusterUuid,hostUuid) should be set"));
        }
    }

    @SuppressWarnings({"unchecked"})
    private void validateVmNicParams(NewVmInstanceMessage msg) {
        if (!StringUtils.isEmpty(msg.getVmNicParams())) {
            if (CollectionUtils.isEmpty(msg.getL3NetworkUuids())) {
                throw new ApiMessageInterceptionException(argerr("l3NetworkUuids cannot be empty when vmNicParams is provided"));
            }

            List<VmNicParam> vmNicParams;
            try {
                vmNicParams = JSONObjectUtil.toCollection(msg.getVmNicParams(), ArrayList.class, VmNicParam.class);
            } catch (JsonSyntaxException e) {
                throw new ApiMessageInterceptionException(argerr("invalid json format, causes: %s", e.getMessage()));
            }

            new VmNicParamValidator().withVmNicParams(vmNicParams)
                    .withL3Uuids(msg.getL3NetworkUuids())
                    .withDefaultL3Uuid(msg.getDefaultL3NetworkUuid())
                    .withSupportNicDriverTypes(nicManager.getSupportNicDriverTypes())
                    .withVmType(msg.getType())
                    .isWindowsVm(ImagePlatform.Windows.toString().equals(msg.getPlatform()))
                    .validate();
        }
    }
}
