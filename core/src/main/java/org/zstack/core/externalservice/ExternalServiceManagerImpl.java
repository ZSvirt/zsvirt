package org.zstack.core.externalservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.GlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.external.service.*;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.UserTagVO;
import org.zstack.header.tag.UserTagVO_;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.zstack.core.Platform.operr;

public class ExternalServiceManagerImpl extends AbstractService implements ExternalServiceManager {
    @Autowired
    public CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private final Map<String, ExternalService> services = new ConcurrentHashMap<>();

    @Override
    public ExternalService registerService(ExternalService service) {
        if (services.containsKey(service.getName())) {
            throw new OperationFailureException(operr("service[%s] has been registered", service.getName()));
        }

        services.put(service.getName(), service);
        return service;
    }

    @Override
    public void deregisterService(String name) {
        services.remove(name);
    }

    @Override
    public ExternalService getService(String name) {
        return services.get(name);
    }

    @Override
    public ExternalService getService(String name, Supplier<ExternalService> supplier) {
        ExternalService service = services.get(name);
        if (service != null) {
            return service;
        }

        service = supplier.get();
        return registerService(service);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        }  else {
            handleLocalMessage(msg);
        }
    }

    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetExternalServicesMsg) {
            handle((APIGetExternalServicesMsg) msg);
        } else if (msg instanceof APIReloadExternalServiceMsg) {
            handle((APIReloadExternalServiceMsg) msg);
        } else if (msg instanceof APIAddExternalServiceConfigurationMsg){
            handle((APIAddExternalServiceConfigurationMsg) msg);
        } else if (msg instanceof APIUpdateExternalServiceConfigurationMsg) {
            handle((APIUpdateExternalServiceConfigurationMsg) msg);
        } else if (msg instanceof APIDeleteExternalServiceConfigurationMsg) {
            handle((APIDeleteExternalServiceConfigurationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof ApplyExternalServiceConfigurationMsg) {
            handle((ApplyExternalServiceConfigurationMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIReloadExternalServiceMsg msg) {
        APIReloadExternalServiceEvent event = new APIReloadExternalServiceEvent(msg.getId());
        ExternalService service = services.get(msg.getName());
        if (service == null) {
            event.setError(operr("service[%s] is not registered", msg.getName()));
            bus.publish(event);
            return;
        }

        if (!service.getExternalServiceCapabilities().isReloadConfig()) {
            event.setError(operr("service[%s] does not support reload config", msg.getName()));
        }

        if (service.isAlive()) {
            service.reload();
        } else {
            event.setError(operr("service[%s] is not running", msg.getName()));
        }

        bus.publish(event);
    }

    private void handle(APIGetExternalServicesMsg msg) {
        APIGetExternalServicesReply reply = new APIGetExternalServicesReply();
        reply.setInventories(new ArrayList<>());

        services.forEach((name, service) -> {
            ExternalServiceInventory inv = new ExternalServiceInventory();
            inv.setName(name);
            inv.setStatus(service.isAlive() ? ExternalServiceStatus.RUNNING.toString() : ExternalServiceStatus.STOPPED.toString());
            inv.setCapabilities(service.getExternalServiceCapabilities());
            inv.setServiceType(service.getServiceType());
            reply.getInventories().add(inv);
        });

        bus.reply(msg, reply);
    }

    private void handle(APIAddExternalServiceConfigurationMsg msg ){
        APIAddExternalServiceConfigurationEvent event = new APIAddExternalServiceConfigurationEvent(msg.getId());

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                createExternalServiceConfiguration(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("create-update-delete-external-service-configuration-%s", msg.getExternalServiceType());
            }

            @Override
            public String getName() {
                return String.format("create-external-service-configuration-type-%s", msg.getExternalServiceType());
            }
        });

    }

    private void createExternalServiceConfiguration(APIAddExternalServiceConfigurationMsg msg, APIAddExternalServiceConfigurationEvent evt, Completion completion) {
        if (!hasRegisteredServiceType(msg.getExternalServiceType())) {
            completion.fail(operr("unable to find external service type [%s]", msg.getExternalServiceType()));
            return;
        }
        final String configuration;
        try {
            configuration = ExternalServiceConfigurationInventory.restoreMaskedRemoteWritePassword(msg.getConfiguration(), null);
        } catch (IllegalArgumentException e) {
            completion.fail(operr("invalid external service configuration: %s", e.getMessage()));
            return;
        }

        // create db record
        ExternalServiceConfigurationVO configurationVO = new ExternalServiceConfigurationVO();
        configurationVO.setUuid(msg.getResourceUuid() != null ? msg.getResourceUuid() : Platform.getUuid());
        configurationVO.setServiceType(msg.getExternalServiceType());
        configurationVO.setConfiguration(configuration);
        configurationVO.setDescription(msg.getDescription());
        configurationVO = dbf.persistAndRefresh(configurationVO);

        ExternalServiceConfigurationInventory inv = ExternalServiceConfigurationInventory.valueOf(configurationVO);
        evt.setInventory(inv);
        final String uuid = configurationVO.getUuid();
        final String serviceType = configurationVO.getServiceType();

        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                inv.setApplyResults(returnValue);
                evt.setInventory(inv);
                ErrorCode errorCode = firstFailure(returnValue);
                if (errorCode != null) {
                    rollbackCreatedExternalServiceConfiguration(uuid, serviceType, errorCode, completion);
                    return;
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                rollbackCreatedExternalServiceConfiguration(uuid, serviceType, errorCode, completion);
            }
        });
    }

    private void rollbackCreatedExternalServiceConfiguration(String uuid, String serviceType, ErrorCode originalError, Completion completion) {
        dbf.removeByPrimaryKey(uuid, ExternalServiceConfigurationVO.class);
        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                completion.fail(originalError);
            }

            @Override
            public void fail(ErrorCode rollbackError) {
                completion.fail(originalError);
            }
        });
    }

    private void handle(APIUpdateExternalServiceConfigurationMsg msg){
        APIUpdateExternalServiceConfigurationEvent event = new APIUpdateExternalServiceConfigurationEvent(msg.getId());
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);
        final String syncKey = vo != null ? vo.getServiceType() : msg.getUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                updateExternalServiceConfiguration(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("create-update-delete-external-service-configuration-%s", syncKey);
            }

            @Override
            public String getName() {
                return String.format("update-external-service-configuration-%s", msg.getUuid());
            }
        });
    }

    private void updateExternalServiceConfiguration(APIUpdateExternalServiceConfigurationMsg msg, APIUpdateExternalServiceConfigurationEvent evt, Completion completion) {
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);

        if (vo == null) {
            completion.fail(operr("unable to find external service configuration with uuid [%s]", msg.getUuid()));
            return;
        }

        final String oldDescription = vo.getDescription();
        final String oldConfiguration = vo.getConfiguration();
        final String serviceType = vo.getServiceType();
        final String configuration;
        try {
            configuration = msg.getConfiguration() == null ? null :
                    ExternalServiceConfigurationInventory.restoreMaskedRemoteWritePassword(msg.getConfiguration(), oldConfiguration);
        } catch (IllegalArgumentException e) {
            completion.fail(operr("invalid external service configuration: %s", e.getMessage()));
            return;
        }
        boolean configChanged = configuration != null && !configuration.equals(oldConfiguration);
        boolean updated = false;

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
            updated = true;
        }
        if (configuration != null) {
            vo.setConfiguration(configuration);
            updated = true;
        }
        if (updated) {
            vo = dbf.updateAndRefresh(vo);
        }

        ExternalServiceConfigurationInventory inv = ExternalServiceConfigurationInventory.valueOf(vo);
        evt.setInventory(inv);
        if (!configChanged) {
            completion.success();
            return;
        }

        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                inv.setApplyResults(returnValue);
                evt.setInventory(inv);
                ErrorCode errorCode = firstFailure(returnValue);
                if (errorCode != null) {
                    rollbackUpdatedExternalServiceConfiguration(msg.getUuid(), serviceType, oldDescription, oldConfiguration, errorCode, completion);
                    return;
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                rollbackUpdatedExternalServiceConfiguration(msg.getUuid(), serviceType, oldDescription, oldConfiguration, errorCode, completion);
            }
        });
    }

    private void rollbackUpdatedExternalServiceConfiguration(String uuid, String serviceType, String oldDescription,
            String oldConfiguration, ErrorCode originalError, Completion completion) {
        ExternalServiceConfigurationVO vo = dbf.findByUuid(uuid, ExternalServiceConfigurationVO.class);
        if (vo != null) {
            vo.setDescription(oldDescription);
            vo.setConfiguration(oldConfiguration);
            dbf.update(vo);
        }

        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                completion.fail(originalError);
            }

            @Override
            public void fail(ErrorCode rollbackError) {
                completion.fail(originalError);
            }
        });
    }

    private void handle(APIDeleteExternalServiceConfigurationMsg msg) {
        APIDeleteExternalServiceConfigurationEvent event = new APIDeleteExternalServiceConfigurationEvent(msg.getId());
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);
        final String syncKey = vo != null ? vo.getServiceType() : msg.getUuid();

        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                deleteExternalServiceConfiguration(msg, event, new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(event);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        event.setError(errorCode);
                        bus.publish(event);
                        chain.next();
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return String.format("create-update-delete-external-service-configuration-%s", syncKey);
            }

            @Override
            public String getName() {
                return String.format("delete-external-service-configuration-%s", msg.getUuid());
            }
        });
    }

    private void deleteExternalServiceConfiguration(APIDeleteExternalServiceConfigurationMsg msg, APIDeleteExternalServiceConfigurationEvent evt, Completion completion) {
        // delete db record
        ExternalServiceConfigurationVO vo = dbf.findByUuid(msg.getUuid(), ExternalServiceConfigurationVO.class);
        String serviceType;
        if (vo != null) {
            serviceType = vo.getServiceType();
            SavedResourceRelations relations = saveResourceRelations(vo.getUuid());
            dbf.remove(vo);
            applyDeletedExternalServiceConfiguration(vo, relations, serviceType, evt, completion);
        } else {
            completion.success();
        }
    }

    private void applyDeletedExternalServiceConfiguration(ExternalServiceConfigurationVO vo, SavedResourceRelations relations,
            String serviceType, APIDeleteExternalServiceConfigurationEvent evt, Completion completion) {
        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                evt.setApplyResults(returnValue);
                ErrorCode errorCode = firstFailure(returnValue);
                if (errorCode != null) {
                    rollbackDeletedExternalServiceConfiguration(vo, relations, serviceType, errorCode, completion);
                    return;
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                rollbackDeletedExternalServiceConfiguration(vo, relations, serviceType, errorCode, completion);
            }
        });
    }

    private void rollbackDeletedExternalServiceConfiguration(ExternalServiceConfigurationVO vo, SavedResourceRelations relations, String serviceType,
            ErrorCode originalError, Completion completion) {
        dbf.persist(copyExternalServiceConfigurationVO(vo));
        restoreResourceRelations(relations);
        applyExternalServiceConfigurationToAllNodes(serviceType, new ReturnValueCompletion<List<ApplyExternalConfigurationResult>>(completion) {
            @Override
            public void success(List<ApplyExternalConfigurationResult> returnValue) {
                completion.fail(originalError);
            }

            @Override
            public void fail(ErrorCode rollbackError) {
                completion.fail(originalError);
            }
        });
    }

    private ExternalServiceConfigurationVO copyExternalServiceConfigurationVO(ExternalServiceConfigurationVO vo) {
        // The original VO may have been removed from Hibernate's persistence context; re-persist a clean instance.
        ExternalServiceConfigurationVO copy = new ExternalServiceConfigurationVO();
        copy.setUuid(vo.getUuid());
        copy.setServiceType(vo.getServiceType());
        copy.setConfiguration(vo.getConfiguration());
        copy.setDescription(vo.getDescription());
        copy.setCreateDate(vo.getCreateDate());
        copy.setLastOpDate(vo.getLastOpDate());
        return copy;
    }

    private SavedResourceRelations saveResourceRelations(String resourceUuid) {
        SavedResourceRelations relations = new SavedResourceRelations();
        relations.accountRefs = copyAccountResourceRefs(Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, resourceUuid)
                .list());
        relations.systemTags = copySystemTags(Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, resourceUuid)
                .list());
        relations.userTags = copyUserTags(Q.New(UserTagVO.class)
                .eq(UserTagVO_.resourceUuid, resourceUuid)
                .list());
        return relations;
    }

    private void restoreResourceRelations(SavedResourceRelations relations) {
        for (AccountResourceRefVO ref : relations.accountRefs) {
            dbf.persist(ref);
        }
        for (SystemTagVO tag : relations.systemTags) {
            dbf.persist(tag);
        }
        for (UserTagVO tag : relations.userTags) {
            dbf.persist(tag);
        }
    }

    private List<AccountResourceRefVO> copyAccountResourceRefs(List<AccountResourceRefVO> refs) {
        List<AccountResourceRefVO> copies = new ArrayList<>();
        for (AccountResourceRefVO ref : refs) {
            AccountResourceRefVO copy = new AccountResourceRefVO();
            copy.setAccountUuid(ref.getAccountUuid());
            copy.setResourceUuid(ref.getResourceUuid());
            copy.setResourceType(ref.getResourceType());
            copy.setAccountPermissionFrom(ref.getAccountPermissionFrom());
            copy.setResourcePermissionFrom(ref.getResourcePermissionFrom());
            copy.setType(ref.getType());
            copy.setCreateDate(ref.getCreateDate());
            copy.setLastOpDate(ref.getLastOpDate());
            copies.add(copy);
        }
        return copies;
    }

    private List<SystemTagVO> copySystemTags(List<SystemTagVO> tags) {
        List<SystemTagVO> copies = new ArrayList<>();
        for (SystemTagVO tag : tags) {
            copies.add(new SystemTagVO(tag));
        }
        return copies;
    }

    private List<UserTagVO> copyUserTags(List<UserTagVO> tags) {
        List<UserTagVO> copies = new ArrayList<>();
        for (UserTagVO tag : tags) {
            copies.add(new UserTagVO(tag));
        }
        return copies;
    }

    private static class SavedResourceRelations {
        List<AccountResourceRefVO> accountRefs = Collections.emptyList();
        List<SystemTagVO> systemTags = Collections.emptyList();
        List<UserTagVO> userTags = Collections.emptyList();
    }

    private ErrorCode firstFailure(List<ApplyExternalConfigurationResult> results) {
        for (ApplyExternalConfigurationResult result : results) {
            if (!result.isSuccess()) {
                return result.getErrorCode() != null ? result.getErrorCode() :
                        operr("failed to apply external service configuration on management node [%s]", result.getManagementNodeUuid());
            }
        }
        return null;
    }

    private boolean hasRegisteredServiceType(String serviceType) {
        for (ExternalService service : services.values()) {
            if (serviceType.equals(service.getServiceType())) {
                return true;
            }
        }
        return false;
    }

    private void applyExternalServiceConfigurationToAllNodes(String serviceType, ReturnValueCompletion<List<ApplyExternalConfigurationResult>> completion) {
        final List<ApplyExternalConfigurationResult> results = Collections.synchronizedList(new ArrayList<>());

        FlowChain chain = new SimpleFlowChain();
        chain.setName("apply-external-service-configuration-to-all-nodes");
        chain.then(new Flow() {
            String __name__ = "apply-external-service-configuration";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> mnUuids = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues();

                new While<>(mnUuids).each((mnUuid, whileCompletion) -> {
                    ApplyExternalServiceConfigurationMsg amsg = new ApplyExternalServiceConfigurationMsg();
                    amsg.setServiceType(serviceType);
                    bus.makeServiceIdByManagementNodeId(amsg, SERVICE_ID, mnUuid);
                    bus.send(amsg, new CloudBusCallBack(whileCompletion) {
                        @Override
                        public void run(MessageReply reply) {
                            ApplyExternalConfigurationResult result = new ApplyExternalConfigurationResult();
                            result.setManagementNodeUuid(mnUuid);
                            results.add(result);

                            if (!reply.isSuccess()) {
                                result.setErrorCode(reply.getError());
                                whileCompletion.done();
                                return;
                            }
                            whileCompletion.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                trigger.rollback();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success(new ArrayList<>(results));
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    private void handle(ApplyExternalServiceConfigurationMsg msg) {
        ApplyExternalServiceConfigurationReply reply = new ApplyExternalServiceConfigurationReply();

        regenerateExternalServiceConfiguration(msg.getServiceType(), new ReturnValueCompletion<String>(msg) {
            @Override
            public void success(String returnValue) {
                reply.setValue(returnValue);
                reply.setManagementNodeUuid(Platform.getManagementServerId());
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void regenerateExternalServiceConfiguration(String serviceType, ReturnValueCompletion<String> completion) {
        for (ExternalService service : services.values()) {
            if (serviceType.equals(service.getServiceType())) {
                try{
                    service.externalConfig(serviceType);
                    completion.success(serviceType);
                } catch (Exception e) {
                    completion.fail(operr("failed to apply external service configuration for type [%s]: %s", serviceType, e.getMessage()));
                }
                return;
            }
        }
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success(serviceType);
            return;
        }
        completion.fail(operr("unable to find external service type [%s]", serviceType));
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
