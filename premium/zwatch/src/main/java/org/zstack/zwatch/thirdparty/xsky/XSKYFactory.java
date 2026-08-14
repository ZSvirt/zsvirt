package org.zstack.zwatch.thirdparty.xsky;

import org.zstack.core.db.Q;
import org.zstack.zwatch.thirdparty.Constants;
import org.zstack.zwatch.thirdparty.ThirdpartyFactory;
import org.zstack.zwatch.thirdparty.ThirdpartyPlatformBase;
import org.zstack.zwatch.thirdparty.api.APIAddThirdpartyPlatformMsg;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformInventory;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO;
import org.zstack.zwatch.thirdparty.entity.ThirdpartyPlatformVO_;

/**
 * Created by yaoning.li on 2020/7/15.
 */
public class XSKYFactory implements ThirdpartyFactory {
    @Override
    public String getThirdpartyType() {
        return Constants.XSKY;
    }

    @Override
    public ThirdpartyPlatformVO createMedia(ThirdpartyPlatformVO vo, APIAddThirdpartyPlatformMsg msg) {
        return vo;
    }

    @Override
    public ThirdpartyPlatformInventory getMediaInventory(ThirdpartyPlatformVO vo) {
        return ThirdpartyPlatformInventory.valueOf(vo);
    }

    @Override
    public ThirdpartyPlatformInventory getMediaInventory(String uuid) {
        ThirdpartyPlatformVO vo = Q.New(ThirdpartyPlatformVO.class)
                .eq(ThirdpartyPlatformVO_.uuid, uuid)
                .find();
        return ThirdpartyPlatformInventory.valueOf(vo);
    }

    @Override
    public ThirdpartyPlatformBase getThirdpartyPlatformBase(String uuid) {
        ThirdpartyPlatformVO vo = Q.New(ThirdpartyPlatformVO.class)
                .eq(ThirdpartyPlatformVO_.uuid, uuid)
                .find();
        XSKYPlatformBase xskyPlatformBase = new XSKYPlatformBase(vo);
        return xskyPlatformBase;
    }
}
