package org.zstack.header.core.external.service

import java.sql.Timestamp

doc {

	title "外部服务配置"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "serviceType"
		desc "外部服务类型, 如 Prometheus2, FluentBitServer"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "configuration"
		desc "外部服务配置, 使用 json 格式。若已保存配置包含 remote_write.basic_auth.password 等凭据，查询结果中的 password 会脱敏为 ****** 返回；如需通过 Update 回写该脱敏值，系统会保留对应 remote_write 已保存的旧密码"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "1.0.0, zsv_5.1.0"
	}
}
