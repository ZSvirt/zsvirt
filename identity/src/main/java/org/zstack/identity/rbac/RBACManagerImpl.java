package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.PolicyMatcher;
import org.zstack.header.identity.rbac.RBAC;
import org.zstack.header.identity.role.*;
import org.zstack.header.identity.role.api.APICreateRoleEvent;
import org.zstack.header.identity.role.api.APICreateRoleMsg;
import org.zstack.header.identity.role.api.RoleMessage;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.isEmpty;

public class RBACManagerImpl extends AbstractService implements
        RBACManager, Component, PrepareDbInitialValueExtensionPoint, RolePolicyChecker,
        AfterCreateAccountExtensionPoint {
    private static final CLogger logger = Utils.getLogger(RBACManagerImpl.class);

    private static PolicyMatcher matcher = new PolicyMatcher();

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof RoleMessage) {
            passThrough((RoleMessage)msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage(msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void passThrough(RoleMessage msg) {
        RoleVO vo = dbf.findByUuid(msg.getRoleUuid(), RoleVO.class);
        if (vo == null) {
            throw new CloudRuntimeException(String.format("RoleVO[uuid:%s] not existing, it may have been deleted", msg.getRoleUuid()));
        }

        new RoleBase(vo).handleMessage((Message) msg);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(Message msg) {
        if (msg instanceof APICreateRoleMsg) {
            handle((APICreateRoleMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateRoleMsg msg) {
        final RoleSpec spec = RoleSpec.valueOf(msg);

        RoleVO vo = spec.buildVOWithoutPolicies();
        RolePolicyUpdater updater = RolePolicyUpdater.pure();
        for (RolePolicyStatement statement : spec.getPoliciesToCreate()) {
            updater.add(statement);
        }

        updater.squeeze();
        List<RolePolicyVO> policiesNeedCreate = updater.collectAllPolicyIdsNeedCreate();

        new SQLBatch() {
            @Override
            protected void scripts() {
                persist(vo);

                policiesNeedCreate.forEach(p -> p.setRoleUuid(spec.getUuid()));
                for (RolePolicyVO policy : policiesNeedCreate) {
                    Set<RolePolicyResourceRefVO> refs = policy.getResourceRefs();

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

        APICreateRoleEvent evt = new APICreateRoleEvent(msg.getId());
        evt.setInventory(RoleInventory.valueOf(dbf.findByUuid(vo.getUuid(), RoleVO.class)));
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    @Transactional
    public void prepareDbInitialValue() {
        Set<String> existsRoles = new HashSet<>(Q.New(RoleVO.class)
                .eq(RoleVO_.type, RoleType.Predefined)
                .select(RoleVO_.uuid)
                .listValues());
        final List<org.zstack.header.description.role.Role> roleList = RBAC.roles.stream()
                .collect(Collectors.toList());

        List<RoleVO> rolesNeedCreate = new ArrayList<>();
        List<AccountResourceRefVO> refsNeedCreate = new ArrayList<>();
        List<RolePolicyVO> policiesNeedCreate = new ArrayList<>();
        List<RoleAccountRefVO> accountRefsNeedCreate = new ArrayList<>();
        List<String> policiesNeedClean = new ArrayList<>();

        for (org.zstack.header.description.role.Role role : roleList) {
            if (!existsRoles.contains(role.uuid)) {
                RoleVO rvo = new RoleVO();
                rvo.setUuid(role.uuid);
                rvo.setName(String.format("predefined: %s", role.name));
                rvo.setType(RoleType.Predefined);
                rvo.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                rolesNeedCreate.add(rvo);

                AccountResourceRefVO sh = new AccountResourceRefVO();
                sh.setResourceType(RoleVO.class.getSimpleName());
                sh.setResourceUuid(rvo.getUuid());
                sh.setType(AccessLevel.SharePublic);
                refsNeedCreate.add(sh);

                policiesNeedCreate.addAll(RBAC.toStatements(role));

                if (AccountConstant.OTHER_ROLE_UUID.equals(role.uuid)
                        || AccountConstant.LEGACY_ROLE_UUID.equals(role.uuid)) {
                    List<String> accountUuidList = Q.New(AccountVO.class)
                            .in(AccountVO_.type, list(AccountType.Normal, AccountType.ThirdParty))
                            .select(AccountVO_.uuid)
                            .listValues();
                    for (String accountUuid : accountUuidList) {
                        RoleAccountRefVO ref = new RoleAccountRefVO();
                        ref.setAccountUuid(accountUuid);
                        ref.setRoleUuid(role.uuid);
                        accountRefsNeedCreate.add(ref);
                    }
                }
                continue;
            }

            Set<String> existsActions = new HashSet<>(Q.New(RolePolicyVO.class)
                    .eq(RolePolicyVO_.roleUuid, role.uuid)
                    .select(RolePolicyVO_.actions)
                    .listValues());
            List<RolePolicyVO> expectedPolicies = RBAC.toStatements(role);
            Set<String> expectedActions =
                    CollectionUtils.transformToSet(expectedPolicies, RolePolicyVO::getActions);

            if (expectedActions.size() == existsActions.size()) {
                existsActions.removeAll(expectedActions);

                if (existsActions.isEmpty()) {
                    continue;
                }
            }

            policiesNeedClean.add(role.uuid);
            policiesNeedCreate.addAll(expectedPolicies);
        }

        if (!rolesNeedCreate.isEmpty()) {
            dbf.persistCollection(rolesNeedCreate);
        }
        if (!refsNeedCreate.isEmpty()) {
            dbf.persistCollection(refsNeedCreate);
        }
        if (!policiesNeedClean.isEmpty()) {
            SQL.New(RolePolicyVO.class).in(RolePolicyVO_.roleUuid, policiesNeedClean).delete();
        }
        if (!accountRefsNeedCreate.isEmpty()) {
            dbf.persistCollection(accountRefsNeedCreate);
        }
        if (!policiesNeedCreate.isEmpty()) {
            dbf.persistCollection(policiesNeedCreate);
        }
    }

    @Override
    public ErrorCode checkRolePolicies(List<RolePolicyStatement> policies) {
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9._]+$");

        for (RolePolicyStatement policy : policies) {
            String action = policy.actions;
            String path = action.endsWith("**") ? action.substring(0, action.length() - 2) :
                    action.endsWith("*") ? action.substring(0, action.length() - 1) : action;
            Matcher m = pattern.matcher(path);
            if (!m.matches() || path.contains("..")) {
                return err(IdentityErrors.INVALID_ROLE_POLICY, "invalid role policy actions: %s", action);
            }

            // TODO: UI is currently unable to distinguish admin-only APIs,
            // and this limitation has been temporarily removed for convenience

            if (!action.contains("*")) {
                String fullPath = action.startsWith(".") ?
                        AccountConstant.POLICY_BASE_PACKAGE + action.substring(1) :
                        action;
                if (!RBAC.isValidAPI(fullPath)/* || RBAC.isAdminOnlyAPI(fullPath)*/) {
                    return err(IdentityErrors.INVALID_ROLE_POLICY, "invalid role policy actions: %s", action);
                }
            }
        }

        return null;
    }

    @Override
    public void afterCreateAccount(AccountInventory account) {
        RoleAccountRefVO ref = new RoleAccountRefVO();
        ref.setAccountUuid(account.getUuid());
        ref.setRoleUuid(AccountConstant.OTHER_ROLE_UUID);
        dbf.persist(ref);
    }
}
