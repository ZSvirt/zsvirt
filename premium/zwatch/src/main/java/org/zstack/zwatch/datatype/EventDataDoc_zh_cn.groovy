package org.zstack.zwatch.datatype

import org.zstack.zwatch.datatype.EventFamily.EmergencyLevel

doc {

	title "事件结构"

	field {
		name "namespace"
		desc "名字空间"
		type "String"
		since "2.3"
	}
	field {
		name "name"
		desc "事件名"
		type "String"
		since "2.3"
	}
	field {
		name "labels"
		desc "标签"
		type "Map"
		since "2.3"
	}
	ref {
		name "emergencyLevel"
		path "org.zstack.zwatch.datatype.EventData.emergencyLevel"
		desc "紧急程度"
		type "EmergencyLevel"
		since "2.3"
		clz EmergencyLevel.class
	}
	field {
		name "resourceId"
		desc "产生事件资源的ID（如果为ZStack资源则为资源UUID）"
		type "String"
		since "2.3"
	}
	field {
		name "resourceName"
		desc "资源名称"
		type "String"
		since "2.3"
	}
	field {
		name "error"
		desc "如果事件代表错误，该字段为错误详情"
		type "String"
		since "2.3"
	}
	field {
		name "time"
		desc "事件产生时间"
		type "long"
		since "2.3"
	}
}
