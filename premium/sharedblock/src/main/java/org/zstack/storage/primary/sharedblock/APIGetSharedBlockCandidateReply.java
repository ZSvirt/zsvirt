package org.zstack.storage.primary.sharedblock;

import org.zstack.header.image.*;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/9/21.
 */
@RestResponse(allTo = "results")
public class APIGetSharedBlockCandidateReply extends APIReply {
    private List<SharedBlockCandidateStruct> results;

    public List<SharedBlockCandidateStruct> getResults() {
        return results;
    }

    public void setResults(List<SharedBlockCandidateStruct> results) {
        this.results = results;
    }

    public static APIGetSharedBlockCandidateReply __example__() {
        APIGetSharedBlockCandidateReply reply = new APIGetSharedBlockCandidateReply();
        SharedBlockCandidateStruct struct1 = new SharedBlockCandidateStruct();
        struct1.wwn = "0x5002538d40f50330";
        struct1.hctl = "3:0:0:0";
        struct1.model = "Samsung SSD 850";
        struct1.vendor = "ATA";
        struct1.type = "disk";
        struct1.serial = "S33CNX0H600299D";
        struct1.size = 250059350016l;
        struct1.wwid = "ata-Samsung_SSD_850_EVO_M.2_250GB_S33CNX0H600299D";

        SharedBlockCandidateStruct struct2 = new SharedBlockCandidateStruct();
        struct1.wwn = "0x6b083fe000daf018";
        struct1.hctl = "";
        struct1.model = "MD32xx";
        struct1.vendor = "DELL";
        struct1.type = "mpath";
        struct1.serial = "6b083fe000daf018000015505abbe00a";
        struct1.size = 30003188203520l;

        reply.setResults(Arrays.asList(struct1, struct2));
        return reply;
    }

}
