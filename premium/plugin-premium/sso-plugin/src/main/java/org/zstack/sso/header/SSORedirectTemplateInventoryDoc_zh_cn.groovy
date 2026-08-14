package org.zstack.sso.header

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "认证成功之后跳转模版"

	field {
		name "uuid"
		desc "模版的 UUID，唯一标示该资源"
		type "String"
		since "4.3.0"
	}
	field {
		name "name"
		desc "模版名称"
		type "String"
		since "4.3.0"
	}
	field {
		name "description"
		desc "模版的详细描述"
		type "String"
		since "4.3.0"
	}
	field {
		name "clientUuid"
		desc "对应的 SSO 客户端 UUID，即第三方账户源的 UUID"
		type "String"
		since "4.3.0"
	}
	field {
		name "redirectTemplate"
		desc "跳转的模板"
		type "String"
		since "4.3.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.3.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.3.0"
	}
}
