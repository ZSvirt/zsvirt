package org.zstack.test.integration.compute

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.zone.ZoneVO
import org.zstack.header.zone.ZoneVO_
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

/**
 * 1. test create zone as default zone
 * 2. test batch create zone as default zone
 * 3. test update zone to default zone
 * 4. test batch update zone to default zone
 * 5. test query default zone
 * 6. test delete default zone
 */
class ZoneCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    @Override
    void test() {
        env.create {
            testCreateZoneAsDefaultZone()
            testBatchCreateZoneAsDefaultZone()
            testUpdateZoneToDefaultZone()
            testBatchUpdateZoneToDefaultZone()
            testQueryDefaultZone()
            testDeleteDefaultZone()
            testBatchUnsetDefaultZone()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testCreateZoneAsDefaultZone() {
        ZoneInventory zone = createZone {
            name = "zone1"
            description = "zone1"
            isDefault = true
        } as ZoneInventory

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, zone.uuid)
                .eq(ZoneVO_.isDefault, true)
                .isExists()
        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, true)
                .count() == 1
    }

    void testBatchCreateZoneAsDefaultZone() {
        def list = []

        for (int i = 0; i < 10; i++) {
            int zoneSuffix = i

            def thread = Thread.start {
                ZoneInventory zone = createZone {
                    name = "zone$zoneSuffix"
                    description = "zone$zoneSuffix"
                    isDefault = true
                } as ZoneInventory

                assert zone.isDefault
            }

            list.add(thread)
        }

        list.each {
            it.join()
        }

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, true)
                .count() == 1
    }

    void testUpdateZoneToDefaultZone() {
        ZoneInventory zone = createZone {
            name = "zone1"
            description = "zone1"
            isDefault = false
        } as ZoneInventory
        assert !zone.isDefault

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, zone.uuid)
                .eq(ZoneVO_.isDefault, false)
                .isExists()

        zone.isDefault = true
        updateZone {
            uuid = zone.uuid
            isDefault = zone.isDefault
        }

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, zone.uuid)
                .eq(ZoneVO_.isDefault, true)
                .isExists()
        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, true)
                .count() == 1
    }

    void testBatchUpdateZoneToDefaultZone() {
        def list = []

        for (int i = 0; i < 10; i++) {
            int zoneSuffix = i

            def thread = Thread.start {
                ZoneInventory zone = createZone {
                    name = "zone$zoneSuffix"
                    description = "zone$zoneSuffix"
                    isDefault = false
                } as ZoneInventory

                zone = updateZone {
                    uuid = zone.uuid
                    isDefault = true
                }
                assert zone.isDefault
            }

            list.add(thread)
        }

        list.each {
            it.join()
        }

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, true)
                .count() == 1
    }

    void testQueryDefaultZone() {
        List<ZoneInventory> zones = queryZone {
            conditions = ["isDefault=true"]
        }

        assert zones.size() == 1

        zones = queryZone {
            conditions = ["isDefault=false"]
        }

        assert (long) zones.size() == Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, false)
                .count()

        zones = queryZone {
            sortBy = "isDefault"
            sortDirection = "desc"
        }

        assert zones.get(0).isDefault
    }

    void testDeleteDefaultZone() {
        ZoneInventory zone = createZone {
            name = "zone1"
            description = "zone1"
            isDefault = true
        } as ZoneInventory

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, zone.uuid)
                .eq(ZoneVO_.isDefault, true)
                .isExists()
        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, true)
                .count() == 1

        deleteZone {
            uuid = zone.uuid
        }

        assert !Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, zone.uuid)
                .eq(ZoneVO_.isDefault, true)
                .isExists()
    }

    void testBatchUnsetDefaultZone () {
        def list = []

        for (int i = 0; i < 10; i++) {
            int zoneSuffix = i

            def thread = Thread.start {
                ZoneInventory zone = createZone {
                    name = "zone$zoneSuffix"
                    description = "zone$zoneSuffix"
                    isDefault = true
                } as ZoneInventory

                zone = updateZone {
                    uuid = zone.uuid
                    isDefault = false
                }

                assert !zone.isDefault
            }

            list.add(thread)
        }

        list.each {
            it.join()
        }

        assert Q.New(ZoneVO.class)
                .eq(ZoneVO_.isDefault, true)
                .count() == 0
    }
}
