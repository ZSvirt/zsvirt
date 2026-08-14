import org.junit.Test
import org.zstack.testlib.ApiHelperGenerator

/**
 * Created by xing5 on 2017/3/2.
 */
class TestGenerateApiHelper {
    @Test
    void test() {
        String dir = System.getProperty("user.home")
        new ApiHelperGenerator().generate([dir, "ApiHelper.groovy"].join("/"))
    }
}
