package org.zstack.crypto.keyprovider.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "重加密密钥结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.api.APIRekeyKeyProviderRefsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "5.0.0"
		clz ErrorCode.class
	}
	field {
		name "totalCount"
		desc "本次涉及的源密钥提供者数量（按 resource ref 关联的 provider 去重）"
		type "int"
		since "5.0.0"
	}
	field {
		name "successCount"
		desc "未出现失败 ref 的 provider 数量（含仅有 skip 的 provider）"
		type "int"
		since "5.0.0"
	}
	field {
		name "skippedCount"
		desc "存在被跳过 ref 的 provider 数量"
		type "int"
		since "5.0.0"
	}
	field {
		name "failedCount"
		desc "存在失败 ref 的 provider 数量"
		type "int"
		since "5.0.0"
	}
	field {
		name "providerResults"
		desc "按源密钥提供者聚合的结果；每个元素内含该 provider 下的 skippedResources、failedResources 及各类 ref 计数"
		type "List"
		since "5.0.0"
	}
}
