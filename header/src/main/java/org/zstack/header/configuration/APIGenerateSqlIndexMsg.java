package org.zstack.header.configuration;

import org.zstack.header.message.APIMessage;

import java.util.List;

/**
 */
public class APIGenerateSqlIndexMsg extends APIMessage {
    private String outputPath;
    private List<String> basePackageNames;

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public List<String> getBasePackageNames() {
        return basePackageNames;
    }

    public void setBasePackageNames(List<String> basePackageNames) {
        this.basePackageNames = basePackageNames;
    }
 
    public static APIGenerateSqlIndexMsg __example__() {
        APIGenerateSqlIndexMsg msg = new APIGenerateSqlIndexMsg();


        return msg;
    }

}
