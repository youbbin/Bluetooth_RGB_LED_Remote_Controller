package com.example.rgbledcontrol_bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 100;
    static final int REQUEST_PERMISSIONS = 101;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //블루투스 어댑터
    BluetoothDevice bluetoothDevice; //블루투스 장치
    Set<BluetoothDevice> pairedDevices; //페어링된 장치 집합
    Set<BluetoothDevice> unpairedDevices = new HashSet<>(); // 페어링되지 않은 장치 집합
    ArrayAdapter<String> btArrayAdapter; //기기 검색에 필요한 어댑터
    ArrayList<String> deviceAddressArray;
    BluetoothSocket bluetoothSocket; //블루투스 소켓
    List<String> unpairedList = new ArrayList<>(); // 페어링되지 않은 장치이름 목록
    OutputStream outputStream;
    InputStream inputStream;
    boolean paired = false;
    byte[] colors = new byte[6]; // 블루투스 기기로 보낼 byte 배열 데이터
    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    ConnectedThread connectedThread;

    ColorPicker picker;
    SeekBar seekBar;
    ToggleButton toggleButton;
    Button sendButton;
    Switch modeSwitch;
    int brightness=100;
    TextView connectText;
    int ledStatus;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // 브로드캐스트 리시버
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {   // 전달받은 액션이 ACTION_FOUND 라면
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    if(device.getName() != null) {
                        add(device);
                    }
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get permission
        String[] permission_list = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(MainActivity.this, permission_list, 1);

        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();

        connectText=(TextView) findViewById(R.id.connectText); //연결 상태 출력하는 텍스트뷰

        // 초기 색상 연두색
        colors[1]=(byte)128;
        colors[2]=(byte)255;
        colors[3]=(byte)0;
        colors[4]=(byte)100;
        colors[5]=(byte)0;

        // On,Off 토글 버튼 클릭 이벤트
        toggleButton=(ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){

                if(toggleButton.isChecked()){
                    Toast.makeText(MainActivity.this,"ON",Toast.LENGTH_SHORT).show();
                    ledStatus=1;
                    sendColor();
                }
                else{
                    Toast.makeText(MainActivity.this,"OFF",Toast.LENGTH_SHORT).show();
                    ledStatus=0;
                    sendColor();
                }

            }
        });

        modeSwitch=(Switch)findViewById(R.id.modeSwitch); // 인체 감지 모드 on,off 스위치
        modeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    colors[5]=(byte) 1; // 인체 감지 모드 on
                    Toast.makeText(MainActivity.this,"인체 감지 모드 On",Toast.LENGTH_SHORT).show();
                }

                else {
                    colors[5]=(byte) 0; // 인체 감지 모드 off
                    Toast.makeText(MainActivity.this,"인체 감지 모드 Off",Toast.LENGTH_SHORT).show();
                }
            }
        });
        picker=(ColorPicker) findViewById(R.id.picker);
        SaturationBar saturationBar=(SaturationBar) findViewById(R.id.saturationBar);
        picker.addSaturationBar(saturationBar);
        picker.setShowOldCenterColor(false);

        //led의 세기를 조절하는 시크바
        seekBar=(SeekBar)findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                brightness=seekBar.getProgress(); //brightness : 선택한 밝기 (0~100)
            }

            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // SEND COLOR 버튼 클릭 이벤트
        sendButton=(Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendColor();
                Toast.makeText(MainActivity.this,"색상 전송",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //메뉴
        super.onCreateOptionsMenu(menu);
        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemPaired: //페어링된 기기 목록
                if(bluetoothAdapter.isEnabled())
                    selectPairedDevice();
                return true;
            case R.id.itemSearch: //기기 검색
                if(bluetoothAdapter.isEnabled())
                    selectUnpairedDevice();
                return true;
        }
        return false;
    }

    private void selectPairedDevice() { //페어링된 기기 선택
        pairedDevices = bluetoothAdapter.getBondedDevices();

        AlertDialog.Builder builderPaired = new AlertDialog.Builder(MainActivity.this);
        builderPaired.setTitle("Paired Devices");
        builderPaired.setNegativeButton("취소", null);
        List<String> pairedList = new ArrayList<>(); //페어링된 기기 목록
        for (BluetoothDevice device : pairedDevices) {
            pairedList.add(device.getName());
        }

        final CharSequence[] devices = pairedList.toArray(new CharSequence[pairedList.size()]);
        builderPaired.setItems(devices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bluetoothAdapter.cancelDiscovery();
                paired = true;
                connectDevice(devices[which].toString(), true); // 기기 연결

            }
        });
        builderPaired.setCancelable(false);
        AlertDialog digPaired = builderPaired.create();
        digPaired.show();
    }

    private void selectUnpairedDevice() {
        // 이미 검색 중이라면 검색을 종료하고, 다시 검색 시작
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        AlertDialog.Builder builderSearch = new AlertDialog.Builder(this); // 다이얼로그 생성
        builderSearch.setTitle("Search Devices");
        builderSearch.setNegativeButton("취소", null);
        btArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, unpairedList);

        builderSearch.setAdapter(btArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bluetoothAdapter.cancelDiscovery();
                String name = btArrayAdapter.getItem(which);
                unpairedList.remove(name);
                paired = false;
                connectDevice(name, false);
            }
        });
        AlertDialog dialog = builderSearch.create(); // 빌더로 다이얼로그 만들기
        dialog.show();
    }

    private BluetoothDevice getPairedDevice(String name) { //페어링된 기기 가져오기
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : pairedDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }
    // 페어링되지 않은 장치 목록에서 디바이스 장치 가져오기
    private BluetoothDevice getUnpairedDevice(String name) { //페어링되지 않은 기기 가져오기
        BluetoothDevice selectedDevice = null;

        for(BluetoothDevice device : unpairedDevices) {
            if(name.equals(device.getName())) {
                selectedDevice = device;
                break;
            }
        }
        return selectedDevice;
    }

    private void connectDevice(String selectedDeviceName, boolean paired) { // 기기 연결
        final Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what==1) {
                    try{
                        outputStream = bluetoothSocket.getOutputStream();
                        inputStream = bluetoothSocket.getInputStream();
                        Toast.makeText(getApplicationContext(), "연결 완료", Toast.LENGTH_SHORT).show();
                        connectText.setText(bluetoothDevice.getName()+" connected");
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "연결 실패", Toast.LENGTH_SHORT).show();
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread thread = new Thread(new Runnable() {
            public void run() {
                if(paired)
                    bluetoothDevice = getPairedDevice(selectedDeviceName);
                else
                    bluetoothDevice = getUnpairedDevice(selectedDeviceName);
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

                try {
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                    connectedThread=new ConnectedThread(bluetoothSocket);
                    connectedThread.start();
                    mHandler.sendEmptyMessage(1);
                } catch (IOException e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(-1);
                }
            }
        });
        thread.start();
    }
    private void add(BluetoothDevice device) {
        if(!(pairedDevices.contains(device))) { // 페어링된 장치에 없고
            if(unpairedDevices.add(device)) { // 장치가 추가된다면(중복이 아니라면)
                unpairedList.add(device.getName());    // 페어링되지 않은 장치이름 목록에 이름 추가
            }
        }
        btArrayAdapter.notifyDataSetChanged(); // 변화 반영
        Toast.makeText(this, device.getName()+" search" , Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if (inputStream != null)
                inputStream.close(); // 입력 스트림 닫아주기
            if (outputStream != null)
                outputStream.close(); // 출력 스트림 닫아주기
            if (bluetoothSocket != null)
                bluetoothSocket.close(); // 소켓 닫아주기
        } catch(IOException e) {
            e.printStackTrace();
        }
        unregisterReceiver(mReceiver);
    }


    // 블루투스 활성화 여부에 따른 결과
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT) {
            if(resultCode==RESULT_OK) {
                selectPairedDevice();
            } else if(resultCode==RESULT_CANCELED) {
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // 선택한 색상의 RGB 값을 전송
    public void sendColor() {
        int color = picker.getColor(); //ColorPicker에서 선택한 색상 가져옴
        int r, g, b;

        r = Color.red(color);
        g = Color.green(color);
        b = Color.blue(color);

        if(ledStatus==1) //on
            colors[0] = (byte) 1;
        else if(ledStatus==0) //off
            colors[0] = (byte) 0;

        colors[1] = (byte) r;
        colors[2] = (byte) g;
        colors[3] = (byte) b;
        colors[4] = (byte) brightness;

        if (connectedThread != null)
            connectedThread.write(colors);
    }
}