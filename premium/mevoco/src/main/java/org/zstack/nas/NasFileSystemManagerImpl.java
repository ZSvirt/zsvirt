package org.zstack.nas;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2018/3/6.
 */
public class NasFileSystemManagerImpl extends AbstractService {
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;

    private Map<String, NasFileSystemFactory> nasFileSystemFactories = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APICreateNasFileSystemMsg) {
            handle((APICreateNasFileSystemMsg) msg);
        } else if (msg instanceof APICreateNasMountTargetMsg) {
            handle((APICreateNasMountTargetMsg) msg);
        } else if (msg instanceof NasFileSystemMessage) {
            passThrough((NasFileSystemMessage) msg);
        } else if (msg instanceof NasMountTargetMessage) {
            passThrough((NasMountTargetMessage) msg);
        } else if (msg instanceof NasTypeFileSystemMessage) {
            passThrough((NasTypeFileSystemMessage) msg);
        } else if (msg instanceof NasTypeMountTargetMessage) {
            passThrough((NasTypeMountTargetMessage) msg);
        } else if (msg instanceof NasMessage) {
            passThrough((NasMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void passThrough(final NasMountTargetMessage msg) {
        NasMountTargetVO vo = dbf.findByUuid(msg.getNasMountTargetUuid(), NasMountTargetVO.class);
        DebugUtils.Assert(vo != null, String.format("nas mount target [%s] is not existed yet", msg.getNasMountTargetUuid()));

        NasFileSystemFactory factory = getFactory(vo.getType());
        NasFileSystem nas = factory.getNasMountTarget(vo);
        nas.handleMessage((Message)msg);
    }

    private void passThrough(final NasMessage msg) {
        NasFileSystemFactory factory = getFactory(msg.getNasType());
        NasFileSystem nas = factory.getNas();
        nas.handleMessage((Message)msg);
    }

    private void passThrough(final NasTypeMountTargetMessage msg) {
        NasMountTargetVO vo = new NasMountTargetVO();
        vo.setUuid(Platform.getUuid());
        vo.setType(msg.getNasType());

        NasFileSystemFactory factory = getFactory(msg.getNasType());
        NasFileSystem nas = factory.getNasMountTarget(vo);
        nas.handleMessage((Message)msg);
    }

    private void passThrough(final NasTypeFileSystemMessage msg) {
        NasFileSystemVO vo = new NasFileSystemVO();
        vo.setUuid(Platform.getUuid());
        vo.setType(msg.getNasType());

        NasFileSystemFactory factory = getFactory(msg.getNasType());
        NasFileSystem nas = factory.getNasFileSystem(vo);
        nas.handleMessage((Message)msg);
    }

    private void passThrough(final NasFileSystemMessage msg) {
        NasFileSystemVO vo = dbf.findByUuid(msg.getNasFileSystemUuid(), NasFileSystemVO.class);
        if (vo == null) {
            throw new OperationFailureException(operr("nas file system [%s] is not existed yet", msg.getNasFileSystemUuid()));
        }
        NasFileSystemFactory factory = getFactory(vo.getType());
        NasFileSystem nas = factory.getNasFileSystem(vo);
        nas.handleMessage((Message)msg);
    }

    private void handle(final APICreateNasFileSystemMsg msg) {
        NasFileSystemVO vo = new NasFileSystemVO();
        vo.setDescription(msg.getDescription());
        vo.setName(msg.getName());
        vo.setProtocol(NasProtocolType.valueOf(msg.getProtocol()));
        vo.setType(msg.getType());

        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        NasFileSystemFactory factory = getFactory(msg.getType());
        NasFileSystem nas = factory.getNasFileSystem(vo);
        nas.handleMessage(msg);
    }

    private void handle(final APICreateNasMountTargetMsg msg) {
        NasMountTargetVO vo = new NasMountTargetVO();
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setType(msg.getType());
        vo.setNasFileSystemUuid(msg.getNasFSUuid());
        vo.setAccountUuid(msg.getSession().getAccountUuid());

        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }

        NasFileSystemFactory factory = getFactory(msg.getType());
        NasFileSystem nas = factory.getNasMountTarget(vo);
        nas.handleMessage(msg);
    }

    private NasFileSystemFactory getFactory(String type) {
        NasFileSystemFactory factory = nasFileSystemFactories.get(type);
        if (factory == null) {
            throw new OperationFailureException(operr("cannot find nas factory for type: %s", type));
        }
        return factory;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(NasFileSystemConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateExtensions() {
        for (NasFileSystemFactory f : pluginRgty.getExtensionList(NasFileSystemFactory.class)) {
            NasFileSystemFactory old = nasFileSystemFactories.get(f.getNasFileSystemType());
            if (old != null) {
                throw new OperationFailureException(operr("duplicate NasFileSystemFactory[%s, %s] for type[%s]",
                        f.getClass().getSimpleName(), old.getClass().getSimpleName(), f.getNasFileSystemType()));
            }
            nasFileSystemFactories.put(f.getNasFileSystemType(), f);
        }
    }
}
