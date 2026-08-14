package org.zstack.billing;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class BillingSystemTags {
    public static String PRICE_GPU_OFFERING_UUID_TOKEN = "gpuOfferingUuid";
    public static PatternedSystemTag PRICE_GPU_OFFERING_UUID = new PatternedSystemTag(String.format("gpuOfferingUuid::{%s}", PRICE_GPU_OFFERING_UUID_TOKEN), PriceVO.class);

    public static String PRICE_BAREMETAL2_OFFERING_UUID_TOKEN = "baremetal2OfferingUuid";
    public static PatternedSystemTag PRICE_BAREMETAL2_OFFERING_UUID = new PatternedSystemTag(String.format("baremetal2OfferingUuid::{%s}", PRICE_BAREMETAL2_OFFERING_UUID_TOKEN), PriceVO.class);

    public static String PRICE_USER_CONFIG_TOKEN = "priceUserConfig";
    public static PatternedSystemTag PRICE_USER_CONFIG = new PatternedSystemTag(String.format("priceUserConfig::{%s}", PRICE_USER_CONFIG_TOKEN), PriceVO.class);

    public static String VOLUME_PRICE_USER_CONFIG_TOKEN = "volumePriceUserConfig";
    public static PatternedSystemTag VOLUME_PRICE_USER_CONFIG = new PatternedSystemTag(String.format("volumePriceUserConfig::{%s}", VOLUME_PRICE_USER_CONFIG_TOKEN), VolumeVO.class);

    public static String VOLUME_ATTRIBUTE_USER_CONFIG_TOKEN = "volumeAttributeUserConfig";
    public static PatternedSystemTag VOLUME_ATTRIBUTE_USER_CONFIG = new PatternedSystemTag(String.format("volumeAttributeUserConfig::{%s}", VOLUME_ATTRIBUTE_USER_CONFIG_TOKEN), VolumeVO.class);
}