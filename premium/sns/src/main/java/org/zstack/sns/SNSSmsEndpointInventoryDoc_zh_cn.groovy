package org.zstack.sns

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "短信接收端结构"

	field {
		name "receivers"
		desc "短信接收者"
		type "List"
		since "3.7.0"
	}
	field {
		name "name"
		desc "短信接收端名称"
		type "String"
		since "3.7.0"
	}
	field {
		name "uuid"
		desc "短信接收端的UUID，唯一标示该资源"
		type "String"
		since "3.7.0"
	}
	field {
		name "description"
		desc "短信接收端的详细描述"
		type "String"
		since "3.7.0"
	}
	field {
		name "type"
		desc "短信接收端类型"
		type "String"
		since "3.7.0"
	}
	field {
		name "state"
		desc "短信接收端状态"
		type "String"
		since "3.7.0"
	}
	field {
		name "platformUuid"
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
