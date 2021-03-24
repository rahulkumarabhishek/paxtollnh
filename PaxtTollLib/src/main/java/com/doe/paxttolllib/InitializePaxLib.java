package com.doe.paxttolllib;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.widget.Toast;

import com.doe.paxttolllib.domain.doecard.samndfelica.felica.FelicaOperations;
import com.doe.paxttolllib.domain.doecard.samndfelica.sam.SAM;
import com.doe.paxttolllib.icc.IccTester;
import com.pax.dal.IDAL;
import com.pax.dal.IPicc;
import com.pax.dal.entity.EPiccType;
import com.pax.dal.exceptions.PiccDevException;
import com.pax.neptunelite.api.NeptuneLiteUser;

import java.io.IOException;

import timber.log.Timber;

public abstract class InitializePaxLib {
    private IDAL felicaDal;
    private static IDAL dal;
    private static Context appContext;
    public  IccDectedThread iccDectedThread;
    public static boolean b = false;
    private IPicc iPicc;
    private SAM sam;
    private FelicaOperations felicaOperations;
    //private DetectFelicaCard detectFelicaCardThred;
    private int repDataLen;
    volatile byte[] reqData = new byte[]{0x06, 0x00, (byte) 0xFF, (byte) 0xFF, 0x01, 0x00};
    volatile byte[] repData = new byte[100];

    public InitializePaxLib(Context applicationContext) {
        appContext = applicationContext;
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        dal = getDal();
        if (iccDectedThread == null) {
            iccDectedThread = new IccDectedThread();
            iccDectedThread.start();
        }
    }


    public static IDAL getDal() {
        if (dal == null) {
            try {
                long start = System.currentTimeMillis();
                dal = NeptuneLiteUser.getInstance().getDal(appContext);
                Timber.i("get dal cost:" + (System.currentTimeMillis() - start) + " ms");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(appContext, "error occurred,DAL is null.", Toast.LENGTH_LONG).show();
            }
        }
        return dal;
    }

//    public void onFelicaCardDiscovered(IPicc ipicc, boolean b) {
//        Timber.d("MainActivity onFelicaCardDiscovered felica detected==%s", b);
//        if (b) {
//            try {
//                byte[] a = mFelicaOperations.readGenericDataWithBalance();
//                Timber.d("MainActivity onFelicaCardDiscovered %s", bytesToHexString(a));
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//            writeTOCard();
//
//        }
//
//    }

    public class IccDectedThread extends Thread {
        @Override
        public void run() {
            super.run();
            IccTester.getInstance().light(true);
            while (!Thread.interrupted()) {
                b = IccTester.getInstance().detect((byte) 2);
                if (b) {
                    byte[] res = IccTester.getInstance().init((byte) 2);
                    if (res == null) {
                        Timber.i("init psam card,but no response");
                        return;
                    }
                    IccTester.getInstance().autoResp((byte) 2, true);// Set whether the iccIsoCommand function automatically sends a GET RESPONSE command。

                    sam = new SAM();
                    try {
                        sam.authenticateSAM();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Timber.e(e);
                    } catch (RemoteException e) {
                        Timber.e(e);
                        Message message = Message.obtain();
                        message.what = 1;
                        message.obj = "sam auhtentication failed";
                        handler.sendMessage(message);
                        SystemClock.sleep(2000);
                        break;
                    }

                    Message message = Message.obtain();
                    message.what = 0;
                    message.obj = "Sam authentication successful";
                    handler.sendMessage(message);
                    SystemClock.sleep(2000);
                    break;

                } else {
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = "sam not detected";
                    handler.sendMessage(message);

                    SystemClock.sleep(2000);
                }
            }
        }

        public void closePsamSlot() {
            IccTester.getInstance().close((byte) 2);
            IccTester.getInstance().light(false);
        }

        public void closeFelicaCardSlot() {
            try {
                iPicc.close();
            } catch (PiccDevException e) {
                e.printStackTrace();
            }
        }

        private Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        Timber.i("init response" + msg.obj.toString());
                        openFelicaSlot();
                        break;
                    case 1:

                    default:
                        break;
                }
            }

            ;
        };

        public void openFelicaSlot() {

            try {
                felicaDal = NeptuneLiteUser.getInstance().getDal(appContext);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (null == felicaDal) {
                Toast.makeText(appContext, "ERROR! Failed to get DAL!", Toast.LENGTH_LONG).show();
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

            felicaOperations = new FelicaOperations(sam, iPicc);
            //writeTOCard();
            if (sam != null) {
                onSamAuthentication(sam, felicaOperations);
            }

        }
    }

    public void openFelica() {

        try {
            iPicc.close();
        } catch (PiccDevException e) {
            e.printStackTrace();
        }

        try {
            felicaDal = NeptuneLiteUser.getInstance().getDal(appContext);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null == felicaDal) {
            Toast.makeText(appContext, "ERROR! Failed to get DAL!", Toast.LENGTH_LONG).show();
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
    }

//    public boolean isFelicaCardNearBy() {
//        if (detectFelicaCardThred == null) {
//            detectFelicaCardThred = new DetectFelicaCard();
//            detectFelicaCardThred.start();
//        }
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (detectFelicaCardThred!=null){
//                    interruptMyThread(detectFelicaCardThred);
//                    onFelicaCardDiscovered(iPicc,false);
//                }
//            }
//        }, 6000);
//        return true;
//    }

    //    public class DetectFelicaCard extends Thread {
//        @Override
//        public void run() {
//            super.run();
//            //polling CMD Sample
//            repDataLen = 100;
//            while (!isInterrupted()) {
//                try {
//                    Timber.d("DetectFelicaCard reqData  = %s", bytesToHexString(reqData));
//                    repData = iPicc.cmdExchange(reqData, repDataLen);
//                } catch (PiccDevException e) {
//                    //SystemClock.sleep(1000);
//                    Timber.e(e, "felica card timeout");
//                    e.printStackTrace();
//                    continue;
//                }
//                Timber.d("DetectFelicaCard Response length = %s", repData.length);
//                Timber.d("DetectFelicaCard Response data = %s", bytesToHexString(repData));
//                Timber.d("DetectFelicaCard felica card detected");
//                //SystemClock.sleep(1000);
//                onFelicaCardDiscovered(iPicc,true);
//                break;
//            }
//            interruptMyThread(detectFelicaCardThred);
//        }
//    }

    public IPicc getIPicc() {
        return iPicc;
    }

    private void interruptMyThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            Timber.d("interruptMyThread");
        }

    }

//    public void stopScanning(){
//        if(detectFelicaCardThred!=null){
//            detectFelicaCardThred.interrupt();
//            detectFelicaCardThred=null;
//        }else {
//            iccDectedThread.interrupt();
//            iccDectedThread=null;
//        }
//    }

    public abstract void onSamAuthentication(SAM sam, FelicaOperations felicaOperations);
}
