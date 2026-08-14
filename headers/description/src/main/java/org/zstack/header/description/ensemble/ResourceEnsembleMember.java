package org.zstack.header.description.ensemble;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ResourceEnsembleMember {
    private Class<?> clazz;
    private Consumer<Map<String, List<String>>> findChildrenByParentUuid;
    private Consumer<Map<String, String>> findParentByChildUuid;
    private ResourceEnsembleMember parent;
    private final List<ResourceEnsembleMember> children = new ArrayList<>();

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Consumer<Map<String, List<String>>> getFindChildrenByParentUuid() {
        return findChildrenByParentUuid;
    }

    public void setFindChildrenByParentUuid(Consumer<Map<String, List<String>>> findChildrenByParentUuid) {
        this.findChildrenByParentUuid = findChildrenByParentUuid;
    }

    public Consumer<Map<String, String>> getFindParentByChildUuid() {
        return findParentByChildUuid;
    }

    public void setFindParentByChildUuid(Consumer<Map<String, String>> findParentByChildUuid) {
        this.findParentByChildUuid = findParentByChildUuid;
    }

    public ResourceEnsembleMember getParent() {
        return parent;
    }

    public void setParent(ResourceEnsembleMember parent) {
        this.parent = parent;
    }

    public List<ResourceEnsembleMember> getChildren() {
        return children;
    }
}
