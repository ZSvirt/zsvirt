package org.zstack.cloudformation;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.cloudformation.monitor.ResourceStackVmPortRefVO;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.ScanVmPortMsg;
import org.zstack.header.host.ScanVmPortReply;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/11/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmPortMonitorTask implements PeriodicTask {
    private static final CLogger logger = Utils.getLogger(VmPortMonitorTask.class);

    private int interval;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public VmPortMonitorTask(int interval) {
        this.interval = interval;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public String getName() {
        return "monitor-vm-port-in-ResourceStack";
    }

    @Override
    public void run() {
        thdf.chainSubmit(new ChainTask(null) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                monitor(chain);
            }

            @Override
            public String getName() {
                return "monitor-vm-port-in-ResourceStack-chaintask";
            }

            @Override
            protected int getMaxPendingTasks() {
                return 1;
            }

            @Override
            protected String getDeduplicateString() {
                return getSyncSignature();
            }
        });
    }

    private void monitor(SyncTaskChain chain) {
        List<ResourceStackVmPortRefVO> refs = Q.New(ResourceStackVmPortRefVO.class).list();

        new While<>(refs)
                .all(this::updateVmPortStatus)
                .run(new WhileDoneCompletion(chain) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        chain.next();
                    }
                });
    }

    private String getBrName(VmInstanceInventory vm) {
        String l2Uuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid).eq(L3NetworkVO_.uuid, vm.getDefaultL3NetworkUuid()).findValue();
        String brname = KVMSystemTags.L2_BRIDGE_NAME.getTokenByResourceUuid(l2Uuid, KVMSystemTags.L2_BRIDGE_NAME_TOKEN);
        if (brname == null) {
            throw new OperationFailureException(operr("cannot find l2_bridge_name of l2[%s] from systemTag", l2Uuid));
        }
        return String.format("%s_%s", brname, vm.getDefaultL3NetworkUuid());
    }

    private String getDefaultIp(VmInstanceInventory vm) {
        for (VmNicInventory nic: vm.getVmNics()) {
            if (nic.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid())) {
                return nic.getIp();
            }
        }
        throw new OperationFailureException(operr("cannot find default ip on vm[%s]", vm.getUuid()));
    }

    private void updateVmPortStatus(final ResourceStackVmPortRefVO ref, NoErrorCompletion completion) {
        VmInstanceVO vm = dbf.findByUuid(ref.getVmInstanceUuid(), VmInstanceVO.class);
        if (vm == null) {
            logger.debug(String.format("vm[%s] has been deleted", ref.getVmInstanceUuid()));
            completion.done();
            return;
        }
        if (vm.getState() != VmInstanceState.Running) {
            logger.debug(String.format("vm[%s] state is %s (expected: Running), skip port status monitor", vm.getState().toString(), ref.getVmInstanceUuid()));
            completion.done();
            return;
        }

        if (vm.getHostUuid() == null) {
            logger.debug(String.format("cannot find hostUuid on vm[%s]", ref.getVmInstanceUuid()));
            completion.done();
            return;
        }

        ScanVmPortMsg msg = new ScanVmPortMsg();
        msg.setPort(ref.getPort());
        msg.setHostUuid(vm.getHostUuid());

        VmInstanceInventory inv = VmInstanceInventory.valueOf(vm);
        msg.setBrName(getBrName(inv));
        msg.setIp(getDefaultIp(inv));

        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, vm.getHostUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    dbf.reload(ref);
                    ScanVmPortReply r = reply.castReply();
                    if (r.isSupportScan()) {
                        String status = r.getStatus().get(String.valueOf(ref.getPort()));
                        ref.setStatus(status);
                        dbf.updateAndRefresh(ref);
                    }
                }
                completion.done();
            }
        });
    }
}
