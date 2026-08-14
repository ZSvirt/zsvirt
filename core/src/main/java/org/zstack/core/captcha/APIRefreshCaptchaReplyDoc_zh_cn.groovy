package org.zstack.core.captcha

import org.zstack.header.errorcode.ErrorCode

doc {

	title "刷新验证码后的结果"

	ref {
		name "error"
		path "APIRefreshCaptchaReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	field {
		name "captchaUuid"
		desc "验证码的唯一标识符"
		type "String"
		since "0.6"
	}
	field {
		name "captcha"
		desc "验证码图片的base64"
		type "String"
		since "0.6"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
}
