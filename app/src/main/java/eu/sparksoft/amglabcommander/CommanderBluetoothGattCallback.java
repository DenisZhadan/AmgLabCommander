package eu.sparksoft.amglabcommander;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommanderBluetoothGattCallback extends BluetoothGattCallback {

    public MainActivity mainActivity;
    public static final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTIC2_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    Handler handler = new Handler();
    public Double time_now;
    public Double time_split;
    public Double time_first;
    public List<Double> shotSequence = new ArrayList<Double>();

    public void initDescriptor(BluetoothGatt gatt) {
        if (gatt == null) {
            // ToDo gatt is null
            return;
        }
        BluetoothGattService service = gatt.getService(SERVICE_UUID);
        if (service == null) {
            // ToDo gatt service not found
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC2_UUID);
        if (characteristic == null) {
            // ToDo service charateristic not found
            return;
        }
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
            Log.v("onConnectionStateChange", "Timer Connected");
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.v("onConnectionStateChange", "Timer Disconnected");
        }
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.v("onServicesDiscovered", "" + status);
        if (status == 0) {
            initDescriptor(gatt);
            showTextConnected();
        }
    }

    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.v("onCharacteristicRead", "" + status);
        if (status == 0) {
            //ToDo bluetoothGattCharacteristic
        }
    }

    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        byte[] bytes = characteristic.getValue();
        String str = "";
        if (bytes[0] >= 10 && bytes[0] <= 26) {
            if (bytes[0] == 10) {
                shotSequence.clear(); //coming first line of data
            }
            for (int i = 1; i <= bytes[1]; i++) {
                shotSequence.add(convertData(bytes[i * 2], bytes[i * 2 + 1]));
            }
            updateText();
        } else if (bytes[0] == 1) {
            if (bytes[1] == 5) {
                //ToDo timer start
            } else if (bytes[1] == 8) {
                //ToDo timer stop waiting
            } else if (bytes[1] == 3) {
                // BLE push
                //bytes[0..1] - type
                //bytes[2..3] - short number?
                //bytes[4..5] - time
                //bytes[6..7] - split
                //bytes[8..9] - first shot
                //bytes[10..11] - ???
                //bytes[12..13] - series/batch

                time_now = convertData(bytes[4], bytes[5]);
                time_split = convertData(bytes[6], bytes[7]);
                time_first = convertData(bytes[8], bytes[9]);
                updateText();
                str = String.format("Time: %.2f%nSplit: %.2f%nFirst: %.2f%n", time_now, time_split, time_first);
            }
        }

        for (byte item : bytes) {
            str += String.valueOf(item) + ", ";
        }
        Log.v("onCharacteristicChanged", str);
    }

    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.v("onDescriptorWrite", "" + status);
    }

    private Double convertData(byte value1, byte value2) {
        int value = 256 * value1 + value2;
        if (value2 <= 0) {
            value += 256;
        }
        return value / 100.0d;
    }

    private void updateText() { //ToDo only for demo, after remove!!!
        Thread t = new Thread() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        CommanderBluetoothGattCallback.this.mainActivity.updateText();
                    }
                });
            }
        };
        t.start();
    }

    private void showTextConnected() { //ToDo only for demo, after remove!!!
        Thread t = new Thread() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        CommanderBluetoothGattCallback.this.mainActivity.ShowTextConnected();
                    }
                });
            }
        };
        t.start();
    }
}
