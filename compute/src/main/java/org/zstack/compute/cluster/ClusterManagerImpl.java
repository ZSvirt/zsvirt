package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.cluster.arch.ClusterResourceConfigInitializer;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.defer.Deferred;
import org.zstack.header.AbstractService;
import org.zstack.header.cluster.*;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.tag.TagManager;
import org.zstack.utils.ObjectUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.FieldPrinter;
import org.zstack.utils.logging.CLogger;

import java.util.*;

public class ClusterManagerImpl extends AbstractService implements ClusterManager {
    private static final CLogger logger = Utils.getLogger(ClusterManager.class);
    private static final FieldPrinter printer = Utils.getFieldPrinter();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ClusterResourceConfigInitializer crci;

    private Map<String, ClusterFactory> clusterFactories = Collections.synchronizedMap(new HashMap<>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<>();

    static {
        allowedMessageAfterSoftDeletion.add(ClusterDeletionMsg.class);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateClusterMsg) {
            handle((APICreateClusterMsg) msg);
        } else if (msg instanceof ClusterMessage) {
            passThrough((ClusterMessage) msg);
        }
    }

    private void passThrough(ClusterMessage msg) {
        ClusterVO vo = dbf.findByUuid(msg.getClusterUuid(), ClusterVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(msg.getClass())) {
            ClusterEO eo = dbf.findByUuid(msg.getClusterUuid(), ClusterEO.class);
            vo = ObjectUtils.newAndCopy(eo, ClusterVO.class);
        }

        if (vo == null) {
            String err = String.format("Cannot find cluster: %s, it may have been deleted", msg.getClusterUuid());
            bus.replyErrorByMessageType((Message) msg, err);
            return;
        }
        
        ClusterFactory factory = this.getClusterFactory(ClusterType.valueOf(vo.getType()));
        Cluster cluster = factory.getCluster(vo);
        cluster.handleMessage((Message) msg);
    }


    private void doCreateCluster(CreateClusterMessage msg, ReturnValueCompletion<ClusterInventory> completion) {
        String clusterType = msg.getType();
        if (clusterType == null) {
            clusterType = BaseClusterFactory.type.toString();
        }

        ClusterFactory factory = this.getClusterFactory(ClusterType.valueOf(clusterType));
        ClusterVO vo = new ClusterVO();

        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }

        vo.setArchitecture(msg.getArchitecture());
        vo.setDescription(msg.getDescription());
        vo.setHypervisorType(msg.getHypervisorType());
        vo.setManagementNodeId(Platform.getManagementServerId());
        vo.setZoneUuid(msg.getZoneUuid());
        vo.setState(ClusterState.Enabled);
        vo.setName(msg.getClusterName());
        vo = factory.createCluster(vo, msg);

        if (msg instanceof APICreateMessage) {
            tagMgr.createTagsFromAPICreateMessage((APICreateMessage)msg, vo.getUuid(), ClusterVO.class.getSimpleName());
        } else if (msg instanceof NeedReplyMessage) {
            NeedReplyMessage m = (NeedReplyMessage) msg;
            tagMgr.createTags(m.getSystemTags(), m.getUserTags(), vo.getUuid(), ClusterVO.class.getSimpleName());
        }

        ClusterInventory inv = ClusterInventory.valueOf(vo);

        crci.initClusterResourceConfigValue(inv);
        logger.debug(String.format("Created new cluster: %s", printer.print(inv)));
        completion.success(inv);
    }

    private void handle(CreateClusterMsg msg) {
        CreateClusterReply reply = new CreateClusterReply();

        doCreateCluster(msg, new ReturnValueCompletion<ClusterInventory>(msg) {
            @Override
            public void success(ClusterInventory inv) {
                reply.setInventory(inv);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Deferred
    private void handle(APICreateClusterMsg msg) {
        APICreateClusterEvent evt = new APICreateClusterEvent(msg.getId());

        doCreateCluster(msg, new ReturnValueCompletion<ClusterInventory>(msg) {
            @Override
            public void success(ClusterInventory inv) {
                evt.setInventory(inv);
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else if (msg instanceof CreateClusterMsg) {
            handle((CreateClusterMsg) msg);
        } else if (msg instanceof ClusterMessage) {
            passThrough((ClusterMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ClusterConstant.SERVICE_ID);
    }

    private void populateClusterFactories() {
        for (ClusterFactory ext : pluginRgty.getExtensionList(ClusterFactory.class)) {
            ClusterFactory old = clusterFactories.get(ext.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ClusterFactory[%s, %s] for type[%s]",
                        old.getClass().getName(), ext.getClass().getName(), ext.getType()));
            }
            clusterFactories.put(ext.getType().toString(), ext);
        }
    }
       
    @Override
    public boolean start() {
        populateClusterFactories();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private ClusterFactory getClusterFactory(ClusterType type) {
        ClusterFactory factory = clusterFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No ClusterFactory of type[%s] found", type));
        }
        return factory;
    }
}
