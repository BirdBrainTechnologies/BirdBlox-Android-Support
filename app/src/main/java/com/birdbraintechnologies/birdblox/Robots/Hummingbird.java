package com.birdbraintechnologies.birdblox.Robots;

import android.util.Log;

import com.birdbraintechnologies.birdblox.Bluetooth.UARTConnection;
import com.birdbraintechnologies.birdblox.Robots.RobotStates.HBState;
import com.birdbraintechnologies.birdblox.Util.DeviceUtil;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import static com.birdbraintechnologies.birdblox.MainWebView.bbxEncode;
import static com.birdbraintechnologies.birdblox.MainWebView.runJavascript;


/**
 * Represents a Hummingbird device and all of its functionality: Setting outputs, reading sensors
 *
 * @author Terence Sun (tsun1215)
 * @author Shreyan Bakshi (AppyFizz)
 * @author Zhendong Yuan (yzd1998111)
 */
//public class Hummingbird extends Robot<HBState> implements UARTConnection.RXDataListener {
public class Hummingbird extends Robot<HBState, HBState> {
    private final String TAG = this.getClass().getSimpleName();

    /*
     * Command prefixes for the Hummingbird according to spec
     * More info: http://www.hummingbirdkit.com/learning/hummingbird-duo-usb-protocol
     */
    private static final byte TRI_LED_CMD = 'O';
    private static final byte LED_CMD = 'L';
    private static final byte MOTOR_CMD = 'M';
    private static final byte VIB_MOTOR_CMD = 'V';
    private static final byte SERVO_CMD = 'S';
    private static final byte READ_SENSOR_CMD = 's';
    private static final byte READ_ALL_CMD = 'G';
    private static final byte STOP_PERIPH_CMD = 'X';
    private static final byte TERMINATE_CMD = 'R';
    private static final byte PING_CMD = 'z';
    private static final String RENAME_CMD = "AT+GAPDEVNAME";

    private static final byte[] TERMINATECOMMAND = new byte[]{(byte) 'R'};
    private static final byte[] STOPALLCOMMAND = new byte[]{(byte) 'X' };
    private static final byte[] STOPPOLLCOMMAND = new byte[]{READ_ALL_CMD, '6'};
    private static final byte[] STARTPOLLCOMMAND = new byte[]{READ_ALL_CMD, '5'};
    private static final byte[] FIRMWARECOMMAND = "G4".getBytes();

    private static final double HBIRD_BATTERY_INDEX = 4;
    private static final double HBIRD_BATTERY_CONSTANT = 0;
    private static final double HBIRD_RAW_TO_VOLTAGE = 0.0406;
    private static final double HBIRD_GREEN_THRESHOLD = 4.75;
    private static final double HBIRD_YELLOW_THRESHOLD = 4.63;

    private static final int SETALL_INTERVAL_IN_MILLIS = 32; //TODO: Should this be different or the same?
    //private static final int COMMAND_TIMEOUT_IN_MILLIS = 5000;
    //private static final int SEND_ANYWAY_INTERVAL_IN_MILLIS = 50;
    //private static final int START_SENDING_INTERVAL_IN_MILLIS = 0;
    //private static final int MONITOR_CONNECTION_INTERVAL_IN_MILLIS = 1000;
    //private static final int MAX_NO_G4_RESPONSE_BEFORE_DISCONNECT_IN_MILLIS = 15000;
    //private static final int MAX_NO_NORMAL_RESPONSE_BEFORE_DISCONNECT_IN_MILLIS = 3000;


    // This represents the firmware version 2.2a
    private static final byte minFirmwareVersion1 = 2;
    private static final byte minFirmwareVersion2 = 2;
    private static final String minFirmwareVersion3 = "a";

    // This represents the firmware version 2.2b
    private static final byte latestFirmwareVersion1 = 2;
    private static final byte latestFirmwareVersion2 = 2;
    private static final String latestFirmwareVersion3 = "b";

    /*private AtomicBoolean g4;
    private AtomicLong last_sent;
    private AtomicLong last_successfully_sent;

    private String last_battery_status;*/

    //private UARTConnection conn;
    //private byte[] rawSensorValues;
    //private Object rawSensorValuesLock = new Object();

    //private final ReentrantLock lock;
    //private final Condition doneSending;

    /*private final HandlerThread sendThread;
    private final HandlerThread monitorThread;

    private Disposable sendDisposable;
    private Disposable monitorDisposable;

    private byte[] g4response;*/

    //private boolean ATTEMPTED = false;
    //private boolean DISCONNECTED = false;

    /**
     * Initializes a Hummingbird device
     *
     * @param conn Connection established with the Hummingbird device
     */
    public Hummingbird(final UARTConnection conn) {
        super(conn, true);
        //this.conn = conn;

        oldPrimaryState = new HBState();
        newPrimaryState = new HBState();

        //These states aren't used in the Hummingbird case. Can they be removed somehow?
        oldSecondaryState = new HBState();
        newSecondaryState = new HBState();

        /*g4 = new AtomicBoolean(true);
        last_sent = new AtomicLong(System.currentTimeMillis());
        last_successfully_sent = new AtomicLong(System.currentTimeMillis());
        last_battery_status = "";*/

        //lock = new ReentrantLock();
        //doneSending = lock.newCondition();

        /*sendThread = new HandlerThread("SendThread");
        if (!sendThread.isAlive())
            sendThread.start();
        from(sendThread.getLooper());
        Runnable sendRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    lock.tryLock(COMMAND_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
                    sendToRobot();
                    doneSending.signal();
                    if (DISCONNECTED) {
                        return;
                    }
                } catch (NullPointerException | InterruptedException | IllegalMonitorStateException e) {
                    Log.e("SENDHBSIG", "Signalling failed " + e.getMessage());
                } finally {
                    if (lock.isHeldByCurrentThread())
                        lock.unlock();
                }
            }
        };
        sendDisposable = from(sendThread.getLooper()).schedulePeriodicallyDirect(sendRunnable,
                START_SENDING_INTERVAL_IN_MILLIS, SETALL_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
*/
        /*monitorThread = new HandlerThread("SendThread");
        if (!monitorThread.isAlive())
            monitorThread.start();

        Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                long timeOut = g4.get() ? MAX_NO_G4_RESPONSE_BEFORE_DISCONNECT_IN_MILLIS : MAX_NO_NORMAL_RESPONSE_BEFORE_DISCONNECT_IN_MILLIS;
                final long curSysTime = System.currentTimeMillis();
                final long prevTime = last_successfully_sent.get();
                long passedTime = curSysTime - prevTime;
                if (passedTime >= timeOut) {
                    try {
                        runJavascript("CallbackManager.robot.updateStatus('" + bbxEncode(getMacAddress()) + "', false);");
                        runJavascript("CallbackManager.robot.updateBatteryStatus('" + bbxEncode(getMacAddress()) + "', '" + bbxEncode("3") + "');");
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                String macAddr = getMacAddress();
                                //RobotRequestHandler.disconnectFromHummingbird(macAddr);
                                RobotRequestHandler.disconnectFromRobot(RobotType.Hummingbird, macAddr);
                                synchronized (hummingbirdsToConnect) {
                                    if (!hummingbirdsToConnect.contains(macAddr)) {
                                        hummingbirdsToConnect.add(macAddr);
                                    }
                                }
                                if (DISCONNECTED) {
                                    return;
                                }
                            }
                        }.start();
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while auto-disconnecting: " + e.getMessage());
                    }
                }
            }
        };
        monitorDisposable = AndroidSchedulers.from(monitorThread.getLooper()).schedulePeriodicallyDirect(monitorRunnable,
                START_SENDING_INTERVAL_IN_MILLIS, MONITOR_CONNECTION_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS);
    */
    }

    /**
     * Actually sends the commands to the physical Hummingbird,
     * based on certain conditions.
     */
    /*public synchronized void sendToRobot() {
        long currentTime = System.currentTimeMillis();
        if (g4.get()) {
            // Send here
            setSendingTrue();
            g4response = conn.writeBytesWithResponse("G4".getBytes()); //TODO: WITH RESPONSE!
            if (g4response != null && g4response.length > 0) {
                // Successfully sent G4 command
                if (last_successfully_sent != null)
                    last_successfully_sent.set(currentTime);
                g4.set(false);
                runJavascript("CallbackManager.robot.updateStatus('" + bbxEncode(getMacAddress()) + "', true);");
                try {
                    if (rawSensorValues == null) {
                        rawSensorValues = startPollingSensors();
                        conn.addRxDataListener(this);
                    }
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error getting HB sensor values: " + e.getMessage());
                }
                if (!hasMinFirmware()) {
                    Log.d(TAG, "HB disconnecting incompatible...");
                    g4.set(true);
                    runJavascript("CallbackManager.robot.disconnectIncompatible('" + bbxEncode(getMacAddress()) + "', '" + bbxEncode(getFirmwareVersion()) + "', '" + bbxEncode(getMinFirmwareVersion()) + "')");
                    disconnect();
                    Log.d(TAG, "HB disconnected incompatible.");
                } else if (!hasLatestFirmware()) {
                    runJavascript("CallbackManager.robot.updateFirmwareStatus('" + bbxEncode(getMacAddress()) + "', 'old')");
                }
            } else {
                // Sending Non-G4 command failed
            }
            setSendingFalse();
            last_sent.set(currentTime);
            return;
        }
        // Not G4
        if (isCurrentlySending()) {
            // do nothing in this case
            return;
        }
        // Not currently sending s
        if (!primaryStatesEqual()) {
            // Not currently sending, but oldState and newState are different
            // Send here
            setSendingTrue();
            if (conn.writeBytes(newPrimaryState.setAll())) {
                // Successfully sent Non-G4 command
                if (last_successfully_sent != null)
                    last_successfully_sent.set(currentTime);
                oldPrimaryState.copy(newPrimaryState);
                runJavascript("CallbackManager.robot.updateStatus('" + bbxEncode(getMacAddress()) + "', true);");
            } else {
                // Sending Non-G4 command failed
            }
            setSendingFalse();
            last_sent.set(currentTime);
        } else {
            // Not currently sending, and oldState and newState are the same
            if (currentTime - last_sent.get() >= SEND_ANYWAY_INTERVAL_IN_MILLIS) {
                setSendingTrue();
                if (conn.writeBytes(newPrimaryState.setAll())) {
                    // Successfully sent Non-G4 command
                    if (last_successfully_sent != null)
                        last_successfully_sent.set(currentTime);
                    oldPrimaryState.copy(newPrimaryState);
                    runJavascript("CallbackManager.robot.updateStatus('" + bbxEncode(getMacAddress()) + "', true);");
                } else {
                    // Sending Non-G4 command failed
                }
                setSendingFalse();
                last_sent.set(currentTime);
            }
        }
    }*/

    /**
     * Sets the output of the given output type according to args
     *
     * @param outputType Type of the output
     * @param args       Arguments for setting the output
     * @return True if the output was successfully set, false otherwise
     */
    @Override
    public boolean setOutput(String outputType, Map<String, List<String>> args) {
        // Handle stop output type (since it doesn't have a port specification)
        if (outputType.equals("stop")) {
            return stopAll();
        }

        // All remaining outputs are of the format: /out/<outputType>/<port>/<args>...

        int port = Integer.parseInt(args.get("port").get(0));

        switch (outputType) {
            case "servo":
                return setRbSOOutput(oldPrimaryState.getServo(port), newPrimaryState.getServo(port), Integer.parseInt(args.get("angle").get(0)));
            case "motor":
                return setRbSOOutput(oldPrimaryState.getMotor(port), newPrimaryState.getMotor(port), Integer.parseInt(args.get("speed").get(0)));
            case "vibration":
                return setRbSOOutput(oldPrimaryState.getVibrator(port), newPrimaryState.getVibrator(port),
                        Integer.parseInt(args.get("intensity").get(0)));
            case "led":
                return setRbSOOutput(oldPrimaryState.getLED(port), newPrimaryState.getLED(port), Integer.parseInt(args.get("intensity").get(0)));
            case "triled":
                return setRbSOOutput(oldPrimaryState.getTriLED(port), newPrimaryState.getTriLED(port), Integer.parseInt(args.get("red").get(0)),
                        Integer.parseInt(args.get("green").get(0)), Integer.parseInt(args.get("blue").get(0)));
        }
        return false;
    }

    /**
     * Reads the value of the sensor at the given port and returns the formatted value according to
     * sensorType
     *
     * @param sensorType Type of sensor connected to the port (dictates format of the returned
     *                   value)
     * @param portString Port that the sensor is connected to
     * @return A string representing the value of the sensor
     */
    @Override
    public String readSensor(String sensorType, String portString, String axisString) {
        byte rawSensorValue;
        synchronized (rawSensorValuesLock) {
            int port = Integer.parseInt(portString) - 1;
            rawSensorValue = rawSensorValues[port];
        }
        switch (sensorType) {
            case "distance":
                return Double.toString(DeviceUtil.RawToDist(rawSensorValue));
            case "temperature":
                return Double.toString(DeviceUtil.RawToTemp(rawSensorValue));
            case "sound":
                //The duo sound sensor returns raw values already in the range of 0 to 100.
                return Byte.toString(rawSensorValue);
            case "light":
            case "sensor":
            default:
                return Double.toString(DeviceUtil.RawToPercent(rawSensorValue));
        }
    }

    /*private byte[] startPollingSensors() {
        return conn.writeBytesWithResponse(new byte[]{READ_ALL_CMD, '5'});
    }*/

    @Override
    protected void notifyIncompatible() {
        runJavascript("CallbackManager.robot.disconnectIncompatible('" + bbxEncode(getMacAddress()) + "', '" + bbxEncode(getFirmwareVersion()) + "', '" + bbxEncode(getMinFirmwareVersion()) + "')");
    }

    /*private void stopPollingSensors() {
        conn.writeBytes(new byte[]{READ_ALL_CMD, '6'});
    }*/

    /*protected boolean setRbSOOutput(RobotStateObject oldobj, RobotStateObject newobj, int... values) {
        try {
            lock.tryLock(COMMAND_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
            AtomicInteger count = new AtomicInteger(0);
            while (!newobj.equals(oldobj)) {
                if (count.incrementAndGet() > 1) break;
                doneSending.await(COMMAND_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
            }
            if (newobj.equals(oldobj)) {
                newobj.setValue(values);
                if (lock.isHeldByCurrentThread()) {
                    doneSending.signal();
                    lock.unlock();
                }
                return true;
            }
        } catch (InterruptedException | IllegalMonitorStateException | IllegalStateException | IllegalThreadStateException e) {
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
        return false;
    }*/

    /**
     * Resets all hummingbird peripherals to their default values.
     * <p>
     * Sending a {@value #STOP_PERIPH_CMD} should achieve the same
     * thing on legacy firmware.
     *
     * @return True if succeeded in changing state, false otherwise
     */
    /*public boolean stopAll() {
        try {
            lock.tryLock(COMMAND_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
            while (!statesEqual()) {
                doneSending.await(COMMAND_TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
            }
            if (statesEqual()) {
                newState.resetAll();
                if (lock.isHeldByCurrentThread()) {
                    doneSending.signal();
                    lock.unlock();
                }
                return true;
            }
        } catch (InterruptedException | IllegalMonitorStateException | IllegalStateException | IllegalThreadStateException e) {
        } finally {
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
        return false;
    }*/

    /**
     * Returns whether or not this device is connected
     *
     * @return True if connected, false otherwise
     */
    /*public boolean isConnected() {
        return conn.isConnected();
    }*/

    /*public void setConnected() {
        DISCONNECTED = false;
    }*/

    /**
     * Disconnects the device
     */
    /*public void disconnect() {
        if (!DISCONNECTED) {
            if (ATTEMPTED) {
                forceDisconnect();
                return;
            }
            ATTEMPTED = true;
            conn.writeBytes(new byte[]{TERMINATE_CMD});

            AndroidSchedulers.from(sendThread.getLooper()).shutdown();
            sendThread.getLooper().quit();
            if (sendDisposable != null && !sendDisposable.isDisposed())
                sendDisposable.dispose();
            sendThread.interrupt();
            sendThread.quit();

            AndroidSchedulers.from(monitorThread.getLooper()).shutdown();
            monitorThread.getLooper().quit();
            if (monitorDisposable != null && !monitorDisposable.isDisposed())
                monitorDisposable.dispose();
            monitorThread.interrupt();
            monitorThread.quit();


            if (conn != null) {
                conn.removeRxDataListener(this);
                stopPollingSensors();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
                conn.disconnect();
            }
            ATTEMPTED = false;
            DISCONNECTED = true;
        }
    }*/

    /*public boolean getDisconnected() {
        return DISCONNECTED;
    }*/

    /*public void forceDisconnect() {
        String macAddr = getMacAddress();
        if (!DISCONNECTED) {
            ATTEMPTED = false;
            AndroidSchedulers.from(sendThread.getLooper()).shutdown();
            sendThread.getLooper().quit();
            if (sendDisposable != null && !sendDisposable.isDisposed())
                sendDisposable.dispose();
            sendThread.interrupt();
            sendThread.quit();

            AndroidSchedulers.from(monitorThread.getLooper()).shutdown();
            monitorThread.getLooper().quit();
            if (monitorDisposable != null && !monitorDisposable.isDisposed())
                monitorDisposable.dispose();
            monitorThread.interrupt();
            monitorThread.quit();

            if (conn != null) {
                conn.removeRxDataListener(this);
                conn.forceDisconnect();
            }
            synchronized (hummingbirdsToConnect) {
                if (!hummingbirdsToConnect.contains(macAddr)) {
                    hummingbirdsToConnect.add(macAddr);
                }
            }
            DISCONNECTED = true;
        }
    }*/


    /*@Override
    public void onRXData(byte[] newData) {
        synchronized (rawSensorValuesLock) {
            this.rawSensorValues = newData;
            String curBatteryStatus = "";
            double batteryVoltage = (newData[4] & 0xFF) * 0.0406;
            if (batteryVoltage > 4.75) {
                curBatteryStatus = "2";
            } else if (batteryVoltage > 4.63) {
                curBatteryStatus = "1";
            } else {
                curBatteryStatus = "0";
            }
            if (!curBatteryStatus.equals(last_battery_status)) {
                last_battery_status = curBatteryStatus;
                runJavascript("CallbackManager.robot.updateBatteryStatus('" + bbxEncode(getMacAddress()) + "', '" + bbxEncode(curBatteryStatus) + "');");
            }
        }
    }*/

    @Override
    public byte[] getCalibrateCommand() {
        return null;
    }

    @Override
    public byte[] getResetEncodersCommand() {
        return null;
    }

    @Override
    public byte[] getStartPollCommand() {
        return STARTPOLLCOMMAND;
    }

    @Override
    public byte[] getStopPollCommand() {
        return STOPPOLLCOMMAND;
    }

    @Override
    public byte[] getTerminateCommand() {
        return TERMINATECOMMAND;
    }

    @Override
    public byte[] getStopAllCommand() {
        return STOPALLCOMMAND;
    }

    @Override
    public double[] getBatteryConstantsArray() {
        return new double[]{
                HBIRD_BATTERY_INDEX,
                HBIRD_BATTERY_CONSTANT,
                HBIRD_RAW_TO_VOLTAGE,
                HBIRD_GREEN_THRESHOLD,
                HBIRD_YELLOW_THRESHOLD};
    }

    @Override
    public int getCompassIndex() { //Hummingbird does not have a compass
        return 0;
    }

    @Override
    protected void sendSecondaryState(int delayInMillis) { } //Hummingbird does not actually have a secondary state.


    @Override
    public String getHardwareVersion() {
        /*try {
            return Byte.toString(g4response[0]) + Byte.toString(g4response[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Hummingbird hardware version: " + e.getMessage());
            return null;
        }*/
        if (cfresponse == null || cfresponse.length < 2) { return null; }
        return Byte.toString(cfresponse[0]) + Byte.toString(cfresponse[1]);
    }

    public String getFirmwareVersion() {
        try {
            //return Byte.toString(g4response[2]) + "." + Byte.toString(g4response[3]) + new String(new byte[]{g4response[4]}, "utf-8");
            return Byte.toString(cfresponse[2]) + "." + Byte.toString(cfresponse[3]) + new String(new byte[]{cfresponse[4]}, "utf-8");
        } catch (UnsupportedEncodingException | ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Hummingbird firmware version: " + e.getMessage());
            return null;
        }
    }

    String getMinFirmwareVersion() {
        return Byte.toString(minFirmwareVersion1) + "." + Byte.toString(minFirmwareVersion2) + minFirmwareVersion3;
    }

    public String getLatestFirmwareVersion() {
        return Byte.toString(latestFirmwareVersion1) + "." + Byte.toString(latestFirmwareVersion2) + latestFirmwareVersion3;
    }

    @Override
    public boolean hasMinFirmware() {
        try {
            int fw1 = (int) cfresponse[2];//g4response[2];
            int fw2 = (int) cfresponse[3];//g4response[3];
            String fw3 = new String(new byte[]{cfresponse[4]}, "utf-8");//new String(new byte[]{g4response[4]}, "utf-8");
            if (fw1 >= minFirmwareVersion1) {
                if (fw1 > minFirmwareVersion1) return true;
                if (fw2 >= minFirmwareVersion2) {
                    return ((fw2 > minFirmwareVersion2) || (fw3.compareTo(minFirmwareVersion3) >= 0));
                }
            }
            return false;
        } catch (UnsupportedEncodingException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            Log.e(TAG, "Hummingbird firmware version: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasLatestFirmware() {
        try {
            int fw1 = (int) cfresponse[2];//g4response[2];
            int fw2 = (int) cfresponse[3];//g4response[3];
            String fw3 = new String(new byte[]{cfresponse[4]}, "utf-8");//new String(new byte[]{g4response[4]}, "utf-8");
            if (fw1 >= latestFirmwareVersion1) {
                if (fw1 > latestFirmwareVersion1) return true;
                if (fw2 >= latestFirmwareVersion2) {
                    return ((fw2 > latestFirmwareVersion2) || (fw3.compareTo(latestFirmwareVersion3) >= 0));
                }
            }
            return false;
        } catch (UnsupportedEncodingException | ArrayIndexOutOfBoundsException | NullPointerException e) {
            Log.e(TAG, "Hummingbird firmware version: " + e.getMessage());
            return false;
        }
    }

    @Override
    public byte[] getFirmwareCommand() {
        return FIRMWARECOMMAND;
    }

    /*@Override
    protected void addToReconnect() {
        addToHashSet(hummingbirdsToConnect);
    }*/
}