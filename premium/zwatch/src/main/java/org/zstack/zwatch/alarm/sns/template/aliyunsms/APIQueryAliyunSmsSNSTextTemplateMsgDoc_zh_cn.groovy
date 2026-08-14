package org.zstack.zwatch.alarm.sns.template.aliyunsms

import org.zstack.zwatch.alarm.sns.template.aliyunsms.APIQueryAliyunSmsSNSTextTemplateReply
import org.zstack.header.query.APIQueryMessage

doc {
	title "QueryAliyunSmsSNSTextTemplate"

	category "alarm.sns"

	desc """查询SNS监控阿里云短信模板"""

	rest {
		request {
			url "GET /v1/zwatch/alarms/sns/text-templates/aliyun-sms"
			url "GET /v1/zwatch/alarms/sns/text-templates/aliyun-sms/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQueryAliyunSmsSNSTextTemplateMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQueryAliyunSmsSNSTextTemplateReply.class
		}
	}
}