package com.harryrickards.bioniscope;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.app.FragmentTransaction;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.app.ActionBar.OnNavigationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    // TODO Titles on graph axes
    // Increase bluetooth baud rate

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

    // Preferences
    SharedPreferences preferences;

    // Calibration constants
    // TODO Move these to be calibrated
    final static double DIGITAL_TIME_SAMPLE_OFFSET = 1.6;
    final static double ANALOGUE_TIME_SAMPLE_OFFSET = 0;

    // TODO Log only if development enabled

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
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        onConnectionConnecting();

        // Disconnect when disconnect button clicked
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If currently connected
                if (mCommandInterface != null) {
                    // Disconnect from Bluetooth
                    disconnectBluetooth();
                // Otherwise if currently disconnected
                } else {
                    // Connect to Bluetooth
                    setupBluetooth();
                }
            }
        });

        // Preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Show an info message about Bluetooth on the app's first run
        // Based on SO7562786
        boolean firstRun = preferences.getBoolean("firstRun", true);
        if (firstRun) {
            preferences.edit().putBoolean("firstRun", false).commit();
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
        onConnectionConnecting();

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
                onConnectionFailed();
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
        // Change status text to disconnected
        onConnectionDisconnected();
    }


    // Connect to Bluetooth device
    private void connectBluetooth() throws IOException {
        // Scan for bluetooth devices
        if (mBluetoothAdapter.isDiscovering()) {mBluetoothAdapter.cancelDiscovery();}
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
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.w("scope", "FOUND " + device.getName() + " " + device.getAddress());

                String wantedName = preferences.getString("pref_device_name", "HC-06");
                Log.w("scope", "SEARCHING FOR " + wantedName);

                if (device.getName().equals(wantedName)) {
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
                            onConnectionFailed();
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
            onConnectionFailed();
        }


        // TODO Send default values (before requesting sample)

        // Request a sample based on the mode we're currently in
        onRelevantSampleRequested();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Show button and set text
                onConnectionConnected();
            }
        });
    }

    protected void onConnectionFailed() {
        disconnectButton.setText(getString(R.string.connect));
        disconnectButton.setVisibility(View.VISIBLE);
        connectionStatus.setTextColor(Color.RED);
        connectionStatus.setText(getString(R.string.connection_failed));
    }

    protected void onConnectionConnecting() {
        disconnectButton.setVisibility(View.INVISIBLE);
        connectionStatus.setTextColor(Color.BLACK);
        connectionStatus.setText(getString(R.string.connecting));
    }

    protected void onConnectionDisconnected() {
        disconnectButton.setVisibility(View.VISIBLE);
        disconnectButton.setText(getString(R.string.connect));
        connectionStatus.setTextColor(Color.BLACK);
        connectionStatus.setText(getString(R.string.disconnected));
    }

    protected void onConnectionConnected() {
        disconnectButton.setVisibility(View.VISIBLE);
        disconnectButton.setText(getString(R.string.disconnect));
        connectionStatus.setTextColor(Color.parseColor("#ff669900")); // Holo green
        connectionStatus.setText(getString(R.string.connected));
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
        sampleGraph();

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
        sampleSpectrum();

        if (!(currentFragment == GRAPH_FRAGMENT || currentFragment == SPECTRUM_FRAGMENT)) {
            FragmentTransaction transaction2 = getFragmentManager().beginTransaction();
            transaction2.replace(R.id.controlsContainer, mControlsFragment);
            transaction2.addToBackStack(null);
            transaction2.commit();
        }
    }

    // Digital sample requested
    public void onDigitalSampleRequested() {
        if (mDigitalControlsFragment != null) {
            mDigitalControlsFragment.setSampleButtonEnabled(false);
        }

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
                        mDigitalControlsFragment.setSampleButtonEnabled(true);
                    }
                });
            }
        });
        runCommand(command);
    }

    // Analogue sample requested
    public void onSampleRequested() {
        // If we're in the right fragment
        if (currentFragment == GRAPH_FRAGMENT) {
            sampleGraph();
        } else {
            // Otherwise switch to it
            getActionBar().setSelectedNavigationItem(GRAPH_FRAGMENT);
        }
    }

    protected void sampleGraph() {
        if (mControlsFragment != null && mControlsFragment.traceOneEnabled()) {
            mControlsFragment.setSampleButtonOneEnabled(false);
            // Run command to get data from channel A
            runCommand(new Command((byte) 0x00, new byte[]{}, 1024, new CommandInterface.CommandCallback() {
                public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check to see if trace status has changed since we asked for data
                            if (mGraphFragment != null && mControlsFragment.traceOneEnabled()) {
                                mGraphFragment.setData(mData, false);
                            }
                            mControlsFragment.setSampleButtonOneEnabled(true);
                        }
                    });
                }
            }));
        }

        if (mControlsFragment != null && mControlsFragment.traceTwoEnabled()) {
            mControlsFragment.setSampleButtonTwoEnabled(false);
            // Run command to get data from channel B
            runCommand(new Command((byte) 0x01, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
                public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check to see if trace status has changed since we asked for data
                            if (mGraphFragment != null && mControlsFragment.traceTwoEnabled()) {
                                mGraphFragment.setData(mData, true);
                            }
                            mControlsFragment.setSampleButtonTwoEnabled(true);
                        }
                    });
                }
            }));
        }
    }

    // Spectrum sample requested
    public void onSpectrumSampleRequested() {
        // If we're in the right fragment
        if (currentFragment == SPECTRUM_FRAGMENT) {
            sampleSpectrum();
        } else {
            // Otherwise switch to spectrum fragment
            getActionBar().setSelectedNavigationItem(SPECTRUM_FRAGMENT);
        }
    }

    protected void sampleSpectrum() {
        if (mControlsFragment != null && mControlsFragment.traceOneEnabled()) {
            mControlsFragment.setSpectrumSampleButtonOneEnabled(false);
            // Run command to get data from channel A
            runCommand(new Command((byte) 0x00, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
                public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check to see if trace status has changed since we asked for data
                            if (mSpectrumFragment != null && mControlsFragment.traceOneEnabled()) {
                                mSpectrumFragment.setData(mData, false);
                            }
                            mControlsFragment.setSpectrumSampleButtonOneEnabled(true);
                        }
                    });
                }
            }));
        }

        if (mControlsFragment != null && mControlsFragment.traceTwoEnabled()) {
            mControlsFragment.setSpectrumSampleButtonTwoEnabled(false);
            // Run command to get data from channel B
            runCommand(new Command((byte) 0x01, new byte[] {}, 1024, new CommandInterface.CommandCallback() {
                public void commandFinished(byte[] data) {
                    final byte[] mData = data;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check to see if trace status has changed since we asked for data
                            if (mSpectrumFragment != null && mControlsFragment.traceTwoEnabled()) {
                                mSpectrumFragment.setData(mData, true);
                            }
                            mControlsFragment.setSpectrumSampleButtonTwoEnabled(true);
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
        if (mSpectrumFragment != null) {
            mSpectrumFragment.setTimeSample(timeSample);
        }

        Log.w("analogue time sample", Double.toString(timeSample));
        int timeDelay = (int) Math.floor(timeSample - ANALOGUE_TIME_SAMPLE_OFFSET);
        if (timeDelay < 0) { timeDelay = 0; }
        Log.w("analogue time delay", Integer.toString(timeDelay));

        // Set time
        byte[] commandData = new byte[] {(byte) ((byte)timeDelay>>8), (byte) ((byte)timeDelay&0xFF)};
        Log.w("analogue time delay bytes", CommandInterface.bytesToHex(commandData));
        Command command = new Command((byte) 0x11, commandData, 0, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                onRelevantAnalogueSampleRequested();
            }
        });
        runCommand(command);
    }

    protected void onRelevantAnalogueSampleRequested() {
        switch (currentFragment) {
            case GRAPH_FRAGMENT:
                onSampleRequested();
                break;

            case SPECTRUM_FRAGMENT:
                onSpectrumSampleRequested();
                break;
        }
    }

    protected void onRelevantSampleRequested() {
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
    }

    // TODO Toggle capturing the channels when we're not displaying them
    // We don't need to capture these here as we just call mControlsFragment.traceXEnabled() when
    // needed
    public void onTraceOneToggled(boolean value) {
        // If disabling, we need to stop showing the trace
        if (!value) {
            if (mGraphFragment != null) {
                mGraphFragment.hideTraceOne();
            }
            if (mSpectrumFragment != null) {
                mSpectrumFragment.hideTraceOne();
            }
        }
        onRelevantAnalogueSampleRequested();
    }
    public void onTraceTwoToggled(boolean value) {
        // If disabling, we need to stop showing the trace
        if (!value) {
            if (mGraphFragment != null) {
                mGraphFragment.hideTraceTwo();
            }
            if (mSpectrumFragment != null) {
                mSpectrumFragment.hideTraceTwo();
            }
        }
        onRelevantAnalogueSampleRequested();
    }

    public void onTraceOneVoltsDivChanged(double value) {
        if (mGraphFragment != null) {
            mGraphFragment.setVoltsRangeA(value);
        }

        // Calculate pot byte to send
        byte mByte = gainToPotValue(500e-3/value, false);
        Log.w("pot A byte", CommandInterface.bytesToHex(new byte[] {mByte}));

        runCommand(new Command((byte) 0x08, new byte[] {mByte}, 0, new CommandInterface.CommandCallback() {
            @Override
            public void commandFinished(byte[] data) {
            }
        }));
    }
    public void onTraceTwoVoltsDivChanged(double value) {
        if (mGraphFragment != null) {
            mGraphFragment.setVoltsRangeB(value);
        }

        // Calculate pot byte to send
        byte mByte = gainToPotValue(2.5/value, true);
        Log.w("pot B byte", CommandInterface.bytesToHex(new byte[] {mByte}));

        runCommand(new Command((byte) 0x08, new byte[] {mByte}, 0, new CommandInterface.CommandCallback() {
            @Override
            public void commandFinished(byte[] data) {
            }
        }));
    }

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
