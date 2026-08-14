package org.zstack.compute.ovs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.VSwitchManager;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigDeleteExtensionPoint;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigUpdateExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by haibiao.xiao on 5/24/2022
 */
public class VSwitchOvsManagerImpl implements Component, VSwitchManager {
    private static final CLogger logger = Utils.getLogger(VSwitchOvsManagerImpl.class);

    @Autowired
    private ResourceConfigFacade rcf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        setupGlobalConfig();
        return true;
    }

    private void setupGlobalConfig() {
        initValidateExtension();
        initUpdateExtension();
    }

    private void initUpdateExtension() {
        ResourceConfig config = rcf.getResourceConfig(OvsGlobalConfig.OVS_CPU_PINNING.getIdentity());
        config.installLocalUpdateExtension(new ResourceConfigUpdateExtensionPoint() {
            @Override
            public void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue) {
                updateOvsCpuPinning(config, resourceUuid, resourceType, oldValue, newValue);
            }
        });

        config.installLocalDeleteExtension(new ResourceConfigDeleteExtensionPoint() {
            @Override
            public void deleteResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String originValue) {
                setOvsCpuPinning(config, resourceUuid, resourceType);
            }
        });
    }

    private void updateOvsCpuPinning(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue){
        if (!newValue.matches("[(\\d+;)(\\d+)]*")) {
            config.updateValue(resourceUuid, oldValue);
            throw new OperationFailureException(operr("ovs cpu pinning resource config:[%s] format error.", newValue));
        }
        setOvsCpuPinning(config, resourceUuid, resourceType);
    }

    private void setOvsCpuPinning(ResourceConfig config, String resourceUuid, String resourceType) {
        List<String> hostUuids = new ArrayList<>();
        if (resourceType.equals(ClusterVO.class.getSimpleName())) {
            hostUuids = Q.New(HostVO.class)
                    .select(HostVO_.uuid)
                    .eq(HostVO_.clusterUuid, resourceUuid)
                    .eq(HostVO_.status, HostStatus.Connected)
                    .listValues();
        } else if (resourceType.equals(HostVO.class.getSimpleName())) {
            if (!Q.New(HostVO.class).eq(HostVO_.status, HostStatus.Connected).eq(HostVO_.uuid, resourceUuid).isExists()) {
                return;
            }
            hostUuids.add(resourceUuid);
        }

        if (hostUuids.isEmpty()) {
            return;
        }

        List<UpdateHostOvsPmdPinningMsg> msgs = new ArrayList<>();
        for (String hostUuid : hostUuids) {
            UpdateHostOvsPmdPinningMsg msg = new UpdateHostOvsPmdPinningMsg();
            msg.setHostUuid(hostUuid);
            msg.setPmdCpuPinning(config.getResourceConfigValue(hostUuid, String.class));
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hostUuid);
            msgs.add(msg);
        }
        FutureCompletion completion = new FutureCompletion(null);
        ErrorCodeList errList = new ErrorCodeList();
        new While<>(msgs).each((msg, complet) -> {
            bus.send(msg, new CloudBusCallBack(complet) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        errList.getCauses().add(reply.getError());
                        logger.warn(String.format("failed to update ovs pmd cpu pinning on host:%s", msg.getHostUuid()));
                    }
                    complet.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                } else {
                    completion.success();
                }
            }
        });
        completion.await(TimeUnit.SECONDS.toMillis(600));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    private void initValidateExtension() {
    }

    @Override
    public boolean stop() {
        return true;
    }
}
