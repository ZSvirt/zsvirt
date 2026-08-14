package org.zstack.test.integration.zql

import org.zstack.zql.ZQLQueryReturn
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.zql.ZQL

class CountGroupByCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 1
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCountGroupBy()
            testQuery()
            testGroupLimit()
            testEmpty()
        }
    }

    void testCountGroupBy() {
        1.upto(50) {
            createInstanceOffering {
                name = "50"
                cpuNum = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
            }
        }

        1.upto(51) {
            createInstanceOffering {
                name = "51"
                cpuNum = 1
                memorySize = SizeUnit.GIGABYTE.toByte(1)
            }
        }

        def ret = ZQL.fromString("count instanceoffering group by name,cpuNum order by groupCount asc limit 100").getSingleResult() as ZQLQueryReturn
        assert ret.inventoryCounts.size() == 3
        assert ret.inventoryCounts.findAll {it -> it.key.cpuNum == 1}.size() == 3
        assert ret.inventoryCounts.find {it -> it.value == 50L}.key.name == "50"
        assert ret.inventoryCounts.find {it -> it.value == 51L}.key.name == "51"
        assert ret.inventoryCounts.find {it -> it.value == 1L}.key.name == "instanceOffering"
        ret.inventoryCounts.values()[0] == 1
        ret.inventoryCounts.values()[1] == 50
        ret.inventoryCounts.values()[2] == 51
        assert ret.total == 102
    }

    void testQuery() {
        def ret = ZQL.fromString("query instanceoffering return with (total) group by name,cpuNum limit 100").getSingleResult() as ZQLQueryReturn
        assert ret.inventories.size() == 3
        assert ret.total == 102

        def ret2 = ZQL.fromString("query instanceoffering return with (total) limit 100").getSingleResult() as ZQLQueryReturn
        assert ret2.inventories.size() == 100
        assert ret2.total == 102
    }

    void testGroupLimit() {
        def ret = ZQL.fromString("count instanceoffering group by name,cpuNum limit 2").getSingleResult() as ZQLQueryReturn
        assert ret.inventoryCounts.size() == 2
        assert ret.total == 102
    }

    void testEmpty() {
        def ret = ZQL.fromString("count zone group by name").getSingleResult() as ZQLQueryReturn
        assert ret.inventoryCounts == null
        assert ret.total == 0
    }
}
