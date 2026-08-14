package org.zstack.zwatch.thirdparty;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;

/**
 * Create by yaoning.li at 2020/8/4
 */

@TagDefinition
public class ThirdpartyPlatformSystemTags {

    public static final String LAST_SYNC_THIRDPARTY_ALERT_ERROR_TOKEN = "lastSyncThirdpartyAlertError";
    public static PatternedSystemTag LAST_SYNC_THIRDPARTY_ALERT_ERROR = new PatternedSystemTag(
            String.format("lastSyncThirdpartyAlertError::{%s}", LAST_SYNC_THIRDPARTY_ALERT_ERROR_TOKEN),
            ThirdpartyPlatformVO.class
    );

    public static final String SYNC_ALERTS_SUPPORT_PAGING_TOKEN = "syncAlertsSupportPaging";
    public static PatternedSystemTag SYNC_ALERTS_SUPPORT_PAGING = new PatternedSystemTag(
            String.format("syncAlertsSupportPaging::{%s}", SYNC_ALERTS_SUPPORT_PAGING_TOKEN),
            ThirdpartyPlatformVO.class
    );
}
