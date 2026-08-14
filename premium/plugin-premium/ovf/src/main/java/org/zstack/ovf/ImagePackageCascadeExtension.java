package org.zstack.ovf;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.ovf.datatype.ImagePackageVO;
import org.zstack.ovf.datatype.ImagePackageInventory;
import org.zstack.ovf.datatype.ImagePackageVO_;
import org.zstack.ovf.message.DeleteOvaPackageMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Qi Le on 2022/5/3
 */
public class ImagePackageCascadeExtension extends AbstractCascadeExtension {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = ImagePackageVO.class.getSimpleName();
    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(BackupStorageVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<ImagePackageInventory> ctx = ovaPackageFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }
        return null;
    }

    private List<ImagePackageInventory> ovaPackageFromAction(CascadeAction action) {
        List<ImagePackageInventory> ret = null;
        if (BackupStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> bsUuids = ((List<BackupStorageInventory>) action.getParentIssuerContext()).stream()
                    .map(BackupStorageInventory::getUuid)
                    .collect(Collectors.toList());
            List<ImagePackageVO> vos = Q.New(ImagePackageVO.class)
                    .in(ImagePackageVO_.backupStorageUuid, bsUuids)
                    .list();
            if (vos != null) {
                ret = ImagePackageInventory.valueOf(vos);
            }
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> acUuids = ((List<AccountInventory>) action.getParentIssuerContext()).stream()
                    .map(AccountInventory::getUuid)
                    .collect(Collectors.toList());
            List<ImagePackageVO> vos = SQL.New("select imp from ImagePackageVO imp, AccountResourceRefVO ref " +
                            "where imp.uuid = ref.resourceUuid and ref.resourceType = :rtype and ref.type = :type " +
                            "and ref.accountUuid in (:accUuids)")
                    .param("rtype", ImagePackageVO.class.getSimpleName())
                    .param("type", AccessLevel.Own)
                    .param("accUuids", acUuids)
                    .list();
            if (vos != null) {
                ret = ImagePackageInventory.valueOf(vos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }
        if (ret == null) {
            ret = new ArrayList<>();
        }
        return ret;
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            completion.success();
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleCleanUp(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleCleanUp(CascadeAction action, Completion completion) {
        List<ImagePackageInventory> ovas = ovaPackageFromAction(action);
        List<String> ovaUuids = ovas.stream().map(ImagePackageInventory::getUuid).collect(Collectors.toList());
        dbf.removeByPrimaryKeys(ovaUuids, ImagePackageVO.class);
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        List<ImagePackageInventory> ovas = ovaPackageFromAction(action);
        if (ovas.isEmpty()) {
            completion.success();
            return;
        }
        new While<>(ovas).step((ova, wCompletion) -> {
            DeleteOvaPackageMsg msg = new DeleteOvaPackageMsg();
            msg.setUuid(ova.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, OvfManager.SERVICE_ID, msg.getUuid());
            bus.send(msg, new CloudBusCallBack(wCompletion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        wCompletion.addError(reply.getError());
                        wCompletion.done();
                        return;
                    }
                    wCompletion.done();
                }
            });
        }, 3).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList.getCauses().isEmpty()) {
                    completion.success();
                } else {
                    completion.fail(errorCodeList.getCauses().get(0));
                }
            }
        });
    }
}
