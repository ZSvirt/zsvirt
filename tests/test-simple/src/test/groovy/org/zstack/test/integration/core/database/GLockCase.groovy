package org.zstack.test.integration.core.database


import org.zstack.core.db.GLock
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.test.integration.SimpleTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
/**
 * Created by mingjian.deng on 2019/12/28.*/
class GLockCase extends SubCase {
    EnvSpec env
    String lockName = "TestDBLock.lock"
    def lock1
    def lock2

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(SimpleTest.springSpec)
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        env.create {
            prepare()
            testGLockTimeout()
            testGlock()
            testLiteralLockNames()
            testLegacyLockCompatibility()
        }

    }

    void prepare() {
    }

    void testGLockTimeout() {
        // test 3 times
        lock1 = new GLock(lockName, 10)
        lock1.lock()
        sleep 100
        try {
            lock1.unlock()
        } catch (CloudRuntimeException e) {
            assert false
        }

        lock1.lock(1)
        sleep 1500
        // glock has been released by wait_timeout, see ZSTAC-25214
        try {
            lock1.unlock()
            assert false
        } catch (CloudRuntimeException e) {
            logger.info(e.message)
        }

        lock1.lock()
        sleep 1500
        try {
            lock1.unlock()
        } catch (CloudRuntimeException e) {
            assert false
        }
    }

    void testGlock() {
        lock1 = new GLock(lockName, 1)
        lock2 = new GLock(lockName, 1)

        long t1 = 0
        long t2 = 0

        Thread.start {
            lock1.lock()
            sleep 100
            lock1.unlock()
            t1 = System.currentTimeMillis()
        }.join()

        Thread.start {
            lock2.lock()
            t2 = System.currentTimeMillis()
            sleep 100
            lock2.unlock()
        }.join()
        logger.debug("locker.debug: t1=${t1}, t2=${t2}")
        assert t1 <= t2
    }

    void testLiteralLockNames() {
        def dbf = bean(DatabaseFacade.class)
        def names = ["TestDBLock.quote'", 'TestDBLock.back\\slash', "TestDBLock.sql'), 0) --"]
        names.each { name ->
            def glock = new GLock(name, 1)
            glock.lock()
            try {
                def conn = dbf.dataSource.connection
                try {
                    def stmt = conn.prepareStatement('select is_used_lock(?)')
                    try {
                        stmt.setString(1, name)
                        def rs = stmt.executeQuery()
                        try {
                            assert rs.next()
                            assert rs.getObject(1) != null
                        } finally {
                            rs.close()
                        }
                    } finally {
                        stmt.close()
                    }
                } finally {
                    conn.close()
                }
            } finally {
                glock.unlock()
            }

            // A second connection must be able to acquire the exact same name after release.
            def next = new GLock(name, 1)
            next.alsoUseMemoryLock = false
            next.lock()
            next.unlock()
        }
    }

    void testLegacyLockCompatibility() {
        // Use the old SQL on a separate connection to model a node not yet upgraded.
        String name = 'TestDBLock.legacy'
        def conn = bean(DatabaseFacade.class).dataSource.connection
        def glock = new GLock(name, 0)
        glock.alsoUseMemoryLock = false
        def stmt = conn.createStatement()
        try {
            def rs = stmt.executeQuery("select get_lock('${name}', 0)")
            assert rs.next() && rs.getInt(1) == 1
            rs.close()
            try {
                glock.lock()
                assert false: 'new node must contend with a lock held by an old node'
            } catch (CloudRuntimeException expected) {
                assert expected.message.contains('timeout')
            }
            rs = stmt.executeQuery("select release_lock('${name}')")
            assert rs.next() && rs.getInt(1) == 1
            rs.close()

            glock.lock()
            try {
                rs = stmt.executeQuery("select get_lock('${name}', 0)")
                assert rs.next() && rs.getInt(1) == 0
                rs.close()
            } finally {
                glock.unlock()
            }
            rs = stmt.executeQuery("select get_lock('${name}', 0)")
            assert rs.next() && rs.getInt(1) == 1
            rs.close()
        } finally {
            try {
                stmt.executeQuery("select release_lock('${name}')").close()
            } finally {
                stmt.close()
                conn.close()
            }
        }
    }
}
