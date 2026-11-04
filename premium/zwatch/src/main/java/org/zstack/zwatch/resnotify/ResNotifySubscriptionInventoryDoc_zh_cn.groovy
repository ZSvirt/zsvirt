package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.ResNotifyType
import org.zstack.zwatch.resnotify.ResNotifySubscriptionState
import java.sql.Timestamp
import org.zstack.zwatch.resnotify.ResNotifyWebhookRefInventory

doc {

	title "资源变更通知订阅清单"

	field {
		name "uuid"
		desc "资源变更通知订阅的UUID，唯一标识该订阅"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "name"
		desc "资源变更通知订阅名称"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "description"
		desc "资源变更通知订阅的详细描述"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "resourceTypes"
		desc "订阅匹配的资源类型列表，以逗号分隔"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "eventTypes"
		desc "订阅匹配的事件类型列表，以逗号分隔"
		type "String"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "type"
		path "org.zstack.zwatch.resnotify.ResNotifySubscriptionInventory.type"
		desc "资源变更通知订阅类型"
		type "ResNotifyType"
		since "1.0.0, zsv_5.1.0"
		clz ResNotifyType.class
	}
	ref {
		name "state"
		path "org.zstack.zwatch.resnotify.ResNotifySubscriptionInventory.state"
		desc "资源变更通知订阅状态"
		type "ResNotifySubscriptionState"
		since "1.0.0, zsv_5.1.0"
		clz ResNotifySubscriptionState.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "1.0.0, zsv_5.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "1.0.0, zsv_5.1.0"
	}
	ref {
		name "webhookRef"
		path "org.zstack.zwatch.resnotify.ResNotifySubscriptionInventory.webhookRef"
		desc "webhook 通知配置引用"
		type "ResNotifyWebhookRefInventory"
		since "1.0.0, zsv_5.1.0"
		clz ResNotifyWebhookRefInventory.class
	}
}
