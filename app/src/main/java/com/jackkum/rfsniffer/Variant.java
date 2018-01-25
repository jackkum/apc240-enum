package com.jackkum.rfsniffer;

/**
 * Created by jackkum on 25.01.18.
 */

public class Variant {
    RfModlue.DRfsk mDRfsk;
    RfModlue.Pout mPout;
    RfModlue.DRin mDRin;
    RfModlue.Parity mParity;
    RfModlue.Tw mTw;
    long frequency = 0;

    public Variant(long frequency, RfModlue.DRfsk mDRfsk, RfModlue.Pout mPout, RfModlue.DRin mDRin, RfModlue.Parity mParity, RfModlue.Tw mTw){
        this.frequency = frequency;
        this.mDRfsk    = mDRfsk;
        this.mPout     = mPout;
        this.mDRin     = mDRin;
        this.mParity   = mParity;
        this.mTw       = mTw;
    }
}
