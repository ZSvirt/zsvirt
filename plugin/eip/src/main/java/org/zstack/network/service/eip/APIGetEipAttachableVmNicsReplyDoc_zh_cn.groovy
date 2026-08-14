package org.zstack.network.service.eip

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.vm.VmNicInventory
import java.lang.Integer
import java.lang.Boolean
import org.zstack.header.errorcode.ErrorCode

doc {

	title "弹性IP清单"

	ref {
		name "error"
		path "org.zstack.network.service.eip.APIGetEipAttachableVmNicsReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.network.service.eip.APIGetEipAttachableVmNicsReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VmNicInventory.class
	}
	field {
		name "start"
		desc ""
		type "Integer"
		since "3.8"
	}
	field {
		name "more"
		desc ""
		type "Boolean"
		since "3.8"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "3.8"
	}
}
