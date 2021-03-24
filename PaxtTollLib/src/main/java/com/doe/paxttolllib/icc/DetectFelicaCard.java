package com.doe.paxttolllib.icc;

import android.os.Handler;

import com.doe.paxttolllib.InitializePaxLib;
import com.pax.dal.IDAL;
import com.pax.dal.IPicc;
import com.pax.dal.entity.EBeepMode;
import com.pax.dal.entity.EPiccType;
import com.pax.dal.exceptions.EPiccDevException;
import com.pax.dal.exceptions.PiccDevException;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import timber.log.Timber;

import static com.doe.paxttolllib.domain.doecard.samndfelica.Utils.bytesToHexString;

public class DetectFelicaCard {
    private final byte[] reqData = new byte[]{0x06, 0x00, (byte) 0xFF, (byte) 0xFF, 0x01, 0x00};
    private byte[] repData = new byte[100];
    private static DetectFelicaCard instance;
    private scanCallback<Boolean> mCallback;
    private Handler mResultHandler;
    private IDAL felicaDal;
    private IPicc iPicc;
    private ThreadPoolExecutor mExecutor;
    private ArrayList<ScanFelicaCard> mRunningTaskList;
    private static boolean mScanning = false;

    public static DetectFelicaCard getInstance() {
        if (instance == null) {
            synchronized (DetectFelicaCard.class) {
                if (instance == null)
                    instance = new DetectFelicaCard();
            }
        }
        // Return the instance
        return instance;
    }

    private DetectFelicaCard() {

    }

    public void scanFelicaCard(ThreadPoolExecutor executor,
                               Handler resultHandler, scanCallback<Boolean> calback) {
        mRunningTaskList = new ArrayList<>();
        mScanning = true;
        try {
            felicaDal = InitializePaxLib.getDal();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null == felicaDal) {

        }

        iPicc = felicaDal.getPicc(EPiccType.INTERNAL);

        try {
            iPicc.open();

        } catch (PiccDevException e) {
            e.printStackTrace();
        }

        try {
            iPicc.initFelica((byte) 0x00, (byte) 0x00);

        } catch (PiccDevException e) {
            e.printStackTrace();
        }

        mCallback = calback;
        mResultHandler = resultHandler;
        mExecutor = executor;

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ScanFelicaCard task = new ScanFelicaCard();
                mRunningTaskList.add(task);
                task.start();
            }
        });

    }

    private Result<Boolean> scanFelica(IPicc iPicc) {
        while (mScanning) {
            try {
                Timber.d("DetectFelicaCard reqData  = %s", bytesToHexString(reqData));
                int repDataLen = 100;
                repData = iPicc.cmdExchange(reqData, repDataLen);
            } catch (PiccDevException e) {
                if (e.getErrCode() == EPiccDevException.PICC_ERR_NOT_OPEN.getErrCodeFromBasement()) {
                    return new Result.Success<>(false);
                } else if (e.getErrCode() == EPiccDevException.PICC_ERR_TIMEOUT.getErrCodeFromBasement()) {
                    Timber.e(e, "felica card timeout");
                    e.printStackTrace();
                    continue;
                }

            }
            Timber.d("DetectFelicaCard Response length = %s", repData.length);
            Timber.d("DetectFelicaCard Response data = %s", bytesToHexString(repData));
            Timber.d("DetectFelicaCard felica card detected");
            mScanning = false;
            return new Result.Success<>(true);

            // break;
        }
        return new Result.Success<>(false);

    }

    public void stopScanningFelicaCard(ThreadPoolExecutor pool) {
        mScanning = false;
        pool.getQueue().clear();
        for (int i = 0; i < mRunningTaskList.size(); i++) {
            ScanFelicaCard s = mRunningTaskList.get(i);
            s.interrupt();
        }
        mRunningTaskList.clear();
    }

    public interface scanCallback<T> {
        void onScanComplete(Result<T> result);
    }

    class ScanFelicaCard extends Thread {

        @Override
        public void run() {
            Result<Boolean> result = DetectFelicaCard.this.scanFelica(iPicc);
            mResultHandler.post(new Runnable() {
                @Override
                public void run() {
                    felicaDal.getSys().beep(EBeepMode.FREQUENCE_LEVEL_6, 100);
                    mCallback.onScanComplete(result);
                }
            });


        }
    }

}
