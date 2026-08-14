package org.zstack.storage.primary.license;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageLicenseInfo;
import org.zstack.header.storage.primary.PrimaryStorageLicenseInfoFactory;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephGlobalProperty;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO_;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.TimeUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * author:kaicai.hu
 * Date:2019/7/24
 */
public class XskyLicenseInfoFactory implements PrimaryStorageLicenseInfoFactory {
    @Autowired
    private RESTFacade restf;

    public static final String licenseType = "xsky";
    public static final String QUERY_XSKY_LICENSE_PATH = "/v1/licenses";
    public static final String QUERY_XSKY_LICENSE_TEST_PATH = "/licenses/test";

    @Override
    public void getPrimaryStorageLicenseInfo(String primaryStorageUuid, ReturnValueCompletion<PrimaryStorageLicenseInfo> completion) {
        final String monHost = Q.New(CephPrimaryStorageMonVO.class)
                .eq(CephPrimaryStorageMonVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(CephPrimaryStorageMonVO_.status, MonStatus.Connected)
                .select(CephPrimaryStorageMonVO_.hostname)
                .limit(1)
                .findValue();
        if (monHost == null) {
            completion.fail(operr("failed to get primaryStorage[%s] license info, because no MonIP available", primaryStorageUuid));
            return;
        }

        final String url = CoreGlobalProperty.UNIT_TEST_ON ?
                buildUrl("127.0.0.1", "8989", QUERY_XSKY_LICENSE_TEST_PATH) :
                buildUrl(monHost, CephGlobalProperty.GET_XSKY_LICENSE_PORT, QUERY_XSKY_LICENSE_PATH);

        XskyLicenseReturnStruct result = restf.syncJsonGet(url, null, null, XskyLicenseReturnStruct.class, TimeUnit.SECONDS, 5);
        if (result.getLicenses() == null || result.getLicenses().isEmpty()) {
            completion.fail(operr("failed to get primaryStorage[%s] license info, because no data returned", primaryStorageUuid));
            return;
        }

        List<XskyLicenseReturnStruct.License> licenses = result.getLicenses().stream().filter(l -> l.getActive() == Boolean.TRUE).collect(Collectors.toList());
        if (!licenses.isEmpty()) {
            XskyLicenseReturnStruct.License license = licenses.get(0);
            if (StringUtils.isEmpty(license.getExpired_time())) {
                completion.fail(operr("failed to get primaryStorage[%s] license info, because expired_time is null", primaryStorageUuid));
                return;
            }

            String expired_time = TimeUtils.parseRFC3339DateTime(license.getExpired_time());
            if (expired_time == null) {
                completion.fail(operr("failed to parse the date format[%s] of the primaryStorage[%s] license info", license.getExpired_time(), primaryStorageUuid));
                return;
            }

            PrimaryStorageLicenseInfo primaryStorageLicenseInfo = new PrimaryStorageLicenseInfo();
            primaryStorageLicenseInfo.setType(CephConstants.CEPH_PRIMARY_STORAGE_TYPE);
            OffsetDateTime dt = Instant.parse(expired_time).atOffset(ZoneOffset.UTC);
            primaryStorageLicenseInfo.setExpireTime(dt);
            primaryStorageLicenseInfo.setUuid(primaryStorageUuid);
            completion.success(primaryStorageLicenseInfo);
        } else {
            completion.fail(operr("failed to get primaryStorage[%s] license info, because the returned data does not have an active license", primaryStorageUuid));
        }
    }

    @Override
    public String getPrimaryStorageVendor() {
        return licenseType;
    }

    private static String buildUrl(String monAddr, String port, String path) {
        return String.format("http://%s:%s%s", monAddr, port, path);
    }

    @Override
    public void createPrimaryStorageVendorSystemTag(String primaryStorageUuid, String type) {
        if (licenseType.equals(type)) {
            SystemTagCreator creator = PrimaryStorageSystemTags.PRIMARY_STORAGE_VENDOR.newSystemTagCreator(primaryStorageUuid);
            creator.setTagByTokens(map(
                    e(PrimaryStorageSystemTags.PRIMARY_STORAGE_VENDOR_TOKEN, licenseType)
            ));
            creator.ignoreIfExisting = true;
            creator.inherent = false;
            creator.create();
        }
    }
}
