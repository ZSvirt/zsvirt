package org.zstack.header.tpm.api

import org.zstack.header.tpm.api.APIGetTpmCapabilityReply

doc {
	title "GetTpmCapability"

	category "tpm"

	desc """获取 TPM 详情数据"""

	rest {
		request {
			url "GET /v1/tpms/capability"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetTpmCapabilityMsg.class

			desc """"""

			params {

				column {
					name "tpmUuid"
					enclosedIn ""
					desc "TPM UUID"
					location "query"
					type "String"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "虚拟机 UUID"
					location "query"
					type "String"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 5.0.0"
				}
			}
		}

		response {
			clz APIGetTpmCapabilityReply.class
		}
	}
}