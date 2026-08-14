package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.SshPrivateKeyPairInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "生成密钥对返回"

	ref {
		name "inventory"
		path "org.zstack.header.sshkeypair.APIGenerateSshKeyPairReply.inventory"
		desc "SSH 密钥对清单"
		type "SshPrivateKeyPairInventory"
		since "3.17.21"
		clz SshPrivateKeyPairInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.21"
	}
	ref {
		name "error"
		path "org.zstack.header.sshkeypair.APIGenerateSshKeyPairReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.21"
		clz ErrorCode.class
	}
}
