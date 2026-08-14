package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.tacker.PingTracker;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.primary.PingPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class PrimaryStoragePingTracker extends PingTracker implements ManagementNodeReadyExtensionPoint, ManagementNodeChangeListener {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceDestinationMaker destMaker;

    @Override
    public String getResourceName() {
        return "primary storage";
    }

    @Override
    public NeedReplyMessage getPingMessage(String resUuid) {
        PingPrimaryStorageMsg msg = new PingPrimaryStorageMsg();
        msg.setPrimaryStorageUuid(resUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, resUuid);
        return msg;
    }

    @Override
    public int getPingInterval() {
        return PrimaryStorageGlobalConfig.PING_INTERVAL.value(Integer.class);
    }

    @Override
    public int getParallelismDegree() {
        return PrimaryStorageGlobalConfig.PING_PARALLELISM_DEGREE.value(Integer.class);
    }

    @Override
    public void handleReply(String resourceUuid, MessageReply reply) {
        // nothing to do
    }

    @Override
    protected void startHook() {
        PrimaryStorageGlobalConfig.PING_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                pingIntervalChanged();
            }
        });
    }

    private void reScanPrimaryStorage() {
        untrackAll();

        new SQLBatch() {
            @Override
            protected void scripts() {
                long count = sql("select count(ps) from PrimaryStorageVO ps", Long.class).find();
                sql("select ps.uuid from PrimaryStorageVO ps", String.class).limit(1000).paginate(count, (List<String> psUuids) -> {
                    List<String> byUs = psUuids.stream()
                            .filter(psUuid ->
                                    destMaker.isManagedByUs(psUuid))
                            .collect(Collectors.toList());

                    track(byUs);
                });
            }
        }.execute();
    }

    @Override
    public void managementNodeReady() {
        reScanPrimaryStorage();
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        reScanPrimaryStorage();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        reScanPrimaryStorage();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {

    }
}
