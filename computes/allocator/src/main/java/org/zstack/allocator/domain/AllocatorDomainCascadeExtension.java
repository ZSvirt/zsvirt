package org.zstack.allocator.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.allocator.domain.entity.AllocatorDomainInventory;
import org.zstack.allocator.domain.entity.AllocatorDomainVO;
import org.zstack.allocator.domain.entity.AllocatorDomainVO_;
import org.zstack.allocator.domain.header.AllocatorDomainConstant;
import org.zstack.allocator.domain.header.AllocatorDomainErrors;
import org.zstack.allocator.domain.message.AllocatorDomainDeletionMsg;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.err;

public class AllocatorDomainCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(AllocatorDomainCascadeExtension.class);
    private static final String NAME = AllocatorDomainVO.class.getSimpleName();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

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

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        if (isZoneIssuer(action)) {
            List<AllocatorDomainInventory> domains = domainsFromAction(action);
            if (domains != null && !domains.isEmpty()) {
                completion.fail(err(AllocatorDomainErrors.GENERAL_ERROR,
                        "cannot delete zone because allocator domain(s) %s still exist",
                        CollectionUtils.transform(domains, AllocatorDomainInventory::getUuid))
                        .withOpaque(AllocatorDomainConstant.Opaque.ZONE_UUID, domains.get(0).getZoneUuid()));
                return;
            }
        }
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        if (isZoneIssuer(action) && !action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            completion.success();
            return;
        }

        List<AllocatorDomainInventory> domains = domainsFromAction(action);
        if (domains == null || domains.isEmpty()) {
            completion.success();
            return;
        }

        boolean forceDelete = action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE) || isAccountIssuer(action);
        List<AllocatorDomainDeletionMsg> msgs = CollectionUtils.transformAndRemoveNull(domains, inv -> {
            AllocatorDomainDeletionMsg msg = new AllocatorDomainDeletionMsg();
            msg.setUuid(inv.getUuid());
            msg.setForceDelete(forceDelete);
            bus.makeTargetServiceIdByResourceUuid(msg, AllocatorDomainConstant.SERVICE_ID, inv.getUuid());
            return msg;
        });

        bus.send(msgs, 10, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply r : replies) {
                    AllocatorDomainInventory inv = domains.get(replies.indexOf(r));
                    if (!r.isSuccess()) {
                        logger.warn(String.format("failed to delete allocator domain[uuid:%s], %s",
                                inv.getUuid(), r.getError()));
                        completion.fail(r.getError());
                        return;
                    }
                    logger.debug(String.format("deleted allocator domain[uuid:%s]", inv.getUuid()));
                }
                completion.success();
            }
        });
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(AllocatorDomainVO.class);
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(ZoneVO.class.getSimpleName(), AccountVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<AllocatorDomainInventory> domains = domainsFromAction(action);
            if (domains != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(domains);
            }
        }
        return null;
    }

    private boolean isZoneIssuer(CascadeAction action) {
        return ZoneVO.class.getSimpleName().equals(action.getParentIssuer())
                || ZoneVO.class.getSimpleName().equals(action.getRootIssuer());
    }

    private boolean isAccountIssuer(CascadeAction action) {
        return AccountVO.class.getSimpleName().equals(action.getParentIssuer())
                || AccountVO.class.getSimpleName().equals(action.getRootIssuer());
    }

    @SuppressWarnings("unchecked")
    private List<AllocatorDomainInventory> domainsFromAction(CascadeAction action) {
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ZoneInventory> zones = action.getParentIssuerContext();
            List<String> zoneUuids = CollectionUtils.transformAndRemoveNull(zones, ZoneInventory::getUuid);
            if (zoneUuids.isEmpty()) {
                return null;
            }
            List<AllocatorDomainVO> vos = Q.New(AllocatorDomainVO.class)
                    .in(AllocatorDomainVO_.zoneUuid, zoneUuids)
                    .list();
            return vos.isEmpty() ? null : AllocatorDomainInventory.valueOf(vos);
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccountInventory> accounts = action.getParentIssuerContext();
            List<String> accountUuids = CollectionUtils.transformAndRemoveNull(accounts, AccountInventory::getUuid);
            List<AllocatorDomainVO> vos = ResourceHelper.findOwnResources(AllocatorDomainVO.class, accountUuids);
            return vos.isEmpty() ? null : AllocatorDomainInventory.valueOf(vos);
        }

        return null;
    }
}
