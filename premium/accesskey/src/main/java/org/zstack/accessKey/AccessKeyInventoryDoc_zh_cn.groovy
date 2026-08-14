package org.zstack.accessKey

import org.zstack.accessKey.AccessKeyState
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "AccessKey"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "4.0.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.0.0"
	}
	field {
		name "accountUuid"
		desc "账户UUID"
		type "String"
		since "4.0.0"
	}
	field {
		name "userUuid"
		desc "该值已弃用"
		type "String"
		since "4.0.0"
	}
	field {
		name "AccessKeyID"
		desc ""
		type "String"
		since "4.0.0"
	}
	field {
		name "AccessKeySecret"
		desc ""
		type "String"
		since "4.0.0"
	}
	ref {
		name "state"
		path "org.zstack.accessKey.AccessKeyInventory.state"
		desc "null"
		type "AccessKeyState"
		since "4.0.0"
		clz AccessKeyState.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.0.0"
	}
}
