package org.zstack.vrouterRoute;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefVO_;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.operr;


/**
 * Created by weiwang on 17/06/2017.
 */
@InterceptorForService("vrouterRoute")
public class VRouterRouteApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(VRouterRouteApiInterceptor.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private VRouterRouteManager routeManager;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VmInstanceMessage) {
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, VRouterRouteConstants.SERVICE_ID, vmsg.getVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddVRouterRouteEntryMsg) {
            validate((APIAddVRouterRouteEntryMsg) msg);
        } else if (msg instanceof APIAttachVRouterRouteTableToVRouterMsg) {
            validate((APIAttachVRouterRouteTableToVRouterMsg) msg);
        } else if (msg instanceof APIDetachVRouterRouteTableFromVRouterMsg) {
            validate((APIDetachVRouterRouteTableFromVRouterMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIAddVRouterRouteEntryMsg msg) {
        if (msg.getType() == null && msg.getTarget() != null) {
            msg.setType(VRouterRouteEntryType.UserStatic.toString());
        } else if (msg.getType() == null && msg.getTarget() == null) {
            msg.setType(VRouterRouteEntryType.UserBlackHole.toString());
        } else if (msg.getType().equals(VRouterRouteEntryType.UserStatic.toString()) && msg.getTarget() == null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("target cannot be empty if type is UserStatic")
            ));
        } else if (msg.getType().equals(VRouterRouteEntryType.UserBlackHole.toString())  && msg.getTarget() != null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("target must be empty if type is USER_BLACK_HOLE")
            ));
        }

        if (!(msg.getType().equals(VRouterRouteEntryType.UserStatic.toString()) ||
                msg.getType().equals(VRouterRouteEntryType.UserBlackHole.toString()))) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("wrong vrouter route entry type")
            ));
        }

        if (!NetworkUtils.isCidr(msg.getDestination())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("wrong destination")
            ));
        } else if (!msg.getDestination().equals(NetworkUtils.getNetworkAddressFromCidr(msg.getDestination()))) {
            String correct = NetworkUtils.getNetworkAddressFromCidr(msg.getDestination());
            logger.debug(String.format("destination has been correct to %s", correct));
            msg.setDestination(correct);
        }

        if (msg.getTarget() != null && !NetworkUtils.isIpv4Address(msg.getTarget())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("wrong target")
            ));
        }

        if (msg.getType().equals(VRouterRouteEntryType.UserStatic.toString()) && msg.getDestination().equals("0.0.0.0/0")) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("could not allow to configure default router")
            ));
        }

        /* UserStatic and UserBlackHole can not has same prefix */
        List<VRouterRouteEntryVO> routes = Q.New(VRouterRouteEntryVO.class)
                .eq(VRouterRouteEntryVO_.routeTableUuid, msg.getRouteTableUuid())
                .eq(VRouterRouteEntryVO_.destination, msg.getDestination()).list();
        for (VRouterRouteEntryVO r : routes) {
            if ((msg.getType().equals(VRouterRouteEntryType.UserBlackHole.toString()) && r.getType() == VRouterRouteEntryType.UserStatic)
                    || (msg.getType().equals(VRouterRouteEntryType.UserStatic.toString()) && r.getType() == VRouterRouteEntryType.UserBlackHole)){
                throw new ApiMessageInterceptionException(operr("destination[%s] can not has blackHole route and static route at same time"));
            }
        }
    }

    private void validate(APIAttachVRouterRouteTableToVRouterMsg msg) {
        // TODO(WeiW): need to add SQLBatch?
        List<String> tableUuids = routeManager.getTableUuidsFromVrUuid(msg.getVirtualRouterVmUuid());
        if (tableUuids != null && !tableUuids.isEmpty()) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("vrouter [uuid: %s] has already attach route table [uuid: %s]", msg.getVirtualRouterVmUuid(), tableUuids)
            ));
        }

        List<String> newL3Uuids = SQL.New("select nic.l3NetworkUuid from VmNicVO nic, L3NetworkVO l3 where " +
                "nic.vmInstanceUuid = :vruuid and " +
                "nic.l3NetworkUuid = l3.uuid and " +
                "l3.system = false")
                .param("vruuid", msg.getVirtualRouterVmUuid()).list();

        // TODO(WeiW): Assume we support multi guest l3 and all l3s have same provider for route table network service
        String newProviderUuid = null;
        for (String newL3Uuid : newL3Uuids) {
            newProviderUuid = Q.New(NetworkServiceL3NetworkRefVO.class)
                    .select(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid)
                    .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, newL3Uuid)
                    .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, VRouterRouteConstants.VROUTER_ROUTE_NETWORK_SERVICE_TYPE.toString())
                    .findValue();
            if (newProviderUuid != null) {
                break;
            }
        }

        if (newProviderUuid == null) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("l3s[%s] in the virtual router to attach does not has VRouterRoute service attached", newL3Uuids)
            ));
        }

        List<String> vrUuids = routeManager.getVrUuidsFromTableUuid(msg.getRouteTableUuid());
        if (vrUuids.isEmpty()) {
            return;
        }

        List<String> attachedL3Uuids = SQL.New("select nic.l3NetworkUuid from VmNicVO nic, L3NetworkVO l3 where " +
                "nic.vmInstanceUuid = :vruuid and " +
                "nic.l3NetworkUuid = l3.uuid and " +
                "l3.system = false")
                .param("vruuid", vrUuids.get(0)).list();

        for (String attachedL3Uuid : attachedL3Uuids) {
            if (newProviderUuid.equals((String) Q.New(NetworkServiceL3NetworkRefVO.class)
                    .select(NetworkServiceL3NetworkRefVO_.networkServiceProviderUuid)
                    .eq(NetworkServiceL3NetworkRefVO_.l3NetworkUuid, attachedL3Uuid)
                    .eq(NetworkServiceL3NetworkRefVO_.networkServiceType, VRouterRouteConstants.VROUTER_ROUTE_NETWORK_SERVICE_TYPE.toString())
                    .findValue())) {
                return;
            }
        }

        throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                String.format("l3s[%s] in the virtual router to attach has a different network provider [%s] " +
                        "than the l3 in the virtual router already attached to this route table", newL3Uuids, newProviderUuid)
        ));
    }

    private void validate(APIDetachVRouterRouteTableFromVRouterMsg msg) {
        List<String> tableUuids = routeManager.getTableUuidsFromVrUuid(msg.getVirtualRouterVmUuid());
        if (tableUuids == null || !tableUuids.contains(msg.getRouteTableUuid())) {
            throw new ApiMessageInterceptionException(Platform.err(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("the vrouter route table [uuid: %s] not attached to vrouter [uuid: %s]", msg.getRouteTableUuid(), msg.getVirtualRouterVmUuid())
            ));
        }
    }
}
