package org.zstack.sns

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "sns主题订阅"

	field {
		name "topicUuid"
		desc "主题UUID"
		type "String"
		since "2.3.1"
	}
	field {
		name "endpointUuid"
		desc "终端UUID"
		type "String"
		since "2.3.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3.1"
	}
}
