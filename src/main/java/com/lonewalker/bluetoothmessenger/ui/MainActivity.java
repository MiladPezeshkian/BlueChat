package com.lonewalker.bluetoothmessenger.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.lonewalker.bluetoothmessenger.R;
import com.lonewalker.bluetoothmessenger.adapters.DeviceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int REQUEST_DISCOVERABLE = 3;

    private BluetoothAdapter bluetoothAdapter;
    private DeviceAdapter availableDevicesAdapter;
    private List<com.lonewalker.bluetoothmessenger.data.BluetoothDevice> availableDevices;
    private TextView bluetoothStatusText;
    private MaterialButton btnEnableBluetooth;
    private MaterialButton btnScan;
    private CircularProgressIndicator progressIndicator;
    private View emptyStateLayout;
    private ProgressDialog progressDialog;
    private Handler handler;
    private boolean isDiscoverable = false;
    private boolean isScanning = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Broadcast received: " + action);
            
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    // حل مشکل نام‌های null
                    String deviceName = device.getName();
                    String deviceAddress = device.getAddress();
                    if (deviceName == null) deviceName = "Unknown Device";
                    
                    Log.d(TAG, "Device found: " + deviceName + " - " + deviceAddress);
                    
                    // فیلتر کردن دستگاه‌ها - فقط موبایل‌ها
                    if (shouldShowDevice(deviceName, device)) {
                        com.lonewalker.bluetoothmessenger.data.BluetoothDevice appDevice = 
                            new com.lonewalker.bluetoothmessenger.data.BluetoothDevice(device);
                        
                        // جلوگیری از اضافه شدن تکراری
                        boolean exists = false;
                        for (com.lonewalker.bluetoothmessenger.data.BluetoothDevice d : availableDevices) {
                            if (d.getAddress().equals(deviceAddress)) {
                                exists = true;
                                break;
                            }
                        }
                        
                        if (!exists) {
                            availableDevices.add(appDevice);
                            Log.d(TAG, "Added device: " + deviceName + " (Total: " + availableDevices.size() + ")");
                            
                            // آپدیت UI در thread اصلی
                            final String finalDeviceName = deviceName;
                            runOnUiThread(() -> {
                                availableDevicesAdapter.notifyDataSetChanged();
                                updateEmptyState();
                                Toast.makeText(MainActivity.this, "دستگاه پیدا شد: " + finalDeviceName, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        Log.d(TAG, "Device filtered out: " + deviceName);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "Discovery started");
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.VISIBLE);
                    btnScan.setEnabled(false);
                });
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Discovery finished");
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    isScanning = false;
                    updateEmptyState();
                    Toast.makeText(MainActivity.this, "اسکن تمام شد. " + availableDevices.size() + " دستگاه پیدا شد.", Toast.LENGTH_SHORT).show();
                });
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    
                    Log.d(TAG, "Bond state changed for " + device.getName() + ": " + bondState);
                    
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        Log.d(TAG, "Device paired successfully");
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(MainActivity.this, "دستگاه با موفقیت جفت شد!", Toast.LENGTH_SHORT).show();
                        });
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        Log.d(TAG, "Device unpaired");
                        runOnUiThread(() -> {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(MainActivity.this, "جفت شدن دستگاه لغو شد!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();
        initializeViews();
        setupBluetooth();
        setupRecyclerViews();
        checkPermissions();
    }

    private void initializeViews() {
        bluetoothStatusText = findViewById(R.id.bluetoothStatusText);
        btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        btnScan = findViewById(R.id.btnScan);
        progressIndicator = findViewById(R.id.progressIndicator);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());
        btnScan.setEnabled(true);
        btnScan.setOnClickListener(v -> startScan());
    }

    private void setupBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupRecyclerViews() {
        availableDevices = new ArrayList<>();
        availableDevicesAdapter = new DeviceAdapter(availableDevices, new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onDeviceClick(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device) {
                onDeviceSelected(device);
            }

            @Override
            public void onVoiceClick(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device) {
                // This method is now empty as the onVoiceSelected method has been removed
            }

            @Override
            public void onChatClick(com.lonewalker.bluetoothmessenger.data.BluetoothDevice device) {
                onChatSelected(device);
            }
        });
        RecyclerView availableRecyclerView = findViewById(R.id.availableDevicesRecyclerView);
        availableRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        availableRecyclerView.setAdapter(availableDevicesAdapter);
    }

    private void checkPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // مجوزهای بلوتوث
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
        
        // مجوزهای Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        
        // مجوزهای موقعیت مکانی (ضروری برای اسکن)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        
        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            updateBluetoothStatus();
        }
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothStatusText.setText("بلوتوث فعال است");
            btnEnableBluetooth.setVisibility(View.GONE);
        } else {
            bluetoothStatusText.setText(R.string.text_bluetooth_disabled);
            btnEnableBluetooth.setVisibility(View.VISIBLE);
        }
    }

    private void enableBluetooth() {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void startScan() {
        Log.d(TAG, "Starting scan...");
        Toast.makeText(this, "دکمه اسکن کلیک شد!", Toast.LENGTH_SHORT).show();
        
        // بررسی وضعیت اسکن
        if (isScanning) {
            Toast.makeText(this, "در حال حاضر اسکن در حال انجام است!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // بررسی بلوتوث
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "بلوتوث فعال نیست!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // بررسی مجوزهای Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "مجوز اسکن بلوتوث لازم است!", Toast.LENGTH_SHORT).show();
                checkPermissions();
                return;
            }
        }
        
        // بررسی مجوز موقعیت مکانی
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "مجوز موقعیت مکانی لازم است!", Toast.LENGTH_SHORT).show();
            checkPermissions();
            return;
        }
        
        // پاک کردن لیست قبلی
        availableDevices.clear();
        availableDevicesAdapter.notifyDataSetChanged();
        updateEmptyState();
        
        // نمایش progress
        progressIndicator.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        isScanning = true;
        
        // متوقف کردن اسکن قبلی
        if (bluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "Canceling previous discovery");
            bluetoothAdapter.cancelDiscovery();
        }
        
        // اضافه کردن دستگاه‌های جفت شده
        addPairedDevices();
        
        // شروع اسکن جدید
        Log.d(TAG, "Starting new discovery");
        boolean started = bluetoothAdapter.startDiscovery();
        
        if (started) {
            Log.d(TAG, "Discovery started successfully");
            Toast.makeText(this, "اسکن شروع شد...", Toast.LENGTH_SHORT).show();
            
            // تایمر برای اطمینان از اتمام اسکن
            handler.postDelayed(() -> {
                if (isScanning && bluetoothAdapter.isDiscovering()) {
                    Log.d(TAG, "Forcing discovery to stop");
                    bluetoothAdapter.cancelDiscovery();
                }
            }, 30000); // 30 ثانیه
        } else {
            Log.e(TAG, "Failed to start discovery");
            Toast.makeText(this, "شروع اسکن با خطا مواجه شد! دوباره تلاش کنید.", Toast.LENGTH_SHORT).show();
            progressIndicator.setVisibility(View.GONE);
            btnScan.setEnabled(true);
            isScanning = false;
        }
    }

    private void addPairedDevices() {
        if (bluetoothAdapter == null) return;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            com.lonewalker.bluetoothmessenger.data.BluetoothDevice appDevice = 
                new com.lonewalker.bluetoothmessenger.data.BluetoothDevice(device);
            
            boolean exists = false;
            for (com.lonewalker.bluetoothmessenger.data.BluetoothDevice d : availableDevices) {
                if (d.getAddress().equals(device.getAddress())) {
                    exists = true;
                    break;
                }
            }
            
            if (!exists) {
                availableDevices.add(appDevice);
            }
        }
        availableDevicesAdapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        if (availableDevices.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    private void onDeviceSelected(com.lonewalker.bluetoothmessenger.data.BluetoothDevice appDevice) {
        Log.d(TAG, "Device selected: " + appDevice.getName() + " - " + appDevice.getAddress());
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "BluetoothAdapter is null");
            Toast.makeText(this, "خطا: بلوتوث در دسترس نیست!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(appDevice.getAddress());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    Toast.makeText(this, "مجوز اتصال بلوتوث لازم است!", Toast.LENGTH_SHORT).show();
                    checkPermissions();
                    return;
                }
            }
            
            Log.d(TAG, "Device bond state: " + device.getBondState());
            
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "Device already paired, going to chat");
                goToChat(device);
            } else {
                Log.d(TAG, "Starting pairing process");
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(this, "در حال pair شدن...", "لطفا صبر کنید...", true, false);
                
                boolean bondStarted = device.createBond();
                if (!bondStarted) {
                    Log.e(TAG, "Failed to start bonding process");
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(this, "شروع فرآیند pairing با خطا مواجه شد!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Bonding process started successfully");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDeviceSelected: " + e.getMessage(), e);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(this, "خطا در انتخاب دستگاه: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void goToChat(BluetoothDevice device) {
        try {
            Log.d(TAG, "goToChat called with device: " + (device != null ? device.getName() : "null"));
            
            if (device == null) {
                Log.e(TAG, "Device is null in goToChat");
                Toast.makeText(this, "خطا: دستگاه نامعتبر است!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();
            
            if (deviceName == null) deviceName = "دستگاه ناشناس";
            if (deviceAddress == null) {
                Log.e(TAG, "Device address is null");
                Toast.makeText(this, "خطا: آدرس دستگاه نامعتبر است!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "Starting ChatActivity with device: " + deviceName + " (" + deviceAddress + ")");
            
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("device_name", deviceName);
            intent.putExtra("device_address", deviceAddress);
            startActivity(intent);
            
            Log.d(TAG, "ChatActivity started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting ChatActivity: " + e.getMessage(), e);
            Toast.makeText(this, "خطا در شروع چت: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onChatSelected(com.lonewalker.bluetoothmessenger.data.BluetoothDevice appDevice) {
        try {
            Log.d(TAG, "onChatSelected called with device: " + appDevice.getName());
            
            if (appDevice == null) {
                Log.e(TAG, "Device is null in onChatSelected");
                Toast.makeText(this, "خطا: دستگاه نامعتبر است!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // بررسی مجوزها
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "BLUETOOTH_CONNECT permission not granted");
                    Toast.makeText(this, "مجوز اتصال بلوتوث لازم است!", Toast.LENGTH_SHORT).show();
                    checkPermissions();
                    return;
                }
            }
            
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(appDevice.getAddress());
            
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "Device already paired, going to chat");
                goToChat(device);
            } else {
                Log.d(TAG, "Starting pairing process for chat");
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                progressDialog = ProgressDialog.show(this, "در حال pair شدن...", "لطفا صبر کنید...", true, false);
                
                boolean bondStarted = device.createBond();
                if (!bondStarted) {
                    Log.e(TAG, "Failed to start bonding process");
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(this, "شروع فرآیند pairing با خطا مواجه شد!", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Bonding process started successfully");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onChatSelected: " + e.getMessage(), e);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(this, "خطا در انتخاب دستگاه: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Bluetooth enabled");
                updateBluetoothStatus();
            } else {
                Log.d(TAG, "Bluetooth enable denied");
                Toast.makeText(this, "فعال‌سازی بلوتوث رد شد!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_DISCOVERABLE) {
            if (resultCode != 0) {
                isDiscoverable = true;
                Log.d(TAG, "Device is now discoverable");
                Toast.makeText(this, "دستگاه به مدت ۵ دقیقه قابل مشاهده است.", Toast.LENGTH_SHORT).show();
            } else {
                isDiscoverable = false;
                Log.d(TAG, "Discoverable request denied");
                Toast.makeText(this, "درخواست قابل مشاهده شدن رد شد!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.w(TAG, "Permission denied: " + permissions[i]);
                }
            }
            if (allGranted) {
                Log.d(TAG, "All permissions granted");
                updateBluetoothStatus();
            } else {
                Log.w(TAG, "Some permissions denied");
                Toast.makeText(this, "برخی مجوزها رد شدند. عملکرد برنامه محدود خواهد بود.", Toast.LENGTH_LONG).show();
                updateBluetoothStatus();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);
        if (bluetoothAdapter != null) updateBluetoothStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(receiver);
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private boolean shouldShowDevice(String deviceName, BluetoothDevice device) {
        if (deviceName == null) return false;
        
        // تبدیل به حروف کوچک برای مقایسه بهتر
        String lowerName = deviceName.toLowerCase();
        
        // کلمات کلیدی که نشان‌دهنده موبایل هستند
        String[] phoneKeywords = {
            "phone", "mobile", "samsung", "huawei", "xiaomi", "oppo", "vivo", 
            "oneplus", "nokia", "lg", "motorola", "sony", "htc", "google", 
            "pixel", "iphone", "ipad", "galaxy", "note", "edge", "plus", "pro"
        };
        
        // کلمات کلیدی که باید فیلتر شوند (دستگاه‌های غیر موبایل)
        String[] excludeKeywords = {
            "speaker", "headphone", "headset", "earbud", "airpods", "buds",
            "audio", "sound", "music", "radio", "tv", "television", "monitor",
            "printer", "keyboard", "mouse", "camera", "watch", "band", "fitbit",
            "car", "vehicle", "bike", "bicycle", "laptop", "computer", "pc",
            "tablet", "tab", "book", "pad", "console", "game", "controller"
        };
        
        // بررسی کلمات مستثنی
        for (String exclude : excludeKeywords) {
            if (lowerName.contains(exclude)) {
                Log.d(TAG, "Device excluded due to keyword: " + exclude);
                return false;
            }
        }
        
        // بررسی کلمات موبایل
        for (String phone : phoneKeywords) {
            if (lowerName.contains(phone)) {
                Log.d(TAG, "Device included due to keyword: " + phone);
                return true;
            }
        }
        
        // اگر نام دستگاه شامل اعداد باشد (معمولاً موبایل‌ها)
        if (deviceName.matches(".*\\d+.*")) {
            Log.d(TAG, "Device included due to numbers in name");
            return true;
        }
        
        // اگر نام دستگاه خیلی کوتاه باشد (معمولاً موبایل‌ها)
        if (deviceName.length() <= 15) {
            Log.d(TAG, "Device included due to short name");
            return true;
        }
        
        Log.d(TAG, "Device filtered out: " + deviceName);
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}