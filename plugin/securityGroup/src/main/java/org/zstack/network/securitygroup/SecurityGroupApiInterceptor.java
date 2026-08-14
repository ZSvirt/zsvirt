package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.identity.Account;
import org.zstack.identity.QuotaUtil;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.logging.CLogger;

import org.apache.commons.lang.StringUtils;

import javax.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Arrays.asList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;

/**
 */
@InterceptorForService("securityGroup")
public class SecurityGroupApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static CLogger logger = Utils.getLogger(SecurityGroupApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof AddSecurityGroupRuleMessage) {
            AddSecurityGroupRuleMessage amsg = (AddSecurityGroupRuleMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, amsg.getSecurityGroupUuid());
        } else if (msg instanceof SecurityGroupMessage) {
            SecurityGroupMessage smsg = (SecurityGroupMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, smsg.getSecurityGroupUuid());
        } else if (msg instanceof VmNicSecurityGroupMessage) {
            VmNicSecurityGroupMessage vmsg = (VmNicSecurityGroupMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, vmsg.getVmNicUuid());
        }
    }


    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIChangeResourceOwnerMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddSecurityGroupRuleMsg) {
            validate((APIAddSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIAddVmNicToSecurityGroupMsg) {
            validate((APIAddVmNicToSecurityGroupMsg) msg);
        } else if (msg instanceof APIAttachSecurityGroupToL3NetworkMsg) {
            validate((APIAttachSecurityGroupToL3NetworkMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupMsg) {
            validate((APIDeleteSecurityGroupMsg) msg);
        } else if (msg instanceof APIDeleteSecurityGroupRuleMsg) {
            validate((APIDeleteSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIDeleteVmNicFromSecurityGroupMsg) {
            validate((APIDeleteVmNicFromSecurityGroupMsg) msg);
        } else if (msg instanceof APIDetachSecurityGroupFromL3NetworkMsg) {
            validate((APIDetachSecurityGroupFromL3NetworkMsg) msg);
        } if (msg instanceof APICreateSecurityGroupMsg) {
            validate((APICreateSecurityGroupMsg) msg);
        } else if (msg instanceof APIChangeSecurityGroupRuleMsg) {
            validate((APIChangeSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIUpdateSecurityGroupRulePriorityMsg) {
            validate((APIUpdateSecurityGroupRulePriorityMsg) msg);
        } else if (msg instanceof APIChangeVmNicSecurityPolicyMsg) {
            validate((APIChangeVmNicSecurityPolicyMsg) msg);
        } else if (msg instanceof APIChangeSecurityGroupRuleStateMsg) {
            validate((APIChangeSecurityGroupRuleStateMsg) msg);
        } else if (msg instanceof APISetVmNicSecurityGroupMsg) {
            validate((APISetVmNicSecurityGroupMsg) msg);
        } else if (msg instanceof APIValidateSecurityGroupRuleMsg) {
            validate((APIValidateSecurityGroupRuleMsg) msg);
        } else if (msg instanceof APIChangeResourceOwnerMsg) {
            validate((APIChangeResourceOwnerMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private String normalizeIpOrPort(String value) {
        return value == null ? null : StringUtils.deleteWhitespace(value);
    }

    private void validate(APIChangeResourceOwnerMsg msg) {
        AccountResourceRefVO ref = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, msg.getResourceUuid())
                .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                .find();
        if (ref == null) {
            return;
        }

        if (VmInstanceVO.class.getSimpleName().equals(ref.getResourceType())) {
            List<String> nics = SQL.New("select sgRef.vmNicUuid from VmInstanceVO vm, VmNicVO nic, VmNicSecurityGroupRefVO sgRef" +
                    " where vm.uuid = :vmUuid" +
                    " and nic.vmInstanceUuid = vm.uuid" +
                    " and sgRef.vmNicUuid = nic.uuid", String.class)
                    .param("vmUuid", ref.getResourceUuid())
                    .list();
            if (!nics.isEmpty()) {
                throw new ApiMessageInterceptionException(argerr("could not change resource owner, because the resource[uuid:%s, type:VmInstance] has already attached security group", msg.getResourceUuid()));
            }
        }
    }

    private void validate(APIValidateSecurityGroupRuleMsg msg) {
        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getSecurityGroupUuid()).isExists()) {
            throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RESOURCE_NOT_EXIST_ERROR, "invalid security group rule, because security group[uuid:%s] not found", msg.getSecurityGroupUuid()));
        }

        if (msg.getRemoteSecurityGroupUuid() != null) {
            if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getRemoteSecurityGroupUuid()).isExists()) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RESOURCE_NOT_EXIST_ERROR, "invalid security group rule, because remote security group[uuid:%s] not found", msg.getRemoteSecurityGroupUuid()));
            }
        }

        if (msg.getIpVersion() == null) {
            msg.setIpVersion(IPv6Constants.IPv4);
        }
        if (msg.getAction() == null) {
            msg.setAction(SecurityGroupRuleAction.ACCEPT.toString());
        }

        if (msg.getAllowedCidr() == null) {
            msg.setAllowedCidr(msg.getIpVersion() == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
        } else {
            //remove extra space characters
            msg.setAllowedCidr(normalizeIpOrPort(msg.getAllowedCidr()));
            validateIps(msg.getAllowedCidr(), msg.getIpVersion());
        }

        if (msg.getStartPort() == null) {
            msg.setStartPort(-1);
        }

        if (msg.getEndPort() == null) {
            msg.setEndPort(-1);
        }

        if (msg.getSrcIpRange() != null) {
            msg.setSrcIpRange(normalizeIpOrPort(msg.getSrcIpRange()));;
            validateIps(msg.getSrcIpRange(), msg.getIpVersion());
        }

        if (msg.getDstIpRange() != null) {
            msg.setDstIpRange(normalizeIpOrPort(msg.getDstIpRange()));
            validateIps(msg.getDstIpRange(), msg.getIpVersion());
        }

        if (msg.getDstPortRange() != null) {
            msg.setDstPortRange(normalizeIpOrPort(msg.getDstPortRange()));
            validatePorts(msg.getDstPortRange());
        }

        if (SecurityGroupRuleProtocolType.ALL.toString().equals(msg.getProtocol()) || SecurityGroupRuleProtocolType.ICMP.toString().equals(msg.getProtocol())) {
            if (msg.getStartPort() != -1 || msg.getEndPort() != -1) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid security group rule, because startPort and endPort must be -1 when protocol is ALL or ICMP"));
            }
        } else {
            if (msg.getStartPort() > msg.getEndPort()) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid security group rule, because invalid endPort[%d], endPort must be greater than or equal to startPort[%d]", msg.getEndPort(), msg.getStartPort()));
            }
            if (msg.getStartPort() > 65535) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid security group rule, because startPort[%d] must less than 65535 when protocol is[%s]", msg.getStartPort(), msg.getProtocol()));
            }
        }

        if (msg.getRemoteSecurityGroupUuid() != null) {
            if (msg.getSrcIpRange() != null || msg.getDstIpRange() != null) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR, "remoteSecurityGroupUuid[%s] and srcIpRange/dstIpRange cannot be set at the same time", msg.getRemoteSecurityGroupUuid()));
            }
            if (!SecurityGroupConstant.WORLD_OPEN_CIDR.equals(msg.getAllowedCidr()) && !SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6.equals(msg.getAllowedCidr())) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR, "remoteSecurityGroupUuid[%s] and allowedCidr[%s] cannot be set at the same time", msg.getRemoteSecurityGroupUuid(), msg.getAllowedCidr()));
            }
        }

        if (msg.getSrcIpRange() != null) {
            if (msg.getDstIpRange() != null) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR, "srcIpRange[%s] and dstIpRange[%s] cannot be set at the same time", msg.getSrcIpRange(), msg.getDstIpRange()));
            }
            if (SecurityGroupRuleType.Egress.toString().equals(msg.getType())) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_NOT_SUPPORT_ERROR, "srcIpRange cannot be set in Egress rule"));
            }
        }

        if (msg.getDstIpRange() != null) {
            if (SecurityGroupRuleType.Ingress.toString().equals(msg.getType())) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_NOT_SUPPORT_ERROR, "dstIpRange cannot be set in Ingress rule"));
            }
        }

        if (msg.getDstPortRange() != null) {
            if (SecurityGroupRuleProtocolType.ALL.toString().equals(msg.getProtocol()) || SecurityGroupRuleProtocolType.ICMP.toString().equals(msg.getProtocol())) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_NOT_SUPPORT_ERROR, "dstPortRange cannot be set when rule protocol is ALL or ICMP"));
            }

            if (msg.getStartPort() != -1 || msg.getEndPort() != -1) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR, "dstPortRange and startPort/endPort cannot be set at the same time"));
            }
        } else if (msg.getStartPort() >= 0) {
            if (msg.getStartPort().equals(msg.getEndPort())) {
                msg.setDstPortRange(String.valueOf(msg.getStartPort()));
            } else {
                msg.setDstPortRange(String.format("%s-%s", msg.getStartPort(), msg.getEndPort()));
            }
        }

        if (!SecurityGroupConstant.WORLD_OPEN_CIDR.equals(msg.getAllowedCidr()) && !SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6.equals(msg.getAllowedCidr())) {
            if (msg.getSrcIpRange() != null || msg.getDstIpRange() != null) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_FILED_CONFLICT_ERROR, "allowCidr and srcIpRange/dstIpRange cannot be set at the same time"));
            }

            if (SecurityGroupRuleType.Ingress.toString().equals(msg.getType())) {
                msg.setSrcIpRange(msg.getAllowedCidr());
            } else {
                msg.setDstIpRange(msg.getAllowedCidr());
            }
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO targetRule = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
        targetRule.setType(msg.getType());
        targetRule.setRemoteSecurityGroupUuid(msg.getRemoteSecurityGroupUuid());
        targetRule.setAction(msg.getAction());
        targetRule.setProtocol(msg.getProtocol());
        targetRule.setIpVersion(msg.getIpVersion());
        targetRule.setDstIpRange(msg.getDstIpRange());
        targetRule.setSrcIpRange(msg.getSrcIpRange());
        targetRule.setDstPortRange(msg.getDstPortRange());

        // Deduplicate in DB
        List<SecurityGroupRuleVO> vos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid()).eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.valueOf(msg.getType())).list();

        for (SecurityGroupRuleVO vo : vos) {
            APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
            ao.setType(vo.getType().toString());
            ao.setProtocol(vo.getProtocol().toString());
            ao.setIpVersion(vo.getIpVersion());
            ao.setRemoteSecurityGroupUuid(vo.getRemoteSecurityGroupUuid());
            ao.setAction(vo.getAction());
            ao.setSrcIpRange(vo.getSrcIpRange());
            ao.setDstIpRange(vo.getDstIpRange());
            ao.setDstPortRange(vo.getDstPortRange());
            if (ao.equals(targetRule)) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_DUPLICATE_ERROR, "duplicated to rule[uuid:%s] in datebase", vo.getUuid()));
            }
        }
    }

    private void validate(APISetVmNicSecurityGroupMsg msg) {
        VmNicVO nic = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).find();
        if (nic == null) {
            throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because vm nic[uuid:%s] not found", msg.getVmNicUuid()));
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, msg.getVmNicUuid()).list();

        if (msg.getRefs().isEmpty() && refs.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because the vm nic[uuid:%s] not attached to any security group", msg.getVmNicUuid()));
        }

        Map<Integer, String> aoMap = new HashMap<Integer, String>();
        List<Integer> adminIntegers = new ArrayList<>();
        final String vmAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(nic.getVmInstanceUuid());
        for (APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO ao : msg.getRefs()) {

            if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, ao.getSecurityGroupUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because security group[uuid:%s] not found", ao.getSecurityGroupUuid()));
            }

            Integer priority = ao.getPriority();
            if (priority < 1) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because invalid priority, priority[%d] cannot be less than 1", priority));
            }
            
            if (aoMap.containsKey(priority)) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because duplicate priority, both security group %s and %s have priority[%d]", aoMap.get(priority), ao.getSecurityGroupUuid(), priority));
            }
            if (aoMap.containsValue(ao.getSecurityGroupUuid())) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because duplicate security group[uuid:%s]", ao.getSecurityGroupUuid()));
            }
            aoMap.put(priority, ao.getSecurityGroupUuid());

            if (!refs.stream().anyMatch(r -> r.getSecurityGroupUuid().equals(ao.getSecurityGroupUuid()))) {
                checkIfL3NetworkSupportSecurityGroup(asList(msg.getVmNicUuid()));
            }

            String sgOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(ao.getSecurityGroupUuid());
            if (!Account.isAdminPermission(sgOwnerAccountUuid) && !Account.isAdminPermission(vmAccountUuid) && !sgOwnerAccountUuid.equals(vmAccountUuid)) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because security group[uuid:%s] is not owned by account[uuid:%s] or admin", ao.getSecurityGroupUuid(), vmAccountUuid));
            }
            if (Account.isAdminPermission(sgOwnerAccountUuid)) {
                adminIntegers.add(priority);
            }
        }
        if (!aoMap.isEmpty()) {
            Integer[] priorities = aoMap.keySet().toArray(new Integer[aoMap.size()]);
            Arrays.sort(priorities);
            if (priorities[0] != 1) {
                throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because invalid priority, priority expects to start at 1, but [%d]", priorities[0]));
            }
            for (int i = 0; i < priorities.length - 1; i++) {
                if (priorities[i] + 1 != priorities[i + 1]) {
                    throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because invalid priority, priority[%d] and priority[%d] expected to be consecutive", priorities[i], priorities[i + 1]));
                }
            }
        }


        if (!Account.isAdminPermission(msg.getSession())) {
            List<VmNicSecurityGroupRefVO> userRefs = new ArrayList<>();
            List<VmNicSecurityGroupRefVO> otherRefs = new ArrayList<>();

            for (VmNicSecurityGroupRefVO ref : refs) {
                String sgOwnerAccountUuid = new QuotaUtil().getResourceOwnerAccountUuid(ref.getSecurityGroupUuid());
                if (sgOwnerAccountUuid.equals(vmAccountUuid)) {
                    userRefs.add(ref);
                } else {
                    otherRefs.add(ref);
                }
            }

            List<VmNicSecurityGroupRefVO> sortedOtherRefs = otherRefs.stream().sorted(Comparator.comparingInt(VmNicSecurityGroupRefVO::getPriority)).collect(Collectors.toList());
            List<APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO> sortedUserAOs = msg.getRefs().stream().sorted(Comparator.comparingInt(APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO::getPriority)).collect(Collectors.toList());

            if (!sortedOtherRefs.isEmpty()) {
                int count = sortedOtherRefs.size();
                List<APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO> newAOs = new ArrayList<>();
                sortedOtherRefs.forEach(r -> {
                    APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO ao = new APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO();
                    ao.setPriority(sortedOtherRefs.indexOf(r) + 1);
                    ao.setSecurityGroupUuid(r.getSecurityGroupUuid());
                    newAOs.add(ao);
                });
                sortedUserAOs.forEach(a -> {
                    a.setPriority(sortedUserAOs.indexOf(a) + count + 1);
                    newAOs.add(a);
                });

                msg.setRefs(newAOs);
            }
        } else {
            if (!adminIntegers.isEmpty()) {
                Integer[] priorities = adminIntegers.toArray(new Integer[adminIntegers.size()]);
                Arrays.sort(priorities);
                if (priorities[0] != 1) {
                    throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because admin security group priority[%d] must be higher than users", priorities[0]));
                }
                for (int i = 0; i < priorities.length - 1; i++) {
                    if (priorities[i] + 1 != priorities[i + 1]) {
                        throw new ApiMessageInterceptionException(argerr("could no set vm nic security group, because admin security group priority[%d] must be higher than users", priorities[i + 1]));
                    }
                }
            }
        }
    }

    private void validate(APIChangeSecurityGroupRuleStateMsg msg) {
        if (msg.getRuleUuids().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because ruleUuids is empty"));
        }

        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getSecurityGroupUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because security group[uuid:%s] not found", msg.getSecurityGroupUuid()));
        }

        List<String> toChange = new ArrayList<>();
        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid()).in(SecurityGroupRuleVO_.uuid, msg.getRuleUuids()).list();
        msg.getRuleUuids().stream().forEach(r -> {
            SecurityGroupRuleVO vo = rvos.stream().filter(rvo -> rvo.getUuid().equals(r)).findAny().get();
            if (vo == null) {
                throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because security group rule[uuid:%s] not found", r));
            }

            if (!vo.getState().toString().equals(msg.getState())) {
                toChange.add(r);
            }
        });

        if (toChange.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could no change security group rule state, because no security group rule state need to change"));
        } else {
            msg.setRuleUuids(toChange);
        }
    }

    private void validate(APIChangeVmNicSecurityPolicyMsg msg) {
        if (msg.getIngressPolicy() == null && msg.getEgressPolicy() == null) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because ingress policy and egress policy cannot be both null"));
        }
        if (msg.getIngressPolicy() != null && !VmNicSecurityPolicy.isValid(msg.getIngressPolicy())) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because invalid ingress policy[%s]", msg.getIngressPolicy()));
        }

        if (msg.getEgressPolicy() != null && !VmNicSecurityPolicy.isValid(msg.getEgressPolicy())) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because invalid egress policy[%s]", msg.getEgressPolicy()));
        }

        if (!Q.New(VmNicVO.class).eq(VmNicVO_.uuid, msg.getVmNicUuid()).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because vm nic[uuid:%s] not found", msg.getVmNicUuid()));
        }

        VmNicSecurityPolicyVO policy = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, msg.getVmNicUuid()).find();
        if (policy == null) {
            throw new ApiMessageInterceptionException(argerr("could no change vm nic security policy, because vm nic[uuid:%s] has no security policy", msg.getVmNicUuid()));
        }
        
        if (policy.getIngressPolicy().equals(msg.getIngressPolicy())) {
            msg.setIngressPolicy(null);
        }
        
        if (policy.getEgressPolicy().equals(msg.getEgressPolicy())) {
            msg.setEgressPolicy(null);
        }
    }

    private void validate(APIUpdateSecurityGroupRulePriorityMsg msg) {
        if (!SecurityGroupRuleType.isValid(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because invalid type[%s]", msg.getType()));
        }

        SecurityGroupVO sgvo = dbf.findByUuid(msg.getSecurityGroupUuid(), SecurityGroupVO.class);
        if (sgvo == null) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because security group[uuid:%s] is not exist", msg.getSecurityGroupUuid()));
        }

        if (msg.getRules().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rules is empty"));
        }

        HashMap<Integer, String> priorityMap = new HashMap<Integer, String>();
        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, msg.getSecurityGroupUuid())
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.valueOf(msg.getType()))
                .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                .list();
        if (rvos.size() != msg.getRules().size()) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because security group[uuid:%s] rules size not match", msg.getSecurityGroupUuid()));
        }

        for (APIUpdateSecurityGroupRulePriorityMsg.SecurityGroupRulePriorityAO ao : msg.getRules()) {
            if (ao.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
                throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rule priority[%d] is invalid", ao.getPriority()));
            }
            if (priorityMap.containsKey(ao.getPriority())) {
                throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because priority[%d] has duplicate", ao.getPriority()));
            } else {
                priorityMap.put(ao.getPriority(), ao.getRuleUuid());
            }

            rvos.stream().filter(rvo -> rvo.getUuid().equals(ao.getRuleUuid())).findFirst().orElseThrow(() ->
                    new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rule[uuid:%s] not in security group[uuid:%s]", ao.getRuleUuid(), msg.getSecurityGroupUuid())));

            rvos.stream().filter(rvo -> rvo.getPriority() == ao.getPriority()).findFirst().orElseThrow(() ->
                    new ApiMessageInterceptionException(argerr("could not update security group rule priority, because priority[%d] not in security group[uuid:%s]", ao.getPriority(), msg.getSecurityGroupUuid())));
        }

        List<String> uuidList = new ArrayList<>(priorityMap.values());
        if ((int)uuidList.stream().distinct().count() != uuidList.size()) {
            throw new ApiMessageInterceptionException(argerr("could not update security group rule priority, because rule uuid duplicate"));
        }
    }

    private void validate(APIChangeSecurityGroupRuleMsg msg) {
        SecurityGroupRuleVO vo = Q.New(SecurityGroupRuleVO.class).eq((SecurityGroupRuleVO_.uuid), msg.getUuid()).find();
        if (vo == null) {
            throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule uuid[%s] is not exist", msg.getUuid()));
        }

        if (vo.getPriority() == 0) {
            if (msg.getProtocol() != null || msg.getAction() != null || msg.getRemoteSecurityGroupUuid() != null || msg.getSrcIpRange() != null
                || msg.getDstIpRange() != null || msg.getDstPortRange() != null || msg.getPriority() != null) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] is default rule, only the description and status can be set", msg.getUuid()));
                }
        }

        if (msg.getPriority() != null) {
            if (msg.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] priority cannot be set to default rule priority[%d]", msg.getUuid(), SecurityGroupConstant.DEFAULT_RULE_PRIORITY));
            }

            Long count = Q.New(SecurityGroupRuleVO.class)
                    .eq(SecurityGroupRuleVO_.securityGroupUuid, vo.getSecurityGroupUuid())
                    .eq(SecurityGroupRuleVO_.type, vo.getType())
                    .notEq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                    .count();
            if (count.intValue() > SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group %s rules number[%d] is out of max limit[%d]", vo.getType(), count.intValue(), SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
            }
            if (msg.getPriority() > count.intValue()) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because the maximum priority of %s rule is [%d]", vo.getType().toString(), count.intValue()));
            }
            if (msg.getPriority() < 0) {
                msg.setPriority(SecurityGroupConstant.LOWEST_RULE_PRIORITY);
            }
        }

        if (msg.getState() != null) {
            if (!SecurityGroupRuleState.isValid(msg.getState())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because invalid state[%s]", msg.getState()));
            }
        } else {
            msg.setState(vo.getState().toString());
        }

        if (msg.getAction() != null) {
            if (!SecurityGroupRuleAction.isValid(msg.getAction())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because invalid action[%s]", msg.getAction()));
            }
        } else {
            msg.setAction(vo.getAction());
        }

        if (msg.getProtocol() != null) {
            if (!SecurityGroupRuleProtocolType.isValid(msg.getProtocol())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because invalid protocol[%s]", msg.getProtocol()));
            }
        } else {
            msg.setProtocol(vo.getProtocol().toString());
        }

        if (msg.getDescription() != null) {
            if (msg.getDescription().isEmpty()) {
                msg.setDescription(null);
            }
        } else {
            msg.setDescription(vo.getDescription());
        }

        if (StringUtils.isNotEmpty(msg.getSrcIpRange())) {
            if (SecurityGroupRuleType.Egress.equals(vo.getType())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] type is Egress, srcIpRange[%s] cannot be set", msg.getUuid(),  msg.getSrcIpRange()));
            }
            if (StringUtils.isNotEmpty(msg.getDstIpRange())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] type is Ingress, dstIpRange[%s] cannot be set", msg.getUuid(), msg.getDstIpRange()));
            }
            if (StringUtils.isNotEmpty(msg.getRemoteSecurityGroupUuid())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because srcIpRange[%s] is set, remoteSecurityGroupUuid[%s] must be empty", msg.getSrcIpRange(), msg.getRemoteSecurityGroupUuid()));
            }

            msg.setSrcIpRange(normalizeIpOrPort(msg.getSrcIpRange()));
            validateIps(msg.getSrcIpRange(), vo.getIpVersion());
        }

        if (StringUtils.isNotEmpty(msg.getDstIpRange())) {
            if (SecurityGroupRuleType.Ingress.equals(vo.getType())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] type is Ingress, dstIpRange[%s] cannot be set", msg.getUuid(), msg.getDstIpRange()));
            }
            if (StringUtils.isNotEmpty(msg.getSrcIpRange())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because security group rule[%s] type is Egress, srcIpRange[%s] cannot be set", msg.getUuid(), msg.getSrcIpRange()));
            }
            if (StringUtils.isNotEmpty(msg.getRemoteSecurityGroupUuid())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because dstIpRange[%s] is set, remoteSecurityGroupUuid[%s] must be empty", msg.getDstIpRange(), msg.getRemoteSecurityGroupUuid()));
            }

            msg.setDstIpRange(normalizeIpOrPort(msg.getDstIpRange()));
            validateIps(msg.getDstIpRange(), vo.getIpVersion());
        }

        if (StringUtils.isNotEmpty(msg.getRemoteSecurityGroupUuid())) {
            if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, msg.getRemoteSecurityGroupUuid()).isExists()) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because remote security group[uuid:%s] not found", msg.getRemoteSecurityGroupUuid()));
            }
            if (StringUtils.isNotEmpty(msg.getSrcIpRange()) || StringUtils.isNotEmpty(msg.getDstIpRange())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because remote security group[uuid:%s] is set, srcIpRange and dstIpRange must be empty", msg.getRemoteSecurityGroupUuid()));
            }
        }

        if (StringUtils.isNotEmpty(msg.getSrcIpRange()) || StringUtils.isNotEmpty(msg.getDstIpRange()) || StringUtils.isNotEmpty(msg.getRemoteSecurityGroupUuid())) {
            if (StringUtils.isNotEmpty(msg.getSrcIpRange())) {
                msg.setRemoteSecurityGroupUuid(null);
                msg.setDstIpRange(null);
            }
            if (StringUtils.isNotEmpty(msg.getDstIpRange())) {
                msg.setRemoteSecurityGroupUuid(null);
                msg.setSrcIpRange(null);
            }
            if (StringUtils.isNotEmpty(msg.getRemoteSecurityGroupUuid())) {
                msg.setSrcIpRange(null);
                msg.setDstIpRange(null);
            }
        } else {
            if (msg.getSrcIpRange() != null && msg.getSrcIpRange().isEmpty()) {
                msg.setSrcIpRange(null);
            } else {
                msg.setSrcIpRange(vo.getSrcIpRange());
            }

            if (msg.getDstIpRange() != null && msg.getDstIpRange().isEmpty()) {
                msg.setDstIpRange(null);
            } else {
                msg.setDstIpRange(vo.getDstIpRange());
            }

            if (msg.getRemoteSecurityGroupUuid() != null && msg.getRemoteSecurityGroupUuid().isEmpty()) {
                msg.setRemoteSecurityGroupUuid(null);
            } else {
                msg.setRemoteSecurityGroupUuid(vo.getRemoteSecurityGroupUuid());
            }

        }

        if (StringUtils.isNotEmpty(msg.getDstPortRange())) {
            if (SecurityGroupRuleProtocolType.ICMP.toString().equals(msg.getProtocol()) || SecurityGroupRuleProtocolType.ALL.toString().equals(msg.getProtocol())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because rule protocol is [%s], dstPortRange cannot be set", msg.getProtocol()));
            }
            msg.setDstPortRange(normalizeIpOrPort(msg.getDstPortRange()));
            validatePorts(msg.getDstPortRange());
        } else if (msg.getDstPortRange() != null) {
            if (SecurityGroupRuleProtocolType.TCP.toString().equals(msg.getProtocol()) || SecurityGroupRuleProtocolType.UDP.toString().equals(msg.getProtocol())) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because rule protocol is [%s], dstPortRange cannot be empty", msg.getProtocol()));
            }
            msg.setDstPortRange(null);
        } else {
            if (SecurityGroupRuleProtocolType.ICMP.toString().equals(msg.getProtocol()) || SecurityGroupRuleProtocolType.ALL.toString().equals(msg.getProtocol())) {
                msg.setDstPortRange(null);
            } else {
                if (vo.getDstPortRange() == null) {
                    throw new ApiMessageInterceptionException(argerr("could not change security group rule, because rule protocol is [%s], dstPortRange must be set", msg.getProtocol()));
                }
                msg.setDstPortRange(vo.getDstPortRange());
            }
        }

        APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO sao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
        sao.setType(vo.getType().toString());
        sao.setIpVersion(vo.getIpVersion());
        sao.setAction(msg.getAction());
        sao.setProtocol(msg.getProtocol());
        sao.setSrcIpRange(msg.getSrcIpRange());
        sao.setDstIpRange(msg.getDstIpRange());
        sao.setDstPortRange(msg.getDstPortRange());
        sao.setRemoteSecurityGroupUuid(msg.getRemoteSecurityGroupUuid());

        // check duplicate in database
        List<SecurityGroupRuleVO> others = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, vo.getSecurityGroupUuid())
                .eq(SecurityGroupRuleVO_.type, vo.getType())
                .notEq(SecurityGroupRuleVO_.uuid, vo.getUuid()).list();
        for (SecurityGroupRuleVO o : others) {
            APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
            ao.setType(o.getType().toString());
            ao.setIpVersion(o.getIpVersion());
            ao.setProtocol(o.getProtocol().toString());
            ao.setRemoteSecurityGroupUuid(o.getRemoteSecurityGroupUuid());
            ao.setAction(o.getAction());
            ao.setSrcIpRange(o.getSrcIpRange());
            ao.setDstIpRange(o.getDstIpRange());
            ao.setDstPortRange(o.getDstPortRange());
            if (sao.equals(ao)) {
                throw new ApiMessageInterceptionException(argerr("could not change security group rule, because rule[%s] is duplicated to rule[uuid:%s] in datebase", JSONObjectUtil.toJsonString(sao), o.getUuid()));
            }
        }
    }

    private void validatePorts(String ports) {
        if (ports.isEmpty() || ports.startsWith(SecurityGroupConstant.IP_SPLIT) || ports.endsWith(SecurityGroupConstant.IP_SPLIT)) {
            throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid ports[%s]", ports));
        }
        String portArray[];
        if (ports.contains(SecurityGroupConstant.IP_SPLIT)) {
            String[] tmpPorts = ports.split(String.format("%s|%s", SecurityGroupConstant.IP_SPLIT, SecurityGroupConstant.RANGE_SPLIT));
            if (tmpPorts.length > SecurityGroupConstant.PORT_GROUP_NUMBER_LIMIT) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid ports[%s], port range[%s] number[%d] is out of max limit[%d]", ports, Arrays.toString(tmpPorts), tmpPorts.length, SecurityGroupConstant.PORT_GROUP_NUMBER_LIMIT));
            }

            portArray = ports.split(SecurityGroupConstant.IP_SPLIT);
            Stream<String> stream = Stream.of(portArray).distinct();
            if (portArray.length != stream.count()) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid ports[%s], port duplicate", ports));
            }
        } else {
            portArray = new String[]{ports};
        }

        for (String port : portArray) {
            if (port.isEmpty()) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid ports[%s]", ports));
            }
            if (port.contains(SecurityGroupConstant.RANGE_SPLIT)) {
                String portRange[] = port.split(SecurityGroupConstant.RANGE_SPLIT);
                if (portRange.length != 2) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid port range[%s]", port));
                }

                try {
                    Integer startPort = Integer.valueOf(portRange[0]);
                    Integer endPort = Integer.valueOf(portRange[1]);
                    if (startPort >= endPort || startPort < SecurityGroupConstant.PORT_NUMBER_MIN
                        || endPort > SecurityGroupConstant.PORT_NUMBER_MAX) {
                        throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid port range[%s]", port));
                    }
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid port range[%s]", port));
                }
            } else {
                try {
                    Integer.valueOf(port);
                    if (Integer.valueOf(port) < SecurityGroupConstant.PORT_NUMBER_MIN
                        || Integer.valueOf(port) > SecurityGroupConstant.PORT_NUMBER_MAX) {
                        throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid port[%s]", port));
                    }
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_PORT_FIELD_ERROR, "invalid port[%s]", port));
                }
            }
        }
    }

    private void validateIps(String ips, Integer ipVersion) {
        if (ips.isEmpty() || ips.startsWith(SecurityGroupConstant.IP_SPLIT) || ips.endsWith(SecurityGroupConstant.IP_SPLIT)) {
            throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ips[%s]", ips));
        }
        String ipArray[];
        if (ips.contains(SecurityGroupConstant.IP_SPLIT)) {
            ipArray = ips.split(SecurityGroupConstant.IP_SPLIT);
            if (ipArray.length > SecurityGroupConstant.IP_GROUP_NUMBER_LIMIT) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ips[%s], ip number[%d] is out of max limit[%d]", ips, ipArray.length, SecurityGroupConstant.IP_GROUP_NUMBER_LIMIT));
            }
            Stream<String> stream = Stream.of(ipArray).distinct();
            if (ipArray.length != stream.count()) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ips[%s], ip duplicate", ips));
            }
            if (ipVersion == IPv6Constants.IPv6) {
                List<String> ipv6List = Stream.of(ipArray).filter(ip -> ip.contains(SecurityGroupConstant.RANGE_SPLIT)).collect(Collectors.toList());
                if (!ipv6List.isEmpty()) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ips[%s], ip range cannot be used when specifying multiple ipv6 addresses", ips));
                }
            }
        } else {
            ipArray = new String[]{ips};
        }

        for (String ip : ipArray) {
            if (ip.isEmpty()) {
                throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ips[%s]", ips));
            }
            if (ip.contains(SecurityGroupConstant.CIDR_SPLIT)) {
                if (!NetworkUtils.isCidr(ip, ipVersion)) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid cidr[%s], ipVersion[%d]", ip, ipVersion));
                }
                if (ipVersion == IPv6Constants.IPv4 && NetworkUtils.isFullCidr(ip)) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "ipv4 cidr can not be 0.0.0.0/0"));
                } if (ipVersion == IPv6Constants.IPv6 && IPv6NetworkUtils.isFullCidr(ip)) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "ipv6 cidr can not be ::/0"));
                }
                continue;
            }
            if (ip.contains(SecurityGroupConstant.RANGE_SPLIT)) {
                String[] ipRangeArray = ip.split(SecurityGroupConstant.RANGE_SPLIT);
                if (ipRangeArray.length != 2) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ip range[%s]", ip));
                }
                String startIp = ipRangeArray[0];
                String endIp = ipRangeArray[1];
                if (ipVersion == IPv6Constants.IPv4) {
                    NetworkUtils.validateIpRange(startIp, endIp);
                } else {
                    if (!IPv6NetworkUtils.isIpv6Address(startIp) || !IPv6NetworkUtils.isIpv6Address(endIp) || startIp.compareTo(endIp) > 0) {
                        throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ip range[%s]", ip));
                    }
                }
                continue;
            }
            if (ipVersion == IPv6Constants.IPv4) {
                if (!NetworkUtils.isIpv4Address(ip)) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ip[%s], ipVersion[%d]", ip, ipVersion));
                }
            } else {
                if (!IPv6NetworkUtils.isValidIpv6(ip)) {
                    throw new ApiMessageInterceptionException(err(SecurityGroupErrors.RULE_IP_FIELD_ERROR, "invalid ip[%s], ipVersion[%d]", ip, ipVersion));
                }
            }
        }
    }

    private void validate(APIDetachSecurityGroupFromL3NetworkMsg msg) {
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        if (!q.isExists()) {
            throw new ApiMessageInterceptionException(operr("security group[uuid:%s] has not attached to l3Network[uuid:%s], can't detach",
                            msg.getSecurityGroupUuid(), msg.getL3NetworkUuid()));
        }
    }

    private void validate(APIDeleteVmNicFromSecurityGroupMsg msg) {
        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.select(VmNicSecurityGroupRefVO_.vmNicUuid);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.IN, msg.getVmNicUuids());
        q.add(VmNicSecurityGroupRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        List<String> vmNicUuids = q.listValue();
        if (vmNicUuids.isEmpty()) {
            APIDeleteVmNicFromSecurityGroupEvent evt = new APIDeleteVmNicFromSecurityGroupEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        msg.setVmNicUuids(vmNicUuids);
    }

    private void validate(APIDeleteSecurityGroupRuleMsg msg) {
        SimpleQuery<SecurityGroupRuleVO> q = dbf.createQuery(SecurityGroupRuleVO.class);
        q.select(SecurityGroupRuleVO_.uuid);
        q.add(SecurityGroupRuleVO_.uuid, Op.IN, msg.getRuleUuids());
        List<String> uuids = q.listValue();
        uuids.retainAll(msg.getRuleUuids());
        if (uuids.isEmpty()) {
            APIDeleteSecurityGroupRuleEvent evt = new APIDeleteSecurityGroupRuleEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        List<SecurityGroupRuleVO> vos = Q.New(SecurityGroupRuleVO.class).in(SecurityGroupRuleVO_.uuid, uuids).list();
        String sguuid = vos.get(0).getSecurityGroupUuid();
        vos.stream().forEach(vo -> {
            if (!sguuid.equals(vo.getSecurityGroupUuid())) {
                throw new ApiMessageInterceptionException(argerr("can't delete rules of different security group"));
            }
            if (vo.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
                throw new ApiMessageInterceptionException(argerr("can't delete default rule[uuid:%s]", vo.getUuid()));
            }
        });

        msg.setRuleUuids(uuids);
    }

    private void validate(APIDeleteSecurityGroupMsg msg) {
        if (!dbf.isExist(msg.getUuid(), SecurityGroupVO.class)) {
            APIDeleteSecurityGroupEvent evt = new APIDeleteSecurityGroupEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIAttachSecurityGroupToL3NetworkMsg msg) {
        SimpleQuery<SecurityGroupL3NetworkRefVO> q = dbf.createQuery(SecurityGroupL3NetworkRefVO.class);
        q.add(SecurityGroupL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        q.add(SecurityGroupL3NetworkRefVO_.securityGroupUuid, Op.EQ, msg.getSecurityGroupUuid());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("security group[uuid:%s] has attached to l3Network[uuid:%s], can't attach again",
                            msg.getSecurityGroupUuid(), msg.getL3NetworkUuid()));
        }

        SimpleQuery<NetworkServiceL3NetworkRefVO> nq = dbf.createQuery(NetworkServiceL3NetworkRefVO.class);
        nq.add(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, Op.EQ, msg.getL3NetworkUuid());
        nq.add(NetworkServiceL3NetworkRefVO_.networkServiceType, Op.EQ, SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE);
        if (!nq.isExists()) {
            throw new ApiMessageInterceptionException(argerr("the L3 network[uuid:%s] doesn't have the network service type[%s] enabled", msg.getL3NetworkUuid(), SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE));
        }
    }

    private void validate(APIAddVmNicToSecurityGroupMsg msg) {
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.select(VmNicVO_.uuid);
        q.add(VmNicVO_.uuid, Op.IN, msg.getVmNicUuids());
        List<String> uuids = q.listValue();
        if (!uuids.containsAll(msg.getVmNicUuids())) {
            msg.getVmNicUuids().removeAll(uuids);
            throw new ApiMessageInterceptionException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find vm nics[uuids:%s]", msg.getVmNicUuids()
            ));
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.securityGroupUuid, msg.getSecurityGroupUuid()).list();
        if (!refs.isEmpty()) {
            refs.stream().forEach(ref -> {
                if (uuids.contains(ref.getVmNicUuid())) {
                    throw new ApiMessageInterceptionException(argerr("vm nic[uuid:%s] has been attach to security group[uuid:%s]", ref.getVmNicUuid(), msg.getSecurityGroupUuid()));
                }
            });
        }

        checkIfL3NetworkSupportSecurityGroup(uuids);

        msg.setVmNicUuids(uuids);
    }

    private void checkIfL3NetworkSupportSecurityGroup(List<String> vmNicUuids) {
        if (vmNicUuids.isEmpty()) {
            return;
        }

        List<VmNicVO> nics = Q.New(VmNicVO.class).in(VmNicVO_.uuid, vmNicUuids).list();

        for(VmNicVO nic : nics) {
            if (!Q.New(NetworkServiceL3NetworkRefVO.class).eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, nic.getL3NetworkUuid())
                    .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE).isExists()) {
                throw new ApiMessageInterceptionException(argerr("the netwotk service[type:%s] not enabled on the l3Network[uuid:%s] of nic[uuid:%s]", SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE, nic.getL3NetworkUuid(), nic.getUuid()));
            }
        }
    }

    @Transactional(readOnly = true)
    private void checkIfVmNicFromAttachedL3Networks(String securityGroupUuid, List<String> uuids) {
        String sql = "select nic.uuid from SecurityGroupL3NetworkRefVO ref, VmNicVO nic, UsedIpVO ip where ref.l3NetworkUuid = ip.l3NetworkUuid and ip.vmNicUuid = nic.uuid " +
                " and ref.securityGroupUuid = :sgUuid and nic.uuid in (:nicUuids)";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("nicUuids", uuids);
        q.setParameter("sgUuid", securityGroupUuid);
        List<String> nicUuids = q.getResultList();

        List<String> wrongUuids = new ArrayList<String>();
        for (String uuid : uuids) {
            if (!nicUuids.contains(uuid)) {
                wrongUuids.add(uuid);
            }
        }

        if (!wrongUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("VM nics[uuids:%s] are not on L3 networks that have been attached to the security group[uuid:%s]",
                            wrongUuids, securityGroupUuid));
        }
    }

    private boolean checkAllowedCidrValid(String cidr) {
        if (StringUtils.isEmpty(cidr)) {
            return false;
        }

        if (SecurityGroupConstant.WORLD_OPEN_CIDR.equals(cidr) || SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6.equals(cidr)) {
            return false;

        }

        return true;
    }

    private void validate(APIAddSecurityGroupRuleMsg msg) {
        String sgUuid = msg.getSecurityGroupUuid();
        List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> rules = msg.getRules();

        if (!Q.New(SecurityGroupVO.class).eq(SecurityGroupVO_.uuid, sgUuid).isExists()) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group[uuid:%s] does not exist", sgUuid));
        }
        if (rules.isEmpty() || rules.size() > SecurityGroupConstant.ONE_API_RULES_MAX_NUM) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the rules cannot be empty or exceed the max number %d",  SecurityGroupConstant.ONE_API_RULES_MAX_NUM));
        }

        if (msg.getRemoteSecurityGroupUuids() != null && !msg.getRemoteSecurityGroupUuids().isEmpty()) {
            if (msg.getRemoteSecurityGroupUuids().stream().distinct().count() != msg.getRemoteSecurityGroupUuids().size()) {
                throw new ApiMessageInterceptionException(argerr("could not add security group rule, because duplicate uuid in remoteSecurityGroupUuids: %s", msg.getRemoteSecurityGroupUuids()));
            }

            List<String> sgUuids = Q.New(SecurityGroupVO.class).select(SecurityGroupVO_.uuid).in(SecurityGroupVO_.uuid, msg.getRemoteSecurityGroupUuids()).listValues();
            msg.getRemoteSecurityGroupUuids().stream().forEach(uuid -> {
                sgUuids.stream().filter(s -> s.equals(uuid)).findFirst().orElseThrow(() ->
                        new ApiMessageInterceptionException(argerr("could not add security group rule, because security group[uuid:%s] does not exist", uuid)));
            });

            rules.stream().forEach(r -> {
                if (r.getRemoteSecurityGroupUuid() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the remote security group uuid is conflict"));
                }
            });

            List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> aos = new ArrayList<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO>();

            msg.getRemoteSecurityGroupUuids().stream().forEach(uuid -> {
                rules.stream().forEach(r -> {
                    APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
                    ao.setType(r.getType());
                    ao.setState(r.getState());
                    ao.setDescription(r.getDescription());
                    ao.setRemoteSecurityGroupUuid(uuid);
                    ao.setIpVersion(r.getIpVersion());
                    ao.setProtocol(r.getProtocol());
                    ao.setSrcIpRange(r.getSrcIpRange());
                    ao.setDstIpRange(r.getDstIpRange());
                    ao.setDstPortRange(r.getDstPortRange());
                    ao.setAction(r.getAction());
                    ao.setAllowedCidr(r.getAllowedCidr());
                    ao.setStartPort(r.getStartPort());
                    ao.setEndPort(r.getEndPort());
                    aos.add(ao);
                });
            });

            if (!aos.isEmpty()) {
                msg.setRules(aos);
            }
        }

        if (msg.getPriority() == null || msg.getPriority() < 0) {
            msg.setPriority(SecurityGroupConstant.LOWEST_RULE_PRIORITY);
        }

        if (msg.getPriority() == SecurityGroupConstant.DEFAULT_RULE_PRIORITY) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because rule priority must greater than %d or equals %d", SecurityGroupConstant.DEFAULT_RULE_PRIORITY, SecurityGroupConstant.LOWEST_RULE_PRIORITY));
        }

        List<APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO> newRules = msg.getRules();

        // Basic check
        for (APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao : newRules) {
            if (!SecurityGroupRuleProtocolType.isValid(ao.getProtocol())) {
                throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule protocol[%s], valid protocols are %s", ao.getProtocol(), SecurityGroupRuleProtocolType.getAllProtocol()));
            }

            if (ao.getAction() == null) {
                ao.setAction(SecurityGroupRuleAction.ACCEPT.toString());
            } else {
                if (!SecurityGroupRuleAction.isValid(ao.getAction())) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule action[%s], valid actions are %s", ao.getAction(), SecurityGroupRuleAction.getAllAction()));
                }
            }

            if (ao.getIpVersion() == null) {
                ao.setIpVersion(IPv6Constants.IPv4);
            } else {
                if (ao.getIpVersion() != IPv6Constants.IPv4 && ao.getIpVersion() != IPv6Constants.IPv6) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule ipVersion[%d], valid ipVersions are %d/%d", ao.getIpVersion(), IPv6Constants.IPv4, IPv6Constants.IPv6));
                }
            }

            if (StringUtils.isEmpty(ao.getAllowedCidr())) {
                ao.setAllowedCidr(ao.getIpVersion() == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
            }

            if (SecurityGroupRuleType.Egress.toString().equals(ao.getType())) {
                if (ao.getSrcIpRange() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the srcIpRange[%s] is not allowed to set for egress rule", ao.getSrcIpRange()));
                }

                if (checkAllowedCidrValid(ao.getAllowedCidr())) {
                    if (ao.getDstIpRange() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the allowedCidr[%s] and dstIpRange[%s] are in conflict", ao.getAllowedCidr(), ao.getDstIpRange()));
                    }
                    ao.setDstIpRange(ao.getAllowedCidr());
                }

                if (ao.getDstIpRange() != null) {
                    if (ao.getRemoteSecurityGroupUuid() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the ip range[%s] and remoteSecurityGroupUuid[%s] are in conflict", ao.getDstIpRange(), ao.getRemoteSecurityGroupUuid()));
                    }
                    ao.setDstIpRange(normalizeIpOrPort(ao.getDstIpRange()));
                    validateIps(ao.getDstIpRange(), ao.getIpVersion());
                }
            } else {
                if (ao.getDstIpRange() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the dstIpRange[%s] is not allowed to set for ingress rule", ao.getDstIpRange()));
                }

                if (checkAllowedCidrValid(ao.getAllowedCidr())) {
                    if (ao.getSrcIpRange() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the allowedCidr[%s] and srcIpRange[%s] are in conflict", ao.getAllowedCidr(), ao.getSrcIpRange()));
                    }
                    ao.setSrcIpRange(ao.getAllowedCidr());
                }

                if (ao.getSrcIpRange() != null) {
                    if (ao.getRemoteSecurityGroupUuid() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the ip range[%s] and remoteSecurityGroupUuid[%s] are in conflict", ao.getSrcIpRange(), ao.getRemoteSecurityGroupUuid()));
                    }
                    ao.setSrcIpRange(normalizeIpOrPort(ao.getSrcIpRange()));
                    validateIps(ao.getSrcIpRange(), ao.getIpVersion());
                }
            }

            if (ao.getStartPort() == null) {
                ao.setStartPort(-1);
            }

            if (ao.getEndPort() == null) {
                ao.setEndPort(-1);
            }

            if (SecurityGroupRuleProtocolType.ALL.toString().equals(ao.getProtocol()) || SecurityGroupRuleProtocolType.ICMP.toString().equals(ao.getProtocol())) {
                if (ao.getDstPortRange() != null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the protocol type ALL or ICMP cant not set dstPortRange[%s]", ao.getDstPortRange()));
                }
                if (ao.getStartPort() != -1 || ao.getEndPort() != -1) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the protocol type ALL or ICMP cant not set startPort or endPort"));
                }
            } else {
                if (ao.getStartPort() >= SecurityGroupConstant.PORT_NUMBER_MIN && ao.getEndPort() <= SecurityGroupConstant.PORT_NUMBER_MAX) {
                    if (ao.getStartPort() > ao.getEndPort()) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because invalid rule endPort[%d], endPort must be greater than or equal to startPort[%d]", ao.getEndPort(), ao.getStartPort()));
                    }
                    if (ao.getDstPortRange() != null) {
                        throw new ApiMessageInterceptionException(argerr("could not add security group rule, because dstPortRange[%s] and starPort[%s] are in conflict", ao.getDstPortRange(), ao.getStartPort()));
                    }


                    if (ao.getStartPort().equals(ao.getEndPort())) {
                        ao.setDstPortRange(String.valueOf(ao.getStartPort()));
                    } else {
                        ao.setDstPortRange(String.format("%s-%s", ao.getStartPort(), ao.getEndPort()));
                    }
                }

                if (ao.getDstPortRange() == null) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because the protocol type TCP/UDP must set dstPortRange"));
                }
                ao.setDstPortRange(normalizeIpOrPort(ao.getDstPortRange()));
                validatePorts(ao.getDstPortRange());
            }
        }

        // Deduplicate in API
        for (int i = 0; i < newRules.size() - 1; i++) {
            for (int j = newRules.size() - 1; j > i; j--) {
                if (newRules.get(i).equals(newRules.get(j))) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because rule[%s] and rule[%s] are dupilicated",
                            JSONObjectUtil.toJsonString(newRules.get(i)), JSONObjectUtil.toJsonString(newRules.get(j))));
                }
            }
        }

        // Deduplicate in DB
        List<SecurityGroupRuleVO> vos = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sgUuid).list();

        for (SecurityGroupRuleVO vo : vos) {
            APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
            ao.setType(vo.getType().toString());
            ao.setAllowedCidr(vo.getAllowedCidr());
            ao.setProtocol(vo.getProtocol().toString());
            ao.setStartPort(vo.getStartPort());
            ao.setEndPort(vo.getEndPort());
            ao.setIpVersion(vo.getIpVersion());
            ao.setRemoteSecurityGroupUuid(vo.getRemoteSecurityGroupUuid());
            ao.setAction(vo.getAction());
            ao.setSrcIpRange(vo.getSrcIpRange());
            ao.setDstIpRange(vo.getDstIpRange());
            ao.setDstPortRange(vo.getDstPortRange());
            for (APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO sao : newRules) {
                if (ao.equals(sao)) {
                    throw new ApiMessageInterceptionException(argerr("could not add security group rule, because rule[%s] is duplicated to rule[uuid:%s] in datebase", JSONObjectUtil.toJsonString(sao), vo.getUuid()));
                }
            }
        }

        int ingressRuleCount = vos.stream().filter(vo -> SecurityGroupRuleType.Ingress.equals(vo.getType()) && vo.getPriority() != SecurityGroupConstant.DEFAULT_RULE_PRIORITY).collect(Collectors.toList()).size();
        int egressRuleCount = vos.stream().filter(vo -> SecurityGroupRuleType.Egress.equals(vo.getType()) && vo.getPriority() != SecurityGroupConstant.DEFAULT_RULE_PRIORITY).collect(Collectors.toList()).size();
        int toCreateIngressRuleCount = newRules.stream().filter(ao -> SecurityGroupRuleType.Ingress.toString().equals(ao.getType())).collect(Collectors.toList()).size();
        int toCreateEgressRuleCount = newRules.stream().filter(ao -> SecurityGroupRuleType.Egress.toString().equals(ao.getType())).collect(Collectors.toList()).size();

        if (ingressRuleCount >= SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class) && toCreateIngressRuleCount > 0) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules has reached the maximum limit[%d]",
                    SecurityGroupRuleType.Ingress, SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if (egressRuleCount >= SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class) && toCreateEgressRuleCount > 0) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules has reached the maximum limit[%d]",
                    SecurityGroupRuleType.Egress, SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if ((ingressRuleCount + toCreateIngressRuleCount) > SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules number[%d] is out of max limit[%d]",
                    SecurityGroupRuleType.Ingress, (ingressRuleCount + toCreateIngressRuleCount), SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if ((egressRuleCount + toCreateEgressRuleCount) > SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because security group %s rules number[%d] is out of max limit[%d]",
                    SecurityGroupRuleType.Egress, (egressRuleCount + toCreateEgressRuleCount), SecurityGroupGlobalConfig.SECURITY_GROUP_RULES_NUM_LIMIT.value(Integer.class)));
        }
        if (msg.getPriority() > (ingressRuleCount + 1) && toCreateIngressRuleCount > 0) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because priority[%d] must be consecutive, the ingress rule maximum priority is [%d]", msg.getPriority(), ingressRuleCount));
        }
        if (msg.getPriority() > (egressRuleCount + 1) && toCreateEgressRuleCount > 0) {
            throw new ApiMessageInterceptionException(argerr("could not add security group rule, because priority[%d] must be consecutive, the egress rule maximum priority is [%d]", msg.getPriority(), egressRuleCount));
        }
    }

    private void validate(APICreateSecurityGroupMsg msg) {
        if (msg.getIpVersion() == null) {
            msg.setIpVersion(IPv6Constants.IPv4);
        }
    }
}
