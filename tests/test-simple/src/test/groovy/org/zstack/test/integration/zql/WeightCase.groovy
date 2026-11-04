package org.zstack.test.integration.zql

import org.zstack.core.db.DBGraph
import org.zstack.header.cluster.ClusterVO
import org.zstack.header.vm.VmInstanceVO
import org.zstack.testlib.SubCase

class WeightCase extends SubCase {
    @Override
    void setup() {
        useSpring(ZqlTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testDBGraphVerticesFinding()
    }

    static void testDBGraphVerticesFinding() {
        def vertex = DBGraph.findVerticesWithSmallestWeight(VmInstanceVO.class, ClusterVO.class)
        assert vertex.entityClass == VmInstanceVO.class
        assert vertex.next != null
        assert vertex.srcKey == "clusterUuid"
        assert vertex.dstKey == "uuid"

        assert vertex.next.entityClass == ClusterVO.class
        assert vertex.next.next == null
        assert vertex.next.previous == vertex
        assert vertex.next.srcKey == null
        assert vertex.next.dstKey == null
    }

    @Override
    void clean() {
    }
}
