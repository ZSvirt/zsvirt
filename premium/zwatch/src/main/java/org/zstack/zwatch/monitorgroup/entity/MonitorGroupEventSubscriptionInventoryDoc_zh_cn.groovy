package org.zstack.zwatch.monitorgroup.entity

import java.sql.Timestamp

doc {

	title "资源分组事件报警器"

	field {
		name "groupUuid"
		desc "资源分组 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "eventSubscriptionUuid"
		desc "事件报警器 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "eventRuleTemplateUuid"
		desc "事件报警模板 UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.10.0"
	}
}
