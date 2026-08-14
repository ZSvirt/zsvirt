package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.hash.ApacheHash;
import org.zstack.utils.hash.ConsistentHash;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestConsistentHash {

    private String uuid() {
        return UUID.randomUUID().toString();
    }

    @Test
    public void test() {
        testNodeHash();
        testNodeAdd();
    }

    private void testNodeHash() {
        List<String> nodes = new ArrayList<String>();
        Map<String, Integer> m = new HashMap();
        for (int i=0; i<10; i++) {
            String guid = uuid();
            nodes.add(guid);
            m.put(guid, new Integer(0));
        }

        ConsistentHash<String> chash = new ConsistentHash(new ApacheHash(), 1000, nodes);

        for (int i=0; i<100000; i++) {
            String msg = uuid();
            String node = chash.get(msg);
            Integer count = (Integer) m.get(node);
            int c = count + 1;
            m.put(node, c);
        }

        for (Map.Entry<String, Integer> e : m.entrySet()) {
            System.out.println(String.format("node[%s]: %s", e.getKey(), e.getValue()));
        }
    }

    private void testNodeAdd() {
        List<String> nodes = new ArrayList<String>();
        for (int i=0; i<5; i++) {
            String guid = uuid();
            nodes.add(guid);
        }

        ConsistentHash<String> chash = new ConsistentHash(new ApacheHash(), 1000, nodes);
        List<String> uuids = new ArrayList<>();
        for (int i=0; i<100; i++) {
            uuids.add(uuid());
        }

        HashMap<String, List<String>> managedResourcesBefore = new HashMap<>();
        for (String uuid : uuids) {
            String node = chash.get(uuid);
            managedResourcesBefore.computeIfAbsent(node, k -> new ArrayList<>()).add(uuid);
        }
        managedResourcesBefore.values().forEach(Collections::sort);

        String guid = uuid();
        chash.add(guid);

        HashMap<String, List<String>> managedResourcesAfter = new HashMap<>();
        for (String uuid : uuids) {
            String node = chash.get(uuid);
            managedResourcesAfter.computeIfAbsent(node, k -> new ArrayList<>()).add(uuid);
        }

        managedResourcesAfter.values().forEach(Collections::sort);
        managedResourcesBefore.keySet().forEach(it -> {
            System.out.println(it);
            System.out.println(managedResourcesBefore.get(it));
            System.out.println(managedResourcesAfter.get(it));
            assert managedResourcesBefore.get(it).containsAll(managedResourcesAfter.get(it));
        });
    }
}
