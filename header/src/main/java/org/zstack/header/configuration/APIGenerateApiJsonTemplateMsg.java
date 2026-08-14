package org.zstack.header.configuration;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.List;

public class APIGenerateApiJsonTemplateMsg extends APIMessage {
    @APIParam(required = false)
    private String exportPath;

    @APIParam(required = false)
    private List<String> basePackageNames;

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public List<String> getBasePackageNames() {
        return basePackageNames;
    }

    public void setBasePackageNames(List<String> basePackageNames) {
        this.basePackageNames = basePackageNames;
    }
 
    public static APIGenerateApiJsonTemplateMsg __example__() {
        APIGenerateApiJsonTemplateMsg msg = new APIGenerateApiJsonTemplateMsg();


        return msg;
    }

}
