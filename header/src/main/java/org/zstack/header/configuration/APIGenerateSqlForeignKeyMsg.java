package org.zstack.header.configuration;

import org.zstack.header.message.APIMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

/**
 */
public class APIGenerateSqlForeignKeyMsg extends APIMessage {
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
 
    public static APIGenerateSqlForeignKeyMsg __example__() {
        APIGenerateSqlForeignKeyMsg msg = new APIGenerateSqlForeignKeyMsg();


        return msg;
    }

}
