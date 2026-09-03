package org.zstack.sso.header

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "OAuth2 客户端"

	field {
		name "uuid"
		desc "OAuth2 客户端的 UUID，唯一标示该资源"
		type "String"
		since "4.3.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.3.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.3.0"
	}
	field {
		name "type"
		desc "类型, OAuth2"
		type "String"
		since "4.10.6"
	}
	field {
		name "createAccountStrategy"
		desc "创建账号的策略"
		type "String"
		since "4.10.6"
	}
	field {
		name "deleteAccountStrategy"
		desc "删除账号的策略"
		type "String"
		since "4.10.6"
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
		name "clientId"
		desc "客户端 ID"
		type "String"
		since "4.3.0"
	}
	field {
		name "clientSecret"
		desc "客户端密钥"
		type "String"
		since "4.3.0"
	}
	field {
		name "grantType"
		desc "认证模式"
		type "String"
		since "4.3.0"
	}
	field {
		name "loginMNUrl"
		desc "免密登录的 URL"
		type "String"
		since "4.10.6"
	}
	field {
		name "redirectUrl"
		desc "用户自定义回调的 URL"
		type "String"
		since "4.10.6"
	}
	field {
		name "authorizationUrl"
		desc "认证 URL"
		type "String"
		since "4.3.0"
	}
	field {
		name "tokenUrl"
		desc "认证 Token URL"
		type "String"
		since "4.3.0"
	}
	field {
		name "userinfoUrl"
		desc "用户信息 URL"
		type "String"
		since "4.3.0"
	}
	field {
		name "logoutUrl"
		desc "用户登出 URL"
		type "String"
		since "4.3.0"
	}
	field {
		name "usernameProperty"
		desc "用户登录该虚拟化平台时使用哪个字段用作用户名"
		type "String"
		since "4.10.6"
	}
}
