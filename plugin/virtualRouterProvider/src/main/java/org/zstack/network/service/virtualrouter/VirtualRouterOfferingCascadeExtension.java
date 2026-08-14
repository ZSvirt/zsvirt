package org.zstack.network.service.virtualrouter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.configuration.InstanceOfferingDeletionMsg;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageDeletionStruct;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;

import javax.persistence.Query;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class VirtualRouterOfferingCascadeExtension extends AbstractAsyncCascadeExtension {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static String NAME = VirtualRouterOfferingVO.class.getSimpleName();

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

    @Transactional
    private void deleteInstanceOfferingEONotReferredByVm() {
        String sql = "delete from InstanceOfferingEO i" +
                " where i.deleted is not null" +
                " and i.uuid not in" +
                " (" +
                " select vm.instanceOfferingUuid" +
                " from VmInstanceVO vm" +
                " where vm.instanceOfferingUuid is not null" +
                " )";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.executeUpdate();
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        deleteInstanceOfferingEONotReferredByVm();
        completion.success();
    }

    private List<VirtualRouterOfferingInventory> offeringFromAction(CascadeAction action) {
        List<VirtualRouterOfferingInventory> ret = null;
        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L3NetworkInventory> l3s = action.getParentIssuerContext();
            List<String> l3uuids = transformAndRemoveNull(l3s, L3NetworkInventory::getUuid);

            List<VirtualRouterOfferingVO> offeringVOs = Q.New(VirtualRouterOfferingVO.class)
                    .in(VirtualRouterOfferingVO_.publicNetworkUuid, l3uuids)
                    .list();
            List<VirtualRouterOfferingVO> lst = Q.New(VirtualRouterOfferingVO.class)
                    .in(VirtualRouterOfferingVO_.managementNetworkUuid, l3uuids)
                    .list();
            offeringVOs.addAll(lst);
            ret = VirtualRouterOfferingInventory.valueOf1(offeringVOs);
        } else if (ImageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> imgUuids = transformAndRemoveNull(action.getParentIssuerContext(),
                    (ImageDeletionStruct arg) -> arg.getDeleteAll() ? arg.getImage().getUuid() : null);
            if (imgUuids != null && !imgUuids.isEmpty()) {
                List<VirtualRouterOfferingVO> offeringVOs = Q.New(VirtualRouterOfferingVO.class)
                        .in(VirtualRouterOfferingVO_.imageUuid, imgUuids)
                        .list();
                ret = VirtualRouterOfferingInventory.valueOf1(offeringVOs);
            }
        }

        if (ret != null && !ret.isEmpty()) {
            return ret;
        } else {
            return null;
        }
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        List<VirtualRouterOfferingInventory> offering = offeringFromAction(action);
        if (offering == null) {
            completion.success();
            return;
        }

        List<String> offeringUuids = transformAndRemoveNull(offering, InstanceOfferingInventory::getUuid);

        List<InstanceOfferingDeletionMsg> msgs = transform(
                offeringUuids, arg -> {
                    InstanceOfferingDeletionMsg msg = new InstanceOfferingDeletionMsg();
                    msg.setInstanceOfferingUuid(arg);
                    msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
                    bus.makeTargetServiceIdByResourceUuid(msg, ConfigurationConstant.SERVICE_ID, arg);
                    return msg;
                });

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                    for (MessageReply r : replies) {
                        if (!r.isSuccess()) {
                            completion.fail(r.getError());
                            return;
                        }
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(L3NetworkVO.class.getSimpleName(), ImageVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<VirtualRouterOfferingInventory> ctx = offeringFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(VirtualRouterOfferingVO.class.getSimpleName()).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
