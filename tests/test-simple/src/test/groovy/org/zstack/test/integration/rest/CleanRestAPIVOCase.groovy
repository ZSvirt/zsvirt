package org.zstack.test.integration.rest

import org.zstack.core.CoreGlobalProperty
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.rest.RESTApiDecoder
import org.zstack.core.rest.RESTApiFacadeImpl
import org.zstack.core.rest.RESTApiGlobalProperty
import org.zstack.header.rest.RestAPIState
import org.zstack.header.rest.RestAPIVO
import org.zstack.header.zone.APICreateZoneEvent
import org.zstack.header.zone.APICreateZoneMsg
import org.zstack.header.zone.ZoneInventory
import org.zstack.test.integration.SimpleTest
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
        useSpring(SimpleTest.springSpec)
        spring {
            include("RESTFacade.xml")
        }
    }

    @Override
    void environment() {
        env = makeEnv {}
    }

    @Override
    void test() {
        env.create{
            restApiImpl = bean(RESTApiFacadeImpl.class)
            entityManagerFactory = restApiImpl.entityManagerFactory
            mgr = entityManagerFactory.createEntityManager()
            tran = mgr.getTransaction()

            testLargeRestAPIResultIsPersisted()
            testMultibyteRestAPIResultReturnsError()
            testOversizedRestAPIResultReturnsError()
            testCleanRestAPIVO()
            testNotCleanRestAPIVO()
            deleteRestAPIVO()
        }
    }

    void testLargeRestAPIResultIsPersisted() {
        String description = "x" * 70_000

        env.message(APICreateZoneMsg.class) { APICreateZoneMsg request, CloudBus bus ->
            APICreateZoneEvent event = new APICreateZoneEvent(request.id)
            ZoneInventory inventory = new ZoneInventory()
            inventory.name = request.name
            inventory.description = description
            event.inventory = inventory
            bus.publish(event)
        }

        APICreateZoneMsg request = new APICreateZoneMsg()
        request.name = "large-rest-result"
        def sent = restApiImpl.send(request)

        def response
        retryInSecs {
            response = restApiImpl.getResult(sent.uuid)
            assert response.state == RestAPIState.Done.toString()
        }

        APICreateZoneEvent storedEvent = RESTApiDecoder.loads(response.result) as APICreateZoneEvent
        assert storedEvent.success
        assert storedEvent.inventory.description == description

        tran.begin()
        mgr.remove(mgr.find(RestAPIVO.class, sent.uuid))
        tran.commit()
    }

    void testMultibyteRestAPIResultReturnsError() {
        String description = "中" * 5_600_000

        env.message(APICreateZoneMsg.class) { APICreateZoneMsg request, CloudBus bus ->
            APICreateZoneEvent event = new APICreateZoneEvent(request.id)
            ZoneInventory inventory = new ZoneInventory()
            inventory.name = request.name
            inventory.description = description
            event.inventory = inventory
            String serialized = RESTApiDecoder.dump(event)
            assert serialized.length() < CoreGlobalProperty.REST_API_RESULT_MAX_LENGTH
            assert serialized.getBytes("UTF-8").length > 16 * 1024 * 1024 - 1
            bus.publish(event)
        }

        APICreateZoneMsg request = new APICreateZoneMsg()
        request.name = "multibyte-rest-result"
        def sent = restApiImpl.send(request)

        def response
        retryInSecs {
            response = restApiImpl.getResult(sent.uuid)
            assert response.state == RestAPIState.Done.toString()
        }

        APICreateZoneEvent failedEvent = RESTApiDecoder.loads(response.result) as APICreateZoneEvent
        assert !failedEvent.success
        assert failedEvent.inventory == null
        assert failedEvent.error.code == "SYS.1020"
        assert failedEvent.error.details.contains("bytes")

        tran.begin()
        mgr.remove(mgr.find(RestAPIVO.class, sent.uuid))
        tran.commit()
    }

    void testOversizedRestAPIResultReturnsError() {
        int maxLength = CoreGlobalProperty.REST_API_RESULT_MAX_LENGTH
        String description = "x" * maxLength

        env.message(APICreateZoneMsg.class) { APICreateZoneMsg request, CloudBus bus ->
            APICreateZoneEvent event = new APICreateZoneEvent(request.id)
            ZoneInventory inventory = new ZoneInventory()
            inventory.name = request.name
            inventory.description = description
            event.inventory = inventory
            assert RESTApiDecoder.dump(event).length() > maxLength
            bus.publish(event)
        }

        APICreateZoneMsg request = new APICreateZoneMsg()
        request.name = "oversized-rest-result"
        def sent = restApiImpl.send(request)

        def response
        retryInSecs {
            response = restApiImpl.getResult(sent.uuid)
            assert response.state == RestAPIState.Done.toString()
        }

        APICreateZoneEvent failedEvent = RESTApiDecoder.loads(response.result) as APICreateZoneEvent
        assert !failedEvent.success
        assert failedEvent.inventory == null
        assert failedEvent.error.code == "SYS.1020"
        assert failedEvent.error.details.contains(APICreateZoneEvent.class.name)
        assert failedEvent.error.details.contains("exceeds the maximum allowed length")
        assert failedEvent.error.details.contains("$maxLength characters")

        tran.begin()
        mgr.remove(mgr.find(RestAPIVO.class, sent.uuid))
        tran.commit()
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
