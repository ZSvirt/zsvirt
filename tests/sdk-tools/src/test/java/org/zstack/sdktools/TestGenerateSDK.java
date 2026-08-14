package org.zstack.sdktools;

import org.junit.Test;
import org.zstack.utils.path.PathUtil;

public class TestGenerateSDK {
    @Test
    public void test() {
        System.setProperty("exitJVMOnBootFailure", "false");
        String path = PathUtil.join(System.getProperty("user.home"), "zstack-sdk/java");
        System.out.println("generate java sdk by RestServer: path=" + path);
        JavaSdkGenerator.generateJavaSdk(path);
    }
}
