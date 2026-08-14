package org.zstack.network.securitygroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6Constants;

public class SecurityGroupUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(SecurityGroupUpgradeExtension.class);

    @Autowired
    DatabaseFacade dbf;

    private void upgradeSecurityGroup() {

        upgradeSecurityGroupRef();

        List<SecurityGroupVO> sgs = dbf.listAll(SecurityGroupVO.class);
        if (sgs.isEmpty()) {
            return;
        }

        sgs.forEach(sg -> {
            List<SecurityGroupRuleVO> rules = Q.New(SecurityGroupRuleVO.class).eq(SecurityGroupRuleVO_.securityGroupUuid, sg.getUuid()).list();

            List<SecurityGroupRuleVO> ingressRules = rules.stream().filter(r -> r.getType().equals(SecurityGroupRuleType.Ingress)).collect(Collectors.toList());
            List<SecurityGroupRuleVO> egressRules = rules.stream().filter(r -> r.getType().equals(SecurityGroupRuleType.Egress)).collect(Collectors.toList());

            upgradeSecurityGroupRules(sg.getUuid(), ingressRules, SecurityGroupRuleType.Ingress);
            upgradeSecurityGroupRules(sg.getUuid(), egressRules, SecurityGroupRuleType.Egress);
        });
    }

    private SecurityGroupRuleVO getDefaultSecurityGroupRule(List<SecurityGroupRuleVO> rules, SecurityGroupRuleType type, Integer ipVersion) {
        String cidr = ipVersion == IPv6Constants.IPv4 ? SecurityGroupConstant.WORLD_OPEN_CIDR : SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6;

        for (SecurityGroupRuleVO r : rules) {
            if (r.getRemoteSecurityGroupUuid() == null || r.getIpVersion() == null) {
                continue;
            }
            if (r.getType().equals(type) && r.getIpVersion().equals(ipVersion) && r.getAllowedCidr().equals(cidr)
                && r.getStartPort() == -1 && r.getEndPort() == -1 && r.getProtocol().equals(SecurityGroupRuleProtocolType.ALL)
                && r.getRemoteSecurityGroupUuid().equals(r.getSecurityGroupUuid())) {
            
                return r;
            }
        }

        return null;
    }

    private void upgradeSecurityGroupRules(String sgUuid, List<SecurityGroupRuleVO> rules, SecurityGroupRuleType type) {
        List<SecurityGroupRuleVO> defaultRules = new ArrayList<>();
        SecurityGroupRuleVO ip4DefaultRule = getDefaultSecurityGroupRule(rules, type, IPv6Constants.IPv4);
        SecurityGroupRuleVO ip6DefaultRule = getDefaultSecurityGroupRule(rules, type, IPv6Constants.IPv6);
        
        if (ip4DefaultRule == null) {
            SecurityGroupRuleVO rule = new SecurityGroupRuleVO();
            rule.setUuid(Platform.getUuid());
            rule.setPriority(SecurityGroupConstant.DEFAULT_RULE_PRIORITY);
            rule.setDescription(SecurityGroupConstant.DEFAULT_RULE_DESCRIPTION);
            rule.setAllowedCidr(SecurityGroupConstant.WORLD_OPEN_CIDR);
            rule.setSecurityGroupUuid(sgUuid);
            rule.setStartPort(-1);
            rule.setEndPort(-1);
            rule.setProtocol(SecurityGroupRuleProtocolType.ALL);
            rule.setType(type);
            rule.setIpVersion(IPv6Constants.IPv4);
            rule.setRemoteSecurityGroupUuid(sgUuid);
            rule.setAction(SecurityGroupRuleAction.ACCEPT.toString());
            rule.setState(SecurityGroupRuleState.Disabled);
            dbf.persist(rule);
        } else {
            ip4DefaultRule.setPriority(SecurityGroupConstant.DEFAULT_RULE_PRIORITY);
            ip4DefaultRule.setDescription(SecurityGroupConstant.DEFAULT_RULE_DESCRIPTION);
            defaultRules.add(ip4DefaultRule);
        }

        if (ip6DefaultRule == null) {
            SecurityGroupRuleVO rule = new SecurityGroupRuleVO();
            rule.setUuid(Platform.getUuid());
            rule.setPriority(SecurityGroupConstant.DEFAULT_RULE_PRIORITY);
            rule.setDescription(SecurityGroupConstant.DEFAULT_RULE_DESCRIPTION);
            rule.setAllowedCidr(SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6);
            rule.setSecurityGroupUuid(sgUuid);
            rule.setStartPort(-1);
            rule.setEndPort(-1);
            rule.setProtocol(SecurityGroupRuleProtocolType.ALL);
            rule.setType(type);
            rule.setIpVersion(IPv6Constants.IPv6);
            rule.setRemoteSecurityGroupUuid(sgUuid);
            rule.setAction(SecurityGroupRuleAction.ACCEPT.toString());
            rule.setState(SecurityGroupRuleState.Disabled);
            dbf.persist(rule);
        } else {
            ip6DefaultRule.setPriority(SecurityGroupConstant.DEFAULT_RULE_PRIORITY);
            ip6DefaultRule.setDescription(SecurityGroupConstant.DEFAULT_RULE_DESCRIPTION);
            defaultRules.add(ip6DefaultRule);
        }

        if (!defaultRules.isEmpty()) {
            dbf.updateCollection(defaultRules);
        }

        boolean isUpdate = rules.stream().anyMatch(r -> r.getPriority() == SecurityGroupConstant.LOWEST_RULE_PRIORITY);
        if (!isUpdate) {
            return;
        }

        List<SecurityGroupRuleVO> userRules = rules.stream().filter(r -> r.getPriority() != SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                .sorted(Comparator.comparing(SecurityGroupRuleVO::getCreateDate)).collect(Collectors.toList());
        if (userRules.isEmpty()) {
            return;
        }

        if (SecurityGroupRuleType.Egress.equals(type)) {
            List<VmNicSecurityPolicyVO> pvos = SQL.New("select policy from VmNicSecurityPolicyVO policy, VmNicVO nic, VmNicSecurityGroupRefVO ref" +
                    " where policy.vmNicUuid = nic.uuid" +
                    " and ref.securityGroupUuid = :sgUuid" +
                    " and ref.vmNicUuid = nic.uuid", VmNicSecurityPolicyVO.class)
                    .param("sgUuid", sgUuid)
                    .list();
            pvos.forEach(pvo -> {
                pvo.setEgressPolicy(VmNicSecurityPolicy.DENY.toString());
            });

            dbf.updateCollection(pvos);
        }

        userRules.forEach(r -> {
            r.setPriority(userRules.indexOf(r) + 1);
            if (!SecurityGroupConstant.WORLD_OPEN_CIDR.equals(r.getAllowedCidr()) && !SecurityGroupConstant.WORLD_OPEN_CIDR_IPV6.equals(r.getAllowedCidr())) {
                if (SecurityGroupRuleType.Ingress.equals(type)) {
                    r.setSrcIpRange(r.getAllowedCidr());
                } else {
                    r.setDstIpRange(r.getAllowedCidr());
                }
            }

            if (r.getStartPort() != -1) {
                if (r.getStartPort() == r.getEndPort()) {
                    r.setDstPortRange(String.valueOf(r.getStartPort()));
                } else {
                    r.setDstPortRange(String.format("%d-%d", r.getStartPort(), r.getEndPort()));
                }
            }
        });

        dbf.updateCollection(userRules);
    }

    private void upgradeSecurityGroupRef() {
        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).list();
        if (refs.isEmpty()) {
            return;
        }

        List<VmNicSecurityPolicyVO> pvos = Q.New(VmNicSecurityPolicyVO.class).list();
        
        Map<String, List<VmNicSecurityGroupRefVO>> nicMap = new HashMap<>();
        List<VmNicSecurityPolicyVO> toCreate = new ArrayList<>();
        
        for (VmNicSecurityGroupRefVO ref : refs) {
            if (!nicMap.containsKey(ref.getVmNicUuid())) {
                nicMap.put(ref.getVmNicUuid(), new ArrayList<>());
            }
            nicMap.get(ref.getVmNicUuid()).add(ref);
        }

        nicMap.keySet().stream().forEach(nicUuid -> {
            if (pvos.stream().anyMatch(pvo -> pvo.getVmNicUuid().equals(nicUuid))) {
                return;
            }
            VmNicSecurityPolicyVO vo = new VmNicSecurityPolicyVO();
            vo.setUuid(Platform.getUuid());
            vo.setVmNicUuid(nicUuid);
            vo.setIngressPolicy(VmNicSecurityPolicy.DENY.toString());
            vo.setEgressPolicy(VmNicSecurityPolicy.ALLOW.toString());
            toCreate.add(vo);
        });

        if (!toCreate.isEmpty()) {
            dbf.persistCollection(toCreate);
        }

        nicMap.values().forEach(refsOfNic -> {
            boolean isUpdate = refsOfNic.stream().anyMatch(r -> r.getPriority() == -1);
            if (!isUpdate) {
                return;
            }

            Map<String, String> sgMap = new HashMap<>();
            List<VmNicSecurityGroupRefVO> toDeleteDuplicate = new ArrayList<>();
            List<VmNicSecurityGroupRefVO> toUpdate = new ArrayList<>();
            refsOfNic.forEach(ref -> {
                if (!sgMap.containsKey(ref.getSecurityGroupUuid())) {
                    sgMap.put(ref.getSecurityGroupUuid(), ref.getSecurityGroupUuid());
                    toUpdate.add(ref);
                } else {
                    toDeleteDuplicate.add(ref);
                }
            });

            if (!toDeleteDuplicate.isEmpty()) {
                dbf.removeCollection(toDeleteDuplicate, VmNicSecurityGroupRefVO.class);
            }

            if (!toUpdate.isEmpty()) {
                List<VmNicSecurityGroupRefVO> sortedRefs = toUpdate.stream().sorted(Comparator.comparing(VmNicSecurityGroupRefVO::getCreateDate)).collect(Collectors.toList());
                sortedRefs.forEach(ref -> {
                    ref.setPriority(sortedRefs.indexOf(ref) + 1);
                });
                dbf.updateCollection(sortedRefs);
            }
        });
    }

    @Override
    public boolean start() {

        if (SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP) {
            upgradeSecurityGroup();
        }

        return true;
    }
 
    @Override
    public boolean stop() {
        return true;
    }
}
