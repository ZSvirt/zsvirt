package org.zstack.test.integration.core.database

import org.hibernate.exception.ConstraintViolationException
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SQLBatch
import org.zstack.core.db.DBSourceUtils
import org.zstack.header.vo.ResourceVO
import org.zstack.header.vo.ResourceVO_
import org.zstack.header.zone.ZoneVO
import org.zstack.header.zone.ZoneVO_
import org.zstack.testlib.SubCase

import java.sql.SQLException
import java.sql.Timestamp

/**
 * Created by david on 7/13/17.
 */
class DatabaseWrapperCase extends SubCase {
    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        assert DBSourceUtils.isDBConnected()
        assert DBSourceUtils.waitDBConnected(5, 5)
        testQ()
        testSQLBatch()
    }

    void testQ() {
        def zoneList1 = Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, "1234")
                .list()

        assert zoneList1 != null : "list should always return non-null"
        assert zoneList1.isEmpty() : "expect no zone found"

        def zoneList2 = Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, "1234")
                .select(ZoneVO_.@name)
                .listValues()

        assert zoneList2 != null : "listValues should always return non-null"
        assert zoneList2.isEmpty() : "expect no values found"

        def zoneList3 = Q.New(ZoneVO.class)
                .eq(ZoneVO_.uuid, "1234")
                .select(ZoneVO_.@name)
                .select(ZoneVO_.description)
                .listTuple()
        assert zoneList3 != null : "listTuples should always return non-null"
        assert zoneList3.isEmpty() : "expect no tuples found"

        try {
            Q.New(ZoneVO.class)
                    .in(ZoneVO_.uuid, ["1234"])
                    .list()
        } catch (RuntimeException e) {
            assert e.getMessage() == "Op.IN value cannot be null or empty"
        }
    }

    void testSQLBatch() {
        def zoneUuid = Platform.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        ZoneVO v2 = new ZoneVO()
        def zoneUuid2 = Platform.uuid
        v2.name = "22222"
        v2.type = "zstack"
        v2.uuid = zoneUuid2
        v2.description = "Permanent"
        v2.setDefault(true)
        v2.createDate = new Timestamp(System.currentTimeMillis())
        dbf.persist(v2)

        try {
            new SQLBatch() {

                @Override
                protected void scripts() {
                    ZoneVO v = new ZoneVO()
                    v2.name = "33333"
                    v2.type = "zstack"
                    v2.uuid = zoneUuid

                    sql(ZoneVO.class)
                            .eq(ZoneVO_.uuid, zoneUuid)
                            .set(ZoneVO_.description, "wrong desc")
                            .update()

                    dbf.getEntityManager().persist(v)
                    throw new ConstraintViolationException("on purpose", new SQLException(), "constraint")
                }

            }.execute()
        } catch (Throwable ignored) {
        }

        assert !dbf.isExist(zoneUuid, ZoneVO.class)
        def v3 = dbf.findByUuid(zoneUuid2, ZoneVO.class)
        assert v3 != null
        assert v3.description == v2.description

        SQL.New(ZoneVO)
                .eq(ZoneVO_.uuid, zoneUuid2)
                .delete()

        String uuid = Platform.getUuid()
        try {
            new SQLBatch() {
                @Override
                protected void scripts() {
                    for (int i = 0; i < 10; i++) {
                        ResourceVO vo = new ResourceVO()
                        vo.setUuid(uuid)
                        vo.setResourceName("test")
                        vo.setResourceType("test");
                        vo.setConcreteResourceType("test")
                        persist(vo)
                    }
                }
            }.execute();
        }catch (Throwable throwable) {
        }
        long count = Q.New(ResourceVO.class).eq(ResourceVO_.uuid, uuid).count()
        assert count == 0
    }

    @Override
    void clean() {
    }
}
