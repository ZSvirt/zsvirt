package org.zstack.sns.platform.microsoftteams

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.platform.microsoftteams.SNSMicrosoftTeamsEndpointInventory

doc {

	title "创建微软Teams接收端返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv_0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.microsoftteams.APICreateSNSMicrosoftTeamsEndpointEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv_3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.sns.platform.microsoftteams.APICreateSNSMicrosoftTeamsEndpointEvent.inventory"
		desc "null"
		type "SNSMicrosoftTeamsEndpointInventory"
		since "zsv_3.10.0"
		clz SNSMicrosoftTeamsEndpointInventory.class
	}
}
