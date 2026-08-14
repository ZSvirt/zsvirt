package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;

/**
 * Created by mingjian.deng on 2018/5/28.
 */
public class TemplateVersionDecoder extends AbstractCfnRootDecoder {
    @Override
    public void decode(JsonElement element, CfnResults result) {
        if (element.isJsonPrimitive()) {
            result.setTemplateVersion(element.getAsString());
        }
    }
}
