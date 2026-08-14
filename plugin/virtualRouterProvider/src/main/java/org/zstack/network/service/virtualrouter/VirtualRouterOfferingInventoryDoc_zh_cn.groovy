package org.zstack.network.service.virtualrouter

import java.lang.Boolean
import java.lang.Integer
import java.lang.Integer
import java.lang.Long
import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "managementNetworkUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "publicNetworkUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
	field {
		name "isDefault"
		desc ""
		type "Boolean"
		since "0.6"
	}
	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "0.6"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "cpuNum"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "cpuSpeed"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "memorySize"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "allocatorStrategy"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "sortKey"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
}
