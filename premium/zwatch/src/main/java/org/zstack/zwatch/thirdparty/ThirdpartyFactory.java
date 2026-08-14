package org.zstack.zwatch.thirdparty;

import org.zstack.zwatch.thirdparty.api.APIAddThirdpartyPlatformMsg;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;

/**
 * Created by yaoning.li on 2020/7/15.
 */
public interface ThirdpartyFactory {
    String getThirdpartyType();

    ThirdpartyPlatformVO createMedia(ThirdpartyPlatformVO vo, APIAddThirdpartyPlatformMsg msg);

    ThirdpartyPlatformInventory getMediaInventory(ThirdpartyPlatformVO vo);

    ThirdpartyPlatformInventory getMediaInventory(String uuid);

    ThirdpartyPlatformBase getThirdpartyPlatformBase(String uuid);
}
