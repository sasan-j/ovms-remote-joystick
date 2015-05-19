package com.snt.RemoteJoystick.ui;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.snt.RemoteJoystick.R;
import com.snt.RemoteJoystick.api.OnResultCommandListener;
import com.snt.RemoteJoystick.entities.CarData;

import com.snt.RemoteJoystick.utils.OVMSNotifications;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by julien on 06/02/15.
 */
public class JoystickFragment extends BaseFragment
        implements OnResultCommandListener, AdapterView.OnClickListener {
    View joystickView = null;
    ImageView disc = null;

    int startMargin = 100;

    int newStickValue;
    int sentStickValue;
    boolean brakeToggle;


    //user recently
    int actionDuration;
    int speedLimit;
    int speedLimitEnabled;
    int spookyEnabled;
    int noThrottleEnabled;


    int sequenceNumber;
    int brakeCommand;
    int remoteOn;
    int autoBrake;
    int brakeMode;
    int timeout;
    int scenario;
    long last_updated;

    private final Handler controlMessageHandler = new Handler();

    private SimpleAdapter listAdapter;

    private final int CONTROL_COMMAND = 101;
    private final int ENABLED = 1;
    private final int DISABLE = 0;
    private final int HARD_SPEED_LIMIT = 10;

    private final int CMD_CONTROL = 101;

    private final int SUB_CMD_RESET = 0;
    private final int SUB_CMD_SPOOKY = 1;
    private final int SUB_CMD_NO_THROTTLE = 2;
    private final int SUB_CMD_FORWARD = 3;
    private final int SUB_CMD_REVERSE = 4;
    private final int SUB_CMD_SPEED_LIMIT = 5;
    private final int SUB_CMD_GET_STATE = 6;


    private ImageButton button_stop;

    private ImageButton btnGoForward;
    private ImageButton btnGoReverse;

    private ImageButton btnLimitSpeed;
    private ImageButton btnSpookyMode;

    private ImageButton btnResetCar;
    private ImageButton btnNoThrottle;

    private NumberPicker pickerSpeedLimit;
    private NumberPicker pickerDuration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        actionDuration = 1;
        speedLimit = HARD_SPEED_LIMIT;
        speedLimitEnabled = DISABLE;
        spookyEnabled = DISABLE;
        noThrottleEnabled = DISABLE;

        remoteOn = 0;
        autoBrake = 1;
        timeout = 100;
        last_updated = -30000;

        //scenario = NO_SCENARIO;

//        getActivity().setRequestedOrientation(
//                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);


        joystickView = inflater.inflate(R.layout.joystick_view, container, false);

        // Assign listener to widgets

        ToggleButton button_on_off = (ToggleButton) joystickView.findViewById(R.id.toggleButton);
        button_stop = (ImageButton) joystickView.findViewById(R.id.imageButtonStop);



        //Get reference to buttons
        btnGoForward = (ImageButton)joystickView.findViewById(R.id.btn_forward);
        btnGoReverse = (ImageButton)joystickView.findViewById(R.id.btn_reverse);

        btnLimitSpeed = (ImageButton)joystickView.findViewById(R.id.btn_speed_limit);
        btnSpookyMode = (ImageButton) joystickView.findViewById(R.id.btn_spooky);

        btnResetCar = (ImageButton) joystickView.findViewById(R.id.btn_reset);
        btnNoThrottle = (ImageButton) joystickView.findViewById(R.id.btn_no_throttle);


        //Set onclick listeners for buttons
        btnGoForward.setOnClickListener(this);
        btnGoReverse.setOnClickListener(this);
        btnLimitSpeed.setOnClickListener(this);
        btnSpookyMode.setOnClickListener(this);
        btnResetCar.setOnClickListener(this);
        btnNoThrottle.setOnClickListener(this);


        //button_on_off.setOnCheckedChangeListener(onOffToggleListener);
        //button_stop.setOnClickListener(this);

        //Get reference to pickers
        pickerSpeedLimit = (NumberPicker) joystickView.findViewById(R.id.picker_speed);
        pickerDuration = (NumberPicker) joystickView.findViewById(R.id.picker_duration);

        //Set picker listeners
        pickerDuration.setOnValueChangedListener(numberPickerChangeListener);
        pickerSpeedLimit.setOnValueChangedListener(numberPickerChangeListener);

        // Populate the numberpicker
        pickerSpeedLimit.setMaxValue(80);
        pickerSpeedLimit.setMinValue(0);
        pickerSpeedLimit.setValue(HARD_SPEED_LIMIT);
        pickerSpeedLimit.setWrapSelectorWheel(false);

        pickerDuration.setMaxValue(50);
        pickerDuration.setMinValue(1);
        pickerDuration.setValue(1);
        pickerDuration.setWrapSelectorWheel(false);

        //Set states
        pickerSpeedLimit.setEnabled(true);
        pickerDuration.setEnabled(true);

        //disc = (ImageView) joystickView.findViewById(R.id.imageView);
        //disc.setOnTouchListener(joystickMotionListener);

        // Center the joystick

        /*
        ViewTreeObserver vto = joystickView.getViewTreeObserver();

        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) disc.getLayoutParams();
                params.topMargin = joystickView.getHeight()/2 - disc.getHeight()/2;
                disc.setLayoutParams(params);

                params = (FrameLayout.LayoutParams) joystickView.findViewById(R.id.imageView2).getLayoutParams();
                params.height = (int)(joystickView.getHeight() * 0.8);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    joystickView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    joystickView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
*/
        // Populate the spinner with scenarios
        /*
        Spinner spinner = (Spinner) joystickView.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.scenarios_array));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(scenarioSelectedHandler);
        */

        //controlMessageHandler.postDelayed(controlMessageRunnable,1000);

        // Populate listview with initial values

        ArrayList<Map<String, String>> list = buildData();
        String[] from = { "name", "value" };
        int[] to = { android.R.id.text1, android.R.id.text2 };

        listAdapter = new SimpleAdapter(getActivity(), list,
                android.R.layout.simple_list_item_2, from, to);
        ((ListView)joystickView.findViewById(R.id.listView)).setAdapter(listAdapter);


        // Inflate the layout for this fragment
        return joystickView;
    }

    /**
     * Called when car data is updated
     * @param carData Car data structure
     */
    @Override
    public void update(CarData carData) {

    }

    private void setListViewValue(int pos, String value) {
        ((Map<String, String>) listAdapter.getItem(pos)).put("value", value);
    }


    /**
     * This method disables Throttle pedal
     */
    protected void setNoThrottle(int noThrottle){


        String msg = String.format("%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_NO_THROTTLE,
                noThrottle);

        sendControlMessage(msg);
    }

    /**
     * This method stops the remote agent and resets everything to normal
     */
    protected void stopRemoteAgent(){


        String msg = String.format("%d,%d",
                CMD_CONTROL,
                SUB_CMD_RESET);

        sendControlMessage(msg);
    }

    /**
     * This method enable or disables spooky mode
     * @param enabled enable or disable
     * @param interval_duration spooky interval in each direction in seconds
     *
     */
    protected void setSpookyMode(int enabled, float interval_duration){

        int intervalCorrected = (int)(interval_duration*10);

        String msg = String.format("%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_SPOOKY,
                enabled,
                intervalCorrected);

        sendControlMessage(msg);
    }

    /**
     * This method enable or disables spooky mode
     * @param speed speed limit
     * @param intervalDuration spooky interval in each direction in seconds
     *
     */
    protected void goForward(int speed, float intervalDuration){

        String msg = String.format("%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_FORWARD,
                validateSpeed(speed),
                correctTimeInterval(intervalDuration));

        sendControlMessage(msg);
    }


    /**
     * This method enable or disables spooky mode
     * @param speed limit
     * @param intervalDuration spooky interval in each direction in seconds
     *
     */
    protected void goReverse(int speed, float intervalDuration){

        String msg = String.format("%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_REVERSE,
                validateSpeed(speed),
                correctTimeInterval(intervalDuration));

        sendControlMessage(msg);
    }

    /**
     * This method enable or disables spooky mode
     * @param enabled limit
     * @param speedLimit spooky interval in each direction in seconds
     *
     */
    protected void setSpeedLimit(int enabled, int speedLimit){

        String msg = String.format("%d,%d,%d,%d",
                CMD_CONTROL,
                SUB_CMD_SPEED_LIMIT,
                enabled,
                validateSpeed(speedLimit));

        sendControlMessage(msg);
    }


    /**
     * This method asks for car status
     */
    protected void askForStatusUpdate(){

        String msg = String.format("%d,%d",
                CMD_CONTROL,
                SUB_CMD_GET_STATE);

        sendControlMessage(msg);
    }

    /**
     * Formats and sends a message containing throttle and brake commands with a sequence number
     * The brakeCommand indicator values are 1: set brakes, 2: disable brakes, 0: no action
     */
    protected void sendControlMessage(String msg) {

        sendCommand("Control", msg, this);

        /*
        sentStickValue = newStickValue;

        long delta = (System.currentTimeMillis()-last_updated)/1000;
        if (delta > 20) {
            ((ImageView)joystickView.findViewById(R.id.image_connection)).setImageResource(R.drawable.connection_unknown);
        }
        else if (delta > 5) {
            ((ImageView)joystickView.findViewById(R.id.image_connection)).setImageResource(R.drawable.connection_bad);
        }
        else {
            ((ImageView)joystickView.findViewById(R.id.image_connection)).setImageResource(R.drawable.connection_good);
        }
        */

    }

    /**
     * Formats and sends a message containing throttle and brake commands with a sequence number
     * The brakeCommand indicator values are 1: set brakes, 2: disable brakes, 0: no action
     */
    protected int correctTimeInterval(float intervalSeconds) {

        return (int)(intervalSeconds*1);

    }


    /**
     * Set max speed to hard speed limit or speed if it's lower
     */
    protected int validateSpeed(int speed) {

        if(speed>=HARD_SPEED_LIMIT)
            return HARD_SPEED_LIMIT;
        else
            return speed;
    }

    /**
     * Called when a command message acknowledgment is received
     * @param result An string array built from the comma separated values in the ack message
     */
    @Override
    public void onResultCommand(String[] result) {


        /*"MP-0 c101,
        brake_state,
        car_speed,
        motor_speed,
        State of charge,
        config_on,
        seq_number,
        ControlOk"
         */

        last_updated = System.currentTimeMillis();

        if (result.length <= 1)
            return;

        int command = Integer.parseInt(result[0]);
        int resCode = Integer.parseInt(result[1]);

        String cmdMessage = getSentCommandMessage(result[0]);

        if (command == CONTROL_COMMAND && result.length > 3 ) {
            if (result[1].equals("0")) { // car is not braking
                ((ImageView)joystickView.findViewById(R.id.image_brake)).setImageResource(R.drawable.brake_gray);
                //brakeToggle = false;
                //brakeCommand = ;
            } else {
                ((ImageView)joystickView.findViewById(R.id.image_brake)).setImageResource(R.drawable.brake);
            }
            /*
            else if (result[1].equals("2")) { // car is emergency braking
                ((ImageButton)joystickView.findViewById(R.id.imageButtonStop)).setImageResource(R.drawable.ic_restart);
                brakeToggle = true;
                brakeCommand = ((brakeCommand == 1) ? 0 : brakeCommand);
            }
            */

            setListViewValue(0,result[2] + " km/h");
            setListViewValue(1,result[3] + " rpm");
            setListViewValue(2,result[4] + " %");

            listAdapter.notifyDataSetChanged(); // notify the value change to the list

            //Config on
            if (result[5].equals("1")){
                ((ToggleButton)joystickView.findViewById(R.id.toggleButton))
                        .setTextColor(Color.GREEN);
            }
            else {
                ((ToggleButton)joystickView.findViewById(R.id.toggleButton))
                        .setTextColor(Color.RED);
            }


            // TODO : use result
        }

        else {

            switch (resCode) {
                case 0: // ok
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.msg_ok),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 1: // failed
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_failed, result[2]),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 2: // unsupported
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unsupported_operation),
                            Toast.LENGTH_SHORT).show();
                    break;
                case 3: // unimplemented
                    Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unimplemented_operation),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }

//        cancelCommand();
    }

    /**
     * Thread sending a control message periodically
     */
    /*
    private final Runnable controlMessageRunnable = new Runnable() {
        public void run() {
            sendControlMessage();
            controlMessageHandler.postDelayed(this,2000); // periodicity in ms
        }
    };
    */

    @Override
    public void onPause() {
        super.onPause();
        stopRemoteAgent();
        // TODO: STAHP EVERYTHING
        // Another activity is taking focus (this activity is about to be "paused").

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        cancelCommand();
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO: STAHP EVERYTHING
        // The activity is no longer visible (it is now "stopped")
        stopRemoteAgent();
    }


    @Override
    public void onResume() {
        super.onResume();

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

    }

    /**
     * Creates an ArrayList of various car values to display on two rows
     * @return ArrayList
     */
    private ArrayList<Map<String, String>> buildData() {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list.add(putData("Speed", "?"));
        list.add(putData("Rot. speed", "?"));
        list.add(putData("Battery", "?"));
        return list;
    }

    /**
     * Create a HashMap object to insert in an ArrayList
     * @param name First row of the list object
     * @param purpose Second row of the list object
     * @return The list object created
     */
    private HashMap<String, String> putData(String name, String purpose) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("name", name);
        item.put("value", purpose);
        return item;
    }


    /**
     * Listener for the speed limit number picker
     */
    private NumberPicker.OnValueChangeListener numberPickerChangeListener =
            new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    switch (picker.getId()) {
                        case R.id.picker_speed:
                            speedLimit = newVal;
                            break;
                        case R.id.picker_duration:
                            actionDuration = newVal;
                            break;
                    }
                }
            };

    /**
     * Listener for the checkboxes
     */
    /*
    private CompoundButton.OnCheckedChangeListener checkboxChangeLister =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    switch (buttonView.getId()) {
                        case R.id.checkBoxAutoBrake:
                            autoBrake = isChecked ? 1 : 0;
                            break;
                        case R.id.checkBoxSpeedLimit:
                            speedLimitEnabled = isChecked ? 1 : 0;
                            break;
                    }
                }
            };
    */

    /**
     * Listener for the scenario dropdown list
     */
    /*
    private AdapterView.OnItemSelectedListener scenarioSelectedHandler =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    // TODO: Do the thing
                    switch (i) {
                        case NO_SCENARIO: // Joystick
                            disc.setEnabled(true);
                            scenario = NO_SCENARIO;
                            break;
                        case SPOOKY_SCENARIO: // Spooky
                            disc.setEnabled(false);
                            scenario = SPOOKY_SCENARIO;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // TODO: STAHP TEH CAR OMG
                }
            };

    */

    /**
     * Called when a view has been clicked
     * @param v The clicked view
     */
    @Override
    public void onClick(View v) {
        actionDuration = pickerDuration.getValue();
        speedLimit = pickerSpeedLimit.getValue();

        switch (v.getId()) {
            /*
            case R.id.imageButtonStop:
                if (scenario != NO_SCENARIO) {
                    scenario = NO_SCENARIO;
                    brakeCommand = ENABLED;
                    ((Spinner) joystickView.findViewById(R.id.spinner)).setSelection(0);
                    //sendControlMessage(); // enable brakes
                }
                else {
                    //TODO: change the image
                    if (brakeCommand == ENABLED) {
                        brakeCommand = DISABLE;

                        button_stop.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop));
                        //sendControlMessage(); // disable brakes
                    } else {
                        brakeCommand = ENABLED;

                        button_stop.setImageDrawable(getResources().getDrawable(R.drawable.ic_stop_gray));
                        //sendControlMessage(); // enable brakes
                    }
                }
                break;
*/
            case R.id.btn_forward:
                goForward(pickerSpeedLimit.getValue(), actionDuration);
                break;
            case R.id.btn_reverse:
                goReverse(pickerSpeedLimit.getValue(), actionDuration);
                break;
            case R.id.btn_speed_limit:
                if(speedLimitEnabled == ENABLED) {
                    setSpeedLimit(DISABLE, 0);
                    speedLimitEnabled = DISABLE;
                }
                else{
                    setSpeedLimit(ENABLED, speedLimit);
                    speedLimitEnabled = ENABLED;
                }
                break;
            case R.id.btn_spooky:
                if(spookyEnabled == DISABLE){
                    setSpookyMode(ENABLED, actionDuration);
                    spookyEnabled = ENABLED;
                } else {
                    setSpookyMode(DISABLE, actionDuration);
                    spookyEnabled = DISABLE;
                }
                break;
            case R.id.btn_reset:
                stopRemoteAgent();
                spookyEnabled = DISABLE;
                speedLimitEnabled = DISABLE;

                break;
            case R.id.btn_no_throttle:
                if(noThrottleEnabled==DISABLE) {
                    setNoThrottle(ENABLED);
                    noThrottleEnabled = ENABLED;
                }
                break;

        }
    }


    //
    private CompoundButton.OnCheckedChangeListener onOffToggleListener =
            new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        remoteOn = ENABLED;
                        // The toggle is enabled
                    } else {
                        remoteOn = DISABLE;
                        // The toggle is disabled
                    }
                    //sendControlMessage();
                }
            };

}