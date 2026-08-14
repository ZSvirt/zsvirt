package org.zstack.zwatch.monitorgroup.entity

import org.zstack.zwatch.datatype.EmergencyLevel
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "事件报警模板"

	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.10.0"
	}
	field {
		name "monitorTemplateUuid"
		desc "监控模板UUID"
		type "String"
		since "3.10.0"
	}
	field {
		name "namespace"
		desc "名字空间"
		type "String"
		since "3.10.0"
	}
	field {
		name "eventName"
		desc "事件名"
		type "String"
		since "3.10.0"
	}
	ref {
		name "emergencyLevel"
		path "org.zstack.zwatch.monitorgroup.entity.EventRuleTemplateInventory.emergencyLevel"
		desc "报警等级"
		type "EmergencyLevel"
		since "3.10.0"
		clz EmergencyLevel.class
	}
	field {
		name "labels"
		desc "事件标签"
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
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.10.0"
	}
	field {
		name "uuid"
		desc "事件报警模板 UUID"
		type "String"
		since "3.10.0"
	}
}
