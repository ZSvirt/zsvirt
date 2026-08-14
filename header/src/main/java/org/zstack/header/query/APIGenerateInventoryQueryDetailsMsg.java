package org.zstack.header.query;

import org.zstack.header.message.APIMessage;

import java.util.List;

public class APIGenerateInventoryQueryDetailsMsg extends APIMessage {
    private String outputDir;
    private List<String> basePackageNames;

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public List<String> getBasePackageNames() {
        return basePackageNames;
    }

    public void setBasePackageNames(List<String> basePackageNames) {
        this.basePackageNames = basePackageNames;
    }
 
    public static APIGenerateInventoryQueryDetailsMsg __example__() {
        APIGenerateInventoryQueryDetailsMsg msg = new APIGenerateInventoryQueryDetailsMsg();


        return msg;
    }

}
