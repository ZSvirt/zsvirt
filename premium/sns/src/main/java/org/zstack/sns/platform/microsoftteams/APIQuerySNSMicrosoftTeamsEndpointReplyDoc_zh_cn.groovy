package org.zstack.sns.platform.microsoftteams

import org.zstack.header.errorcode.ErrorCode
import org.zstack.sns.platform.microsoftteams.SNSMicrosoftTeamsEndpointInventory

doc {

	title "查询微软Teams接收端的返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.sns.platform.microsoftteams.APIQuerySNSMicrosoftTeamsEndpointReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.sns.platform.microsoftteams.APIQuerySNSMicrosoftTeamsEndpointReply.inventories"
		desc "null"
		type "List"
		since "3.10.0"
		clz SNSMicrosoftTeamsEndpointInventory.class
	}
}
