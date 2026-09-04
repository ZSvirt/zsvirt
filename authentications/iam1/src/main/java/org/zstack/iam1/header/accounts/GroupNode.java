package org.zstack.iam1.header.accounts;

import org.zstack.core.db.Q;
import org.zstack.iam1.entity.accounts.AccountGroupVO;
import org.zstack.iam1.entity.accounts.AccountGroupVO_;
import org.zstack.utils.CollectionUtils;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.toMap;

/**
 * Created by Wenhao.Zhang on 2024/09/02
 */
public class GroupNode {
    public String uuid;
    public String parentUuid;
    public String rootGroupUuid;
    public List<GroupNode> children;
    public GroupNode parent;
    public GroupNode root;

    public void child(GroupNode node) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(node);
        node.parent = this;
    }

    public GroupNode find(String uuid) {
        if (Objects.equals(this.uuid, uuid)) {
            return this;
        }
        if (CollectionUtils.isEmpty(children)) {
            return null;
        }
        for (GroupNode child : children) {
            final GroupNode node = child.find(uuid);
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    public boolean hasChild(String uuid) {
        return find(uuid) != null;
    }

    public Set<String> selfAndAllChildrenUuidSet() {
        Set<String> uuidSet = new HashSet<>();
        fillSelfAndChildrenUuid(uuidSet);
        return uuidSet;
    }

    private void fillSelfAndChildrenUuid(Set<String> uuidSet) {
        uuidSet.add(uuid);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (GroupNode child : children) {
            child.fillSelfAndChildrenUuid(uuidSet);
        }
    }

    /**
     * all parent UUID set without self
     */
    public Set<String> ancestorUuidSet() {
        Set<String> uuidSet = new HashSet<>();
        fillParentUuid(uuidSet);
        return uuidSet;
    }

    private void fillParentUuid(Set<String> uuidSet) {
        if (parent == null) {
            return;
        }
        uuidSet.add(parent.uuid);
        parent.fillParentUuid(uuidSet);
    }

    public Set<String> selfAndAncestorUuidSet() {
        Set<String> uuidSet = ancestorUuidSet();
        uuidSet.add(uuid);
        return uuidSet;
    }

    public void retainWithLevel(int level) {
        if (level <= 0) {
            this.children = null;
            return;
        }
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (GroupNode child : children) {
            child.retainWithLevel(level - 1);
        }
    }

    public static GroupNode findSingleGroupNodeWithoutChildren(String uuid) {
        final List<Tuple> tuples = Q.New(AccountGroupVO.class)
                .eq(AccountGroupVO_.uuid, uuid)
                .select(AccountGroupVO_.parentUuid, AccountGroupVO_.rootGroupUuid)
                .listTuple();
        if (tuples.isEmpty()) {
            return null;
        }

        GroupNode node = new GroupNode();
        node.uuid = uuid;
        node.parentUuid = tuples.get(0).get(0, String.class);
        node.rootGroupUuid = tuples.get(0).get(1, String.class);
        return node;
    }

    public static GroupNode findGroupNodeAndChildren(String rootGroupUuid) {
        final List<Tuple> tuples = Q.New(AccountGroupVO.class)
                .eq(AccountGroupVO_.rootGroupUuid, rootGroupUuid)
                .select(AccountGroupVO_.uuid, AccountGroupVO_.parentUuid)
                .listTuple();
        final Map<String, GroupNode> nodeMap = toMap(tuples, tuple -> tuple.get(0, String.class), tuple -> {
            GroupNode node = new GroupNode();
            node.uuid = tuple.get(0, String.class);
            node.parentUuid = tuple.get(1, String.class);
            node.rootGroupUuid = rootGroupUuid;
            return node;
        });

        GroupNode root = nodeMap.get(rootGroupUuid);
        for (GroupNode node : nodeMap.values()) {
            if (node.parentUuid != null) {
                GroupNode parent = nodeMap.get(node.parentUuid);
                parent.child(node);
            }
            node.root = root;
        }
        return root;
    }
}
