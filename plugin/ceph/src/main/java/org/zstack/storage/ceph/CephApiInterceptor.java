package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.primary.APIAttachPrimaryStorageToClusterMsg;
import org.zstack.storage.ceph.backup.*;
import org.zstack.storage.ceph.primary.*;
import org.zstack.utils.CharacterUtils;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by frank on 7/29/2015.
 */
public class CephApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(CephApiInterceptor.class);

    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private static final String MON_URL_FORMAT = "sshUsername:sshPassword@hostname:[sshPort]/?[monPort=]";

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddCephBackupStorageMsg) {
            validate((APIAddCephBackupStorageMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStorageMsg) {
            validate((APIAddCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIAddMonToCephBackupStorageMsg) {
            validate((APIAddMonToCephBackupStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephBackupStorageMonMsg) {
            validate((APIUpdateCephBackupStorageMonMsg) msg);
        } else if (msg instanceof APIAddMonToCephPrimaryStorageMsg) {
            validate((APIAddMonToCephPrimaryStorageMsg) msg);
        } else if (msg instanceof APIUpdateCephPrimaryStorageMonMsg) {
            validate((APIUpdateCephPrimaryStorageMonMsg) msg);
        } else if (msg instanceof APIDeleteCephPrimaryStoragePoolMsg) {
            validate((APIDeleteCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIAddCephPrimaryStoragePoolMsg) {
            validate((APIAddCephPrimaryStoragePoolMsg) msg);
        } else if (msg instanceof APIUpdateCephPrimaryStoragePoolMsg) {
            validate((APIUpdateCephPrimaryStoragePoolMsg) msg);
        }
        
        return msg;
    }

    private void validate(APIUpdateCephPrimaryStoragePoolMsg msg) {
        String psUuid = Q.New(CephPrimaryStoragePoolVO.class)
                .select(CephPrimaryStoragePoolVO_.primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.uuid, msg.getUuid())
                .findValue();

        msg.setPrimaryStorageUuid(psUuid);
    }

    private void validate(APIAddCephPrimaryStoragePoolMsg msg) {
        if (!CharacterUtils.checkCharacter(msg.getPoolName())){
            throw new ApiMessageInterceptionException(argerr("operation failure, because the poolName[poolName:%s] can not include unprintable ascii characters.", msg.getPoolName()));
        }

        String duplicatePoolUuid = Q.New(CephPrimaryStoragePoolVO.class)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, msg.getPrimaryStorageUuid())
                .eq(CephPrimaryStoragePoolVO_.poolName, msg.getPoolName())
                .eq(CephPrimaryStoragePoolVO_.type, msg.getType())
                .select(CephPrimaryStoragePoolVO_.uuid).findValue();
        if (duplicatePoolUuid != null && msg.isCreate()) {
            throw new ApiMessageInterceptionException(argerr(
                    "creation failure, duplicate poolName[%s]. There has been a pool[uuid:%s] with the same name existing.",
                    msg.getPoolName(), duplicatePoolUuid));

        } else if (duplicatePoolUuid != null && !msg.isCreate()) {
            throw new ApiMessageInterceptionException(argerr(
                    "Ceph pool[uuid:%s] with this name is already added into ZStack and used elsewhere, cannot reuse the ceph pool.",
                    duplicatePoolUuid));
        }

    }

    private void validate(APIDeleteCephPrimaryStoragePoolMsg msg) {
        msg.setPrimaryStorageUuid(
                Q.New(CephPrimaryStoragePoolVO.class).select(CephPrimaryStoragePoolVO_.primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.uuid, msg.getUuid()).findValue()
        );
    }

    private void checkExistingPrimaryStorage(List<String> monUrls) {
        List<String> hostnames = transformAndRemoveNull(monUrls, url -> new MonUri(url).getHostname());

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.select(CephPrimaryStorageMonVO_.hostname);
        q.add(CephPrimaryStorageMonVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("cannot add ceph primary storage, there has been some ceph primary storage using mon[hostnames:%s]", existing));
        }
    }

    private void distinctMons(List<String> mons) {
        List<String> monUrls = new ArrayList<>();
        for(String mon: mons) {
            MonUri uri = new MonUri(mon);
            if (!monUrls.contains(uri.getHostname())) {
                monUrls.add(uri.getHostname());
            } else {
                throw new ApiMessageInterceptionException(argerr("Cannot add same host[%s] in mons", uri.getHostname()));
            }
        }
    }

    private void validate(APIAddMonToCephPrimaryStorageMsg msg) {
        checkMonUrls(msg.getMonUrls());
        List<String> hostnames = msg.getMonUrls().stream()
                .map(MonUri::new)
                .map(MonUri::getHostname)
                .collect(Collectors.toList());

        if (Q.New(CephPrimaryStorageMonVO.class).in(CephPrimaryStorageMonVO_.hostname, hostnames).isExists()){
            throw new ApiMessageInterceptionException(argerr("Adding the same Mon node is not allowed"));
        }
    }

    private void validate(APIAddMonToCephBackupStorageMsg msg) {
        checkMonUrls(msg.getMonUrls());

        List<String> hostnames = msg.getMonUrls().stream()
                .map(MonUri::new)
                .map(MonUri::getHostname)
                .collect(Collectors.toList());

        if (Q.New(CephBackupStorageMonVO.class).in(CephBackupStorageMonVO_.hostname, hostnames).isExists()){
            throw new ApiMessageInterceptionException(argerr("Adding the same Mon node is not allowed"));
        }
    }
    private void validate(APIUpdateCephBackupStorageMonMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname()));
        }
        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.select(CephBackupStorageMonVO_.backupStorageUuid);
        q.add(CephPrimaryStorageMonVO_.uuid, Op.EQ, msg.getMonUuid());
        String bsUuid = q.findValue();
        msg.setBackupStorageUuid(bsUuid);
    }

    private void validate(APIUpdateCephPrimaryStorageMonMsg msg) {
        if (msg.getHostname() != null && !NetworkUtils.isIpv4Address(msg.getHostname()) && !NetworkUtils.isHostname(msg.getHostname())) {
            throw new ApiMessageInterceptionException(argerr(
                    String.format("hostname[%s] is neither an IPv4 address nor a valid hostname", msg.getHostname())
            ));
        }

        SimpleQuery<CephPrimaryStorageMonVO> q = dbf.createQuery(CephPrimaryStorageMonVO.class);
        q.select(CephPrimaryStorageMonVO_.primaryStorageUuid);
        q.add(CephPrimaryStorageMonVO_.uuid, Op.EQ, msg.getMonUuid());
        String psUuid = q.findValue();
        msg.setPrimaryStorageUuid(psUuid);
    }

    private void checkMonUrls(List<String> monUrls) {
        distinctMons(monUrls);

        for (String monUrl : monUrls) {
            String url = String.format("ssh://%s", monUrl);
            try {
                new MonUri(url);
            } catch (OperationFailureException ae) {
                throw new ApiMessageInterceptionException(ae.getErrorCode());
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                throw new ApiMessageInterceptionException(argerr("invalid monUrl[%s]. A valid url is in format of %s", monUrl, MON_URL_FORMAT));
            }
        }
    }

    private void validate(APIAddCephPrimaryStorageMsg msg) {
        if (msg.getDataVolumePoolName() != null && msg.getDataVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
                    "dataVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getRootVolumePoolName() != null && msg.getRootVolumePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
                    "rootVolumePoolName can be null but cannot be an empty string"
            ));
        }
        if (msg.getImageCachePoolName() != null && msg.getImageCachePoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr(
                    "imageCachePoolName can be null but cannot be an empty string"
            ));
        }

        checkMonUrls(msg.getMonUrls());
        checkExistingPrimaryStorage(msg.getMonUrls());
    }

    private void checkExistingBackupStorage(List<String> monUrls) {
        List<String> hostnames = transformAndRemoveNull(monUrls, url -> new MonUri(url).getHostname());

        SimpleQuery<CephBackupStorageMonVO> q = dbf.createQuery(CephBackupStorageMonVO.class);
        q.select(CephBackupStorageMonVO_.hostname);
        q.add(CephBackupStorageMonVO_.hostname, Op.IN, hostnames);
        List<String> existing = q.listValue();
        if (!existing.isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("cannot add ceph backup storage, there has been some ceph backup storage using mon[hostnames:%s]", existing));
        }
    }

    private void validate(APIAddCephBackupStorageMsg msg) {
        if (msg.getPoolName() != null && msg.getPoolName().isEmpty()) {
            throw new ApiMessageInterceptionException(argerr("poolName can be null but cannot be an empty string"));
        }else if(msg.isImportImages() && msg.getPoolName() == null){
            throw new ApiMessageInterceptionException(argerr("poolName is required when importImages is true"));
        }

        checkMonUrls(msg.getMonUrls());
        checkExistingBackupStorage(msg.getMonUrls());
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(
            APIAddCephBackupStorageMsg.class,
            APIAddMonToCephBackupStorageMsg.class,
            APIUpdateCephBackupStorageMonMsg.class,
            APIRemoveMonFromCephBackupStorageMsg.class,
            APIAddCephPrimaryStorageMsg.class,
            APIAddMonToCephPrimaryStorageMsg.class,
            APIUpdateCephPrimaryStorageMonMsg.class,
            APIRemoveMonFromCephPrimaryStorageMsg.class,
            APIAddCephPrimaryStoragePoolMsg.class,
            APIUpdateCephPrimaryStoragePoolMsg.class,
            APIDeleteCephPrimaryStoragePoolMsg.class,
            APIAttachPrimaryStorageToClusterMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
