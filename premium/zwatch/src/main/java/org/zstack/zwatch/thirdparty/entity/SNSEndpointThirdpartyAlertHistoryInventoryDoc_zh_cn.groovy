package org.zstack.zwatch.thirdparty.entity

import java.sql.Timestamp

doc {

	title "第三方报警推送历史详细信息"

	field {
		name "alertUuid"
		desc "消息UUID"
		type "String"
		since "3.10"
	}
	field {
		name "endpointUuid"
		desc "接收端UUID"
		type "String"
		since "3.10"
	}
	field {
		name "subscriptionUuid"
		desc "事件订阅器UUID"
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
