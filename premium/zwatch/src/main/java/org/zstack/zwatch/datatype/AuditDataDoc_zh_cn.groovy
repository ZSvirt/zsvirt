package org.zstack.zwatch.datatype



doc {

	title "审计数据结构"

	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "2.3"
	}
	field {
		name "resourceType"
		desc "资源类型"
		type "String"
		since "2.3"
	}
	field {
		name "apiName"
		desc "API名称"
		type "String"
		since "2.3"
	}
	field {
		name "error"
		desc "错误详情"
		type "String"
		since "2.3"
	}
	field {
		name "operatorAccountUuid"
		desc "发起API账号UUID"
		type "String"
		since "2.3"
	}
	field {
		name "duration"
		desc "API耗时"
		type "long"
		since "2.3"
	}
	field {
		name "requestUuid"
		desc "API请求UUID"
		type "String"
		since "2.3"
	}
	field {
		name "responseUuid"
		desc "API返回UUID"
		type "String"
		since "2.3"
	}
	field {
		name "sessionUuid"
		desc "会话UUID"
		type "String"
		since "2.3"
	}
	field {
		name "requestDump"
		desc "API请求JSON存档"
		type "String"
		since "2.3"
	}
	field {
		name "responseDump"
		desc "API返回JSON存档"
		type "String"
		since "2.3"
	}
	field {
		name "time"
		desc "记录生成时间"
		type "long"
		since "2.3"
	}
}
