package org.zstack.routeProtocol;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.message.APIMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.protocol.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vpc.VpcRouterVmVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;

import static org.zstack.core.Platform.argerr;

/**
 *
 */
@InterceptorForService("routeProtocol")
public class RouteProtocolApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected RouterProtocolConfigProxy proxy;
    @Autowired
    private CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VmInstanceMessage) {
            VmInstanceMessage vmsg = (VmInstanceMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, RouteProtocolConstants.SERVICE_ID, vmsg.getVmInstanceUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVRouterOspfAreaMsg) {
            validate((APICreateVRouterOspfAreaMsg)msg);
        } else if (msg instanceof APIAddVRouterNetworksToOspfAreaMsg) {
            validate((APIAddVRouterNetworksToOspfAreaMsg)msg );
        } else if (msg instanceof APIUpdateVRouterOspfAreaMsg) {
            validate((APIUpdateVRouterOspfAreaMsg)msg );
        } else if (msg instanceof APIRemoveVRouterNetworksFromOspfAreaMsg) {
            validate((APIRemoveVRouterNetworksFromOspfAreaMsg)msg );
        } else if (msg instanceof APISetVRouterRouterIdMsg) {
            validate((APISetVRouterRouterIdMsg) msg );
        }

        setServiceId(msg);

        return msg;
    }



    private void validate(APICreateVRouterOspfAreaMsg msg) {
        /*
        the area id should be an IP adress, not in the table
         */
        if (!NetworkUtils.isIpv4Address(msg.getAreaId())) {
            throw new ApiMessageInterceptionException(argerr("[%s] is not formatted as IPv4 address", msg.getAreaId()));
        }

        if (msg.getAreaAuth() != null && RouterAreaAuthType.MD5.toString().equals(msg.getAreaAuth())) {
            if (msg.getKeyId() == null || msg.getPassword() == null) {
                throw new ApiMessageInterceptionException(argerr("KeyID & password must be not null when authentication type is %s", msg.getAreaAuth()));
            }
        }

        if (msg.getAreaAuth() != null && RouterAreaAuthType.Plaintext.toString().equals(msg.getAreaAuth())) {
            if (msg.getPassword() == null) {
                throw new ApiMessageInterceptionException(argerr("password must be not null when authentication type is %s", msg.getAreaAuth()));
            }

            if (msg.getPassword().length() > 8) {
                throw new ApiMessageInterceptionException(argerr("the length of password is at most than 8Bytes when authentication type is %s", msg.getAreaAuth()));

            }
        }

        if ("0.0.0.0".equals(msg.getAreaId()) && msg.getAreaType() != null
                && !RouterAreaType.Standard.toString().equals(msg.getAreaType())) {
            throw new ApiMessageInterceptionException(argerr("AreaId[%s] type must be %s", msg.getAreaId(),RouterAreaType.Standard.toString()));
        }

        RouterAreaVO vo = Q.New(RouterAreaVO.class).eq(RouterAreaVO_.areaId, msg.getAreaId()).find();
        //check areaid exist
        if ( vo != null) {
            throw new ApiMessageInterceptionException(argerr("AreaId[%s] has been created", msg.getAreaId()));
        }
    }

    private void validate(APIUpdateVRouterOspfAreaMsg msg ) {
        /*
        auth type plain-text the password parameter muster not empty, at most 8 bytes
        auth type MD5 the keyId parameter muster not empty, range in [1,255]
         */
        if( msg.getAreaAuth() != null && RouterAreaAuthType.MD5.toString().equals(msg.getAreaAuth())) {
            if (msg.getKeyId() == null || msg.getPassword() == null) {
                throw new ApiMessageInterceptionException(argerr("KeyID & password must be not null when authentication type is %s", msg.getAreaAuth()));
            }
        }

        if( msg.getAreaAuth() != null && RouterAreaAuthType.Plaintext.toString().equals(msg.getAreaAuth())) {
            if (msg.getPassword() == null) {
                throw new ApiMessageInterceptionException(argerr("password must be not null when authentication type is %s",msg.getAreaAuth()));
            }

            if (msg.getPassword().length() > 8) {
                throw new ApiMessageInterceptionException(argerr("the length of password is at most than 8Bytes when authentication type is %s", msg.getAreaAuth()));

            }
        }

        RouterAreaVO vo = Q.New(RouterAreaVO.class).eq(RouterAreaVO_.uuid, msg.getUuid()).find();
        //check areaid type
        if ( vo != null && "0.0.0.0".equals(vo.getAreaId()) && msg.getAreaType() != null
                && !RouterAreaType.Standard.toString().equals(msg.getAreaType())) {
            throw new ApiMessageInterceptionException(argerr("AreaId[%s] type must be %s", vo.getAreaId(),RouterAreaType.Standard.toString()));
        }

    }


    private void validate(APIAddVRouterNetworksToOspfAreaMsg msg) {
        /*
        l3 uuids belong to the virtual router
        l3 and vrouter has not add into any area
         */
        List<L3NetworkVO> l3s = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid).eq(VmNicVO_.vmInstanceUuid, msg.getvRouterUuid()).listValues();

        if (l3s == null || l3s.isEmpty() || !l3s.containsAll(msg.getL3NetworkUuids())) {
            throw new ApiMessageInterceptionException(argerr("All the networks should be in the virtual router[%s]", msg.getvRouterUuid()));
        }

        VpcRouterVmVO vpcVo = dbf.findByUuid(msg.getvRouterUuid(), VpcRouterVmVO.class);
        if (!vpcVo.isHaEnabled()) {
            NetworkRouterAreaRefVO vo = Q.New(NetworkRouterAreaRefVO.class)
                    .eq(NetworkRouterAreaRefVO_.vRouterUuid, msg.getvRouterUuid())
                    .in(NetworkRouterAreaRefVO_.l3NetworkUuid, msg.getL3NetworkUuids()).find();
            if (vo != null) {
                throw new ApiMessageInterceptionException(argerr("The network[%s] have been added into the virtual routerArea[%s]", vo.getL3NetworkUuid(), vo.getRouterAreaUuid()));
            }
        } else {
            String haUuid = proxy.getHaUuidOfVpcRouter(msg.getvRouterUuid());
            NetworkRouterAreaRefVO vo = Q.New(NetworkRouterAreaRefVO.class)
                    .eq(NetworkRouterAreaRefVO_.vRouterUuid, haUuid)
                    .in(NetworkRouterAreaRefVO_.l3NetworkUuid, msg.getL3NetworkUuids()).find();
            if (vo != null) {
                throw new ApiMessageInterceptionException(argerr("The network[%s] have been added into the haGroup[%s]", vo.getL3NetworkUuid(), haUuid));
            }
        }

    }

    private void validate(APIRemoveVRouterNetworksFromOspfAreaMsg msg ) {
        /*
        * all the ref uuids should have the same router
        * */
        /* for the case: remove all the routers in a area, it supports to remove multi-routers' networks
        List<String> routerUuids = Q.New(NetworkRouterAreaRefVO.class).select(NetworkRouterAreaRefVO_.vRouterUuid)
                            .in(NetworkRouterAreaRefVO_.uuid,msg.getUuids()).listValues();
        routerUuids = routerUuids.stream().distinct().collect(Collectors.toList());
        if ( routerUuids != null && routerUuids.size() > 1) {
            throw new ApiMessageInterceptionException(argerr("All the networks should be in the same virtual router"));
        }
*/
    }

    private void validate(APISetVRouterRouterIdMsg msg ) {
        /*router id should be an IPv4 address and uniqueness*/
        if (!NetworkUtils.isIpv4Address(msg.getRouterId())) {
            throw new ApiMessageInterceptionException(argerr("Router ID[%s] is not formatted as IPv4 address", msg.getRouterId()));
        }

        VpcRouterVmVO vpcVo = dbf.findByUuid(msg.getvRouterUuid(), VpcRouterVmVO.class);
        if (!vpcVo.isHaEnabled()) {
            /*
            how to check the uniqueness of a system tag? this is a hack, to be done zhanong.miao
            */
            SystemTagVO tag = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, VirtualRouterVmVO.class.getSimpleName())
                    .eq(SystemTagVO_.tag, String.format("routeProtocolRouterId::%s", msg.getRouterId()))
                    .notEq(SystemTagVO_.resourceUuid, msg.getvRouterUuid()).find();
            if (tag != null) {
                throw new ApiMessageInterceptionException(argerr("Router ID[%s] is not unique in this system", msg.getRouterId()));
            }
        }
    }
}
