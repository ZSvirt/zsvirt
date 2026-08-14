package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageVO;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;

/**
 * Created by miao on 16-10-9.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ImageQuotaUtil {
    private static final CLogger logger = Utils.getLogger(ImageQuotaUtil.class);

    @Autowired
    public DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    protected RESTFacade restf;

    public class ImageQuota {
        public long imageNum;
        public long imageSize;
    }

    @Transactional(readOnly = true)
    public ImageQuota getUsed(String accountUUid) {
        ImageQuota quota = new ImageQuota();

        quota.imageSize = getUsedImageSize(accountUUid);
        quota.imageNum = getUsedImageNum(accountUUid);

        return quota;
    }

    @Transactional(readOnly = true)
    public long getUsedImageNum(String accountUuid) {
        String sql = "select count(image) " +
                " from ImageVO image, AccountResourceRefVO ref " +
                " where image.uuid = ref.resourceUuid " +
                " and ref.accountUuid = :auuid " +
                " and ref.resourceType = :rtype " +
                " and ref.type = :type ";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("auuid", accountUuid);
        q.setParameter("rtype", ImageVO.class.getSimpleName());
        q.setParameter("type", AccessLevel.Own);
        Long imageNum = q.getSingleResult();
        imageNum = imageNum == null ? 0 : imageNum;
        return imageNum;
    }

    @Transactional(readOnly = true)
    public long getUsedImageSize(String accountUuid) {
        String sql = "select sum(image.actualSize) " +
                " from ImageVO image ,AccountResourceRefVO ref " +
                " where image.uuid = ref.resourceUuid " +
                " and ref.accountUuid = :auuid " +
                " and ref.resourceType = :rtype " +
                " and ref.type = :type ";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("auuid", accountUuid);
        q.setParameter("rtype", ImageVO.class.getSimpleName());
        q.setParameter("type", AccessLevel.Own);
        Long imageSize = q.getSingleResult();
        imageSize = imageSize == null ? 0 : imageSize;
        return imageSize;
    }


    public long getLocalImageSizeOnBackupStorage(APIAddImageMsg msg) {
        long imageSizeAsked = 0;

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return imageSizeAsked;
        }

        final String url = msg.getUrl().trim();
        if (url.startsWith("file:///")) {
            GetLocalFileSizeOnBackupStorageMsg gmsg = new GetLocalFileSizeOnBackupStorageMsg();
            String bsUuid = msg.getBackupStorageUuids().get(0);
            gmsg.setBackupStorageUuid(bsUuid);
            gmsg.setUrl(url.split("://")[1]);
            bus.makeTargetServiceIdByResourceUuid(gmsg, BackupStorageConstant.SERVICE_ID, bsUuid);
            GetLocalFileSizeOnBackupStorageReply reply = (GetLocalFileSizeOnBackupStorageReply) bus.call(gmsg);
            if (!reply.isSuccess()) {
                logger.warn(String.format("cannot get image. The image url : %s. description: %s.name: %s",
                        url, msg.getDescription(), msg.getName()));
                throw new OperationFailureException(reply.getError());
            } else {
                imageSizeAsked = reply.getSize();
            }
        } else if (url.startsWith("http") || url.startsWith("https")) {
            String len = null;
            HttpHeaders header = restf.getRESTTemplate().headForHeaders(url);
            try {
                len = header.getFirst("Content-Length");
            } catch (Exception e) {
                logger.warn(String.format("cannot get image.  The image url : %s. description: %s.name: %s",
                        url, msg.getDescription(), msg.getName()));
            }
            imageSizeAsked = len == null ? 0 : Long.parseLong(len);
        }
        return imageSizeAsked;
    }
}