package org.zstack.ha

import java.sql.Timestamp

doc {

	title "Ha策略"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.0"
	}
	field {
		name "fencerName"
		desc "fencer 名字"
		type "String"
		since "3.17.0"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "3.17.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.0"
	}
}
