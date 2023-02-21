package eu.sparksoft.amglabcommander;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothGatt gatt;
    private List<CommanderTimer> timers = new ArrayList<CommanderTimer>();
    private BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
    private CommanderBluetoothGattCallback commanderBluetoothGattCallback;
    private Spinner spinner;
    private TextView textView1;
    private TextView textView2;
    private TextView textView3;
    private TextView textView4;
    public static final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinner = (Spinner) this.findViewById(R.id.spinner);
        textView1 = (TextView) this.findViewById(R.id.textView);
        textView2 = (TextView) this.findViewById(R.id.textView2);
        textView3 = (TextView) this.findViewById(R.id.textView3);
        textView4 = (TextView) this.findViewById(R.id.textView4);
    }

    public void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 2);
        }
    }

    public boolean isAmgLAbTimer(String deviceName) {
        String upperCase = deviceName == null ? "" : deviceName.toUpperCase(Locale.ENGLISH);
        return upperCase.startsWith("AMG LAB COMM") || upperCase.startsWith("COMMANDER");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            // ...do whatever you want with this found device
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            scanner.stopScan(scanCallback);
            spinner.setAdapter(null);
            timers.clear();

            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                // ...do whatever you want with this found device
                String deviceName = device.getName();
                if (isAmgLAbTimer(deviceName)) {
                    timers.add(new CommanderTimer(deviceName, device.getAddress()));
                }
                Log.v("onScanResult", "device name: " + deviceName + " Rssi: " + String.valueOf(result.getRssi()));
            }
            ArrayAdapter reportAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, timers);
            spinner.setAdapter(reportAdapter);
            Log.v("onBatchScanResults", "end");
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.v("onScanFailed", String.valueOf(errorCode));
            // ToDo onScanFailed
        }
    };

    public void onClick(View v) {
        if (scanner != null) {
            List<ScanFilter> filters = null;
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) //SCAN_MODE_LOW_POWER
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES) //
                    //.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH) //CALLBACK_TYPE_ALL_MATCHES
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(2L)
                    .build();
            scanner.startScan(filters, scanSettings, scanCallback);
            Log.d("TAG", "scan started");
        } else {
            Log.e("TAG", "could not get scanner object");
        }
    }

    public void onClick2(View v) {
        String address = timers.get(spinner.getSelectedItemPosition()).address;
        BluetoothDevice device = adapter.getRemoteDevice(address);
        this.commanderBluetoothGattCallback = new CommanderBluetoothGattCallback();
        this.gatt = device.connectGatt(this, true, this.commanderBluetoothGattCallback, TRANSPORT_LE); // ToDo is need TRANSPORT_LE?

        // ToDo only for demo, after remove!!!
        this.commanderBluetoothGattCallback.mainActivity = MainActivity.this;
    }

    public boolean sendMessageToTimer(String value) {
        byte[] bytes = new byte[]{};
        try {
            bytes = value.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        BluetoothGattService service = this.gatt.getService(SERVICE_UUID);
        if (service == null) {
            // ToDo Rx service not found
            Log.v("sendMessageToTimer", "Rx service not found!");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            // ToDo Rx charateristic not found!
            Log.v("sendMessageToTimer", "Rx charateristic not found!");
            return false;
        }
        characteristic.setValue(bytes);
        return this.gatt.writeCharacteristic(characteristic);
    }

    public void onClick3(View v) {
        String msg = "COM START";
        sendMessageToTimer(msg);
    }

    public void onClick4(View v) {
        // ToDo String msg1 = "REQ SCREEN HEX";
        // int i = 1; //1 .. 10
        // Todo String msg3 = String.format("SET SENSITIVITY %02d", i);

        String msg2 = "REQ STRING HEX";
        sendMessageToTimer(msg2);
    }

    public void updateText() { // ToDo only for demo, after remove!!!
        if (this.commanderBluetoothGattCallback != null) {
            textView1.setText(String.format("First: %.2f", this.commanderBluetoothGattCallback.time_first));
            textView2.setText(String.format("Time: %.2f", this.commanderBluetoothGattCallback.time_now));
            textView3.setText(String.format("Split: %.2f", this.commanderBluetoothGattCallback.time_split));
            textView4.setText("Shot sequence: " + this.commanderBluetoothGattCallback.shotSequence.toString());
        }
    }

    public void ShowTextConnected() { // ToDo only for demo, after remove!!!
        Toast.makeText(MainActivity.this, "Timer connected!", Toast.LENGTH_SHORT).show();
    }
}
