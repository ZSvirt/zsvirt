package org.zstack.test.integration.storage.snapshot

import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.Test

/**
 * Created by mingjian.deng on 2019/1/7.*/
class SnapshotTest extends Test {
    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }
}
