package org.zstack.rest;

import org.apache.commons.collections.map.LRUMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIEvent;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/12/8.
 */
public class MysqlAsyncRestStore implements AsyncRestApiStore, Component {
    private static final CLogger logger = Utils.getLogger(MysqlAsyncRestStore.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private ThreadFacade thdf;

    // cache 2000 API results
    private Map<String, APIEvent> results = Collections.synchronizedMap(new LRUMap(RestGlobalProperty.MAX_CACHED_API_RESULTS));
    private Future cleanupThread;

    @Override
    @ExceptionSafe
    public void save(RequestData d) {
        AsyncRestVO vo = new AsyncRestVO();
        vo.setUuid(d.apiMessage.getId());
        vo.setRequestData(d.toJson());
        vo.setState(AsyncRestState.processing);
        dbf.persist(vo);
    }

    @Override
    public RequestData complete(APIEvent evt) {
        RequestData d = null;

        if (destinationMaker.isManagedByUs(evt.getApiId())) {
            AsyncRestVO vo = dbf.findByUuid(evt.getApiId(), AsyncRestVO.class);

            if (vo == null) {
                // for cases that directly send API message which we don't
                // have records
                if (logger.isTraceEnabled()) {
                    logger.warn(String.format("cannot find record for the API event %s", JSONObjectUtil.toJsonString(evt)));
                }

                return null;
            }

            vo.setState(AsyncRestState.done);
            vo.setResult(ApiEventResult.toJson(evt));
            dbf.update(vo);

            d = RequestData.fromJson(vo.getRequestData());
        }

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            // don't use the cache for unit test
            // we want to test the database
            results.put(evt.getApiId(), evt);
        }

        return d;
    }

    @Override
    public AsyncRestQueryResult query(String uuid) {
        AsyncRestQueryResult result = new AsyncRestQueryResult();
        result.setUuid(uuid);

        APIEvent evt = results.get(uuid);
        if (evt != null) {
            result.setState(AsyncRestState.done);
            result.setResult(evt);
            return result;
        }

        AsyncRestVO vo = dbf.findByUuid(uuid, AsyncRestVO.class);
        if (vo == null) {
            result.setState(AsyncRestState.expired);
            return result;
        }

        if (vo.getState() != AsyncRestState.done) {
            result.setState(vo.getState());
            return result;
        }

        try {
            result.setState(AsyncRestState.done);
            result.setResult(ApiEventResult.fromJson(vo.getResult()));

            results.put(uuid, result.getResult());
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        return result;
    }

    @Override
    public boolean start() {
        startExpiredApiCleanupThread();
        RestGlobalConfig.SCAN_EXPIRED_API_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startExpiredApiCleanupThread();
            }
        });

        return true;
    }

    private synchronized void startExpiredApiCleanupThread() {
        if (cleanupThread != null) {
            cleanupThread.cancel(true);
        }

        cleanupThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return RestGlobalConfig.SCAN_EXPIRED_API_INTERVAL.value(Integer.class).longValue();
            }

            @Override
            public String getName() {
                return "scan-expired-api-records";
            }

            @Override
            public void run() {
                try {
                    cleanup();
                } catch (Throwable t) {
                    logger.warn("unhandled error", t);
                }
            }

            private Timestamp getCurrentSqlTime() {
                Query q = dbf.getEntityManager().createNativeQuery("select current_timestamp()");
                return (Timestamp) q.getSingleResult();
            }

            @Transactional
            private void cleanup() {
                Integer expiredPeriod = RestGlobalConfig.COMPLETED_API_EXPIRED_PERIOD.value(Integer.class);
                String sql = "DELETE FROM AsyncRestVO vo WHERE vo.state = :state and vo.createDate < :date";
                Query q = dbf.getEntityManager().createQuery(sql);
                q.setParameter("state", AsyncRestState.done);
                q.setParameter("date", new Timestamp(getCurrentSqlTime().getTime() - expiredPeriod * 1000L));
                q.executeUpdate();
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
