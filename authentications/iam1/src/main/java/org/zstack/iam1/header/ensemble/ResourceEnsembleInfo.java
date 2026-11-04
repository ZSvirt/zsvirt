package org.zstack.iam1.header.ensemble;

import org.zstack.header.description.ensemble.ResourceEnsembleMember;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/08/07
 */
public class ResourceEnsembleInfo {
    public String uuid;
    public ResourceEnsembleMember metadata;
    public ResourceEnsembleInfo parent;
    public final List<ResourceEnsembleInfo> children = new ArrayList<>();

    public Class<?> resourceType() {
        return metadata.getClazz();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        toString(builder, 0);
        return builder.toString();
    }

    private void toString(StringBuilder builder, int level) {
        for (int i = 0; i < level; i++) {
            builder.append("    ");
        }
        builder.append("[").append("uuid=").append(uuid).append(", ").append("type=");
        if (metadata == null) {
            builder.append("???");
        } else {
            builder.append(metadata.getClazz().getSimpleName());
        }
        builder.append("]\n");

        for (ResourceEnsembleInfo child : children) {
            child.toString(builder, level + 1);
        }
    }

    public List<ResourceEnsembleInfo> flatten() {
        final List<ResourceEnsembleInfo> results = new ArrayList<>();
        results.add(this);
        children.forEach(child -> child.flatten(results));
        return results;
    }

    private void flatten(List<ResourceEnsembleInfo> list) {
        list.add(this);
        children.forEach(child -> child.flatten(list));
    }
}
