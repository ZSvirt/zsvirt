package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadPool;
import org.zstack.core.thread.ThreadPoolRegisterExtensionPoint;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.APIIsReadyToGoMsg;
import org.zstack.header.apimediator.APIIsReadyToGoReply;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiWorkerThreadPoolStrategy;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.PortalErrors;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.managementnode.APIGetCurrentTimeMsg;
import org.zstack.header.managementnode.APIGetCurrentTimeReply;
import org.zstack.header.managementnode.APIGetManagementNodeArchMsg;
import org.zstack.header.managementnode.APIGetManagementNodeArchReply;
import org.zstack.header.managementnode.APIGetManagementNodeOSMsg;
import org.zstack.header.managementnode.APIGetManagementNodeOSReply;
import org.zstack.header.managementnode.APIGetPlatformTimeZoneMsg;
import org.zstack.header.managementnode.APIGetPlatformTimeZoneReply;
import org.zstack.header.managementnode.APIGetSupportAPIsMsg;
import org.zstack.header.managementnode.APIGetSupportAPIsReply;
import org.zstack.header.managementnode.APIGetVersionMsg;
import org.zstack.header.managementnode.APIGetVersionReply;
import org.zstack.header.managementnode.APIManagementNodeMessage;
import org.zstack.header.managementnode.IsManagementNodeReadyMsg;
import org.zstack.header.managementnode.IsManagementNodeReadyReply;
import org.zstack.header.managementnode.ManagementNodeConstant;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestAPIExtensionPoint;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


public class ApiMediatorImpl extends AbstractService implements
        ApiMediator,
        ThreadPoolRegisterExtensionPoint,
        GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(ApiMediator.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    private List<RestAPIExtensionPoint> apiExts = new ArrayList<>();

    private final String API_WORKER_SYNC_SIGNATURE = "api.worker";

    private final String ASYNC_API_WORKER_SYNC_SIGNATURE = "async.api.worker";

    private final String MANAGEMENT_API_WORKER_SYNC_SIGNATURE = "management.api.worker";

    private ApiMessageProcessor processor;

    private int apiWorkerNum = 5;
    private int asyncApiWorkerNum = 5;
    private int managementApiWorkerNum = 5;
    private ApiWorkerThreadPoolStrategy threadPoolStrategy = ApiWorkerThreadPoolStrategy.ISOLATED;

    private void dispatchMessage(APIMessage msg) {
        ApiMessageDescriptor desc = processor.getApiMessageDescriptor(msg);
        if (desc == null) {
            Map message = map(e(msg.getClass().getName(), msg));
            ErrorCode err = err(PortalErrors.NO_SERVICE_FOR_MESSAGE, "no PackageDescription route for message: %s", JSONObjectUtil.toJsonString(message));
            logger.warn(err.getDetails());
            bus.replyErrorByMessageType(msg, err);
            return;
        }


        try {
            msg.setServiceId(null);
            msg = processor.process(msg);
        } catch (ApiMessageInterceptionException ie) {
            logger.debug(ie.getError().toString(), ie);
            bus.replyErrorByMessageType(msg, ie.getError());
            return;
        } catch (StopRoutingException e) {
            return;
        }

        if (msg.getServiceId() == null && desc.getServiceId() != null) {
            bus.makeLocalServiceId(msg, desc.getServiceId());
        }

        if (msg.getServiceId() == null) {
            ErrorCode err = inerr("No service id found for API message[%s], message dump: %s", msg.getMessageName(), JSONObjectUtil.toJsonString(msg));
            logger.warn(err.getDetails());
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        if (!msg.hasSystemTag(PortalSystemTags.VALIDATION_ONLY.getTagFormat())) {
            bus.route(msg);
            return;
        }

        // this call is only for validate the API parameters
        if (msg instanceof APISyncCallMessage) {
            APIReply reply = new APIReply();
            bus.reply(msg, reply);
        } else {
            APIEvent evt = new APIEvent(msg.getId());
            bus.publish(evt);
        }
    }


    @Override
    public void handleMessage(final Message msg) {
        apiExts.forEach(e -> e.afterAPIRequest(msg));

        if (threadPoolStrategy == ApiWorkerThreadPoolStrategy.ISOLATED) {
            if (msg instanceof APIManagementNodeMessage) {
                managementCallMessageHandle(msg);
            } else if (msg instanceof APISyncCallMessage) {
                syncCallMessageHandle(msg);
            } else {
                asyncCallMessageHandle(msg);
            }
        } else {
            handleMessageInSharedStrategy(msg);
        }
    }

    /**
     * Handle message using legacy shared thread pool strategy.
     * In shared mode, all API calls (management, sync, async) share the same thread pool,
     * which is controlled by single parameter 'apiWorkerNum'.
     * This mode is kept for backwards compatibility.
     *
     * @param msg The message to be handled
     */
    private void handleMessageInSharedStrategy(final Message msg) {
        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return API_WORKER_SYNC_SIGNATURE;
            }

            @Override
            public int getSyncLevel() {
                return apiWorkerNum;
            }

            @Override
            public String getName() {
                return API_WORKER_SYNC_SIGNATURE;
            }

            @MessageSafe
            void handleMessage(Message msg) {
                doHandleAllMessages(msg);
            }

            @Override
            public Object call() {
                handleMessage(msg);
                return null;
            }
        });
    }

    private void doHandleAllMessages(final Message msg) {
        if (msg instanceof APIIsReadyToGoMsg) {
            handle((APIIsReadyToGoMsg) msg);
        } else if (msg instanceof APIGetVersionMsg) {
            handle((APIGetVersionMsg) msg);
        }else if (msg instanceof APIGetSupportAPIsMsg) {
            handle((APIGetSupportAPIsMsg) msg);
        } else if (msg instanceof APIGetCurrentTimeMsg) {
            handle((APIGetCurrentTimeMsg) msg);
        } else if (msg instanceof APIGetPlatformTimeZoneMsg) {
            handle((APIGetPlatformTimeZoneMsg) msg);
        } else if (msg instanceof APIGetManagementNodeArchMsg) {
            handle((APIGetManagementNodeArchMsg) msg);
        } else if (msg instanceof APIGetManagementNodeOSMsg) {
            handle((APIGetManagementNodeOSMsg) msg);
        } else if (msg instanceof APIMessage) {
            dispatchMessage((APIMessage) msg);
        } else {
            logger.debug("Not an APIMessage.Message ID is " + msg.getId());
        }
    }

    private void managementCallMessageHandle(final Message msg) {
        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return MANAGEMENT_API_WORKER_SYNC_SIGNATURE;
            }

            @Override
            public int getSyncLevel() {
                return managementApiWorkerNum;
            }

            @Override
            public String getName() {
                return MANAGEMENT_API_WORKER_SYNC_SIGNATURE;
            }

            @MessageSafe
            void handleMessage(Message msg) {
                doHandleManagementMessage(msg);
            }

            @Override
            public Object call() {
                handleMessage(msg);
                return null;
            }
        });
    }

    private void doHandleManagementMessage(final Message msg) {
        if (msg instanceof APIIsReadyToGoMsg) {
            handle((APIIsReadyToGoMsg) msg);
        } else if (msg instanceof APIGetVersionMsg) {
            handle((APIGetVersionMsg) msg);
        }else if (msg instanceof APIGetSupportAPIsMsg) {
            handle((APIGetSupportAPIsMsg) msg);
        } else if (msg instanceof APIGetCurrentTimeMsg) {
            handle((APIGetCurrentTimeMsg) msg);
        } else if (msg instanceof APIGetPlatformTimeZoneMsg) {
            handle((APIGetPlatformTimeZoneMsg) msg);
        } else if (msg instanceof APIGetManagementNodeArchMsg) {
            handle((APIGetManagementNodeArchMsg) msg);
        } else if (msg instanceof APIGetManagementNodeOSMsg) {
            handle((APIGetManagementNodeOSMsg) msg);
        } else {
            logger.debug("Not an APIMessage.Message ID is " + msg.getId());
        }
    }

    private void doHandleMessage(final Message msg) {
        if (msg instanceof APIMessage) {
            dispatchMessage((APIMessage) msg);
        } else {
            logger.debug("Not an APIMessage.Message ID is " + msg.getId());
        }
    }

    private void asyncCallMessageHandle(final Message msg) {
        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return ASYNC_API_WORKER_SYNC_SIGNATURE;
            }

            @Override
            public int getSyncLevel() {
                return asyncApiWorkerNum;
            }

            @Override
            public String getName() {
                return ASYNC_API_WORKER_SYNC_SIGNATURE;
            }

            @MessageSafe
            void handleMessage(Message msg) {
                doHandleMessage(msg);
            }

            @Override
            public Object call() {
                handleMessage(msg);
                return null;
            }
        });
    }

    private void syncCallMessageHandle(final Message msg) {
        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return API_WORKER_SYNC_SIGNATURE;
            }

            @Override
            public int getSyncLevel() {
                return apiWorkerNum;
            }

            @Override
            public String getName() {
                return API_WORKER_SYNC_SIGNATURE;
            }

            @MessageSafe
            void handleMessage(Message msg) {
                doHandleMessage(msg);
            }

            @Override
            public Object call() {
                handleMessage(msg);
                return null;
            }
        });
    }

    private void handle(APIGetSupportAPIsMsg msg) {
        APIGetSupportAPIsReply reply = new APIGetSupportAPIsReply();
        reply.setSupportApis(processor.getSupportApis());
        bus.reply(msg, reply);
    }

    private void handle(APIGetPlatformTimeZoneMsg msg) {
        APIGetPlatformTimeZoneReply reply = new APIGetPlatformTimeZoneReply();
        ZonedDateTime time = ZonedDateTime.now();
        reply.setOffset(time.getOffset().getId());
        reply.setTimezone(time.getZone().getId());
        bus.reply(msg, reply);
    }

    private void handle(APIGetVersionMsg msg) {
        APIGetVersionReply reply = new APIGetVersionReply();
        reply.setVersion(dbf.getDbVersion());
        bus.reply(msg, reply);
    }

    private void handle(APIGetCurrentTimeMsg msg) {
        Map<String, Long> ret = new HashMap<>();
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = System.currentTimeMillis()/1000;
        ret.put("MillionSeconds", currentTimeMillis);
        ret.put("Seconds", currentTimeSeconds);
        APIGetCurrentTimeReply reply = new APIGetCurrentTimeReply();
        reply.setCurrentTime(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIGetManagementNodeArchMsg msg) {
        APIGetManagementNodeArchReply reply = new APIGetManagementNodeArchReply();
        reply.setArchitecture(System.getProperty("os.arch").equals("amd64") ? "x86_64" : System.getProperty("os.arch"));
        bus.reply(msg, reply);
    }

    private void handle(APIGetManagementNodeOSMsg msg) {
        APIGetManagementNodeOSReply reply = new APIGetManagementNodeOSReply();
        reply.setName(System.getProperty("os.name"));
        reply.setVersion(System.getProperty("os.version"));
        bus.reply(msg, reply);
    }

    private void handle(final APIIsReadyToGoMsg msg) {
        final APIIsReadyToGoReply areply = new APIIsReadyToGoReply();

        IsManagementNodeReadyMsg imsg = new IsManagementNodeReadyMsg();
        String nodeId = msg.getManagementNodeId();
        if (nodeId == null) {
            bus.makeLocalServiceId(imsg, ManagementNodeConstant.SERVICE_ID);
            nodeId = Platform.getManagementServerId();
        } else {
            bus.makeServiceIdByManagementNodeId(imsg, ManagementNodeConstant.SERVICE_ID, msg.getManagementNodeId());
        }

        final String fnodeId = nodeId;
        areply.setManagementNodeId(nodeId);
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    areply.setError(reply.getError());
                } else {
                    IsManagementNodeReadyReply r = (IsManagementNodeReadyReply) reply;
                    if (!r.isReady()) {
                        areply.setError(err(SysErrors.NOT_READY_ERROR,
                                "management node[uuid:%s] is not ready yet", fnodeId));
                    }
                }
                bus.reply(msg, areply);
            }
        });
    }

    @Override
    public String getId() {
        return ApiMediatorConstant.SERVICE_ID;
    }

    @Override
    public boolean start() {
        processor = new ApiMessageProcessorImpl(new HashMap<>());
        bus.registerService(this);
        apiExts = pluginRgty.getExtensionList(RestAPIExtensionPoint.class);

        logThreadPoolConfiguration();
        return true;
    }

    private void logThreadPoolConfiguration() {
        if (threadPoolStrategy == ApiWorkerThreadPoolStrategy.SHARED) {
            logger.info("API Worker running in SHARED thread pool mode:");
            logger.info("- All API calls share same thread pool");
            logger.info("- Available parameters:");
            logger.info(String.format("  * apiWorkerNum: %d (thread pool size for all API calls)", apiWorkerNum));
        } else {
            logger.info("API Worker running in ISOLATED thread pool mode:");
            logger.info("- Using separate thread pools for different API types");
            logger.info("- Available parameters:");
            logger.info(String.format("  * managementApiWorkerNum: %d (thread pool size for management APIs)", managementApiWorkerNum));
            logger.info(String.format("  * apiWorkerNum: %d (thread pool size for sync APIs)", apiWorkerNum));
            logger.info(String.format("  * asyncApiWorkerNum: %d (thread pool size for async APIs)", asyncApiWorkerNum));
            logger.info(String.format("  * threadPoolStrategy: %s (SHARED/ISOLATED)", threadPoolStrategy));
        }
    }

    @Override
    public boolean stop() {
        bus.unregisterService(this);
        return true;
    }

    public void setApiWorkerNum(int apiWorkerNum) {
        this.apiWorkerNum = apiWorkerNum;
    }

    public void setAsyncApiWorkerNum(int asyncApiWorkerNum) {
        this.asyncApiWorkerNum = asyncApiWorkerNum;
    }

    public void setManagementApiWorkerNum(int managementApiWorkerNum) {
        this.managementApiWorkerNum = managementApiWorkerNum;
    }

    public void setThreadPoolStrategy(ApiWorkerThreadPoolStrategy threadPoolStrategy) {
        this.threadPoolStrategy = threadPoolStrategy;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> lst = new ArrayList<>();
        lst.add(APICreateMessage.class);
        return lst;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateMessage) {
            APICreateMessage cmsg = (APICreateMessage) msg;
            if (cmsg.getResourceUuid() != null) {
                if (!StringDSL.isZStackUuid(cmsg.getResourceUuid())) {
                    throw new ApiMessageInterceptionException(argerr("resourceUuid[%s] is not a valid uuid. A valid uuid is a UUID(v4 recommended) with '-' stripped. " +
                                    "see http://en.wikipedia.org/wiki/Universally_unique_identifier for format of UUID, the regular expression uses" +
                                    " to validate a UUID is '[0-9a-f]{8}[0-9a-f]{4}[1-5][0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}'", cmsg.getResourceUuid()));
                }
            }
        }
        return msg;
    }

    @Override
    public ApiMessageProcessor getProcesser() {
        return processor;
    }

    @Override
    public List<ThreadPool> registerThreadPool() {
        if (threadPoolStrategy == ApiWorkerThreadPoolStrategy.SHARED) {
            return null;
        }

        ThreadPool pool = new ThreadPool();
        pool.setSyncSignature(API_WORKER_SYNC_SIGNATURE);
        pool.setThreadNum(apiWorkerNum);

        ThreadPool asyncPool = new ThreadPool();
        asyncPool.setSyncSignature(ASYNC_API_WORKER_SYNC_SIGNATURE);
        asyncPool.setThreadNum(asyncApiWorkerNum);

        ThreadPool managementPool = new ThreadPool();
        managementPool.setSyncSignature(MANAGEMENT_API_WORKER_SYNC_SIGNATURE);
        managementPool.setThreadNum(managementApiWorkerNum);

        return Arrays.asList(pool, asyncPool, managementPool);
    }
}
