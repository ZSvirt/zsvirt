package org.zstack.header.description.ensemble;

import org.zstack.header.description.PackageDescriptionRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ResourceEnsembleContributorBuilder {
    private final List<ResourceEnsembleMember> members = new ArrayList<>();
    private Class<?> master;

    public ResourceEnsembleContributorBuilder resource(Class<?> c) {
        ResourceEnsembleMember member = new ResourceEnsembleMember();
        member.setClazz(c);
        members.add(member);
        return this;
    }

    public ResourceEnsembleContributorBuilder resourceWithCustomizeFindingMethods(
            Class<?> c,
            Consumer<Map<String, List<String>>> findChildrenByParentUuid,
            Consumer<Map<String, String>> findParentByChildUuid) {
        ResourceEnsembleMember member = new ResourceEnsembleMember();
        member.setClazz(c);
        member.setFindChildrenByParentUuid(findChildrenByParentUuid);
        member.setFindParentByChildUuid(findParentByChildUuid);
        members.add(member);
        return this;
    }

    public ResourceEnsembleContributorBuilder contributeTo(Class<?> c) {
        master = c;
        return this;
    }

    public void build() {
        Objects.requireNonNull(master);

        ResourceEnsembleMember masterMember = findMemberFromGlobal(master);
        if (masterMember == null) {
            masterMember = new ResourceEnsembleMember();
            masterMember.setClazz(master);
            PackageDescriptionRegistry.ensembleMembers.add(masterMember);
        }

        for (ResourceEnsembleMember member : members) {
            ResourceEnsembleMember existsMember = findMemberFromGlobal(member.getClazz());
            if (existsMember == null) {
                member.setParent(masterMember);
                masterMember.getChildren().add(member);
                PackageDescriptionRegistry.ensembleMembers.add(member);
                continue;
            }

            existsMember.setParent(masterMember);
            masterMember.getChildren().add(existsMember);
            if (existsMember.getFindChildrenByParentUuid() == null && member.getFindChildrenByParentUuid() != null) {
                existsMember.setFindChildrenByParentUuid(member.getFindChildrenByParentUuid());
            }
            if (existsMember.getFindParentByChildUuid() == null && member.getFindParentByChildUuid() != null) {
                existsMember.setFindParentByChildUuid(member.getFindParentByChildUuid());
            }
        }
    }

    private @Nullable ResourceEnsembleMember findMemberFromGlobal(Class<?> clazz) {
        return PackageDescriptionRegistry.ensembleMembers.stream()
                .filter(c -> Objects.equals(clazz, c.getClazz()))
                .findFirst()
                .orElse(null);
    }
}
