package org.zstack.network.service.virtualrouter.vyos;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.rest.RESTFacade;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.*;

public class VyosVersionVersionManagerImpl implements VyosVersionManager {
    private final static CLogger logger = Utils.getLogger(VyosVersionVersionManagerImpl.class);

    @Override
    public void vyosRouterVersionCheck(String vrUuid, ReturnValueCompletion<VyosVersionCheckResult> completion) {
        VyosVersionCheckResult result = new VyosVersionCheckResult();

        String managementVersion = getManagementVersion();
        if (managementVersion == null) {
            completion.success(result);
            return;
        }

        String zvrVersion = new VirtualRouterMetadataOperator().getZvrVersion(vrUuid);
        if (zvrVersion == null) {
            logger.warn(String.format("virtual router[uuid: %s] has no zvr version tag", vrUuid));
            result.setNeedReconnect(true);
            result.setRebuildSnat(true);
            completion.success(result);
            return;
        }

        zvrVersion = zvrVersion.trim();
        if (!(VirtualRouterMetadataOperator.zvrVersionCheck(zvrVersion))) {
            logger.warn(String.format("virtual router[uuid: %s] version [%s] format error", vrUuid, zvrVersion));
            result.setNeedReconnect(true);
            result.setRebuildSnat(true);
            completion.success(result);
            return;
        }

        VersionComparator mnVersion = new VersionComparator(managementVersion);
        VersionComparator remoteVersion = new VersionComparator(zvrVersion);
        result.setVersion(zvrVersion);
        if (mnVersion.compare(remoteVersion) > 0) {
            logger.warn(String.format("virtual router[uuid: %s] version [%s] is older than management node version [%s]",vrUuid, zvrVersion, managementVersion));
            result.setNeedReconnect(true);
            if (remoteVersion.compare(VyosConstants.SNAT_REBUILD_VERSION) < 0) {
                result.setRebuildSnat(true);
            }
        } else {
            logger.debug(String.format("virtual router[uuid: %s] successfully finish the version check", vrUuid));
        }
        completion.success(result);
    }

    private String getManagementVersion() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "3.10.0.0";
        }

        String managementVersion = null;
        String path = null;
        try {
            path = PathUtil.findFileOnClassPath(VyosConstants.VYOS_VERSION_PATH, true).getAbsolutePath();
        } catch (RuntimeException e) {
            logger.error(String.format("vyos version file find file because %s", e.getMessage()));
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            managementVersion = br.readLine();
        } catch (IOException e) {
            logger.error(String.format("vyos version file %s read error: %s", path, e.getMessage()));
            return null;
        }

        if (!(VirtualRouterMetadataOperator.zvrVersionCheck(managementVersion))) {
            logger.error(String.format("vyos version file format error: %s", managementVersion));
            return null;
        }

        return managementVersion;
    }
}
