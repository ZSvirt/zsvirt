package org.zstack.log.server

import org.zstack.log.server.LogCategory
import org.zstack.log.server.LogType
import org.zstack.log.server.LogLevel
import java.sql.Timestamp

doc {

	title "日志服务器"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "category"
		path "org.zstack.log.server.LogServerInventory.category"
		desc "null"
		type "LogCategory"
		since "1.0.0, zsv_5.1.0"
		clz LogCategory.class
	}
	ref {
		name "type"
		path "org.zstack.log.server.LogServerInventory.type"
		desc "null"
		type "LogType"
		since "1.0.0, zsv_5.1.0"
		clz LogType.class
	}
	ref {
		name "level"
		path "org.zstack.log.server.LogServerInventory.level"
		desc "null"
		type "LogLevel"
		since "1.0.0, zsv_5.1.0"
		clz LogLevel.class
	}
	field {
		name "configuration"
		desc ""
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "1.0.0, zsv_5.1.0"
	}
}
