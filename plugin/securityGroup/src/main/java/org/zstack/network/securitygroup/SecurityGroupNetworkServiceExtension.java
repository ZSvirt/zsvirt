package org.zstack.network.securitygroup;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.service.NetworkServiceProviderType;
import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.*;
import org.zstack.network.service.AbstractNetworkServiceExtension;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.Query;

/**
 */
public class SecurityGroupNetworkServiceExtension extends AbstractNetworkServiceExtension implements VmAfterAttachNicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SecurityGroupNetworkServiceExtension.class);

    @Autowired
    private CloudBus bus;

    @Override
    public NetworkServiceExtensionPosition getNetworkServiceExtensionPosition() {
        return NetworkServiceExtensionPosition.AFTER_VM_CREATED;
    }

    @Override
    public NetworkServiceType getNetworkServiceType() {
        return SecurityGroupProviderFactory.networkServiceType;
    }

    private List<String> getVmNicUuids(String vmUuid, String l3Uuid, Collection<String> targetVmNicUuids) {
        List<String> vmNicUuids = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .select(VmNicVO_.uuid)
                .listValues();
        if (targetVmNicUuids == null) {
            return vmNicUuids;
        }

        return vmNicUuids.stream().filter(targetVmNicUuids::contains).collect(Collectors.toList());
    }

    private List<String> getVmNicUuids(String vmUuid, String l3Uuid, int deviceId) {
        String vmNicUuid = Q.New(VmNicVO.class)
                .eq(VmNicVO_.l3NetworkUuid, l3Uuid)
                .eq(VmNicVO_.vmInstanceUuid, vmUuid)
                .eq(VmNicVO_.deviceId, deviceId)
                .select(VmNicVO_.uuid)
                .findValue();
        return vmNicUuid == null ? Collections.emptyList() : Collections.singletonList(vmNicUuid);
    }

    private void syncSecurityGroups(String vmUuid, List<String> vmNicUuids, List<String> securityGroupUuids) {
        for (String vmNicUuid : vmNicUuids) {
            List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmNicUuid).eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, vmUuid).list();
            List<VmNicSecurityGroupRefVO> toCreate = new ArrayList<>();
            for (String sgUuid : securityGroupUuids) {
                refs.stream().filter(ref -> ref.getSecurityGroupUuid().equals(sgUuid)).findAny().orElseGet(() -> {
                    VmNicSecurityGroupRefVO refVO = new VmNicSecurityGroupRefVO();
                    refVO.setUuid(Platform.getUuid());
                    refVO.setSecurityGroupUuid(sgUuid);
                    refVO.setVmInstanceUuid(vmUuid);
                    refVO.setVmNicUuid(vmNicUuid);
                    toCreate.add(refVO);
                    return refVO;
                });
            }
            if (!toCreate.isEmpty()) {
                toCreate.forEach(ref -> ref.setPriority(refs.size() + toCreate.indexOf(ref) + 1));
                dbf.persistCollection(toCreate);

                if (!Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vmNicUuid).isExists()) {
                    VmNicSecurityPolicyVO policyVO = new VmNicSecurityPolicyVO();
                    policyVO.setUuid(Platform.getUuid());
                    policyVO.setVmNicUuid(vmNicUuid);
                    policyVO.setIngressPolicy(VmNicSecurityPolicy.DENY.toString());
                    policyVO.setEgressPolicy(VmNicSecurityPolicy.ALLOW.toString());
                    dbf.persist(policyVO);
                }
            }
        }
    }

    private void syncSecurityGroupPolicy(List<String> vmNicUuids, String ingressPolicy, String egressPolicy) {
        for (String vmNicUuid : vmNicUuids) {
            SQL.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vmNicUuid)
                    .set(VmNicSecurityPolicyVO_.ingressPolicy, ingressPolicy)
                    .set(VmNicSecurityPolicyVO_.egressPolicy, egressPolicy).update();
        }
    }

    private List<String> getExactTags(PatternedSystemTag systemTag, String vmUuid) {
        return systemTag.getTags(vmUuid).stream().filter(systemTag::isMatch).collect(Collectors.toList());
    }

    private void deleteExactTags(PatternedSystemTag systemTag, String vmUuid, List<String> tags) {
        tags.forEach(tag -> systemTag.delete(vmUuid, tag));
    }

    private List<String> syncSystemTagToVmNicSecurityGroup(String vmUuid) {
        return syncSystemTagToVmNicSecurityGroup(vmUuid, null);
    }

    private List<String> syncSystemTagToVmNicSecurityGroup(String vmUuid, Collection<String> targetVmNicUuids) {
        final List<String> sgUuids = new ArrayList<>();
        List<String> tags = getExactTags(VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF, vmUuid);

        for (String tag : tags) {
            Map<String, String> tokens = VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF.getTokensByTag(tag);
            String l3Uuid = tokens.get(VmSystemTags.L3_UUID_TOKEN);
            List<String> securityGroupUuids = Arrays.asList(tokens.get(VmSystemTags.SECURITY_GROUP_UUIDS_TOKEN).split(","));
            sgUuids.addAll(securityGroupUuids);
            syncSecurityGroups(vmUuid, getVmNicUuids(vmUuid, l3Uuid, targetVmNicUuids), securityGroupUuids);
        }
        deleteExactTags(VmSystemTags.L3_NETWORK_SECURITY_GROUP_UUIDS_REF, vmUuid, tags);

        tags = getExactTags(VmSystemTags.SECURITY_GROUP_POLICY, vmUuid);
        for (String tag : tags) {
            Map<String, String> tokens = VmSystemTags.SECURITY_GROUP_POLICY.getTokensByTag(tag);
            String l3Uuid = tokens.get(VmSystemTags.L3_UUID_TOKEN);
            String ingressPolicy = tokens.get(VmSystemTags.SECURITY_GROUP_INGRESS_POLICY_TOKEN);
            String egressPolicy = tokens.get(VmSystemTags.SECURITY_GROUP_EGRESS_POLICY_TOKEN);
            syncSecurityGroupPolicy(getVmNicUuids(vmUuid, l3Uuid, targetVmNicUuids), ingressPolicy, egressPolicy);
        }
        deleteExactTags(VmSystemTags.SECURITY_GROUP_POLICY, vmUuid, tags);

        return sgUuids.stream().distinct().collect(Collectors.toList());
    }

    private List<String> syncVmNicParamToVmNicSecurityGroup(VmInstanceSpec servedVm) {
        final List<String> sgUuids = new ArrayList<>();
        if (servedVm.getL3Networks() == null || servedVm.getL3Networks().isEmpty()) {
            return sgUuids;
        }

        Map<String, Map<String, String>> policies = servedVm.getExtensionData(
                VmInstanceConstant.Params.VmNicSecurityPolicyByDeviceId.toString(), Map.class);
        String vmUuid = servedVm.getVmInventory().getUuid();
        for (int deviceId = 0; deviceId < servedVm.getL3Networks().size(); deviceId++) {
            VmNicSpec nicSpec = servedVm.getL3Networks().get(deviceId);
            if (nicSpec.getL3Invs() == null || nicSpec.getL3Invs().isEmpty()) {
                continue;
            }

            VmNicParam nicParam = nicSpec.getVmNicParam();
            if (nicParam.getSgUuids() == null || nicParam.getSgUuids().isEmpty()) {
                continue;
            }

            String l3Uuid = nicSpec.getL3Invs().get(0).getUuid();
            List<String> vmNicUuids = getVmNicUuids(vmUuid, l3Uuid, deviceId);
            if (vmNicUuids.isEmpty()) {
                continue;
            }

            sgUuids.addAll(nicParam.getSgUuids());
            syncSecurityGroups(vmUuid, vmNicUuids, nicParam.getSgUuids());

            Map<String, String> policy = policies == null ? null : policies.get(String.valueOf(deviceId));
            if (policy != null) {
                syncSecurityGroupPolicy(vmNicUuids,
                        policy.get(VmSystemTags.SECURITY_GROUP_INGRESS_POLICY_TOKEN),
                        policy.get(VmSystemTags.SECURITY_GROUP_EGRESS_POLICY_TOKEN));
            }
        }

        return sgUuids.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void applyNetworkService(VmInstanceSpec servedVm, Map<String, Object> data, final Completion completion) {
        List<String> sgUuids = new ArrayList<>();
        sgUuids.addAll(syncSystemTagToVmNicSecurityGroup(servedVm.getVmInventory().getUuid()));
        sgUuids.addAll(syncVmNicParamToVmNicSecurityGroup(servedVm));
        sgUuids = sgUuids.stream().distinct().collect(Collectors.toList());

        Map<NetworkServiceProviderType, List<L3NetworkInventory>> map = getNetworkServiceProviderMap(SecurityGroupProviderFactory.networkServiceType,
                VmNicSpec.getL3NetworkInventoryOfSpec(servedVm.getL3Networks()));
        if (map.isEmpty()) {
            completion.success();
            return;
        }

        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(servedVm.getVmInventory().getUuid());
        msg.setHostUuid(servedVm.getDestHost().getUuid());
        msg.setSgUuids(sgUuids);
        bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);
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
    }

    @Override
    public void releaseNetworkService(final VmInstanceSpec servedVm, Map<String, Object> data, final NoErrorCompletion completion) {
        if (!Optional.ofNullable(servedVm.getDestHost()).isPresent()){
            completion.done();
            return;
        }
        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(servedVm.getVmInventory().getUuid());
        msg.setHostUuid(servedVm.getDestHost().getUuid());
        msg.setDeleteAllRules(true);
        List<String> uuids = new ArrayList<String>();
        for (VmNicInventory nic: servedVm.getDestNics()) {
            uuids.add(nic.getUuid());
        }
        msg.setNicUuids(uuids);

        bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.debug(String.format("failed to remove security group rules for vm[uuid:%s], %s", servedVm.getVmInventory().getUuid(), reply.getError()));
                }
                if (servedVm.getCurrentVmOperation() == VmInstanceConstant.VmOperation.DetachNic) {
                    for (VmNicInventory nic: servedVm.getDestNics()) {
                        deleteVmNicSecurityGroupRef(nic.getUuid());
                    }
                }
                if (servedVm.getCurrentVmOperation() == VmInstanceConstant.VmOperation.ChangeNicNetwork) {
                    for (String nicUuid : uuids) {
                        deleteVmNicSecurityGroupRef(nicUuid);
                    }
                }

                completion.done();
            }
        });
    }

    @Transactional
    private void deleteVmNicSecurityGroupRef(String vmNicUuid){
	    String sql = String.format("delete from %s ref where ref.%s = :id",
			    VmNicSecurityGroupRefVO.class.getSimpleName(), "vmNicUuid");
	    Query query = dbf.getEntityManager().createQuery(sql);
	    query.setParameter("id", vmNicUuid);
	    query.executeUpdate();
    }

    @Override
    public void afterAttachNic(String nicUuid, VmInstanceInventory vmInstanceInventory, Completion completion) {
        List<String> sgUuids = syncSystemTagToVmNicSecurityGroup(vmInstanceInventory.getUuid(), Collections.singletonList(nicUuid));
        if (StringUtils.isEmpty(vmInstanceInventory.getHostUuid())) {
            completion.success();
            return;
        }
        RefreshSecurityGroupRulesOnVmMsg msg = new RefreshSecurityGroupRulesOnVmMsg();
        msg.setVmInstanceUuid(vmInstanceInventory.getUuid());
        msg.setHostUuid(vmInstanceInventory.getHostUuid());
        msg.setSgUuids(sgUuids);
        bus.makeLocalServiceId(msg, SecurityGroupConstant.SERVICE_ID);
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
    }

    @Override
    public void afterAttachNicRollback(String nicUuid, VmInstanceInventory vmInstanceInventory, NoErrorCompletion completion) {
        logger.debug(String.format("securityGroupNetworkServiceExtension after attach nic starting rollback, hardDelete VmNicSecurityGroupRefVO data"));
        SQL.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, nicUuid)
                .eq(VmNicSecurityGroupRefVO_.vmInstanceUuid, vmInstanceInventory.getUuid())
                .hardDelete();
        completion.done();
    }

    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, List<String> systemTags, Completion completion) {
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, Completion completion) {
        completion.success();
    }
}
