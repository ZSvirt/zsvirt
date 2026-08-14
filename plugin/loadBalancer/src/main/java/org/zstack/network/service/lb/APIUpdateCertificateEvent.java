package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by shixin on 04/12/2018
 */
@RestResponse(allTo = "inventory")
public class APIUpdateCertificateEvent extends APIEvent {
    private CertificateInventory inventory;

    public APIUpdateCertificateEvent() {
    }

    public APIUpdateCertificateEvent(String apiId) {
        super(apiId);
    }

    public CertificateInventory getInventory() {
        return inventory;
    }

    public void setInventory(CertificateInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateCertificateEvent __example__() {
        APIUpdateCertificateEvent event = new APIUpdateCertificateEvent();
        CertificateInventory cer = new CertificateInventory();

        cer.setName("Test-Cer");
        cer.setDescription("Certificate for lb");
        cer.setCertificate("123456789");

        event.setInventory(cer);

        return event;
    }

}
