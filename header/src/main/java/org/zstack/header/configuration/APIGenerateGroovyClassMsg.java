package org.zstack.header.configuration;

import org.zstack.header.message.APIMessage;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIGenerateGroovyClassMsg extends APIMessage {
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
 
    public static APIGenerateGroovyClassMsg __example__() {
        APIGenerateGroovyClassMsg msg = new APIGenerateGroovyClassMsg();


        return msg;
    }

}
