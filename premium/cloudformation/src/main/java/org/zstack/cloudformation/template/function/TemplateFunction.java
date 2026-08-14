package org.zstack.cloudformation.template.function;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;

import java.util.List;

/**
 * Created by mingjian.deng on 2018/5/30.
 */
public interface TemplateFunction {
    Object getFunctionResult(JsonElement element, CfnResults result);

    /**
     * used for search the dependence resources from [Parameters] and [Resources]
     * return 'List<String>' is the list of dependence resource names
     */
    List<String> getRefValue(JsonElement element, CfnResults result);

    String getErrorInfo();
}
