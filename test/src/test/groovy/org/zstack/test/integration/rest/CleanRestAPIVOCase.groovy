package org.zstack.test.integration.rest

import org.zstack.core.rest.RESTApiFacadeImpl
import org.zstack.core.rest.RESTApiGlobalProperty
import org.zstack.header.rest.RestAPIState
import org.zstack.header.rest.RestAPIVO
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.Query
import java.sql.Timestamp
import java.util.concurrent.TimeUnit;

class CleanRestAPIVOCase extends SubCase{
    EnvSpec env
    RESTApiFacadeImpl restApiImpl
    EntityManagerFactory entityManagerFactory
    EntityManager mgr
    EntityTransaction tran

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
        spring {
            include("RESTFacade.xml")
        }
    }

    @Override
    void environment() {
        env = env {
        }
    }

    @Override
    void test() {
        env.create{
            restApiImpl = bean(RESTApiFacadeImpl.class)
            entityManagerFactory = restApiImpl.entityManagerFactory
            mgr = entityManagerFactory.createEntityManager()
            tran = mgr.getTransaction()

            testCleanRestAPIVO()
            testNotCleanRestAPIVO()
            deleteRestAPIVO()
        }
    }

    void testCleanRestAPIVO() {
        for (int i = 0; i < 1200; i++){
            RestAPIVO vo = new RestAPIVO()
            vo.setUuid(String.valueOf(i))
            vo.setApiMessageName("org.zstack.core.rest.DeleteRestAPpiVOMsg")
            vo.setState(RestAPIState.Processing)
            vo.setLastOpDate(new Timestamp(System.currentTimeMillis() - (1000 * 60 * 60 * 24)))
            tran.begin()
            mgr.persist(vo)
            mgr.flush()
            mgr.refresh(vo)
            tran.commit()
        }

        for (int i = 1200; i < 1400; i++){
            RestAPIVO vo = new RestAPIVO()
            vo.setUuid(String.valueOf(i))
            vo.setApiMessageName("org.zstack.core.rest.DeleteRestAPpiVOMsg")
            vo.setState(RestAPIState.Processing)
            vo.setLastOpDate(new Timestamp(System.currentTimeMillis()))
            tran.begin()
            mgr.persist(vo)
            mgr.flush()
            mgr.refresh(vo)
            tran.commit()
        }

        RESTApiGlobalProperty.CLEAN_RESTAPIVO_DELAY = 1
        RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY = 1
        restApiImpl.refreshIntervalClean()

        retryInSecs {
            tran.begin()
            String sql = "select count(*) from RestAPIVO"
            Query query = mgr.createQuery(sql)
            List result = query.resultList
            tran.commit()
            assert result.get(0) == 200
        }
    }

    void testNotCleanRestAPIVO() {
        RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY = -1
        restApiImpl.refreshIntervalClean()
        TimeUnit.SECONDS.sleep(3)
        retryInSecs {
            tran.begin()
            String sql = "select count(*) from RestAPIVO"
            Query query = mgr.createQuery(sql)
            List result = query.resultList
            tran.commit()
            assert result.get(0) == 200
        }
    }

    void deleteRestAPIVO() {
            tran.begin()
            String sql = "delete from RestAPIVO"
            Query query = mgr.createQuery(sql)
            int ret = query.executeUpdate()
            tran.commit()
            assert ret == 200
        mgr.close()
    }
}
