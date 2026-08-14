package org.zstack.storage.addon.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.LinkedHashMap;

import static org.zstack.core.Platform.multiErr;
import static org.zstack.core.Platform.operr;

public class ExternalPrimaryStorageManagerImpl extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorageManagerImpl.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ExternalPrimaryStorageFactory factory;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof PrimaryStorageMessage) {
            passThrough((PrimaryStorageMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(PrimaryStorageMessage msg) {
        ExternalPrimaryStorageVO vo = dbf.findByUuid(msg.getPrimaryStorageUuid(), ExternalPrimaryStorageVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("cannot find ExternalPrimaryStorage[uuid:%s]", msg.getPrimaryStorageUuid()));
        }

        PrimaryStorage ext = factory.getPrimaryStorage(vo);
        ext.handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIDiscoverExternalPrimaryStorageMsg) {
            handle((APIDiscoverExternalPrimaryStorageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIDiscoverExternalPrimaryStorageMsg msg) {
        APIDiscoverExternalPrimaryStorageEvent event = new APIDiscoverExternalPrimaryStorageEvent(msg.getId());
        ExternalPrimaryStorageInventory inventory = new ExternalPrimaryStorageInventory();
        inventory.setUrl(msg.getUrl());
        inventory.setConfig(JSONObjectUtil.toObject(msg.getConfig(), LinkedHashMap.class));
        inventory.setIdentity(msg.getIdentity());

        if (msg.getIdentity() != null) {
            ExternalPrimaryStorageSvcBuilder builder = pluginRgty.getExtensionFromMap(msg.getIdentity(), ExternalPrimaryStorageSvcBuilder.class);
            builder.discover(msg.getUrl(), msg.getConfig(), new ReturnValueCompletion<LinkedHashMap>(msg) {
                @Override
                public void success(LinkedHashMap addonInfo) {
                    inventory.setIdentity(msg.getIdentity());
                    inventory.setAddonInfo(addonInfo);
                    event.setInventory(inventory);
                    bus.publish(event);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    event.setError(errorCode);
                    bus.publish(event);
                }
            });
            return;
        }

        new While<>(pluginRgty.getExtensionList(ExternalPrimaryStorageSvcBuilder.class)).each((builder, comp) -> {
            builder.discover(msg.getUrl(), msg.getConfig(), new ReturnValueCompletion<LinkedHashMap>(comp) {
                @Override
                public void success(LinkedHashMap addonInfo) {
                    inventory.setIdentity(builder.getIdentity());
                    inventory.setAddonInfo(addonInfo);
                    event.setInventory(inventory);
                    comp.allDone();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    comp.addError(errorCode);
                    comp.done();
                }
            });
        }).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (event.getInventory() == null) {
                    event.setError(multiErr(errorCodeList, "cannot connect any external storage"));
                }

                bus.publish(event);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ExternalPrimaryStorageConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(BackupStorageSelector.class, BackupStorageSelector::getIdentity);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
