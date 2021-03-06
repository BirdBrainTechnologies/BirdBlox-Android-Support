package com.birdbraintechnologies.birdblox.Robots.RobotStates;

import com.birdbraintechnologies.birdblox.Robots.RobotStates.RobotStateObjects.LEDArray;

import java.util.Arrays;

/**
 * Created by krissie on 9/4/18.
 */

public class LedArrayState extends RobotState<LedArrayState> {
    private LEDArray[] ledArray;

    public LedArrayState() {
        ledArray = new LEDArray[1];

        for (int i = 0; i < ledArray.length; i++) ledArray[i] = new LEDArray();
    }

    public LEDArray getLedArray() {
        return ledArray[0];
    }

    /**
     * Compares the current ('this') LedArrayState object with another LedArrayState object for equality.
     *
     * @param las The other LedArrayState object.
     * @return Returns true if they're equal (all their attributes
     * have the same values), false otherwise.
     */
    @Override
    public synchronized boolean equals_helper(LedArrayState las) {
        return Arrays.equals(ledArray, las.ledArray);
    }

    /**
     * Compares the current ('this') LedArrayState object with another object for equality.
     *
     * @param las The other object.
     * @return Returns true if they're equal (they're both LedArrayState objects, and all
     * their attributes have the same values), false otherwise.
     */
    @Override
    public synchronized boolean equals(Object las) {
        // self check
        if (this == las)
            return true;
        // null check
        if (las == null)
            return false;
        // type check and cast
        if (getClass() != las.getClass())
            return false;
        return equals_helper((LedArrayState) las);
    }

    /**
     * Copies all attributes of the input LedArrayState into the current ('this') LedArrayState.
     *
     * @param source The LedArrayState from which the attributes are copied.
     */
    @Override
    public synchronized void copy(LedArrayState source) {
        for (int i = 0; i < ledArray.length; i++) {
            ledArray[i].setValue(source.ledArray[i].getCharacters());
        }
    }

    /**
     * Generates a byte array that can be sent to the Robot,
     * to set all the attributes to their current values.
     *
     * @return A byte array containing the required values for all
     * the state objects, in the order shown below.
     */
    @Override
    public synchronized byte[] setAll() {
        byte[] all = new byte[20];
        all[0] = (byte) 0xCC;
        int[] lightData = ledArray[0].getCharacters();
        if (lightData[lightData.length - 1] == 0) {
            // symbol
            all[1] = (byte) 0x80;
            all[2] = ConstructByteFromInts(lightData, 24, 25);
            all[3] = ConstructByteFromInts(lightData, 16, 24);
            all[4] = ConstructByteFromInts(lightData, 8, 16);
            all[5] = ConstructByteFromInts(lightData, 0, 8);
        } else if (lightData.length == 1 && lightData[0] == -1) {
            //reset
            all[1] = (byte) 0x00;
            all[2] = (byte) 0xFF;
            all[3] = (byte) 0xFF;
            all[4] = (byte) 0xFF;
        } else {
            // flash
            int flashLen = lightData.length - 1;
            all[1] = (byte) ((byte) 0x40 | (byte) flashLen);
            for (int i = 0; i < flashLen; i++) {
                all[2 + i] = (byte) lightData[i];
            }
        }
        return all;
    }
    private synchronized byte ConstructByteFromInts(int[] data, int start, int end) {
        int resultByte = 0;
        for (int i = start; i < end; i++) {
            resultByte = resultByte + (data[i] << (i - start));
        }
        return (byte) resultByte;
    }

    /**
     * Resets all attributes of all state objects to their default values.
     */
    @Override
    public synchronized void resetAll() {
        for (int i = 0; i < ledArray.length; i++) ledArray[i] = new LEDArray();
    }
}
