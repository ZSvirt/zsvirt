package org.zstack.zwatch.datatype;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public interface QueryObject {
    CLogger logger = Utils.getLogger(QueryObject.class);

    /**
     * examples:
     * input  : 2018-11-14T16:55:18.1Z
     * output : 2018-11-14T16:55:18.100Z
     *
     * input  : 2018-11-14T16:55:18.42Z
     * output : 2018-11-14T16:55:18.420Z
     *
     * input  : 2018-11-14T16:55:18.042Z
     * output : 2018-11-14T16:55:18.042Z
     */
    default String millisecondBitCompletion(String time) {
        if (!time.endsWith("Z") || !time.contains(".")) {
            return time;
        }

        int millIndex = time.lastIndexOf(".") + 1;
        if (time.length() - millIndex > 3) {
            return time;
        }

        String mills = time.substring(millIndex, time.length() - 1);
        int millsLength = mills.length();
        if (millsLength == 1) {
            return String.format("%s%s00Z", time.substring(0, millIndex), mills);
        } else if (millsLength == 2) {
            return String.format("%s%s0Z", time.substring(0, millIndex), mills);
        }

        return time;
    }
}
