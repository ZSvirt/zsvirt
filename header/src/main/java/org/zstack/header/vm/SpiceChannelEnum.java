package org.zstack.header.vm;

import java.util.ArrayList;
import java.util.List;

public enum SpiceChannelEnum {
    main, display, inputs, cursor, playback, record, smartcard, usbredir;

    public static final Integer MAIN = 1;
    public static final Integer DISPLAY = 1 << 1;
    public static final Integer INPUTS = 1 << 2;
    public static final Integer CURSOR = 1 << 3;
    public static final Integer PLAYBACK = 1 << 4;
    public static final Integer RECORD = 1 << 5;
    public static final Integer SMARTCARD = 1 << 6;
    public static final Integer USBREDIR = 1 << 7;

    public static boolean isMain(int mask) {
        return (mask & MAIN) != 0;
    }

    public static boolean isDisplay(int mask) {
        return (mask & DISPLAY) != 0;
    }

    public static boolean isInputs(int mask) {
        return (mask & INPUTS) != 0;
    }

    public static boolean isCursor(int mask) {
        return (mask & CURSOR) != 0;
    }

    public static boolean isPlayback(int mask) {
        return (mask & PLAYBACK) != 0;
    }

    public static boolean isRecord(int mask) {
        return (mask & RECORD) != 0;
    }

    public static boolean isSmartcard(int mask) {
        return (mask & SMARTCARD) != 0;
    }

    public static boolean isUsbredir(int mask) {
        return (mask & USBREDIR) != 0;
    }

    public static List<String> getSpiceChannels(int mask) {
        List<String> channels = new ArrayList<>();
        if (SpiceChannelEnum.isMain(mask)) {
            channels.add(SpiceChannelEnum.main.toString());
        }
        if (SpiceChannelEnum.isDisplay(mask)) {
            channels.add(SpiceChannelEnum.display.toString());
        }
        if (SpiceChannelEnum.isInputs(mask)) {
            channels.add(SpiceChannelEnum.inputs.toString());
        }
        if (SpiceChannelEnum.isCursor(mask)) {
            channels.add(SpiceChannelEnum.cursor.toString());
        }
        if (SpiceChannelEnum.isPlayback(mask)) {
            channels.add(SpiceChannelEnum.playback.toString());
        }
        if (SpiceChannelEnum.isRecord(mask)) {
            channels.add(SpiceChannelEnum.record.toString());
        }
        if (SpiceChannelEnum.isSmartcard(mask)) {
            channels.add(SpiceChannelEnum.smartcard.toString());
        }
        if (SpiceChannelEnum.isUsbredir(mask)) {
            channels.add(SpiceChannelEnum.usbredir.toString());
        }
        return channels;
    }
}
