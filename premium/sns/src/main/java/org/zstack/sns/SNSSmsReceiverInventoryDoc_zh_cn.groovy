package org.zstack.sns

import org.zstack.sns.SmsReceiverType

doc {

	title "短信接收者结构"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.7.0"
	}
	field {
		name "phoneNumber"
		desc "短信接收号码"
		type "String"
		since "3.7.0"
	}
	field {
		name "endpointUuid"
		desc "短信接收端Uuid"
		type "String"
		since "3.7.0"
	}
	ref {
		name "type"
		path "org.zstack.sns.SNSSmsReceiverInventory.type"
		desc "短信接收端类型"
		type "SmsReceiverType"
		since "3.7.0"
		clz SmsReceiverType.class
	}
	field {
		name "description"
		desc "短信接收者描述"
		type "String"
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
