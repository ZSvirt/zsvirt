import org.junit.Test
import org.zstack.rest.RestServer

import java.nio.file.Paths

/**
 * Created by xing5 on 2016/12/21.
 */
class TestGenerateMarkDownDoc {

    @Test
    void test() {
        RestServer.generateMarkdownDoc(Paths.get("../").toAbsolutePath().normalize().toString())
    }
}
