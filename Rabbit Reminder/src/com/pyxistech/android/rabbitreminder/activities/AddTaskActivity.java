package com.pyxistech.android.rabbitreminder.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pyxistech.android.rabbitreminder.R;
import com.pyxistech.android.rabbitreminder.models.TaskItem;

public class AddTaskActivity extends Activity implements LocationListener {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_task);
		
		if (savedInstanceState == null) {
			getParametersFromBundle(getIntent().getExtras());
		}
		else {
			index = savedInstanceState.getInt("index");
			latitude = savedInstanceState.getDouble("latitude");
			longitude = savedInstanceState.getDouble("longitude");
		}
		
		refreshUi();
	}


	private void getParametersFromBundle(Bundle bundle) {
		if (bundle != null) {
			TaskItem item = bundle.getParcelable("item");
			if( item != null ) {
				text = item.getText();
				
				latitude = item.getLatitude();
				longitude = item.getLongitude();
				
				index = bundle.getInt("index");
			}
		}
	}


	private void refreshUi() {
		Button addTaskButton = (Button) findViewById(R.id.add_task_button);
		EditText editText = (EditText) findViewById(R.id.new_task_text);

		editText.setText(text);
		TextView locationView = (TextView) AddTaskActivity.this.findViewById(R.id.location_display_text);
		locationView.setText("latitude: " + latitude + " - longitude: " + longitude);
		
		addTaskButton.setOnClickListener(listener);
		
		setCurrentGpsLocation(null);
		
		Button setMyLocationButton = (Button) AddTaskActivity.this.findViewById(R.id.set_current_location_button);
		setMyLocationButton.setOnClickListener(setMyLocationButtonListener);
	}

	@Override
	public void setIntent (Intent newIntent) {
		super.setIntent(newIntent);
		
		getParametersFromBundle(newIntent.getExtras());
		refreshUi();
	}
    
    
	OnClickListener setMyLocationButtonListener = new OnClickListener() {
		public void onClick(View v) {
			setCurrentGpsLocation(null);
			
			checkGPSAvailability();
			
			longitude = currentLongitude;
			latitude = currentLatitude;
			
			TextView locationView = (TextView) AddTaskActivity.this.findViewById(R.id.location_display_text);
			locationView.setText("latitude: " + latitude + " - longitude: " + longitude);
		}

		private void checkGPSAvailability() {
			if (currentLatitude == null || currentLongitude == null) {
				Toast toast = Toast.makeText(getApplicationContext(), R.string.gps_availability_alert, Toast.LENGTH_SHORT);
				toast.show();
			}
		}
	};
	
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
		outState.putInt("index", index);
		outState.putDouble("latitude", latitude);
		outState.putDouble("longitude", longitude);
    }
	
	private OnClickListener listener = new OnClickListener() {
		public void onClick(View v) {
			Intent data = new Intent();
			
			EditText editText = (EditText) findViewById(R.id.new_task_text);
			String text = editText.getText().toString();
			data.putExtra("newTaskText", text);
			data.putExtra("latitude", latitude);
			data.putExtra("longitude", longitude);
			data.putExtra("index", index);

			AddTaskActivity.this.setResult(Activity.RESULT_OK, data);
			AddTaskActivity.this.finish();
		}
	};
	
	private int index = -1;
	private String text = "";
	private Double currentLatitude = null;
	private Double currentLongitude = null;
	private Double latitude;
	private Double longitude;

	// GPS management
	private LocationManager locationManager;

	private void setCurrentGpsLocation(Location location) {
		if (location == null) {
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); 
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		if( location != null ) {
			currentLatitude = location.getLatitude();
			currentLongitude = location.getLongitude();
		} else {
			currentLatitude = null;
			currentLongitude = null;
		}
	}
	
	public void onLocationChanged(Location location) {
		setCurrentGpsLocation(location);
	}

	public void onProviderDisabled(String provider) {
		setCurrentGpsLocation(null);
	}

	public void onProviderEnabled(String provider) {
		setCurrentGpsLocation(null);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		setCurrentGpsLocation(null);
	}
}
