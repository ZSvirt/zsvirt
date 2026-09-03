package org.zstack.sso.header

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "CAS 客户端"

	field {
		name "uuid"
		desc "CAS 客户端的 UUID，唯一标示该资源"
		type "String"
		since "4.10.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "4.10.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "4.10.6"
	}
	field {
		name "type"
		desc "类型, CAS"
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
		since "4.10.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.10.6"
	}
	field {
		name "loginMNUrl"
		desc "免密登录的 URL"
		type "String"
		since "4.10.6"
	}
	field {
		name "redirectUrl"
		desc "用户自定义回调 URL"
		type "String"
		since "4.10.6"
	}
	field {
		name "casServerLoginUrl"
		desc "CAS 服务的登录 URL"
		type "String"
		since "4.10.6"
	}
	field {
		name "casServerUrlPrefix"
		desc "CAS 服务的 URL 前缀"
		type "String"
		since "4.10.6"
	}
	field {
		name "serverName"
		desc "MN 的地址，示例：http://127.0.0.1:8080/sso"
		type "String"
		since "4.10.6"
	}
	ref {
		name "state"
		desc "客户端状态"
		type "CasState"
		since "4.10.6"
	}
	field {
		name "usernameProperty"
		desc "用户登录该虚拟化平台时使用哪个字段用作用户名"
		type "String"
		since "4.10.6"
	}
}
