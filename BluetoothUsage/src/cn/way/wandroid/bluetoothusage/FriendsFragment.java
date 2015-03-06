package cn.way.wandroid.bluetoothusage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.way.wandroid.bluetooth.BluetoothManager;
import cn.way.wandroid.bluetooth.BluetoothManager.BluetoothClientConnection;
import cn.way.wandroid.bluetooth.BluetoothManager.BluetoothConnectionListener;
import cn.way.wandroid.bluetooth.BluetoothManager.ConnectionState;
import cn.way.wandroid.bluetooth.BluetoothManager.DeviceState;
import cn.way.wandroid.bluetooth.BluetoothManager.DeviceStateListener;
import cn.way.wandroid.bluetooth.BluetoothManager.DiscoveryListener;
import cn.way.wandroid.utils.Toaster;

@SuppressLint({ "InflateParams", "NewApi" })
public class FriendsFragment extends Fragment {
	private ViewGroup view;//主视图
	private ListView boundedListView;//已经配对列表
	private ArrayAdapter<BluetoothDevice> adapterBounded;
	private ListView foundListView;//附近设备列表
	private ArrayAdapter<BluetoothDevice> adapterFound;
	private ArrayList<BluetoothDevice> friends = new ArrayList<BluetoothDevice>();
	private ArrayList<BluetoothDevice> nearbyDevices = new ArrayList<BluetoothDevice>();
	private BluetoothManager bluetoothManager;
	
	private HashMap<String,BluetoothClientConnection> conns = new HashMap<String, BluetoothManager.BluetoothClientConnection>() ;
	private Button searchBtn;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.bluetooth_im_page_friends, null);
		
		view.findViewById(R.id.discoverableBtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BluetoothManager.requestDiscoverable(getActivity());
			}
		});
		searchBtn = (Button) view.findViewById(R.id.searchBtn);
		searchBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doDescovery();
			}
		});
		
		boundedListView = (ListView) view.findViewById(R.id.im_friends_list);
	
		adapterBounded = new ArrayAdapter<BluetoothDevice>(getActivity(), 0){
			@Override
			public int getCount() {
				return friends.size();
			}
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = convertView;
				if (view == null) {
					view = getActivity().getLayoutInflater().inflate(R.layout.bluetooth_im_list_friends_cell, null);
				}
				ViewHolder holder = (ViewHolder) view.getTag();
				if (holder==null) {
					holder = new ViewHolder();
					holder.nameTV = (TextView) view.findViewById(R.id.name);
					holder.stateTV = (TextView) view.findViewById(R.id.state);
					view.setTag(holder);
				}
				BluetoothDevice bd = friends.get(position);
				holder.nameTV.setText(bd.getName());
				holder.stateTV.setText(BluetoothManager.getBondState(bd));
				return view;
			}
			class ViewHolder{
				TextView nameTV;//可设置备注
				TextView stateTV;//对好友可见|对附近所有人可见
			}
		};
		boundedListView.setAdapter(adapterBounded);
		boundedListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				final BluetoothDevice bd = friends.get(position);
				deviceConnectToServer(bd);
			}
		});
		
		
		//////////////////////////////////////////////////////////////////////////////////////
		
		foundListView = (ListView) view.findViewById(R.id.im_discoveries_list);
		adapterFound = new ArrayAdapter<BluetoothDevice>(getActivity(), 0){
			@Override
			public int getCount() {
				return nearbyDevices.size();
			}
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = convertView;
				if (view == null) {
					view = getActivity().getLayoutInflater().inflate(R.layout.bluetooth_im_list_friends_cell, null);
				}
				ViewHolder holder = (ViewHolder) view.getTag();
				if (holder==null) {
					holder = new ViewHolder();
					holder.nameTV = (TextView) view.findViewById(R.id.name);
					holder.stateTV = (TextView) view.findViewById(R.id.state);
					view.setTag(holder);
				}
				BluetoothDevice bd = nearbyDevices.get(position);
				holder.nameTV.setText(bd.getName()+"");
				holder.stateTV.setText(bd.getAddress());
				return view;
			}
			class ViewHolder{
				TextView nameTV;//可设置备注
				TextView stateTV;//对好友可见|对附近所有人可见
			}
		};
		foundListView.setAdapter(adapterFound);
		foundListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				BluetoothDevice bd = nearbyDevices.get(position);
				deviceConnectToServer(bd);
			}
		});
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (view!=null&&view.getParent()!=null) {
			((ViewGroup)view.getParent()).removeView(view);
		}
		return view;
	}
	@Override
	public void onResume() {
		super.onResume();
		updateBoundedListView();	
	}
	public void updateBoundedListView(){
		friends.clear();
		if (getBluetoothManager()!=null) {
			friends.addAll(getBluetoothManager().getBondedDevices());
		}
		adapterBounded.notifyDataSetChanged();
	}
	private void deviceConnectToServer(BluetoothDevice bd){
		if(getBluetoothManager()!=null){
			Toast.makeText(getActivity(), "准备创建连接："+bd, 0).show();
			if (conns.size()==0||conns.get(bd.getAddress())==null) {
				final BluetoothClientConnection bcc = 
				bluetoothManager.createClientConnection();
				doConnect(bcc, bd);
				conns.put(bd.getAddress(), bcc);
			}else{
				BluetoothClientConnection bcc = conns.get(bd.getAddress());
				if (bcc.getState()==ConnectionState.CONNECTED) {
					bcc.write("我是客户端消息！");
				}else if (bcc.getState()==ConnectionState.DISCONNECTED){
					doConnect(bcc, bd);
				}
				getActivity().getActionBar().setTitle("客户端连接状态："+bcc.getState());
			}
		}else{
			Toast.makeText(getActivity(), "无法连接："+bd, 0).show();
		}
	}
	private void doConnect(final BluetoothClientConnection bcc,BluetoothDevice bd){
		bcc.connect(MainActivity.M_UUID, bd, MainActivity.IS_SECURE,new BluetoothConnectionListener() {
			
			@Override
			public void onConnectionStateChanged(ConnectionState state,
					int errorCode) {
				getActivity().getActionBar().setTitle("客户端连接状态："+state);
				if (state == ConnectionState.CONNECTED) {
					bcc.write(new String("我是客户端消息"));
				}
			}
			@Override
			public void onDataReceived(byte[] data) {
				Toast.makeText(getActivity(), "DataReceived:    "+new String(data), 0).show();
			}
		});
	}
	private void doDescovery(){
		if (!getBluetoothManager().isEnabled()) {
			getBluetoothManager().setDeviceStateListener(new DeviceStateListener() {
				@Override
				public void onStateChanged(DeviceState state) {
					if (state == DeviceState.ON) {
						doDescovery();
					}
					Toaster.instance(getActivity()).setup("state : "+state).show();
					if (state == DeviceState.OFF) {
						searchBtn.setText("查找");
					}
				}
			});
			getBluetoothManager().enable();
			return;
		}
		if (getBluetoothManager()!=null) {
			searchBtn.setText("正在查找...");
			getBluetoothManager().startDiscovery(new DiscoveryListener() {
				@Override
				public void onFinished() {
					if (bluetoothManager!=null) {
						searchBtn.setText("查找");
					}
				}
				
				@Override
				public void onDevicesChanged(Collection<BluetoothDevice> devices) {
					if (bluetoothManager!=null) {
						nearbyDevices.clear();
						nearbyDevices.addAll(devices);
						adapterFound.notifyDataSetChanged();
					}
				}
			});
		}
	}
	
	public BluetoothManager getBluetoothManager() {
		return bluetoothManager;
	}
	public void setBluetoothManager(BluetoothManager manager) {
		this.bluetoothManager = manager;
	}
}
