package org.zstack.sns

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "SNS主题结构"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "2.3"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3"
	}
}
