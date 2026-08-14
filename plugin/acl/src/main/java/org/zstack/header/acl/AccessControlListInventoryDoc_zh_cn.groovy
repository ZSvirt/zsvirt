package org.zstack.header.acl

import java.lang.Integer
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.acl.AccessControlListEntryInventory

doc {

	title "访问控制策略组清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
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
		name "ipVersion"
		desc "IP地址版本"
		type "Integer"
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
	ref {
		name "entries"
		path "org.zstack.header.acl.AccessControlListInventory.entries"
		desc "IP组清单列表"
		type "List"
		since "3.9"
		clz AccessControlListEntryInventory.class
	}
}
