package org.zstack.billing.generator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.billing.BillingConstants;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

/**
 * Created by lining on 2019/5/10.
 */
@LongJobFor(GenerateAccountBillingMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BillingGeneratorJob implements LongJob {
    private static final CLogger logger = Utils.getLogger(BillingGeneratorJob.class);

    @Autowired
    protected CloudBus bus;

    @Autowired
    protected DatabaseFacade dbf;

    @Autowired
    private PluginRegistry pluginRgty;

    @Autowired
    private ResourceDestinationMaker destinationMaker;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        List<AccountVO> accountVOS = Q.New(AccountVO.class).list();

        List<GenerateAccountBillingMsg> msgs = new ArrayList<>();

        accountVOS.forEach(accountVO -> {
            GenerateAccountBillingMsg generateAccountBillingMsg = new GenerateAccountBillingMsg();
            generateAccountBillingMsg.setAccountUuid(accountVO.getUuid());
            bus.makeTargetServiceIdByResourceUuid(generateAccountBillingMsg, BillingConstants.SERVICE_ID, accountVO.getUuid());
            msgs.add(generateAccountBillingMsg);
        });

        if (msgs.isEmpty()) {
            completion.success(null);
        }

        new While<>(msgs).step((generateAccountBillingMsg, whileCompletion) -> {
            bus.send(generateAccountBillingMsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.error(String.format("generate account[%s] fail, %s", generateAccountBillingMsg.getAccountUuid(), reply.getError().getDetails()));
                    }

                    whileCompletion.done();
                }
            });
        }, 1).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success(null);
            }
        });
    }
}
