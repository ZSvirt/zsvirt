package org.zstack.compute.bonding;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.bonding.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO_;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

@InterceptorForService("bonding")
public class HostNetworkBondingApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(HostNetworkBondingApiInterceptor.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof BondingMessage) {
            BondingMessage bmsg = (BondingMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, HostNetworkBondingConstant.SERVICE_ID, bmsg.getBondingUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateBondingMsg) {
            validate((APICreateBondingMsg) msg);
        } else if (msg instanceof APIUpdateBondingMsg) {
            validate((APIUpdateBondingMsg) msg);
        } else if (msg instanceof APIDeleteBondingMsg) {
            validate((APIDeleteBondingMsg) msg);
        } else if (msg instanceof APIAttachNicToBondingMsg) {
            validate((APIAttachNicToBondingMsg) msg);
        } else if (msg instanceof APIDetachNicFromBondingMsg) {
            validate((APIDetachNicFromBondingMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void checkBondingExistingNameOnHosts(String bondingName, List<String> hostUuids) {
        //Restrictions on bond names
        List<String> hostUuidsWithBond = Q.New(HostNetworkBondingVO.class)
                .in(HostNetworkBondingVO_.hostUuid, hostUuids)
                .eq(HostNetworkBondingVO_.bondingName, bondingName)
                .select(HostNetworkBondingVO_.hostUuid)
                .listValues();

        if (!hostUuidsWithBond.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("interface[%s] has already been created on host[%s]", bondingName, hostUuidsWithBond.get(0)));
        }
    }

    private void validate(APICreateBondingMsg msg) {
        //bondingName
        checkBondingExistingNameOnHosts(msg.getBondingName(), msg.getHostUuids());

        //slaves
        if (msg.getSlaveUuids() == null && msg.getSlaveNames() == null) {
            throw new ApiMessageInterceptionException(argerr("require one of slaveUuids and slaveNames"));
        } else if (msg.getSlaveUuids() == null && msg.getSlaveNames() != null) {
            msg.setSlaveUuids(HostNetworkBondingUtils.getSlaveUuidsBySlaveNamesOnHosts(msg.getSlaveNames(), msg.getHostUuids()));
        }

        msg.setSlavesMap(HostNetworkBondingUtils.checkAndGetBondingSlavesMapOnHosts(msg.getSlaveUuids(), msg.getHostUuids()));

        //type
        if (msg.getType() != null && !HostNetworkBondingConstant.LINUX_BONDING_TYPE.equals(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("invalid bonding type[%s]", msg.getType()));
        }

        //mode & xmitHashPolicy
        ErrorCode err = HostNetworkBondingUtils.validateBondingModeAndPolicy(msg.getMode(), msg.getXmitHashPolicy());
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }

        msg.getSlavesMap().values().forEach(slaves ->
                HostNetworkBondingUtils.checkBondingSlavesAmountWithCertainMode(slaves.size(), msg.getMode()));
    }

    private void validate(APIUpdateBondingMsg msg) {
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);

        //slaves
        if (msg.getSlaveUuids() == null && msg.getSlaveNames() != null) {
            msg.setSlaveUuids(HostNetworkBondingUtils.getSlaveUuidsBySlaveNamesOnHosts(msg.getSlaveNames(), bondingVO.getHostUuid()));
        }

        if (msg.getSlaveUuids() != null) {
            msg.setSlaveUuids(HostNetworkBondingUtils.checkAndGetBondingSlavesOnHost(
                    msg.getSlaveUuids(), bondingVO.getHostUuid(), msg.getBondingUuid()));
        }

        //type
        if (msg.getType() != null && !HostNetworkBondingConstant.LINUX_BONDING_TYPE.equals(msg.getType())) {
            throw new ApiMessageInterceptionException(argerr("invalid bonding type[%s]", msg.getType()));
        }

        //mode & xmitHashPolicy
        String mode = msg.getMode() == null ? bondingVO.getMode() : msg.getMode();
        String xmitHashPolicy = null;
        if (msg.getXmitHashPolicy() != null) {
            xmitHashPolicy = msg.getXmitHashPolicy();
        } else if (!HostNetworkBondingConstant.BONDING_MODE_AB.equals(mode)) {
            xmitHashPolicy = bondingVO.getXmitHashPolicy();
        }
        ErrorCode err = HostNetworkBondingUtils.validateBondingModeAndPolicy(mode, xmitHashPolicy);
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }

        if (msg.getSlaveUuids() != null) {
            HostNetworkBondingUtils.checkBondingSlavesAmountWithCertainMode(msg.getSlaveUuids().size(), mode);
        }
    }

    private void validate(APIDeleteBondingMsg msg) {
        if (!dbf.isExist(msg.getUuid(), HostNetworkBondingVO.class)) {
            APIDeleteBondingEvent evt = new APIDeleteBondingEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        HostNetworkBondingVO bondingVO = Q.New(HostNetworkBondingVO.class).eq(HostNetworkBondingVO_.uuid, msg.getUuid()).find();
        ErrorCode err = HostNetworkBondingUtils.validateDeleteBonding(bondingVO);
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }
    }

    private void validate(APIAttachNicToBondingMsg msg) {
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);

        //slaves check
        List<String> newSlaveUuids = bondingVO.getSlaves().stream().map(HostNetworkInterfaceVO::getUuid).distinct().collect(Collectors.toList());
        newSlaveUuids.addAll(msg.getSlaveUuids());
        Set<String> newSlaveSet = new HashSet<>(newSlaveUuids);
        if (newSlaveUuids.size() != newSlaveSet.size()) {
            throw new ApiMessageInterceptionException(argerr("can not attach interfaces repeatedly in a bond[%s].", msg.getUuid()));
        }

        HostNetworkBondingUtils.checkAndGetBondingSlavesOnHost(newSlaveUuids, bondingVO.getHostUuid(), msg.getBondingUuid());

        Integer newSlaveSize = bondingVO.getSlaves().size() + msg.getSlaveUuids().size();
        HostNetworkBondingUtils.checkBondingSlavesAmountWithCertainMode(newSlaveSize, bondingVO.getMode());
    }

    private void validate(APIDetachNicFromBondingMsg msg) {
        HostNetworkBondingVO bondingVO = dbf.findByUuid(msg.getUuid(), HostNetworkBondingVO.class);

        //slaves check
        for (String interfaceUuid : msg.getSlaveUuids()) {
            boolean isSlave = Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, interfaceUuid)
                    .eq(HostNetworkInterfaceVO_.bondingUuid, msg.getUuid()).isExists();
            if (!isSlave) {
                throw new ApiMessageInterceptionException(argerr("can not detach interface[%s] in a bond[%s] which are is bonded.", interfaceUuid, msg.getUuid()));
            }
        }

        List<String> newSlaveUuids = bondingVO.getSlaves().stream().map(HostNetworkInterfaceVO::getUuid).distinct().collect(Collectors.toList());
        newSlaveUuids.removeAll(msg.getSlaveUuids());
        Set<String> detachSlaveSet = new HashSet<>(msg.getSlaveUuids());
        if (msg.getSlaveUuids().size() != detachSlaveSet.size()) {
            throw new ApiMessageInterceptionException(argerr("can not detach interfaces repeatedly in a bond[%s].", msg.getUuid()));
        }

        Integer newSlaveSize = bondingVO.getSlaves().size() - msg.getSlaveUuids().size();
        HostNetworkBondingUtils.checkBondingSlavesAmountWithCertainMode(newSlaveSize, bondingVO.getMode());
    }
}
