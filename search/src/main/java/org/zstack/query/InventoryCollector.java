package org.zstack.query;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.zstack.header.description.PackageDescriptionRegistry;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.query.ExpandedQueryStruct;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.metamodel.StaticMetamodel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class InventoryCollector {
    private static final CLogger logger = Utils.getLogger(InventoryCollector.class);

    public Map<Class<?>, EntityInfo> entityInfos = new HashMap<>();
    public Map<Class<?>, Class<?>> inventoryQueryMessageMap = new HashMap<>();
    public Map<Class<?>, List<ExpandedQueryAliasInfo>> aliasInfos = new HashMap<>();
    public Map<Class<?>, List<ExpandedQueryStruct>> expandedQueryStructs = new HashMap<>();
    public Map<Class<?>, Class<?>> metaModelClasses = new HashMap<>();
    final Objenesis objenesis = new ObjenesisStd();

    public void collect() {
        try {
            fillInventoryQueryMessageMap();
            // NOTE: don't change the order
            populatePackageDescriptionExtensions();
            buildExpandedQueryAliasInfo();
            populateEntityInfo();
            completeAliasInfo();
            inheritExpandedQueryAndAliases();
            removeSuppressedExpandedQuery();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void fillInventoryQueryMessageMap() {
        BeanUtils.reflections.getSubTypesOf(APIQueryMessage.class).forEach(msgClass -> {
            AutoQuery at = msgClass.getAnnotation(AutoQuery.class);
            if (at == null) {
                logger.warn(String.format("query message[%s] doesn't have AutoQuery annotation, expanded query alias would not take effect", msgClass.getName()));
                return;
            }

            inventoryQueryMessageMap.put(at.inventoryClass(), msgClass);
        });
    }

    private void populatePackageDescriptionExtensions() {
        for (org.zstack.header.description.query.ExpandedQuery eq : PackageDescriptionRegistry.expandedQueries) {
            addExpandedQueryStruct(toExpandedQueryStruct(eq), "PackageDescriptionRegistry");
        }
        for (org.zstack.header.description.query.ExpandedQueryAlias alias : PackageDescriptionRegistry.expandedQueryAliases) {
            addExpandedQueryAlias(alias.inventoryClass, alias.alias, alias.expandedField,
                    "PackageDescriptionRegistry");
        }
    }

    private static ExpandedQueryStruct toExpandedQueryStruct(org.zstack.header.description.query.ExpandedQuery eq) {
        ExpandedQueryStruct s = new ExpandedQueryStruct();
        s.setInventoryClassToExpand(eq.inventoryClassToExpand);
        s.setExpandedField(eq.expandedField);
        s.setInventoryClass(eq.inventoryClass);
        s.setForeignKey(eq.foreignKey);
        s.setExpandedInventoryKey(eq.expandedInventoryKey);
        s.setHidden(eq.hidden);
        s.setSuppressedInventoryClass(eq.suppressedInventoryClass);
        return s;
    }

    private void addExpandedQueryStruct(ExpandedQueryStruct s, String source) {
        List<ExpandedQueryStruct> exts = expandedQueryStructs.computeIfAbsent(
                s.getInventoryClassToExpand(), k -> new ArrayList<>());
        for (ExpandedQueryStruct existing : exts) {
            if (existing.getExpandedField().equals(s.getExpandedField())) {
                if (existing.getInventoryClass().equals(s.getInventoryClass())
                        && Objects.equals(existing.getForeignKey(), s.getForeignKey())
                        && Objects.equals(existing.getExpandedInventoryKey(), s.getExpandedInventoryKey())
                        && existing.isHidden() == s.isHidden()
                        && Objects.equals(existing.getSuppressedInventoryClass(), s.getSuppressedInventoryClass())) {
                    return;
                }
                throw new CloudRuntimeException(String.format(
                        "duplicate expanded query on %s field[%s] from %s conflicts with an existing declaration",
                        s.getInventoryClassToExpand().getName(), s.getExpandedField(), source));
            }
        }
        exts.add(s);
    }

    private void addExpandedQueryAlias(Class<?> inventoryClass, String alias, String expandField, String source) {
        ExpandedQueryAliasInfo info = new ExpandedQueryAliasInfo();
        info.isFromAnnotation = false;
        info.inventoryClassDefiningThisAlias = inventoryClass;
        info.queryMessageClass = inventoryQueryMessageMap.get(info.inventoryClassDefiningThisAlias);
        DebugUtils.Assert(info.queryMessageClass != null, String.format(
                "%s defines an expanded query alias[%s], but no query message declares inventory class[%s] to which the alias maps",
                source, alias, inventoryClass));
        info.expandField = expandField;
        info.alias = alias;

        List<ExpandedQueryAliasInfo> infos = aliasInfos.computeIfAbsent(info.queryMessageClass, k -> new ArrayList<>());
        for (ExpandedQueryAliasInfo existing : infos) {
            if (existing.alias.equals(alias)) {
                if (existing.expandField.equals(expandField)
                        && existing.inventoryClassDefiningThisAlias.equals(inventoryClass)) {
                    return;
                }
                throw new CloudRuntimeException(String.format(
                        "duplicate expanded query alias[%s] on %s from %s conflicts with an existing declaration",
                        alias, inventoryClass.getName(), source));
            }
        }
        infos.add(info);
    }

    private void buildExpandedQueryAliasInfo() {
        Set<Class<?>> invClasses = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);

        for (Class<?> invClass : invClasses) {
            ExpandedQueryAliases aliases = invClass.getAnnotation(ExpandedQueryAliases.class);
            if (aliases == null) {
                continue;
            }

            for (ExpandedQueryAlias alias : aliases.value()) {
                Class<?> aliasInventoryClass = alias.target() == Object.class ? invClass : alias.target();

                ExpandedQueryAliasInfo info = new ExpandedQueryAliasInfo();
                info.alias = alias.alias();
                info.expandField = alias.expandedField();
                info.queryMessageClass = inventoryQueryMessageMap.get(aliasInventoryClass);
                info.inventoryClassDefiningThisAlias = aliasInventoryClass;
                info.isFromAnnotation = true;
                if (info.queryMessageClass == null) {
                    throw new CloudRuntimeException(String.format("inventory[%s] declares expanded query alias, but not query message declare this inventory class in AutoQuery annotation",
                            aliasInventoryClass.getName()));
                }

                List<ExpandedQueryAliasInfo> lst = aliasInfos.computeIfAbsent(info.queryMessageClass, k -> new ArrayList<>());
                lst.add(info);
            }
        }
    }

    private EntityInfo buildEntityInfo(Class<?> invClass) throws NoSuchMethodException {
        EntityInfo info = entityInfos.get(invClass);
        if (info != null) {
            return info;
        }

        info = new EntityInfo(this, invClass);
        entityInfos.put(invClass, info);

        return info;
    }

    private void populateEntityInfo() throws NoSuchMethodException {
        Set<Class<?>> metaClasses = BeanUtils.reflections.getTypesAnnotatedWith(StaticMetamodel.class);
        for (Class<?> it : metaClasses) {
            StaticMetamodel at = it.getAnnotation(StaticMetamodel.class);
            metaModelClasses.put(at.value(), it);
        }

        Set<Class<?>> invClasses = BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class);

        for (Class<?> invClass : invClasses) {
            EntityInfo info = buildEntityInfo(invClass);

            if (info.inventoryAnnotation.parent().length > 0) {
                Parent pat = info.inventoryAnnotation.parent()[0];
                Class<?> pinvClass = pat.inventoryClass();
                DebugUtils.Assert(pinvClass.isAnnotationPresent(Inventory.class), String.format("inventory[%s]'s parent inventory class[%s] is not annotated by @Inventory", info.inventoryClass.getName(), pinvClass.getName()));
                EntityInfo pinfo = buildEntityInfo(pinvClass);
                info.parent = pinfo;
                pinfo.children.add(info);
            }

        }

        for (EntityInfo e : entityInfos.values()) {
            e.buildFlatTypeEntityMap();
        }
    }

    private void completeAliasInfo() {
        class ExpandedQueryAliasInfoCompletion {
            ExpandedQueryAliasInfo alias;

            ExpandedQueryAliasInfoCompletion(ExpandedQueryAliasInfo alias) {
                this.alias = alias;
            }

            private ExpandedQueryStruct findTargetExpandedQueryStruct(String[] expandedFields, EntityInfo entityInfo) {
                ExpandedQueryStruct struct = null;
                for (String exf : expandedFields) {
                    struct = entityInfo.expandedQueries.get(exf);
                    if (struct == null) {
                        ExpandedQueryAliasInfo exalias = entityInfo.aliases.get(exf);
                        DebugUtils.Assert(exalias != null, String.format("cannot find expanded query or alias[%s] on %s", exf, entityInfo.inventoryClass));
                        struct = findTargetExpandedQueryStruct(exalias.expandField.split("\\."), entityInfo);
                    }
                    entityInfo = entityInfos.get(struct.getInventoryClass());
                }
                return struct;
            }

            void complete() {
                EntityInfo entityInfo = entityInfos.get(alias.inventoryClassDefiningThisAlias);
                String[] expandedFields = alias.expandField.split("\\.");
                DebugUtils.Assert(expandedFields.length != 0, String.format("alias[%s] defined in %s is invalid", alias.expandField, alias.inventoryClassDefiningThisAlias));
                ExpandedQueryStruct struct = findTargetExpandedQueryStruct(expandedFields, entityInfo);
                alias.inventoryClass = struct.getInventoryClass();
                alias.check(expandedQueryStructs);
            }
        }

        for (List<ExpandedQueryAliasInfo> infos : aliasInfos.values()) {
            for (ExpandedQueryAliasInfo alias : infos) {
                new ExpandedQueryAliasInfoCompletion(alias).complete();
            }
        }
    }

    private void inheritExpandedQueryAndAliases(EntityInfo current, EntityInfo ancestor) {
        if (ancestor == null) {
            return;
        }

        if (!ancestor.aliases.isEmpty()) {
            Class<?> msgClz = inventoryQueryMessageMap.get(current.inventoryClass);
            List<ExpandedQueryAliasInfo> aliases = aliasInfos.get(msgClz);
            if (aliases == null) {
                aliases = new ArrayList<>();
            }
            aliases.addAll(ancestor.aliases.values());

            current.addQueryAliases(aliases);
        }

        if (!ancestor.expandedQueries.isEmpty()) {
            current.expandedQueries.putAll(ancestor.expandedQueries);
        }

        inheritExpandedQueryAndAliases(current, ancestor.parent);
    }

    private void inheritExpandedQueryAndAliases() {
        for (EntityInfo info : entityInfos.values()) {
            inheritExpandedQueryAndAliases(info, info.parent);
        }
    }

    private void removeSuppressedExpandedQuery() {
        for (EntityInfo info : entityInfos.values()) {
            Map<String, ExpandedQueryStruct> ess = new HashMap<>(info.expandedQueries);
            for (Entry<String, ExpandedQueryStruct> e : ess.entrySet()) {
                if (e.getValue().getSuppressedInventoryClass() != null) {
                    ExpandedQueryStruct toSuppressed = null;
                    for (ExpandedQueryStruct s : info.expandedQueries.values()) {
                        if (s.getInventoryClass() == e.getValue().getSuppressedInventoryClass()) {
                            toSuppressed = s;
                            break;
                        }
                    }

                    DebugUtils.Assert(toSuppressed != null, String.format("ExpandedQuery[%s] of %s is going to suppress a undefined ExpandedQuery that has inventory class[%s]",
                            e.getValue().getExpandedField(), info.inventoryClass, e.getValue().getSuppressedInventoryClass()));

                    info.expandedQueries.remove(toSuppressed.getExpandedField());
                    logger.debug(String.format("ExpandedQuery[%s] of %s suppresses ExpandedQuery[%s]",
                            e.getValue().getExpandedField(), info.inventoryClass, toSuppressed.getExpandedField()));
                }
            }
        }
    }
}
