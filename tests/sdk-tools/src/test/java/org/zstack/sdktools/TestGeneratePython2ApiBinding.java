package org.zstack.sdktools;

import org.junit.Test;
import org.zstack.sdktools.apibinding.Python2ApiBindingGenerator;

public class TestGeneratePython2ApiBinding {
    @Test
    public void test() throws Exception {
        System.setProperty("exitJVMOnBootFailure", "false");
        new Python2ApiBindingGenerator().generate(null, null);
    }
}
