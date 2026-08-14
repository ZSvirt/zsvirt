package org.zstack.network.l2.virtualSwitch.header

import org.zstack.network.l2.virtualSwitch.header.UplinkGroupType
import java.sql.Timestamp

doc {

	title "上行链路组"

	field {
		name "interfaceName"
		desc "接口名称"
		type "String"
		since "4.3.0"
	}
	ref {
		name "type"
		path "org.zstack.network.l2.virtualSwitch.header.UplinkGroupInventory.type"
		desc "null"
		type "UplinkGroupType"
		since "4.3.0"
		clz UplinkGroupType.class
	}
	field {
		name "bondingUuid"
		desc "绑定UUID"
		type "String"
		since "4.3.0"
	}
	field {
		name "interfaceUuid"
		desc "接口UUID"
		type "String"
		since "4.3.0"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "4.3.0"
	}
	field {
		name "l2NetworkUuid"
		desc "二层网络UUID"
		type "String"
		since "4.3.0"
	}
	field {
		name "l2ProviderType"
		desc "二层网络实现类型"
		type "String"
		since "4.3.0"
	}
	field {
		name "bridgeName"
		desc "网桥名称"
		type "String"
		since "4.3.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.3.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.3.0"
	}
}
