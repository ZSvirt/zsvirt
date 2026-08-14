package org.zstack.zops

import org.zstack.zops.ChronyServerInfo

doc {

	title "时间源服务器对"

	ref {
		name "internal"
		path "org.zstack.zops.ChronyServerInfoPair.internal"
		desc "内部时间源服务器"
		type "ChronyServerInfo"
		since "3.17.21"
		clz ChronyServerInfo.class
	}
	ref {
		name "external"
		path "org.zstack.zops.ChronyServerInfoPair.external"
		desc "外部时间源服务器"
		type "ChronyServerInfo"
		since "3.17.21"
		clz ChronyServerInfo.class
	}
}
