package org.zstack.header.identity.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.header.identity.AccountConstant.POLICY_BASE_PACKAGE;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

public class RolePolicyStatement {
    public RolePolicyEffect effect = RolePolicyEffect.Allow;
    public String actions;
    public List<Resource> resources = new ArrayList<>();

    public static class Resource {
        public String uuid;
        public RolePolicyResourceEffect effect;
        public String resourceType;
        public Resource(String uuid, RolePolicyResourceEffect effect) {
            this.uuid = uuid;
            this.effect = effect;
        }
    }

    public static RolePolicyStatement valueOf(RolePolicyVO policy) {
        String text = toStringStatement(policy);
        return valueOf(text);
    }

    public static RolePolicyStatement valueOf(String policy) {
        String statement = policy.trim();

        RolePolicyStatement value = new RolePolicyStatement();
        if (statement.startsWith("Allow:")) {
            value.effect = RolePolicyEffect.Allow;
            statement = statement.substring("Allow:".length()).trim();
        } else if (statement.startsWith("Exclude:")) {
            value.effect = RolePolicyEffect.Exclude;
            statement = statement.substring("Exclude:".length()).trim();
        }

        final String[] split = statement.split("->");
        if (split.length == 2) {
            value.resources.addAll(parseResource(split[1]));
        } else if (split.length > 2) {
            return null;
        }

        value.actions = parseAction(split[0]);
        return value;
    }

    public static List<RolePolicyStatement> valueOf(Map<String, Object> map) {
        RolePolicyEffect effect;
        List<Resource> resources = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        final Object effectObject = map.get("effect");
        if (effectObject == null || "Allow".equals(effectObject)) {
            effect = RolePolicyEffect.Allow;
        } else if ("Exclude".equals(effectObject)) {
            effect = RolePolicyEffect.Exclude;
        } else {
            return null;
        }

        final Object resourcesObject = map.get("resources");
        if (resourcesObject == null) {
            // do-nothing
        } else if (resourcesObject instanceof String) {
            resources.addAll(parseResource((String) resourcesObject));
        } else if (resourcesObject instanceof List) {
            ((List<?>) resourcesObject).forEach(r -> resources.addAll(parseResource((String) r)));
        } else {
            return null;
        }

        final Object actionsObject = map.get("actions");
        if (actionsObject instanceof String) {
            actions.add(parseAction((String) actionsObject));
        } else if (actionsObject instanceof List) {
            ((List<?>) actionsObject).forEach(r -> actions.add(parseAction(Objects.toString(r))));
        } else {
            return null;
        }

        return transform(actions, action -> {
            RolePolicyStatement statement = new RolePolicyStatement();
            statement.effect = effect;
            statement.actions = action;
            statement.resources = new ArrayList<>(resources);
            return statement;
        });
    }

    public static String toStringStatement(RolePolicyVO vo) {
        if (isEmpty(vo.getResourceRefs())) {
            return toStringStatement(vo.getEffect(), vo.getActions());
        }
        return toStringStatement(vo.getEffect(), vo.getActions(), vo.getResourceRefs());
    }

    public static String toStringStatement(RolePolicyInventory inventory) {
        return toStringStatement(
                RolePolicyEffect.valueOf(inventory.getEffect()), inventory.getActions());
    }

    public static String toStringStatement(RolePolicyEffect effect, String action) {
        String prefix = RolePolicyEffect.Allow.equals(effect) ? "" : effect + ": ";
        return String.format("%s%s", prefix, action);
    }

    public static String toStringStatement(RolePolicyEffect effect, String action, Set<RolePolicyResourceRefVO> resourceRefs) {
        String prefix = RolePolicyEffect.Allow.equals(effect) ? "" : effect + ": ";

        List<RolePolicyResourceRefVO> refs = new ArrayList<>(resourceRefs);
        refs.sort(Comparator.comparing(RolePolicyResourceRefVO::getResourceUuid));

        String suffix = String.join(",", transform(refs,
                ref -> ref.getEffect() == RolePolicyResourceEffect.Allow ?
                        ref.getResourceUuid() :
                        ref.getEffect() + ":" + ref.getResourceUuid()));

        // Exclude: <api> -> <resourceUuid>,<resourceUuid>,<resourceUuid>,Exclude:<resourceUuid>
        return String.format("%s%s -> %s", prefix, action, suffix);
    }

    public List<RolePolicyVO> toVO() {
        if (resources.isEmpty()) {
            RolePolicyVO vo = new RolePolicyVO();
            vo.setActions(actions);
            vo.setEffect(effect);
            return list(vo);
        }

        Map<String, List<Resource>> typeResourcesMap = groupBy(resources, resource -> resource.resourceType);
        List<RolePolicyVO> policies = new ArrayList<>();
        for (Map.Entry<String, List<Resource>> entry : typeResourcesMap.entrySet()) {
            RolePolicyVO vo = new RolePolicyVO();
            vo.setActions(actions);
            vo.setEffect(effect);
            vo.setResourceType(entry.getKey());
            vo.setResourceRefs(new HashSet<>());

            for (Resource resource : entry.getValue()) {
                RolePolicyResourceRefVO ref = new RolePolicyResourceRefVO();
                ref.setResourceUuid(resource.uuid);
                ref.setEffect(resource.effect);
                vo.getResourceRefs().add(ref);
            }

            policies.add(vo);
        }

        return policies;
    }

    private static List<Resource> parseResource(String statement) {
        final String[] split = statement.trim().split(",");
        return Arrays.stream(split)
                .map(text -> new Resource(text.trim(), RolePolicyResourceEffect.Allow))
                .collect(Collectors.toList());
    }

    public static String parseAction(String statement) {
        statement = statement.trim();

        if (statement.startsWith(POLICY_BASE_PACKAGE)) {
            statement = "." + statement.substring(POLICY_BASE_PACKAGE.length());
        }

        return statement;
    }
}
