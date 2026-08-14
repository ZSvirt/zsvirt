package org.zstack.testlib

/**
 * Created by xing5 on 2017/3/3.
 */
interface Case {
    void environment()
    void test()
    void run()
    void clean()
}