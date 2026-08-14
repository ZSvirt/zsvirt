package org.zstack.routeProtocol;


import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.protocol.*;
import org.zstack.header.vpc.ha.VpcHaGroupConstants;
import org.zstack.header.vpc.ha.VpcHaGroupInventory;
import org.zstack.header.vpc.ha.VpcHaGroupVO;
import org.zstack.header.vpc.ha.VpcHaGroupVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.vpc.ha.VpcHaGroupOperator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class RouteProtocolNetworkCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(RouteProtocolNetworkCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private RouterProtocolConfigProxy proxy;

    private static final String NAME = NetworkRouterAreaRefVO.class.getSimpleName();
    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                RouterAreaVO.class.getSimpleName(),
                ApplianceVmVO.class.getSimpleName(),
                L3NetworkVO.class.getSimpleName(),
                VpcHaGroupVO.class.getSimpleName()
        );
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<NetworkRouterAreaRefInventory> ctx = RouteProtocolNetworkRefFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }
    private void handleDeletion(CascadeAction action, final Completion completion) {
        final List<NetworkRouterAreaRefInventory> invs = RouteProtocolNetworkRefFromAction(action);
        if (invs == null || invs.isEmpty()) {
            completion.success();
            return;
        }

        List<String> uuids = invs.stream().map(NetworkRouterAreaRefInventory::getUuid).collect(Collectors.toList());
        logger.debug(String.format("remove networks from OSPF: %s", uuids));

        dbf.removeByPrimaryKeys(uuids, NetworkRouterAreaRefVO.class);

        if (ApplianceVmVO.class.getSimpleName().equals(action.getRootIssuer())
                || VpcHaGroupVO.class.getSimpleName().equals(action.getRootIssuer())) {
            completion.success();
            return;
        }

        List<String> vRouterUuids = new ArrayList<>();
        for (NetworkRouterAreaRefInventory inv : invs) {
            if (!inv.getApplianceVmType().equals(VpcHaGroupConstants.VPCHA_GROUP_VROUTER_VM_TYPE)) {
                vRouterUuids.add(inv.getvRouterUuid());
            } else {
                vRouterUuids.add(proxy.getMasterVrUuid(inv.getvRouterUuid()));
            }
        }
        vRouterUuids = vRouterUuids.stream().distinct().collect(Collectors.toList());

        /*
        delete L3 or RouterArea case, refresh all the routers
         */
        RefreshRouterProtocolMsg rmsg = new RefreshRouterProtocolMsg();
        rmsg.setvRouterUuids(vRouterUuids);
        bus.makeTargetServiceIdByResourceUuid(rmsg, RouteProtocolConstants.SERVICE_ID, rmsg.getvRouterUuids().get(0));
        bus.send(rmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(reply.getError().toString());
                    completion.fail(reply.getError());
                } else {
                    logger.debug(String.format("remove networks of router [uuid:%s] from OSPF  success", rmsg.getvRouterUuids()));
                    completion.success();
                }
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private List<NetworkRouterAreaRefInventory> RouteProtocolNetworkRefFromAction(CascadeAction action) {
        if (RouterAreaVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<NetworkRouterAreaRefVO> networkRefs = new ArrayList<>();
            List<RouterAreaInventory> invs = action.getParentIssuerContext();
            if (invs != null && !invs.isEmpty()) {
                for (RouterAreaInventory inv : invs) {
                    networkRefs.addAll(Q.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.routerAreaUuid, inv.getUuid()).list());
                }
            }

            if (!networkRefs.isEmpty()) {
                return NetworkRouterAreaRefInventory.valueOf(networkRefs);
            }
        } else if (ApplianceVmVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ApplianceVmInventory> invs = action.getParentIssuerContext();
            List<NetworkRouterAreaRefVO> networkRefs = new ArrayList<>();
            if (invs != null && !invs.isEmpty()) {
                for (ApplianceVmInventory inv : invs) {
                    if (inv.isHaEnabled()) {
                        VpcHaGroupVO vo = Q.New(VpcHaGroupVO.class).eq(VpcHaGroupVO_.uuid, VpcHaGroupOperator.getVpcHaGroupUuid(inv.getUuid())).find();
                        if (vo != null && vo.getVrs().size() > 1) {
                            continue;
                        }
                    }

                    networkRefs.addAll(Q.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.vRouterUuid, inv.getUuid()).list());
                }
            }

            if (!networkRefs.isEmpty()) {
                return NetworkRouterAreaRefInventory.valueOf(networkRefs);
            }
        } else if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L3NetworkInventory> invs = action.getParentIssuerContext();
            List<NetworkRouterAreaRefVO> networkRefs = new ArrayList<>();
            if (invs != null && !invs.isEmpty()) {
                for (L3NetworkInventory inv : invs) {
                    networkRefs.addAll(Q.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.l3NetworkUuid, inv.getUuid()).list());
                }
            }

            if (!networkRefs.isEmpty()) {
                return NetworkRouterAreaRefInventory.valueOf(networkRefs);
            }

        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (VpcHaGroupVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VpcHaGroupInventory> invs = action.getParentIssuerContext();
            List<NetworkRouterAreaRefVO> networkRefs = new ArrayList<>();
            if (invs != null && !invs.isEmpty()) {
                for (VpcHaGroupInventory inv : invs) {
                    networkRefs.addAll(Q.New(NetworkRouterAreaRefVO.class).eq(NetworkRouterAreaRefVO_.vRouterUuid, inv.getUuid()).list());
                }
            }

            if (!networkRefs.isEmpty()) {
                return NetworkRouterAreaRefInventory.valueOf(networkRefs);
            }
        }
        return null;
    }

}
