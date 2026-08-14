package org.zstack.pluginpremium.externalapiadapter

import com.aliyun.ak.sdk.core.model.AccessKeyInfo
import com.aliyun.ak.sdk.core.model.AccessKeyStatus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterGlobalPropertyConfigException
import org.zstack.pluginpremium.externalapiadapter.exception.APIAdapterSpecifiedErrorException
import org.zstack.pluginpremium.externalapiadapter.exception.InvalidParameterException
import org.zstack.sdk.ErrorCode
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_API_ACCESSKEYID_KEY
import static org.zstack.pluginpremium.externalapiadapter.ExternalAPIAdapterConstants.ECS_API_SIGNATURE_KEY
/**
 * Created by lining on 2018/4/22.
 */
class ExternalAPIAdapterUtils {
    private static final CLogger logger = Utils.getLogger(ExternalAPIAdapterUtils.class)

    static final Gson gson

    // key: accessKey, value: zstackAccountName
    private static final Map accountAccessKey = [:]

    // key: accessKey, value:  accessSecretKey
    private static final Map accessSecretKey = [:]

    static {
        gson = new GsonBuilder().create()
    }

    static def changeValueType(String value, Class typeClass) {
        if (value == null && typeClass == null) {
            return value
        }

        String typeClassName = typeClass.getName()
        if (typeClassName == String.class.getName()) {
            return value
        }

        if (typeClassName == Integer.class.getName()) {
            return Integer.parseInt(value)
        }

        if (typeClassName == ArrayList.class.getName()) {
            return gson.fromJson(value, typeClass)
        }
    }

    private static final String ENCODING = "UTF-8"

    private static String percentEncode(String value) throws UnsupportedEncodingException {
        return value != null ? URLEncoder.encode(value, ENCODING).replace("+", "%20").replace("*", "%2A").replace("%7E", "~") : null
    }

    static boolean validateECSAPISignature(Map ecsAPIParam, String accessSecretKey, String method) {
        String signature = ecsAPIParam.get(ECS_API_SIGNATURE_KEY)

        assert signature != null

        String validateSign = makeECSAPISignature(ecsAPIParam, accessSecretKey, method)
        return signature == validateSign
    }

    static boolean validateECSAPISignature(Map ecsAPIParam, String method) {
        if (accessSecretKey.isEmpty()) {
            loadAccountAccessKey()
        }

        String accessKeyId = ecsAPIParam.get(ECS_API_ACCESSKEYID_KEY)
        String accessSecret
        if (accessSecretKey.containsKey(accessKeyId)) {
            accessSecret = accessSecretKey.get(accessKeyId)
            logger.debug("Found access secret from external api adapter global properties. ak: ${accessKeyId}".toString())
        }
        if (accessSecret == null) {
            logger.debug("try get access secret from UmmAk")
            accessSecret = getSecretFromUmm(accessKeyId)
            if (accessSecret == null) {
                ErrorCode code = new ErrorCode(
                        details: "Can not verify accessKey[$accessKeyId], neither configured in the ZStack nor can be fetched from ummAk.".toString()
                )
                throw new InvalidParameterException(ECS_API_ACCESSKEYID_KEY, code)
            }
        }

        return validateECSAPISignature(ecsAPIParam, accessSecret, method)
    }

    static String getSecretFromUmm(String accesskey) {
        AccessKeyInfo accessKeyInfo = null
        try {
            accessKeyInfo = UmmAkUtil.getAccessKeyInfo(accesskey)
        } catch (Exception e) {
            logger.debug("failed to get ak info from ummAk.", e)
            throw new APIAdapterSpecifiedErrorException(
                    "service.timeout.ummak",
                    "cannot get ak info from ummAk, details:\n$e.message".toString()
            )
        } catch (Error err) {
            logger.debug("failed to start ummAkUtil", err)
            throw new APIAdapterSpecifiedErrorException(
                    "service.failed.ummak",
                    "cannot init ummAk util, details:\n$err.message".toString()
            )
        }

        if (AccessKeyStatus.ENABLED == accessKeyInfo.getStatus()) {
            logger.debug("got access secret from UmmAk".toString())
            return accessKeyInfo.getSecret()
        }
        logger.debug("access key not enabled.")
        return null
    }

    static String makeECSAPISignature(Map ecsAPIParam, String accessSecretKey, String method) {
        accessSecretKey += "&"

        Map parameters = ecsAPIParam.entrySet().stream()
                .filter{ x -> x.getKey() != ECS_API_SIGNATURE_KEY }
                .collect(Collectors.toMap({x -> x.getKey()}, {x -> x.getValue()}))

        if (method == null) {
            method = "GET"
        }

        String[] sortedKeys = parameters.keySet().toArray([])
        Arrays.sort(sortedKeys)

        final String SEPARATOR = "&"

        StringBuilder stringToSign = new StringBuilder()
        stringToSign.append(method).append(SEPARATOR)
        stringToSign.append(percentEncode("/")).append(SEPARATOR)
        StringBuilder canonicalizedQueryString = new StringBuilder()
        for(String key : sortedKeys) {
            canonicalizedQueryString.append("&")
                    .append(percentEncode(key)).append("=")
                    .append(percentEncode(parameters.get(key)))
        }

        stringToSign.append(percentEncode(
                canonicalizedQueryString.toString().substring(1)))

        final String ALGORITHM = "HmacSHA1"
        final String ENCODING = "UTF-8"
        Mac mac = Mac.getInstance(ALGORITHM)
        mac.init(new SecretKeySpec(accessSecretKey.getBytes(ENCODING), ALGORITHM))

        byte[] signData = mac.doFinal(stringToSign.toString().getBytes(ENCODING))
        String signature = new String(Base64.encodeBase64(signData))

        return signature
    }

    static String getZStackAccountNameByAccessKey(String accessKeyId) {
        if (accountAccessKey.isEmpty()) {
            loadAccountAccessKey()
        }

        String accountName = accountAccessKey.get(accessKeyId)
        if (accountName == null){
//            throw new InvalidParameterException(ECS_API_ACCESSKEYID_KEY, new ErrorCode(
//                    code: ECSErrorCode.InvalidParameter,
//                    details: "Cannot find account"
//            ))
            //todo this is a hack, need find a solution for this.
            List accountList = accountAccessKey.values().collect {it}
            return accountList.get(0)
        }

        return accountName
    }

    private synchronized static void loadAccountAccessKey() {
        if (StringUtils.isEmpty(ExternalAPIAdapterGlobalProperty.ZSTACK_ACCOUNT_PASSWORD_ACCESSKEY_ACCESSSECRET)) {
            throw new APIAdapterGlobalPropertyConfigException("ExternalAPIAdapter.ZStackAccountAccess", "not config")
        }

        String[] configs = ExternalAPIAdapterGlobalProperty.ZSTACK_ACCOUNT_PASSWORD_ACCESSKEY_ACCESSSECRET.split(",")
        if (configs.size() == 0) {
            throw new APIAdapterGlobalPropertyConfigException("ExternalAPIAdapter.ZStackAccountAccess", "format error")
        }

        for (String config : configs) {
            String[] accountAccessConfigs = splitAccountAccessConfig(config)
            if (accountAccessConfigs.size() != 4) {
                throw new APIAdapterGlobalPropertyConfigException("ExternalAPIAdapter.ZStackAccountAccess", "format error")
            }

            String accountName = accountAccessConfigs[0]
            String accessKey = accountAccessConfigs[2]
            accountAccessKey.put(accessKey, accountName)

            String accessSecret = accountAccessConfigs[3]
            accessSecretKey.put(accessKey, accessSecret)
        }
    }

    static String[] splitAccountAccessConfig(String config) {
        return config.split("::")
    }

    static String formatZonedDateTime(ZonedDateTime time) {
        return time.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    static ZonedDateTime convertDateTime(Date date) {
        return ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))
    }

    static String formatIso8601Date(Date date) {
        return formatZonedDateTime(convertDateTime(date))
    }

    static String randomUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "")
    }

    static String randomUUID(String name) {
        if (name == null) {
            return randomUUID()
        } else {
            return UUID.nameUUIDFromBytes(name.getBytes()).toString().replaceAll("-", "")
        }
    }

    static boolean checkParam(String paramName, Map paramMap) {
        def value = paramMap.get(paramName)
        if (value == null) {
            return false
        }
        return true
    }

}
