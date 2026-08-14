package org.zstack.zops



doc {

	title "ceph健康状态"

	field {
		name "OK"
		desc "健康"
		type "CephHealthStatus"
		since "3.17.21"
	}
	field {
		name "WARN"
		desc "警告"
		type "CephHealthStatus"
		since "3.17.21"
	}
	field {
		name "ERR"
		desc "错误"
		type "CephHealthStatus"
		since "3.17.21"
	}
	field {
		name "UNKNOWN"
		desc "未知"
		type "CephHealthStatus"
		since "3.17.21"
	}
}
