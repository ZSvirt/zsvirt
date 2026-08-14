package org.zstack.portal.managementnode;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CanonicalEventEmitter;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.debug.DebugManager;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.IsManagementNodeReadyMsg;
import org.zstack.header.managementnode.IsManagementNodeReadyReply;
import org.zstack.header.managementnode.ManagementNodeCanonicalEvent;
import org.zstack.header.managementnode.ManagementNodeCanonicalEvent.LifeCycle;
import org.zstack.header.managementnode.ManagementNodeCanonicalEvent.ManagementNodeLifeCycleData;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeConstant;
import org.zstack.header.managementnode.ManagementNodeExitMsg;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeState;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.FindSameNodeExtensionPoint;
import org.zstack.header.vo.ResourceInventory;
import org.zstack.portal.apimediator.ApiMediator;
import org.zstack.utils.BootErrorLog;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.zstack.utils.ExceptionDSL.throwableSafe;

public class ManagementNodeManagerImpl extends AbstractService implements ManagementNodeManager, FindSameNodeExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ManagementNodeManager.class);

    private List<ComponentWrapper> components;
    private List<PrepareDbInitialValueExtensionPoint> prepareDbExts;
    private ManagementNodeVO node;
    private volatile boolean isRunning = true;
    private volatile int isNodeRunning = NODE_STARTING;
    private static final String INVENTORY_LOCK = "ManagementNodeManager.inventory_lock";
    private final int INVENTORY_LOCK_TIMEOUT = 600; /* 10 mins */
    private static boolean started = false;
    private static boolean stopped = false;
    private Future<Void> heartBeatTask = null;
    private HeartBeatDBSource heartBeatDBSource;
    private List<ManagementNodeChangeListener> lifeCycleExtension = new ArrayList<ManagementNodeChangeListener>();
    // A dictionary (nodeId -> ManagementNodeInventory) of joined management Node
    final private Map<String, ManagementNodeInventory> joinedManagementNodes = new ConcurrentHashMap<>();

    private static int NODE_STARTING = 0;
    private static int NODE_RUNNING = 1;
    private static int NODE_FAILED = -1;

    @Override
    public ResourceInventory findSameNode(String hostname) {
        String uuid = Q.New(ManagementNodeVO.class).eq(ManagementNodeVO_.hostName, hostname)
                .select(ManagementNodeVO_.uuid).findValue();
        if (uuid == null) {
            return null;
        } else {
            ResourceInventory info = new ResourceInventory();
            info.setUuid(uuid);
            info.setResourceType(ManagementNodeVO.class.getSimpleName());
            return info;
        }
    }

    public static class ManagementNodeTimeRegressionCanonicalEvent extends CanonicalEventEmitter {
        ManagementNodeCanonicalEvent.ManagementNodeTemporalRegressionData data;

        public ManagementNodeTimeRegressionCanonicalEvent(String nodeUuid, String hostname) {
            data = new ManagementNodeCanonicalEvent.ManagementNodeTemporalRegressionData();
            data.setNodeUuid(nodeUuid);
            data.setHostname(hostname);
        }

        public void fire() {
            fire(ManagementNodeCanonicalEvent.NODE_TEMPORAL_REGRESSION_PATH, data);
        }
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBusIN bus;
    @Autowired
    private ApiMediator apim;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DebugManager debugManager;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    @Autowired
    private EventFacade evtf;

    private boolean sigUsr2 = false;

    void init() {
        heartBeatDBSource = new HeartBeatDBSource();
    }

    private final ManagementNodeChangeListener nodeLifeCycle = new ManagementNodeChangeListener() {
        @Override
        public void nodeJoin(ManagementNodeInventory inv) {
            final String nodeId = inv.getUuid();
            if (joinedManagementNodes.putIfAbsent(nodeId, inv) != null) {
                return;
            }

            logger.debug(String.format("missing management node[uuid:%s] in current hash ring, call node-join", nodeId));
            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.nodeJoin(inv);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.nodeJoin(inv);
                }
            });
        }

        @Override
        public void nodeLeft(ManagementNodeInventory inv) {
            final String nodeId = inv.getUuid();
            if (joinedManagementNodes.remove(nodeId) == null && !destinationMaker.getManagementNodesInHashRing().contains(nodeId)) {
                logger.debug(String.format("the management node[uuid:%s] is not in our hash ring, ignore this node-left call", nodeId));
                return;
            }

            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.nodeLeft(inv);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.nodeLeft(inv);
                }
            });
        }

        @Override
        public void iAmDead(ManagementNodeInventory inv) {
            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.iAmDead(inv);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.iAmDead(inv);
                }
            });
        }

        @Override
        public void iJoin(ManagementNodeInventory inv) {
            ManagementNodeChangeListener l = (ManagementNodeChangeListener) destinationMaker;
            l.iJoin(inv);

            CollectionUtils.safeForEach(lifeCycleExtension, new ForEachFunction<ManagementNodeChangeListener>() {
                @Override
                public void run(ManagementNodeChangeListener arg) {
                    arg.iJoin(inv);
                }
            });
        }
    };

    private interface ComponentWrapper {
        void start();

        void stop();
    }

    private void notifyStop() {
        isRunning = false;
        synchronized (this) {
            this.notify();
        }
    }

    private void handle(ManagementNodeExitMsg msg) {
        logger.debug(getId() + " received ManagementNodeExitMsg, going to exit. Details: " + msg.getDetails());
        notifyStop();
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleLocalMessage(Message msg) {
        if (msg.getClass() == ManagementNodeExitMsg.class) {
            handle((ManagementNodeExitMsg) msg);
        } else if (msg instanceof IsManagementNodeReadyMsg) {
            handle((IsManagementNodeReadyMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(IsManagementNodeReadyMsg msg) {
        IsManagementNodeReadyReply reply = new IsManagementNodeReadyReply();
        reply.setReady(isNodeRunning == NODE_RUNNING);
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ManagementNodeConstant.SERVICE_ID);
    }


    private void startComponents() {
        for (ComponentWrapper c : components) {
            c.start();
        }
    }

    private void stopComponents() {
        for (final ComponentWrapper c : components) {
            try {
                c.stop();
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }

    private void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String reason = System.getProperty(Platform.EXIT_REASON);
            if (reason != null) {
                logger.debug(String.format("JVM shutdown hook is called[reason: %s], start stopping management node", reason));
            } else {
                logger.debug("JVM shutdown hook is called, start stopping management node");
            }

            String skipStop = System.getProperty(Platform.SKIP_STOP);
            if (!StringUtils.isEmpty(skipStop) && skipStop.equalsIgnoreCase(Boolean.TRUE.toString())) {
                logger.debug("Exit directly, skip the managementNode exit");
                return;
            }

            stop(true);
        }));
    }

    private void populateComponents() {
        components = new ArrayList<>();
        for (final Component c : pluginRgty.getExtensionList(Component.class)) {
            components.add(new ComponentWrapper() {
                boolean isStart = false;

                @Override
                public void start() {
                    logger.info("starting component: " + c.getClass().getName());
                    long start = System.currentTimeMillis();
                    c.start();
                    long end = System.currentTimeMillis();
                    logger.info(String.format("component[%s] starts successfully, cost %d ms to start", c.getClass(), end - start));
                    isStart = true;
                }

                @Override
                public void stop() {
                    if (isStart) {
                        throwableSafe((Runnable) () -> {
                            c.stop();
                            logger.info("Stopped component: " + c.getClass().getName());
                            isStart = false;
                        }, String.format("unable to stop component[%s]", c.getClass().getName()));
                    }
                }
            });
        }

        prepareDbExts = pluginRgty.getExtensionList(PrepareDbInitialValueExtensionPoint.class);
    }

    private void callPrepareDbExtensions() {
        for (PrepareDbInitialValueExtensionPoint extp : prepareDbExts) {
            extp.prepareDbInitialValue();
        }
    }

    private void populateExtensions() {
        lifeCycleExtension = pluginRgty.getExtensionList(ManagementNodeChangeListener.class);
    }

    private EventCallback nodeLifeCycleCallback = new EventCallback() {
        @Override
        protected void run(Map tokens, Object data) {
            if (evtf.isFromThisManagementNode(tokens)) {
                return;
            }

            ManagementNodeLifeCycleData d = (ManagementNodeLifeCycleData) data;

            if (LifeCycle.NodeJoin.toString().equals(d.getLifeCycle())) {
                nodeLifeCycle.nodeJoin(d.getInventory());
            } else if (LifeCycle.NodeLeft.toString().equals(d.getLifeCycle())) {
                nodeLifeCycle.nodeLeft(d.getInventory());
            } else {
                throw new CloudRuntimeException(String.format("unknown lifecycle[%s]", d.getLifeCycle()));
            }
        }
    };

    void setSigUsr2() {
        sigUsr2 = true;
    }

    private void dumpDebugMessages() {
        for (String signal : debugManager.getDebugSignals()) {
            debugManager.handleSig(signal);
        }
    }

    @Override
    public boolean start() {
        if (started) {
            /* largely for unittest, the ComponentLoaderWebListener and Api may both call start()
	         */
            logger.debug("Management Node has already started, ignore this call");
            return true;
        }

        populateExtensions();

        started = true;
        stopped = true;

        class Result {
            boolean success;
        }

        final Result ret = new Result();

        GLock lock = new GLock(INVENTORY_LOCK, INVENTORY_LOCK_TIMEOUT);
		/*
	     * The lock is being held until we join in, otherwise the inventory
	     * may be deleted by other exiting node because we have not
		 * persisted our entry in management_node table yet, or two starting
		 * nodes persist inventory concurrently.
	     */
        lock.lock();
        try {
            final ManagementNodeManagerImpl self = this;
            FlowChain bootstrap = FlowChainBuilder.newSimpleFlowChain();
            bootstrap.setName("management-node-bootstrap");
            bootstrap.preCheck(data -> {
                return Platform.IS_RUNNING ? null : new ErrorCode(SysErrors.INTERNAL.toString(),
                        "the management node is not running for some reason while starting");
            });
            bootstrap.then(new Flow() {
                String __name__ = "bootstrap-cloudbus";

                // CloudBus is special, it is initialized in Platform.createComponentLoaderFromWebApplicationContext(),
                // however, when exception happens in bootstrap we need to stop bus in rollback, because the exception
                // cannot make JVM exist and cloudbus.stop is only called in JVM exit hook;
                @Override
                public void run(FlowTrigger trigger, Map data) {
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    bus.stop();
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "populate-components";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    populateComponents();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "register-node-on-cloudbus";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    bus.registerService(self);
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    bus.unregisterService(self);
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "call-prepare-db-extension";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    callPrepareDbExtensions();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "start-components";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    startComponents();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    stopComponents();
                    trigger.rollback();
                }
            }).then(new Flow() {
                String __name__ = "create-DB-record";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            String ip = Platform.getManagementServerIp();
                            String uuid = Platform.getManagementServerId();

                            sql(ManagementNodeVO.class).eq(ManagementNodeVO_.uuid, uuid).hardDelete();

                            ManagementNodeVO vo = new ManagementNodeVO();
                            vo.setHostName(ip);
                            vo.setUuid(uuid);
                            persist(vo);
                            reload(vo);
                            node = vo;
                        }
                    }.execute();

                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (node != null) {
                        dbf.remove(node());
                    }

                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "start-heartbeat";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    setupHeartbeat();
                    trigger.next();
                }
            }).then(new Flow() {
                String __name__ = "start-api-mediator";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    apim.start();
                    trigger.next();
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    apim.stop();
                    trigger.rollback();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "set-node-to-running";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    node.setState(ManagementNodeState.RUNNING);
                    node = dbf.updateAndRefresh(node);
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "I-join";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    nodeLifeCycle.iJoin(ManagementNodeInventory.valueOf(node));
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "node-is-ready";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    for (ManagementNodeReadyExtensionPoint ext : pluginRgty.getExtensionList(ManagementNodeReadyExtensionPoint.class)) {
                        ext.managementNodeReady();
                    }

                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "listen-node-life-cycle-events";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    evtf.on(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, nodeLifeCycleCallback);
                    trigger.next();
                }
            }).then(new NoRollbackFlow() {
                String __name__ = "say-I-join";

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    ManagementNodeLifeCycleData d = new ManagementNodeLifeCycleData();
                    d.setNodeUuid(node().getUuid());
                    d.setInventory(ManagementNodeInventory.valueOf(node()));
                    d.setLifeCycle(LifeCycle.NodeJoin.toString());
                    evtf.fire(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, d);
                    trigger.next();
                }
            }).done(new FlowDoneHandler(null) {
                @Override
                public void handle(Map data) {
                    ret.success = true;
                }
            }).error(new FlowErrorHandler(null) {
                @Override
                public void handle(ErrorCode errCode, Map data) {
                    new BootErrorLog().write(errCode.toString());
                    ret.success = false;
                }
            }).start();
        } finally {
            lock.unlock();
        }

        if (!ret.success || !Platform.IS_RUNNING) {
            logger.warn(String.format("management node[%s] failed to start for some reason", Platform.getUuid()));
            stopped = true;

            if (CoreGlobalProperty.EXIT_JVM_ON_BOOT_FAILURE) {
                logger.debug(String.format("unable to start management node[%s], see previous exception. exitJVMOnBootFailure is set to true, exit JVM now", Platform.getManagementServerId()));
                System.exit(1);
            } else {
                throw new CloudRuntimeException(String.format("unable to start management node[%s], see previous exception", Platform.getManagementServerId()));
            }
        }

        stopped = false;

        installShutdownHook();
        DebugSignalHandler.listenTo("USR2", this);

        logger.info("Management node: " + getId() + " starts successfully");

        synchronized (this) {
            isNodeRunning = NODE_RUNNING;
            while (isRunning) {
                try {
                    if (this.sigUsr2) {
                        dumpDebugMessages();
                        this.sigUsr2 = false;
                    }
                    this.wait(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while daemon is running, continue ...", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        logger.debug("quited main-loop, start stopping management node");
        stop();
        return true;
    }

    private void setupHeartbeat() {
        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startHeartbeat();
            }
        });

        startHeartbeat();
    }

    private class HeartBeatDBSource {
        private final Connection conn;
        private final SingleConnectionDataSource source;
        private final AtomicBoolean destroyed = new AtomicBoolean(false);
        private final ExecutorService connectionTimeoutExecutor;
        JdbcTemplate jdbc;

        HeartBeatDBSource() {
            try {
                connectionTimeoutExecutor = Executors.newFixedThreadPool(3);
                conn = dbf.getExtraDataSource().getConnection();
                conn.setNetworkTimeout(connectionTimeoutExecutor, (int) TimeUnit.SECONDS.toMillis(PortalGlobalProperty.HEART_BEAT_QUERY_TIMEOUT));
                source = new SingleConnectionDataSource(conn, true);
                jdbc = new JdbcTemplate(source);
            } catch (SQLException e) {
                throw new CloudRuntimeException(e);
            }
        }

        @ExceptionSafe
        public void destroy() {
            if (!destroyed.compareAndSet(false, true)) {
                return;
            }

            source.destroy();
            try {
                conn.close();
            } catch (SQLException e) {
                throw new CloudRuntimeException(e);
            }

            connectionTimeoutExecutor.shutdown();
        }
    }

    private ManagementNodeVO node() {
        DebugUtils.Assert(node != null, "node is set to null!!!");
        return node;
    }

    private synchronized void startHeartbeat() {
        if (heartBeatTask != null) {
            heartBeatTask.cancel(true);
        }

        heartBeatTask = thdf.submit(new Task<Void>() {
            // WARNING: NO dbf(DatabaseFacade) used in this task,
            // you MUST USE heartBeatDBSource for any database operation

            private final List<ManagementNodeVO> suspects = new ArrayList<>();
            private Timestamp lastHearbeatTime = null;

            @Override
            public String getName() {
                return String.format("managementNode-%s-heartbeat", Platform.getManagementServerId());
            }

            private boolean amIalive() {
                String sql = "select count(*) from ManagementNodeVO where uuid = ?";
                long count = heartBeatDBSource.jdbc.queryForObject(sql, new Object[]{node().getUuid()}, Long.class);
                return count != 0;
            }

            private ManagementNodeVO getNode(String uuid) {
                try {
                    String sql = "select * from ManagementNodeVO where uuid = ?";
                    return (ManagementNodeVO) heartBeatDBSource.jdbc.queryForObject(sql, new Object[]{uuid}, new BeanPropertyRowMapper(ManagementNodeVO.class));
                } catch (IncorrectResultSizeDataAccessException e) {
                    return null;
                }
            }

            private int deleteNode(ManagementNodeVO vo) {
                String sql = "delete from ManagementNodeVO where uuid = ?";
                int ret = heartBeatDBSource.jdbc.update(sql, vo.getUuid());
                logger.debug(String.format("deleted management node[uuid:%s, ip:%s]'s heartbeat from database, ret:%s", vo.getUuid(), vo.getHostName(), ret));
                return ret;
            }

            @AsyncThread
            private void nodeDie(ManagementNodeVO n) {
                logger.debug("Node " + n.getUuid() + " has gone because its heartbeat stopped");
                nodeLifeCycle.nodeLeft(ManagementNodeInventory.valueOf(n));

                ManagementNodeLifeCycleData d = new ManagementNodeLifeCycleData();
                d.setInventory(ManagementNodeInventory.valueOf(n));
                d.setNodeUuid(n.getUuid());
                d.setLifeCycle(LifeCycle.NodeLeft.toString());
                evtf.fire(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, d);
            }

            private void fenceSuspects() {
                for (ManagementNodeVO vo : suspects) {
                    ManagementNodeVO n = getNode(vo.getUuid());
                    if (n == null) {
                        continue;
                    }

                    long elapse = n.getHeartBeat().getTime() - vo.getHeartBeat().getTime();
                    if (elapse != 0) {
                        continue;
                    }

                    deleteNode(n);
                    nodeDie(n);
                }
            }

            private void updateHeartbeat() {
                lastHearbeatTime = getNodeHeartbeatTime(node().getUuid());
                String sql = "update ManagementNodeVO set heartBeat = NULL where uuid = ?";
                if (heartBeatDBSource.jdbc.update(sql, node().getUuid()) > 0) {
                    ManagementNodeVO n = getNode(node().getUuid());
                    if (n != null) {
                        node = n;
                    } else {
                        logger.warn(String.format("updateHeartbeat cannot find our record[uuid:%s] in database, we are deleted by other nodes", node().getUuid()));
                    }
                }
            }

            private Timestamp getNodeHeartbeatTime(String uuid) {
                try {
                    String sql = "select heartBeat from ManagementNodeVO where uuid = ?";
                    return heartBeatDBSource.jdbc.queryForObject(sql, new Object[]{uuid}, Timestamp.class);
                } catch (IncorrectResultSizeDataAccessException e) {
                    return null;
                }
            }

            private Timestamp getCurrentSqlTime() {
                return heartBeatDBSource.jdbc.queryForObject("select current_timestamp()", Timestamp.class);
            }

            private void checkAllNodesHealth() {
                String sql = "select * from ManagementNodeVO where state = 'RUNNING'";
                List<ManagementNodeVO> all = heartBeatDBSource.jdbc.query(sql, new BeanPropertyRowMapper(ManagementNodeVO.class));
                suspects.clear();

                // make sure the heartbeat is updated at least once during a failure period
                final long delta = TimeUnit.SECONDS.toMillis(
                        (long) (PortalGlobalProperty.MAX_HEARTBEAT_FAILURE + 1)
                                * ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class)
                );
                List<ManagementNodeVO> nodesInDb = new ArrayList<>();

                for (ManagementNodeVO vo : all) {
                    if (!StringDSL.isZStackUuid(vo.getUuid())) {
                        logger.warn(String.format("found a weird management node, it's UUID not a ZStack uuid, delete it. %s",
                                JSONObjectUtil.toJsonString(ManagementNodeInventory.valueOf(vo))));
                        deleteNode(vo);
                        continue;
                    }

                    nodesInDb.add(vo);

                    Timestamp curr = getCurrentSqlTime();
                    if (curr == null) {
                        throw new CloudRuntimeException("failed to get management node current heartbeat timestamp from database." +
                                " Returned timestamp is null");
                    }

                    if (vo.getUuid().equals(node().getUuid())) {
                        if (lastHearbeatTime != null && lastHearbeatTime.getTime() > curr.getTime()) {
                            new ManagementNodeTimeRegressionCanonicalEvent(vo.getUuid(), vo.getHostName()).fire();
                        }

                        continue;
                    }

                    Timestamp lastHeartbeat = vo.getHeartBeat();
                    if (lastHeartbeat.getTime() > curr.getTime()) {
                        new ManagementNodeTimeRegressionCanonicalEvent(vo.getUuid(), vo.getHostName()).fire();
                    }

                    if (Math.abs(lastHeartbeat.getTime() - curr.getTime()) > delta) {
                        suspects.add(vo);
                        logger.warn(String.format("management node[uuid:%s, hostname: %s]'s heart beat has stopped for %s secs, add it in suspicious list",
                                vo.getUuid(), vo.getHostName(), TimeUnit.MILLISECONDS.toSeconds(curr.getTime() - lastHeartbeat.getTime())));
                    }
                }

                Set<String> nodeUuidsInDb = nodesInDb.stream().map(ManagementNodeVO::getUuid).collect(Collectors.toSet());

                // When a node is dying, we may not receive the the dead notification because the message bus may be also dead
                // at that moment. By checking if the node UUID is still in our hash ring, we know what nodes should be kicked out
                destinationMaker.getManagementNodesInHashRing().forEach(nodeUuid -> {
                    if (!nodeUuidsInDb.contains(nodeUuid)) {
                        logger.warn(String.format("found that a management node[uuid:%s] had no heartbeat in database but still in our hash ring," +
                                "notify that it's dead", nodeUuid));
                        ManagementNodeInventory inv = new ManagementNodeInventory();
                        inv.setUuid(nodeUuid);
                        inv.setHostName(destinationMaker.getNodeInfo(nodeUuid).getNodeIP());

                        nodeLifeCycle.nodeLeft(inv);
                    }
                });

                // check if any node missing in our hash ring
                nodesInDb.forEach(n -> {
                    if (n.getUuid().equals(node().getUuid()) || suspects.contains(n)) {
                        return;
                    }

                    new Runnable() {
                        @Override
                        @AsyncThread
                        public void run() {
                            nodeLifeCycle.nodeJoin(ManagementNodeInventory.valueOf(n));
                        }
                    }.run();
                });
            }

            @Override
            public Void call() {
                logger.debug(String.format("heartbeat thread[%s] for management node[uuid:%s] started",
                        Thread.currentThread().getName(), node().getUuid()));
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        // the heartbeat task may be cancelled by the heartbeat interval change,
                        // just return, don't break, otherwise it stops the management node
                        logger.debug(String.format("heartbeat thread[%s] for management node[uuid:%s] is interrupted, exit heartbeat loop",
                                Thread.currentThread().getName(), node().getUuid()));
                        return null;
                    }

                    try {
                        if (!amIalive()) {
                            logger.warn(String.format("cannot find my[uuid:%s] heartbeat in database, quit process", node().getUuid()));
                            // this stops the management node
                            break;
                        }

                        if (logger.isTraceEnabled()) {
                            logger.trace("heartbeat is ticketing ...");
                        }

                        updateHeartbeat();
                        fenceSuspects();
                        checkAllNodesHealth();
                    } catch (Throwable t) {
                        if (handleHeartbeatFailure(t)) {
                            // this stops the management node
                            break;
                        }
                    }

                    sleepAHeartbeatInterval();
                }

                stop();
                return null;
            }

            private boolean sleepAHeartbeatInterval() {
                try {
                    TimeUnit.SECONDS.sleep(ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Long.class));
                    return true;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            private HeartBeatDBSource newHeartBeatDBSource() {
                if (heartBeatDBSource != null) {
                    heartBeatDBSource.destroy();
                }

                heartBeatDBSource = new HeartBeatDBSource();
                return heartBeatDBSource;
            }

            private boolean handleHeartbeatFailure(Throwable t) {
                logger.warn(String.format("we meet a database heartbeat failure caused by[%s], try to handle it", t.getMessage()));

                int heartbeatFailureTimes = 0;

                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        return false;
                    }

                    if (heartbeatFailureTimes > PortalGlobalProperty.MAX_HEARTBEAT_FAILURE) {
                        logger.warn(String.format("the heartbeat has failed %s times that is greater than the max allowed value[%s]," +
                                " quit process", heartbeatFailureTimes, PortalGlobalProperty.MAX_HEARTBEAT_FAILURE), t);
                        return true;
                    }

                    heartbeatFailureTimes ++;

                    boolean databaseError = false;
                    logger.warn(String.format("the database heartbeat has failed %s times, we will try to recover the connection %s times", heartbeatFailureTimes, PortalGlobalProperty.MAX_HEARTBEAT_FAILURE - heartbeatFailureTimes));

                    try {
                        heartBeatDBSource.jdbc.queryForObject("select 1", Integer.class);
                    } catch (Throwable t1) {
                        logger.warn(String.format("cannot communicate to the database, it's most likely the DB stopped or rebooted;" +
                                "try creating a new database connection. %s", t1.getMessage()), t1);
                        databaseError = true;
                    }

                    if (!databaseError) {
                        logger.debug("it seems the database connection failure recovers, continue heartbeat");
                        break;
                    }

                    try {
                        heartBeatDBSource = newHeartBeatDBSource();
                        logger.debug("the database heartbeat connection is successfully re-created");
                        break;
                    } catch (Throwable t1) {
                        logger.warn(String.format("unable to create a database connection, %s, will try it later", t1.getMessage()), t1);
                    }

                    sleepAHeartbeatInterval();
                }

                return false;
            }
        });

        logger.debug(String.format("started heartbeat thread for management node[uuid:%s]", Platform.getManagementServerId()));
    }

    @Deferred
    public boolean stop(boolean skipExit) {
        Platform.IS_RUNNING = false;

        if (stopped) {
            /* avoid repeated call from JVM shutdown hook, if process is exited from a former stop() call
             */
            return true;
        }

        // this makes sure the process exits even any exceptions happened during the stop process
        Defer.defer(() -> {
            if (CoreGlobalProperty.EXIT_JVM_ON_STOP) {
                new Runnable() {
                    @Override
                    @ExceptionSafe
                    public void run() {
                        logger.info("exitJVMOnStop is set to true, exit the JVM");
                    }
                }.run();

                if (skipExit) {
                    return;
                }

                System.exit(0);
            }
        });

        stopped = true;
        final Service self = this;
        logger.debug(String.format("start stopping the management node[uuid:%s]", node().getUuid()));

        class Stopper {
            private void stop() {
                stopApiOnCloudBus();
                stopApi();
                iAmDead();
                stopComponents();
                deleteNode();
                notifyOtherNodes();
                unregisterCloudBus();
                stopCloudBus();
                quitLoop();
                stopThreadFacade();
            }

            @ExceptionSafe
            private void stopThreadFacade() {
                thdf.stop();
            }

            @ExceptionSafe
            private void quitLoop() {
                notifyStop();
            }

            @ExceptionSafe
            private void stopCloudBus() {
                bus.stop();
            }

            @ExceptionSafe
            private void unregisterCloudBus() {
                bus.unregisterService(self);
            }

            @ExceptionSafe
            private void notifyOtherNodes() {
                ManagementNodeLifeCycleData d = new ManagementNodeLifeCycleData();
                d.setNodeUuid(node().getUuid());
                d.setLifeCycle(LifeCycle.NodeLeft.toString());
                d.setInventory(ManagementNodeInventory.valueOf(node()));
                evtf.fire(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH, d);
            }

            @ExceptionSafe
            private void deleteNode() {
                dbf.removeByPrimaryKey(Platform.getManagementServerId(), ManagementNodeVO.class);
            }

            @ExceptionSafe
            private void iAmDead() {
                nodeLifeCycle.iAmDead(ManagementNodeInventory.valueOf(node()));
            }

            @ExceptionSafe
            private void stopApiOnCloudBus() {
                bus.unregisterService(apim);
            }

            @ExceptionSafe
            private void stopApi() {
                apim.stop();
            }
        }

        new Stopper().stop();

        logger.info("Management node: " + getId() + " exits successfully");

        return true;
    }

    @Override
    public boolean stop() {
        return stop(false);
    }

    @AsyncThread
    private void startInThread() {
        try {
            start();
            isNodeRunning = NODE_RUNNING;
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            isNodeRunning = NODE_FAILED;
        }
    }

    @Override
    public void startNode() {
        startInThread();
        while (isNodeRunning == NODE_STARTING) {
            logger.debug("management node is still initializing ...");
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CloudRuntimeException(e);
            }
        }

        if (isNodeRunning == NODE_FAILED) {
            logger.debug("error happened when starting node, stop the management node now");
            stop();
            throw new CloudRuntimeException("failed to start management node");
        }
    }

    @Override
    public void quit(String reason) {
        new BootErrorLog().write(reason);
        logger.debug(String.format("stopping the management node because %s", reason));
        stop();
    }
}
