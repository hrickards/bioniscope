package com.harryrickards.bioniscope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Used to send commands via an input and output stream
 */
public class CommandInterface {
    OutputStream mOutputStream;
    InputStream mInputStream;

    // Queue for storing commands to run
    Queue<Command> mQueue = new LinkedList<Command>();

    public interface CommandCallback {
        public void commandFinished(byte[] data);
    }

    public CommandInterface(OutputStream mmOutputStream, InputStream mmInputStream) {
        mOutputStream = mmOutputStream;
        mInputStream = mmInputStream;
        startCommunication();
    }

    // Queue up a Command to run
    public void runCommand(Command command) {
        mQueue.add(command);
    }

    // Communicate (immediately) a command to device
    private void communicateCommand(Command command) throws IOException {
        // Write command byte
        mOutputStream.write(command.command);

        // Write output data bytes
        if (command.outData.length > 0) {
            mOutputStream.write(command.outData);
        }

        // Read input data into buffer
        byte[] buffer = new byte[command.inDataLength];
        int  bufferPosition = 0;
        while (bufferPosition < command.inDataLength) {
            int bytesAvailable = mInputStream.available();
            if (bytesAvailable > 0) {
                bufferPosition += mInputStream.read(buffer, bufferPosition, Math.min(bytesAvailable,command.inDataLength-bufferPosition));
            }
        }
        // Read newline to finish command
        while (true) {int bytesAvailable = mInputStream.available();
            if (bytesAvailable > 0) {
                mInputStream.read();
                break;
            }
        }

        command.callback.commandFinished(buffer);

        // Log data
        //Log.i("scope", "Sent command " + bytesToHex(new byte[]{command.command}));
        //Log.i("scope", "Sent data " + bytesToHex(command.outData));
        //Log.i("scope", "Received data " + bytesToHex(buffer));
    }

    // Handle transceiving commands over IO streams
    private void startCommunication() {
        // Thread to handle communication with device
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()) {

                   // Log.w("scope", "initial");
                    // Skip this iteration if IO streams are null
                    if (mInputStream == null || mOutputStream == null) {
                        continue;
                    }

                    try {
                        // Retrieve command to run
                        Command command = mQueue.poll();
                        // If queue was not empty, run it
                        if (command != null) {
                            communicateCommand(command);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        // Start thread (loops indefinitely)
        workerThread.start();
    }

    // Format bytes as a hex string. Copied from SO #9655181
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
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
