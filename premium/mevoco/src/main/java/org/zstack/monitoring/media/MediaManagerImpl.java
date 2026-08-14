package org.zstack.monitoring.media;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.AccountManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2017/6/11.
 */
public class MediaManagerImpl extends AbstractService implements MediaManager {
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private DatabaseFacade dbf;

    private Map<String, MediaFactory> mediaFactories = new HashMap<>();

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    public MediaFactory getMediaFactory(String type) {
        MediaFactory f = mediaFactories.get(type);
        if (f == null) {
            throw new CloudRuntimeException(String.format("cannot find the MediaFactory[type:%s]", type));
        }
        return f;
    }

    private void populateExtensions() {
        for (MediaFactory f : pluginRgty.getExtensionList(MediaFactory.class)) {
            MediaFactory old = mediaFactories.get(f.getMediaType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate MediaFactory[%s, %s] with the same type[%s]",
                        old.getClass(), f.getClass(), f.getMediaType()));
            }

            mediaFactories.put(f.getMediaType(), f);
        }
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof MediaMessage) {
            passThrough((MediaMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(MediaMessage msg) {
        MediaVO vo = dbf.findByUuid(msg.getMediaMessageUuid(), MediaVO.class);
        if (vo == null) {
            throw new CloudRuntimeException(String.format("cannot find media[uuid:%s], it may have been deleted", msg.getMediaMessageUuid()));
        }

        new MediaBase(vo).handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateMediaMsg) {
            handle((APICreateMediaMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateMediaMsg msg) {
        APICreateMediaEvent evt = new APICreateMediaEvent(msg.getId());

        MediaVO vo = new MediaVO();
        vo.setUuid(msg.getResourceUuid() == null ? Platform.getUuid() : msg.getResourceUuid());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setType(msg.getType());
        vo.setState(MediaState.Enabled);
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        MediaFactory f = getMediaFactory(msg.getType());
        MediaVO finalVo = vo;
        vo = new SQLBatchWithReturn<MediaVO>() {
            @Override
            protected MediaVO scripts() {
                return f.createMedia(finalVo, msg);
            }
        }.execute();

        evt.setInventory(f.getMediaInventory(vo));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(MediaConstants.SERVICE_ID);
    }
}
