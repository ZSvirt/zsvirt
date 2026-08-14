package org.zstack.sns.platform.email

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "邮箱地址"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.7.0"
	}
	field {
		name "emailAddress"
		desc ""
		type "String"
		since "3.7.0"
	}
	field {
		name "endpointUuid"
		desc ""
		type "String"
		since "3.7.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.7.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.7.0"
	}
}
