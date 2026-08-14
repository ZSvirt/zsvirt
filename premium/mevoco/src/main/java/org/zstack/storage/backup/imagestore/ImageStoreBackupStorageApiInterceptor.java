package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.agent.ProxyHardwareFactory;
import org.zstack.header.agent.ProxyHardware;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.List;
import java.util.Optional;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.list;

@InterceptorForService("storage.backup.imagestore")
public class ImageStoreBackupStorageApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddImageStoreBackupStorageMsg) {
            validate((APIAddImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIQueryImageStoreBackupStorageMsg) {
            validate((APIQueryImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateImageStoreBackupStorageMsg) {
            validate((APIUpdateImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APISyncImageFromImageStoreBackupStorageMsg) {
            validate((APISyncImageFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIRecoveryImageFromImageStoreBackupStorageMsg) {
            validate((APIRecoveryImageFromImageStoreBackupStorageMsg) msg);
        } else if (msg instanceof APIReclaimSpaceFromImageStoreMsg) {
            validate((APIReclaimSpaceFromImageStoreMsg) msg);
        }

        return msg;
    }

    private void validate(final APIReclaimSpaceFromImageStoreMsg msg) {
        DebugUtils.Assert(isImageStore(msg.getUuid()), "backup storage is not ImageStore");
    }

    private void isExisted(String bsUuid, String installPath) {
        String imageUuid = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, bsUuid)
                .eq(ImageBackupStorageRefVO_.installPath, installPath)
                .select(ImageBackupStorageRefVO_.imageUuid)
                .findValue();
        if (imageUuid != null) {
            throw new ApiMessageInterceptionException(argerr(
                    "target backup storage[uuid:%s] already contains the image [uuid:%s]", bsUuid, imageUuid));
        }
    }

    private boolean isImageStore(String bsUuid) {
        return Q.New(ImageStoreBackupStorageVO.class)
                .eq(ImageStoreBackupStorageVO_.uuid, bsUuid)
                .isExists();
    }

    private void validate(final APIRecoveryImageFromImageStoreBackupStorageMsg msg) {
        DebugUtils.Assert(isImageStore(msg.getSrcBackupStorageUuid()), "source backup storage is not ImageStore");
        DebugUtils.Assert(isImageStore(msg.getDstBackupStorageUuid()), "destination backup storage is not ImageStore");

        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class).
                eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getSrcBackupStorageUuid()).
                eq(ImageBackupStorageRefVO_.imageUuid, msg.getUuid()).find();
        if (ref == null) {
            throw new ApiMessageInterceptionException(Platform.argerr("source backup storage[%s] doesn't contain image[%s]",
                    msg.getSrcBackupStorageUuid(), msg.getUuid()));
        }

        isExisted(msg.getDstBackupStorageUuid(), ref.getInstallPath());
    }

    private void validate(final APISyncImageFromImageStoreBackupStorageMsg msg) {
        DebugUtils.Assert(isImageStore(msg.getSrcBackupStorageUuid()), "source backup storage is not ImageStore");
        DebugUtils.Assert(isImageStore(msg.getDstBackupStorageUuid()), "destination backup storage is not ImageStore");

        ImageBackupStorageRefVO ref = Q.New(ImageBackupStorageRefVO.class).
                eq(ImageBackupStorageRefVO_.backupStorageUuid, msg.getSrcBackupStorageUuid()).
                eq(ImageBackupStorageRefVO_.imageUuid, msg.getUuid()).find();
        if (ref == null) {
            throw new ApiMessageInterceptionException(Platform.argerr("src backupstorage[%s] doesn't contain image[%s]",
                    msg.getSrcBackupStorageUuid(), msg.getUuid()));
        }

        isExisted(msg.getDstBackupStorageUuid(), ref.getInstallPath());
    }

    private void validate(APIUpdateImageStoreBackupStorageMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
        }
    }

    private void validate(APIQueryImageStoreBackupStorageMsg msg) {
        boolean found = false;
        for (QueryCondition qcond : msg.getConditions()) {
            if ("type".equals(qcond.getName())) {
                qcond.setOp(QueryOp.EQ.toString());
                qcond.setValue(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE);
                found = true;
                break;
            }
        }

        if (!found) {
            msg.addQueryCondition("type", QueryOp.EQ, ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE);
        }
    }

    private void validate(APIAddImageStoreBackupStorageMsg msg) {
        if (!NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
        }

        List<String> sysTags = msg.getSystemTags();

        Optional.ofNullable(sysTags).ifPresent(tags -> tags.forEach(tag -> {
            if (ImageStoreSystemTags.SYNC_NETWORK.isMatch(tag)) {
                imageStoreCidrValidator(ImageStoreSystemTags.SYNC_NETWORK.getTokenByTag(tag, ImageStoreSystemTags.SYNC_NETWORK_TOKEN));
            }
            if (ImageStoreSystemTags.BACKUP_CIDR.isMatch(tag)) {
                imageStoreCidrValidator(ImageStoreSystemTags.BACKUP_CIDR.getTokenByTag(tag, ImageStoreSystemTags.BACKUP_CIDR_TOKEN));
            }
        }));

        SimpleQuery<ImageStoreBackupStorageVO> q = dbf.createQuery(ImageStoreBackupStorageVO.class);
        q.add(ImageStoreBackupStorageVO_.hostname, Op.EQ, msg.getHostname());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(operr("duplicate backup storage. There has been an image store backup storage[hostname:%s]", msg.getHostname()));
        }

        String dir = msg.getUrl();
        if (dir == null) {
            throw new ApiMessageInterceptionException(argerr("file path needed"));
        }

        if (!dir.startsWith("/")) {
            throw new ApiMessageInterceptionException(argerr("absolute file path required: %s", dir));
        }
        if (dir.startsWith("/proc") || dir.startsWith("/dev") || dir.startsWith("/sys")) {
            throw new ApiMessageInterceptionException(argerr("the url contains an invalid folder[/dev or /proc or /sys]"));
        }
        for (int counter = 1; counter < dir.length(); ++counter) {
            char ch = dir.charAt(counter);
            if (Character.isLetterOrDigit(ch) || ch == '.' || ch == '/' || ch == '_' || ch == '-') {
                continue;
            }

            throw new ApiMessageInterceptionException(argerr("file path contains invalid character: %s", dir));
        }

        if (msg.getPassword() == null) {
            ProxyHardware proxyHardware = getProxyHardware(msg.getHostname());
            if (proxyHardware == null || proxyHardware.getPassword() == null) {
                throw new ApiMessageInterceptionException(operr("the password for the physical machine [%s] is empty. " +
                        "please set a password", msg.getHostname()));
            }
            msg.setPassword(proxyHardware.getPassword());
            msg.setUsername(msg.getUsername() != null ? msg.getUsername() : proxyHardware.getUsername());
        }

        if (msg instanceof APIAddDisasterImageStoreBackupStorageMsg) {
            validate((APIAddDisasterImageStoreBackupStorageMsg) msg);
        }
    }

    private ProxyHardware getProxyHardware(String hostname) {
        for (ProxyHardwareFactory factory : pluginRgty.getExtensionList(ProxyHardwareFactory.class)) {
            ProxyHardware proxyHardware = factory.getProxyHardware(hostname);
            if (proxyHardware != null) {
                return proxyHardware;
            }
        }
        return null;
    }

    private void imageStoreCidrValidator(String cidr) {
        if (cidr != null) {
            String fmtCidr = NetworkUtils.fmtCidr(cidr);
            if (!fmtCidr.equals(cidr)) {
                throw new OperationFailureException(argerr("[%s] is not a standard cidr, do you mean [%s]?", cidr, fmtCidr));
            }
        }
    }

    private void validate(final APIAddDisasterImageStoreBackupStorageMsg msg) {
        if (msg.getEndPoint() != null) {
            DebugUtils.Assert(msg.getAttachPoint() != null, "if endpoint be set, then must set attach point");
            DebugUtils.Assert(msg.getAttachPoint().startsWith("/"), String.format("attach point must start with '/', but is: %s", msg.getAttachPoint()));
        }
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return list(
            APIAddImageStoreBackupStorageMsg.class,
            APIAddDisasterImageStoreBackupStorageMsg.class,
            APIUpdateImageStoreBackupStorageMsg.class,
            APIQueryImageStoreBackupStorageMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.DEFAULT;
    }
}
