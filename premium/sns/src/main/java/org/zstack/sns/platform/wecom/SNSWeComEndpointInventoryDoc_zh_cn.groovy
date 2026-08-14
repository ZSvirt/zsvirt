package org.zstack.sns.platform.wecom

import java.sql.Timestamp
import org.zstack.sns.SNSApplicationPlatformInventory

doc {

	title "SNSWeComEndpointInventory"

	field {
		name "url"
		desc "企业微信机器人Webhook URL"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "atAll"
		desc "是否@所有人"
		type "boolean"
		since "zsv 4.2.0"
	}
	field {
		name "atPersonUserIds"
		desc "@用户ID"
		type "List"
		since "zsv 4.2.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "type"
		desc "终端类型"
		type "String"
		since "zsv 4.2.0"
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
		since "zsv 4.2.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv 4.2.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv 4.2.0"
	}
	ref {
		name "platform"
		path "org.zstack.sns.platform.wecom.SNSWeComEndpointInventory.platform"
		desc "null"
		type "SNSApplicationPlatformInventory"
		since "zsv 4.2.0"
		clz SNSApplicationPlatformInventory.class
	}
}
