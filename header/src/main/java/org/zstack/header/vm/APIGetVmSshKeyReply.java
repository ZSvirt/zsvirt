package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;


/**
 * Created by luchukun on 8/4/16.
 */
@RestResponse(fieldsTo = {"sshKey"})
public class APIGetVmSshKeyReply extends APIReply {
    private String sshKey;

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public String getSshKey() {
        return sshKey;
    }
 
    public static APIGetVmSshKeyReply __example__() {
        APIGetVmSshKeyReply reply = new APIGetVmSshKeyReply();
        reply.sshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtgdrSt2i/4ayXoiR6qNd7dykOOcz205NVSUgw41GOZW3PdXa9WWMF1APHtj8L2tYm3lKgIDSy1aQtpIEenu6L03BeqfPwepf2L89aZ/W4UPRpce9/bO4mSSJ6kvbZ7hFv+4KLUJCB9O7UrcFu7J/QwrkHNVNX1NsVUpqebp3Ny8bxj0Wbr9ecqPKTclzygARRGz71iDQaEhLrQqy/Q8vr+/G1/uyAYfTnifCuuMTfh5DEsuKD1AEHMBVZEbJ4zupR4gywXnGRxHmEwE464scACxeFWVx2flIXkTK8f3W0KBLCJ8VtTd8KxvKQBu2jJ70avmXNOzb5IaBDS root@172-20-12-46";
        return reply;
    }

}
