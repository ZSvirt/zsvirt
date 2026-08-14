package org.zstack.header.vm

import org.zstack.header.vm.APIDeleteVmUserDefinedXmlEvent

doc {
	title "DeleteVmUserDefinedXml"

	category "mevoco"

	desc """删除虚拟机自定义XML"""

	rest {
		request {
			url "DELETE /v1/vm-instances/{vmInstanceUuid}/xml"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteVmUserDefinedXmlMsg.class

			desc """"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "3.10"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
			}
		}

		response {
			clz APIDeleteVmUserDefinedXmlEvent.class
		}
	}
}