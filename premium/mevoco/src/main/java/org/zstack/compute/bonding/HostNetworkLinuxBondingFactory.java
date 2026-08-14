package org.zstack.compute.bonding;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.compute.host.MevocoKVMAgentCommands;
import org.zstack.compute.host.MevocoKVMConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.*;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.*;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingType;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostNetworkLinuxBondingFactory implements HostNetworkBondingFactory {
    protected static final CLogger logger = Utils.getLogger(HostNetworkLinuxBondingFactory.class);
    private static HostNetworkBondingType type = new HostNetworkBondingType(HostNetworkBondingConstant.LINUX_BONDING_TYPE);

    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private static final int BONDING_SPEED_REFRESH_RETRY_TIMES = 5;
    private static final long BONDING_SPEED_REFRESH_DELAY_SECONDS = 3L;

    @Override
    public HostNetworkBondingType getType() {
        return type;
    }

    private void refreshBondingSpeedAsync(HostNetworkBondingInventory bondingInv) {
        refreshBondingSpeedAsync(bondingInv, 1);
    }

    private void refreshBondingSpeedAsync(HostNetworkBondingInventory bondingInv, int attempt) {
        thdf.submitTimeoutTask(() -> doRefreshBondingSpeed(bondingInv, attempt), TimeUnit.SECONDS, BONDING_SPEED_REFRESH_DELAY_SECONDS);
    }

    private void doRefreshBondingSpeed(HostNetworkBondingInventory bondingInv, int attempt) {
        if (!Q.New(HostNetworkBondingVO.class)
                .eq(HostNetworkBondingVO_.hostUuid, bondingInv.getHostUuid())
                .eq(HostNetworkBondingVO_.bondingName, bondingInv.getBondingName())
                .isExists()) {
            return;
        }

        MevocoKVMAgentCommands.GetHostBondingFactsCmd cmd = new MevocoKVMAgentCommands.GetHostBondingFactsCmd();
        cmd.setBondName(bondingInv.getBondingName());
        cmd.setManagementServerIp(Platform.getManagementServerIp());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(bondingInv.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.GET_HOST_BONDING_FACTS);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, bondingInv.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    rescheduleBondingSpeedRefresh(bondingInv, attempt,
                            String.format("call agent failed: %s", reply.getError()));
                    return;
                }

                KVMHostAsyncHttpCallReply rly = reply.castReply();
                MevocoKVMAgentCommands.GetHostBondingFactsResponse rsp = rly.toResponse(MevocoKVMAgentCommands.GetHostBondingFactsResponse.class);
                if (!rsp.isSuccess()) {
                    rescheduleBondingSpeedRefresh(bondingInv, attempt, rsp.getError());
                    return;
                }

                HostNetworkBondingInventory refreshed = rsp.getBonding();
                Long speed = refreshed == null ? null : refreshed.getSpeed();
                if (speed == null || speed <= 0) {
                    rescheduleBondingSpeedRefresh(bondingInv, attempt,
                            String.format("bonding[%s] speed is not ready", bondingInv.getBondingName()));
                    return;
                }

                bondingInv.setSpeed(speed);
                SQL.New(HostNetworkBondingVO.class)
                        .eq(HostNetworkBondingVO_.hostUuid, bondingInv.getHostUuid())
                        .eq(HostNetworkBondingVO_.bondingName, bondingInv.getBondingName())
                        .set(HostNetworkBondingVO_.speed, speed)
                        .update();
            }
        });
    }

    private void rescheduleBondingSpeedRefresh(HostNetworkBondingInventory bondingInv, int attempt, String reason) {
        if (attempt >= BONDING_SPEED_REFRESH_RETRY_TIMES) {
            logger.debug(String.format("stop refreshing speed for bonding[name:%s, hostUuid:%s] after %s attempts, reason: %s",
                    bondingInv.getBondingName(), bondingInv.getHostUuid(), attempt, reason));
            return;
        }

        logger.debug(String.format("retry refreshing speed for bonding[name:%s, hostUuid:%s], attempt: %s, reason: %s",
                bondingInv.getBondingName(), bondingInv.getHostUuid(), attempt + 1, reason));
        refreshBondingSpeedAsync(bondingInv, attempt + 1);
    }

    @Override
    public void createBonding(final HostNetworkBondingInventory bondingInv, final Completion completion) {
        final MevocoKVMAgentCommands.CreateBondingCmd cmd = new MevocoKVMAgentCommands.CreateBondingCmd();
        cmd.setBondName(bondingInv.getBondingName());
        cmd.setSlaves(bondingInv.getSlaves());
        cmd.setMode(bondingInv.getMode());
        cmd.setXmitHashPolicy(bondingInv.getXmitHashPolicy());
        cmd.setType(HostNetworkBondingConstant.LINUX_BONDING_TYPE);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(bondingInv.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.ADD_BONDING_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, bondingInv.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to add linux bonding to host[uuid:%s] : %s",
                            bondingInv.getHostUuid(), reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    MevocoKVMAgentCommands.CreateBondingRsp rsp = rly.toResponse(MevocoKVMAgentCommands.CreateBondingRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to add linux bonding to host[uuid:%s] : %s",
                                bondingInv.getHostUuid(), rsp.getError()));
                        return;
                    } else {
                        refreshBondingSpeedAsync(bondingInv);
                        logger.debug(String.format("successfully add linux bonding to host[uuid:%s]",
                                bondingInv.getHostUuid()));
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void updateBonding(HostNetworkBondingInventory preBondingInv, HostNetworkBondingInventory bondingInv, Completion completion) {
        final MevocoKVMAgentCommands.UpdateBondingCmd cmd = new MevocoKVMAgentCommands.UpdateBondingCmd();
        cmd.setBondName(bondingInv.getBondingName());
        cmd.setOldSlaves(preBondingInv.getSlaves());
        cmd.setSlaves(bondingInv.getSlaves());
        cmd.setMode(bondingInv.getMode());
        cmd.setXmitHashPolicy(bondingInv.getXmitHashPolicy());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(bondingInv.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.UPDATE_BONDING_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, bondingInv.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to update linux bonding on host[uuid:%s] : %s",
                            bondingInv.getHostUuid(), reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    MevocoKVMAgentCommands.UpdateBondingRsp rsp = rly.toResponse(MevocoKVMAgentCommands.UpdateBondingRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to update linux bonding on host[uuid:%s] : %s",
                                bondingInv.getHostUuid(), rsp.getError()));
                        return;
                    } else {
                        refreshBondingSpeedAsync(bondingInv);
                        logger.debug(String.format("successfully update linux bonding on host[uuid:%s]",
                                bondingInv.getHostUuid()));
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void deleteBonding(HostNetworkBondingInventory bondingInv, Completion completion) {
        final MevocoKVMAgentCommands.DeleteBondingCmd cmd = new MevocoKVMAgentCommands.DeleteBondingCmd();
        cmd.setBondName(bondingInv.getBondingName());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(bondingInv.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.DEL_BONDING_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, bondingInv.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to remove linux bonding from host[uuid:%s] : %s",
                            bondingInv.getHostUuid(), reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    MevocoKVMAgentCommands.DeleteBondingRsp rsp = rly.toResponse(MevocoKVMAgentCommands.DeleteBondingRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to remove linux bonding from host[uuid:%s] : %s",
                                bondingInv.getHostUuid(), rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully remove linux bonding from host[uuid:%s]",
                                bondingInv.getHostUuid()));
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void attachNicToBonding(HostNetworkBondingInventory bondingInv, List<HostNetworkInterfaceInventory> slaves, Completion completion) {
        final MevocoKVMAgentCommands.AttachNicToBondingCmd cmd = new MevocoKVMAgentCommands.AttachNicToBondingCmd();
        cmd.setBondName(bondingInv.getBondingName());
        cmd.setSlaves(slaves);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(bondingInv.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.ATTACH_NIC_TO_BONDING_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, bondingInv.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to attach nic to linux bonding on host[uuid:%s] : %s",
                            bondingInv.getHostUuid(), reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    MevocoKVMAgentCommands.AttachNicToBondingRsp rsp = rly.toResponse(MevocoKVMAgentCommands.AttachNicToBondingRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to attach nic to linux bonding on host[uuid:%s] : %s",
                                bondingInv.getHostUuid(), rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully attach nic to linux bonding on host[uuid:%s]",
                                bondingInv.getHostUuid()));
                    }
                }

                completion.success();
            }
        });
    }

    @Override
    public void detachNicFromBonding(HostNetworkBondingInventory bondingInv, List<HostNetworkInterfaceInventory> slaves, Completion completion) {
        final MevocoKVMAgentCommands.DetachNicFromBondingCmd cmd = new MevocoKVMAgentCommands.DetachNicFromBondingCmd();
        cmd.setBondName(bondingInv.getBondingName());
        cmd.setSlaves(slaves);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setHostUuid(bondingInv.getHostUuid());
        kmsg.setCommand(cmd);
        kmsg.setPath(MevocoKVMConstant.DETACH_NIC_FROM_BONDING_PATH);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, bondingInv.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(operr("failed to detach nic from linux bonding on host[uuid:%s] : %s",
                            bondingInv.getHostUuid(), reply.getError()));
                    return;
                } else {
                    KVMHostAsyncHttpCallReply rly = reply.castReply();
                    MevocoKVMAgentCommands.DetachNicFromBondingRsp rsp = rly.toResponse(MevocoKVMAgentCommands.DetachNicFromBondingRsp.class);
                    if (!rsp.isSuccess()) {
                        completion.fail(operr("failed to detach nic from linux bonding on host[uuid:%s] : %s",
                                bondingInv.getHostUuid(), rsp.getError()));
                        return;
                    } else {
                        logger.debug(String.format("successfully detach nic from  linux bonding on host[uuid:%s]",
                                bondingInv.getHostUuid()));
                    }
                }

                completion.success();
            }
        });
    }
}
