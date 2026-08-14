package org.zstack.header.image;

import org.zstack.header.description.PackageDescription;
import org.zstack.mevoco.MevocoConstants;

public class ImageMevocoRBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "image-mevoco";
    }

    {
        apis()
                .api(
                        APIGetImageQgaMsg.class,
                        APISetImageQgaMsg.class,
                        APISetImageSecurityLevelMsg.class
                )
                .toService(MevocoConstants.SERVICE_ID)
                .build();
    }
}
