package org.zstack.iam1.compute.ensemble;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.role.RolePolicyChecker;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.vo.ResourceTypeMetadata;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.iam1.header.ensemble.ResourceEnsembleInfo;
import org.zstack.identity.rbac.ResourceSharingExtensionPoint;
import org.zstack.identity.header.ShareResourceContext;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

/**
 * Created by Wenhao.Zhang on 2024/08/06
 */
public class EnsembleExtensions implements ResourceSharingExtensionPoint,
        RolePolicyChecker {
    protected static final CLogger logger = Utils.getLogger(EnsembleExtensions.class);

    @Autowired
    private DatabaseFacade databaseFacade;

    @Override
    public void beforeSharingResource(ShareResourceContext context) {
        Set<String> existsEnsembleUuidSet = new HashSet<>();

        context.uuidResourceMap.forEach((uuid, resourceVO) -> {
            Class<?> resourceType = ResourceTypeMetadata.resourceTypeForName(resourceVO.getResourceType());
            ResourceEnsembleInfo ensembleInfo = ResourceEnsembleHelper.findResourceEnsemble(uuid, resourceType);
            if (ensembleInfo == null || existsEnsembleUuidSet.contains(ensembleInfo.uuid)) {
                return;
            }

            String masterUuid = ensembleInfo.uuid;
            existsEnsembleUuidSet.add(masterUuid);
            ensembleInfo = ResourceEnsembleHelper.findAllChildrenResources(masterUuid, ensembleInfo.resourceType());
            if (ensembleInfo == null) {
                return;
            }

            List<ResourceEnsembleInfo> children = ensembleInfo.flatten();
            context.additionResources(
                    transform(children, info ->
                            new ResourceVO(new Object[] { info.uuid, null, info.resourceType().getSimpleName() })),
                    masterUuid);
        });
    }

    @Transactional
    public void changeResourceEnsemble(String masterResourceUuid, List<String> resourceUuidList) {
        if (isEmpty(resourceUuidList)) {
            return;
        }

        SQL.New(AccountResourceRefVO.class)
                .in(AccountResourceRefVO_.resourceUuid, resourceUuidList)
                .in(AccountResourceRefVO_.type, list(AccessLevel.Share, AccessLevel.SharePublic))
                .notNull(AccountResourceRefVO_.resourcePermissionFrom)
                .delete();

        List<Tuple> tuples = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, masterResourceUuid)
                .in(AccountResourceRefVO_.type, list(AccessLevel.Share, AccessLevel.SharePublic))
                .eq(AccountResourceRefVO_.resourcePermissionFrom, masterResourceUuid)
                .select(
                        AccountResourceRefVO_.accountUuid,
                        AccountResourceRefVO_.accountPermissionFrom,
                        AccountResourceRefVO_.type
                )
                .listTuple();

        Map<String, String> uuidResourceMap = toMap(
                Q.New(ResourceVO.class)
                        .in(ResourceVO_.uuid, resourceUuidList)
                        .select(ResourceVO_.uuid, ResourceVO_.resourceType)
                        .listTuple(),
                t -> t.get(0, String.class),
                t -> t.get(1, String.class));

        String masterResourceType = Q.New(ResourceVO.class)
                .eq(ResourceVO_.uuid, masterResourceUuid)
                .select(ResourceVO_.resourceType)
                .findValue();

        logger.debug("Below resources add to a new ensemble:\n" + String.join("\n", transform(resourceUuidList,
                uuid -> String.format("\t%s[uuid=%s] -> %s ResourceEnsemble[uuid=%s]",
                        uuidResourceMap.get(uuid), uuid, masterResourceType, masterResourceUuid))));

        if (tuples.isEmpty()) {
            return;
        }

        List<AccountResourceRefVO> refsToCreate = new ArrayList<>();
        for (Tuple tuple : tuples) {
            for (String resourceUuid : resourceUuidList) {
                AccountResourceRefVO ref = new AccountResourceRefVO();
                ref.setAccountUuid(tuple.get(0, String.class));
                ref.setResourceUuid(resourceUuid);
                ref.setResourceType(uuidResourceMap.get(resourceUuid));
                ref.setAccountPermissionFrom(tuple.get(1, String.class));
                ref.setResourcePermissionFrom(masterResourceUuid);
                ref.setType(tuple.get(2, AccessLevel.class));
                refsToCreate.add(ref);
            }
        }
        databaseFacade.persistCollection(refsToCreate);
    }

    @Override
    public ErrorCode checkRolePolicies(List<RolePolicyStatement> policies) {
        Map<String, List<RolePolicyStatement.Resource>> resources = policies.stream()
                .flatMap(policy -> policy.resources.stream())
                .collect(Collectors.groupingBy(resource -> resource.resourceType));

        for (Map.Entry<String, List<RolePolicyStatement.Resource>> entry : resources.entrySet()) {
            Class<?> typeClass = ResourceTypeMetadata.resourceTypeForName(entry.getKey());
            if (ResourceEnsembleHelper.isEnsembleMasterType(typeClass)) {
                continue;
            }
            return err(IdentityErrors.INVALID_ROLE_POLICY,
                    "invalid role policy resource: resource[uuid:%s] is not a ensemble master resource",
                    entry.getValue().get(0).uuid);
        }

        return null;
    }
}
