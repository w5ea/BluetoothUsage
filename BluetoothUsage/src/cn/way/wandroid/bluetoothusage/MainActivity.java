package cn.way.wandroid.bluetoothusage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.UUID;

import android.location.GpsStatus.NmeaListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.style.TtsSpan.DigitsBuilder;
import android.util.Log;
import android.widget.Toast;
import cn.way.wandroid.bluetooth.BluetoothManager;
import cn.way.wandroid.bluetooth.Utils;
import cn.way.wandroid.bluetooth.BluetoothManager.BluetoothConnectionListener;
import cn.way.wandroid.bluetooth.BluetoothManager.BluetoothServerConnection;
import cn.way.wandroid.bluetooth.BluetoothManager.BluetoothSupportException;
import cn.way.wandroid.bluetooth.BluetoothManager.ConnectionState;
import cn.way.wandroid.bluetooth.BluetoothManager.DeviceState;
import cn.way.wandroid.bluetooth.BluetoothManager.DeviceStateListener;
import cn.way.wandroid.utils.DigitalTrans;
import cn.way.wandroid.utils.Toaster;

public class MainActivity extends FragmentActivity {

	private BluetoothManager bluetoothManager;
	public static final UUID M_UUID =
	        UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a67");
	public static boolean IS_SECURE = true;
	@Override
	protected void onDestroy() {
		if (bluetoothManager!=null) {
			bluetoothManager.release();
		}
		super.onDestroy();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		int minValue = 0;
		int maxValue = Short.MAX_VALUE*2+1;
		String uuid = new cn.way.wandroid.bluetooth.UUID(Integer.toHexString(minValue),true).toString();
		Log.d("test", uuid);
//		Log.d("test", Integer.toHexString(value));
		try {
			bluetoothManager = BluetoothManager.instance(this);
			bluetoothManager.setDeviceStateListener(new DeviceStateListener() {
				@Override
				public void onStateChanged(DeviceState state) {
					switch (state) {
					case TURNING_ON:
						getActionBar().setTitle("正在开启蓝牙");
						break;
					case ON:
						getActionBar().setTitle("蓝牙已经开启");
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						FriendsFragment hf = new FriendsFragment();
						hf.setBluetoothManager(bluetoothManager);
						ft.replace(R.id.bluetooth_page_main_root, hf);
						ft.commit();
					
						final BluetoothServerConnection bsc = bluetoothManager.getServerConnection();
						getActionBar().setTitle("服务器已经开启");
						bsc.connect(M_UUID, IS_SECURE,new BluetoothConnectionListener() {
							@Override
							public void onConnectionStateChanged(ConnectionState state,
									int errorCode) {
								getActionBar().setTitle(state.toString()+"|"+bsc.getClientDevice().getName());
							}

							@Override
							public void onDataReceived(byte[] data) {
								Toast.makeText(MainActivity.this, "服务器收到:    "+new String(data), 0).show();
								bsc.write("服务器向外发送的消息");
							}
						});
						break;
					case OFF:
						break;
					case TURNING_OFF:
						break;
					}
				}
			});
			bluetoothManager.enable();
		} catch (BluetoothSupportException e) {
			Toaster.instance(this).setup(e.toString()).show();
		}
	}
}
