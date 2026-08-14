package org.zstack.cloudformation.template.decoder;

import com.google.gson.JsonElement;
import org.zstack.cloudformation.template.struct.CfnResults;
import org.zstack.cloudformation.template.struct.PreParameterStruct;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.Set;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2019/5/31.
 */
public class PreParameterDecoder extends ParameterDecoder {
    private static final CLogger logger = Utils.getLogger(PreParameterDecoder.class);

    private static String NAME = "Pre-Parameters";

    protected PreParameterStruct getParamFromResult(CfnResults result, String paraName) {
        for (PreParameterStruct s: result.getPreparams()) {
            if (s.getParamName().equals(paraName)) {
                return s;
            }
        }
        return null;
    }

    @Override
    public void decode(JsonElement element, CfnResults result) {
        if (element.isJsonObject()) {
            Set<Map.Entry<String, JsonElement>> es = element.getAsJsonObject().entrySet();
            es.forEach(e -> {
                PreParameterStruct p = null;
                for (PreParameterStruct t: result.getPreparams()) {
                    if (t.getParamName().equals(e.getKey())) {
                        p = t;
                    }
                }
                if (p == null) {
                    p = new PreParameterStruct();
                    p.setParamName(e.getKey());
                    result.getPreparams().add(p);
                }

                decodeType(e.getValue(), result, e.getKey());
                decodeParams(e.getValue(), result, e.getKey());
            });
        } else {
            throw new OperationFailureException(operr("Parameters root body must be json object!"));
        }
    }
}
