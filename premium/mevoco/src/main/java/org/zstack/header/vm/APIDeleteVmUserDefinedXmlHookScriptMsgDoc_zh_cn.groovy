package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmUserDefinedXmlHookScriptEvent

doc {
	title "DeleteVmUserDefinedXmlHookScript"

	category "mevoco"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{vmInstanceUuid}/xml-hook-script"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmUserDefinedXmlHookScriptMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteVmUserDefinedXmlHookScriptEvent.class
		}
	}
}