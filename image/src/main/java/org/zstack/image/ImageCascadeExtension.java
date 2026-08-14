package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.image.*;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class ImageCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ImageCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = ImageVO.class.getSimpleName();

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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        cleanupImageEO();
        completion.success();
    }

    private void cleanupImageEO() {
        String sql = "select i.uuid from ImageEO i where i.deleted is not null and i.uuid not in (select vm.imageUuid from VmInstanceVO vm where vm.imageUuid is not null)";
        dbf.hardDeleteCollectionSelectedBySQL(sql, ImageVO.class);
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<ImageDeletionStruct> structs = imageFromAction(action);
        if (structs == null) {
            completion.success();
            return;
        }

        List<ImageDeletionMsg> msgs = transformAndRemoveNull(structs, arg -> {
            ImageDeletionMsg msg = new ImageDeletionMsg();
            msg.setImageUuid(arg.getImage().getUuid());
            if (!arg.getDeleteAll()) {
                msg.setBackupStorageUuids(arg.getBackupStorageUuids());
            }
            ImageDeletionPolicy deletionPolicy = deletionPolicyFromAction(action);
            msg.setDeletionPolicy(deletionPolicy == null ? null : deletionPolicy.toString());
            bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, arg.getImage().getUuid());
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
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
        return Arrays.asList(BackupStorageVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Transactional(readOnly = true)
    private List<ImageDeletionStruct> getImageOnBackupStorage(List<String> bsUuids) {
        String sql = "select ref.backupStorageUuid, img from ImageVO img, ImageBackupStorageRefVO ref where img.uuid = ref.imageUuid and ref.backupStorageUuid in (:bsUuids) group by img.uuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("bsUuids", bsUuids);
        List<Tuple> ts = q.getResultList();

        Map<String, ImageDeletionStruct> tmp = new HashMap<String, ImageDeletionStruct>();
        for (Tuple t : ts) {
            String bsUuid = t.get(0, String.class);
            ImageVO img = t.get(1, ImageVO.class);
            ImageDeletionStruct struct = tmp.get(img.getUuid());
            if (struct == null) {
                struct = new ImageDeletionStruct();
                struct.setImage(ImageInventory.valueOf(img));
                struct.setBackupStorageUuids(new ArrayList<String>());
                tmp.put(img.getUuid(), struct);
            }
            struct.getBackupStorageUuids().add(bsUuid);
        }

        List<ImageDeletionStruct> structs = new ArrayList<ImageDeletionStruct>();
        structs.addAll(tmp.values());
        return structs;
    }

    private ImageDeletionPolicy deletionPolicyFromAction(CascadeAction action) {
        if (BackupStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return ImageDeletionPolicy.DeleteReference;
        } else {
            return null;
        }
    }

    private List<ImageDeletionStruct> imageFromAction(CascadeAction action) {
        List<ImageDeletionStruct> ret = null;
        if (BackupStorageVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> bsuuids = transformAndRemoveNull(action.getParentIssuerContext(), BackupStorageInventory::getUuid);

            ret =  getImageOnBackupStorage(bsuuids);
            ret = ret.isEmpty() ? null : ret;
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            final List<String> auuids = CollectionUtils.transform(action.getParentIssuerContext(), AccountInventory::getUuid);

            List<ImageVO> imgvos = ResourceHelper.findOwnResources(ImageVO.class, auuids);
            if (!imgvos.isEmpty()) {
                ret = CollectionUtils.transform(imgvos, arg -> {
                    ImageDeletionStruct s = new ImageDeletionStruct();
                    s.setImage(ImageInventory.valueOf(arg));
                    return s;
                });
            }
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<ImageDeletionStruct> ctx = imageFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
