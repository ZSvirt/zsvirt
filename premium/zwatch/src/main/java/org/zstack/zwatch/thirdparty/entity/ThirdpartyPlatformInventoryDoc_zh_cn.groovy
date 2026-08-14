package org.zstack.zwatch.thirdparty.entity

import java.sql.Timestamp
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "第三方报警源详细信息"

	field {
		name "uuid"
		desc "平台UUID"
		type "String"
		since "3.10"
	}
	field {
		name "name"
		desc "平台名称"
		type "String"
		since "3.10"
	}
	field {
		name "type"
		desc "平台类型"
		type "String"
		since "3.10"
	}
	field {
		name "url"
		desc "平台地址"
		type "String"
		since "3.10"
	}
	field {
		name "template"
		desc "消息转换模板"
		type "String"
		since "3.10"
	}
	field {
		name "state"
		desc "平台状态"
		type "String"
		since "3.10"
	}
	field {
		name "description"
		desc "平台详细描述"
		type "String"
		since "3.10"
	}
	field {
		name "lastSyncDate"
		desc "上一次同步消息时间"
		type "Timestamp"
		since "3.10"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.10"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.10"
	}
}
