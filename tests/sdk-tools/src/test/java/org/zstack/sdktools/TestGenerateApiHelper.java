package org.zstack.sdktools;

import org.junit.Test;
import org.zstack.utils.path.PathUtil;

public class TestGenerateApiHelper {
    @Test
    public void test() {
        System.setProperty("exitJVMOnBootFailure", "false");
        String path = PathUtil.join(System.getProperty("user.home"), "ApiHelper.groovy");
        System.out.println("generate ApiHelper: path=" + path);
        new ApiHelperGenerator().generate(path);
    }
}
