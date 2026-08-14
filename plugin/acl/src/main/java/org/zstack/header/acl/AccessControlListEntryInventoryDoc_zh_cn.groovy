package org.zstack.header.acl

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "访问控制信息"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.9"
	}
	field {
		name "aclUuid"
		desc ""
		type "String"
		since "3.9"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "3.9"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.9"
	}
	field {
		name "domain"
		desc "访问域的标识"
		type "String"
		since "3.9"
	}
	field {
		name "url"
		desc "URL 地址"
		type "String"
		since "3.9"
	}
	field {
		name "ipEntries"
		desc "IP 列表"
		type "String"
		since "3.9"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.9"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.9"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.9"
	}
}
