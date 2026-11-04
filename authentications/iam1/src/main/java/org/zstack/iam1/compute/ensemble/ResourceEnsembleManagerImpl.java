package org.zstack.iam1.compute.ensemble;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APIRevokeResourceSharingEvent;
import org.zstack.header.identity.APIRevokeResourceSharingMsg;
import org.zstack.header.identity.APIShareResourceEvent;
import org.zstack.header.identity.APIShareResourceMsg;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.iam1.api.ensemble.APIGetResourceEnsembleMembersMsg;
import org.zstack.iam1.api.ensemble.APIGetResourceEnsembleMembersReply;
import org.zstack.iam1.api.ensemble.APIGetResourceSharingMsg;
import org.zstack.iam1.api.ensemble.APIGetResourceSharingReply;
import org.zstack.iam1.compute.rbac.IAM1RBACConstant;
import org.zstack.iam1.entity.ensemble.AccountGroupSharingView;
import org.zstack.iam1.entity.ensemble.AccountSharingView;
import org.zstack.iam1.entity.ensemble.ResourceEnsembleInventory;
import org.zstack.iam1.header.ensemble.ResourceEnsembleInfo;
import org.zstack.iam1.message.rbac.RevokeResourceSharingMsg;
import org.zstack.iam1.message.rbac.ShareResourceMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.iam1.compute.accounts.AccountGroupConstant.GROUP_FORM_UPDATE_TARGET;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/08/07
 */
public class ResourceEnsembleManagerImpl extends AbstractService {
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIGetResourceEnsembleMembersMsg) {
            handle((APIGetResourceEnsembleMembersMsg) msg);
        } else if (msg instanceof APIGetResourceSharingMsg) {
            handle((APIGetResourceSharingMsg) msg);
        } else if (msg instanceof APIShareResourceMsg) {
            handle((APIShareResourceMsg) msg);
        } else if (msg instanceof APIRevokeResourceSharingMsg) {
            handle((APIRevokeResourceSharingMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ResourceEnsembleConstant.SERVICE_ID);
    }

    private void handle(APIGetResourceEnsembleMembersMsg message) {
        APIGetResourceEnsembleMembersReply reply = new APIGetResourceEnsembleMembersReply();

        ResourceEnsembleInfo ensemble = ResourceEnsembleHelper.findResourceEnsemble(message.getUuid());
        if (ensemble == null) {
            bus.reply(message, reply);
            return;
        }

        ensemble = ResourceEnsembleHelper.findAllChildrenResources(ensemble.uuid, ensemble.resourceType());
        if (ensemble == null) {
            throw new CloudRuntimeException("should not be here: ensemble == null");
        }

        String masterUuid = ensemble.uuid;
        List<ResourceVO> resources = transform(
            Q.New(ResourceVO.class)
                    .in(ResourceVO_.uuid, transform(ensemble.flatten(), info -> info.uuid))
                    .select(ResourceVO_.uuid, ResourceVO_.resourceName, ResourceVO_.resourceType)
                    .listTuple(),
            tuple -> new ResourceVO(
                    new Object[] { tuple.get(0, String.class), tuple.get(1, String.class), tuple.get(2, String.class) })
        );

        ResourceVO masterResource = findOneOrNull(resources, resource -> masterUuid.equals(resource.getUuid()));
        resources.remove(masterResource);
        reply.setInventory(ResourceEnsembleInventory.valueOf(masterResource, resources));
        bus.reply(message, reply);
    }

    private void handle(APIGetResourceSharingMsg message) {
        APIGetResourceSharingReply reply = new APIGetResourceSharingReply();

        ResourceEnsembleInfo ensemble = ResourceEnsembleHelper.findResourceEnsemble(message.getUuid());
        if (ensemble == null) {
            bus.reply(message, reply);
            return;
        }

        reply.setUuid(message.getUuid());
        reply.setMasterUuid(ensemble.uuid);
        reply.setMasterResourceType(ensemble.resourceType().getSimpleName());

        List<AccountResourceRefVO> refs = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, message.getUuid())
                .in(AccountResourceRefVO_.type, list(AccessLevel.Share, AccessLevel.SharePublic))
                .eq(AccountResourceRefVO_.resourcePermissionFrom, reply.getMasterUuid())
                .list();
        reply.setToPublic(findOneOrNull(refs, ref -> ref.getType() == AccessLevel.SharePublic) != null);

        List<AccountResourceRefVO> shareToAccounts = filter(refs,
                ref -> ref.getAccountPermissionFrom() == null && ref.getAccountUuid() != null);
        List<AccountSharingView> accountViews = new ArrayList<>();
        reply.setAccounts(accountViews);

        for (AccountResourceRefVO shareToAccount : shareToAccounts) {
            final AccountSharingView view = new AccountSharingView();
            view.setUuid(shareToAccount.getAccountUuid());
            accountViews.add(view);
        }

        Set<String> shareToGroups = refs.stream()
                .map(AccountResourceRefVO::getAccountPermissionFrom)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<AccountGroupSharingView> groupViews = new ArrayList<>();
        reply.setAccountGroups(groupViews);

        for (String groupUuid : shareToGroups) {
            final AccountGroupSharingView view = new AccountGroupSharingView();
            view.setUuid(groupUuid);
            groupViews.add(view);
        }

        bus.reply(message, reply);
    }

    private void handle(APIShareResourceMsg message) {
        ShareResourceMsg innerMsg = ShareResourceMsg.valueOf(message);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, IAM1RBACConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIShareResourceEvent event = new APIShareResourceEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIRevokeResourceSharingMsg message) {
        RevokeResourceSharingMsg innerMsg = RevokeResourceSharingMsg.valueOf(message);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, IAM1RBACConstant.SERVICE_ID, GROUP_FORM_UPDATE_TARGET);
        bus.send(innerMsg, new CloudBusCallBack(message) {
            @Override
            public void run(MessageReply reply) {
                APIRevokeResourceSharingEvent event = new APIRevokeResourceSharingEvent(message.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }
}
