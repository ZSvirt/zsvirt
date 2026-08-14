package org.zstack.imagereplicator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReplicationGroup {
    /* Assuming we have two replication groups:
     *   rg1 -> [ bs1, bs2 ] : bs1 and bs2 will be replicated with each other
     *   rg2 -> [ bs2, bs3 ] : bs2 and bs3 will be replicated with each other
     *
     * then, we have the in memory representation of 'rg' below:
     *   bs1 -> [ bs2 ]
     *   bs2 -> [ bs1, bs3 ]
     *   bs3 -> [ bs2 ]
     *
     * that is, a directed acyclic graph.
     */

    private Map<String, Set<String>> rg = new ConcurrentHashMap<>();

    private static void addSiblingsToGroup(Set<String> s, Map<String, Set<String>> g) {
        for (String bsUuid : s) {
            Set<String> peers = g.getOrDefault(bsUuid, new HashSet<>());
            peers.addAll(s.stream().filter(u -> !u.equals(bsUuid)).collect(Collectors.toSet()));
            if (!peers.isEmpty()) {
                g.put(bsUuid, peers);
            }
        }
    }

    ReplicationGroup(Collection<ImageReplicationGroupBackupStorageRefVO> refVOS) {
        // rgUuid -> Set<bsUuid>
        Map<String, Set<String>> groups = refVOS.stream()
                .collect(
                        Collectors.groupingBy(ImageReplicationGroupBackupStorageRefVO::getReplicationGroupUuid,
                                Collectors.mapping(ImageReplicationGroupBackupStorageRefVO::getBackupStorageUuid,
                                        Collectors.toSet())
                        ));
        for (Set<String> s : groups.values()) {
            addSiblingsToGroup(s, rg);
        }
    }

    Set<String> getSiblings(String k) {
        return new HashSet<>(rg.getOrDefault(k, new HashSet<>()));
    }

    void addSiblings(Set<String> s) {
        addSiblingsToGroup(s, rg);
    }

    Set<String> getBackupStorageUuids() {
        return rg.keySet();
    }

    /**
     * An image is added to <code>bsUuids</code>, we now compute working sets as a map of:
     *   bsUuid -> replication target BS uuids
     *
     * @param bsUuids To which images has been uploaded.
     * @return working set.
     */
    Map<String, Set<String>> getWorkSet(Set<String> bsUuids) {
        Map<String, Set<String>> ws = new HashMap<>();
        for (String k : bsUuids) {
            Set<String> s = getSiblings(k);
            if (!s.isEmpty()) {
                ws.put(k, s);
            }
        }
        return ws;
    }
}
