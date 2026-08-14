package org.zstack.identity.rbac;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.rbac.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIResourceScope;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.identity.APIRequestChecker;
import org.zstack.identity.Account;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.header.errorcode.SysErrors.*;
import static org.zstack.utils.CollectionUtils.transform;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RBACResourceRequestChecker implements APIRequestChecker {
    protected APIMessage message;

    @Override
    public void check(APIMessage message) {
        this.message = message;
        check();
    }

    protected org.zstack.header.description.permission.Permission getRBACInfo() {
        return RBAC.apiBuckets.get(message.getClass().getName()).permission;
    }

    private static class AccountResourceBundle {
        String accountUuid;
        String resourceUuid;
    }

    private Collection<AccountResourceBundle> toAccountResourceBundles(List<String> resourceUuids, List<Tuple> tss) {
        Map<String, AccountResourceBundle> m = new HashMap<>();

        tss.forEach(ts -> {
            String accountUuid = ts.get(0, String.class);
            String resUuid = ts.get(1, String.class);
            AccountResourceBundle b = new AccountResourceBundle();
            b.accountUuid = accountUuid;
            b.resourceUuid = resUuid;
            m.put(resUuid, b);
        });

        resourceUuids.forEach(ruuid -> {
            if (!m.containsKey(ruuid)) {
                AccountResourceBundle b = new AccountResourceBundle();
                b.accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID;
                b.resourceUuid = ruuid;
                m.put(ruuid, b);
            }
        });

        return m.values();
    }

    protected List<String> getResourceUuids(APIMessage.FieldParam param) throws IllegalAccessException {
        List<String> uuids = new ArrayList<>();
        if (String.class.isAssignableFrom(param.field.getType())) {
            String uuid = (String) param.field.get(message);
            if (uuid != null) {
                uuids.add(uuid);
            }
        } else if (Collection.class.isAssignableFrom(param.field.getType())) {
            Collection u = (Collection<? extends String>) param.field.get(message);
            if (u != null) {
                uuids.addAll(u);
            }
        } else {
            throw new CloudRuntimeException(String.format("not supported field type[%s] for %s#%s", param.field.getType(), message.getClass(), param.field.getName()));
        }

        return uuids;
    }

    private void check() {
        if (Account.isAdminPermission(message.getSession())) {
            return;
        }

        org.zstack.header.description.permission.Permission info = getRBACInfo();

        new SQLBatch() {
            @Override
            protected void scripts() {
                APIMessage.getApiParams().get(message.getClass()).forEach(this::checkOperationTarget);
            }

            private void checkOperationTarget(APIMessage.FieldParam param) {
                Class<?>[] resourceTypes = param.param.resourceType();
                if (resourceTypes.length == 0)  {
                    return;
                }

                if (APIMessage.isReadOnlyApi(message.getClass())) {
                    // no check to read api
                    return;
                }

                Class<?> resourceType = resourceTypes[0];
                try {
                    if (resourceType.equals(AccountVO.class)) {
                        checkAccountAPIParam(param);
                    } else if (resourceType.equals(SystemTagVO.class)) {
                        checkIfTheAccountOwnTheTaggedResource(param);
                    } else {
                        checkIfTheAccountCanAccessTheResource(param);
                    }
                } catch (OperationFailureException oe) {
                    throw oe;
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            }

            private void checkAccountAPIParam(APIMessage.FieldParam param) throws IllegalAccessException {
                APIResourceScope scope = apiResourceScope(param);
                if (scope != APIResourceScope.MustOwner) {
                    return;
                }
                List<String> uuids = getResourceUuids(param);
                String currentAccountUuid = message.getSession().getAccountUuid();
                if (uuids.stream().anyMatch(uuid -> !Objects.equals(uuid, currentAccountUuid))) {
                    String parameterName = param.field.getName();
                    throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                            "Operations on other accounts are not permitted", parameterName));
                }
            }

            private void removeSharedToPublicResources(List<String> uuids) {
                if (uuids.isEmpty()) {
                    return;
                }

                final List<String> sharedResourceUuidList = q(AccountResourceRefVO.class)
                        .in(AccountResourceRefVO_.resourceUuid, uuids)
                        .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                        .select(AccountResourceRefVO_.resourceUuid)
                        .listValues();
                uuids.removeAll(sharedResourceUuidList);
            }

            private Collection<AccountResourceBundle> getAccountResourceBundles(List<String> uuids) {
                List<Tuple> ts = q(AccountResourceRefVO.class)
                        .select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid)
                        .in(AccountResourceRefVO_.resourceUuid, uuids)
                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                        .listTuple();

                ts.addAll(
                    q(AccountResourceRefVO.class)
                            .select(AccountResourceRefVO_.accountUuid, AccountResourceRefVO_.resourceUuid)
                            .in(AccountResourceRefVO_.resourceUuid, uuids)
                            .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                            .eq(AccountResourceRefVO_.accountUuid, message.getSession().getAccountUuid())
                            .listTuple()
                );

                return toAccountResourceBundles(uuids, ts);
            }

            private void checkIfTheAccountOwnTheTaggedResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);
                if (uuids.isEmpty()) {
                    return;
                }

                List<Tuple> taggedResource = q(SystemTagVO.class).select(SystemTagVO_.resourceUuid, SystemTagVO_.resourceType)
                        .in(SystemTagVO_.uuid, uuids).listTuple();
                if (taggedResource.isEmpty()) {
                    return;
                }

                List<String> taggedResUuids = taggedResource.stream()
                        .map(ts -> ts.get(0, String.class))
                        .collect(Collectors.toList());
                Collection<AccountResourceBundle> bundles = getAccountResourceBundles(taggedResUuids);

                taggedResource.forEach(tuple -> {
                    String uuid = tuple.get(0, String.class);
                    String type = tuple.get(1, String.class);
                    Optional<AccountResourceBundle> opt = bundles.stream()
                            .filter(b -> b.accountUuid.equals(message.getSession().getAccountUuid()) && b.resourceUuid.equals(uuid))
                            .findFirst();
                    if (!opt.isPresent()) {
                        throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                                "account[uuid:%s] has no permission to set system tag with resource[uuid:%s, type:%s]",
                                message.getSession().getAccountUuid(), uuid, type));
                    }
                });
            }

            private void checkIfTheAccountCanAccessTheResource(APIMessage.FieldParam param) throws IllegalAccessException {
                List<String> uuids = getResourceUuids(param);

                final String accountUuid = message.getSession().getAccountUuid();
                APIResourceScope scope = apiResourceScope(param);

                AccessibleResourceChecker checker = AccessibleResourceChecker.forAccount(accountUuid)
                        .withScope(scope);

                final Class<?>[] resourceTypes = param.param.resourceType();
                if (resourceTypes.length == 1) {
                    checker.withResourceType(resourceTypes[0]);
                }

                List<String> inaccessibleResources = checker.findOutAllInaccessibleResources(uuids);
                if (!inaccessibleResources.isEmpty()) {
                    noAccessWithSharableScopeError(accountUuid, inaccessibleResources, scope);
                }
            }

            private void noAccessWithSharableScopeError(String accountUuid, List<String> inaccessibleResources, APIResourceScope scope) {
                List<Tuple> tuples = q(ResourceVO.class)
                        .in(ResourceVO_.uuid, inaccessibleResources)
                        .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                        .listTuple();
                List<String> texts = transform(tuples,
                        tuple -> String.format("%s[uuid:%s]", tuple.get(1, String.class), tuple.get(0, String.class)));
                if (scope == APIResourceScope.AllowedSharing) {
                    throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                            "account[uuid:%s] has no access to resources with allow-sharing scope: %s",
                            accountUuid, String.join("\n\t", texts)));
                }
                throw new OperationFailureException(err(RESOURCE_NOT_ACCESSIBLE,
                        "account[uuid:%s] has no access to resources with owner-only scope: %s",
                        accountUuid, String.join(",", texts)));
            }
        }.execute();
    }

    public APIResourceScope apiResourceScope(APIMessage.FieldParam param) {
        String scope = param.param.scope();
        if (!APIParam.SCOPE_AUTO.equals(scope)) {
            return APIResourceScope.valueOf(scope);
        }

        Class<?> resourceType = param.param.resourceType()[0];

        return RBAC.isResourceGlobalReadable(resourceType) ?
                APIResourceScope.AllowedAll :
                APIResourceScope.AllowedSharing;
    }
}
