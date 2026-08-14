package org.zstack.cloudformation;

import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.accessKey.AccessKeyVO;
import org.zstack.accessKey.AccessKeyVO_;
import org.zstack.cloudformation.template.CloudFormationCreator;
import org.zstack.cloudformation.template.CloudFormationDecoder;
import org.zstack.cloudformation.template.decoder.DecoderUtils;
import org.zstack.cloudformation.template.decoder.ResourceDecoder;
import org.zstack.cloudformation.template.struct.ResourceStruct;
import org.zstack.cloudformation.template.struct.ResourceType;
import org.zstack.cloudformation.template.struct.*;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.cloudformation.PreviewResourceStruct;
import org.zstack.header.cloudformation.ResourceStackInventory;
import org.zstack.header.cloudformation.StackParameters;
import org.zstack.header.cloudformation.SupportedResourceStruct;
import org.zstack.header.cloudformation.*;
import org.zstack.header.cloudformation.monitor.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.service.NetworkServiceProviderVO;
import org.zstack.header.network.service.NetworkServiceProviderVO_;
import org.zstack.header.query.QueryBelongFilter;
import org.zstack.identity.AccountManager;
import org.zstack.network.l3.AttachNetworkServiceToL3Msg;
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg;
import org.zstack.network.securitygroup.SecurityGroupVO_;
import org.zstack.query.QueryUtils;
import org.zstack.sdk.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Tuple;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by mingjian.deng on 2018/6/5.
 */
public class CloudFormationManagerImpl extends AbstractService implements CloudFormationExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint, QueryBelongFilter {
    private static final CLogger logger = Utils.getLogger(CloudFormationManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    protected CascadeFacade casf;
    @Autowired
    private AccountManager acmgr;
    @Autowired
    private PluginRegistry pluginRgty;



    protected Future<Void> vmPortMonitor;


    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddStackTemplateMsg) {
            handle((APIAddStackTemplateMsg) msg);
        } else if (msg instanceof APIUpdateStackTemplateMsg) {
            handle((APIUpdateStackTemplateMsg) msg);
        } else if (msg instanceof APIDeleteStackTemplateMsg) {
            handle((APIDeleteStackTemplateMsg) msg);
        } else if (msg instanceof APIPreviewResourceStackMsg) {
            handle((APIPreviewResourceStackMsg) msg);
        } else if (msg instanceof APICreateResourceStackMsg) {
            handle((APICreateResourceStackMsg) msg);
        } else if (msg instanceof APIDeleteResourceStackMsg) {
            handle((APIDeleteResourceStackMsg) msg);
        } else if (msg instanceof APIUpdateResourceStackMsg) {
            handle((APIUpdateResourceStackMsg) msg);
        } else if (msg instanceof APIGetResourceFromResourceStackMsg) {
            handle((APIGetResourceFromResourceStackMsg) msg);
        } else if (msg instanceof APIGetResourceStackFromResourceMsg) {
            handle((APIGetResourceStackFromResourceMsg) msg);
        } else if (msg instanceof APICheckStackTemplateParametersMsg) {
            handle((APICheckStackTemplateParametersMsg) msg);
        } else if (msg instanceof APIRestartResourceStackMsg) {
            handle((APIRestartResourceStackMsg) msg);
        } else if (msg instanceof APIGetSupportedCloudFormationResourcesMsg) {
            handle((APIGetSupportedCloudFormationResourcesMsg) msg);
        } else if (msg instanceof APIDecodeStackTemplateMsg) {
            handle((APIDecodeStackTemplateMsg) msg);
        } else if (msg instanceof APIGetResourceStackVmStatusMsg) {
            handle((APIGetResourceStackVmStatusMsg) msg);
        } else if (msg instanceof APIAddResourceStackVmPortMonitorMsg) {
            handle((APIAddResourceStackVmPortMonitorMsg) msg);
        } else if (msg instanceof APIDeleteResourceStackVmPortMonitorMsg) {
            handle((APIDeleteResourceStackVmPortMonitorMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateStackResourceMsg) {
            handle((CreateStackResourceMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final CreateStackResourceMsg msg) {
        CreateStackResourceReply sreply = new CreateStackResourceReply();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-resourcestack-%s", msg.getUuid()));

        chain.then(new ShareFlow() {
            String contentJson = msg.getTemplateContent();
            String paramsJson = msg.getParameters();
            String preParamsJson = msg.getPreParameters();
            CfnResults result;
            ResourceStackVO rvo = new ResourceStackVO();

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "parse template to results";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloudFormationDecoder decoder = new CloudFormationDecoder();
                        result = decoder.decodeFromContent(contentJson, paramsJson, preParamsJson, true);

                        rvo.setUuid(msg.getUuid());
                        rvo.setAccountUuid(msg.getSession().getAccountUuid());
                        rvo.setDescription(msg.getDescription());
                        rvo.setName(msg.getName());
                        rvo.setEnableRollback(msg.getRollback() == null || msg.getRollback());
                        rvo.setTemplateContent(contentJson);
                        if (paramsJson != null) {
                            rvo.setParamContent(paramsJson);
                        }
                        rvo.setVersion(result.getTemplateVersion());
                        rvo.setType(msg.getType());
                        rvo.setStatus(ResourceStackStatus.Initial);
                        rvo = dbf.persistAndRefresh(rvo);
                        for (ClousFormationTemplateExtensionPoint exp: pluginRgty.getExtensionList(ClousFormationTemplateExtensionPoint.class)) {
                            exp.afterCreateResourceStack(ResourceStackInventory.valueOf(rvo), msg.getSource());
                        }

                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "start create stack resources";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        thdf.chainSubmit(new ChainTask(msg) {
                            @Override
                            public String getSyncSignature() {
                                return "create-resource-stack";
                            }

                            @Override
                            public void run(SyncTaskChain chain) {
                                data.put("startCreate", true);
                                createStack(msg.getSession(), msg.getTimeout(), rvo.getUuid(), result, new ReturnValueCompletion<ResourceStackInventory>(chain) {
                                    @Override
                                    public void success(ResourceStackInventory inventory) {
                                        sreply.setInventory(inventory);
                                        trigger.next();
                                        chain.next();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        trigger.fail(errorCode);
                                        chain.next();
                                    }
                                });
                            }

                            @Override
                            public String getName() {
                                return getSyncSignature();
                            }
                        });
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, sreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                Object startCreate = data.get("startCreate");
                if (startCreate == null) {
                    ResourceStackVO rvo = dbf.findByUuid(msg.getUuid(), ResourceStackVO.class);
                    if (rvo != null) {
                        rvo.setStatus(ResourceStackStatus.Failed);
                        rvo.setReason(errCode.getDetails());
                        dbf.updateAndRefresh(rvo);
                    }
                }
                sreply.setError(errCode);
                bus.reply(msg, sreply);
            }
        }).start();
    }

    private static List<String> getResourcesFromAction(String action) {
        String msg = DecoderUtils.getMsgFromResourceType(action, "Action");
        return DecoderUtils.getResourcesTypeInMsg(msg);
    }

    private static List<String> getResourcesFromResource(String resource) {
        String msg = DecoderUtils.getMsgFromResourceType(resource, "Resource");
        return DecoderUtils.getResourcesTypeInMsg(msg);
    }

    private static List<SupportedResourceStruct> getSupportedResources(ResourceStackVersion version, String type) {
        List<SupportedResourceStruct> resources = new ArrayList<>();
        for(String action: CloudFormationConstants.supportedActions) {
            SupportedResourceStruct s = new SupportedResourceStruct();
            s.setName(action);
            s.setType("Action");
            s.setActionName(action + "Action");
            s.setResources(getResourcesFromAction(action));
            resources.add(s);
        }

        for(String resource: CloudFormationConstants.supportedResources) {
            SupportedResourceStruct s = new SupportedResourceStruct();
            s.setName(resource);
            s.setType("Resource");
            s.setActionName(DecoderUtils.getCreateAction(resource));
            s.setResources(getResourcesFromResource(resource));
            resources.add(s);
        }

        return resources;
    }

    private void handle(final APIGetSupportedCloudFormationResourcesMsg msg) {
        APIGetSupportedCloudFormationResourcesReply reply = new APIGetSupportedCloudFormationResourcesReply();
        reply.setResources(getSupportedResources(ResourceStackVersion.valueOf(msg.getVersion()), msg.getType()));
        bus.reply(msg, reply);
    }

    private void handle(final APIDeleteResourceStackVmPortMonitorMsg msg) {
        APIDeleteResourceStackVmPortMonitorEvent evt = new APIDeleteResourceStackVmPortMonitorEvent(msg.getId());
        UpdateQuery sql = SQL.New(ResourceStackVmPortRefVO.class).eq(ResourceStackVmPortRefVO_.vmInstanceUuid, msg.getVmInstanceUuid());
        if (msg.getPort() != null) {
            sql.eq(ResourceStackVmPortRefVO_.port, msg.getPort());
        }
        if (msg.getStackUuid() != null) {
            sql.eq(ResourceStackVmPortRefVO_.stackUuid, msg.getStackUuid());
        }
        sql.hardDelete();

        bus.publish(evt);
    }

    private void handle(final APIAddResourceStackVmPortMonitorMsg msg) {
        APIAddResourceStackVmPortMonitorEvent evt = new APIAddResourceStackVmPortMonitorEvent(msg.getId());
        String stackUuid = msg.getStackUuid();
        if (msg.getStackUuid() == null) {
            stackUuid = Q.New(CloudFormationStackResourceRefVO.class).select(CloudFormationStackResourceRefVO_.stackUuid).eq(CloudFormationStackResourceRefVO_.resourceUuid, msg.getVmInstanceUuid()).findValue();
        }
        if (stackUuid == null) {
            evt.setError(operr("no stackUuid found for the vmInstance[%s]", msg.getVmInstanceUuid()));
            bus.publish(evt);
            return;
        }
        if(Q.New(ResourceStackVmPortRefVO.class).eq(ResourceStackVmPortRefVO_.vmInstanceUuid, msg.getVmInstanceUuid()).
                eq(ResourceStackVmPortRefVO_.stackUuid, stackUuid).eq(ResourceStackVmPortRefVO_.port, msg.getPort()).isExists()) {
            bus.publish(evt);
            return;
        }

        ResourceStackVmPortRefVO ref = new ResourceStackVmPortRefVO();
        ref.setPort(msg.getPort());
        ref.setStackUuid(stackUuid);
        ref.setVmInstanceUuid(msg.getVmInstanceUuid());
        ref.setStatus("initialize");

        dbf.persistAndRefresh(ref);

        bus.publish(evt);
    }

    private void handle(final APIGetResourceStackVmStatusMsg msg) {
        APIGetResourceStackVmStatusReply reply = new APIGetResourceStackVmStatusReply();
        Map<String, Map<String, String>> status = new HashMap<>();
        List<ResourceStackVmPortRefVO> refs = Q.New(ResourceStackVmPortRefVO.class).eq(ResourceStackVmPortRefVO_.stackUuid, msg.getUuid()).list();
        for (ResourceStackVmPortRefVO ref: refs) {
            Map<String, String> portStatus = status.get(ref.getVmInstanceUuid()) == null ? new HashMap<>() : status.get(ref.getVmInstanceUuid());
            portStatus.put(String.valueOf(ref.getPort()), ref.getStatus());
            status.put(ref.getVmInstanceUuid(), portStatus);
        }
        reply.setPortStatus(status);

        bus.reply(msg, reply);
    }

    private String getTemplateContent(final APICheckStackTemplateParametersMsg msg) {
        if (msg.getTemplateContent() != null) {
            return msg.getTemplateContent();
        } else {
            StackTemplateVO vo = dbf.findByUuid(msg.getUuid(), StackTemplateVO.class);
            DebugUtils.Assert(vo != null, String.format("cannot find StackTemplateVO [%s] here!", msg.getUuid()));
            return vo.getContent();
        }
    }

    private List<StackParameters> getStackParameters(String content) {
        List<StackParameters> parameters = new ArrayList<>();

        CloudFormationDecoder decoder = new CloudFormationDecoder();
        CfnResults result = decoder.decodeFromContent(content, null, false);
        ResourceDecoder rDecoder = new ResourceDecoder();
        Map<String, String> resourceTypes = rDecoder.getResourceParametersType(new JsonParser().parse(content));

        result.getParams().forEach(p -> {
            StackParameters param = new StackParameters();
            param.setConstraintDescription(p.getConstraintDescription());
            param.setDescription(p.getDescription());
            param.setLabel(p.getLabel());
            param.setNoEcho(p.getNoEcho());
            param.setType(p.getType());
            if (p.getDefaultValue() != null) {
                param.setDefaultValue(p.getDefaultValue().toString());
            }
            param.setParamName(p.getParamName());
            param.setResourceType(resourceTypes.get(p.getParamName()));
            parameters.add(param);
        });

        return parameters;
    }

    private List<StackParameters> getStackPreParameters(String content) {
        List<StackParameters> parameters = new ArrayList<>();

        CloudFormationDecoder decoder = new CloudFormationDecoder();
        CfnResults result = decoder.decodeFromContent(content, null, false);
        ResourceDecoder rDecoder = new ResourceDecoder();
        Map<String, String> resourceTypes = rDecoder.getResourceParametersType(new JsonParser().parse(content));

        result.getPreparams().forEach(p -> {
            StackParameters param = new StackParameters();
            param.setConstraintDescription(p.getConstraintDescription());
            param.setDescription(p.getDescription());
            param.setLabel(p.getLabel());
            param.setNoEcho(p.getNoEcho());
            param.setType(p.getType());
            if (p.getDefaultValue() != null) {
                param.setDefaultValue(p.getDefaultValue().toString());
            }
            param.setParamName(p.getParamName());
            param.setResourceType(resourceTypes.get(p.getParamName()));
            parameters.add(param);
        });

        return parameters;
    }

    private void handle(final APICheckStackTemplateParametersMsg msg) {
        APICheckStackTemplateParametersReply reply = new APICheckStackTemplateParametersReply();
        String content = getTemplateContent(msg);

        reply.setParameters(getStackParameters(content));
        reply.setPreparameters(getStackPreParameters(content));

        bus.reply(msg, reply);
    }

    private String getResourceFromInventory(String inventory) {
        if (inventory.endsWith("Inventory")) {
            return inventory.replace("Inventory", "");
        } else {
            return inventory;
        }
    }

    private void restartStack(final APIRestartResourceStackMsg msg, ReturnValueCompletion<ResourceStackInventory> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("restart resource stack");
        chain.then(new ShareFlow() {
            boolean startCreate = false;
            CfnResults result;
            ResourceStackVO vo = dbf.findByUuid(msg.getUuid(), ResourceStackVO.class);
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "parse template to results";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloudFormationDecoder decoder = new CloudFormationDecoder();
                        String preparams = fetchPreParams(msg.getUuid());
                        result = decoder.decodeFromContent(vo.getTemplateContent(), vo.getParamContent(), preparams, true);
                        vo.setStatus(ResourceStackStatus.Creating);
                        vo = dbf.updateAndRefresh(vo);

                        trigger.next();
                    }
                });

                flow(new Flow() {
                    String __name__ = "create actions";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloudFormationCreator creator = new CloudFormationCreator();
                        StackData stackData = getStackData(msg.getSession(), msg.getTimeout(), vo);
                        startCreate = true;
                        CfnActions actions = creator.createResource(result, stackData);

                        for (ResultStruct result: actions.getResults()) {
                            if (CloudFormationConstants.notInRefResources.contains(result.getResourceType())) {
                                continue;
                            }
                            CloudFormationStackResourceRefVO ref = new CloudFormationStackResourceRefVO();
                            ref.setReserve(result.isReserve());
                            ref.setResourceType(result.getResourceType());
                            ref.setResourceUuid(result.getResourceUuid());
                            ref.setResourceName(result.getResourceName());
                            ref.setStackUuid(vo.getUuid());
                            ref.setRound(result.getRound());
                            dbf.persistAndRefresh(ref);
                        }

                        if (actions.getErrCode().isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(actions.getErrCode().getErrorCode());
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        vo.setStatus(ResourceStackStatus.Created);
                        vo = dbf.updateAndRefresh(vo);

                        completion.success(vo.toInventory());
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (!startCreate) {
                            if (vo.getStatus() != null) {
                                vo.setStatus(ResourceStackStatus.Failed);
                                vo.setReason(errCode.getDetails());
                                vo = dbf.updateAndRefresh(vo);
                            }
                            completion.fail(errCode);
                            return;
                        }

                        vo.setReason(errCode.getDetails());
                        if (vo.isEnableRollback()) {
                            vo.setStatus(ResourceStackStatus.Rollbacked);
                            vo = dbf.updateAndRefresh(vo);
                        } else {
                            vo.setStatus(ResourceStackStatus.Failed);
                            vo = dbf.updateAndRefresh(vo);
                        }
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APIRestartResourceStackMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("restart-resource-stack-%s", msg.getUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                APIRestartResourceStackEvent evt = new APIRestartResourceStackEvent(msg.getId());
                restartStack(msg, new ReturnValueCompletion<ResourceStackInventory>(chain) {
                    @Override
                    public void success(ResourceStackInventory inventory) {
                        evt.setInventory(inventory);
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(final APIGetResourceStackFromResourceMsg msg) {
        APIGetResourceStackFromResourceReply reply = new APIGetResourceStackFromResourceReply();
        Map<String, String> stack = new HashMap<>();
        CloudFormationStackResourceRefVO ref = Q.New(CloudFormationStackResourceRefVO.class).eq(CloudFormationStackResourceRefVO_.resourceUuid, msg.getResourceUuid()).find();
        if (ref != null) {
            stack.put("stackUuid", ref.getStackUuid());
        }
        for (ClousFormationTemplateExtensionPoint exp: pluginRgty.getExtensionList(ClousFormationTemplateExtensionPoint.class)) {
            exp.afterGetResourceStackFromResource(stack);
        }
        reply.setStack(stack);
        bus.reply(msg, reply);
    }

    private void handle(final APIGetResourceFromResourceStackMsg msg) {
        APIGetResourceFromResourceStackReply reply = new APIGetResourceFromResourceStackReply();
        List<CloudFormationStackResourceRefVO> refs = Q.New(CloudFormationStackResourceRefVO.class).eq(CloudFormationStackResourceRefVO_.stackUuid, msg.getUuid()).list();
        try {
            for (CloudFormationStackResourceRefVO ref: refs) {
                String uuid = ref.getResourceUuid();
                Class<?> inventoryClass = Class.forName(ref.getResourceType());
                Class<?> entityClass = QueryUtils.getEntityClassFromInventoryClass(inventoryClass);
                Method inventoryValueOf = inventoryClass.getMethod("valueOf", entityClass);
                Object entity = dbf.findByUuid(uuid, entityClass);
                if (entity == null) {
                    continue;
                }
                Object inventory = inventoryValueOf.invoke(inventoryClass, entity);
                Map<String, Object> r = new HashMap<>();
                r.put(getResourceFromInventory(inventoryClass.getSimpleName()), inventory);
                reply.getResources().add(r);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.warn(e.getMessage());
            reply.setError(operr(e.getMessage()));
        }

        bus.reply(msg, reply);
    }

    private void handle(final APIUpdateResourceStackMsg msg) {
        APIUpdateResourceStackEvent evt = new APIUpdateResourceStackEvent(msg.getId());
        ResourceStackVO vo = dbf.findByUuid(msg.getUuid(), ResourceStackVO.class);
        if (vo == null) {
            evt.setError(operr("ResourceStackVO: [%s] has been deleted...", msg.getUuid()));
            bus.publish(evt);
            return;
        }
        if (msg.getParameters() != null) {
            vo.setParamContent(msg.getParameters());
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }
        if (msg.getTemplateContent() != null) {
            vo.setTemplateContent(msg.getTemplateContent());
        }

        if (msg.getRollback() != null) {
            vo.setEnableRollback(msg.getRollback());
        }
        vo = dbf.updateAndRefresh(vo);
        evt.setInventory(vo.toInventory());
        bus.publish(evt);
    }

    private void deleteStack(String uuid, SessionInventory session, final Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-resource-stack-%s", uuid));
        ResourceStackVO vo = dbf.findByUuid(uuid, ResourceStackVO.class);
        if (vo == null) {
            completion.fail(operr("ResourceStackVO [%s] already been deleted!", uuid));
            return;
        }
        vo.setStatus(ResourceStackStatus.Deleting);
        vo = dbf.updateAndRefresh(vo);
        final List<ResourceStackInventory> ctx = Collections.singletonList(ResourceStackInventory.valueOf(vo));

        chain.then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, ResourceStackVO.class.getSimpleName(),
                        ctx, new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            @Override
            public void run(final FlowTrigger trigger, Map data) {
                CascadeAction action = new CFCascadeAction().
                        setRootIssuer(ResourceStackVO.class.getSimpleName()).
                        setRootIssuerContext(ctx).
                        setParentIssuer(ResourceStackVO.class.getSimpleName()).
                        setParentIssuerContext(ctx).
                        setActionCode(CascadeConstant.DELETION_DELETE_CODE).
                        setFullTraverse(true);
                ((CFCascadeAction)action).setSession(session);
                casf.asyncCascade(action, new Completion(trigger) {
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
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                casf.asyncCascade(CascadeConstant.DELETION_CLEANUP_CODE, ResourceStackVO.class.getSimpleName(),
                        ctx, new Completion(trigger) {
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
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(final APIDeleteResourceStackMsg msg) {
        APIDeleteResourceStackEvent evt = new APIDeleteResourceStackEvent(msg.getId());
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                deleteStack(msg.getUuid(), msg.getSession(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-daho-cloud-connection-%s", msg.getUuid());
            }
        });
    }

    private StackData getStackData(SessionInventory session, long timeout, ResourceStackVO vo) {
        StackData data = new StackData();
        AccountVO account = dbf.findByUuid(session.getAccountUuid(), AccountVO.class);
        data.setAccountUuid(account.getUuid());
        data.setAccountName(account.getName());
        data.setStackUuid(vo.getUuid());
        data.setStackName(vo.getName());
        data.setRollback(vo.isEnableRollback());
        data.setTimeout(timeout);
        Tuple tuple = Q.New(AccessKeyVO.class).select(AccessKeyVO_.AccessKeyID, AccessKeyVO_.AccessKeySecret)
                .eq(AccessKeyVO_.uuid, session.getUuid()).findTuple();
        if (tuple == null) {
            data.setSessionUuid(session.getUuid());
            return data;
        }
        data.setAccessKeyId((String) tuple.get(0));
        data.setAccessKeySecret((String) tuple.get(1));
        return data;
    }

    private void updateCloudFormationStackResourceRef(List<ResultStruct> results, String stackUuid) {
        for (ResultStruct result: results) {
            if (CloudFormationConstants.notInRefResources.contains(result.getResourceType())) {
                continue;
            }
            if (!Q.New(CloudFormationStackResourceRefVO.class).eq(CloudFormationStackResourceRefVO_.resourceUuid, result.getResourceUuid())
                    .eq(CloudFormationStackResourceRefVO_.stackUuid, stackUuid).isExists()) {
                CloudFormationStackResourceRefVO ref = new CloudFormationStackResourceRefVO();
                ref.setReserve(result.isReserve());
                ref.setResourceType(result.getResourceType());
                ref.setResourceUuid(result.getResourceUuid());
                ref.setResourceName(result.getResourceName());
                ref.setStackUuid(stackUuid);
                ref.setRound(result.getRound());
                dbf.persistAndRefresh(ref);
            }
        }
    }

    private void createStack(final SessionInventory session, long timeout, String uuid, CfnResults result, final ReturnValueCompletion<ResourceStackInventory> completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("start create resource stack");
        chain.then(new ShareFlow() {
            ResourceStackVO rvo = dbf.findByUuid(uuid, ResourceStackVO.class);
            @Override
            public void setup() {
//                flow(new NoRollbackFlow() {
//                    String __name__ = "check quota and capacities";
//                    @Override
//                    public void run(FlowTrigger trigger, Map data) {
//                        trigger.next();
//                    }
//                });

                flow(new NoRollbackFlow() {
                    String __name__ = "create actions";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        CloudFormationCreator creator = new CloudFormationCreator();
                        rvo.setStatus(ResourceStackStatus.Creating);
                        rvo = dbf.updateAndRefresh(rvo);
                        StackData stackData = getStackData(session, timeout, rvo);
                        CfnActions actions = creator.createResource(result, stackData);

                        if (!actions.getErrCode().isSuccess() && rvo.isEnableRollback()) {
                            trigger.fail(actions.getErrCode().getErrorCode());
                            return;
                        }

                        updateCloudFormationStackResourceRef(actions.getResults(), rvo.getUuid());

                        if (actions.getErrCode().isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(actions.getErrCode().getErrorCode());
                        }
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        rvo.setStatus(ResourceStackStatus.Created);
                        rvo.setOutputs(JSONObjectUtil.toJsonString(result.getOutputs()));
                        rvo = dbf.updateAndRefresh(rvo);
                        completion.success(rvo.toInventory());
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        rvo.setReason(errCode.getDetails());
                        if (rvo.isEnableRollback()) {
                            rvo.setStatus(ResourceStackStatus.Rollbacked);
                            rvo = dbf.updateAndRefresh(rvo);
                        } else {
                            rvo.setStatus(ResourceStackStatus.Failed);
                            rvo = dbf.updateAndRefresh(rvo);
                        }
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void handle(final APICreateResourceStackMsg msg) {
        APICreateResourceStackEvent evt = new APICreateResourceStackEvent(msg.getId());
        CreateStackResourceMsg cmsg = new CreateStackResourceMsg();
        if (msg.getTemplateContent() != null) {
            cmsg.setTemplateContent(msg.getTemplateContent());
        } else if (msg.getTemplateUuid() != null) {
            StackTemplateVO template = dbf.findByUuid(msg.getTemplateUuid(), StackTemplateVO.class);
            if (!template.getState()) {
                evt.setError(operr("template [%s] chosen is disabled", template.getUuid()));
                bus.publish(evt);
                return;
            }
            cmsg.setTemplateContent(template.getContent());
        } else {
            evt.setError(operr("templateContent must be set!"));
            bus.publish(evt);
            return;
        }
        cmsg.setParameters(msg.getParameters());
        cmsg.setTimeout(msg.getTimeout());
        cmsg.setSession(msg.getSession());
        cmsg.setRollback(msg.getRollback());
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setType(msg.getType());
        cmsg.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : getUuid());
        bus.makeLocalServiceId(cmsg, CloudFormationConstant.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    CreateStackResourceReply r = reply.castReply();
                    evt.setInventory(r.getInventory());
                } else {
                    evt.setError(reply.getError());
                }
                bus.publish(evt);
            }
        });

    }

    private String getContentFromTemplate(String templateUuid) {
        StackTemplateVO vo = dbf.findByUuid(templateUuid, StackTemplateVO.class);
        DebugUtils.Assert(vo != null, String.format("cannot find StackTemplateVO [%s] here!", templateUuid));
        return vo.getContent();
    }

    private String fetchPreParams(String stackUuid) {
        if (stackUuid != null) {
            ResourceStackVO stack = dbf.findByUuid(stackUuid, ResourceStackVO.class);
            if (stack == null) {
                return null;
            }
            for (ClousFormationTemplateExtensionPoint exp: pluginRgty.getExtensionList(ClousFormationTemplateExtensionPoint.class)) {
                String preparams = exp.getPreParameters(ResourceStackInventory.valueOf(stack));
                if (preparams != null) {
                    return preparams;
                }
            }
        }
        return null;
    }

    private void handle(final APIDecodeStackTemplateMsg msg) {
        APIDecodeStackTemplateReply reply = new APIDecodeStackTemplateReply();
        CloudFormationDecoder decoder = new CloudFormationDecoder();
        String content = msg.getTemplateContent() == null ? getContentFromTemplate(msg.getUuid()) : msg.getTemplateContent();
        String params = msg.getParameters();

        CfnResults result = decoder.decodeFromContent(content, params, msg.getPreparameters(), true);

        result.getResources().forEach(resource -> reply.getResources().add(resource));
        reply.setResources(result.getResources().stream().
                sorted(Comparator.comparing(ResourceStruct::getType)).collect(Collectors.toList()));

        List<StackParameters> parameters = getStackParameters(content);
        Map<String, String> parameterMaps = new HashMap<>();
        parameters.stream().filter(p -> p.getResourceType() != null).forEach(p -> {
            ResourceStruct r = new ResourceStruct();
            r.setResourceType(p.getResourceType());
            r.setResourceName(p.getParamName());
            r.setCreated(true);
            r.setType(ResourceType.Resource);
            Map<String, Object> properties = new HashMap<>();
            result.getParams().forEach(t -> {
                if (t.getParamName().equals(p.getParamName())) {
                    properties.put("uuid", t.getValue());
                    if (t.getValue() instanceof List) {
                        List<String> tmp = (List)t.getValue();
                        Collections.sort(tmp);
                        parameterMaps.put(tmp.toString(), t.getParamName());
                    } else {
                        parameterMaps.put(t.getValue().toString(), t.getParamName());
                    }
                }
            });
            if (properties.isEmpty()) {
                throw new OperationFailureException(operr("cannot find parameters for %s, which is %s type, please check parameters",
                        p.getParamName(), p.getResourceType()));
            }
            r.setProperties(properties);
            reply.getResources().add(r);
        });

        for (ResourceStruct resource: reply.getResources()) {
            if (resource.isCreated()) {
                continue;
            }

            // add indegree from input paramters which is belong to zstack resource
            for (Map.Entry<String, Object> property: resource.getProperties().entrySet()) {
                String value;
                if (property.getValue() instanceof List) {
                    List<String> tmp = getValueFromList((List)property.getValue());
                    value = tmp.toString();
                } else {
                    value = property.getValue().toString();
                }
                String tmp = parameterMaps.get(value);
                if (tmp != null && !resource.getInDegree().contains(tmp)) {
                    resource.getInDegree().add(tmp);
                }
            }
        }

        bus.reply(msg, reply);
    }

    private List<String> getValueFromList(List property) {
        Object[] tmp = property.toArray();
        List<String> tmp1 = new ArrayList<>();
        for (Object o: tmp) {
            if (o instanceof List) {
                tmp1.addAll(getValueFromList((List)o));
            } else {
                tmp1.add(o.toString());
            }
        }
        Collections.sort(tmp1);
        return tmp1;
    }

    private void handle(final APIPreviewResourceStackMsg msg) {
        APIPreviewResourceStackReply reply = new APIPreviewResourceStackReply();
        PreviewResourceStruct restruct = new PreviewResourceStruct();
        CloudFormationDecoder decoder = new CloudFormationDecoder();
        String content = msg.getTemplateContent() == null ? getContentFromTemplate(msg.getUuid()) : msg.getTemplateContent();

        CfnResults result = decoder.decodeFromContent(content, msg.getParameters(), msg.getPreParameters(), true);

        restruct.setConditions(result.getConditionParams());

        CloudFormationCreator creator = new CloudFormationCreator();
        CfnActions actions = creator.dryRun(result);

        actions.getActions().forEach(action -> {
            restruct.getActions().add(action);
        });

        reply.setPreview(restruct);
        bus.reply(msg, reply);
    }

    private void handle(final APIDeleteStackTemplateMsg msg) {
        APIDeleteStackTemplateEvent evt = new APIDeleteStackTemplateEvent(msg.getId());
        StackTemplateVO vo = dbf.findByUuid(msg.getUuid(), StackTemplateVO.class);
        if (vo == null) {
            evt.setError(operr("StackTemplateVO has been deleted..."));
            bus.publish(evt);
            return;
        }

        List<String> tags = CloudFormationSystemTags.SYSTEM_TEMPLATE.getTags(msg.getUuid(), StackTemplateVO.class);
        if (tags != null && tags.contains(CloudFormationSystemTags.SYSTEM_TEMPLATE_TOKEN)) {
            evt.setError(operr("cannot delete or update system template: %s", vo.getName()));
            bus.publish(evt);
            return;
        }

        dbf.removeByPrimaryKey(vo.getUuid(), StackTemplateVO.class);
        bus.publish(evt);
    }

    private String getContentFromMsg(final APIAddStackTemplateMsg msg) {
        if (msg.getTemplateContent() != null) {
            return msg.getTemplateContent();
        } else if (msg.getUrl() != null) {
            //TODO: must set!
            return null;
        } else {
            throw new OperationFailureException(operr("content must be set by templateContent or url!"));
        }
    }

    private void handle(final APIAddStackTemplateMsg msg) {
        APIAddStackTemplateEvent evt = new APIAddStackTemplateEvent(msg.getId());
        StackTemplateVO vo = new StackTemplateVO();
        vo.setContent(msg.getTemplateContent());
        vo.setUuid(Platform.getUuid());
        vo.setName(msg.getName());
        vo.setType(msg.getType());
        vo.setDescription(msg.getDescription());
        vo.setMd5sum(StringDSL.getMd5Sum(msg.getTemplateContent()));
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        CloudFormationDecoder decoder = new CloudFormationDecoder();
        String content = getContentFromMsg(msg);
        if (content == null) {
            evt.setError(operr("get null content input"));
            bus.publish(evt);
            return;
        }
        CfnResults result = decoder.decodeFromContent(content, null, null, false);
        ResourceStackVersion version = ResourceStackVersion.get(result.getTemplateVersion());
        if (version == null) {
            evt.setError(operr("invalid cloudformation template version: %s", result.getTemplateVersion()));
            bus.publish(evt);
            return;
        }
        vo.setVersion(version.toString());
        dbf.persistAndRefresh(vo);
        evt.setInventory(vo.toInventory());
        bus.publish(evt);
    }

    private void handle(final APIUpdateStackTemplateMsg msg) {
        APIUpdateStackTemplateEvent evt = new APIUpdateStackTemplateEvent(msg.getId());
        StackTemplateVO vo = dbf.findByUuid(msg.getUuid(), StackTemplateVO.class);
        if (vo == null) {
            evt.setError(operr("StackTemplateVO: [%s] has been deleted...", msg.getUuid()));
            bus.publish(evt);
            return;
        }

        if (msg.getState() != null) {
            vo.setState(msg.getState());
        }
        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }
        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }
        if (msg.getTemplateContent() != null) {
            vo.setContent(msg.getTemplateContent());
            vo.setMd5sum(StringDSL.getMd5Sum(msg.getTemplateContent()));
        }
        vo = dbf.updateAndRefresh(vo);
        evt.setInventory(vo.toInventory());
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(CloudFormationConstant.SERVICE_ID);
    }

    private synchronized void startVmPortMonitor() {
        int interval = CloudFormationGlobalConfig.VM_PORT_CHECK_INTERVAL.value(Integer.class);
        if (vmPortMonitor != null) {
            vmPortMonitor.cancel(true);
        }
        if (interval <= 0) {
            logger.debug(String.format("%s disabled because interval set to %s secs",
                    this.getClass().getSimpleName(), interval));
            return;
        }

        logger.debug(String.format("%s starts with the interval %s secs", this.getClass().getSimpleName(), interval));
        vmPortMonitor = thdf.submitPeriodicTask(new VmPortMonitorTask(interval));
    }

    private void startMonitor() {
        CloudFormationGlobalConfig.VM_PORT_CHECK_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> {
            startVmPortMonitor();
        });
        startVmPortMonitor();
    }

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.UNIT_TEST_ON || Platform.isSimulatorOn()) {
            ZSClient.configure(
                    new ZSConfig.Builder().setPort(Platform.getManagementNodeServicePort()).setContextPath("zstack")
                            .setDefaultPollingInterval(100, TimeUnit.MILLISECONDS)
                            .setDefaultPollingTimeout(TimeUnit.MINUTES.toMillis(15), TimeUnit.MILLISECONDS)
                            .setReadTimeout(10, TimeUnit.MINUTES)
                            .setWriteTimeout(10, TimeUnit.MINUTES)
                            .build()
            );
            configSystemTemplate();
        }
        startMonitor();
        return true;
    }

    private void configSystemTemplate() {
        List<String> systemTemplates = PathUtil.scanFolderOnClassPath(CloudFormationConstant.systemTemplateFolder);
        List<String> exists = new ArrayList<>();
        for (String template: systemTemplates) {
            String name = PathUtil.fileName(template);
            if (name.endsWith(".json")) {
                name = name.substring(0, name.lastIndexOf(".json"));
            }
            File templateFile = new File(template);
            try {
                String content = FileUtils.readFileToString(templateFile);
                CloudFormationUtils.validateTemplate(content, false);
                createSystemTemplate(name, content);
                exists.add(name);
            } catch (IOException e) {
                logger.warn(String.format("read system template file failed, due to: %s", e.getMessage()));
            }
        }
        String sql = "select t.uuid from StackTemplateVO t, SystemTagVO s where s.resourceUuid=t.uuid and  t.name not in (:name)";
        List<String> dels = SQL.New(sql).param("name", exists).list();
        if (!dels.isEmpty()) {
            logger.debug(String.format("delete the drop templates: %s", dels.toString()));
        }
        List<String> delsInValid = SQL.New("select distinct(s.uuid) from StackTemplateVO s, SystemTagVO t where s.name like 'ZStack.System.v%' and s.uuid not in (select resourceUuid from SystemTagVO)").list();
        if (!delsInValid.isEmpty()) {
            // see ZSTAC-25138
            logger.debug(String.format("delete the invalid templates: %s", delsInValid.toString()));
            dels.addAll(delsInValid);
        }
        if (!dels.isEmpty()) {
            sql = "delete from StackTemplateVO where uuid in (:uuids)";
            SQL.New(sql).param("uuids", dels).execute();
        }
    }

    private AccountResourceRefVO getSharedVO(String resourceUuid) {
        AccountResourceRefVO ref = new AccountResourceRefVO();
        ref.setResourceUuid(resourceUuid);
        ref.setResourceType(StackTemplateVO.class.getSimpleName());
        ref.setType(AccessLevel.SharePublic);
        return ref;
    }

    private void createSystemTag(String stackUuid) {
        SystemTagCreator creator = CloudFormationSystemTags.SYSTEM_TEMPLATE.newSystemTagCreator(stackUuid);
        creator.inherent = true;
        creator.recreate = false;
        creator.create();
    }

    private void createSystemTemplate(String name, String content) {
        StackTemplateVO old = Q.New(StackTemplateVO.class).eq(StackTemplateVO_.name, name).find();
        if (old != null) {
            boolean exists = Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.resourceUuid, old.getUuid())
                    .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                    .isExists();
            if (!exists) {
                // fix bug and compatible with old versions: ZSTAC-13371
                AccountResourceRefVO ref = getSharedVO(old.getUuid());
                dbf.persistAndRefresh(ref);
            }
            if (!CloudFormationSystemTags.SYSTEM_TEMPLATE.hasTag(old.getUuid())) {
                createSystemTag(old.getUuid());
            }
            return;
        }

        logger.info(String.format("found new cloudformation system template: [%s], start to add it.", name));
        StackTemplateVO vo = new StackTemplateVO();
        vo.setContent(content);
        vo.setUuid(Platform.getUuid());
        vo.setName(name);
        vo.setType("zstack");
        vo.setMd5sum(StringDSL.getMd5Sum(content));
        vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);

        CloudFormationDecoder decoder = new CloudFormationDecoder();
        CfnResults result = decoder.decodeFromContent(content, null, false);
        ResourceStackVersion version = ResourceStackVersion.get(result.getTemplateVersion());
        if (version == null) {
            logger.warn(String.format("invalid cloudformation template version: %s", result.getTemplateVersion()));
            return;
        }
        vo.setVersion(version.toString());
        vo.setDescription(result.getDescription());


        // share to all users
        AccountResourceRefVO ref = getSharedVO(vo.getUuid());

        new SQLBatch() {
            @Override
            protected void scripts() {
                // double check, to make sure the name is uniq
                if (sql("select vo from StackTemplateVO vo where vo.name = :name").param("name", name).list().size() <= 0) {
                    dbf.persistAndRefresh(vo);
                }

                boolean exists = q(AccountResourceRefVO.class)
                        .eq(AccountResourceRefVO_.resourceUuid, ref.getResourceUuid())
                        .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                        .isExists();
                if (!exists) {
                    dbf.persist(ref);
                }
            }
        }.execute();

        // create system tag
        createSystemTag(vo.getUuid());
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void beforeCloudFormationAction(Object action) {
        if (action instanceof AddSecurityGroupRuleAction) {
            beforeHook((AddSecurityGroupRuleAction) action);
        }
    }

    private void beforeHook(AddSecurityGroupRuleAction create) {
        List rules = create.rules;
        if (rules == null) {
            return;
        }
        for (Object rule: rules) {
            Map r = (HashMap)rule;
            APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO ao = new APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO();
            for (Field f: ao.getClass().getDeclaredFields()) {
                String v = f.getName();
                f.setAccessible(true);
                if (r.get(v) != null) {
                    if (Integer.class.isAssignableFrom(f.getType()) || Integer.TYPE.isAssignableFrom(f.getType())) {
                        r.put(v, Integer.valueOf(r.get(v).toString()));
                    } else if (Long.class.isAssignableFrom(f.getType()) || Long.TYPE.isAssignableFrom(f.getType())) {
                        r.put(v, Long.valueOf(r.get(v).toString()));
                    } else {
                        r.put(v, r.get(v).toString());
                    }
                }
            }
        }
    }

    @Override
    public void afterCloudFormationAction(Object action, Object result, StackData stackData) {
        if (action instanceof CreateL3NetworkAction) {
            // we attach network services for user, with indicated system tags
            afterHook((CreateL3NetworkAction)action, (CreateL3NetworkResult)result);
        }
        CloudFormationUtils.attachSystemTagToResources(action, result, CloudFormationSystemTags.CREATE_BY_CLOUDFORMATION);
    }

    private void afterHook(CreateL3NetworkAction create, CreateL3NetworkResult l3) {
        if (create.systemTags == null || create.systemTags.size() == 0) {
            return;
        }

        String sUuid = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.type, "SecurityGroup").select(SecurityGroupVO_.uuid).findValue();
        String fUuid = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.type, "Flat").select(SecurityGroupVO_.uuid).findValue();
        String vUuid = Q.New(NetworkServiceProviderVO.class).eq(NetworkServiceProviderVO_.type, "vrouter").select(SecurityGroupVO_.uuid).findValue();

        AttachNetworkServiceToL3Msg amsg = new AttachNetworkServiceToL3Msg();
        Map<String, List<String>> servicesMap = new HashMap<>();
        amsg.setL3NetworkUuid(l3.inventory.uuid);

        if (create.systemTags.contains("networkservices::Flat")) {
            // flat network
            servicesMap.put(fUuid, list("VipQos", "DNS", "HostRoute", "Userdata", "Eip", "DHCP"));
            servicesMap.put(sUuid, list("SecurityGroup"));
        } else if (create.systemTags.contains("networkservices::VRouter")) {
            // vpc network
            servicesMap.put(vUuid, list("IPsec", "VRouterRoute", "CentralizedDNS",
                    "VipQos", "SNAT", "LoadBalancer", "PortForwarding", "Eip", "DNS"));
            servicesMap.put(fUuid,  list("DHCP", "Userdata"));
            servicesMap.put(sUuid, list("SecurityGroup"));
        } else if (create.systemTags.contains("networkservices::Public")) {
            // public network
            servicesMap.put(fUuid,  list("DHCP", "Userdata"));
            servicesMap.put(sUuid, list("SecurityGroup"));
        } else if (create.systemTags.contains("networkservices::Private")) {
            // private network for vrouter
            servicesMap.put(vUuid, list("IPsec", "VRouterRoute", "CentralizedDNS", "VipQos",
                    "SNAT", "LoadBalancer", "PortForwarding", "Eip", "DNS"));
            servicesMap.put(fUuid,  list("DHCP", "Userdata"));
            servicesMap.put(sUuid, list("SecurityGroup"));
        }

        if (create.systemTags.contains("networkservices::NoDHCP")) {
            // remove DHCP service
            servicesMap.get(fUuid).remove("DHCP");
        }
        amsg.setNetworkServices(servicesMap);
        bus.makeTargetServiceIdByResourceUuid(amsg, L3NetworkConstant.SERVICE_ID, amsg.getL3NetworkUuid());
        bus.send(amsg);
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        CloudFormationStackResourceRefVO refvo = Q.New(CloudFormationStackResourceRefVO.class).eq(CloudFormationStackResourceRefVO_.resourceUuid, ref.getResourceUuid()).find();
        if (refvo == null) {
            return;
        }
        String accountUuid = acmgr.getOwnerAccountUuidOfResource(refvo.getStackUuid());
        if (!accountUuid.equals(newOwnerUuid)) {
            // if now owner is not stack owner, remove it.
            logger.debug(String.format("Change owner from %s to %s, resource [%s] quit from stack [%s]",
                    accountUuid, newOwnerUuid, ref.getResourceUuid(), refvo.getStackUuid()));
            dbf.remove(refvo);
        }
    }

    @Override
    public String filterName() {
        return "cloudformation";
    }

    private boolean inResourceStack(String resourceUuid) {
        return Q.New(CloudFormationStackResourceRefVO.class).eq(CloudFormationStackResourceRefVO_.resourceUuid, resourceUuid).isExists();
    }

    @Override
    public String convertFilterNameToZQL(String filterName) {
        String[] ss = filterName.split(":");
        if ("true".equals(ss[1])) {
            return "has ('cloudformation::autotag')";
        } else if ("false".equals(ss[1])) {
            return "not has ('cloudformation::autotag')";
        } else {
            throw new OperationFailureException(argerr("[cloudformation] filterName must be cloudformation:true or cloudformation:false"));
        }
    }
}
