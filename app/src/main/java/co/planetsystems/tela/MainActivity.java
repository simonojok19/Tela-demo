package co.planetsystems.tela;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.suprema.BioMiniFactory;
import com.suprema.CaptureResponder;
import com.suprema.IBioMiniDevice;
import com.suprema.IUsbEventHandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import co.planetsystems.tela.dialog.ClockAndOutDialog;

public class MainActivity extends AppCompatActivity {
    //Flag.
    public static final boolean mbUsbExternalUSBManager = false;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String CLOCK_DIALOG_TAG = "co.planetsystems.tela.MainActivity.CLOCK_IN_OUT_DIALOG_TAG";
    public static final int ENROLL_TEACHTER = 542;
    private UsbManager mUsbManager = null;
    private PendingIntent mPermissionIntent= null;
    //

    private static BioMiniFactory mBioMiniFactory = null;
    public static final int REQUEST_WRITE_PERMISSION = 786;
    public IBioMiniDevice mCurrentDevice = null;
    private MainActivity mainContext;

    public final static String TAG = "Tela Log";
    private TextView mLogView;
    private TextView mStatusView;
    private ScrollView mScrollLog = null;
    private CardView backgroundCard;
    private TextClock textClock;
    private TextView textDate;
    private Button attendance;
    private Button enroll;
    private Button verify;
    private Button clockIn;
    private Button capture;
    private Button clockOut;
    public IBioMiniDevice.TemplateData teacherCapturedTemplate;
    private Bitmap teacherImage;
    private TeacherViewModel teacherViewModel;

    private IBioMiniDevice.CaptureOption mCaptureOptionDefault = new IBioMiniDevice.CaptureOption();
    private CaptureResponder mCaptureResponseDefault = new CaptureResponder() {
        @Override
        public boolean onCaptureEx(final Object context, final Bitmap capturedImage,
                                   final IBioMiniDevice.TemplateData capturedTemplate,
                                   final IBioMiniDevice.FingerState fingerState) {
            log("onCapture : Capture successful!");
            printState(getResources().getText(R.string.capture_single_ok));

            log(((IBioMiniDevice) context).popPerformanceLog());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if((capturedImage != null)) {
                        ImageView iv = (ImageView) findViewById(R.id.finger_image);
                        if(iv != null) {
                            iv.setImageBitmap(capturedImage);

                        }
                    }
                    if (capturedTemplate != null) {
                        Toast.makeText(MainActivity.this, "Template exist", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Template exist is null", Toast.LENGTH_SHORT).show();
                    }
                }
            });

//            Toast.makeText(MainActivity.this, "Stored well", Toast.LENGTH_SHORT).show();
            return true;
        }

        @Override
        public void onCaptureError(Object contest, int errorCode, String error) {
            log("onCaptureError : " + error + " ErrorCode :" + errorCode);
            if( errorCode != IBioMiniDevice.ErrorCode.OK.value())
                printState(getResources().getText(R.string.capture_single_fail) + "("+error+")");
        }
    };
    private CaptureResponder mCaptureResponsePrev = new CaptureResponder() {
        @Override
        public boolean onCaptureEx(final Object context, final Bitmap capturedImage,
                                   final IBioMiniDevice.TemplateData capturedTemplate,
                                   final IBioMiniDevice.FingerState fingerState) {

            Log.d("CaptureResponsePrev", String.format(Locale.ENGLISH , "captureTemplate.size (%d) , fingerState(%s)" , capturedTemplate== null? 0 : capturedTemplate.data.length, String.valueOf(fingerState.isFingerExist)));
            printState(getResources().getText(R.string.start_capture_ok));
            byte[] pImage_raw =null;
            if( (mCurrentDevice!= null && (pImage_raw = mCurrentDevice.getCaptureImageAsRAW_8() )!= null)) {
                Log.d("CaptureResponsePrev ", String.format(Locale.ENGLISH, "pImage (%d) , FP Quality(%d)", pImage_raw.length , mCurrentDevice.getFPQuality(pImage_raw, mCurrentDevice.getImageWidth(), mCurrentDevice.getImageHeight(), 2)));
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(capturedImage != null) {
                        ImageView iv = (ImageView) findViewById(R.id.finger_image);
                        if(iv != null) {
                            iv.setImageBitmap(capturedImage);
                        }
                    }
                }
            });
            return true;
        }

        @Override
        public void onCaptureError(Object context, int errorCode, String error) {
            log("onCaptureError : " + error);
            log(((IBioMiniDevice)context).popPerformanceLog());
            if( errorCode != IBioMiniDevice.ErrorCode.OK.value())
                printState(getResources().getText(R.string.start_capture_fail));
        }
    };
    synchronized public void printState(final CharSequence str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView)findViewById(R.id.status_view)).setText(str);
            }
        });

    }
    synchronized public void log(final String msg)
    {
        Log.d(TAG, msg);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if( mLogView == null){
                    mLogView = (TextView) findViewById(R.id.log_text );
                }
                if(mLogView != null) {
                    mLogView.append(msg + "\n");
                    if(mScrollLog != null) {
                        mScrollLog.fullScroll(mScrollLog.FOCUS_DOWN);
                    }else{
                        Log.d("Log " , "ScrollView is null");
                    }
                }
                else {
                    Log.d("", msg);
                }
            }
        });
    }

    synchronized public void printRev(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
//                ((TextView) findViewById(R.id.revText)).setText(msg);
            }
        });
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver(){
        public void onReceive(Context context,Intent intent){
            String action = intent.getAction();
            if(ACTION_USB_PERMISSION.equals(action)){
                synchronized(this){
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
                        if(device != null){
                            if( mBioMiniFactory == null) return;
                            mBioMiniFactory.addDevice(device);
                            log(String.format(Locale.ENGLISH ,"Initialized device count- BioMiniFactory (%d)" , mBioMiniFactory.getDeviceCount() ));
                        }
                    }
                    else{
                        Log.d(TAG, "permission denied for device"+ device);
                    }
                }
            }
        }
    };
    public void checkDevice(){
        if(mUsbManager == null) return;
        log("checkDevice");
        HashMap<String , UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIter = deviceList.values().iterator();
        while(deviceIter.hasNext()){
            UsbDevice _device = deviceIter.next();
            if( _device.getVendorId() ==0x16d1 ){
                //Suprema vendor ID
                mUsbManager.requestPermission(_device , mPermissionIntent);
            }else{
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainContext = this;
        attendance = findViewById(R.id.list_teachers);
        enroll = findViewById(R.id.enroll);
        verify = findViewById(R.id.verify);
        clockIn = findViewById(R.id.clock_in);
        clockOut = findViewById(R.id.clock_out);
        capture = findViewById(R.id.capture);

        // disable very thing
//        disableButton(enroll);
//        disableButton(verify);
//        disableButton(clockIn);
//        disableButton(clockOut);
//        disableButton(capture);

        mCaptureOptionDefault.frameRate = IBioMiniDevice.FrameRate.SHIGH;
        backgroundCard = findViewById(R.id.card_background);
        mStatusView = findViewById(R.id.status_view);
        textClock = findViewById(R.id.textClock);
        textClock.setFormat24Hour("hh:mm:ss a EEE MMM d");
        textClock.animate();
        textDate = findViewById(R.id.date_view);
        textDate.setText(getCurrentDate());

        teacherViewModel = new ViewModelProvider(this).get(TeacherViewModel.class);

        enroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((teacherCapturedTemplate != null)) {
                    Intent intent = new Intent(MainActivity.this, EnrollActivity.class);
                    intent.setAction(EnrollActivity.ACTION_ENROLL);
                    intent.putExtra(EnrollActivity.CAPTURED_BITMAP, teacherImage);
                    intent.putExtra(EnrollActivity.CAPTURED_TEMPLATE, teacherCapturedTemplate.data);
                    startActivityForResult(intent, ENROLL_TEACHTER);
                } else {
                    Toast.makeText(MainActivity.this, "Please Capture your fingerprint", Toast.LENGTH_SHORT).show();
                }
            }
        });

        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VerifyActivity.class);
                intent.setAction(EnrollActivity.ACTION_VERIFY);
                startActivity(intent);
            }
        });

        findViewById(R.id.clock_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClockAndOutDialog dialog = ClockAndOutDialog.newInstance(
                        "Simon Peter",
                        "Clock IN",
                        "08: 30 am");
                dialog.show(getSupportFragmentManager(), CLOCK_DIALOG_TAG);
            }
        });


        findViewById(R.id.capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backgroundCard.setCardBackgroundColor(getResources().getColor(R.color.colorBackgroundCapturing));
                ((ImageView) findViewById(R.id.finger_image)).setImageDrawable(getDrawable(R.drawable.ic_fingerprint_black_24dp));
                if(mCurrentDevice != null) {
                    //mCaptureOptionDefault.captureTimeout = (int)mCurrentDevice.getParameter(IBioMiniDevice.ParameterType.TIMEOUT).value;
                    mCurrentDevice.captureSingle(
                            mCaptureOptionDefault,
                            mCaptureResponseDefault,
                            true);
                }
            }
        });

        attendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, TeacherListActivity.class);
                startActivity(intent);
            }
        });

        if(mBioMiniFactory != null) {
            mBioMiniFactory.close();
        }

        restartBioMini();

        printRev(""+mBioMiniFactory.getSDKInfo());

    }

    void restartBioMini() {
        if(mBioMiniFactory != null) {
            mBioMiniFactory.close();
        }
        if( mbUsbExternalUSBManager ){
            mUsbManager = (UsbManager)getSystemService(Context.USB_SERVICE);
            mBioMiniFactory = new BioMiniFactory(mainContext, mUsbManager){
                @Override
                public void onDeviceChange(DeviceChangeEvent event, Object dev) {
                    log("----------------------------------------");
                    log("Fingerprint Scanner Changed : " + event + " using external usb-manager");
                    log("----------------------------------------");
                    handleDevChange(event, dev);
                }
            };
            //
            mPermissionIntent = PendingIntent.getBroadcast(this,0,new Intent(ACTION_USB_PERMISSION),0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(mUsbReceiver, filter);
            checkDevice();
        }else {
            mBioMiniFactory = new BioMiniFactory(mainContext) {
                @Override
                public void onDeviceChange(DeviceChangeEvent event, Object dev) {
                    log("----------------------------------------");
                    log("Fingerprint Scanner Changed : " + event);
                    log("----------------------------------------");
                    handleDevChange(event, dev);

                }
            };
        }
    }

    void handleDevChange(IUsbEventHandler.DeviceChangeEvent event, Object dev) {
        if (event == IUsbEventHandler.DeviceChangeEvent.DEVICE_ATTACHED && mCurrentDevice == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int cnt = 0;
                    while (mBioMiniFactory == null && cnt < 20) {
                        SystemClock.sleep(1000);
                        cnt++;
                    }
                    if (mBioMiniFactory != null) {
                        mCurrentDevice = mBioMiniFactory.getDevice(0);
                        printState(getResources().getText(R.string.device_attached));
                        Log.d(TAG, "Hardware Attached : " + mCurrentDevice);
                        if (mCurrentDevice != null /*&& mCurrentDevice.getDeviceInfo() != null*/) {
                            enableButton(capture);
                            backgroundCard.setCardBackgroundColor(getResources().getColor(R.color.colorBackgroundConnected));
                            log(" DeviceName : " + mCurrentDevice.getDeviceInfo().deviceName);
                            log("         SN : " + mCurrentDevice.getDeviceInfo().deviceSN);
                            log("SDK version : " + mCurrentDevice.getDeviceInfo().versionSDK);
                        }
                    }
                }
            }).start();
        } else if (mCurrentDevice != null && event == IUsbEventHandler.DeviceChangeEvent.DEVICE_DETACHED && mCurrentDevice.isEqual(dev)) {
            backgroundCard.setCardBackgroundColor(getResources().getColor(R.color.colorRed));
            disableButton(capture);
            disableButton(verify);
            disableButton(enroll);
            disableButton(clockOut);
            disableButton(clockIn);
            printState(getResources().getText(R.string.device_detached));
            Log.d(TAG, "Fingerprint Scanner removed : " + mCurrentDevice);
            mCurrentDevice = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (mBioMiniFactory != null) {
            mBioMiniFactory.close();
            mBioMiniFactory = null;
        }
        if( mbUsbExternalUSBManager ){
            unregisterReceiver(mUsbReceiver);
        }
        super.onDestroy();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},  REQUEST_WRITE_PERMISSION);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            log("Permission Granted");
        }
    }
    @Override
    public void onPostCreate(Bundle savedInstanceState){
        requestPermission();
        super.onPostCreate(savedInstanceState);
    }

    private String getCurrentDate() {
        Date toDay = Calendar.getInstance().getTime();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        return simpleDateFormat.format(toDay);
    }

    private void disableButton(Button view) {
        view.setEnabled(false);
        view.setBackgroundColor(getResources().getColor(R.color.colorLight));
        view.setTextColor(getResources().getColor(R.color.white));
    }

    private void enableButton(Button view) {
        view.setEnabled(true);
        view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        view.setTextColor(getResources().getColor(R.color.colorLight));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setting:
                // do some settings
                return true;

            case R.id.about:
                // do about things
                return true;

            case R.id.help:
                // do some help
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK && requestCode == ENROLL_TEACHTER) {
            Toast.makeText(this, "Saving teacher", Toast.LENGTH_SHORT).show();
            Teacher teacher = new Teacher(
                    intent.getStringExtra(EnrollActivity.NATIONAL_ID),
                    intent.getStringExtra(EnrollActivity.FIRST_NAME),
                    intent.getStringExtra(EnrollActivity.LAST_NAME),
                    intent.getStringExtra(EnrollActivity.PHONE_NUMBER),
                    intent.getStringExtra(EnrollActivity.EMAIL_ADDRESS),
                    intent.getStringExtra(EnrollActivity.GENDER),
                    intent.getStringExtra(EnrollActivity.SCHOOL_NAME),
                    intent.getStringExtra(EnrollActivity.DISTRICT),
                    intent.getStringExtra(EnrollActivity.ROLE),
                    intent.getByteArrayExtra(EnrollActivity.CAPTURED_BITMAP),
                    intent.getByteArrayExtra(EnrollActivity.CAPTURED_TEMPLATE),
                    "23/93/10029",
                    null
            );
            teacherViewModel.enrollTeacher(teacher);
        }
    }
}
