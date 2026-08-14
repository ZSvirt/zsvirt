package org.zstack.pluginpremium.externalapiadapter;

import com.aliyun.ak.sdk.core.model.AccessKeyInfo;
import com.aliyun.ak.sdk.core.model.AccessKeyStatus;
import com.aliyun.ak.sdk.proxy.AkClient;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isBlank;
/**
 * Created by Qi Le on 2019-07-24
 */
public class UmmAkUtil {
    private static final CLogger logger = Utils.getLogger(UmmAkUtil.class);

    private static AkClient getAkClient() {
        AkClient akClient = new AkClient();
        String key = ExternalAPIAdapterGlobalProperty.UMM_ACCESSKEY_ID;
        String secret = ExternalAPIAdapterGlobalProperty.UMM_ACCESSKEY_SECRET;
        String baseUrl = ExternalAPIAdapterGlobalProperty.UMM_BASE_URL;
        akClient.setKey(key);
        akClient.setSecret(secret);
        akClient.setBaseUrl(baseUrl);
        akClient.setConnectTimeout(5000);

        return akClient;
    }

    public static AccessKeyInfo getAccessKeyInfo (String accessKeyId) throws IOException {
        AkClient akClient = getAkClient();
        return akClient.getAccessKeyInfo(accessKeyId);
    }

    public static boolean validateAccessKey(String pk, String accessKeyId, String accessKeySecret) throws IOException {
        if (isBlank(pk) || isBlank(accessKeyId) || isBlank(accessKeySecret)) {
            logger.error(String.format("pk/accessKeyId/accessKeySecret is blank, pk: [%s], accessKeyId: [%s]", pk, accessKeyId));
            return false;
        }

        AccessKeyInfo accessKeyInfo = getAccessKeyInfo(accessKeyId);
        if (!pk.equalsIgnoreCase(accessKeyInfo.getOwner() + "")
                || !accessKeySecret.equalsIgnoreCase(accessKeyInfo.getSecret())
                || !AccessKeyStatus.ENABLED.equals(accessKeyInfo.getStatus())) {
            return false;
        }

        return true;
    }

    public static boolean validateAccessKey(String accessKeyId, String accessKeySecret) throws IOException {
        if (isBlank(accessKeyId) || isBlank(accessKeySecret)) {
            logger.error(String.format("accessKeyId/accessKeySecret is blank, pk: [], accessKeyId: [%s]", accessKeyId));
            return false;
        }

        AccessKeyInfo accessKeyInfo = getAccessKeyInfo(accessKeyId);
        if (!accessKeySecret.equalsIgnoreCase(accessKeyInfo.getSecret())
                || !AccessKeyStatus.ENABLED.equals(accessKeyInfo.getStatus())) {
            return false;
        }

        return true;
    }

    public static String getActiveAccessKeySecret(String pk, String accessKeyId) throws IOException {
        if (isBlank(pk) || isBlank(accessKeyId)) {
            logger.error(String.format("pk/accessKeyId is blank, pk: [%s], accessKeyId: [%s]", pk, accessKeyId));
            return null;
        }

        AccessKeyInfo accessKeyInfo = getAccessKeyInfo(accessKeyId);
        if (!pk.equalsIgnoreCase(accessKeyInfo.getOwner() + "")
                || !AccessKeyStatus.ENABLED.equals(accessKeyInfo.getStatus())) {
            return null;
        }

        return accessKeyInfo.getSecret();
    }
}
