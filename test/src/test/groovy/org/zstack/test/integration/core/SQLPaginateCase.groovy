package org.zstack.test.integration.core

import org.zstack.core.db.SQL
import org.zstack.core.thread.AsyncThread
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.PaginateCompletion
import org.zstack.header.identity.AccountConstant
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SQLPaginateCase extends SubCase{
    EnvSpec env
    static TIME_OUT = TimeUnit.SECONDS.toMillis(15)

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        spring{
            include("AccountManager.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv {

        }
    }

    @Override
    void test() {
        env.create {
            testPaginateWithCompletion()
            testPaginateSkipIncreaseCompletion()
        }
    }

    void testPaginateSkipIncreaseCompletion() {
        FutureCompletion future = new FutureCompletion(null)

        Map<Integer, List<String>> result = [:]
        def count = 0
        SQL.New("select uuid from AccountVO vo where uuid != :adminUuid", String.class).param("adminUuid", AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .limit(3).skipIncreaseOffset(true).paginate(10, new SQL.Do() {
            @Override
            @AsyncThread
            void accept(List items, PaginateCompletion completion) {
                result.put(count, items)
                count++
                completion.done()
            }
        }, new NoErrorCompletion() {
            @Override
            void done() {
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert result.size() == 4
        assert result.get(0).size() == 3
        assert result.get(1).size() == 3
        assert result.get(2).size() == 3
        assert result.get(3).size() == 3

        assert result.get(0).containsAll(result.get(1))
        assert result.get(1).containsAll(result.get(2))
        assert result.get(2).containsAll(result.get(0))
    }

    void testPaginateWithCompletion() {
        def total = 10
        createAccounts(total)

        FutureCompletion future = new FutureCompletion(null)

        Map<Integer, List<String>> result = [:]
        AtomicInteger count = new AtomicInteger(0)
        SQL.New("select uuid from AccountVO vo where uuid != :adminUuid", String.class).param("adminUuid", AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
                .limit(3).paginate(total, new SQL.Do() {
            @Override
            @AsyncThread
            void accept(List items, PaginateCompletion completion) {
                result.put(count.get(), items)
                count.incrementAndGet()
                completion.done()
            }
        }, new NoErrorCompletion() {
            @Override
            void done() {
                future.success()
            }
        })

        future.await(TIME_OUT)
        assert result.size() == 4
        assert result.get(0).size() == 3
        assert result.get(1).size() == 3
        assert result.get(2).size() == 3
        assert result.get(3).size() == 1

        assert !result.get(0).intersect(result.get(1))
        assert !result.get(1).intersect(result.get(2))
        assert !result.get(2).intersect(result.get(0))
    }



    void createAccounts(int num) {
        for (int i = 0; i < num; i++) {
            createAccount {
                name = "ac" + i
                password = "password"
            }
        }
    }
}
