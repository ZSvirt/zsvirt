package org.zstack.sso.header

import java.sql.Timestamp

doc {

	title "OAuth2 Token"

	field {
		name "uuid"
		desc "OAuth2 Token 的 UUID，唯一标示该资源；注意它不是 ResourceVO"
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
		name "userUuid"
		desc "对应的 ZSphere 中账号的 UUID，也称为 accountUuid"
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
	field {
		name "accessToken"
		desc "访问令牌"
		type "String"
		since "4.3.0"
	}
	field {
		name "idToken"
		desc "ID 令牌"
		type "String"
		since "4.3.0"
	}
	field {
		name "refreshToken"
		desc "刷新使用令牌"
		type "String"
		since "4.3.0"
	}
}
