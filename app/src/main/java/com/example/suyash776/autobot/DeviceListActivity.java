package com.example.suyash776.autobot;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
//import com.example.CoolBluetooth_Version_4.R;

public class DeviceListActivity extends Activity {
	// Debugging
	private static final String TAG = "DeviceListActivity";
	private static final boolean D = true;
	
	// Return Intent extra
	public static String EXTRA_DEVICE_ADDRESS = "device_address";

	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.listview);

		// Set result CANCELLED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		// Get the local bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Initialize the button to perform device discovery
	    Button scanButton = (Button) findViewById(R.id.button_scan);
	    Button cancelButton = (Button) findViewById(R.id.button_cancel);
	    scanButton.setVisibility(View.GONE);
	
		doDiscovery();			

		// Initialize array adapters. One for already paired devices and one for newly paired devices
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.simplerow);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this,R.layout.simplerow);

		// Find and set up the ListView for paired devices
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		// Find and set up the ListView for the newly discovered devices
		ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);
		
		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (pairedDevices.size() > 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());
			}
		} else {
			 String noDevices = getResources().getText(R.string.none_paired).toString();
			mPairedDevicesArrayAdapter.add(noDevices);
		}
		scanButton.setVisibility(View.VISIBLE);
		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {				
				mNewDevicesArrayAdapter.clear();
				doDiscovery();
			}
		});
		
		cancelButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				//Create the result intent and include the MAC address
				Intent intent = new Intent();
				
				//Set result and finish the Activity
				setResult(Activity.RESULT_CANCELED, intent);
				finish();				
			}
		});
	}

	// Start discovering nearby devices
	private void doDiscovery() {
		if (D) Log.d(TAG, "doDiscovery()");
		
		setProgressBarIndeterminate(true);	// Indicate scanning in the title
		setTitle(R.string.scanning);
		
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);	// Turn on sub-title for new devices
		
		if (mBtAdapter.isDiscovering()) {	// If we are already discovering, stop it
			mBtAdapter.cancelDiscovery();
		}

		// Request discovery from BluetoothAdapter
		mBtAdapter.startDiscovery();

	}

    @Override
	protected void onDestroy() {
		
		super.onDestroy();
		
		// Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
	}

	// The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                
                setTitle(R.string.select_device);
                String noDevices = getResources().getText(R.string.none_found).toString();
                if (mNewDevicesArrayAdapter.getCount() == 0) {                    
                    mNewDevicesArrayAdapter.add(noDevices);
                } else {
                	mNewDevicesArrayAdapter.remove(noDevices);
                }                                               
            }
        }
    };

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			mBtAdapter.cancelDiscovery();
			
			// Get the device MAC address, which is the last 17 chars in the View
			String info = ((TextView) v).getText().toString();
			String address = info.substring(info.length() - 17);
			
			//Create the result intent and include the MAC address
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
			
			//Set result and finish the Activity
			setResult(Activity.RESULT_OK, intent);
			finish();
		}    	
    };
}
