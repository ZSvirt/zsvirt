package org.zstack.billing;

import org.zstack.billing.spendingcalculator.volume.data.DataVolumeSpending;
import org.zstack.billing.spendingcalculator.volume.data.DataVolumeSpendingInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import java.util.Date;
import java.util.List;
import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 3/4/2016.
 */
@RestResponse(fieldsTo = { "all" })
public class APICalculateAccountSpendingReply extends APIReply {

    private double total;

    private List<Spending> spending;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<Spending> getSpending() {
        return spending;
    }

    public void setSpending(List<Spending> spending) {
        this.spending = spending;
    }

    public static APICalculateAccountSpendingReply __example__() {
        APICalculateAccountSpendingReply reply = new APICalculateAccountSpendingReply();
        reply.setTotal(200d);
        DataVolumeSpending dataVolumeSpending = new DataVolumeSpending();
        dataVolumeSpending.resourceName = "data-volume-for-vm-xxxxx";
        dataVolumeSpending.resourceUuid = uuid();
        dataVolumeSpending.spending = 200d;
        DataVolumeSpendingInventory inv = new DataVolumeSpendingInventory();
        inv.startTime = new Date(0).getTime();
        inv.endTime = DocUtils.date;
        inv.spending = 200d;
        inv.volumeSize = 200;
        dataVolumeSpending.sizeInventory = list(inv);
        Spending spending = new Spending();
        spending.addDetails(dataVolumeSpending);
        reply.setSpending(list(spending));
        return reply;
    }
}
