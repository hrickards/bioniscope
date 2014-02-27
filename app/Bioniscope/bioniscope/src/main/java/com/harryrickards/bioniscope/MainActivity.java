package com.harryrickards.bioniscope;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.app.FragmentTransaction;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends Activity implements OnNavigationListener,
        ControlsFragment.OnControlChangedListener,
        DigitalControlsFragment.OnDigitalControlChangedListener,
        DigitalFragment.OnDigitalActionInterface {

    // TODO All scales on graphs

    // Bluetooth
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mDevice;
    CommandInterface mCommandInterface;
    private static final int REQUEST_ENABLE_BT = 1; // Request code returned from enabling BT
    OutputStream mOutputStream;
    InputStream mInputStream;
    BluetoothSocket mSocket;

    // Connection status
    TextView connectionStatus;
    Button disconnectButton;

    // Fragments
    GraphFragment mGraphFragment;
    ControlsFragment mControlsFragment;
    DigitalFragment mDigitalFragment;
    DigitalControlsFragment mDigitalControlsFragment;
    SpectrumFragment mSpectrumFragment;
    static final int GRAPH_FRAGMENT = 0;
    static final int SPECTRUM_FRAGMENT = 1;
    static final int DIGITAL_FRAGMENT = 2;
    int currentFragment = -1;

    // Calibration constants
    final static double DIGITAL_TIME_SAMPLE_OFFSET = 1.6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup dropdown navigation in the action bar
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(
                actionBar.getThemedContext(), R.array.dropdownOptions,
                android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);

        // Show connection status to user
        connectionStatus = (TextView) findViewById(R.id.connectionStatus);
        connectionStatus.setText(getString(R.string.connecting));

        // Disconnect when disconnect button clicked
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If currently connected
                if (mCommandInterface != null) {
                    // Disconnect from Bluetooth
                    disconnectBluetooth();
                // Otherwise if currently disconnected
                } else {
                    // Hide button while connecting
                    disconnectButton.setVisibility(View.INVISIBLE);
                    // Connect to Bluetooth
                    setupBluetooth();
                }
            }
        });


        // Show an info message about Bluetooth on the app's first run
        // Based on SO7562786
        boolean firstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean("firstRun", true);
        if (firstRun) {
            getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstRun", false)
                    .commit();
        }

        // Check device has bluetooth and get bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), getString(R.string.bluetooth_required),
                    Toast.LENGTH_LONG).show();
            // Go back to home screen
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else if (firstRun) {
            // Show the dialog asking the user to enable bluetooth before setting up bluetooth
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.first_run_title))
                    .setMessage(getString(R.string.first_run_body))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Called when okay pressed
                            setupBluetooth();
                        }
                    }).show();
        } else {
            // Setup bluetooth connection straight away
            setupBluetooth();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }

    private  void setupBluetooth() {
        // Set status text to connecting
        connectionStatus.setText(getString(R.string.connecting));

        // Enable bluetooth if not enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            // Connect to bluetooth device in a background thread
            new ConnectBluetoothTask().execute();
        }
    }

    private class ConnectBluetoothTask extends AsyncTask<Void, Void, Void> {
        private Exception e = null;

        @Override
        protected Void doInBackground(Void... args) {
            try {
                connectBluetooth();
            } catch (IOException mE) {
                e = mE;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void args) {
            if (e != null) {
                // Show button
                disconnectButton.setVisibility(View.VISIBLE);
                connectionStatus.setText(getString(R.string.connection_failed));
                disconnectButton.setText(getString(R.string.connect));
            }
        }
    }

    // Disconnect from Bluetooth
    private void disconnectBluetooth() {
        // Set command interface to null (used to check if connected)
        mCommandInterface = null;

        // Close streams
        if (mInputStream != null) {
            try {mInputStream.close();} catch (IOException e) { e.printStackTrace(); }
            mInputStream = null;
        }
        if (mOutputStream != null) {
            try {mOutputStream.close();} catch (IOException e) { e.printStackTrace(); }
            mOutputStream = null;
        }

        // Close socket
        if (mSocket != null) {
            try {mSocket.close();} catch (IOException e) { e.printStackTrace(); }
            mSocket = null;
        }

        // Make device null
        mDevice = null;

        // Change disconnect button text to connect
        disconnectButton.setText(getString(R.string.connect));
        // Change status text to disconnected
        connectionStatus.setText(getString(R.string.disconnected));
    }


    // Connect to Bluetooth device
    private void connectBluetooth() throws IOException {
        // Scan for bluetooth devices
        mBluetoothAdapter.startDiscovery();
        BluetoothReceiver mReceiver = new BluetoothReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    public class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the device that's been found and check if it's the HC06
                // TODO Allow user to set name to check against
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.w("scope", "FOUND " + device.getName() + " " + device.getAddress());
                if (device.getName().equals("HC-06")) {
                    // So we know we've found something
                    mDevice = device;

                    // Stop discovery
                    mBluetoothAdapter.cancelDiscovery();

                    // Connect to the device
                    connectToDevice();
                }
                // Called if discovery fails or when we stop it when we succeed
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (mDevice == null) {
                    // Couldn't find the device
                    Log.w("scope", "no HC06 found");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            disconnectButton.setVisibility(View.VISIBLE);
                            connectionStatus.setText(getString(R.string.connection_failed));
                            disconnectButton.setText(getString(R.string.connect));
                        }
                    });
                }
            }
        }
    }

    public void connectToDevice() {
        try {
            // Setup serial communication with device
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Serial UUID
            mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
            mSocket.connect();

            mOutputStream = mSocket.getOutputStream();
            mInputStream = mSocket.getInputStream();

            // Setup new CommandInterface to start communicating with device
            mCommandInterface = new CommandInterface(mOutputStream, mInputStream);
        } catch (IOException e) {
            Log.w("scope", "bluetooth IO exception");
            disconnectButton.setVisibility(View.VISIBLE);
            connectionStatus.setText(getString(R.string.connection_failed));
            disconnectButton.setText(getString(R.string.connect));
        }


        // TODO Send default values (before requesting sample)

        // Request a sample based on the mode we're currently in
        switch (currentFragment) {
            case SPECTRUM_FRAGMENT:
                onSpectrumSampleRequested();
                break;

            case DIGITAL_FRAGMENT:
                onDigitalSampleRequested();
                break;

            // GRAPH_FRAGMENT
            default:
                onSampleRequested();
                break;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Show button
                disconnectButton.setVisibility(View.VISIBLE);
                // No exceptions and device found
                connectionStatus.setText(getString(R.string.connected));
                disconnectButton.setText(getString(R.string.disconnect));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If we've just returned from enabling bluetooth, run the BT setup process again
        if (requestCode == REQUEST_ENABLE_BT) {
            setupBluetooth();
        }
    }

    // Run a command
    private void runCommand(Command command) {
        // If command interface has not been initialised yet, silently drop command
        if (mCommandInterface != null) {
            mCommandInterface.runCommand(command);
            Log.w("SENT", CommandInterface.bytesToHex(new byte[] {command.command})+","+CommandInterface.bytesToHex(command.outData));
        } else {
            Log.w("DROPPED", "dropped command as mCommandInterface null");
            Log.w("DROPPED", CommandInterface.bytesToHex(new byte[] {command.command})+","+CommandInterface.bytesToHex(command.outData));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Called when dropdown navigation pressed
    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        switch(position) {
            case GRAPH_FRAGMENT:    switchToGraph();
                                    break;
            case SPECTRUM_FRAGMENT: switchToSpectrum();
                                    break;
            case DIGITAL_FRAGMENT:  switchToDigital();
                                    break;
            default:                break;
        }
        // Set after the switchToX call
        currentFragment = position;
        return false;
    }

    // Save controls permanently & in current app instance (persistent across rotation)
    // Based on SO#151777
    /*
    @Override
    protected void onPause() {
        super.onPause();

        // Store in SharedPreferences
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Editor editor = preferences.edit();

        editor.putBoolean(TRACE_ONE_ENABLED, traceOneEnabled);
        editor.putBoolean(TRACE_TWO_ENABLED, traceTwoEnabled);
        putDouble(editor, TRACE_ONE_VOLTS_DIV, traceOneVoltsDiv);
        putDouble(editor, TRACE_TWO_VOLTS_DIV, traceTwoVoltsDiv);
        putDouble(editor, TIME_DIV, timeDiv);

        editor.commit();
    }


    // SharedPreferences doesn't support doubles by default, hence these workarounds
    // Copied from copolli @ SO 16319237
    Editor putDouble(final Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    // Retrieve controls saved in onPause
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        traceOneEnabled = preferences.getBoolean(TRACE_ONE_ENABLED, true);
        traceTwoEnabled = preferences.getBoolean(TRACE_TWO_ENABLED, true);
        traceOneVoltsDiv = getDouble(preferences, TRACE_ONE_VOLTS_DIV, 1.0);
        traceTwoVoltsDiv = getDouble(preferences, TRACE_TWO_VOLTS_DIV, 2.0);
        timeDiv = getDouble(preferences, TIME_DIV, 1.0);

        // setControls();
    }

    // Interface methods for when controls are changed
    public void onTraceOneToggled(boolean enabled) {
        traceOneEnabled = enabled;
    }
    public void onTraceTwoToggled(boolean enabled) {
        traceTwoEnabled = enabled;
    }
    public void onTraceOneVoltsDivChanged(double value) {
        traceOneVoltsDiv = value;
        // Update y bounds of graph
        // updateGraphYBounds();
    }
    public void onTraceTwoVoltsDivChanged(double value) {
        traceTwoVoltsDiv = value;
        // Update y bounds of graph
        // updateGraphYBounds();
    }
    public void onTimeDivChanged(double value) {
        timeDiv = value;
    }
    */

    //public void updateGraphYBounds() {
        // Use the maximum volts/div out of traces one and two
        //double voltsDiv = (traceOneVoltsDiv > traceTwoVoltsDiv) ? traceOneVoltsDiv : traceTwoVoltsDiv;
       // if (mGraphFragment != null && currentFragment == GRAPH_FRAGMENT) {
            //mGraphFragment.setYBounds(-voltsDiv*10, voltsDiv*10);
       // }
    //}

    // Set values of controls
    /*public void setControls() {
        ControlsFragment controlsFragment = (ControlsFragment)
                getSupportFragmentManager().findFragmentById(R.id.controlsFragment);
        controlsFragment.setTraceOneEnabled(traceOneEnabled);
        controlsFragment.setTraceTwoEnabled(traceTwoEnabled);
        controlsFragment.setTraceOneVoltsDiv(traceOneVoltsDiv);
        controlsFragment.setTraceTwoVoltsDiv(traceTwoVoltsDiv);
        controlsFragment.setTimeDiv(timeDiv);
        updateGraphYBounds();
    }*/

    // Switch to graph view
    public void switchToGraph() {
        if (mGraphFragment == null) {
            mGraphFragment = new GraphFragment();
        }
        if (mControlsFragment == null) {
            mControlsFragment = new ControlsFragment();
        }

        FragmentTransaction transaction1 = getFragmentManager().beginTransaction();
        transaction1.replace(R.id.fragmentContainer, mGraphFragment);
        transaction1.addToBackStack(null);
        transaction1.commit();

        // Sample
        onSampleRequested();

        if (!(currentFragment == GRAPH_FRAGMENT || currentFragment == SPECTRUM_FRAGMENT)) {
            FragmentTransaction transaction2 = getFragmentManager().beginTransaction();
            transaction2.replace(R.id.controlsContainer, mControlsFragment);
            transaction2.addToBackStack(null);
            transaction2.commit();
        }
    }

    // Switch to digital (basic logic analyser) view
    public void switchToDigital() {
        if (mDigitalFragment == null) {
            mDigitalFragment = new DigitalFragment();
        }
        if (mDigitalControlsFragment == null) {
            mDigitalControlsFragment = new DigitalControlsFragment();
        }

        // Sample
        onDigitalSampleRequested();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, mDigitalFragment);
        transaction.replace(R.id.controlsContainer, mDigitalControlsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Switch to spectrum (FFT) view
    public void switchToSpectrum() {
        if (mSpectrumFragment == null) {
            mSpectrumFragment = new SpectrumFragment();
            Log.w("scope", "new spec fragment");
        }
        if (mControlsFragment == null) {
            mControlsFragment = new ControlsFragment();
        }

        FragmentTransaction transaction1 = getFragmentManager().beginTransaction();
        transaction1.replace(R.id.fragmentContainer, mSpectrumFragment);
        transaction1.addToBackStack(null);
        transaction1.commit();

        // Sample
        onSpectrumSampleRequested();

        if (!(currentFragment == GRAPH_FRAGMENT || currentFragment == SPECTRUM_FRAGMENT)) {
            FragmentTransaction transaction2 = getFragmentManager().beginTransaction();
            transaction2.replace(R.id.controlsContainer, mControlsFragment);
            transaction2.addToBackStack(null);
            transaction2.commit();
        }
    }

    // Digital sample requested
    public void onDigitalSampleRequested() {
        // Run command to get data
        Command command = new Command((byte) 0x02, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                final byte[] mData = data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mDigitalFragment != null) {
                            mDigitalFragment.setData(mData);
                        }
                    }
                });
            }
        });
        runCommand(command);
    }

    // Analogue sample requested
    public void onSampleRequested() {
        if (mControlsFragment != null && mControlsFragment.traceOneEnabled()) {
            // Run command to get data from channel A
            runCommand(new Command((byte) 0x00, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            if (mGraphFragment != null) {
                                mGraphFragment.setData(mData, false);
                            }
                        }
                });
                }
        }));
        }

        if (mControlsFragment != null && mControlsFragment.traceTwoEnabled()) {
            // Run command to get data from channel B
            runCommand(new Command((byte) 0x01, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            if (mGraphFragment != null) {
                                mGraphFragment.setData(mData, true);
                            }
                        }
                });
                }
        }));
        }
    }

    // Spectrum sample requested
    public void onSpectrumSampleRequested() {
        if (mControlsFragment != null && mControlsFragment.traceOneEnabled()) {
            // Run command to get data from channel A
            runCommand(new Command((byte) 0x00, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                            if (mSpectrumFragment != null) {
                                mSpectrumFragment.setData(mData, false);
                            }
                        }
                });
                }
        }));
        }

        if (mControlsFragment != null && mControlsFragment.traceTwoEnabled()) {
            // Run command to get data from channel B
            runCommand(new Command((byte) 0x01, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                final byte[] mData = data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mSpectrumFragment != null) {
                            mSpectrumFragment.setData(mData, true);
                        }
                    }
                });
                }
        }));
        }
    }

    public void onDigitalTimeSampleChanged(double timeSample) {
        if (mDigitalFragment != null) {
            mDigitalFragment.setTimeSample(timeSample);
        }

        Log.w("digital time sample", Double.toString(timeSample));
        int timeDelay = (int) Math.floor(timeSample - DIGITAL_TIME_SAMPLE_OFFSET);
        if (timeDelay < 0) { timeDelay = 0; }
        Log.w("digital time delay", Integer.toString(timeDelay));

        // Set time
        byte[] commandData = new byte[] {(byte) ((byte)timeDelay>>8), (byte) ((byte)timeDelay&0xFF)};
        Log.w("digital time delay bytes", CommandInterface.bytesToHex(commandData));
        Command command = new Command((byte) 0x06, commandData, 0, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                onDigitalSampleRequested();
            }
        });
        runCommand(command);
    }

    public void onTimeSampleChanged(double timeSample) {
        if (mGraphFragment != null) {
            mGraphFragment.setTimeSample(timeSample);
        }

        Log.w("analogue time sample", Double.toString(timeSample));
        int timeDelay = (int) Math.floor(timeSample);
        if (timeDelay < 0) { timeDelay = 0; }
        Log.w("analogue time delay", Integer.toString(timeDelay));

        // Set time
        byte[] commandData = new byte[] {(byte) ((byte)timeDelay>>8), (byte) ((byte)timeDelay&0xFF)};
        Log.w("analogue time delay bytes", CommandInterface.bytesToHex(commandData));
        Command command = new Command((byte) 0x11, commandData, 0, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                onSampleRequested();
            }
        });
        runCommand(command);
    }

    protected void onRelevantAnalogSampleRequested() {
        switch (currentFragment) {
            case GRAPH_FRAGMENT:
                onSampleRequested();
                break;

            case SPECTRUM_FRAGMENT:
                onSpectrumSampleRequested();
                break;
        }
    }

    // TODO Toggle capturing the channels when we're not displaying them
    // We don't need to capture these here as we just call mControlsFragment.traceXEnabled() when
    // needed
    public void onTraceOneToggled(boolean value) {
        Log.w("trace one toggled", Boolean.toString(value));
        onRelevantAnalogSampleRequested();
        if (mGraphFragment != null) {
            mGraphFragment.setTraceOneVisibility(value);
        }
        if (mSpectrumFragment != null) {
            mSpectrumFragment.setTraceOneVisibility(value);
        }
    };
    public void onTraceTwoToggled(boolean value) {
        Log.w("trace two toggled", Boolean.toString(value));
        onRelevantAnalogSampleRequested();
        if (mGraphFragment != null) {
            mGraphFragment.setTraceTwoVisibility(value);
        }
        if (mSpectrumFragment != null) {
            mSpectrumFragment.setTraceTwoVisibility(value);
        }
    };

    public void onTraceOneVoltsDivChanged(double value) {
        // Calculate pot byte to send
        byte mByte = gainToPotValue(500e-3/value, false);
        Log.w("pot A byte", CommandInterface.bytesToHex(new byte[] {mByte}));

        runCommand(new Command((byte) 0x08, new byte[] {mByte}, 0, new CommandInterface.CommandCallback() {
            @Override
            public void commandFinished(byte[] data) {
            }
        }));
    };
    public void onTraceTwoVoltsDivChanged(double value) {
        // Calculate pot byte to send
        byte mByte = gainToPotValue(500e-3/value, true);
        Log.w("pot B byte", CommandInterface.bytesToHex(new byte[] {mByte}));

        runCommand(new Command((byte) 0x08, new byte[] {mByte}, 0, new CommandInterface.CommandCallback() {
            @Override
            public void commandFinished(byte[] data) {
            }
        }));
    };

    // Convert an amplifier gain and address to a pot value to send
    public byte gainToPotValue(double gain, boolean address) {
        // Av = 5e3/R
        double resistance = 5E3/gain;

        // Resistance varies linearly between 175 and 9.5k
        int value = (int) (127 - (resistance-175)/(9.5e3-175)*127);
        // We need a value between 127 and 0
        if (value < 0 || value > 127) { value = 0; }

        byte mByte = (byte) value;

        // Set MSB based on address
        if (address) {
            mByte |= 0x80;
        } else {
            mByte &= 0x7F;
        }

        return mByte;
    }
}
