package org.zstack.crypto.keyprovider.kms.api

import org.zstack.header.errorcode.ErrorCode

doc {

	title "从KMS获取服务端证书"

	field {
		name "serverCertPem"
		desc "服务端证书"
		type "String"
		since "zsv_5.0.0"
	}
	field {
		name "selfSigned"
		desc "服务端证书是否为自签证书"
		type "boolean"
		since "zsv_5.0.0"
	}
	field {
		name "serverCertInfo"
		desc "服务端证书解析信息"
		type "CertificateInfo"
		since "zsv_5.0.0"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_5.0.0"
	}
	ref {
		name "error"
		path "org.zstack.crypto.keyprovider.kms.api.APIGetKmsServerCertFromKmsEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "zsv_5.0.0"
		clz ErrorCode.class
	}
}
