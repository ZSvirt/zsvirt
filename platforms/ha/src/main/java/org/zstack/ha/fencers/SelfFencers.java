package org.zstack.ha.fencers;

import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.ha.HaHelper;
import org.zstack.ha.header.HaGlobalConfig;
import org.zstack.ha.message.SelfFencerStruct;
import org.zstack.ha.spi.HaHypervisorFactory;
import org.zstack.ha.spi.SelfFencerHypervisorBackend;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.zstack.utils.CollectionUtils.transform;

public class SelfFencers {
    private static final CLogger logger = Utils.getLogger(SelfFencers.class);

    public interface SelfFencerOperator {
        void operate(SelfFencerStruct param);
    }

    public static class SelfFencerParamGenerator {
        public SelfFencerOperator operator;
        public List<String> clusterUuids;

        public void run() {
            DebugUtils.Assert(operator != null, "SelfFencerOperator must be set");

            if (clusterUuids == null) {
                clusterUuids = Q.New(ClusterVO.class)
                        .select(ClusterVO_.uuid)
                        .listValues();
            }

            if (clusterUuids.isEmpty()) {
                return;
            }

            List<SelfFencerStruct> structList = new ArrayList<>();
            for (String clusterUuid : clusterUuids) {
                List<PrimaryStorageVO> ps = Q.New(PrimaryStorageClusterRefVO.class, PrimaryStorageVO.class)
                    .table0()
                        .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                        .eq(PrimaryStorageClusterRefVO_.primaryStorageUuid).table1(PrimaryStorageVO_.uuid)
                    .table1()
                        .selectThisTable()
                    .list();

                if (ps.isEmpty()) {
                    continue;
                }

                List<PrimaryStorageInventory> psInvs = PrimaryStorageInventory.valueOf(ps);

                final int STEP = 256;
                int offset = 0;
                while (true) {
                    List<HostVO> vos = Q.New(HostVO.class)
                            .eq(HostVO_.clusterUuid, clusterUuid)
                            .eq(HostVO_.status, HostStatus.Connected)
                            .start(offset)
                            .limit(STEP)
                            .list();

                    for (HostInventory host : HostInventory.valueOf(vos)) {
                        List<String> fencers = HaHelper.findExecuteFencers();
                        for (PrimaryStorageInventory psInv : psInvs) {
                            SelfFencerStruct param = new SelfFencerStruct();
                            param.host = host;
                            param.primaryStorages = Collections.singletonList(psInv);
                            param.strategy = HaStrategyHelper.getPrimaryStorageSelfFencerStrategy(psInv.getUuid(), psInv.getType()).toString();
                            param.fencers = fencers;
                            structList.add(param);
                        }
                    }

                    if (vos.size() < STEP) {
                        break;
                    }
                    offset += STEP;
                }
            }

            if (structList.isEmpty()) {
                return;
            }

            new While<>(structList).step((struct, whileCompletion) -> {
                operator.operate(struct);
                whileCompletion.done();
            }, 16).run(new WhileDoneCompletion(null) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    logger.warn("some self-fencer generator occur errors:\n" +
                            String.join("\n", transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails)));
                }
            });
        }
    }

    public static class CancelStorageHeartbeatFileAndGatewayPing {
        private final SelfFencerStruct param;

        public CancelStorageHeartbeatFileAndGatewayPing(SelfFencerStruct param) {
            this.param = param;
        }

        public void cancel() {
            final HostInventory host = param.host;
            final HaHypervisorFactory f = findHaHypervisorFactory(host.getHypervisorType());
            if (f == null) {
                logger.warn(String.format("cannot find HaHypervisorFactory for the hypervisor type[%s], no need to cancel its self-fencer",
                        host.getHypervisorType()));
                return;
            }

            SelfFencerHypervisorBackend bkd = f.createSelfFencerBackend(param);
            bkd.cancel(new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully cancelled the self-fencer on the host[uuid:%s, name:%s]", host.getUuid(), host.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("unable to cancel the self-fencer on the host[uuid:%s, name:%s], %s",
                            host.getUuid(), host.getName(), errorCode));
                }
            });
        }
    }

    public static class SetupStorageHeartbeatFileFencer {
        private final SelfFencerStruct param;
        public boolean setupSkipped;

        public SetupStorageHeartbeatFileFencer(SelfFencerStruct param) {
            this.param = param;
        }

        public void setup() {
            setup(new NopeCompletion());
        }

        public void setup(Completion completion) {
            if (!HaHelper.isVmHaEnabled()) {
                setupSkipped = true;
                logger.debug(String.format("the global config[%s] is set to false, skip setting up self-fencer on the hosts",
                        HaGlobalConfig.ALL.getCanonicalName()));
                completion.success();
                return;
            }

            final HostInventory host = param.host;
            final HaHypervisorFactory f = findHaHypervisorFactory(host.getHypervisorType());
            if (f == null) {
                setupSkipped = true;
                logger.warn(String.format("cannot find HaHypervisorFactory for the hypervisor type[%s], no way to setup self-fencer",
                        host.getHypervisorType()));
                completion.success();
                return;
            }

            SelfFencerHypervisorBackend bkd = f.createSelfFencerBackend(param);
            bkd.setup(new Completion(completion) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully setup the self-fencer on the host[uuid:%s, name:%s]", host.getUuid(),
                            host.getName()));
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to setup the self-fencer on the host[uuid:%s, name:%s], %s", host.getUuid(),
                            host.getName(), errorCode.getReadableDetails()));
                    completion.fail(errorCode);
                }
            });
        }
    }

    public static class SyncStorageFencerState {
        private final SelfFencerStruct param;

        public SyncStorageFencerState(SelfFencerStruct param) {
            this.param = param;
        }

        public void sync() {
            if (!HaHelper.isVmHaEnabled()) {
                logger.debug(String.format("the global config[%s] is set to false, skip sync self-fencer from host",
                        HaGlobalConfig.ALL.getCanonicalName()));
                return;
            }

            final HostInventory host = param.host;
            final HaHypervisorFactory f = findHaHypervisorFactory(host.getHypervisorType());
            if (f == null) {
                logger.warn(String.format("cannot find HaHypervisorFactory for the hypervisor type[%s], no way to sync self-fencer state",
                        host.getHypervisorType()));
                return;
            }

            SelfFencerHypervisorBackend bkd = f.createSelfFencerBackend(param);
            bkd.sync(new Completion(null) {
                @Override
                public void success() {
                    logger.debug(String.format("successfully sync the self-fencer state from the host[uuid:%s, name:%s]",
                            host.getUuid(), host.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    logger.warn(String.format("failed to sync the self-fencer state from the host[uuid:%s, name:%s], %s",
                            host.getUuid(), host.getName(), errorCode));
                }
            });
        }
    }

    // ------------------------------------- Utility Handers ------------------------------------

    private static HaHypervisorFactory findHaHypervisorFactory(String hypervisorType) {
        List<HaHypervisorFactory> extensionList = Platform.getComponentLoader().getComponent(PluginRegistry.class)
                .getExtensionList(HaHypervisorFactory.class);
        for (HaHypervisorFactory factory : extensionList) {
            if (Objects.equals(factory.getHypervisorType(), hypervisorType)) {
                return factory;
            }
        }
        return null;
    }
}
