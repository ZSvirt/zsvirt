package org.zstack.core.rest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.log.LogSafeGson;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.log.MaskSensitiveInfo;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTApiFacade;
import org.zstack.header.rest.RestAPIResponse;
import org.zstack.header.rest.RestAPIState;
import org.zstack.header.rest.RestAPIVO;
import org.zstack.header.search.APISearchMessage;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RESTApiFacadeImpl extends AbstractService implements RESTApiFacade, CloudBusEventListener, Component {
    private static final CLogger logger = Utils.getLogger(RESTApiFacadeImpl.class);

    private EntityManagerFactory entityManagerFactory;
    private Set<String> basePkgNames;
    private List<String> processingRequests = Collections.synchronizedList(new ArrayList<>(100));
    private Future<Void> restAPIVOCleanTask = null;
    private final static int restResultMaxLength = initMaxRestResultLength();

    private static final Set<String> maskSensitiveInfoClassNames = Platform.getReflections()
            .getTypesAnnotatedWith(MaskSensitiveInfo.class).stream()
            .map(Class::getSimpleName).collect(Collectors.toSet());

    @Autowired
    private ResourceDestinationMaker destMaker;

    @Autowired
    private CloudBus bus;

    @Autowired
    private ThreadFacade thdf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof DeleteRestApiVOMsg) {
            handle((DeleteRestApiVOMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(RESTApiConstant.SERVICE_ID);
    }

    private void handle(final DeleteRestApiVOMsg msg){
        int ret = 1;
        int delete = 0;
        EntityManager mgr = getEntityManager();
        EntityTransaction tran = mgr.getTransaction();
        Long time = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.DAYS.toSeconds(msg.getRetentionDay());
        long start = System.currentTimeMillis();
        try {
            while (ret > 0) {
                String sql = String.format("delete from RestAPIVO where unix_timestamp(lastOpDate) <= %d limit 1000", time);
                tran.begin();
                Query query = mgr.createNativeQuery(sql);
                ret = query.executeUpdate();
                tran.commit();
                delete = delete + ret;
                if (delete == 0) {
                    logger.debug("no RestApiVO history to clean");
                    return;
                }
            }
            logger.debug(String.format("delete %d days ago RestApiVO history %d, cost %d ms", msg.getRetentionDay(), delete, System.currentTimeMillis() - start));
        } catch (Exception e) {
            tran.rollback();
            logger.warn(String.format("unable to delete RestApiVO history because %s", e));
        } finally {
            mgr.close();
        }
    }

    void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Set<APIEvent> boundEvents = new HashSet<APIEvent>(100);
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
        scanner.resetFilters(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(APIEvent.class));
        for (String pkg : getBasePkgNames()) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                if (clazz == APIEvent.class) {
                    continue;
                }
                APIEvent evt = (APIEvent) clazz.newInstance();
                boundEvents.add(evt);
            }
        }

        bus.subscribeEvent(this, boundEvents.toArray(new APIEvent[boundEvents.size()]));
    }

    private static int initMaxRestResultLength() {
        int limit = CoreGlobalProperty.REST_API_RESULT_MAX_LENGTH;
        limit = Math.min(limit, RESTApiConstant.REST_API_RESULT_MAX_LENGTH);
        return Math.max(limit, RESTApiConstant.REST_API_RESULT_MIN_LENGTH);
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    private RestAPIVO persist(APIMessage msg) {
        RestAPIVO vo = new RestAPIVO();
        vo.setUuid(msg.getId());
        vo.setApiMessageName(msg.getMessageName());
        vo.setState(RestAPIState.Processing);
        EntityManager mgr = getEntityManager();
        EntityTransaction tran = mgr.getTransaction();
        try {
            tran.begin();
            mgr.persist(vo);
            mgr.flush();
            mgr.refresh(vo);
            tran.commit();
            return vo;
        } catch (Exception e) {
            ExceptionDSL.exceptionSafe(tran::rollback);
            throw new CloudRuntimeException(e);
        } finally {
            ExceptionDSL.exceptionSafe(mgr::close);
        }
    }

    @Override
    public RestAPIResponse send(APIMessage msg) {
        assert !(msg instanceof APIListMessage) && !(msg instanceof APISearchMessage) : "You must invoke call(APIMessage) for APIListMessage or APISearchMsg, the message you pass is "
                + msg.getMessageName();
        RestAPIResponse rsp = new RestAPIResponse();
        RestAPIVO vo = persist(msg);
        processingRequests.add(vo.getUuid());
        rsp.setCreatedDate(vo.getCreateDate());
        rsp.setState(vo.getState().toString());
        rsp.setUuid(vo.getUuid());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        bus.send(msg);
        return rsp;
    }

    @Override
    public RestAPIResponse call(APIMessage msg) {
        RestAPIResponse rsp = new RestAPIResponse();
        rsp.setCreatedDate(new Date());
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        MessageReply reply = bus.call(msg);
        rsp.setFinishedDate(new Date());
        rsp.setState(RestAPIState.Done.toString());

        if (CoreGlobalProperty.MASK_SENSITIVE_INFO || maskSensitiveInfoClassNames.contains(reply.getClass().getSimpleName())) {
            reply = (MessageReply) LogSafeGson.desensitize(reply);
        }

        rsp.setResult(RESTApiDecoder.dump(reply));
        return rsp;
    }

    private RestAPIVO find(String uuid) {
        EntityManager mgr = getEntityManager();
        EntityTransaction tran = mgr.getTransaction();
        try {
            tran.begin();
            RestAPIVO vo = mgr.find(RestAPIVO.class, uuid);
            tran.commit();
            return vo;
        } catch (Exception e) {
            tran.rollback();
            throw new CloudRuntimeException(e);
        } finally {
            mgr.close();
        }
    }
    
    @Override
    public RestAPIResponse getResult(String uuid) {
        RestAPIVO vo = find(uuid);
        if (vo == null) {
            return null;
        }
        RestAPIResponse rsp = new RestAPIResponse();
        rsp.setCreatedDate(vo.getCreateDate());
        rsp.setFinishedDate(vo.getLastOpDate());
        rsp.setResult(vo.getResult());
        rsp.setState(vo.getState().toString());
        rsp.setUuid(vo.getUuid());
        return rsp;
    }

    private synchronized EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    private boolean update(APIEvent e) {
        String sql = "update RestAPIVO r set r.result = :result, r.state = :state where r.uuid = :uuid";
        EntityManager mgr = getEntityManager();
        EntityTransaction tran = mgr.getTransaction();
        try {
            tran.begin();
            Query query = mgr.createQuery(sql);
            query.setParameter("result", getApiResult(e));
            query.setParameter("state", RestAPIState.Done);
            query.setParameter("uuid", e.getApiId());
            int ret = query.executeUpdate();
            tran.commit();
            return ret > 0;
        } catch (Exception ex) {
            tran.rollback();
            throw new CloudRuntimeException(ex);
        } finally {
            mgr.close();
        }
    }

    private static String getApiResult(APIEvent e) {
        String apiResult = RESTApiDecoder.dump(e);
        apiResult = StringUtils.length(apiResult) > restResultMaxLength ?
                StringUtils.left(apiResult, restResultMaxLength) : apiResult;
        return apiResult;
    }

    @Override
    public boolean handleEvent(Event e) {
        try {
            if (e instanceof APIEvent) {
                APIEvent ae = (APIEvent) e;
                if (processingRequests.contains(ae.getApiId())) {
                    boolean ret = update(ae);
                    processingRequests.remove(ae.getApiId());
                    if (!ret) {
                        logger.warn(String.format("Cannot find RestAPIVO[uuid:%s], something wrong happened", ae.getApiId()));
                    }
                }
            } else {
                bus.dealWithUnknownMessage(e);
            }
        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }

        return false;
    }

    public Set<String> getBasePkgNames() {
        if (basePkgNames == null) {
            basePkgNames = new HashSet<String>();
            basePkgNames.add("org.zstack");
        }
        return basePkgNames;
    }

    public synchronized void refreshIntervalClean() {
        if (restAPIVOCleanTask != null){
            restAPIVOCleanTask.cancel(true);
        }
        startIntervalClean();
    }

    private void startIntervalClean() {
        checkParams();
        if (RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY == -1){
            logger.debug("ResetApiVO retention day -1 ,not clean");
            return;
        }
        restAPIVOCleanTask = thdf.submitPeriodicTask(restAPIVOCleanTask(), RESTApiGlobalProperty.CLEAN_RESTAPIVO_DELAY);
    }

    private void checkParams() {
        if (RESTApiGlobalProperty.CLEAN_RESTAPIVO_DELAY < 0 || RESTApiGlobalProperty.CLEAN_RESTAPIVO_DELAY > 3600) {
            throw new IllegalArgumentException("RestApiVO period clean task delay time must >= 0s and <= 3600s");
        }

        if (RESTApiGlobalProperty.CLEAN_INTERVAL_SECOND < 86400 || RESTApiGlobalProperty.CLEAN_INTERVAL_SECOND > 864000) {
            throw new IllegalArgumentException("RestApiVO period clean task interval must >= 86400s and <= 864000s");
        }

        if (RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY < -1 || RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY > 365) {
            throw new IllegalArgumentException("RestApiVO retention day must >= -1 day and <= 365 day, if set -1, will not clean RestApiVO");
        }
    }

    private PeriodicTask restAPIVOCleanTask(){
        return new PeriodicTask(){

            @Override
            public void run() {
                if (!destMaker.isManagedByUs(RESTApiConstant.CleanRestAPIVOKey)) {
                    logger.debug(String.format("Not send DeleteRestApiVOMsg because not managed by us"));
                    return;
                }
                DeleteRestApiVOMsg msg = new DeleteRestApiVOMsg();
                msg.setRetentionDay(RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY);
                bus.makeTargetServiceIdByResourceUuid(msg, RESTApiConstant.SERVICE_ID, RESTApiConstant.CleanRestAPIVOKey);
                bus.send(msg);
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return RESTApiGlobalProperty.CLEAN_INTERVAL_SECOND;
            }

            @Override
            public String getName() {
                return String.format("clean-RestApiVO-periodic-Task");
            }
        };
    }

    @Override
    public boolean start() {
        startIntervalClean();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
