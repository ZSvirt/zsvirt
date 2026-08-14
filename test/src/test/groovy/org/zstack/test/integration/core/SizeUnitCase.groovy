package org.zstack.test.integration.core

import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by MaJin on 2017-07-28.
 */
class SizeUnitCase extends SubCase{

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        long size = SizeUnit.MEGABYTE.toByte(128L)
        assert SizeUnit.GIGABYTE.convert(size, SizeUnit.BYTE) == 0
        assert SizeUnit.GIGABYTE.convert((double)size, SizeUnit.BYTE) > 0.124
        assert SizeUnit.GIGABYTE.convert((double)size, SizeUnit.BYTE) < 0.126
    }

    @Override
    void clean() {

    }
}
