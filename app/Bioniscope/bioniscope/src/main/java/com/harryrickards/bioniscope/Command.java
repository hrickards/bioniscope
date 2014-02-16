package com.harryrickards.bioniscope;

/**
 * Struct-like class for storing commands to be sent
 */
public class Command {
    public byte command;
    public byte[] outData;
    public int inDataLength;

    public Command(byte mCommand, byte[] mOutData, int mInDataLength) {
        command = mCommand;
        outData = mOutData;
        inDataLength = mInDataLength;
    }
}
