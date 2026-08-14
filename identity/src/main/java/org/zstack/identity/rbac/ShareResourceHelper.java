package org.zstack.identity.rbac;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.identity.header.ShareResourceContext;
import org.zstack.utils.Utils;
import org.zstack.utils.data.Pair;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.*;

public class ShareResourceHelper {
    private static final CLogger logger = Utils.getLogger(ShareResourceHelper.class);

    @Autowired
    protected DatabaseFacade databaseFacade;
    @Autowired
    protected PluginRegistry pluginRegistry;

    public void shareToAccounts(ShareResourceContext context, List<String> accountUuids) {
        emitBeforeSharingExtensions(context);

        final Set<String> allMasterResources = context.findAllMasterResources();
        List<AccountResourceRefVO> needPersists = new ArrayList<>();

        for (String masterResource : allMasterResources) {
            List<AccountResourceRefVO> refs = context.buildShareAccountRecords(masterResource, accountUuids);

            List<Tuple> existsTuples = Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                    .in(AccountResourceRefVO_.resourceUuid, transform(refs, AccountResourceRefVO::getResourceUuid))
                    .in(AccountResourceRefVO_.accountUuid, accountUuids)
                    .eq(AccountResourceRefVO_.resourcePermissionFrom, masterResource)
                    .isNull(AccountResourceRefVO_.accountPermissionFrom)
                    .select(
                            AccountResourceRefVO_.resourceUuid,
                            AccountResourceRefVO_.accountUuid,
                            AccountResourceRefVO_.resourcePermissionFrom
                    )
                    .listTuple();
            Set<String> existsRecords = transformToSet(existsTuples,
                    tuple -> tuple.get(0, String.class) + "," + tuple.get(1, String.class) + "," + tuple.get(2, String.class));
            refs.removeIf(ref -> existsRecords.contains(String.format("%s,%s,%s",
                    ref.getResourceUuid(), ref.getAccountUuid(), ref.getResourcePermissionFrom())));
            needPersists.addAll(refs);
        }

        List<AccountResourceRefVO> refs = context.buildShareAccountRecordsForSolitaryResources(accountUuids);
        if (!refs.isEmpty()) {
            List<Tuple> existsTuples = Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                    .in(AccountResourceRefVO_.resourceUuid, transform(refs, AccountResourceRefVO::getResourceUuid))
                    .in(AccountResourceRefVO_.accountUuid, accountUuids)
                    .isNull(AccountResourceRefVO_.resourcePermissionFrom)
                    .isNull(AccountResourceRefVO_.accountPermissionFrom)
                    .select(AccountResourceRefVO_.resourceUuid, AccountResourceRefVO_.accountUuid)
                    .listTuple();
            Set<Pair<String, String>> resourceAccountPairs = transformToSet(existsTuples,
                    tuple -> new Pair<>(tuple.get(0, String.class), tuple.get(1, String.class)));
            refs.removeIf(ref -> resourceAccountPairs.contains(
                    new Pair<>(ref.getResourceUuid(), ref.getAccountUuid())));
            needPersists.addAll(refs);
        }

        if (needPersists.isEmpty()) {
            return;
        }

        databaseFacade.persistCollection(needPersists);
        String texts = StringUtils.join(transform(needPersists,
                shared -> String.format("\tuuid:%s type:%s", shared.getResourceUuid(), shared.getResourceType())), "\n");
        logger.debug(String.format("Shared below resources to account[uuid:%s]: \n%s", accountUuids, texts));
    }

    public void shareToPublic(ShareResourceContext context) {
        emitBeforeSharingExtensions(context);

        final Set<String> allMasterResources = context.findAllMasterResources();
        List<AccountResourceRefVO> needPersists = new ArrayList<>();

        for (String masterResource : allMasterResources) {
            List<AccountResourceRefVO> refs = context.buildShareToPublicRecords(masterResource);

            List<String> existsUuidList = Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                    .in(AccountResourceRefVO_.resourceUuid, transform(refs, AccountResourceRefVO::getResourceUuid))
                    .eq(AccountResourceRefVO_.resourcePermissionFrom, masterResource)
                    .select(AccountResourceRefVO_.resourceUuid)
                    .listValues();
            refs.removeIf(ref -> existsUuidList.contains(ref.getResourceUuid()));
            needPersists.addAll(refs);
        }

        List<AccountResourceRefVO> refs = context.buildShareToPublicRecordsForSolitaryResources();
        if (!refs.isEmpty()) {
            List<String> existsUuidList = Q.New(AccountResourceRefVO.class)
                    .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                    .in(AccountResourceRefVO_.resourceUuid, transform(refs, AccountResourceRefVO::getResourceUuid))
                    .isNull(AccountResourceRefVO_.resourcePermissionFrom)
                    .select(AccountResourceRefVO_.resourceUuid)
                    .listValues();
            refs.removeIf(ref -> existsUuidList.contains(ref.getResourceUuid()));
            needPersists.addAll(refs);
        }

        if (needPersists.isEmpty()) {
            return;
        }

        databaseFacade.persistCollection(needPersists);
        String texts = StringUtils.join(transform(needPersists,
                shared -> String.format("\tuuid:%s type:%s", shared.getResourceUuid(), shared.getResourceType())), "\n");
        logger.debug(String.format("Shared below resources to public: \n%s", texts));
    }

    public void revokeSharingToAccounts(ShareResourceContext context, List<String> accountUuids) {
        emitBeforeSharingExtensions(context);

        final Set<String> masterResourceUuidSet = context.findAllMasterResources();
        final Set<String> resourceUuidSet = context.findAllSolitaryResources();
        if (!masterResourceUuidSet.isEmpty()) {
            SQL.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourcePermissionFrom, masterResourceUuidSet)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                    .in(AccountResourceRefVO_.accountUuid, accountUuids)
                    .isNull(AccountResourceRefVO_.accountPermissionFrom)
                    .delete();
        }
        if (!resourceUuidSet.isEmpty()) {
            SQL.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuidSet)
                    .eq(AccountResourceRefVO_.type, AccessLevel.Share)
                    .in(AccountResourceRefVO_.accountUuid, accountUuids)
                    .isNull(AccountResourceRefVO_.accountPermissionFrom)
                    .delete();
        }

        resourceUuidSet.addAll(masterResourceUuidSet);
        logger.debug(String.format("Revoke shared resource for type(Share to Account): \n%s\nWith accounts: \n%s",
                StringUtils.join(transform(resourceUuidSet, uuid -> String.format("\tuuid:%s", uuid)), "\n"),
                StringUtils.join(transform(accountUuids, uuid -> String.format("\tuuid:%s", uuid)), "\n")));
    }

    public void revokeSharingToPublic(ShareResourceContext context) {
        emitBeforeSharingExtensions(context);

        final Set<String> masterResourceUuidSet = context.findAllMasterResources();
        final Set<String> resourceUuidSet = context.findAllSolitaryResources();
        if (!masterResourceUuidSet.isEmpty()) {
            SQL.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourcePermissionFrom, masterResourceUuidSet)
                    .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                    .delete();
        }
        if (!resourceUuidSet.isEmpty()) {
            SQL.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuidSet)
                    .eq(AccountResourceRefVO_.type, AccessLevel.SharePublic)
                    .delete();
        }
        resourceUuidSet.addAll(masterResourceUuidSet);
        logger.debug(String.format("Revoke shared resource for type(SharePublic): \n%s",
                StringUtils.join(transform(resourceUuidSet, uuid -> String.format("\tuuid:%s", uuid)), "\n")));
    }

    public void revokeSharingAll(ShareResourceContext context) {
        emitBeforeSharingExtensions(context);

        final Set<String> masterResourceUuidSet = context.findAllMasterResources();
        final Set<String> resourceUuidSet = context.findAllSolitaryResources();
        if (!masterResourceUuidSet.isEmpty()) {
            SQL.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourcePermissionFrom, masterResourceUuidSet)
                    .in(AccountResourceRefVO_.type, list(AccessLevel.Share, AccessLevel.SharePublic))
                    .isNull(AccountResourceRefVO_.accountPermissionFrom)
                    .delete();
        }
        if (!resourceUuidSet.isEmpty()) {
            SQL.New(AccountResourceRefVO.class)
                    .in(AccountResourceRefVO_.resourceUuid, resourceUuidSet)
                    .in(AccountResourceRefVO_.type, list(AccessLevel.Share, AccessLevel.SharePublic))
                    .isNull(AccountResourceRefVO_.accountPermissionFrom)
                    .delete();
        }
        resourceUuidSet.addAll(masterResourceUuidSet);
        logger.debug(String.format("Revoke shared resource for all types: \n%s",
                StringUtils.join(transform(resourceUuidSet, uuid -> String.format("\tuuid:%s", uuid)), "\n")));
    }

    protected void emitBeforeSharingExtensions(ShareResourceContext context) {
        safeForEach(pluginRegistry.getExtensionList(ResourceSharingExtensionPoint.class),
                it -> it.beforeSharingResource(context));
    }
}
