package org.zstack.monitoring.media

doc {

	title "Email媒体清单"

	field {
		name "smtpServer"
		desc "SMTP服务器地址"
		type "String"
		since "2.1"
	}
	field {
		name "smtpPort"
		desc "SMTP服务器端口"
		type "Integer"
		since "2.1"
	}
	field {
		name "username"
		desc "SMTP用户名"
		type "String"
		since "2.1"
	}
	field {
		name "password"
		desc "SMTP密码"
		type "String"
		since "2.1"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.1"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.1"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.1"
	}
	field {
		name "type"
		desc "类型，为Email"
		type "String"
		since "2.1"
	}
	field {
		name "state"
		desc "状态，Enabled/Disable"
		type "String"
		since "2.1"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.1"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.1"
	}
}
