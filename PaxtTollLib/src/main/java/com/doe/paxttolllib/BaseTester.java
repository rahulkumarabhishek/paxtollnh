package com.doe.paxttolllib;

import android.util.Log;

import timber.log.Timber;

public class BaseTester {

    private String childName = "";

    public BaseTester() {
        
    }

    public void logTrue(String method) {
        childName = getClass().getSimpleName() + ".";
        String trueLog = childName + method;
        Timber.i("IPPITest%s", trueLog);

    }

    public void logErr(String method, String errString) {
        childName = getClass().getSimpleName() + ".";
        String errorLog = childName + method + "   errorMessage：" + errString;
        Timber.e("IPPITest%s", errorLog);
    }
}
