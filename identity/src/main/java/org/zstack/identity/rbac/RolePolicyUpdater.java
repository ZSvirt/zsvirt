package org.zstack.identity.rbac;

import org.springframework.lang.NonNull;
import org.zstack.header.identity.role.RolePolicyEffect;
import org.zstack.header.identity.role.RolePolicyResourceRefVO;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RolePolicyVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.*;

public class RolePolicyUpdater {
    public Map<String, RolePolicy> policies = new HashMap<>();

    public static RolePolicyUpdater of(List<RolePolicyVO> existsPolicies) {
        Map<String, List<RolePolicyVO>> keyPoliciesMap = groupBy(existsPolicies, RolePolicyVO::getActions);
        RolePolicyUpdater updater = new RolePolicyUpdater();
        keyPoliciesMap.forEach((key, vos) -> updater.policies.put(key, RolePolicy.of(key, vos)));
        return updater;
    }

    public static RolePolicyUpdater pure() {
        return new RolePolicyUpdater();
    }

    public void delete(@NonNull RolePolicyStatement statement) {
        String key = statement.actions;
        RolePolicy policy = policies.get(key);

        if (policy == null) {
            return;
        }
        policy.delete(statement);
    }

    public void add(@NonNull RolePolicyStatement statement) {
        String key = statement.actions;
        RolePolicy policy = policies.computeIfAbsent(key, k -> RolePolicy.of(key));
        policy.add(statement);
    }

    public void squeeze() {
        for (RolePolicy policy : policies.values()) {
            policy.squeeze();
        }
    }

    public Set<Long> collectAllPolicyIdsNeedDelete() {
        return policies.values().stream()
                .filter(policy -> policy.updated)
                .flatMapToLong(policy -> Arrays.stream(policy.originalPolicyIds))
                .mapToObj(Long::valueOf)
                .collect(Collectors.toSet());
    }

    public List<RolePolicyVO> collectAllPolicyIdsNeedCreate() {
        return policies.values().stream()
                .filter(policy -> policy.updated)
                .flatMap(policy -> policy.vos.stream())
                .collect(Collectors.toList());
    }

    public static class RolePolicy {
        String key; // api
        List<RolePolicyVO> vos;
        boolean updated = false;
        long[] originalPolicyIds;

        public static RolePolicy of(String key) {
            RolePolicy policy = new RolePolicy();
            policy.key = key;
            policy.vos = new ArrayList<>();
            policy.originalPolicyIds = new long[0];
            return policy;
        }

        public static RolePolicy of(String key, List<RolePolicyVO> vos) {
            RolePolicy policy = new RolePolicy();
            policy.key = key;
            policy.vos = vos;
            policy.originalPolicyIds = vos.stream()
                    .map(RolePolicyVO::getId)
                    .filter(Objects::nonNull)
                    .mapToLong(Long::longValue)
                    .toArray();
            return policy;
        }

        public void delete(RolePolicyStatement statement) {
            if (isEmpty(vos)) {
                return;
            }

            if (isEmpty(statement.resources)) {
                boolean anyDelete = vos.removeIf(vo -> vo.getEffect() == statement.effect);
                if (anyDelete) {
                    renovate();
                }
                return;
            }

            // TODO: we assure all statement.resources[x].effect == Allow here,
            //     because we only support resourceEffect == Allow

            Set<String> allResourceToDelete = transformToSet(statement.resources, resource -> resource.uuid);
            final Set<String> resourceUuidToDelete = vos.stream()
                    .filter(policy -> policy.getResourceRefs() != null)
                    .flatMap(policy -> policy.getResourceRefs().stream())
                    .filter(ref -> allResourceToDelete.contains(ref.getResourceUuid()))
                    .map(RolePolicyResourceRefVO::getResourceUuid)
                    .collect(Collectors.toSet());
            if (resourceUuidToDelete.isEmpty()) {
                return;
            }

            renovate();
            for (Iterator<RolePolicyVO> iterator = vos.iterator(); iterator.hasNext();) {
                RolePolicyVO vo = iterator.next();

                vo.getResourceRefs().removeIf(ref -> resourceUuidToDelete.contains(ref.getResourceUuid()));
                if (vo.getResourceRefs().isEmpty()) {
                    iterator.remove();
                }
            }
        }

        public void add(RolePolicyStatement statement) {
            renovate();
            vos.addAll(statement.toVO());
        }

        private void renovate() {
            if (updated) {
                return;
            }

            updated = true;
            List<RolePolicyVO> origin = vos;
            vos = new ArrayList<>(origin.size());

            for (RolePolicyVO originPolicy : origin) {
                if (originPolicy.getId() == null) {
                    vos.add(originPolicy);
                    continue;
                }

                RolePolicyVO vo = new RolePolicyVO();
                vo.setActions(originPolicy.getActions());
                vo.setEffect(originPolicy.getEffect());
                vo.setResourceType(originPolicy.getResourceType());
                vo.setResourceRefs(new HashSet<>());
                vos.add(vo);

                for (RolePolicyResourceRefVO originRef : originPolicy.getResourceRefs()) {
                    RolePolicyResourceRefVO ref = new RolePolicyResourceRefVO();
                    ref.setResourceUuid(originRef.getResourceUuid());
                    ref.setEffect(originRef.getEffect());
                    vo.getResourceRefs().add(ref);
                }
            }
        }

        /**
         * <p><b>Example 1</b><br/>
         * first: vos=["API", "Exclude: API"]<br/>
         * then:  vos=["Exclude: API"]<br/>
         *
         * <p><b>Example 2</b><br/>
         * first: vos=["Exclude: API", "API"]<br/>
         * then:  vos=["API"]<br/>
         *
         * <p><b>Example 3</b><br/>
         * first: vos=["Exclude: API", "API", "API", "API", "API", "API"]<br/>
         * then:  vos=["API"]<br/>
         *
         * <p><b>Example 4</b><br/>
         * first: vos=["Exclude: API", "API", "Exclude: API", "API", "Exclude: API"]<br/>
         * then:  vos=["Exclude: API"] (because last one is Exclude)<br/>
         *
         * <p><b>Example 5</b><br/>
         * first: vos=["API -> uuid-1", "API -> uuid-2"] (uuid-1 is VM, uuid-2 is VM) <br/>
         * then:  vos=["API -> uuid-1,uuid-2"]<br/>
         *
         * <p><b>Example 6</b><br/>
         * first: vos=["API -> uuid-1", "API -> uuid-2"] (uuid-1 is VM, uuid-2 is L3) <br/>
         * then:  vos=["API -> uuid-1, "API -> uuid-2"]<br/>
         *
         * <p><b>Example 7</b><br/>
         * first: vos=["API -> uuid-1", "API -> uuid-2", "Exclude: API"]<br/>
         * then:  vos=["Exclude: API"]<br/>
         *
         * <p><b>Example 8</b><br/>
         * first: vos=["API -> uuid-1", "Exclude: API", "API -> uuid-2"]<br/>
         * then:  vos=["API -> uuid-2"] (because last one is allow with specific UUID)<br/>
         *
         * </p>
         */
        public void squeeze() {
            if (isEmpty(vos) || vos.size() == 1) {
                return;
            }

            // Squeeze by RolePolicyVO.effect
            final RolePolicyVO[] array = vos.toArray(new RolePolicyVO[0]);
            RolePolicyEffect lastItemEffect = array[array.length - 1].getEffect();
            int i = array.length - 2;
            for (; i >= 0; i--) {
                RolePolicyVO vo = array[i];
                if (vo.getEffect() != lastItemEffect) {
                    break;
                }
            }
            if (i >= 0) {
                Arrays.fill(array, 0, i + 1, null);
                vos = Arrays.stream(array)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            // Squeeze by RolePolicyVO.resourceType
            // Only policy[effect=Allow] support resourceType
            final RolePolicyVO withoutResource = findOneOrNull(vos, policy -> policy.getResourceType() == null);
            if (withoutResource != null) {
                boolean anyDelete = vos.removeIf(vo -> vo != withoutResource);
                if (anyDelete) {
                    renovate();
                }
                return;
            }

            Map<String, List<RolePolicyVO>> typePoliciesMap = groupBy(vos, RolePolicyVO::getResourceType);
            Map<String, RolePolicyResourceRefVO> uuidResourceRefMap = new HashMap<>();
            boolean anyUpdate = false;

            for (List<RolePolicyVO> list : typePoliciesMap.values()) {
                if (list.size() == 1 && !isEmpty(list.get(0).getResourceRefs())) {
                    continue;
                }

                anyUpdate = true;
                uuidResourceRefMap.clear();
                list.stream()
                        .map(RolePolicyVO::getResourceRefs)
                        .filter(Objects::nonNull)
                        .flatMap(Set::stream)
                        .forEach(ref -> uuidResourceRefMap.put(ref.getResourceUuid(), ref));
                if (uuidResourceRefMap.isEmpty()) {
                    list.clear();
                    continue;
                }

                RolePolicyVO basePolicy = list.get(0);
                basePolicy.setResourceRefs(new HashSet<>(uuidResourceRefMap.values()));

                list.clear();
                list.add(basePolicy);
            }

            if (anyUpdate) {
                vos = typePoliciesMap.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
                renovate();
            }
        }
    }
}
