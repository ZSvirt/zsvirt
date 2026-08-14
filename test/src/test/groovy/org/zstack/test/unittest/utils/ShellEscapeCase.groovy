package org.zstack.test.unittest.utils

import org.junit.Test
import org.zstack.utils.ShellUtils

class ShellEscapeCase {
    @Test
    void test() {
        assert ShellUtils.escapeShellText(null) == null
        assert ShellUtils.escapeShellText("") == ""
        assert ShellUtils.escapeShellText("     ") == "\\ \\ \\ \\ \\ "
        assert ShellUtils.escapeShellText("abc") == "abc"
        assert ShellUtils.escapeShellText("#123") == "\\#123"
        assert ShellUtils.escapeShellText("!@#\$") == "\\!@\\#\\\$"
        assert ShellUtils.escapeShellText(";init 0;") == "\\;init\\ 0\\;"
    }
}
