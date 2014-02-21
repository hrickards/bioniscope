package com.harryrickards.bioniscope;

/**
 * Struct-like class for storing commands to be sent
 */
public class Command {
    public byte command;
    public byte[] outData;
    public int inDataLength;
    public CommandInterface.CommandCallback callback;

    public Command(byte mCommand, byte[] mOutData, int mInDataLength, CommandInterface.CommandCallback mCallback) {
        command = mCommand;
        outData = mOutData;
        inDataLength = mInDataLength;
        callback = mCallback;
    }
}
