package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;

import java.util.List;

/**
 * Created by frank on 9/16/2015.
 */
public interface AddImageExtensionPoint {
    void validateAddImage(List<String> bsUuids);

    void preAddImage(ImageInventory img);

    void beforeAddImage(ImageInventory img);

    void afterAddImage(ImageInventory img);

    void failedToAddImage(ImageInventory img, ErrorCode err);
}
