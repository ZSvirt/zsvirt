package org.zstack.sns.platform.dingtalk

import java.sql.Timestamp
import org.zstack.sns.SNSApplicationPlatformInventory

doc {

	title "钉钉终端结构"

	field {
		name "url"
		desc "钉钉机器人URL"
		type "String"
		since "2.3"
	}
	field {
		name "atAll"
		desc "是否@所有人"
		type "boolean"
		since "2.3"
	}
	field {
		name "secret"
		desc "钉钉秘钥"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "atPersonPhoneNumbers"
		desc "@用户的电话号码"
		type "List"
		since "2.3"
	}
	field {
		name "name"
		desc "资源名称"
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
		name "description"
		desc "资源的详细描述"
		type "String"
		since "2.3"
	}
	field {
		name "type"
		desc "终端类型"
		type "String"
		since "2.3"
	}
	field {
		name "state"
		desc "状态"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "platformUuid"
		desc "应用平台UUID"
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
	ref {
		name "platform"
		path "org.zstack.sns.platform.dingtalk.SNSDingTalkEndpointInventory.platform"
		desc "null"
		type "SNSApplicationPlatformInventory"
		since "zsv 4.2.0"
		clz SNSApplicationPlatformInventory.class
	}
}
