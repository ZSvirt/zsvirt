package org.zstack.sns;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APIChangeResourceOwnerMsg;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.ReportQuotaExtensionPoint;
import org.zstack.header.identity.quota.QuotaMessageHandler;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;
import org.zstack.sns.platform.email.APICreateSNSEmailPlatformMsg;
import org.zstack.sns.platform.email.SNSApplicationPlatformSystemTags;
import org.zstack.sns.platform.http.APICreateSNSHttpEndpointMsg;
import org.zstack.tag.SystemTagCreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.*;

public class SNSManagerImpl extends AbstractService implements PrepareDbInitialValueExtensionPoint, SNSManager, ReportQuotaExtensionPoint {
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;

    private Map<String, SNSApplicationPlatformFactory> platformFactories = new HashMap<>();
    private Map<String, SNSApplicationEndpointFactory> endpointFactories = new HashMap<>();

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof SNSApplicationEndpointMessage) {
            passThrough((SNSApplicationEndpointMessage) msg);
        } else if (msg instanceof SNSApplicationPlatformMessage) {
            passThrough((SNSApplicationPlatformMessage) msg);
        } else if (msg instanceof SNSTopicMessage) {
            passThrough((SNSTopicMessage) msg);
        } else if (msg instanceof SNSApplicationEndpointTestConnectionMessage) {
            passThrough((SNSApplicationEndpointTestConnectionMessage) msg);
        } else if (msg instanceof SNSApplicationPlatformTestConnectionMessage) {
            passThrough((SNSApplicationPlatformTestConnectionMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(SNSTopicMessage msg) {
        SNSTopicVO vo = dbf.findByUuid(msg.getTopicUuid(), SNSTopicVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find the SNSTopic[uuid:%s], it may have been deleted", msg.getTopicUuid()));
        }

        SNSTopic topic = null;
        for (SNSTopicFactory snsTopicFactory : pluginRgty.getExtensionList(SNSTopicFactory.class)) {
            topic = snsTopicFactory.createSNSTopic(msg.getTopicUuid());
            if (topic != null) {
                break;
            }
        }

        topic = topic == null ? new SNSTopicBase(vo) : topic;
        topic.handleMessage((Message) msg);
    }

    private void passThrough(SNSApplicationPlatformTestConnectionMessage msg) {
        if (msg.getPlatformType() == null) {
            throw new OperationFailureException(operr("cannot find the endpointType",
                    msg.getPlatformType()));
        }

        SNSApplicationPlatform platform = getSNSApplicationPlatformFactory(msg.getPlatformType().toString())
                .getSNSApplicationPlatform("");
        platform.handleMessage((Message) msg);
    }

    private void passThrough(SNSApplicationEndpointTestConnectionMessage msg) {
        if (msg.getEndpointType() == null) {
            throw new OperationFailureException(operr("cannot find the endpointType",
                    msg.getEndpointType()));
        }

        SNSApplicationEndpoint endpoint = getSNSApplicationEndpointFactory(msg.getEndpointType().toString())
                .getSNSApplicationEndpoint();
        endpoint.handleMessage((Message) msg);
    }

    private void passThrough(SNSApplicationPlatformMessage msg) {
        SNSApplicationPlatformVO vo = dbf.findByUuid(msg.getApplicationPlatformUuid(), SNSApplicationPlatformVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find SNSApplicationPlatform[uuid:%s], it may have been deleted", msg.getApplicationPlatformUuid()));
        }

        SNSApplicationPlatform platform = getSNSApplicationPlatformFactory(vo.getType()).getSNSApplicationPlatform(vo.getUuid());
        platform.handleMessage((Message) msg);
    }

    private void passThrough(SNSApplicationEndpointMessage msg) {
        SNSApplicationEndpointVO vo = dbf.findByUuid(msg.getApplicationEndpointUuid(), SNSApplicationEndpointVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find SNSApplicationEndpoint[uuid:%s], it may have been deleted", msg.getApplicationEndpointUuid()));
        }

        SNSApplicationEndpoint endpoint = getSNSApplicationEndpointFactory(vo.getType()).getSNSApplicationEndpoint(vo.getUuid());
        endpoint.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateSNSApplicationPlatformMsg) {
            handle((APICreateSNSApplicationPlatformMsg) msg);
        } else if (msg instanceof APICreateSNSTopicMsg) {
            handle((APICreateSNSTopicMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateSNSTopicMsg msg) {
        SNSTopicVO vo = new SNSTopicVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setState(SNSTopicState.Enabled);
        vo.setOwnerType(SNSTopicOwnerType.Customized);
        vo.setDescription(msg.getDescription());
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setLocale(msg.getLocale());

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);
                reload(vo);
            }
        }.execute();

        APICreateSNSTopicEvent evt = new APICreateSNSTopicEvent(msg.getId());
        evt.setInventory(SNSTopicInventory.valueOf(vo));
        bus.publish(evt);
    }

    @Override
    public SNSApplicationPlatformFactory getSNSApplicationPlatformFactory(String type) {
        SNSApplicationPlatformFactory f = platformFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find SNSApplicationPlatformFactory with type[%s]", type));
        }
        return f;
    }

    @Override
    public SNSApplicationEndpointFactory getSNSApplicationEndpointFactory(String type) {
        SNSApplicationEndpointFactory f = endpointFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find SNSApplicationEndpointFactory with type[%s]", type));
        }
        return f;
    }

    private void handle(APICreateSNSApplicationPlatformMsg msg) {
        SNSApplicationPlatformVO vo = new SNSApplicationPlatformVO();
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setType(msg.getType());
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setState(SNSApplicationPlatformState.Enabled);
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        SNSApplicationPlatformFactory f = getSNSApplicationPlatformFactory(msg.getType());
        SNSApplicationPlatformVO rvo = f.createApplicationPlatform(vo, msg);

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(rvo);
                reload(rvo);
            }
        }.execute();

        if (msg instanceof APICreateSNSEmailPlatformMsg) {
            SystemTagCreator creator = SNSApplicationPlatformSystemTags.SNS_ENCRYPTTYPE.newSystemTagCreator(rvo.getUuid());
            creator.setTagByTokens(map(e(SNSApplicationPlatformSystemTags.SNS_ENCRYPTTYPE_TOKEN, ((APICreateSNSEmailPlatformMsg) msg).getEncryptType())));
            creator.ignoreIfExisting = false;
            creator.inherent = true;
            creator.create();
        }

        APICreateSNSApplicationPlatformEvent evt = new APICreateSNSApplicationPlatformEvent(msg.getId());
        evt.setInventory(f.getSNSApplicationPlatformInventory(rvo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SNSConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    private void populateExtensions() {
        pluginRgty.getExtensionList(SNSApplicationPlatformFactory.class).forEach(f -> {
            SNSApplicationPlatformFactory old = platformFactories.get(f.getApplicationPlatformType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate SNSApplicationPlatformFactory[%s, %s] with the same type[%s]", old.getClass(), f.getClass(), f.getApplicationPlatformType()));
            }

            platformFactories.put(f.getApplicationPlatformType(), f);
        });

        pluginRgty.getExtensionList(SNSApplicationEndpointFactory.class).forEach(f -> {
            SNSApplicationEndpointFactory old = endpointFactories.get(f.getApplicationEndpointType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate SNSApplicationEndpointFactory[%s, %s] with the same application platform type[%s]",
                        f.getClass(), old.getClass(), f.getApplicationEndpointType()));
            }

            endpointFactories.put(f.getApplicationEndpointType(), f);
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(SNSApplicationPlatformVO.class).eq(SNSApplicationPlatformVO_.type, SNSConstants.SYSTEM_PLATFORM).isExists()) {
                    SNSApplicationPlatformVO vo = new SNSApplicationPlatformVO();
                    vo.setName("system");
                    vo.setDescription("this is used by endpoints which doesn't have own application platforms");
                    vo.setState(SNSApplicationPlatformState.Enabled);
                    vo.setType(SNSConstants.SYSTEM_PLATFORM);
                    vo.setUuid(SNSConstants.SYSTEM_PLATFORM_UUID);
                    vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    persist(vo);
                }
            }
        }.execute();
    }

    @Override
    public List<Quota> reportQuota() {
        Quota quota = new Quota();
        quota.defineQuota(new SNSEndpointNumQuotaDefinition());
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APICreateSNSApplicationEndpointMsg.class)
                .addCounterQuota(SNSQuotaConstant.SNS_ENDPOINT_NUM));
        quota.addQuotaMessageChecker(new QuotaMessageHandler<>(APIChangeResourceOwnerMsg.class)
                .addCheckCondition((msg) -> Q.New(SNSApplicationEndpointVO.class)
                        .eq(SNSApplicationEndpointVO_.uuid, msg.getResourceUuid())
                        .isExists())
                .addCounterQuota(SNSQuotaConstant.SNS_ENDPOINT_NUM));
        return list(quota);
    }
}
