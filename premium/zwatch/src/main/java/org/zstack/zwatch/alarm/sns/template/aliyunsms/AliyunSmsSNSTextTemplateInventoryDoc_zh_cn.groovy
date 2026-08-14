package org.zstack.zwatch.alarm.sns.template.aliyunsms

import java.lang.Boolean
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "SNS阿里云短信文本模板结构"

	field {
		name "alarmTemplateCode"
		desc "资源报警器模板Code"
		type "String"
		since "3.7.0"
	}
	field {
		name "sign"
		desc "短信签名名称"
		type "String"
		since "3.7.0"
	}
	field {
		name "eventTemplateCode"
		desc "事件报警器模板Code"
		type "String"
		since "3.7.0"
	}
	field {
		name "eventTemplate"
		desc "事件报警器模板文本"
		type "String"
		since "3.7.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.7.0"
	}
	field {
		name "name"
		desc "模板名称"
		type "String"
		since "3.7.0"
	}
	field {
		name "description"
		desc "模板的详细描述"
		type "String"
		since "3.7.0"
	}
	field {
		name "applicationPlatformType"
		desc "应用平台类型"
		type "String"
		since "3.7.0"
	}
	field {
		name "template"
		desc "资源报警器模板文本"
		type "String"
		since "3.7.0"
	}
	field {
		name "recoveryTemplate"
		desc "恢复模板文本"
		type "String"
		since "3.7.0"
	}
	field {
		name "defaultTemplate"
		desc "是否为默认模板"
		type "Boolean"
		since "3.7.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.7.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.7.0"
	}
}
