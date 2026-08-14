package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.Utils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.path.PathUtils;

public class TestPathUtil {

	@Test
	public void test() {
		PathUtils p = Utils.getPathUtil();
		
		System.out.println(p.join("a", "b", "c", "d"));
		System.out.println(p.join("/a", "/b", "c", "d"));
		System.out.println(p.join("/a", "/b", "//c", "d"));

        System.out.println(PathUtil.parentFolder("tmp/a"));
        System.out.println(PathUtil.parentFolder("/tmp/a"));
        System.out.println(PathUtil.parentFolder("tmp/a.xx"));
        System.out.println(PathUtil.parentFolder("/tmp/a.xx"));
        System.out.println(PathUtil.parentFolder("/tmp/a/a.xx"));
        System.out.println(PathUtil.parentFolder("/tmp/a/a"));
	}
}
