package org.zstack.header.tpm.api

import org.zstack.header.tpm.api.APIAddTpmEvent

doc {
	title "AddTpm"

	category "tpm"

	desc """虚拟机添加 TPM"""

	rest {
		request {
			url "POST /v1/tpms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddTpmMsg.class

			desc """"""

			params {

				column {
					name "keyProviderUuid"
					enclosedIn "params"
					desc "密钥提供程序 UUID"
					location "body"
					type "String"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn "params"
					desc "虚拟机 UUID"
					location "body"
					type "String"
					optional false
					since "zsv 5.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源 UUID"
					location "body"
					type "String"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签 UUID 列表"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APIAddTpmEvent.class
		}
	}
}