package org.zstack.image;

import org.zstack.header.image.ImageVO;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 * Created by xing5 on 2016/7/18.
 */
@TagDefinition
public class ImageSystemTags {
    public static String IMAGE_NAME_TOKEN = "imageName";
    public static String PRIMARY_STORAGE_UUID_TOKEN = "primaryStorageUuid";
    public static String IMAGE_INJECT_QEMUGA_TOKEN = "qemuga";
    public static PatternedSystemTag IMAGE_INJECT_QEMUGA = new PatternedSystemTag(String.format("%s", IMAGE_INJECT_QEMUGA_TOKEN), ImageVO.class);
    public static PatternedSystemTag DELETED_IMAGE_CACHE = new PatternedSystemTag(
            String.format("imageName::{%s}::primaryStorageUuid::{%s}", IMAGE_NAME_TOKEN, PRIMARY_STORAGE_UUID_TOKEN),
            ImageVO.class
    );

    public static String IMAGE_DEPLOY_REMOTE_TOKEN = "remote";
    public static PatternedSystemTag IMAGE_DEPLOY_REMOTE = new PatternedSystemTag(String.format("%s", IMAGE_DEPLOY_REMOTE_TOKEN), ImageVO.class);

    public static String IMAGE_SEVER_CERT_TOKEN = "imageCert";
    public static PatternedSystemTag IMAGE_SERVER_CERT = new PatternedSystemTag(
            String.format("image::cert::{%s}", IMAGE_SEVER_CERT_TOKEN),
            ImageVO.class
    );

    public static String IMAGE_SOURCE_TYPE_TOKEN = "sourceType";
    public static PatternedSystemTag IMAGE_SOURCE_TYPE = new PatternedSystemTag(String.format("sourceType::{%s}", IMAGE_SOURCE_TYPE_TOKEN), ImageVO.class);

    public static String BOOT_MODE_TOKEN = "bootMode";
    public static PatternedSystemTag BOOT_MODE = new PatternedSystemTag(String.format("bootMode::{%s}", BOOT_MODE_TOKEN), ImageVO.class);

    public static String IMAGE_CREATED_BY_SYSTEM_TOKEN = "CreatedBySystem";
    public static SystemTag IMAGE_CREATED_BY_SYSTEM = new SystemTag(IMAGE_CREATED_BY_SYSTEM_TOKEN, ImageVO.class);

    public static SystemTag TEMPORARY_IMAGE = new SystemTag("temporary", ImageVO.class);

    public static String IMAGE_GUEST_TOOLS_VERSION_TOKEN = "guestToolsVersion";
    public static PatternedSystemTag IMAGE_GUEST_TOOLS =
            new PatternedSystemTag(String.format("GuestTools::{%s}", IMAGE_GUEST_TOOLS_VERSION_TOKEN), ImageVO.class);

    public static PatternedSystemTag PACKER_BUILD = new PatternedSystemTag("packer", ImageVO.class);

    public static String APPLIANCE_IMAGE_TYPE_TOKEN = "applianceType";
    public static PatternedSystemTag APPLIANCE_IMAGE_TYPE = new PatternedSystemTag(String.format("applianceType::{%s}", APPLIANCE_IMAGE_TYPE_TOKEN), ImageVO.class);

    public static String IMAGE_ID ="imageId";
    public static PatternedSystemTag UPLOAD_IMAGE_INFO = new PatternedSystemTag(String.format("uploadImage::{%s}", IMAGE_ID), LongJobVO.class);
}
