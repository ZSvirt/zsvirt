package org.zstack.header.description.query;

import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for inventory expanded-query declarations (configuration only).
 * Prefer multi-line chains, e.g.
 * <pre>
 * expandedQuery()
 *     .on(VmNicInventory.class)
 *     .expose("loadBalancerListener")
 *     .then(LoadBalancerListenerVmNicRefInventory.class)
 *         .named("loadBalancerListenerRef")
 *         .where(VmNicInventory.class, "uuid")
 *         .equals(LoadBalancerListenerVmNicRefInventory.class, "vmNicUuid")
 *         .end()
 *     .then(LoadBalancerListenerInventory.class)
 *         .named("listener")
 *         .end()
 *     .build();
 *
 * expandedQuery()
 *     .on(VipInventory.class)
 *     .expose("loadBalancer")
 *     .then(LoadBalancerInventory.class)
 *         .where(VipInventory.class, "uuid")
 *         .equals(LoadBalancerInventory.class, "vipUuid")
 *         .end()
 *     .build();
 * </pre>
 */
public class ExpandedQueryBuilder {
    private Class<?> onInventory;
    private String expose;
    private final List<ExpandedQueryStepBuilder> steps = new ArrayList<>();

    public ExpandedQueryBuilder on(Class<?> inventoryClass) {
        DebugUtils.Assert(inventoryClass != null, "on inventory class cannot be null");
        this.onInventory = inventoryClass;
        return this;
    }

    /**
     * Public condition prefix callers write (e.g. {@code loadBalancerListener} in
     * {@code loadBalancerListener.uuid=...}). When it equals the sole hop field name, no alias is registered.
     */
    public ExpandedQueryBuilder expose(String publicName) {
        DebugUtils.Assert(publicName != null && !publicName.isEmpty(), "expose name cannot be empty");
        this.expose = publicName;
        return this;
    }

    public ExpandedQueryStepBuilder then(Class<?> hopInventoryClass) {
        DebugUtils.Assert(onInventory != null, "call on(Class) before then()");
        DebugUtils.Assert(hopInventoryClass != null, "then inventory class cannot be null");
        ExpandedQueryStepBuilder step = new ExpandedQueryStepBuilder(this, hopInventoryClass);
        steps.add(step);
        return step;
    }

    void ensureStepRegistered(ExpandedQueryStepBuilder step) {
        if (!steps.contains(step)) {
            steps.add(step);
        }
    }

    public void build() {
        DebugUtils.Assert(onInventory != null, "call on(Class) before build()");
        DebugUtils.Assert(!steps.isEmpty(), "call then(Class) at least once before build()");
        DebugUtils.Assert(expose != null && !expose.isEmpty(), "call expose(String) before build()");

        Class<?> expandOnto = onInventory;
        List<String> pathParts = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            ExpandedQueryStepBuilder step = steps.get(i);
            boolean last = i == steps.size() - 1;
            boolean hasJoin = step.foreignKey != null && step.expandedInventoryKey != null;

            String fieldName = step.named;
            if (fieldName == null && steps.size() == 1) {
                fieldName = expose;
            }
            DebugUtils.Assert(fieldName != null && !fieldName.isEmpty(),
                    "call named(String) on hop, or omit named for a single hop that uses expose()");

            if (hasJoin) {
                DebugUtils.Assert(step.whereSourceClass != null, "call where(Class, key) before equals()");
                DebugUtils.Assert(step.whereSourceClass.equals(expandOnto),
                        String.format("where source class[%s] must be the inventory being expanded[%s]",
                                step.whereSourceClass.getName(), expandOnto.getName()));
                // Intermediate hops are hidden; the last hop with a join is public.
                boolean hidden = !last;
                PackageDescriptionRegistry.addExpandedQuery(new ExpandedQuery(
                        expandOnto,
                        fieldName,
                        step.hopInventoryClass,
                        step.foreignKey,
                        step.expandedInventoryKey,
                        hidden,
                        step.suppressedInventoryClass));
            } else {
                DebugUtils.Assert(step.named != null,
                        "hop without where/equals must still call named(String) for path derivation");
            }

            pathParts.add(fieldName);
            expandOnto = step.hopInventoryClass;
        }

        String path = String.join(".", pathParts);
        if (!expose.equals(path)) {
            PackageDescriptionRegistry.addExpandedQueryAlias(new ExpandedQueryAlias(onInventory, expose, path));
        }
    }
}
