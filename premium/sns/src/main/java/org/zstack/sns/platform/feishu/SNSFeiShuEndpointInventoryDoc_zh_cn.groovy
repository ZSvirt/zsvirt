package org.zstack.sns.platform.feishu

import java.sql.Timestamp
import org.zstack.sns.SNSApplicationPlatformInventory

doc {

	title "SNSFeiShuEndpointInventory"

	field {
		name "url"
		desc "飞书机器人URL"
		type "String"
		since "zsv 4.2.0"
	}
	field {
		name "atAll"
		desc "@所有人"
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
		name "secret"
		desc "飞书秘钥"
		type "String"
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
		desc "类型"
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
		desc "平台UUID"
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
		path "org.zstack.sns.platform.feishu.SNSFeiShuEndpointInventory.platform"
		desc "null"
		type "SNSApplicationPlatformInventory"
		since "zsv 4.2.0s"
		clz SNSApplicationPlatformInventory.class
	}
}
