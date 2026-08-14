package org.zstack.core.errorcode



doc {

	title "错误码内容"

	field {
		name "category"
		desc "错误码所属目录"
		type "String"
		since "3.3.0"
	}
	field {
		name "code"
		desc "错误码代号"
		type "String"
		since "3.3.0"
	}
	field {
		name "regex"
		desc "错误码匹配关键字"
		type "String"
		since "3.3.0"
	}
	field {
		name "message_cn"
		desc "错误码内容中文"
		type "String"
		since "3.3.0"
	}
	field {
		name "message_en"
		desc "错误码内容英文"
		type "String"
		since "3.3.0"
	}
	field {
		name "source"
		desc "错误来源"
		type "String"
		since "3.3.0"
	}
	field {
		name "method"
		desc "匹配方法，distance(字符串比较)或regex(正则)"
		type "String"
		since "3.6.0"
	}
	field {
		name "distance"
		desc "若使用distance匹配，此处为精确度(1为最精确)"
		type "Double"
		since "3.6.0"
	}
}
