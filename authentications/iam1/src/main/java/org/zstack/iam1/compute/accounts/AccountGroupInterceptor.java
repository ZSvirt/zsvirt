package org.zstack.iam1.compute.accounts;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vo.ResourceTypeMetadata;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.iam1.api.accounts.*;
import org.zstack.iam1.compute.ensemble.ResourceEnsembleHelper;
import org.zstack.iam1.entity.accounts.*;
import org.zstack.iam1.header.IAM1Errors;
import org.zstack.utils.CollectionUtils;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;

/**
 * Created by Wenhao.Zhang on 2024/08/28
 */
@InterceptorForService("iam1Accounts")
public class AccountGroupInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade databaseFacade;

    @Override
    public APIMessage intercept(APIMessage message) throws ApiMessageInterceptionException {
        if (message instanceof APIAddAccountToGroupMsg) {
            validate((APIAddAccountToGroupMsg) message);
        } else if (message instanceof APIRemoveAccountFromGroupMsg) {
            validate((APIRemoveAccountFromGroupMsg) message);
        } else if (message instanceof APIAttachRoleToAccountGroupMsg) {
            validate((APIAttachRoleToAccountGroupMsg) message);
        } else if (message instanceof APIDetachRoleFromAccountGroupMsg) {
            validate((APIDetachRoleFromAccountGroupMsg) message);
        } else if (message instanceof APIShareResourceToGroupMsg) {
            validate((APIShareResourceToGroupMsg) message);
        } else if (message instanceof APIRevokeResourceSharingToGroupMsg) {
            validate((APIRevokeResourceSharingToGroupMsg) message);
        } else if (message instanceof APIUpdateAccountGroupMsg) {
            validate((APIUpdateAccountGroupMsg) message);
        } else if (message instanceof APIMoveAccountGroupMsg) {
            validate((APIMoveAccountGroupMsg) message);
        }
        return message;
    }

    private void validate(APIAddAccountToGroupMsg message) {
        List<String> accountsInGroup = Q.New(AccountGroupAccountRefVO.class)
                .select(AccountGroupAccountRefVO_.accountUuid)
                .in(AccountGroupAccountRefVO_.accountUuid, message.getAccountUuids())
                .eq(AccountGroupAccountRefVO_.groupUuid, message.getAccountGroupUuid())
                .listValues();
        message.getAccountUuids().removeAll(accountsInGroup);

        if (message.getAccountUuids().isEmpty()) {
            bus.publish(new APIAddAccountToGroupEvent(message.getId()));
            throw new StopRoutingException();
        }
    }

    private void validate(APIRemoveAccountFromGroupMsg message) {
        boolean equalToExpected = !Q.New(AccountGroupAccountRefVO.class)
                .in(AccountGroupAccountRefVO_.accountUuid, message.getAccountUuids())
                .eq(AccountGroupAccountRefVO_.groupUuid, message.getAccountGroupUuid())
                .isExists();

        if (equalToExpected) {
            bus.publish(new APIRemoveAccountFromGroupEvent(message.getId()));
            throw new StopRoutingException();
        }
    }

    private void validate(APIAttachRoleToAccountGroupMsg message) {
        List<String> groupRoles = Q.New(AccountGroupRoleRefVO.class)
                .select(AccountGroupRoleRefVO_.roleUuid)
                .in(AccountGroupRoleRefVO_.roleUuid, message.getRoleUuids())
                .eq(AccountGroupRoleRefVO_.groupUuid, message.getAccountGroupUuid())
                .listValues();
        message.getRoleUuids().removeAll(groupRoles);

        if (message.getRoleUuids().isEmpty()) {
            bus.publish(new APIAddAccountToGroupEvent(message.getId()));
            throw new StopRoutingException();
        }
    }

    private void validate(APIDetachRoleFromAccountGroupMsg message) {
        boolean equalToExpected = !Q.New(AccountGroupRoleRefVO.class)
                .in(AccountGroupRoleRefVO_.roleUuid, message.getRoleUuids())
                .eq(AccountGroupRoleRefVO_.groupUuid, message.getAccountGroupUuid())
                .isExists();

        if (equalToExpected) {
            bus.publish(new APIDetachRoleFromAccountGroupEvent(message.getId()));
            throw new StopRoutingException();
        }
    }

    private void validate(APIShareResourceToGroupMsg message) {
        List<String> sharedResources = Q.New(AccountGroupResourceRefVO.class)
                .select(AccountGroupResourceRefVO_.resourceUuid)
                .in(AccountGroupResourceRefVO_.resourceUuid, message.getResourceUuids())
                .eq(AccountGroupResourceRefVO_.groupUuid, message.getGroupUuid())
                .listValues();
        message.getResourceUuids().removeAll(sharedResources);

        if (message.getResourceUuids().isEmpty()) {
            bus.publish(new APIShareResourceToGroupEvent(message.getId()));
            throw new StopRoutingException();
        }

        // Check: the resources must be in resource ensembles
        final List<Tuple> tuples = Q.New(ResourceVO.class)
                .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                .in(ResourceVO_.uuid, message.getResourceUuids())
                .listTuple();

        final Set<String> resourceTypes = CollectionUtils.transformToSet(tuples, tuple -> tuple.get(1, String.class));
        for (String resourceType : resourceTypes) {
            if (ResourceEnsembleHelper.inEnsemble(ResourceTypeMetadata.resourceTypeForName(resourceType))) {
                continue;
            }

            List<String> invalidUuidList = tuples.stream()
                    .filter(tuple -> resourceType.equals(tuple.get(1, String.class)))
                    .map(tuple -> tuple.get(0, String.class))
                    .collect(Collectors.toList());
            throw new ApiMessageInterceptionException(err(IAM1Errors.NOT_RESOURCE_ENSEMBLE_MEMBER,
                    "resources[uuid:%s, type:%s] are not resource ensemble members",
                    invalidUuidList, resourceType));
        }
    }

    private void validate(APIRevokeResourceSharingToGroupMsg message) {
        boolean equalToExpected = !Q.New(AccountGroupResourceRefVO.class)
                .in(AccountGroupResourceRefVO_.resourceUuid, message.getResourceUuids())
                .eq(AccountGroupResourceRefVO_.groupUuid, message.getGroupUuid())
                .isExists();

        if (equalToExpected) {
            bus.publish(new APIRevokeResourceSharingToGroupEvent(message.getId()));
            throw new StopRoutingException();
        }
    }

    private void validate(APIUpdateAccountGroupMsg message) {
        if (message.getName() == null && message.getDescription() == null) {
            APIUpdateAccountGroupEvent event = new APIUpdateAccountGroupEvent(message.getId());
            event.setInventory(AccountGroupInventory.valueOf(
                    databaseFacade.findByUuid(message.getUuid(), AccountGroupVO.class)));
            bus.publish(event);
            throw new StopRoutingException();
        }
    }

    private void validate(APIMoveAccountGroupMsg message) {
        boolean equalToExpected;

        if (message.getParentUuid() == null) {
            equalToExpected = Q.New(AccountGroupVO.class)
                    .eq(AccountGroupVO_.uuid, message.getUuid())
                    .isNull(AccountGroupVO_.parentUuid)
                    .isExists();
        } else {
            equalToExpected = Q.New(AccountGroupVO.class)
                    .eq(AccountGroupVO_.uuid, message.getUuid())
                    .eq(AccountGroupVO_.parentUuid, message.getParentUuid())
                    .isExists();
        }

        if (equalToExpected) {
            APIMoveAccountGroupEvent event = new APIMoveAccountGroupEvent(message.getId());
            event.setInventory(AccountGroupInventory.valueOf(
                    databaseFacade.findByUuid(message.getUuid(), AccountGroupVO.class)));
            bus.publish(event);
            throw new StopRoutingException();
        }
    }
}
