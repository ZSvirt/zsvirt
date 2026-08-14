package org.zstack.zwatch.migratedb

import java.lang.Boolean

doc {

	title "审计数据清单"

	field {
		name "id"
		desc ""
		type "long"
		since "3.11.3"
	}
	field {
		name "createTime"
		desc ""
		type "long"
		since "3.11.3"
	}
	field {
		name "apiName"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "clientBrowser"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "clientIp"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "duration"
		desc ""
		type "long"
		since "3.11.3"
	}
	field {
		name "error"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "operator"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "requestDump"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "3.11.3"
	}
	field {
		name "requestUuid"
		desc "请求UUID"
		type "String"
		since "3.11.3"
	}
	field {
		name "operatorAccountUuid"
		desc "操作用户UUID"
		type "String"
		since "3.11.3"
	}
	field {
		name "responseDump"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "success"
		desc ""
		type "Boolean"
		since "3.11.3"
	}
	field {
		name "signedText"
		desc ""
		type "String"
		since "3.11.3"
	}
	field {
		name "resourceType"
		desc "资源类型"
		type "String"
		since "3.18.0"
	}
	field {
		name "resourceName"
		desc "资源名称"
		type "String"
		since "3.18.0"
	}
}
