package org.zstack.zwatch.thirdparty.entity

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "第三方报警消息详细信息"

	field {
		name "uuid"
		desc "第三方报警消息UUID"
		type "String"
		since "3.10"
	}
	field {
		name "thirdpartyPlatformUuid"
		desc "第三方报警源UUID"
		type "String"
		since "3.10"
	}
	field {
		name "product"
		desc "产品"
		type "String"
		since "3.10"
	}
	field {
		name "service"
		desc "服务"
		type "String"
		since "3.10"
	}
	field {
		name "metric"
		desc "监控项"
		type "String"
		since "3.10"
	}
	field {
		name "alertLevel"
		desc "报警级别"
		type "String"
		since "3.10"
	}
	field {
		name "alertTime"
		desc "报警时间"
		type "Timestamp"
		since "3.10"
	}
	field {
		name "dimensions"
		desc "报警细节"
		type "String"
		since "3.10"
	}
	field {
		name "message"
		desc "报警内容"
		type "String"
		since "3.10"
	}
	field {
		name "dataSource"
		desc "数据源"
		type "String"
		since "3.10"
	}
	field {
		name "sourceText"
		desc "源消息"
		type "String"
		since "3.10"
	}
	field {
		name "readStatus"
		desc "已读状态"
		type "String"
		since "3.10"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.10"
	}
}
