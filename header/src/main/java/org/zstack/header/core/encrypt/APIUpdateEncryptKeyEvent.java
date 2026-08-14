package org.zstack.header.core.encrypt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/12/28.
 */
@RestResponse
public class APIUpdateEncryptKeyEvent extends APIEvent {
	// only for test
	private EncryptKeyInventory inventory;

	public APIUpdateEncryptKeyEvent() {
	super(null);
}

	public APIUpdateEncryptKeyEvent(String apiId) {
		super(apiId);
	}

	public EncryptKeyInventory getInventory() {
		return inventory;
	}

	public void setInventory(EncryptKeyInventory inventory) {
		this.inventory = inventory;
	}

	public static APIUpdateEncryptKeyEvent __example__() {
        APIUpdateEncryptKeyEvent event = new APIUpdateEncryptKeyEvent();
        return event;
    }

}
