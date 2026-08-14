package org.zstack.sns.platform.email

import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "SNS邮件平台结构"

	field {
		name "smtpServer"
		desc "SMTP服务器地址"
		type "String"
		since "2.3"
	}
	field {
		name "smtpPort"
		desc "SMTP服务器端口"
		type "int"
		since "2.3"
	}
	field {
		name "username"
		desc "用户名"
		type "String"
		since "2.3"
	}
	field {
		name "password"
		desc "密码"
		type "String"
		since "2.3"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "2.3"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "2.3"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3"
	}
	field {
		name "type"
		desc "类型"
		type "String"
		since "2.3"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "2.3"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "2.3"
	}
}
