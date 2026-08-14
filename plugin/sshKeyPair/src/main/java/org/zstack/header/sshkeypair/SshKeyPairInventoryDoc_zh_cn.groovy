package org.zstack.header.sshkeypair

import java.sql.Timestamp

doc {

	title "密钥对"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.17.21"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.17.21"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.17.21"
	}
	field {
		name "publicKey"
		desc ""
		type "String"
		since "3.17.21"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.17.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.17.21"
	}
}
