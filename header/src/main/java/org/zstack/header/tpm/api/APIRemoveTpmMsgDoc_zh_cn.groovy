package org.zstack.header.tpm.api

import org.zstack.header.tpm.api.APIRemoveTpmEvent

doc {
	title "RemoveTpm"

	category "tpm"

	desc """虚拟机删除 TPM"""

	rest {
		request {
			url "DELETE /v1/tpms"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveTpmMsg.class

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
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
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
			clz APIRemoveTpmEvent.class
		}
	}
}