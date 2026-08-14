package org.zstack.zwatch.alarm

import org.zstack.zwatch.datatype.Label.Operator

doc {

	title "事件订阅标签结构"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3.1"
	}
	field {
		name "key"
		desc "标签名"
		type "String"
		since "2.3.1"
	}
	ref {
		name "operator"
		path "org.zstack.zwatch.alarm.EventSubscriptionLabelInventory.operator"
		desc "操作符"
		type "Operator"
		since "2.3.1"
		clz Operator.class
	}
	field {
		name "value"
		desc "标签值"
		type "String"
		since "2.3.1"
	}
}
