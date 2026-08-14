package org.zstack.utils.path;

import java.io.File;

public class PathUtilImpl implements PathUtils {
	@Override
	public String join(String... paths) {
		assert paths != null && paths.length > 0;
		
		File parent = new File(paths[0]);
		for (int i=1; i<paths.length; i++) {
			parent = new File(parent, paths[i]);
		}
		return parent.getPath();
	}
}
