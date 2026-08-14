package org.zstack.expon;

import org.zstack.core.singleflight.MultiNodeSingleFlightImpl;
import org.zstack.externalStorage.primary.ExternalStorageFencerType;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.addon.primary.*;
import org.zstack.header.volume.VolumeAfterExpungeExtensionPoint;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExponStorageFactory implements ExternalPrimaryStorageSvcBuilder, BackupStorageSelector, VolumeAfterExpungeExtensionPoint {
    public static final ExternalStorageFencerType FENCER_TYPE = new ExternalStorageFencerType(ExponConstants.IDENTITY, VolumeProtocol.iSCSI.toString());

    private List<String> preferBackupStorageTypes;

    private static Map<String, ExponStorageController> controllers = new ConcurrentHashMap<>();


    @Override
    public PrimaryStorageControllerSvc buildControllerSvc(ExternalPrimaryStorageVO vo) {
        ExponStorageController svc = new ExponStorageController(vo);
        MultiNodeSingleFlightImpl.register(svc.getApiHelper());
        controllers.put(vo.getUuid(), svc);
        return svc;
    }

    @Override
    public PrimaryStorageNodeSvc buildNodeSvc(ExternalPrimaryStorageVO vo) {
        return new ExponStorageController(vo);
    }

    @Override
    public void discover(String url, String config, ReturnValueCompletion<LinkedHashMap> completion) {
        ExponStorageController controller = new ExponStorageController(url);
        MultiNodeSingleFlightImpl.register(controller.getApiHelper());
        controller.connect(config, url, completion);
    }

    @Override
    public String getIdentity() {
        return ExponConstants.IDENTITY;
    }

    @Override
    public List<String> getPreferBackupStorageTypes() {
        return preferBackupStorageTypes;
    }

    public void setPreferBackupStorageTypes(List<String> preferBackupStorageTypes) {
        this.preferBackupStorageTypes = preferBackupStorageTypes;
    }

    // TODO: hard code for less http call.
    @Override
    public void volumeAfterExpunge(VolumeInventory volume) {
        if (volume.getInstallPath() == null || !volume.getInstallPath().startsWith("expon://")) {
            return;
        }

        controllers.get(volume.getPrimaryStorageUuid()).cleanActiveRecord(volume);
    }
}
