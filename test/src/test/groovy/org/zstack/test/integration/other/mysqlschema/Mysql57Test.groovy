package org.zstack.test.integration.other.mysqlschema

import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

/**
 * Created by lining on 2017/2/27.
 */

/**
 * PR测试系统会把这个特别的Test放在mysql 5.7的环境去执行
 * 如果执行成功，就意味着db schema兼容mysql 5.7
 */
class Mysql57Test extends Test {
    static SpringSpec springSpec = makeSpring {
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        runSubCases()
    }
}
