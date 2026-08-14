package org.zstack.network.hostNetworkInterface.lldp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.Platform;
import org.zstack.core.db.SQL;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;
import org.zstack.network.hostNetworkInterface.*;
import org.zstack.network.hostNetworkInterface.lldp.api.*;
import org.zstack.network.hostNetworkInterface.lldp.entity.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

public class LldpManagerImpl extends AbstractService implements HostAfterConnectedExtensionPoint, HostDeleteExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LldpManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private final Map<String, Object> interfaceLocks = new ConcurrentHashMap<>();

    private Object getInterfaceLock(String interfaceUuid) {
        return interfaceLocks.computeIfAbsent(interfaceUuid, k -> new Object());
    }

    @Override
    public int getSyncLevel() {
        return super.getSyncLevel();
    }

    @MessageSafe
    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIChangeHostNetworkInterfaceLldpModeMsg) {
            handle((APIChangeHostNetworkInterfaceLldpModeMsg) msg);
        } else if (msg instanceof APIGetHostNetworkInterfaceLldpMsg) {
            handle((APIGetHostNetworkInterfaceLldpMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeHostNetworkInterfaceLldpModeMsg msg) {
        APIChangeHostNetworkInterfaceLldpModeEvent event = new APIChangeHostNetworkInterfaceLldpModeEvent(msg.getId());

        final LldpKvmAgentCommands.ChangeLldpModeCmd cmd = new LldpKvmAgentCommands.ChangeLldpModeCmd();
        List <HostNetworkInterfaceVO> interfaceVOS = Q.New(HostNetworkInterfaceVO.class)
                .in(HostNetworkInterfaceVO_.uuid, msg.getInterfaceUuids())
                .list();
        List<String> interfaceNames = interfaceVOS.stream().map(HostNetworkInterfaceVO::getInterfaceName).collect(Collectors.toList());
        String hostUuid = interfaceVOS.get(0).getHostUuid();
        cmd.setPhysicalInterfaceNames(interfaceNames);
        cmd.setMode(msg.getMode());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.CHANGE_LLDP_MODE_PATH);
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setSuccess(false);
                    event.setError(reply.getError());
                    bus.publish(event);
                    return;
                } else {
                    List<HostNetworkInterfaceLldpVO> toCreate = new ArrayList<>();
                    List<HostNetworkInterfaceLldpVO> toUpdate = new ArrayList<>();

                    for (HostNetworkInterfaceVO interfaceVO : interfaceVOS) {
                        HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceVO.getUuid()).find();
                        if (vo == null) {
                            vo = new HostNetworkInterfaceLldpVO();
                            vo.setUuid(Platform.getUuid());
                            vo.setInterfaceUuid(interfaceVO.getUuid());
                            vo.setMode(msg.getMode());
                            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                            toCreate.add(vo);
                        } else {
                            vo.setMode(msg.getMode());
                            toUpdate.add(vo);
                        }
                    }

                    if (!toCreate.isEmpty()) {
                        dbf.persistCollection(toCreate);
                    }
                    if (!toUpdate.isEmpty()) {
                        dbf.updateCollection(toUpdate);
                    }
                    List<HostNetworkInterfaceLldpVO> combinedList = new ArrayList<>(toCreate);
                    combinedList.addAll(toUpdate);
                    event.setInventories(HostNetworkInterfaceLldpInventory.valueOf(combinedList));
                }
                bus.publish(event);
            }
        });
    }

    private void copyInventoryToVO(HostNetworkInterfaceLldpRefVO vo, LldpInfoStruct inv) {
        vo.setChassisId(inv.getChassisId());
        vo.setTimeToLive(inv.getTimeToLive());
        vo.setManagementAddress(inv.getManagementAddress());
        vo.setSystemName(inv.getSystemName());
        vo.setSystemDescription(inv.getSystemDescription());
        vo.setSystemCapabilities(inv.getSystemCapabilities());
        vo.setPortId(inv.getPortId());
        vo.setPortDescription(inv.getPortDescription());
        vo.setVlanId(inv.getVlanId());
        vo.setAggregationPortId(inv.getAggregationPortId());
        vo.setMtu(inv.getMtu());
    }

    private void syncHostNetworkInterfaceLldpInDb(String interfaceUuid, LldpInfoStruct lldpInfo) {
        if (lldpInfo == null) {
            return;
        }

        Object interfaceLock = getInterfaceLock(interfaceUuid);
        synchronized (interfaceLock) {
            HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuid).find();
            HostNetworkInterfaceLldpRefVO refVO = Q.New(HostNetworkInterfaceLldpRefVO.class).eq(HostNetworkInterfaceLldpRefVO_.lldpUuid, vo.getUuid()).find();
            if (refVO == null) {
                refVO = new HostNetworkInterfaceLldpRefVO();
                refVO.setLldpUuid(vo.getUuid());
                copyInventoryToVO(refVO, lldpInfo);
                dbf.persistAndRefresh(refVO);
            } else {
                copyInventoryToVO(refVO, lldpInfo);
                // explicitly update the data to indicate the last refresh time
                refVO.setLastOpDate(new Timestamp(System.currentTimeMillis()));
                dbf.updateAndRefresh(refVO);
            }
        }
    }

    private void handle(APIGetHostNetworkInterfaceLldpMsg msg) {
        APIGetHostNetworkInterfaceLldpReply greply = new APIGetHostNetworkInterfaceLldpReply();

        HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(msg.getInterfaceUuid(), HostNetworkInterfaceVO.class);
        final LldpKvmAgentCommands.GetLldpInfoCmd cmd = new LldpKvmAgentCommands.GetLldpInfoCmd();
        cmd.setPhysicalInterfaceName(interfaceVO.getInterfaceName());

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.GET_LLDP_INFO_PATH);
        kmsg.setHostUuid(interfaceVO.getHostUuid());
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, interfaceVO.getHostUuid());
        bus.send(kmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    greply.setError(reply.getError());
                    bus.reply(msg, greply);
                    return;
                } else {
                    KVMHostAsyncHttpCallReply r = reply.castReply();
                    LldpKvmAgentCommands.GetLldpInfoResponse rsp = r.toResponse(LldpKvmAgentCommands.GetLldpInfoResponse.class);
                    if (!rsp.isSuccess()) {
                        greply.setError(operr("operation error, because %s", rsp.getError()));
                    } else {
                        HostNetworkInterfaceLldpVO vo = Q.New(HostNetworkInterfaceLldpVO.class).eq(HostNetworkInterfaceLldpVO_.interfaceUuid, msg.getInterfaceUuid()).find();

                        syncHostNetworkInterfaceLldpInDb(msg.getInterfaceUuid(), rsp.getLldpInfo());
                        HostNetworkInterfaceLldpRefVO lldpRefVO =  Q.New(HostNetworkInterfaceLldpRefVO.class)
                                .eq(HostNetworkInterfaceLldpRefVO_.lldpUuid, vo.getUuid())
                                .find();
                        greply.setLldp(lldpRefVO != null ? HostNetworkInterfaceLldpRefInventory.valueOf(lldpRefVO) : null);
                    }
                }
                bus.reply(msg, greply);
            }
        });
    }

    private void applyHostNetworkLldpConfig(List<HostNetworkInterfaceLldpVO> lldpVOS, String hostUuid, Completion completion) {
        LldpKvmAgentCommands.ApplyLldpConfigCmd cmd = new LldpKvmAgentCommands.ApplyLldpConfigCmd();
        List<LldpConfigSyncStruct.LldpModeConfig> configs = new ArrayList<>();
        for (HostNetworkInterfaceLldpVO lldpVO : lldpVOS) {
            LldpConfigSyncStruct.LldpModeConfig config = new LldpConfigSyncStruct.LldpModeConfig();
            HostNetworkInterfaceVO interfaceVO = dbf.findByUuid(lldpVO.getInterfaceUuid(), HostNetworkInterfaceVO.class);
            config.setPhysicalInterfaceName(interfaceVO.getInterfaceName());
            config.setMode(lldpVO.getMode());
            configs.add(config);
        }
        cmd.setLldpConfig(configs);

        KVMHostAsyncHttpCallMsg kmsg = new KVMHostAsyncHttpCallMsg();
        kmsg.setPath(LldpConstant.APPLY_LLDP_CONFIG_PATH);
        kmsg.setHostUuid(hostUuid);
        kmsg.setCommand(cmd);
        bus.makeTargetServiceIdByResourceUuid(kmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(kmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        List<String> interfaceUuids = Q.New(HostNetworkInterfaceLldpVO.class)
                .select(HostNetworkInterfaceLldpVO_.interfaceUuid)
                .listValues();
        List<String> interfaceUuidsOnHost = Q.New(HostNetworkInterfaceVO.class)
                .select(HostNetworkInterfaceVO_.uuid)
                .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                .listValues();

        if (interfaceUuids != null && !interfaceUuids.isEmpty()) {
            interfaceUuids = Q.New(HostNetworkInterfaceVO.class)
                    .select(HostNetworkInterfaceVO_.uuid)
                    .eq(HostNetworkInterfaceVO_.hostUuid, host.getUuid())
                    .notIn(HostNetworkInterfaceVO_.uuid, interfaceUuids)
                    .listValues();
        } else {
            interfaceUuids = interfaceUuidsOnHost;
        }

        List<HostNetworkInterfaceLldpVO> lldpVOS = new ArrayList<>();
        for (String interfaceUuid : interfaceUuids) {
            HostNetworkInterfaceLldpVO vo = new HostNetworkInterfaceLldpVO();
            vo.setUuid(Platform.getUuid());
            vo.setInterfaceUuid(interfaceUuid);
            vo.setMode(LldpConstant.mode.rx_only.toString());
            vo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            lldpVOS.add(vo);
        }
        dbf.persistCollection(lldpVOS);

        if (interfaceUuidsOnHost.isEmpty()) {
            return;
        }

        lldpVOS = Q.New(HostNetworkInterfaceLldpVO.class)
                .in(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuidsOnHost)
                .list();
        if (lldpVOS.isEmpty()) {
            return;
        }

        applyHostNetworkLldpConfig(lldpVOS, host.getUuid(), new Completion(null) {
            @Override
            public void success() {
                logger.debug("apply the lldp configuration after host reconnected successfully");
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("fail to apply the lldp configuration after host reconnected:%n%s",
                        errorCode.getReadableDetails()));
            }
        });
    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory inventory) {
        List<String> interfaceUuidsOnHost = Q.New(HostNetworkInterfaceVO.class)
                .select(HostNetworkInterfaceVO_.uuid)
                .eq(HostNetworkInterfaceVO_.hostUuid, inventory.getUuid())
                .listValues();
        if (interfaceUuidsOnHost == null || interfaceUuidsOnHost.isEmpty()) {
            return;
        }
        List<String> lldpUuidsOnHost = Q.New(HostNetworkInterfaceLldpVO.class)
                .select(HostNetworkInterfaceLldpVO_.uuid)
                .in(HostNetworkInterfaceLldpVO_.interfaceUuid, interfaceUuidsOnHost)
                .listValues();
        SQL.New(HostNetworkInterfaceLldpVO.class)
                .in(HostNetworkInterfaceLldpVO_.uuid, lldpUuidsOnHost)
                .hardDelete();
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {

    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LldpConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

}
