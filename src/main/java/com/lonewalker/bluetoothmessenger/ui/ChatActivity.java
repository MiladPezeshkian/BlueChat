package com.lonewalker.bluetoothmessenger.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lonewalker.bluetoothmessenger.R;
import com.lonewalker.bluetoothmessenger.adapters.MessageAdapter;
import com.lonewalker.bluetoothmessenger.data.AppDatabase;
import com.lonewalker.bluetoothmessenger.data.Message;
import com.lonewalker.bluetoothmessenger.data.MessageDao;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    private static final String APP_NAME = "BlueChat";
    private static final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private String deviceName;
    private String deviceAddress;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private AcceptThread acceptThread;
    private boolean isServer = false;
    private boolean isConnected = false;
    private boolean isConnecting = false;

    private TextView chatTitle;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private EditText messageInput;
    private ImageButton sendButton;
    private View connectionStatusLayout;
    private TextView connectionStatusText;
    private MaterialButton retryButton;
    private ProgressDialog progressDialog;
    private MessageDao messageDao;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ImageButton voiceCallToggleButton;
    // Voice call fields
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread voiceSendThread;
    private Thread voiceReceiveThread;
    private volatile boolean isVoiceCallActive = false;
    private static final int SAMPLE_RATE = 16000;
    private static final int AUDIO_PACKET_SIZE = 1024;
    private static final int REQUEST_RECORD_AUDIO = 1001;
    private static final int PACKET_HEADER_SIZE = 3; // 1 byte type + 2 bytes length
    private static final int JITTER_BUFFER_SIZE = 8; // 8 packets = 320ms
    private static final int JITTER_BUFFER_DELAY_MS = 60;
    private static final int QUALITY_HIGH = 16000;
    private static final int QUALITY_LOW = 8000;
    private int currentSampleRate = QUALITY_HIGH;
    private int consecutiveFailures = 0;
    private long lastPacketTime = 0;
    private static final int MAX_FAILURES = 5;
    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_chat);

            deviceName = getIntent().getStringExtra("device_name");
            deviceAddress = getIntent().getStringExtra("device_address");
            if (deviceName == null) deviceName = "دستگاه ناشناس";
            
            Log.d(TAG, "ChatActivity onCreate - device: " + deviceName + " (" + deviceAddress + ")");

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Log.e(TAG, "BluetoothAdapter is null");
                Toast.makeText(this, "بلوتوث در این دستگاه پشتیبانی نمی‌شود!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            messageDao = AppDatabase.getInstance(this).messageDao();
            executorService = Executors.newCachedThreadPool();
            mainHandler = new Handler(Looper.getMainLooper());

            initializeViews();
            setupToolbar();
            setupRecyclerView();
            loadMessages();
            
            // افزایش delay برای اطمینان از کامل شدن pairing
            mainHandler.postDelayed(this::establishConnection, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in ChatActivity onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "خطا در شروع چت: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        chatTitle = findViewById(R.id.chatTitle);
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        connectionStatusLayout = findViewById(R.id.connectionStatusLayout);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        retryButton = findViewById(R.id.retryButton);
        voiceCallToggleButton = findViewById(R.id.voiceCallToggleButton);

        chatTitle.setText(deviceName);
        sendButton.setOnClickListener(v -> sendMessage());
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
        
        if (retryButton != null) {
            retryButton.setOnClickListener(v -> establishConnection());
        }
        voiceCallToggleButton.setEnabled(false);
        voiceCallToggleButton.setOnClickListener(v -> {
            if (!isVoiceCallActive) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                } else {
                    startVoiceCall();
                }
            } else {
                stopVoiceCall();
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(deviceName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(messages);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        executorService.execute(() -> {
            try {
                List<Message> loadedMessages = messageDao.getMessagesByDevice(deviceAddress);
                mainHandler.post(() -> {
                    messages.clear();
                    messages.addAll(loadedMessages);
                    messageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading messages: " + e.getMessage());
            }
        });
    }

    private void establishConnection() {
        if (isConnecting) return;
        
        isConnecting = true;
        isConnected = false;
        
        // بررسی مجوزها
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "مجوز اتصال بلوتوث لازم است!", Toast.LENGTH_SHORT).show();
                isConnecting = false;
                return;
            }
        }
        
        progressDialog = ProgressDialog.show(this, "در حال اتصال", "در حال برقراری اتصال...", true, false);
        
        // تایمر برای timeout
        mainHandler.postDelayed(() -> {
            if (isConnecting && !isConnected) {
                Log.d(TAG, "Connection timeout");
                isConnecting = false;
                onConnectionFailed();
            }
        }, 25000); // 25 ثانیه timeout
        
        // ابتدا سعی می‌کنیم به عنوان کلاینت متصل شویم
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Attempting to connect as client to: " + deviceAddress);
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                
                // بررسی وضعیت pairing
                if (device.getBondState() != android.bluetooth.BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "Device is not paired yet");
                    mainHandler.post(() -> {
                        isConnecting = false;
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(ChatActivity.this, "دستگاه هنوز جفت نشده است. لطفا دوباره تلاش کنید.", Toast.LENGTH_LONG).show();
                        onConnectionFailed();
                    });
                    return;
                }
                
                // تلاش اول - اتصال مستقیم
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(APP_UUID);
                    bluetoothSocket.connect();
                    isServer = false;
                    Log.d(TAG, "Client connection successful on first try");
                    mainHandler.post(() -> {
                        isConnecting = false;
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        onConnectionEstablished();
                    });
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "First connection attempt failed: " + e.getMessage());
                }
                
                // تلاش دوم - اتصال با روش متفاوت
                try {
                    if (bluetoothSocket != null) {
                        bluetoothSocket.close();
                    }
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(APP_UUID);
                    bluetoothSocket.connect();
                    isServer = false;
                    Log.d(TAG, "Client connection successful on second try");
                    mainHandler.post(() -> {
                        isConnecting = false;
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        onConnectionEstablished();
                    });
                    return;
                } catch (IOException e) {
                    Log.e(TAG, "Second connection attempt failed: " + e.getMessage());
                }
                
                // اگر هر دو تلاش شکست خورد، به عنوان سرور شروع می‌کنیم
                Log.d(TAG, "Both client attempts failed, starting as server");
                mainHandler.post(this::startAsServer);
                
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in connection: " + e.getMessage(), e);
                mainHandler.post(this::startAsServer);
            }
        });
    }

    private void startAsServer() {
        Log.d(TAG, "Starting as server");
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage("در انتظار اتصال...");
        }
        isServer = true;
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    private void onConnectionEstablished() {
        isConnected = true;
        connectionStatusLayout.setVisibility(View.GONE);
        connectionStatusText.setText("متصل");
        sendButton.setEnabled(true);
        messageInput.setEnabled(true);
        Toast.makeText(this, "اتصال با " + deviceName + " برقرار شد", Toast.LENGTH_SHORT).show();
        
        // شروع گوش دادن به پیام‌ها
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        voiceCallToggleButton.setEnabled(true);
    }

    private void onConnectionFailed() {
        isConnecting = false;
        isConnected = false;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        
        connectionStatusLayout.setVisibility(View.VISIBLE);
        connectionStatusText.setText("اتصال برقرار نشد");
        if (retryButton != null) {
            retryButton.setVisibility(View.VISIBLE);
        }
        sendButton.setEnabled(false);
        messageInput.setEnabled(false);
        
        Toast.makeText(this, "اتصال با " + deviceName + " برقرار نشد. دوباره تلاش کنید.", Toast.LENGTH_LONG).show();
    }

    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty() || !isConnected || connectedThread == null) {
            if (!isConnected) {
                Toast.makeText(this, "اتصال برقرار نیست!", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Message message = new Message();
        message.setDeviceAddress(deviceAddress);
        message.setContent(messageText);
        message.setTimestamp(System.currentTimeMillis());
        message.setSent(true);
        message.setSenderAddress(bluetoothAdapter.getAddress());
        message.setReceiverAddress(deviceAddress);

        // ارسال پیام
        connectedThread.write(messageText.getBytes());
        
        // ذخیره در دیتابیس
        executorService.execute(() -> {
            try {
                messageDao.insert(message);
                mainHandler.post(() -> {
                    messages.add(message);
                    messageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                    messageInput.setText("");
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving message: " + e.getMessage());
            }
        });
    }

    private void receiveMessage(String content) {
        Message message = new Message();
        message.setDeviceAddress(deviceAddress);
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());
        message.setSent(false);
        message.setSenderAddress(deviceAddress);
        message.setReceiverAddress(bluetoothAdapter.getAddress());

        // ذخیره در دیتابیس
        executorService.execute(() -> {
            try {
                messageDao.insert(message);
                mainHandler.post(() -> {
                    messages.add(message);
                    messageAdapter.notifyDataSetChanged();
                    scrollToBottom();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving received message: " + e.getMessage());
            }
        });
    }

    private void scrollToBottom() {
        if (messages.size() > 0) {
            messagesRecyclerView.smoothScrollToPosition(messages.size() - 1);
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        mainHandler.post(ChatActivity.this::onConnectionFailed);
                        return;
                    }
                }
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
                Log.d(TAG, "Server socket created successfully");
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
                mainHandler.post(ChatActivity.this::onConnectionFailed);
                return;
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            long startTime = System.currentTimeMillis();
            long timeout = 30000; // 30 seconds timeout for server
            
            while (!isConnected && !Thread.interrupted() && (System.currentTimeMillis() - startTime) < timeout) {
                try {
                    if (serverSocket != null) {
                        Log.d(TAG, "Waiting for client connection...");
                        socket = serverSocket.accept();
                        Log.d(TAG, "Client connected: " + socket.getRemoteDevice().getName());
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    synchronized (ChatActivity.this) {
                        bluetoothSocket = socket;
                        isConnected = true;
                    }
                    mainHandler.post(() -> {
                        isConnecting = false;
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        onConnectionEstablished();
                    });
                    break;
                }
            }
            
            // اگر اتصال برقرار نشد
            if (!isConnected) {
                Log.d(TAG, "Server connection failed - timeout or interrupted");
                mainHandler.post(ChatActivity.this::onConnectionFailed);
            }
        }

        public void cancel() {
            try {
                if (serverSocket != null) {
                    serverSocket.close();
                    Log.d(TAG, "Server socket closed");
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer = new byte[1024];

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                while (isConnected) {
                    Packet packet = readPacket();
                    if (packet == null) break;
                    if (packet.type == 0x01) {
                        String received = new String(packet.data);
                        mainHandler.post(() -> receiveMessage(received));
                    }
                    // audio packets are handled in the voice receive thread
                }
            } catch (Exception e) {
                Log.e(TAG, "Connection lost", e);
                mainHandler.post(() -> {
                    isConnected = false;
                    Toast.makeText(ChatActivity.this, "اتصال قطع شد", Toast.LENGTH_SHORT).show();
                });
            }
        }

        public void write(byte[] bytes) {
            try {
                byte[] header = new byte[PACKET_HEADER_SIZE];
                header[0] = 0x01; // text
                int len = bytes.length;
                header[1] = (byte) (len & 0xFF);
                header[2] = (byte) ((len >> 8) & 0xFF);
                mmOutStream.write(header);
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                mainHandler.post(() -> {
                    isConnected = false;
                    Toast.makeText(ChatActivity.this, "اتصال قطع شد", Toast.LENGTH_SHORT).show();
                });
            }
        }

        public void writeAudio(byte[] buffer, int offset, int length) {
            try {
                byte[] header = new byte[PACKET_HEADER_SIZE];
                header[0] = 0x02; // audio
                header[1] = (byte) (length & 0xFF);
                header[2] = (byte) ((length >> 8) & 0xFF);
                mmOutStream.write(header);
                mmOutStream.write(buffer, offset, length);
            } catch (IOException e) {
                Log.e(TAG, "Error sending audio data", e);
            }
        }

        public Packet readPacket() {
            try {
                int type = mmInStream.read();
                if (type == -1) return null;
                int len1 = mmInStream.read();
                int len2 = mmInStream.read();
                if (len1 == -1 || len2 == -1) return null;
                int length = (len2 << 8) | len1;
                if (length <= 0 || length > 8192) return null;
                byte[] data = new byte[length];
                int totalRead = 0;
                while (totalRead < length) {
                    int read = mmInStream.read(data, totalRead, length - totalRead);
                    if (read == -1) return null;
                    totalRead += read;
                }
                return new Packet(type, data);
            } catch (IOException e) {
                Log.e(TAG, "Error reading packet", e);
            }
            return null;
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private static class Packet {
        public int type;
        public byte[] data;
        public Packet(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }

    private void startVoiceCall() {
        isVoiceCallActive = true;
        runOnUiThread(() -> {
            voiceCallToggleButton.setImageResource(R.drawable.ic_call_end);
            voiceCallToggleButton.setContentDescription("پایان تماس صوتی");
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
            Toast.makeText(this, "تماس صوتی شروع شد", Toast.LENGTH_SHORT).show();
        });
        startVoiceSendThread();
        startVoiceReceiveThread();
    }

    private void stopVoiceCall() {
        isVoiceCallActive = false;
        runOnUiThread(() -> {
            voiceCallToggleButton.setImageResource(R.drawable.ic_call);
            voiceCallToggleButton.setContentDescription("شروع تماس صوتی");
            messageInput.setEnabled(true);
            sendButton.setEnabled(true);
            Toast.makeText(this, "تماس صوتی پایان یافت", Toast.LENGTH_SHORT).show();
        });
        stopVoiceSendThread();
        stopVoiceReceiveThread();
    }

    private void startConnectionMonitor() {
        new Thread(() -> {
            while (isVoiceCallActive && isConnected) {
                try {
                    Thread.sleep(1000);
                    long now = System.currentTimeMillis();
                    if (now - lastPacketTime > CONNECTION_TIMEOUT) {
                        consecutiveFailures++;
                        if (consecutiveFailures >= MAX_FAILURES) {
                            // Switch to lower quality
                            if (currentSampleRate == QUALITY_HIGH) {
                                currentSampleRate = QUALITY_LOW;
                                runOnUiThread(() -> Toast.makeText(this, "کیفیت صدا کاهش یافت", Toast.LENGTH_SHORT).show());
                                restartAudioThreads();
                            }
                        }
                    } else {
                        consecutiveFailures = 0;
                        // Try to switch back to high quality
                        if (currentSampleRate == QUALITY_LOW && consecutiveFailures == 0) {
                            currentSampleRate = QUALITY_HIGH;
                            runOnUiThread(() -> Toast.makeText(this, "کیفیت صدا بهبود یافت", Toast.LENGTH_SHORT).show());
                            restartAudioThreads();
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void restartAudioThreads() {
        stopVoiceSendThread();
        stopVoiceReceiveThread();
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        startVoiceSendThread();
        startVoiceReceiveThread();
    }

    private void startVoiceSendThread() {
        voiceSendThread = new Thread(() -> {
            try {
                Log.d(TAG, "Starting voice send thread");
                if (audioRecord != null) {
                    audioRecord.release();
                    audioRecord = null;
                }
                int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBuf, AUDIO_PACKET_SIZE * 2));
                byte[] buffer = new byte[AUDIO_PACKET_SIZE];
                audioRecord.startRecording();
                Log.d(TAG, "AudioRecord started recording");
                while (isVoiceCallActive && isConnected) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        Log.d(TAG, "Sending audio packet, size: " + read);
                        connectedThread.writeAudio(buffer, 0, read);
                    }
                }
                audioRecord.stop();
                Log.d(TAG, "Voice send thread stopped");
            } catch (Exception e) {
                Log.e(TAG, "Voice send error: " + e.getMessage());
            }
        });
        voiceSendThread.start();
    }

    private void stopVoiceSendThread() {
        if (voiceSendThread != null) {
            voiceSendThread.interrupt();
            voiceSendThread = null;
        }
        if (audioRecord != null) {
            try { audioRecord.release(); } catch (Exception ignored) {}
            audioRecord = null;
        }
    }

    private void startVoiceReceiveThread() {
        voiceReceiveThread = new Thread(() -> {
            try {
                Log.d(TAG, "Starting voice receive thread");
                if (audioTrack != null) {
                    audioTrack.release();
                    audioTrack = null;
                }
                int minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minBuf, AUDIO_PACKET_SIZE * 2),
                        AudioTrack.MODE_STREAM);
                audioTrack.play();
                Log.d(TAG, "AudioTrack started playing");
                while (isVoiceCallActive && isConnected && connectedThread != null) {
                    Packet packet = connectedThread.readPacket();
                    if (packet != null && packet.type == 0x02 && packet.data.length > 0) {
                        Log.d(TAG, "Received audio packet, size: " + packet.data.length);
                        audioTrack.write(packet.data, 0, packet.data.length);
                    }
                }
                audioTrack.stop();
                Log.d(TAG, "Voice receive thread stopped");
            } catch (Exception e) {
                Log.e(TAG, "Voice receive error: " + e.getMessage());
            }
        });
        voiceReceiveThread.start();
    }

    private void stopVoiceReceiveThread() {
        if (voiceReceiveThread != null) {
            voiceReceiveThread.interrupt();
            voiceReceiveThread = null;
        }
        if (audioTrack != null) {
            try { audioTrack.release(); } catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
        stopVoiceSendThread();
        stopVoiceReceiveThread();
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceCall();
            } else {
                Toast.makeText(this, "مجوز میکروفون لازم است تا تماس صوتی برقرار شود!", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
} 