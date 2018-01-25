package com.jackkum.rfsniffer;

/**
 * Created by jackkum on 25.01.18.
 */

public class RfModlue {
    private static int frequency = 0;
    public static enum DRfsk {
        _1KB,_2KB,_5KB,_10KB,_20KB,_40KB,
    };

    public static enum Pout {
        min,_1,_2,_3,_4,_5,_6,max
    };

    public static enum DRin {
        _1K2,_2K4,_4K8,_9K6,_19K2,_38K4,_57K6
    };

    public static enum Parity {
        NONE, EVEN, ODD
    };

    public static enum Tw {
        _0_05,_0_1,_0_2,_0_4,_0_6,_1,_1_5,_2,_2_5,_3,_4,_5
    }

    public static int getLastFrequency()
    {
        return frequency;
    }

    public static byte[] settingsArray(Variant v)
    {
        RfModlue.frequency = frequency;
        byte [] array = {
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) (v.frequency>>16),
                (byte) (v.frequency>>8),
                (byte) (v.frequency),
                (byte) v.mDRfsk.ordinal(),
                (byte) v.mPout.ordinal(),
                (byte) v.mDRin.ordinal(),
                (byte) v.mParity.ordinal(),
                (byte) v.mTw.ordinal(),
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff,
                (byte) 0xff
        };

        return CRC16.arrayWithCrc16(array);
    }
}
