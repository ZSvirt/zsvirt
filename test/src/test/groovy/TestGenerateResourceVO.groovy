import org.junit.Test
import org.zstack.testlib.ResourceVOGenerator

/**
 * Created by xing5 on 2017/4/19.
 */
class TestGenerateResourceVO {
    @Test
    void test() {
        new ResourceVOGenerator().generate(System.getProperty("user.home"))
    }
}
