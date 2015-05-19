package com.snt.RemoteJoystick.ui;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
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

    int sequenceNumber;
    int brakeCommand;
    int remoteOn;
    int autoBrake;
    int brakeMode;
    int speedLimit;
    int speedLimitEnabled;
    int timeout;
    int scenario;
    long last_updated;

    private final Handler controlMessageHandler = new Handler();

    private SimpleAdapter listAdapter;

    private final int CONTROL_COMMAND = 101;
    private final int ENABLED = 1;
    private final int DISABLE = 0;
    private final int NO_SCENARIO = 0;
    private final int SPOOKY_SCENARIO = 1;
    private final int HARD_SPEED_LIMIT = 5;

    //This means if current value differs THROTTLE_FILTER_PERCENTAGE % to previous value
    //the new value will be sent to the car
    private int THROTTLE_FILTER_PERCENTAGE = 50;


    private ImageButton button_stop;
    private CheckBox checkbox_autobrake;
    private CheckBox checkBox_speedLimit;
    private NumberPicker numberPicker_speedlimit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        newStickValue = 0;
        sentStickValue = 0;
        brakeToggle = false;

        sequenceNumber = 0;
        brakeCommand = 0;
        remoteOn = 0;
        autoBrake = 1;
        speedLimit = HARD_SPEED_LIMIT;
        speedLimitEnabled = 0;
        timeout = 100;
        last_updated = -30000;

        scenario = NO_SCENARIO;

//        getActivity().setRequestedOrientation(
//                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);


        joystickView = inflater.inflate(R.layout.joystick_view, container, false);

        // Assign listener to widgets

        ToggleButton button_on_off = (ToggleButton) joystickView.findViewById(R.id.toggleButton);
        button_stop = (ImageButton) joystickView.findViewById(R.id.imageButtonStop);
        checkbox_autobrake = (CheckBox) joystickView.findViewById(R.id.checkBoxAutoBrake);
        checkBox_speedLimit = (CheckBox) joystickView.findViewById(R.id.checkBoxSpeedLimit);
        numberPicker_speedlimit = (NumberPicker) joystickView.findViewById(R.id.numberPicker);

        button_on_off.setOnCheckedChangeListener(onOffToggleListener);
        button_stop.setOnClickListener(this);
        checkbox_autobrake.setOnCheckedChangeListener(checkboxChangeLister);
        checkBox_speedLimit.setOnCheckedChangeListener(checkboxChangeLister);
        numberPicker_speedlimit.setOnValueChangedListener(numberPickerChangeListener);

        // Populate the numberpicker
        numberPicker_speedlimit.setMaxValue(80);
        numberPicker_speedlimit.setMinValue(0);
        numberPicker_speedlimit.setValue(HARD_SPEED_LIMIT);
        numberPicker_speedlimit.setWrapSelectorWheel(false);

        //Set states
        numberPicker_speedlimit.setEnabled(false);
        checkbox_autobrake.setChecked(true);
        checkbox_autobrake.setEnabled(false);
        checkBox_speedLimit.setChecked(true);
        checkBox_speedLimit.setEnabled(false);

        disc = (ImageView) joystickView.findViewById(R.id.imageView);
        disc.setOnTouchListener(joystickMotionListener);

        // Center the joystick

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

        // Populate the spinner with scenarios

        Spinner spinner = (Spinner) joystickView.findViewById(R.id.spinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                getResources().getStringArray(R.array.scenarios_array));
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(scenarioSelectedHandler);

        controlMessageHandler.postDelayed(controlMessageRunnable,1000);

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
        ((Map<String, String>)listAdapter.getItem(pos)).put("value",value);
    }

    /**
     * Defines the joystick value, and send the control message if it differs by
     * at least 80 points of percentage from the last value sent
     * @param value The new value
     */
    private void setStickValue(int value) {


        //Log.d("throttle", String.valueOf(value));
        newStickValue = value;
        /*
        if (Math.abs(newStickValue - sentStickValue) > THROTTLE_FILTER_PERCENTAGE
                || (newStickValue == 0 && sentStickValue != 0) ) {
            sendControlMessage();
        }
        */
        if(sentStickValue != 0 && newStickValue == 0){
            sendControlMessage();
        }

    }


    /**
     * This method stops the remote agent and resets everything to normal
     */
    protected void sendStopRemoteAgent(){
        newStickValue = 0;
        brakeCommand = 0;
        sequenceNumber = 0;
        autoBrake = 0;
        brakeMode = 0;
        remoteOn = 0;
        speedLimit = 0;
        speedLimitEnabled = 0;
        timeout = 0;
        scenario = 0;
        sendControlMessage();
    }

    /**
     * Formats and sends a message containing throttle and brake commands with a sequence number
     * The brakeCommand indicator values are 1: set brakes, 2: disable brakes, 0: no action
     */
    protected void sendControlMessage() {
        String msg = "101,0,0,0,0";
        /**
         * 101 command parameters in order are
         * Forward Throttle
         * Backward Throttle
         * Break Enable
         * Sequence Number
         * Brake Mode
         * Remote Agent Enable
         * SpeedLimit
         * SpeedLimit Enable
         * Timeout
         * Scenario
         */

        // Always set throttle to zero if brake is on
        if(brakeCommand == ENABLED){
            newStickValue = 0;
        }

        msg = String.format("101,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
                newStickValue > 0 ? newStickValue : 0,
                newStickValue < 0 ? Math.abs(newStickValue) : 0,
                (brakeCommand == ENABLED ? ENABLED : DISABLE),
                sequenceNumber,
                brakeMode,/*brake mode*/
                remoteOn,
                speedLimit,
                speedLimitEnabled,
                timeout,
                scenario);

        sequenceNumber = (sequenceNumber + 1) % 100;

        sendCommand("Control", msg, this);
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
    private final Runnable controlMessageRunnable = new Runnable() {
        public void run() {
            sendControlMessage();
            controlMessageHandler.postDelayed(this,2000); // periodicity in ms
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        //brakeCommand = 1;
        //sendControlMessage();
        sendStopRemoteAgent();
        // TODO: STAHP EVERYTHING
        // Another activity is taking focus (this activity is about to be "paused").

        getActivity().setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        cancelCommand();
    }

    @Override
    public void onStop() {
        super.onStop();
        //brakeCommand = 1;
        //sendControlMessage();
        // TODO: STAHP EVERYTHING
        // The activity is no longer visible (it is now "stopped")
        sendStopRemoteAgent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //brakeCommand = 1;
        //sendControlMessage();
        // TODO: STAHP EVERYTHING
        // The activity is about to be destroyed.
        sendStopRemoteAgent();
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
                        case R.id.numberPicker:
                            speedLimit = newVal;
                            break;
                    }
                }
            };

    /**
     * Listener for the checkboxes
     */
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

    /**
     * Listener for the scenario dropdown list
     */
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



    /**
     * Called when a view has been clicked
     * @param v The clicked view
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
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

    /**
     * Called when the joystick is touched
     */
    private View.OnTouchListener joystickMotionListener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) v.getLayoutParams();
                    int[] coord = new int [2];
                    joystickView.getLocationOnScreen(coord);
                    int yOffset = coord[1];
                    int correctedY = 0, relativeY = 0;
                    int maxHeight = joystickView.getHeight();
                    int radius = disc.getHeight()/2;

                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_MOVE:
                            relativeY = (int) event.getRawY() - yOffset;
                            correctedY = (int) (relativeY + radius > maxHeight * 0.9 ?
                                    maxHeight * 0.9 - radius : (relativeY - radius < maxHeight * 0.1 ?
                                    maxHeight * 0.1 + radius : relativeY));

                            params.topMargin = correctedY - radius;
                            setStickValue((int)((correctedY-maxHeight*0.1-radius)*200/(maxHeight*0.8-2*radius)-100)*-1);

                            disc.setLayoutParams(params);
                            break;

                        case MotionEvent.ACTION_UP:
                            params.topMargin = maxHeight/2 - radius;
                            disc.setLayoutParams(params);
                            setStickValue(0);
                            break;
                    }

                    return true;
                }
            };
}