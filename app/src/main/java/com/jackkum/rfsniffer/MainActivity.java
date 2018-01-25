package com.jackkum.rfsniffer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "USB-Serial";

    private TextView log;
    private ArrayList<String> lines = new ArrayList();
    private ArrayList<Variant> variants = new ArrayList();

    private ReadData rd;
    private int position = 0;
    private long lastSendTime = System.currentTimeMillis();

    UsbManager mUsbManager;
    UsbSerialDriver mDriver;
    PendingIntent mPermissionIntent;
    UsbSerialPort mUsbSerialPort;
    UsbDeviceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_scrolling);

        log = (TextView) findViewById(R.id.logMeesages);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        RfModlue.DRfsk  drfsks[]   = {RfModlue.DRfsk._1KB, RfModlue.DRfsk._2KB, RfModlue.DRfsk._5KB, RfModlue.DRfsk._10KB, RfModlue.DRfsk._20KB, RfModlue.DRfsk._40KB};
        RfModlue.Pout   pouts[]    = {RfModlue.Pout._1, RfModlue.Pout._2, RfModlue.Pout._3, RfModlue.Pout._4, RfModlue.Pout._5, RfModlue.Pout._6};
        RfModlue.DRin   drins[]    = {RfModlue.DRin._1K2, RfModlue.DRin._2K4, RfModlue.DRin._4K8, RfModlue.DRin._9K6, RfModlue.DRin._19K2, RfModlue.DRin._38K4, RfModlue.DRin._57K6};
        RfModlue.Parity parities[] = {RfModlue.Parity.NONE, RfModlue.Parity.EVEN, RfModlue.Parity.ODD};
        RfModlue.Tw     tws[]      = {RfModlue.Tw._0_1, RfModlue.Tw._0_2, RfModlue.Tw._0_4, RfModlue.Tw._0_05, RfModlue.Tw._0_6, RfModlue.Tw._1, RfModlue.Tw._1_5, RfModlue.Tw._2, RfModlue.Tw._2_5, RfModlue.Tw._3, RfModlue.Tw._3, RfModlue.Tw._4, RfModlue.Tw._5};

        for(long i = 433000L; i < 437000L; i += 200) {
            for(RfModlue.DRfsk drfsk: drfsks){
                for(RfModlue.Pout pout: pouts){
                    for(RfModlue.DRin drin : drins){
                        for(RfModlue.Parity parity : parities){
                            for(RfModlue.Tw tw : tws){
                                variants.add(new Variant(i, drfsk, pout, drin, parity, tw));
                            }
                        }
                    }
                }
            }
        }
    }

    public void addLog(String message)
    {
        Calendar now = Calendar.getInstance();
        for(String l : message.split("\n")){
            String line = String.format("[%02d:%02d:%02d] %s", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND), l);
            lines.add(0, line);
        }

        String tmp = "";
        for(String line : lines){
            tmp += line + "\n";
        }

        log.setText(tmp);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        rd = new ReadData();
        rd.execute();

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        //Check currently connected devices
        updateDeviceList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);

        try {
            rd.stop();
            mConnection.close();
        } catch(Exception e){}
    }

    private void updateDeviceList() {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
        if (availableDrivers.isEmpty()) {
            addLog("Devices not found");
            return;
        }

        addLog("Got devices, choose one");

        // Open a connection to the first available driver.
        mDriver = availableDrivers.get(0);
        mConnection = mUsbManager.openDevice(mDriver.getDevice());
        if (mConnection == null) {
            addLog("Request permission...");
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            mUsbManager.requestPermission(mDriver.getDevice(), mPermissionIntent);
            return;
        }

        addLog("Get port");
        mUsbSerialPort = mDriver.getPorts().get(0);

        try {
            addLog("Open connection...");
            mUsbSerialPort.open(mConnection);
            addLog("Success opened");
        } catch(Exception e){
            e.printStackTrace();
            addLog("Error: " + e.toString());
        }
    }


    /*
     * Receiver to catch user permission responses, which are required in order to actuall
     * interact with a connected device.
     */
    private static final String ACTION_USB_PERMISSION = "com.android.recipes.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                    //Query the device's descriptor
                    //getDeviceStatus(device);
                    addLog("Permission allowed for: " + device.getDeviceName());
                } else {
                    Log.d(TAG, "permission denied for device " + device);
                }
            }
        }
    };

    final Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 0:
                    addLog("Read: " + bytesToHex((byte [])msg.obj));
                    break;
                case 1:
                    addLog("Error: " + msg.obj);
                    break;
            }
        }
    };

    private class ReadData extends AsyncTask {

        private boolean active = true;

        public void stop()
        {
            active = false;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            while(active){

                try {
                    if(mUsbSerialPort != null){

                        if((System.currentTimeMillis() - lastSendTime) > 5000){
                            Variant v = variants.get(position++);
                            if(position >= variants.size()){
                                position = 0;
                            }

                            lastSendTime = System.currentTimeMillis();
                            mUsbSerialPort.write(RfModlue.settingsArray(v), 18);
                        }

                        mUsbSerialPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                        byte buffer[] = new byte[16];
                        int numBytesRead = mUsbSerialPort.read(buffer, 1000);
                        Log.d(TAG, "Read " + numBytesRead + " bytes.");

                        handler.obtainMessage(0, buffer).sendToTarget();
                    }
                } catch(Exception e){
                    e.printStackTrace();
                    handler.obtainMessage(1, e.toString()).sendToTarget();
                }

                if( ! active){
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch(Exception e){}
            }

            return null;
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
