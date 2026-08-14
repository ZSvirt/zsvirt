package org.zstack.core.config;

import org.zstack.header.message.APIReply;

public class APIListGlobalConfigReply extends APIReply {
	private GlobalConfigInventory[] inventories;

	public GlobalConfigInventory[] getInventories() {
    	return inventories;
    }

	public void setInventories(GlobalConfigInventory[] inventories) {
    	this.inventories = inventories;
    }

    public static APIListGlobalConfigReply __example__() {
        APIListGlobalConfigReply reply = new APIListGlobalConfigReply();
        GlobalConfigInventory inventorie1= new GlobalConfigInventory();
        inventorie1.setCategory("example");
        inventorie1.setDefaultValue("defaultValue");
        inventorie1.setDescription("example");
        inventorie1.setName("name");
        inventorie1.setValue("value");
        GlobalConfigInventory [] inventories = new GlobalConfigInventory[1];
        inventories[0]= inventorie1;
        reply.setInventories(inventories);
        return reply;
    }

}
