package org.zstack.header.tpm.api

import org.zstack.header.tpm.api.APIUpdateTpmEvent

doc {
	title "UpdateTpm"

	category "tpm"

	desc """更新 TPM"""

	rest {
		request {
			url "PUT /v1/tpms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateTpmMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "updateTpm"
					desc "虚拟机 UUID"
					location "body"
					type "String"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "tpmUuid"
					enclosedIn "updateTpm"
					desc "TPM UUID"
					location "body"
					type "String"
					optional true
					since "zsv 5.0.0"
				}
				column {
					name "keyProviderUuid"
					enclosedIn "updateTpm"
					desc "密钥提供程序 UUID"
					location "body"
					type "String"
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
			clz APIUpdateTpmEvent.class
		}
	}
}