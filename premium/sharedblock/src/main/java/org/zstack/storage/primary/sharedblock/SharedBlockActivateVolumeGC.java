package org.zstack.storage.primary.sharedblock;

import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.TimeBasedGarbageCollector;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.zstack.storage.primary.sharedblock.SharedBlockPrimaryStorageCanonicalEvent.UPDATE_ACTIVATE_VOLUME_GC_PATH;

public class SharedBlockActivateVolumeGC extends TimeBasedGarbageCollector {
    private static final CLogger logger = Utils.getLogger(SharedBlockActivateVolumeGC.class);
    @GC
    public String primaryStorageUuid;
    @GC
    public String hypervisorType;
    @GC
    public String installPath;
    @GC
    public LinkedHashMap<String, String> hostLockingTypeMap;
    @GC
    public int currentNumOfFailures;

    public static final int MAXNUM_FAILURES_NUM = 10;

    public void syncGcContextFromDB() {
        GarbageCollectorVO gc = dbf.findByUuid(getUuid(), GarbageCollectorVO.class);
        SharedBlockActivateVolumeGC context = JSONObjectUtil.toObject(gc.getContext(), SharedBlockActivateVolumeGC.class);
        this.hostLockingTypeMap = context.hostLockingTypeMap;
        this.currentNumOfFailures = context.currentNumOfFailures;
    }

    public void prepare() {
        Runnable r = () -> {
            SharedBlockPrimaryStorageCanonicalEvent.UpdateActivateVolumeGC data = new SharedBlockPrimaryStorageCanonicalEvent.UpdateActivateVolumeGC();
            data.setGcUuid(getUuid());
            data.setAction("add");
            data.setContext(buildContext());
            evtf.fire(UPDATE_ACTIVATE_VOLUME_GC_PATH, data);
        };
        r.run();
    }

    public void destroy() {
        Runnable r = () -> {
            SharedBlockPrimaryStorageCanonicalEvent.UpdateActivateVolumeGC data = new SharedBlockPrimaryStorageCanonicalEvent.UpdateActivateVolumeGC();
            data.setGcUuid(getUuid());
            data.setAction("remove");
            evtf.fire(UPDATE_ACTIVATE_VOLUME_GC_PATH, data);
        };
        r.run();
    }

    @Override
    protected void triggerNow(GCCompletion gcCompletion) {
        syncGcContextFromDB();
        if (currentNumOfFailures > MAXNUM_FAILURES_NUM) {
            logger.debug(String.format("sharedblock activating volume[installPath:%s] is canceled because num of failures has exceeded %d times", installPath, MAXNUM_FAILURES_NUM));
            gcCompletion.cancel();
            return;
        }

        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class) || hostLockingTypeMap == null || hostLockingTypeMap.isEmpty()) {
            gcCompletion.cancel();
            return;
        }

        logger.debug(String.format("running sharedblock activate volume[installPath:%s], operation sequence: %s",
                installPath, hostLockingTypeMap));

        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        copy.putAll(hostLockingTypeMap);
        new While<>(copy.entrySet()).each((item, completion) -> {
            String hostUuid = item.getKey();
            ActivateVolumeOnPrimaryStorageMsg amsg = new ActivateVolumeOnPrimaryStorageMsg();
            amsg.setHostUuid(hostUuid);
            amsg.setLockingType(item.getValue());
            amsg.setPrimaryStorageUuid(primaryStorageUuid);
            amsg.setInstallPath(installPath);
            amsg.setHypervisorType(hypervisorType);
            bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
            bus.send(amsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("unable to activate path:%s right now, skip this GC job because %s", installPath, reply.getError().getDetails()));
                        completion.addError(reply.getError());
                        completion.allDone();
                    } else {
                        hostLockingTypeMap.remove(hostUuid);
                        updateContext();
                        completion.done();
                    }
                }
            });
        }).run(new WhileDoneCompletion(gcCompletion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    gcCompletion.success();
                } else {
                    currentNumOfFailures ++;
                    updateContext();
                    gcCompletion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }
}
