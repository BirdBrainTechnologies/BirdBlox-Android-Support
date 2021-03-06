package com.birdbraintechnologies.birdblox.Util;

/**
 * Utility class for the Hummingbird. Contains library functions for converting from raw sensor
 * values to different units.
 *
 * @author Brandon Price
 * @author Terence Sun (tsun1215)
 * @author Zhendong Yuan (yzd1998111)
 */
public class DeviceUtil {

    /**
     * Converts raw readings from sensors [0,255] into percentage [0,100]
     *
     * @param raw Raw reading from sensor
     * @return Sensor reading as a percentage
     */
    public static double RawToKnob(int raw) {
        return (raw * 100.0 / 230.0) > 100.0 ? 100.0 : (raw * 100.0 / 230.0);
    }

    public static double RawToPercent(byte raw) {
        return RawToInt(raw) / 2.55;
    }

    public static double RawToDistance(int raw) {
        return raw * 117.0 / 100.0;
    }

    /**
     * Converts percent readings [0,100] to raw [0,255]
     */
    public static byte PercentToRaw(double percent) {
        return (byte) (percent * 2.55);
    }

    /**
     * Converts raw readings from sensors [0,255] into temperature
     *
     * @param raw Raw reading from sensor
     * @return Sensor reading as temperature
     */
    public static double RawToTemp(byte raw) {
        return ((RawToInt(raw) - 127.0) / 2.4 + 25) * 100 / 100;
    }

    /**
     * Converts raw readings from sensors [0,255] into distance
     *
     * @param raw Raw reading from sensor
     * @return Sensor reading as distance
     */
    public static double RawToDist(byte raw) {
        double reading = RawToInt(raw) * 4.0;
        if (reading < 130) {
            return 100;
        } else {
            // Formula based on mathematical regression
            reading = reading - 120.0;
            if (reading > 680.0) {
                return 5;
            } else {
                double sensor_val_square = reading * reading;
                double distance = sensor_val_square * sensor_val_square * reading * -0.000000000004789 +
                        sensor_val_square * sensor_val_square * 0.000000010057143 -
                        sensor_val_square * reading * 0.000008279033021 +
                        sensor_val_square * 0.003416264518201 -
                        reading * 0.756893112198934 +
                        90.707167605683000;
                return distance;
            }
        }
    }

    /**
     * Converts raw readings from sensors [0,255] into accelerometer values.
     * @param rawAccl the byte array of raw accelerometer values in 3 directions
     * @param axisString the axis of acceleration
     * @return the acceleration in a specific axis based on the raw value.
     */
    public static double RawToAccl(byte[] rawAccl, String axisString) {
        switch (axisString) {
            case "x":
                return Complement(RawToInt(rawAccl[0])) * 196.0 / 1280.0;
            case "y":
                return Complement(RawToInt(rawAccl[1])) * 196.0 / 1280.0;
            case "z":
                return Complement(RawToInt(rawAccl[2])) * 196.0 / 1280.0;
        }
        return 0.0;
    }

    /**
     * Converts raw readings from sensors [0,255] into magnetometer values.
     * @param rawMag the byte array of raw magnetometer values in 3 directions
     * @param axisString the axis of magnetometer.
     * @return the magnetometer value in a specific axis based on the raw value.
     */
    public static double RawToMag(byte[] rawMag, String axisString) {
        short mx = (short) ((rawMag[1] & 0xFF) | (rawMag[0] << 8)) ;
        short my = (short) ((rawMag[3] & 0xFF) | (rawMag[2] << 8)) ;
        short mz = (short) ((rawMag[5] & 0xFF) | (rawMag[4] << 8)) ;

        switch (axisString) {
            case "x":
                return mx;
            case "y":
                return my;
            case "z":
                return mz;
        }
        return 0.0;
    }

    /**
     * Converts raw readings from sensors [0,255] into angle in degrees.
     * @param rawMag the byte array of raw magnetometer values in 3 directions
     * @param rawAccl the byte array of raw accelerometer values in 3 directions
     * @return the anglge in degrees based on the raw magnetometer values and raw accelerometer values.
     */
    public static double RawToCompass(byte[] rawAccl, byte[] rawMag) {
        double ax = Complement(RawToInt(rawAccl[0])) * 1.0;
        double ay = Complement(RawToInt(rawAccl[1])) * 1.0;
        double az = Complement(RawToInt(rawAccl[2])) * 1.0;

        short mx = (short) ((rawMag[1] & 0xFF) | (rawMag[0] << 8)) ;
        short my = (short) ((rawMag[3] & 0xFF) | (rawMag[2] << 8)) ;
        short mz = (short) ((rawMag[5] & 0xFF) | (rawMag[4] << 8)) ;


        double phi = Math.atan(-ay / az);
        double theta = Math.atan(ax / (ay * Math.sin(phi) + az * Math.cos(phi)));

        double xp = mx;
        double yp = my * Math.cos(phi) - mz * Math.sin(phi);
        double zp = my * Math.sin(phi) + mz * Math.cos(phi);

        double xpp = xp * Math.cos(theta) + zp * Math.sin(theta);
        double ypp = yp;

        double angle = 180.0 + Math.toDegrees(Math.atan2(xpp, ypp));
        return angle;
    }

    /**
     * Converts raw readings from sensors [0,255] into sound value
     *
     * @param raw Raw reading from sensor
     * @return Sensor reading as sound
     */
    public static double RawToSound(int raw) {
        return (raw * 200.0) / 255.0;
    }

    /**
     * Converts raw readings from sensors [0,255] into light value
     *
     * @param raw Raw reading from sensor
     * @return Sensor reading as light
     */
    public static double RawToLight(int raw) {
        return (raw * 100.0) / 255.0;
    }


    /**
     * Converts raw readings from sensors [0,255] into voltage
     *
     * @param raw Raw reading from sensor
     * @return Sensor reading as voltage
     */
    public static double RawToVoltage(int raw) {
        return (raw * 3.3) / 255.0;
    }

    /**
     * Converts raw readings from bytes (always signed), into an unsigned int value
     *
     * @param raw Reading from sensor
     * @return Sensor value represented as an int [0,255]
     */
    public static int RawToInt(byte raw) {
        return raw & 0xff;
    }

    /**
     * Take 2s complement of a given int
     * @param prev the number that 2s complement will be taken on
     * @return 2s complement of input
     */
    public static int Complement(int prev) {
        if (prev > 127) {
            prev = prev - 256;
        }
        return prev;
    }

    /**
     * Converts raw readings from bytes (always signed), into String representation
     * of an unsigned, scaled value
     *
     * @param raw Reading from sensor
     * @return Sensor value represented as a String [0,255]
     */
    public static String RawToPad(byte raw){
        int val = raw & 0xFF;
        val = val * 114 / 255;
        return Integer.toString(val);
    }
}