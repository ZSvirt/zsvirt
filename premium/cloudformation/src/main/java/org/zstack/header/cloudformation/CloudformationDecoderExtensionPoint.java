package org.zstack.header.cloudformation;

import org.zstack.cloudformation.template.decoder.CfnRootDecoder;

/**
 * Created by mingjian.deng on 2019/5/31.
 */
public interface CloudformationDecoderExtensionPoint {
    CfnRootDecoder getDecoderFromString(String type);
}
