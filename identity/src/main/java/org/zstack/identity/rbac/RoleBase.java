package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.identity.role.*;
import org.zstack.header.identity.role.api.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.utils.CollectionUtils.isEmpty;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RoleBase {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    protected RoleVO self;

    public RoleBase(RoleVO self) {
        this.self = self;
    }

    protected RoleInventory getSelfInventory() {
        return RoleInventory.valueOf(self);
    }

    @MessageSafe
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
        if (msg instanceof APIDeleteRoleMsg) {
            handle((APIDeleteRoleMsg) msg);
        } else if (msg instanceof APIAttachRoleToAccountMsg) {
            handle((APIAttachRoleToAccountMsg) msg);
        } else if (msg instanceof APIDetachRoleFromAccountMsg) {
            handle((APIDetachRoleFromAccountMsg) msg);
        } else if (msg instanceof APIUpdateRoleMsg) {
            handle((APIUpdateRoleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIUpdateRoleMsg msg) {
        updateRole(RoleSpec.valueOf(msg));

        self = dbf.findByUuid(self.getUuid(), RoleVO.class);
        APIUpdateRoleEvent evt = new APIUpdateRoleEvent(msg.getId());
        evt.setInventory(RoleInventory.valueOf(self));
        bus.publish(evt);
    }

    private void handle(APIDetachRoleFromAccountMsg msg) {
        SQL.New(RoleAccountRefVO.class)
                .eq(RoleAccountRefVO_.accountUuid, msg.getAccountUuid())
                .eq(RoleAccountRefVO_.roleUuid, msg.getRoleUuid())
                .isNull(RoleAccountRefVO_.accountPermissionFrom)
                .delete();

        APIDetachRoleFromAccountEvent evt = new APIDetachRoleFromAccountEvent(msg.getId());
        bus.publish(evt);
    }

    private void handle(APIAttachRoleToAccountMsg msg) {
        RoleAccountRefVO ref = new RoleAccountRefVO();
        ref.setAccountUuid(msg.getAccountUuid());
        ref.setRoleUuid(msg.getRoleUuid());
        dbf.persist(ref);
        bus.publish(new APIAttachRoleToAccountEvent(msg.getId()));
    }

    private void handle(APIDeleteRoleMsg msg) {
        SQL.New(RoleVO.class).eq(RoleVO_.uuid, self.getUuid()).hardDelete();
        SQL.New(RolePolicyVO.class).eq(RolePolicyVO_.roleUuid, self.getUuid()).hardDelete();
        bus.publish(new APIDeleteRoleEvent(msg.getId()));
    }

    @Transactional
    private void updateRole(RoleSpec spec) {
        updateRoleNameDescription(spec);

        if (spec.isClearPoliciesBeforeUpdate()) {
            SQL.New(RolePolicyVO.class)
                    .eq(RolePolicyVO_.roleUuid, spec.getUuid())
                    .delete();
            spec.getPoliciesToDelete().clear();
        }

        if (!spec.getPoliciesToDelete().isEmpty() || !spec.getPoliciesToCreate().isEmpty()) {
            updateRolePolicy(spec);
        }
    }

    private void updateRoleNameDescription(RoleSpec spec) {
        final UpdateQuery sql = SQL.New(RoleVO.class).eq(RoleVO_.uuid, spec.getUuid());
        boolean updated = false;

        if (spec.getName() != null) {
            sql.set(RoleVO_.name, spec.getName());
            sql.set(RoleVO_.resourceName, spec.getName());
            updated = true;
        }
        if (spec.getDescription() != null) {
            sql.set(RoleVO_.description, spec.getDescription());
            updated = true;
        }
        if (updated) {
            sql.update();
        }
    }

    private void updateRolePolicy(RoleSpec spec) {
        List<RolePolicyVO> existsPolicies = Q.New(RolePolicyVO.class)
                .eq(RolePolicyVO_.roleUuid, spec.getUuid())
                .list();
        RolePolicyUpdater updater = RolePolicyUpdater.of(existsPolicies);

        for (RolePolicyStatement statement : spec.getPoliciesToDelete()) {
            updater.delete(statement);
        }

        for (RolePolicyStatement statement : spec.getPoliciesToCreate()) {
            updater.add(statement);
        }

        updater.squeeze();

        Set<Long> policyIdsNeedDelete = updater.collectAllPolicyIdsNeedDelete();
        List<RolePolicyVO> policiesNeedCreate = updater.collectAllPolicyIdsNeedCreate();
        if (policyIdsNeedDelete.isEmpty() && policiesNeedCreate.isEmpty()) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!policyIdsNeedDelete.isEmpty()) {
                    sql(RolePolicyVO.class)
                            .in(RolePolicyVO_.id, policyIdsNeedDelete)
                            .delete();
                }

                if (policiesNeedCreate.isEmpty()) {
                    return;
                }

                policiesNeedCreate.forEach(p -> p.setRoleUuid(spec.getUuid()));
                for (RolePolicyVO policy : policiesNeedCreate) {
                    Set<RolePolicyResourceRefVO> refs = new HashSet<>(policy.getResourceRefs());

                    persist(policy);
                    if (isEmpty(refs)) {
                        continue;
                    }

                    reload(policy);
                    for (RolePolicyResourceRefVO ref : refs) {
                        ref.setRolePolicyId(policy.getId());
                        persist(ref);
                    }
                }
            }
        }.execute();
    }
}
