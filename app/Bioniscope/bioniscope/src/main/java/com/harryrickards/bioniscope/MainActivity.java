package com.harryrickards.bioniscope;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.OnNavigationListener;
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

public class MainActivity extends ActionBarActivity implements OnNavigationListener,
        ControlsFragment.OnControlChangedListener,
        DigitalControlsFragment.OnDigitalControlChangedListener,
        DigitalFragment.OnDigitalActionInterface,
        SpectrumControlsFragment.OnSpectrumControlChangedListener {

    // Bluetooth
    BluetoothAdapter mBluetoothAdapter;
    CommandInterface mCommandInterface;
    private static final int REQUEST_ENABLE_BT = 1; // Request code returned from enabling BT
    OutputStream mOutputStream;
    InputStream mInputStream;
    BluetoothSocket mSocket;

    boolean traceOneEnabled;
    boolean traceTwoEnabled;
    double traceOneVoltsDiv;
    double traceTwoVoltsDiv;
    double timeDiv; // Is really frequency (to reduce rounding errors)

    // For savedInstanceState
    static final String TRACE_ONE_ENABLED = "traceOneEnabled";
    static final String TRACE_TWO_ENABLED = "traceTwoEnabled";
    static final String TRACE_ONE_VOLTS_DIV = "traceOneVoltsDiv";
    static final String TRACE_TWO_VOLTS_DIV = "traceTwoVoltsDiv";
    static final String TIME_DIV = "timeDiv";

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
    int currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup dropdown navigation in the action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setDisplayShowTitleEnabled(false);
        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(
                actionBar.getThemedContext(), R.array.dropdownOptions,
                android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
        // Go to digital
        actionBar.setSelectedNavigationItem(2);

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
        } else {
            // Setup bluetooth connection
            setupBluetooth();
        }
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

            // TODO Remove
            // Send sample data
            /*
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(!Thread.currentThread().isInterrupted()) {
                        runCommand(new Command((byte) 0x13, new byte[]{0x05, 0x08}, 3));
                        try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
                    }
                }
            }).start();
            */
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
            // Show button
            disconnectButton.setVisibility(View.VISIBLE);
            if (e == null) {
                connectionStatus.setText(getString(R.string.connected));
                disconnectButton.setText(getString(R.string.disconnect));
            } else {
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

        // Change disconnect button text to connect
        disconnectButton.setText(getString(R.string.connect));
        // Change status text to disconnected
        connectionStatus.setText(getString(R.string.disconnected));
    }


    // Connect to Bluetooth device
    private void connectBluetooth() throws IOException {
        // Connect to preset Bluetooth device
        // TODO choose device
        String address = "00:13:12:18:62:59";
        BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(address);

        // Setup serial communication with device
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Serial UUID
        mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
        mSocket.connect();

        mOutputStream = mSocket.getOutputStream();
        mInputStream = mSocket.getInputStream();

        // Setup new CommandInterface to start communicating with device
        mCommandInterface = new CommandInterface(mOutputStream, mInputStream);

        // Start by requesting a digital sample
        // TODO Check mode
        onDigitalSampleRequested();
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
        currentFragment = position;
        switch(position) {
            case GRAPH_FRAGMENT:    switchToGraph();
                                    break;
            case SPECTRUM_FRAGMENT: switchToSpectrum();
                                    break;
            case DIGITAL_FRAGMENT:  switchToDigital();
                                    break;
            default:                break;
        }
        return false;
    }

    // Save controls permanently & in current app instance (persistent across rotation)
    // Based on SO#151777
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
        updateGraphYBounds();
    }
    public void onTraceTwoVoltsDivChanged(double value) {
        traceTwoVoltsDiv = value;
        // Update y bounds of graph
        updateGraphYBounds();
    }
    public void onTimeDivChanged(double value) {
        timeDiv = value;
    }

    public void updateGraphYBounds() {
        // Use the maximum volts/div out of traces one and two
        double voltsDiv = (traceOneVoltsDiv > traceTwoVoltsDiv) ? traceOneVoltsDiv : traceTwoVoltsDiv;
        if (mGraphFragment != null && currentFragment == GRAPH_FRAGMENT) {
            //mGraphFragment.setYBounds(-voltsDiv*10, voltsDiv*10);
        }
    }

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

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, mGraphFragment);
        transaction.replace(R.id.controlsContainer, mControlsFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        updateGraphYBounds();
    }

    // Switch to digital (basic logic analyser) view
    public void switchToDigital() {
        if (mDigitalFragment == null) {
            mDigitalFragment = new DigitalFragment();
        }
        if (mDigitalControlsFragment == null) {
            mDigitalControlsFragment = new DigitalControlsFragment();
        }

        // Sample provided we're connected to BT
        if (mCommandInterface != null) {
            onDigitalSampleRequested();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, mDigitalFragment);
        transaction.replace(R.id.controlsContainer, mDigitalControlsFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Switch to spectrum (FFT) view
    public void switchToSpectrum() {
        if (mSpectrumFragment == null) {
            mSpectrumFragment = new SpectrumFragment();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, mSpectrumFragment);
        transaction.addToBackStack(null);
        transaction.commit();
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

    public void onDigitalTimeSampleChanged(double timeSample) {
        if (mDigitalFragment != null) {
            mDigitalFragment.setTimeSample(timeSample);
        }

        Log.w("digital time sample", Double.toString(timeSample));
        int timeDelay = (int) Math.floor(timeSample - 1.6);
        if (timeDelay < 0) { timeDelay = 0; }
        Log.w("digital time delay", Integer.toString(timeDelay));

        // Set time
        byte[] commandData = new byte[] {(byte) ((byte)timeDelay>>8), (byte) ((byte)timeDelay&0xFF)};
        Log.w("time delay bytes", CommandInterface.bytesToHex(commandData));
        Command command = new Command((byte) 0x06, commandData, 0, new CommandInterface.CommandCallback() {
            public void commandFinished(byte[] data) {
                onDigitalSampleRequested();
            }
        });
        runCommand(command);
    }
}
