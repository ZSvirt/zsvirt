package org.zstack.vrouterRoute;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmSyncConfigToHaGroupExtensionPoint;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.service.VirtualRouterHaGroupExtensionPoint;
import org.zstack.header.vrouterRoute.DeleteVRouterEntryInnerMsg;
import org.zstack.header.vrouterRoute.DeleteVRouterEntryInnerReply;
import org.zstack.header.vrouterRoute.SyncVRouterEntryFromRouteTableMsg;
import org.zstack.header.vrouterRoute.SyncVRouterEntryFromRouteTableRely;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.*;

/**
 * Created by weiwang on 15/06/2017.
 */
public class VRouterRouteManagerImpl extends AbstractService implements VRouterRouteManager, ApplianceVmSyncConfigToHaGroupExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VRouterRouteManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;

    private Map<String, VRouterRouteBackend> backends = new HashMap<>();
    private Map<String, RouteTableServiceFactory> factories = new HashMap<>();

    private void populateExtensions() {
        for (VRouterRouteBackend bkd : pluginRgty.getExtensionList(VRouterRouteBackend.class)) {
            VRouterRouteBackend old = backends.get(bkd.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VRouterRouteBackend[%s, %s] for the network service provider" +
                        " type[%s]", old.getClass(), bkd.getClass(), bkd.getNetworkServiceProviderType()));
            }

            backends.put(bkd.getNetworkServiceProviderType(), bkd);
        }

        for (RouteTableServiceFactory f : pluginRgty.getExtensionList(RouteTableServiceFactory.class)) {
            RouteTableServiceFactory old = factories.get(f.getApplianceVmType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate RouteTableServiceFactories[%s, %s] for the vrouter type[]%s",
                        old.getClass(), f.getClass(), f.getApplianceVmType()));
            }

            factories.put(f.getApplianceVmType(), f);
        }
    }

    private Integer DEFAULT_ROUTE_DISTANCE = 128;
    private static Map<String, VRouterRouteEntryType> ROUTE_TYPES = ImmutableMap.of(
            "S", VRouterRouteEntryType.UserStatic,
            "C", VRouterRouteEntryType.DirectConnect,
            "K", VRouterRouteEntryType.ZStack,
            "O", VRouterRouteEntryType.OSPF);

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }


    private void handleLocalMessage(Message msg) {
        if (msg instanceof VRouterRouteEntryDeletionMsg) {
            handle((VRouterRouteEntryDeletionMsg) msg);
        } else if (msg instanceof VRouterRouteTableDeletionMsg) {
            handle((VRouterRouteTableDeletionMsg) msg);
        } else if (msg instanceof SyncVRouterEntryFromRouteTableMsg) {
            handle((SyncVRouterEntryFromRouteTableMsg) msg);
        } else if (msg instanceof DeleteVRouterEntryInnerMsg) {
            handle((DeleteVRouterEntryInnerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APIAddVRouterRouteEntryMsg) {
            handle((APIAddVRouterRouteEntryMsg) msg);
        } else if (msg instanceof APIAttachVRouterRouteTableToVRouterMsg) {
            handle((APIAttachVRouterRouteTableToVRouterMsg) msg);
        } else if (msg instanceof APICreateVRouterRouteTableMsg) {
            handle((APICreateVRouterRouteTableMsg) msg);
        } else if (msg instanceof APIDeleteVRouterRouteEntryMsg) {
            handle((APIDeleteVRouterRouteEntryMsg) msg);
        } else if (msg instanceof APIDeleteVRouterRouteTableMsg) {
            handle((APIDeleteVRouterRouteTableMsg) msg);
        } else if (msg instanceof APIDetachVRouterRouteTableFromVRouterMsg) {
            handle((APIDetachVRouterRouteTableFromVRouterMsg) msg);
        } else if (msg instanceof APIGetVRouterRouteTableMsg) {
            handle((APIGetVRouterRouteTableMsg) msg);
        } else if (msg instanceof APIUpdateVRouterRouteTableMsg) {
            handle((APIUpdateVRouterRouteTableMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final DeleteVRouterEntryInnerMsg msg) {
        DeleteVRouterEntryInnerReply reply = new DeleteVRouterEntryInnerReply();
        VRouterRouteEntryVO vo = dbf.findByUuid(msg.getUuid(), VRouterRouteEntryVO.class);
        String tableUuid = vo.getRouteTableUuid();
        dbf.remove(vo);
        syncRouteTable(tableUuid);
        bus.reply(msg, reply);
    }

    private void handle(final SyncVRouterEntryFromRouteTableMsg msg) {
        SyncVRouterEntryFromRouteTableRely reply = new SyncVRouterEntryFromRouteTableRely();
        syncRouteTable(msg.getUuid());
        bus.reply(msg, reply);
    }

    private VRouterRouteBackend getBackendFromRouteTable(String tableUuid) {
        List<String> vrUuids = getVrUuidsFromTableUuid(tableUuid);

        VRouterRouteBackend bkd = getBackendFromVRouter(vrUuids.get(0));
        return bkd;
    }

    private VRouterRouteBackend getBackendFromVRouter(String vrouterVmUuid) {
        VirtualRouterVmVO vo = Q.New(VirtualRouterVmVO.class).eq(VirtualRouterVmVO_.uuid, vrouterVmUuid).find();

        if (vo == null) {
            throw new OperationFailureException(argerr("virtual router[uuid:%s] can not find", vrouterVmUuid));
        }

        RouteTableServiceFactory f = factories.get(vo.getApplianceVmType());
        if (f == null) {
            throw new OperationFailureException(operr("can not find service factory for virtual router type[%s]", vo.getApplianceVmType()));
        }

        return backends.get(f.getProviderTypeFromVRouter(vrouterVmUuid));
    }

    protected void handle(VRouterRouteEntryDeletionMsg msg) {
        VRouterRouteEntryDeletionReply reply = new VRouterRouteEntryDeletionReply();
        VRouterRouteEntryVO vo = Q.New(VRouterRouteEntryVO.class)
                .eq(VRouterRouteEntryVO_.uuid, msg.getUuid())
                .find();
        dbf.remove(vo);
        syncRouteTable(vo.getRouteTableUuid());
        bus.reply(msg, reply);
    }

    protected void handle(VRouterRouteTableDeletionMsg msg) {
        VRouterRouteTableDeletionReply reply = new VRouterRouteTableDeletionReply();

        VRouterRouteTableVO vo = dbf.findByUuid(msg.getUuid(), VRouterRouteTableVO.class);

        List<String> vrUuids = getVrUuidsFromTableUuid(msg.getUuid());
        dbf.remove(vo);
        if (vrUuids == null || vrUuids.isEmpty()) {
            logger.debug(String.format("there are no vr attached to route table [uuid: %s]", msg.getUuid()));
            bus.reply(msg, reply);
            return;
        }

        vrUuids.forEach( vr -> detachRouterTableFromVRouter(msg.getUuid(), vr));
        syncRouteTable(null, vrUuids);
        bus.reply(msg, reply);
    }

    protected void syncRouteTable(String tableUuid, List<String> vrouterUuids) {
        VRouterRouteBackend bkd = getBackendFromVRouter(vrouterUuids.get(0));
        List<VRouterRouteEntryTo> tos = new ArrayList<>();

        if (tableUuid == null) {
            // Note(WeiW): This must be detach the table from vrouter
            bkd.syncRouteEntries(tos, vrouterUuids, true);
            return;
        } else {
            List<VRouterRouteEntryVO> entryVOS = Q.New(VRouterRouteEntryVO.class).eq(VRouterRouteEntryVO_.routeTableUuid, tableUuid).list();
            if (entryVOS.isEmpty()) {
                return;
            }

            tos = entryVOS.stream().map(entryVO -> VRouterRouteEntryTo.valueOf(entryVO)).collect(Collectors.toList());
            bkd.syncRouteEntries(tos, vrouterUuids, true);
        }
    }

    protected void syncRouteTable(String tableUuid) {
        List<String> vrUuids = getVrUuidsFromTableUuid(tableUuid);
        if (vrUuids.isEmpty()) {
            logger.debug(String.format("there are no vr attached to route table [uuid: %s]", tableUuid));
            return;
        }

        VRouterRouteBackend bkd = getBackendFromRouteTable(tableUuid);

        List<VRouterRouteEntryVO> entryVOS = Q.New(VRouterRouteEntryVO.class).eq(VRouterRouteEntryVO_.routeTableUuid, tableUuid).list();
        List<VRouterRouteEntryTo> tos = entryVOS.stream().map(entryVO -> VRouterRouteEntryTo.valueOf(entryVO)).collect(Collectors.toList());

        bkd.syncRouteEntries(tos, vrUuids, true);
    }

    protected void handle(APIAddVRouterRouteEntryMsg msg) {
        APIAddVRouterRouteEntryEvent evt = new APIAddVRouterRouteEntryEvent(msg.getId());

        VRouterRouteEntryVO vo = new VRouterRouteEntryVO();
        vo.setUuid(msg.getResourceUuid() != null? msg.getResourceUuid(): Platform.getUuid());
        vo.setDestination(msg.getDestination());
        vo.setTarget(msg.getTarget());
        vo.setDescription(msg.getDescription());
        vo.setRouteTableUuid(msg.getRouteTableUuid());
        vo.setType(VRouterRouteEntryType.valueOf(msg.getType()));
        vo.setDistance(msg.getDistance() != null? msg.getDistance(): DEFAULT_ROUTE_DISTANCE);
        vo = dbf.persistAndRefresh(vo);

        syncRouteTable(msg.getRouteTableUuid());

        // Note(WeiW): We don't care weather the route actually add to vrouter, since it has been
        // add to db, we will assurance it finally will be add (eventual consistency).
        logger.debug(String.format("successfully add route [uuid:%s, routeTableUuid]: \n%s",
                        vo.getUuid(), vo.getRouteTableUuid(), VRouterRouteEntryTo.valueOf(vo)));
        evt.setInventory(VRouterRouteEntryInventory.valueOf(vo));
        bus.publish(evt);
    }

    protected void handle(APIAttachVRouterRouteTableToVRouterMsg msg) {
        APIAttachVRouterRouteTableToVRouterEvent event = new APIAttachVRouterRouteTableToVRouterEvent(msg.getId());

        attachRouterTableToVRouter(msg.getRouteTableUuid(), msg.getVirtualRouterVmUuid());

        VRouterRouteTableVO tvo = Q.New(VRouterRouteTableVO.class)
                .eq(VRouterRouteTableVO_.uuid, msg.getRouteTableUuid()).find();

        syncRouteTable(msg.getRouteTableUuid(), asList(msg.getVirtualRouterVmUuid()));
        event.setInventory(VRouterRouteTableInventory.valueOf(tvo));
        bus.publish(event);
    }

    protected void handle(APICreateVRouterRouteTableMsg msg) {
        APICreateVRouterRouteTableEvent evt = new APICreateVRouterRouteTableEvent(msg.getId());

        VRouterRouteTableVO vo = new VRouterRouteTableVO();
        vo.setUuid(msg.getResourceUuid() != null? msg.getResourceUuid(): Platform.getUuid());
        vo.setName(msg.getName());
        vo.setType("User");
        vo.setDescription(msg.getDescription());

        vo = dbf.persistAndRefresh(vo);
        tagMgr.createTagsFromAPICreateMessage(msg, vo.getUuid(), VRouterRouteTableVO.class.getSimpleName());
        evt.setInventory(VRouterRouteTableInventory.valueOf(dbf.reload(vo)));
        bus.publish(evt);
    }

    protected void handle(APIDeleteVRouterRouteEntryMsg msg) {
        APIDeleteVRouterRouteEntryEvent evt = new APIDeleteVRouterRouteEntryEvent(msg.getId());

        DeleteVRouterEntryInnerMsg deimsg = new DeleteVRouterEntryInnerMsg();
        deimsg.setUuid(msg.getUuid());

        bus.makeTargetServiceIdByResourceUuid(deimsg, VRouterRouteConstants.SERVICE_ID, msg.getUuid());
        bus.send(deimsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug(String.format("successfully deleted route [uuid:%s, routeTableUuid:%s]",
                            msg.getUuid(), msg.getRouteTableUuid()));
                    VRouterRouteTableVO vrtvo = Q.New(VRouterRouteTableVO.class)
                            .eq(VRouterRouteTableVO_.uuid, msg.getRouteTableUuid()).find();
                    evt.setInventory(VRouterRouteTableInventory.valueOf(vrtvo));
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    protected void handle(APIDeleteVRouterRouteTableMsg msg) {
        APIDeleteVRouterRouteTableEvent evt = new APIDeleteVRouterRouteTableEvent(msg.getId());

        VRouterRouteTableVO vo = Q.New(VRouterRouteTableVO.class)
                .eq(VRouterRouteTableVO_.uuid, msg.getUuid())
                .find();

        final String issuer = VRouterRouteTableVO.class.getSimpleName();
        final List<VRouterRouteTableInventory> ctx = asList(VRouterRouteTableInventory.valueOf(vo));
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-route-table-%s", vo.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-route-table-permissive-check";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-route-table-permissive-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "delete-route-table-force-delete";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                                @Override
                                public void success() {
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });
                }

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                        bus.publish(evt);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        evt.setError(err(SysErrors.DELETE_RESOURCE_ERROR, errCode.getDetails()).withCause(errCode));
                        bus.publish(evt);
                    }
                });
            }
        }).start();
    }

    protected void handle(APIDetachVRouterRouteTableFromVRouterMsg msg) {
        APIDetachVRouterRouteTableFromVRouterEvent event = new APIDetachVRouterRouteTableFromVRouterEvent(msg.getId());

        detachRouterTableFromVRouter(msg.getRouteTableUuid(), msg.getVirtualRouterVmUuid());

        VRouterRouteTableVO tvo = Q.New(VRouterRouteTableVO.class)
                .eq(VRouterRouteTableVO_.uuid, msg.getRouteTableUuid()).find();

        syncRouteTable(null, asList(msg.getVirtualRouterVmUuid()));
        event.setInventory(VRouterRouteTableInventory.valueOf(tvo));
        bus.publish(event);
    }

    protected void handle(APIGetVRouterRouteTableMsg msg) {
        APIGetVRouterRouteTableReply reply = new APIGetVRouterRouteTableReply();
        VRouterRouteBackend bkd = getBackendFromVRouter(msg.getVirtualRouterVmUuid());
        bkd.getVRouterRouteTable(msg.getVirtualRouterVmUuid(), new ReturnValueCompletion<String>(null) {
            @Override
            public void success(String returnValue) {
                List<VRouterRouteEntryAO> result = parseRawRoutes(returnValue);
                reply.setInventories(result);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });

    }

    private void handle(APIUpdateVRouterRouteTableMsg msg) {
        APIUpdateVRouterRouteTableEvent evt = new APIUpdateVRouterRouteTableEvent(msg.getId());
        VRouterRouteTableVO vo = dbf.findByUuid(msg.getUuid(), VRouterRouteTableVO.class);

        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the route table [uuid:%s]", msg.getUuid()));
        }

        boolean update = false;

        if (msg.getName() != null) {
            vo.setName(msg.getName());
            update = true;
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            update = true;
        }

        if (update) {
            vo = dbf.updateAndRefresh(vo);
        }

        evt.setInventory(VRouterRouteTableInventory.valueOf(vo));
        bus.publish(evt);
    }

    private static List<VRouterRouteEntryAO> parseRawRoutes(String raw) {
        List<VRouterRouteEntryAO> result = new ArrayList<>();
        String destination = new String();
        VRouterRouteEntryType type = null;
        String distance = new String();
        for (String route : raw.split("\n")) {
            VRouterRouteEntryAO entry = new VRouterRouteEntryAO();
            String target = new String();

            Pattern patternDestination = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\/\\d{1,2}");
            Matcher matcherDestination = patternDestination.matcher(route);
            if (matcherDestination.find()) {
                destination = matcherDestination.group(0).trim();
                type = ROUTE_TYPES.get(route.substring(0, 1));
            }
            if (route.matches(".*Null0.*")) {
                type = VRouterRouteEntryType.UserBlackHole;
            } else if (route.matches(".*0\\.0\\.0\\.0\\/0.*")) {
                type = VRouterRouteEntryType.ZStack;
            }

            if (type == null) {
                type = VRouterRouteEntryType.Unknown;
                logger.debug("unknown route type");
            }

            Pattern patternDistance = Pattern.compile("\\[\\d{1,3}\\/\\d\\]");
            Matcher matcherDistance = patternDistance.matcher(route);
            if (matcherDistance.find()) {
                distance = matcherDistance.group(0).split("\\/")[0].substring(1);
            }

            Pattern patternTarget = Pattern.compile("via\\ \\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
            Matcher matcherTarget = patternTarget.matcher(route);
            if (matcherTarget.find()) {
                target = matcherTarget.group(0).split(" ")[1];
                entry.setTarget(target);
            }
            Pattern patternRecursive = Pattern.compile(".*recursive\\ via\\ \\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*");
            Matcher matcherRecursive= patternRecursive.matcher(route);
            if (matcherRecursive.find()) {
                target = matcherRecursive.group(0).split(" ")[2];
            }
            if (target.isEmpty()) {
                patternTarget = Pattern.compile("lo|eth\\d|Null0");
                matcherTarget = patternTarget.matcher(route);
                if (matcherTarget.find()) {
                    entry.setTarget(matcherTarget.group(0));
                }
            }

            entry.setDestination(destination);
            entry.setType(type);
            entry.setDistance(Integer.parseInt(distance));

            if (route.matches(".*\\*.*")) {
                entry.setStatus("active");
            } else {
                entry.setStatus("inactive");
            }

            if (entry.getType().equals(VRouterRouteEntryType.UserStatic) ||
                    entry.getType().equals(VRouterRouteEntryType.UserBlackHole)) {
                VRouterRouteEntryVO vo = Q.New(VRouterRouteEntryVO.class)
                        .eq(VRouterRouteEntryVO_.destination, destination)
                        .eq(VRouterRouteEntryVO_.type, type)
                        .eq(VRouterRouteEntryVO_.target, target)
                        .eq(VRouterRouteEntryVO_.distance, distance)
                        .find();
                if (vo != null) {
                    entry.setUuid(vo.getUuid());
                    entry.setDescription(vo.getDescription());
                }
            } else if (entry.getType().equals(VRouterRouteEntryType.DirectConnect)) {
                entry.setDistance(null);
            }
            result.add(entry);
        }
        return result;
    }


    @Override
    public void attachRouterTableToVRouter(String tableUuid, String vrUuid) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            VirtualRouterVRouterRouteTableRefVO vo = new VirtualRouterVRouterRouteTableRefVO();
            vo.setRouteTableUuid(tableUuid);
            vo.setVirtualRouterVmUuid(vrUuid);
            dbf.persistAndRefresh(vo);
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.attachNetworkServiceToHaRouter(VRouterRouteTableVO.class.getSimpleName(),asList(tableUuid), vrUuid);
            }
        }
    }

    @Override
    public void detachRouterTableFromVRouter(String tableUuid, String vrUuid) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            SQL.New(VirtualRouterVRouterRouteTableRefVO.class)
                    .eq(VirtualRouterVRouterRouteTableRefVO_.routeTableUuid, tableUuid)
                    .eq(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid, vrUuid).delete();
        } else {
            for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
                ext.detachNetworkServiceFromHaRouter(VRouterRouteTableVO.class.getSimpleName(),asList(tableUuid), vrUuid);
            }
        }
    }

    @Override
    public List<String> getVrUuidsFromTableUuid(String tableUuid) {
        List<String> res = new ArrayList<>();
        List<String> vrUuids = Q.New(VirtualRouterVRouterRouteTableRefVO.class)
                .select(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid)
                .eq(VirtualRouterVRouterRouteTableRefVO_.routeTableUuid, tableUuid)
                .listValues();
        if (!vrUuids.isEmpty()) {
            res.addAll(vrUuids);
        }

        List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
        if (exps != null && !exps.isEmpty()) {
            vrUuids = exps.get(0).getHaVrUuidsFromNetworkService(VRouterRouteTableVO.class.getSimpleName(), tableUuid);
        }

        if (!vrUuids.isEmpty()) {
            res.addAll(vrUuids);
        }

        return res;
    }

    @Override
    public List<String> getTableUuidsFromVrUuid(String vrUuid) {
        VirtualRouterVmVO vrVo = dbf.findByUuid(vrUuid, VirtualRouterVmVO.class);
        if (!vrVo.isHaEnabled()) {
            return Q.New(VirtualRouterVRouterRouteTableRefVO.class)
                    .select(VirtualRouterVRouterRouteTableRefVO_.routeTableUuid)
                    .eq(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid, vrUuid).listValues();
        } else {
            List<VirtualRouterHaGroupExtensionPoint> exps = pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class);
            if (exps != null && !exps.isEmpty()) {
                return exps.get(0).getNetworkServicesFromHaVrUuid(VRouterRouteTableVO.class.getSimpleName(), vrUuid);
            } else {
                return new ArrayList<>();
            }
        }
    }

    @Override
    public void applianceVmSyncConfigToHa(ApplianceVmInventory inv, String haUuid) {
        List<String> tables = Q.New(VirtualRouterVRouterRouteTableRefVO.class)
                .select(VirtualRouterVRouterRouteTableRefVO_.routeTableUuid)
                .eq(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid, inv.getUuid()).listValues();
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            ext.attachNetworkServiceToHaRouter(VRouterRouteTableVO.class.getSimpleName(), tables, inv.getUuid());
        }
    }

    @Override
    public void applianceVmSyncConfigToHaRollback(ApplianceVmInventory inv, String haUuid) {
        List<String> tables = Q.New(VirtualRouterVRouterRouteTableRefVO.class)
                .select(VirtualRouterVRouterRouteTableRefVO_.routeTableUuid)
                .eq(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid, inv.getUuid()).listValues();
        for (VirtualRouterHaGroupExtensionPoint ext : pluginRgty.getExtensionList(VirtualRouterHaGroupExtensionPoint.class)) {
            ext.detachNetworkServiceFromHaRouter(VRouterRouteTableVO.class.getSimpleName(), tables, inv.getUuid());
        }
    }

    @Override
    public void applianceVmSyncConfigAfterAddToHaGroup(ApplianceVmInventory inv, String haUuid, NoErrorCompletion completion) {
        SQL.New(VirtualRouterVRouterRouteTableRefVO.class).eq(VirtualRouterVRouterRouteTableRefVO_.virtualRouterVmUuid, inv.getUuid()).delete();
        completion.done();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VRouterRouteConstants.SERVICE_ID);
    }
}
