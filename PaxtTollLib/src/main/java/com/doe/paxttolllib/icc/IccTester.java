package com.doe.paxttolllib.icc;

import com.doe.paxttolllib.BaseTester;
import com.doe.paxttolllib.InitializePaxLib;
import com.pax.dal.IIcc;
import com.pax.dal.exceptions.IccDevException;

public class IccTester extends BaseTester {


    private IIcc icc;
    private static IccTester iccTester;

    private IccTester() {
        icc = InitializePaxLib.getDal().getIcc();
    }

    public static IccTester getInstance() {
        if (iccTester == null) {
            iccTester = new IccTester();
        }
        return iccTester;
    }

    public byte[] init(byte slot) {
        byte[] initRes = null;
        try {
            initRes = icc.init(slot);
            logTrue("init");
            return initRes;
        } catch (IccDevException e) {
            e.printStackTrace();
            logErr("init", e.toString());
            return null;
        }
    }

    public boolean detect(byte slot) {
        boolean res = false;
        try {
            res = icc.detect(slot);
            logTrue("detect");
            return res;
        } catch (IccDevException e) {
            e.printStackTrace();
            logErr("detect", e.toString());
            return res;
        }
    }

    public void close(byte slot) {
        try {
            icc.close(slot);
            logTrue("close");
        } catch (IccDevException e) {
            e.printStackTrace();
            logErr("close", e.toString());
        }
    }

    public void autoResp(byte slot, boolean autoresp) {
        try {
            icc.autoResp(slot, autoresp);
            logTrue("autoResp");
        } catch (IccDevException e) {
            e.printStackTrace();
            logErr("autoResp", e.toString());
        }
    }

    public byte[] isoCommand(byte slot, byte[] send) {
        try {
            byte[] resp = icc.isoCommand(slot, send);
            logTrue("isoCommand");
            return resp;
        } catch (IccDevException e) {
            e.printStackTrace();
            logErr("isoCommand", e.toString());
            return null;
        }
    }
    
    public void light(boolean flag){
        try {
            icc.light(flag);
            logTrue("light");
        } catch (IccDevException e) {
            e.printStackTrace();
            logErr("light", e.toString());
        }
    }
}
