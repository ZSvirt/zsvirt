package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIAddExternalServiceConfigurationEvent

doc {
	title "新建外部服务配置"

	category "externalService"

	desc """新建外部服务配置"""

	rest {
		request {
			url "POST /v1/external/service/configurations"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddExternalServiceConfigurationMsg.class

			desc """"""

			params {

				column {
					name "externalServiceType"
					enclosedIn "params"
					desc "外部服务类型, 例如 Prometheus2"
					location "body"
					type "String"
					optional false
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "configuration"
					enclosedIn "params"
					desc "外部服务配置, 使用 json 格式。若配置中包含 remote_write.basic_auth.password 等凭据，系统会按原文保存；查询外部服务配置时 password 会脱敏为 ****** 返回。新增配置时不得提交脱敏值 ******，需提交真实密码"
					location "body"
					type "String"
					optional false
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIAddExternalServiceConfigurationEvent.class
		}
	}
}