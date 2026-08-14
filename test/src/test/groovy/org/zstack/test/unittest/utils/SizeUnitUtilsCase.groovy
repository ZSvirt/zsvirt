package org.zstack.test.unittest.utils;

import org.junit.Test;
import org.zstack.utils.SizeUtils;

class SizeUnitUtilsCase {
    @Test
    void testIsSizeUnit() {
        assert SizeUtils.isSizeString2("50GB")
        assert !SizeUtils.isSizeString2("50GG")

        assert SizeUtils.isSizeString2("50MB")
        assert !SizeUtils.isSizeString2("50MM")

        assert SizeUtils.isSizeString2("50KB")
        assert !SizeUtils.isSizeString2("50KK")

        assert !SizeUtils.isSizeString2("50")
    }
}