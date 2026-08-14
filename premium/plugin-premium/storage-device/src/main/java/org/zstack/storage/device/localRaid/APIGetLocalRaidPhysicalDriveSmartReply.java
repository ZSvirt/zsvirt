package org.zstack.storage.device.localRaid;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/9/21.
 */
@RestResponse(allTo = "result")
public class APIGetLocalRaidPhysicalDriveSmartReply extends APIReply {
    private List<SmartDataStruct> result;

    public List<SmartDataStruct> getResult() {
        return result;
    }

    public void setResult(List<SmartDataStruct> result) {
        this.result = result;
    }

    public static APIGetLocalRaidPhysicalDriveSmartReply __example__() {
        APIGetLocalRaidPhysicalDriveSmartReply reply = new APIGetLocalRaidPhysicalDriveSmartReply();

        SmartDataStruct s0 = new SmartDataStruct();
        s0.setId(1);
        s0.setAttributeName("Raw_Read_Error_Rate");
        s0.setFlag("0x002f");
        s0.setValue(200);
        s0.setWorst(200);
        s0.setThresh(051);
        s0.setType("Pre-fail");
        s0.setUpdated("Always");
        s0.setWhenFailed("-");
        s0.setRawValue(0L);

        SmartDataStruct s1 = new SmartDataStruct();
        s1.setId(3);
        s1.setAttributeName("Spin_Up_Time");
        s1.setFlag("0x0027");
        s1.setValue(142);
        s1.setWorst(142);
        s1.setThresh(021);
        s1.setType("Pre-fail");
        s1.setUpdated("Always");
        s1.setWhenFailed("-");
        s1.setRawValue(3866L);

        SmartDataStruct s2 = new SmartDataStruct();
        s2.setId(4);
        s2.setAttributeName("Start_Stop_Count");
        s2.setFlag("0x0032");
        s2.setValue(100);
        s2.setWorst(100);
        s2.setThresh(0);
        s2.setType("Old_age");
        s2.setUpdated("Always");
        s2.setWhenFailed("-");
        s2.setRawValue(22L);

        SmartDataStruct s3 = new SmartDataStruct();
        s3.setId(5);
        s3.setAttributeName("Reallocated_Sector_Ct");
        s3.setFlag("0x0033");
        s3.setValue(200);
        s3.setWorst(200);
        s3.setThresh(140);
        s3.setType("Pre-fail");
        s3.setUpdated("Always");
        s3.setWhenFailed("-");
        s3.setRawValue(0L);

        SmartDataStruct s4 = new SmartDataStruct();
        s4.setId(7);
        s4.setAttributeName("Seek_Error_Rate");
        s4.setFlag("0x002e");
        s4.setValue(200);
        s4.setWorst(200);
        s4.setThresh(0);
        s4.setType("Old_age");
        s4.setUpdated("Always");
        s4.setWhenFailed("-");
        s4.setRawValue(0L);

        SmartDataStruct s5 = new SmartDataStruct();
        s5.setId(9);
        s5.setAttributeName("Power_On_Hours");
        s5.setFlag("0x0032");
        s5.setValue(97);
        s5.setWorst(97);
        s5.setThresh(0);
        s5.setType("Old_age");
        s5.setUpdated("Always");
        s5.setWhenFailed("-");
        s5.setRawValue(2585L);

        SmartDataStruct s6 = new SmartDataStruct();
        s6.setId(10);
        s6.setAttributeName("Spin_Retry_Count");
        s6.setFlag("0x0032");
        s6.setValue(100);
        s6.setWorst(253);
        s6.setThresh(0);
        s6.setType("Old_age");
        s6.setUpdated("Always");
        s6.setWhenFailed("-");
        s6.setRawValue(0L);

        SmartDataStruct s7 = new SmartDataStruct();
        s7.setId(11);
        s7.setAttributeName("Calibration_Retry_Count");
        s7.setFlag("0x0032");
        s7.setValue(100);
        s7.setWorst(253);
        s7.setThresh(0);
        s7.setType("Old_age");
        s7.setUpdated("Always");
        s7.setWhenFailed("-");
        s7.setRawValue(0L);

        SmartDataStruct s8 = new SmartDataStruct();
        s8.setId(12);
        s8.setAttributeName("Power_Cycle_Count");
        s8.setFlag("0x0032");
        s8.setValue(100);
        s8.setWorst(100);
        s8.setThresh(0);
        s8.setType("Old_age");
        s8.setUpdated("Always");
        s8.setWhenFailed("-");
        s8.setRawValue(22L);

        SmartDataStruct s9 = new SmartDataStruct();
        s9.setId(16);
        s9.setAttributeName("Unknown_Attribute");
        s9.setFlag("0x0022");
        s9.setValue(0);
        s9.setWorst(200);
        s9.setThresh(0);
        s9.setType("Old_age");
        s9.setUpdated("Always");
        s9.setWhenFailed("-");
        s9.setRawValue(34516442916L);

        SmartDataStruct s10 = new SmartDataStruct();
        s10.setId(183);
        s10.setAttributeName("Runtime_Bad_Block");
        s10.setFlag("0x0032");
        s10.setValue(100);
        s10.setWorst(100);
        s10.setThresh(0);
        s10.setType("Old_age");
        s10.setUpdated("Always");
        s10.setWhenFailed("-");
        s10.setRawValue(0L);

        SmartDataStruct s11 = new SmartDataStruct();
        s11.setId(192);
        s11.setAttributeName("Power-Off_Retract_Count");
        s11.setFlag("0x0032");
        s11.setValue(200);
        s11.setWorst(200);
        s11.setThresh(0);
        s11.setType("Old_age");
        s11.setUpdated("Always");
        s11.setWhenFailed("-");
        s11.setRawValue(20L);

        SmartDataStruct s12 = new SmartDataStruct();
        s12.setId(193);
        s12.setAttributeName("Load_Cycle_Count");
        s12.setFlag("0x0032");
        s12.setValue(200);
        s12.setWorst(200);
        s12.setThresh(0);
        s12.setType("Old_age");
        s12.setUpdated("Always");
        s12.setWhenFailed("-");
        s12.setRawValue(4L);

        SmartDataStruct s13 = new SmartDataStruct();
        s13.setId(194);
        s13.setAttributeName("Temperature_Celsius");
        s13.setFlag("0x0022");
        s13.setValue(112);
        s13.setWorst(106);
        s13.setThresh(0);
        s13.setType("Old_age");
        s13.setUpdated("Always");
        s13.setWhenFailed("-");
        s13.setRawValue(31L);

        SmartDataStruct s14 = new SmartDataStruct();
        s14.setId(196);
        s14.setAttributeName("Reallocated_Event_Count");
        s14.setFlag("0x0032");
        s14.setValue(200);
        s14.setWorst(200);
        s14.setThresh(0);
        s14.setType("Old_age");
        s14.setUpdated("Always");
        s14.setWhenFailed("-");
        s14.setRawValue(0L);

        SmartDataStruct s15 = new SmartDataStruct();
        s15.setId(197);
        s15.setAttributeName("Current_Pending_Sector");
        s15.setFlag("0x0032");
        s15.setValue(200);
        s15.setWorst(200);
        s15.setThresh(0);
        s15.setType("Old_age");
        s15.setUpdated("Always");
        s15.setWhenFailed("-");
        s15.setRawValue(0L);

        SmartDataStruct s16 = new SmartDataStruct();
        s16.setId(198);
        s16.setAttributeName("Offline_Uncorrectable");
        s16.setFlag("0x0030");
        s16.setValue(200);
        s16.setWorst(200);
        s16.setThresh(0);
        s16.setType("Old_age");
        s16.setUpdated("Offline");
        s16.setWhenFailed("-");
        s16.setRawValue(0L);

        SmartDataStruct s17 = new SmartDataStruct();
        s17.setId(199);
        s17.setAttributeName("UDMA_CRC_Error_Count");
        s17.setFlag("0x0032");
        s17.setValue(200);
        s17.setWorst(200);
        s17.setThresh(0);
        s17.setType("Old_age");
        s17.setUpdated("Always");
        s17.setWhenFailed("-");
        s17.setRawValue(0L);

        SmartDataStruct s18 = new SmartDataStruct();
        s18.setId(200);
        s18.setAttributeName("Multi_Zone_Error_Rate");
        s18.setFlag("0x08");
        s18.setValue(200);
        s18.setWorst(200);
        s18.setThresh(0);
        s18.setType("Old_age");
        s18.setUpdated("Offline");
        s18.setWhenFailed("-");
        s18.setRawValue(0L);

        reply.setResult(Arrays.asList(s0, s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16, s17, s18));
        return reply;
    }

}
