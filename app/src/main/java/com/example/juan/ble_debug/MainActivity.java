package com.example.juan.ble_debug;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.example.juan.self.goals.GoSleepGD;
import com.example.juan.self.goals.IncreasePrecisionGD;
import com.example.juan.self.goals.SaveEnergyGD;
import com.example.juan.self.goals.WakeUpGD;
import com.example.juan.self.plans.DisableNNPD;
import com.example.juan.self.plans.EnableNNPD;
import com.example.juan.self.plans.IncreaseBatteryPeriodPD;
import com.example.juan.self.plans.IncreaseUARTPeriodPD;
import com.example.juan.self.plans.ReduceBatteryPeriodPD;
import com.example.juan.self.plans.ReduceUARTPeriodPD;
import com.example.juan.self.plans.SleepSensorsPD;
import com.example.juan.self.plans.WakeUpSensorsPD;
import com.example.juan.tanit.dspl.PreferenceBasedFeatureModel;
import com.example.juan.tanit.dspl.StringCrossTreeConstraint;
import com.example.juan.tanit.plan.PlanPreference;
import com.example.juan.tanit.selfadaptation.Optimization;
import com.example.juan.tanit.selfadaptation.PreferenceBasedEngine;
import com.example.juan.tanit.selfadaptation.SAActivityInterface;
import com.example.juan.tanit.selfadaptation.SAComponentInterface;
import com.example.juan.tanit.selfadaptation.WellnessFactor;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import static java.lang.StrictMath.sqrt;

public class MainActivity extends AppCompatActivity implements SAActivityInterface {
    // auto-adaptación
    private PreferenceBasedEngine preferenceBasedEngine;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private com.example.juan.tanit.dspl.Configuration currentConfiguration;
    private UARTServiceInterface uartService;
    private BatteryServiceInterface batteryService;
    //private Integer batteryPeriod,uartPeriod;

    // detección de usuario en reposo
    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";
    private PendingIntent mPendingIntent;
    private TransitionsReceiver mTransitionsReceiver;


    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int BATT_PROFILE_CONNECTED = 22;
    private static final int BATT_PROFILE_DISCONNECTED = 23;

    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BatteryService battService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private TextView textv, textBatt;
    private TextInputEditText editTextSample, editTextBattery;
    private TextInputLayout textInputSample, textInputBattery;
    private Button btn_connect, btn_send, btn_debug;
    private RadioButton radioFSM, radioMLP;
    private RadioGroup radioG;
    private ProgressBar batLevelBar;
    private Toolbar mActionBarToolbar;
    int counter = 0;
    boolean debug;
    byte system_sel;
    boolean neuralNetwork;
    int content_sample;
    Double sampleDouble;
    int content_bat;

    SimpleDateFormat dateStr = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    SimpleDateFormat dateFile = new SimpleDateFormat("dd-MM-yyyy");
    String fullPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CaneControl";
    File dir;
    File file_sensor;
    File file_bat;
    String fName_sensor;
    String fName_bat;
    String strRate, strSys;

    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            default:
                return "UNKNOWN";
        }
    }

    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mActionBarToolbar);
        getSupportActionBar().setTitle("Cane Control");

        textv = (TextView) findViewById(R.id.textView);
        textv.setMovementMethod(new ScrollingMovementMethod());
        editTextSample = (TextInputEditText) findViewById(R.id.editText_sample);
        textInputSample = (TextInputLayout) findViewById(R.id.text_input_layout_sample);
        editTextBattery = (TextInputEditText) findViewById(R.id.editText_battery);
        textInputBattery = (TextInputLayout) findViewById(R.id.text_input_layout_battery);
        textBatt = (TextView) findViewById(R.id.textLevel);
        btn_connect = (Button) findViewById(R.id.buttonConnect);
        btn_send = (Button) findViewById(R.id.buttonSend);
        btn_debug = (Button) findViewById(R.id.buttonDebug);
        radioG = (RadioGroup) findViewById(R.id.sys_group);
        radioFSM = (RadioButton) findViewById(R.id.FSM_radio_btn) ;
        radioMLP = (RadioButton) findViewById(R.id.MLP_radio_btn) ;
        batLevelBar = (ProgressBar) findViewById(R.id.batteryLevel);
        batLevelBar.setMax(100);
        textBatt.setText("0%");
        btn_send.setEnabled(false);
        btn_debug.setEnabled(false);

        try {
            dir = new File(fullPath);
            //int index;
            //String nameLFile;

            if (!dir.exists()) {
                dir.mkdirs();
                //index = 1;
            } /*else {
                File[] files = dir.listFiles();
                nameLFile = files[files.length-1].toString();
                if(nameLFile.contains(dateFile.format(new Date()))){
                    nameLFile = nameLFile.replace(fullPath + "/" + dateFile.format(new Date()) + "_DataLog_","");
                    int indexP = nameLFile.indexOf(".");
                    index = Integer.valueOf(nameLFile.substring(0,indexP)) + 1;
                } else {
                    index = 1;
                }
            }*/
            Date fDate = new Date();
            fName_sensor = dateStr.format(fDate) + "_sensor.txt";
            fName_bat = dateStr.format(fDate) + "_bat.txt";
            file_sensor = new File(fullPath, fName_sensor);
            file_bat = new File (fullPath, fName_bat);
            file_sensor.createNewFile();
            file_bat.createNewFile();

        } catch(Exception e) {
            e.printStackTrace();
        }

        // inma: Inicialización del Bluetooth
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // inma: Se inicializa el uart
        service_init();

        // Handle Disconnect & Connect button
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // inma: cuando pulsamos el botón nos conectamos al Bluetooth
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btn_connect.getText().equals("Connect")){
                        btn_connect.setText("Connecting...");
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            mService.disconnect();
                            battService.disconnect();
                        }
                    }
                }
            }
        });

        // configuración del sistema dada por el usuario.
        // toma datos de los campos y los envía al sensor para que se configure
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                content_sample = Integer.parseInt(editTextSample.getText().toString());
                content_bat = Integer.parseInt(editTextBattery.getText().toString());
                sampleDouble = (double)content_sample;

                if(debug){
                    textv.setText(null);
                }
                debug = false;
                if (validate(content_sample, content_bat)) {
                    byte[] value = new byte[3];
                    if(radioFSM.isChecked()){
                        system_sel = 0; //FMS seleccionado
                        neuralNetwork = false;
                        strSys = " FSM";
                    } else {
                        system_sel = 1; //MLP seleccionado
                        neuralNetwork = true;
                        strSys = " MLP";
                    }

                    if(content_sample == 0){
                        strRate = dateStr.format(new Date()) + " Sleep Mode " + "\n";
                    } else {
                        strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                                    " Hz | Battery rate: " + content_bat + " min\n";
                    }
                        value[0] = (byte)(content_sample & 0xFF);
                        value[1] = (byte)(content_bat & 0xFF);
                        value[2] = system_sel;
                        // envío de información al sensor
                        mService.writeRXCharacteristic(value);
                        textv.append(strRate);
                        try {
                            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                            fOut.write(strRate.getBytes());
                            fOut.close();
                        }
                        catch(IOException e) {
                            Log.e("Exception", "File write failed: " + e.toString());
                        }
                        editTextSample.getText().clear();
                        editTextBattery.getText().clear();
                        radioG.clearCheck();
                }
            }
        });

        // el botón de debug ya no debería de utilizarse...
        btn_debug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*try {
                    String strDebug = "#";
                    byte[] value;
                    if(!debug){
                        textv.setText(null);
                    }
                    debug = true;
                    value = strDebug.getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
            }
        });
        // lecuturas de acelerómetro
        /*sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(deviceSensors.size()>0){
            // Hay algún acelerómetro en el móvil
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }*/
    }
    // termina el oncreate.

    private void setupActivityTransitions() {
        List<ActivityTransition> transitions = new ArrayList<>();
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());
        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        // Register for Transitions Updates.
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mPendingIntent);
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.i(TAG, "Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Transitions Api could not be registered: " + e);
                    }
                });
    }


    private void selfAdaptation(){
        // 1. Se crea la engine de auto-adaptación, los factores de wellness y el contexto
        PreferenceBasedFeatureModel pbFeatureModel=new PreferenceBasedFeatureModel();
        preferenceBasedEngine=new PreferenceBasedEngine(this,pbFeatureModel);
        WellnessFactor batteryFactor=new WellnessFactor("battery", Optimization.MAX,100.0);
        preferenceBasedEngine.addWellnessFactor(batteryFactor);
        preferenceBasedEngine.updateWellnessFactor("battery",100.0);
        preferenceBasedEngine.updateContext("battery",100.0);
        WellnessFactor precisionFactor=new WellnessFactor("precision",Optimization.MAX,100.0);
        preferenceBasedEngine.addWellnessFactor(precisionFactor);
        preferenceBasedEngine.updateWellnessFactor("precision",100.0);
        preferenceBasedEngine.updateContext("precision",100.0);
        preferenceBasedEngine.updateContext("repose",false);
        preferenceBasedEngine.updateContext("awake",true);
        //batteryPeriod=6;
        //uartPeriod=5;

        // creación de servicios
        // UART
        // parámetros
        HashMap<String, List<String>> parameterTable=new HashMap<>();
        List<String> parameterValue=new LinkedList<>();
        parameterValue.add("5");
        parameterValue.add("10");
        parameterTable.put("uart_period",parameterValue);
        parameterValue=new LinkedList<>();
        parameterValue.add("enable");
        parameterValue.add("disable");
        parameterTable.put("nn",parameterValue);
        // calidad
        List<String> positive=new LinkedList<>();
        positive.add("5");
        positive.add("enable");
        HashMap<String, StringCrossTreeConstraint> qualityTable=new HashMap<>();
        qualityTable.put("q1",new StringCrossTreeConstraint(positive,new LinkedList<String>()));
        positive=new LinkedList<>();
        positive.add("5");
        positive.add("disable");
        qualityTable.put("q2",new StringCrossTreeConstraint(positive,new LinkedList<String>()));
        positive.add("10");
        positive.add("disable");
        qualityTable.put("q3",new StringCrossTreeConstraint(positive,new LinkedList<String>()));
        preferenceBasedEngine.registerService("uart",qualityTable,parameterTable,true);
        // Battery
        // parámetros
        parameterTable=new HashMap<>();
        parameterValue=new LinkedList<>();
        parameterValue.add("6");
        parameterValue.add("11");
        parameterTable.put("battery_period",parameterValue);
        // calidad
        positive=new LinkedList<>();
        positive.add("6");
        qualityTable=new HashMap<>();
        qualityTable.put("q1",new StringCrossTreeConstraint(positive,new LinkedList<String>()));
        positive=new LinkedList<>();
        positive.add("11");
        qualityTable.put("q2",new StringCrossTreeConstraint(positive,new LinkedList<String>()));
        preferenceBasedEngine.registerService("battery",qualityTable,parameterTable,true);

        // Objetivos
        //GoSleepGD goSleepGD=new GoSleepGD();
        //WakeUpGD wakeUpGD=new WakeUpGD();
        IncreasePrecisionGD increasePrecisionGD=new IncreasePrecisionGD();
        SaveEnergyGD saveEnergyGD=new SaveEnergyGD();


        // Descripciones de Plan
        DisableNNPD disableNNPD=new DisableNNPD();
        EnableNNPD enableNNPD=new EnableNNPD();
        IncreaseBatteryPeriodPD increaseBatteryPeriodPD =new IncreaseBatteryPeriodPD();
        IncreaseUARTPeriodPD increaseUARTPeriodPD =new IncreaseUARTPeriodPD();
        ReduceBatteryPeriodPD reduceBatteryPeriodPD =new ReduceBatteryPeriodPD();
        ReduceUARTPeriodPD reduceUARTPeriodPD =new ReduceUARTPeriodPD();
        SleepSensorsPD sleepSensorsPD=new SleepSensorsPD();
        WakeUpSensorsPD wakeUpSensorsPD=new WakeUpSensorsPD();

        // se registra la plan preference para cada plan
        // Tendremos que jugar con estos factores para confirmar que tenemos el comportamiento deseado.
        // disablennpd
        HashMap<WellnessFactor,Double> preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,-50.0);
        preferenceTable.put(precisionFactor,-50.0);
        PlanPreference disableNNPP=new PlanPreference(disableNNPD,preferenceTable,1.0);
        preferenceBasedEngine.registerPlanDescription(saveEnergyGD,disableNNPD,disableNNPP);
        // enablennpd
        preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,50.0);
        preferenceTable.put(precisionFactor,50.0);
        PlanPreference enableNNPP=new PlanPreference(enableNNPD,preferenceTable,1.0);
        preferenceBasedEngine.registerPlanDescription(increasePrecisionGD,enableNNPD,enableNNPP);
        // IncreaseBatteryPeriodPD
        preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,25.0);
        preferenceTable.put(precisionFactor,25.0);
        PlanPreference increaseBatteryPeriodPP=new PlanPreference(increaseBatteryPeriodPD,preferenceTable,0.25);
        preferenceBasedEngine.registerPlanDescription(saveEnergyGD, increaseBatteryPeriodPD,increaseBatteryPeriodPP);
        // IncreaseUARTPeriodPD
        preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,25.0);
        preferenceTable.put(precisionFactor,25.0);
        PlanPreference increaseUARTPeriodPP=new PlanPreference(increaseUARTPeriodPD,preferenceTable,0.25);
        preferenceBasedEngine.registerPlanDescription(saveEnergyGD, increaseUARTPeriodPD,increaseUARTPeriodPP);
        // ReduceBatteryPeriodPD
        preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,-25.0);
        preferenceTable.put(precisionFactor,-25.0);
        PlanPreference reduceBatteryPeriodPP=new PlanPreference(reduceBatteryPeriodPD,preferenceTable,0.25);
        preferenceBasedEngine.registerPlanDescription(increasePrecisionGD, reduceBatteryPeriodPD,reduceBatteryPeriodPP);
        // ReduceUARTPeriodPD
        preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,-25.0);
        preferenceTable.put(precisionFactor,-25.0);
        PlanPreference reduceUARTPeriodPP=new PlanPreference(reduceUARTPeriodPD,preferenceTable,0.25);
        preferenceBasedEngine.registerPlanDescription(increasePrecisionGD, reduceUARTPeriodPD,reduceUARTPeriodPP);
        // SleepSensorsPD
        /*preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,-50.0);
        preferenceTable.put(precisionFactor,-50.0);
        PlanPreference sleepSensorsPP=new PlanPreference(sleepSensorsPD,preferenceTable,1.0);
        preferenceBasedEngine.registerPlanDescription(goSleepGD,sleepSensorsPD,sleepSensorsPP);*/
        // WakeUpSensorsPD
        /*preferenceTable=new HashMap<>();
        preferenceTable.put(batteryFactor,50.0);
        preferenceTable.put(precisionFactor,50.0);
        PlanPreference wakeUpSensorsPP=new PlanPreference(wakeUpSensorsPD,preferenceTable,1.0);
        preferenceBasedEngine.registerPlanDescription(wakeUpGD,wakeUpSensorsPD,wakeUpSensorsPP);*/

        // configuración inicial de los servicios
        batteryService=new BatteryServiceInterface(this);
        //batteryService.start();
        uartService=new UARTServiceInterface(this);
        //uartService.start();

        // configuración inicial del FM
        currentConfiguration=pbFeatureModel.getMinimalConfiguration();
        // Servicios
        // battery
        int serviceId=pbFeatureModel.getID("battery");
        currentConfiguration.add(serviceId);
        // parámetros
        serviceId=pbFeatureModel.getID("battery_parameters");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("battery_period");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("6");
        currentConfiguration.add(serviceId);
        // calidad
        serviceId=pbFeatureModel.getID("battery_quality");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("battery_q1");
        currentConfiguration.add(serviceId);
        // uart
        serviceId=pbFeatureModel.getID("uart");
        currentConfiguration.add(serviceId);
        // parámetros
        serviceId=pbFeatureModel.getID("uart_parameters");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("uart_period");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("5");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("nn");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("enable");
        currentConfiguration.add(serviceId);
        // calidad
        serviceId=pbFeatureModel.getID("uart_quality");
        currentConfiguration.add(serviceId);
        serviceId=pbFeatureModel.getID("uart_q1");
        currentConfiguration.add(serviceId);
        // Objetivos
        /**
         * GoSleepGD goSleepGD=new GoSleepGD();
         * IncreasePrecisionGD increasePrecisionGD=new IncreasePrecisionGD();
         * SaveEnergyGD saveEnergyGD=new SaveEnergyGD();
         * WakeUpGD wakeUpGD=new WakeUpGD();
         * */
        //int goalId=pbFeatureModel.getID(goSleepGD.getGoalClass().toString().toLowerCase());
        //currentConfiguration.add(goalId);
        int goalId=pbFeatureModel.getID(saveEnergyGD.getGoalClass().toString().toLowerCase());
        currentConfiguration.add(goalId);
        // Planes
        /**
         * DisableNNPD disableNNPD=new DisableNNPD();
         * EnableNNPD enableNNPD=new EnableNNPD();
         * IncreaseBatteryPeriodPD increaseBatteryPeriodPD=new IncreaseBatteryPeriodPD();
         * IncreaseUARTPeriodPD increaseUARTPeriodPD=new IncreaseUARTPeriodPD();
         * ReduceBatteryPeriodPD reduceBatteryPeriodPD=new ReduceBatteryPeriodPD();
         * ReduceUARTPeriodPD reduceUARTPeriodPD=new ReduceUARTPeriodPD();
         * SleepSensorsPD sleepSensorsPD=new SleepSensorsPD();
         * WakeUpSensorsPD wakeUpSensorsPD=new WakeUpSensorsPD();
         * */
        int planId=pbFeatureModel.getID(disableNNPD.getPlanClass().toString().toLowerCase());
        currentConfiguration.add(planId);
        planId=pbFeatureModel.getID(increaseBatteryPeriodPD.getPlanClass().toString().toLowerCase());
        currentConfiguration.add(planId);
        planId=pbFeatureModel.getID(increaseUARTPeriodPD.getPlanClass().toString().toLowerCase());
        currentConfiguration.add(planId);
        planId=pbFeatureModel.getID(sleepSensorsPD.getPlanClass().toString().toLowerCase());
        currentConfiguration.add(planId);
        preferenceBasedEngine.setCurrentConfiguration(currentConfiguration);
        Log.d("cane","La configuración inicial es "+currentConfiguration.toString());
        // se inicia el servicio de auto-adaptación
        preferenceBasedEngine.start();


    }

    protected void updatePrecisionValue(Double value){
        preferenceBasedEngine.updateContext("precision",value);
        preferenceBasedEngine.updateWellnessFactor("precision",value);
        Log.d("cane", "La precisión actual es "+value);
        // se actualiza el valor awake
        if(batteryService.getPeriod()==6 && uartService.getPeriod()==5){
            Log.d("cane","Estamos awake");
            preferenceBasedEngine.updateContext("awake",true);
        }else{
            Log.d("cane","NO estamos awake");
            preferenceBasedEngine.updateContext("awake",false);
        }

    }

    protected void updateBatteryValue(Double value){
        Log.d("batt","Se actualiza el valor de la batería");
        preferenceBasedEngine.updateContext("battery",value);
        preferenceBasedEngine.updateWellnessFactor("battery",value);
    }

    private boolean validate(int strSample, int strBat) {
        String rateError = null;
        String batteryError = null;
        boolean validRate, validBat, validSystem;

        if(TextUtils.isEmpty(editTextSample.getText())) {
            rateError = getString(R.string.mandatory);
            validRate = false;
        } else if(strSample > 127){
            rateError = getString(R.string.maximun_val);
            validRate = false;
        } else {
            rateError = null;
            validRate = true;
        }
        toggleTextInputLayoutError(textInputSample, rateError);
        clearFocus();

        if(TextUtils.isEmpty(editTextBattery.getText())) {
            batteryError = getString(R.string.mandatory);
            validBat = false;
        } else if(strBat > 127){
            batteryError = getString(R.string.maximun_val);
            validBat = false;
        } else {
            validBat = true;
        }
        toggleTextInputLayoutError(textInputBattery, batteryError);

        if(!radioFSM.isChecked() && !radioMLP.isChecked()){
            radioMLP.setError("Selec Item");
            validSystem = false;
        } else {
            radioMLP.setError(null);
            validSystem = true;
        }
        clearFocus();

        if(validRate && validBat && validSystem){
            return true;
        } else {
            return false;
        }
    }

    private static void toggleTextInputLayoutError(@NonNull TextInputLayout textInputLayout,
                                                   String msg) {
        textInputLayout.setError(msg);
        if (msg == null) {
            textInputLayout.setErrorEnabled(false);
        } else {
            textInputLayout.setErrorEnabled(true);
        }
    }

    private void clearFocus() {
        View view = this.getCurrentFocus();
        if (view != null && view instanceof EditText) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context
                    .INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private ServiceConnection BattServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            battService = ((BatteryService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + battService);
            if (!battService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            battService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    protected void actionChangeNN(Boolean neuralNetwork){
        byte[] value = new byte[3];
        //habilitar red neuroanl. true -> habilitado
        //neuralNetwork = intent.getBooleanExtra(UartService.EXTRA_DATA_UART, neuralNetwork);
        Double previousValue = (Double)preferenceBasedEngine.getContext().getValue("precision");
        Double newValue;

        value[0] = (byte)(content_sample & 0xFF);
        value[1] = (byte)(content_bat & 0xFF);
        if(neuralNetwork){
            system_sel = 1;
            strSys = " MLP";
            newValue=previousValue+50;
        } else {
            system_sel = 0;
            strSys = " FSM";
            newValue=previousValue-50;
        }
        value[2] = system_sel;
        strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                " Hz | Battery rate: " + content_bat + " min\n";
        textv.append(strRate);
        mService.writeRXCharacteristic(value);
        updatePrecisionValue(newValue);

        try {
            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
            fOut.write(strRate.getBytes());
            fOut.close();
        }
        catch(IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            final String strData;
            final Intent mIntent = intent;
            //*********************//

            if (action.equals(UartService.ACTION_GATT_CONNECTED_UART)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String strLog = dateStr.format(new Date()) + " Connected to BLE\n";
                        try{
                            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                            fOut.write(strLog.getBytes());
                            fOut.close();
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                        textv.append(strLog);
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btn_connect.setText("Disconnect");
                        mState = UART_PROFILE_CONNECTED;
                        btn_send.setEnabled(true);
                        btn_debug.setEnabled(true);
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED_UART)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String strLog = dateStr.format(new Date()) + " Disconnected to BLE\n";
                        try{
                            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                            fOut.write(strLog.getBytes());
                            fOut.close();
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                        textv.append(strLog);
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btn_connect.setText("Connect");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        btn_send.setEnabled(false);
                        btn_debug.setEnabled(false);
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED_UART)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE_UART)) {
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA_UART);
                if(!debug) {
                    counter++;
                    final float medS = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 0, 2)).getShort() / 1000.0f;
                    final float medNS = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 2, 4)).getShort() / 1000.0f;
                    char varS = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 4, 6)).getChar();
                    char varNS = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 6, 8)).getChar();

                    float varS_f = varS / 100.0f;
                    float varNS_f = varNS / 100.0f;

                    double sdS = Math.floor(sqrt(varS_f) * 1000) / 1000;
                    double sdNS = Math.floor(sqrt(varNS_f) * 1000) / 1000;

                    strData = dateStr.format(new Date()) + " Activity " + counter + "\n" + "Step mean: " + medS + " s\n" + "NoStep mean: "
                            + medNS + " s\n" + "Step variance: " + varS_f + "ms\n" + "NoStep variance: " + varNS_f + "ms\n" +
                            "Step standard deviation: " + sdS + " ms\n" + "NoStep standard deviation: " + sdNS + " ms\n\n";
                } else {
                    final short d1 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 0, 2)).getShort();
                    final short d2 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 2, 4)).getShort();
                    final short d3 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 4, 6)).getShort();
                    final short d4 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 6, 8)).getShort();
                    final short d5 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 8, 10)).getShort();
                    final short d6 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 10, 12)).getShort();
                    final short d7 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 12, 14)).getShort();
                    final short d8 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 14, 16)).getShort();
                    final short d9 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 16, 18)).getShort();
                    final short d10 = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 18, 20)).getShort();

                    strData = d1+"\n"+d2+"\n"+d3+"\n"+d4+"\n"+d5+"\n"+d6+"\n"+d7+"\n"+d8+"\n"+d9+"\n"+d10+"\n";
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            textv.append(strData);
                            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                            fOut.write(strData.getBytes());
                            fOut.close();
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }

            if (action.equals(UartService.ACTION_CHANGE_NN)) {
                byte[] value = new byte[3];
                //habilitar red neuroanl. true -> habilitado
                neuralNetwork = intent.getBooleanExtra(UartService.EXTRA_DATA_UART, neuralNetwork);
                uartService.setNeuralNetwork(neuralNetwork);
                Double precisionWellness = (Double) preferenceBasedEngine.getContext().getValue("precision");
                Double newPrecision;
                value[0] = (byte) (content_sample & 0xFF);
                value[1] = (byte) (content_bat & 0xFF);
                if (neuralNetwork) {
                    system_sel = 1;
                    strSys = " MLP";
                    newPrecision = precisionWellness + 50;
                } else {
                    system_sel = 0;
                    strSys = " FSM";
                    newPrecision = precisionWellness - 50;
                }
                strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                        " Hz | Battery rate: " + content_bat + " min\n";
                textv.append(strRate);
                updatePrecisionValue(newPrecision);
                value[2] = system_sel;
                mService.writeRXCharacteristic(value);

                try {
                    FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                    fOut.write(strRate.getBytes());
                    fOut.close();
                }
                catch(IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
            }

            if (action.equals(UartService.ACTION_CHANGE_PERIOD)) {
                //cambiar periodo
                byte[] value = new byte[3];
                sampleDouble = intent.getDoubleExtra(UartService.EXTRA_DATA_UART, sampleDouble);
                uartService.setPeriod(sampleDouble.intValue());
                content_sample = sampleDouble.intValue();
                strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                        " Hz | Battery rate: " + content_bat + " min\n";
                textv.append(strRate);
                value[0] = (byte) (content_sample & 0xFF);
                value[1] = (byte) (content_bat & 0xFF);
                value[2] = system_sel;

                mService.writeRXCharacteristic(value);
                // act
                Double newPrecision;
                Double precisionWellness = (Double) preferenceBasedEngine.getContext().getValue("precision");
                if (sampleDouble.compareTo(5.0) == 0) {
                    newPrecision = precisionWellness + 25;
                } else {
                    newPrecision = precisionWellness - 25;
                }
                updatePrecisionValue(newPrecision);

                try {
                    FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                    fOut.write(strRate.getBytes());
                    fOut.close();
                }
                catch(IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
            }

            /*if (action.equals(UartService.ACTION_CHANGE_PRECISION)) {
                Double previousPeriod = new Double(uartService.getPeriod());
                Double value = intent.getDoubleExtra(UartService.EXTRA_DATA_UART, previousPeriod);
                uartPeriod=value.intValue();
                Double newPrecision;
                Double precisionWellness = (Double)preferenceBasedEngine.getContext().getValue("precision");
                if(value.compareTo(5.0)==0){
                    newPrecision=precisionWellness+25;
                }else{
                    newPrecision=precisionWellness-25;
                }
                updatePrecisionValue(newPrecision);
            }*/

            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    protected void actionChangeUartPeriod(Double newPeriod){
        // actualización de la prec ision
        Double precisionWellness = (Double)preferenceBasedEngine.getContext().getValue("precision");
        Double newPrecision;
        byte[] value=new byte[3];
        if(newPeriod.compareTo(5.0)==0){
            newPrecision=precisionWellness+25;
            content_sample = 35;
        }else{
            newPrecision=precisionWellness-25;
            content_sample = 17;
        }
        updatePrecisionValue(newPrecision);
        strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                " Hz | Battery rate: " + content_bat + " min\n";
        textv.append(strRate);

        // envío de valores a la UART
        value[0] = (byte) (content_sample & 0xFF);
        value[1] = (byte) (content_bat & 0xFF);
        value[2] = system_sel;
        mService.writeRXCharacteristic(value);

        try {
            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
            fOut.write(strRate.getBytes());
            fOut.close();
        }
        catch(IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private final BroadcastReceiver BatteryStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, Intent intent) {
            String action = intent.getAction();
            final String strBatt;
            final Intent mIntent = intent;
            //*********************//
            if (action.equals(BatteryService.ACTION_GATT_CONNECTED_BATT)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "BATT_CONNECT_MSG");
                        btn_connect.setText("Disconnect");
                        //mState = UART_PROFILE_CONNECTED;
                        btn_send.setEnabled(true);
                    }
                });
            }

            if (action.equals(BatteryService.ACTION_GATT_DISCONNECTED_BATT)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btn_connect.setText("Connect");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        btn_send.setEnabled(false);
                    }
                });
            }

            if (action.equals(BatteryService.ACTION_GATT_SERVICES_DISCOVERED_BATT)) {
                battService.enableBatteryNotification();
            }

            if (action.equals(BatteryService.ACTION_DATA_AVAILABLE_BATT)) {
                final String battPer;
                final byte[] txBatValue = intent.getByteArrayExtra(BatteryService.EXTRA_DATA_BATT);
                //Log.i("cane", "battery: " + txBatValue[0]);
                final int batValue = map(txBatValue[0],0, 100, 0, 100);
                strBatt = dateStr.format(new Date()) + " Battery level: " + batValue + "%\n";

                Log.d("batt","Se actualiza el valor de la batería");
                preferenceBasedEngine.updateContext("battery",new Double(batValue));
                preferenceBasedEngine.updateWellnessFactor("battery",new Double(batValue));
                battPer = batValue + "%";
                if(batValue > 0) {
                    batLevelBar.setProgress(batValue);
                    textBatt.setText(battPer);
                } else {
                    batLevelBar.setProgress(0);
                    textBatt.setText("0%");
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            FileOutputStream fOut = new FileOutputStream(file_bat, true);
                            fOut.write(strBatt.getBytes());
                            fOut.close();
                            preferenceBasedEngine.updateWellnessFactor("battery",new Double(batValue));
                            preferenceBasedEngine.updateContext("battery",new Double(batValue));
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });

            }

            if (action.equals(BatteryService.ACTION_CHANGE_PERIOD)) {
                byte[] value = new byte[3];
                Double previousPeriod = new Double(batteryService.getPeriod());
                Double period = intent.getDoubleExtra(BatteryService.EXTRA_DATA_BATT, sampleDouble); //conversiones y mando datos (uart)
                batteryService.setPeriod(period.intValue());
                Double precisionWellness = (Double) preferenceBasedEngine.getContext().getValue("precision");
                Double newPrecision;
                if(period.compareTo(11.0)==0){
                    newPrecision=precisionWellness-25;
                }else {
                    newPrecision=precisionWellness+25;
                }
                //batteryPeriod=period.intValue();
                updatePrecisionValue(newPrecision);
                // TODO: Completa el envío
                content_bat = period.intValue();
                strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                        " Hz | Battery rate: " + content_bat + " min\n";
                textv.append(strRate);
                value[0] = (byte) (content_sample & 0xFF);
                value[1] = (byte) (content_bat & 0xFF);
                value[2] = system_sel;
                mService.writeRXCharacteristic(value);

                try {
                    FileOutputStream fOut = new FileOutputStream(file_sensor, true);
                    fOut.write(strRate.getBytes());
                    fOut.close();
                }
                catch(IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
            }

            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    protected void actionChangeBatteryPeriod(Double period){
        byte[] value = new byte[3];
        Double precisionWellness = (Double) preferenceBasedEngine.getContext().getValue("precision");
        Double newPrecision;
        if(period.compareTo(11.0)==0){
            newPrecision=precisionWellness-25;
            content_bat = 5;
        }else {
            newPrecision=precisionWellness+25;
            content_bat = 1;
        }
        //batteryPeriod=period.intValue();
        updatePrecisionValue(newPrecision);

        strRate = dateStr.format(new Date()) + strSys + " - Sample rate: " + content_sample +
                " Hz | Battery rate: " + content_bat + " min\n";
        textv.append(strRate);
        value[0] = (byte) (content_sample & 0xFF);
        value[1] = (byte) (content_bat & 0xFF);
        value[2] = system_sel;
        mService.writeRXCharacteristic(value);

        try {
            FileOutputStream fOut = new FileOutputStream(file_sensor, true);
            fOut.write(strRate.getBytes());
            fOut.close();
        }
        catch(IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private int map(int x, int in_min, int in_max, int out_min, int out_max) {
        int temp = (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
        if (temp < out_min){
            return out_min;
        } else if(temp > out_max){
            return out_max;
        } else {
            return temp;
        }
    }

    private void service_init() {
        Intent UARTIntent = new Intent(this, UartService.class);
        bindService(UARTIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
        Intent BattIntent = new Intent(this, BatteryService.class);
        bindService(BattIntent, BattServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(BatteryStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED_UART);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED_UART);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED_UART);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE_UART);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(BatteryService.ACTION_GATT_CONNECTED_BATT);
        intentFilter.addAction(BatteryService.ACTION_GATT_DISCONNECTED_BATT);
        intentFilter.addAction(BatteryService.ACTION_GATT_SERVICES_DISCOVERED_BATT);
        intentFilter.addAction(BatteryService.ACTION_DATA_AVAILABLE_BATT);
        intentFilter.addAction(BatteryService.DEVICE_DOES_NOT_SUPPORT_BATT);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
        battService.stopSelf();
        battService = null;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (mTransitionsReceiver != null) {
            unregisterReceiver(mTransitionsReceiver);
            mTransitionsReceiver = null;
        }
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Transitions could not be unregistered: " + e);
                    }
                });
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        setupActivityTransitions();
        Log.d(TAG, "onResume");
        /*if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }*/
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
                    battService.connect(deviceAddress);
                    // servicio de auto-adaptación
                    selfAdaptation();
                    Toast.makeText(this, "Connect to BLE Cane", Toast.LENGTH_SHORT).show();
                } else {
                    btn_connect.setText("Connect");
                    Toast.makeText(this, "Imposible to connect. Try again", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }

    @Override
    public SAComponentInterface getComponent(String id) {
        SAComponentInterface res=null;
        if(id.equals("battery")){
            res=batteryService;
        }
        if(id.equals("uart"))res=uartService;
        return res;
    }

    /**
     * A basic BroadcastReceiver to handle intents from from the Transitions API.
     */
    public class TransitionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityTransitionResult.hasResult(intent)){
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                    String activity = toActivityString(event.getActivityType());
                    String transitionType = toTransitionType(event.getTransitionType());
                    Log.d("cane",activity+" "+transitionType);
                    if (event.getActivityType()==DetectedActivity.STILL){
                        if(event.getTransitionType()==ActivityTransition.ACTIVITY_TRANSITION_ENTER){
                            preferenceBasedEngine.updateContext("repose",true);
                        }else if(event.getTransitionType()==ActivityTransition.ACTIVITY_TRANSITION_EXIT){
                            preferenceBasedEngine.updateContext("repose",false);
                        }
                    }

                }
            }

        }
    }
}
