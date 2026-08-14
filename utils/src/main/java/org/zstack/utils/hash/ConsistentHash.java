package org.zstack.utils.hash;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class ConsistentHash<T> {
    private final CLogger logger = Utils.getLogger(ConsistentHash.class);
    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = Collections.synchronizedSortedMap(new TreeMap<Integer, T>());
    private final Set<T> nodes = Collections.synchronizedSet(new HashSet<T>());

    public ConsistentHash(HashFunction hashFunction, int numberOfReplicas,
                          Collection<T> nodes) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;

        for (T node : nodes) {
            add(node);
        }
    }

    public Set<T> getNodes() {
        return nodes;
    }

    public void add(T node) {
        nodes.add(node);

        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeName = node.toString() + i;
            circle.put(hashFunction.hash(nodeName), node);
        }
        logger.debug(String.format("after adding, consistent hash circle has management nodes%s, %s virtual nodes now",
                nodes, circle.size()));
    }

    public void remove(T node) {
        nodes.remove(node);
        logger.debug(String.format("the consistent hash ring currently has nodes%s", nodes));

        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeName = node.toString() + i;
            circle.remove(hashFunction.hash(nodeName));
        }
        logger.debug(String.format("after removing, consistent hash circle has management nodes%s, %s virtual nodes now",
                nodes, circle.size()));
    }

    public boolean hasNode(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            String nodeName = node.toString() + i;
            if (circle.containsKey(hashFunction.hash(nodeName))) {
                return true;
            }
        }

        return false;
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hashFunction.hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }
}
