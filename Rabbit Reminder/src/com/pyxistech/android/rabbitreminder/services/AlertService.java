/**
	RABBIT REMINDER
	Copyright (C) 2010  Pyxis Technologies
	
	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License along
	with this program; if not, write to the Free Software Foundation, Inc.,
	51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.pyxistech.android.rabbitreminder.services;

import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;

import com.pyxistech.android.rabbitreminder.R;
import com.pyxistech.android.rabbitreminder.activities.AlertActivity;
import com.pyxistech.android.rabbitreminder.activities.SettingsActivity;
import com.pyxistech.android.rabbitreminder.models.AlertItem;
import com.pyxistech.android.rabbitreminder.models.AlertList;

public class AlertService extends Service {
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onStart (Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (isThreadNotStarted()) {
			startThread();
			startOnGoingNotification();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		interruptThread();
		stopOnGoingNotification();
	}

	private boolean isThreadNotStarted() {
		return notificationThread == null;
	}

	private void startThread() {
		notificationThread = new AlertThread(this);
		notificationThread.setInterrupted(false);
		notificationThread.start();
	}

	private void interruptThread() {
		notificationThread.setInterrupted(true);
		notificationThread = null;
	}

	private void startOnGoingNotification() {
		Intent intent = new Intent(this, SettingsActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.alert_service_icon, getString(R.string.alert_service_ongoing_notification_info_message), System.currentTimeMillis());
		notification.setLatestEventInfo(this, getString(R.string.alert_service_ongoing_notification_title), getString(R.string.alert_service_ongoing_notification_description), pendingIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		
		nm.notify(ONGOING_SERVICE_NOTIFICATION_ID, notification);
	}
	
	private void stopOnGoingNotification() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(ONGOING_SERVICE_NOTIFICATION_ID);
	}
	
	private static final int ONGOING_SERVICE_NOTIFICATION_ID = 0;
	private AlertThread notificationThread;
}

class AlertThread extends Thread {

	private final String[] PROJECTION = new String[] {
        AlertList.Items._ID, // 0
        AlertList.Items.NAME, // 1
        AlertList.Items.DONE, // 2
        AlertList.Items.LATITUDE, // 3
        AlertList.Items.LONGITUDE, // 4
        AlertList.Items.NOTIFICATION_MODE // 5
    };
    
    public AlertThread(AlertService context) {
    	this.context = context;
    }
    
	public void run() {
		Vector<Integer> localAndAlreadySeenAlerts = new Vector<Integer>();
		
		Looper.prepare();
		
		initializeNetworkManager();
		activateNetworkProvider();
		
		while (!interrupted) {
			Location myLocation = getLocationFromNetwork();
			Vector<AlertItem> undoneAlerts = getUndoneAlerts();
			
			removeDoneAlertFromAlreadySeenArray(localAndAlreadySeenAlerts);
			
			if (myLocation != null && undoneAlerts.size() > 0) {
				myLocation = getAMorePreciseLocationIfNecessary(myLocation, undoneAlerts);
				
				Vector<AlertItem> localUndoneAlerts = getLocalUndoneAlerts(myLocation, undoneAlerts);
				Vector<AlertItem> nonLocalUndoneAlerts = getNonLocalUndoneAlerts(myLocation, undoneAlerts);
				
				notifyUserComingNearLocalAlert(localAndAlreadySeenAlerts, localUndoneAlerts);
				notifyUserGoingAwayFromLocalAlert(localAndAlreadySeenAlerts, nonLocalUndoneAlerts);
			}
			else {
				deactivateGpsProvider();
			}

			threadWait(NOTIFICATION_REFRESH_RATE);
		}
		
		deactivateNetworkProvider();
	}

	private void initializeNetworkManager() {
		lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	private void deactivateNetworkProvider() {
		lm.removeUpdates(networkListener);
	}

	private void activateNetworkProvider() {
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, NOTIFICATION_REFRESH_RATE, 10, networkListener);
	}
	
	private LocationListener gpsListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
		public void onProviderEnabled(String provider) {
		}
		
		public void onProviderDisabled(String provider) {
		}
		
		public void onLocationChanged(Location location) {
		}
	};
	
	private LocationListener networkListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
		
		public void onProviderEnabled(String provider) {
		}
		
		public void onProviderDisabled(String provider) {
		}
		
		public void onLocationChanged(Location location) {
		}
	};

	private Location getAMorePreciseLocationIfNecessary(Location myLocation, Vector<AlertItem> undoneAlerts) {
		if (!isUserFarFromAlerts(myLocation, undoneAlerts)) {
			activateGpsProvider();
			Location myGpsLocation = getLocationFromGPS();
			if (myGpsLocation != null) {						
				myLocation = myGpsLocation;
			}
		}
		else {
			deactivateGpsProvider();
		}
		return myLocation;
	}

	private void deactivateGpsProvider() {
		lm.removeUpdates(gpsListener);
	}

	private void activateGpsProvider() {
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_REFRESH_RATE, 10, gpsListener);
	}
	
	public boolean isUserFarFromAlerts(Location userLocation, Vector<AlertItem> alerts) {
		float shortestDistance = buildLocationFromAlertItem(alerts.get(0)).distanceTo(userLocation);
		for (AlertItem item : alerts) {
			float distanceFromUserLocationToAlertLocation = buildLocationFromAlertItem(item).distanceTo(userLocation);
			if (distanceFromUserLocationToAlertLocation < shortestDistance)
				shortestDistance = distanceFromUserLocationToAlertLocation;
		}
		
		return (shortestDistance > userLocation.getAccuracy() + DEFAULT_DISTANCE_THRESHOLD_FOR_LOCAL_ALERT);
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	private void removeDoneAlertFromAlreadySeenArray(Vector<Integer> localAndAlreadySeenAlerts) {
		for (AlertItem doneAlerts : getDoneAlerts()) {
			if (localAndAlreadySeenAlerts.indexOf(doneAlerts.getIndex()) != -1) {
				localAndAlreadySeenAlerts.remove(localAndAlreadySeenAlerts.indexOf(doneAlerts.getIndex()));
			}
		}
	}

	private void notifyUserGoingAwayFromLocalAlert( Vector<Integer> localAndAlreadySeenAlerts, Vector<AlertItem> nonLocalUndoneAlerts) {
		for (AlertItem alertItem : nonLocalUndoneAlerts) {
			if (localAndAlreadySeenAlerts.indexOf(alertItem.getIndex()) != -1) {	
				if (alertItem.getNotificationMode() == AlertItem.NOTIFY_WHEN_GO_OUT) {
					Intent intent = buildNotificationIntent(alertItem);
					notifyUser(alertItem.getText(), context.getString(R.string.alert_type_description_go_out_of_label), intent);
				}
				localAndAlreadySeenAlerts.remove(localAndAlreadySeenAlerts.indexOf(alertItem.getIndex()));
			}
		}
	}

	private void notifyUserComingNearLocalAlert( Vector<Integer> localAndAlreadySeenAlerts, Vector<AlertItem> localUndoneAlerts) {
		for (AlertItem alertItem : localUndoneAlerts) {
			if (localAndAlreadySeenAlerts.indexOf(alertItem.getIndex()) == -1) {
				if (alertItem.getNotificationMode() == AlertItem.NOTIFY_WHEN_NEAR_OF) {
					Intent intent = buildNotificationIntent(alertItem);
					notifyUser(alertItem.getText(), context.getString(R.string.alert_type_description_near_of_label), intent);
				}
				localAndAlreadySeenAlerts.add(alertItem.getIndex());
			}
		}
	} 

	private Intent buildNotificationIntent(AlertItem alertItem) {
		Intent intent = new Intent(context, AlertActivity.class);
		intent.putExtra("index", (int) alertItem.getIndex());
		intent.putExtra("item", alertItem);
		return intent;
	}

	private void notifyUser(String alertMessage, String alertSubMessage, Intent notificationIntent) {					
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.alert_notification_icon, context.getString(R.string.notification_long_description_text), System.currentTimeMillis());
		PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId++, notificationIntent, 0);
		notification.setLatestEventInfo(context, alertMessage, alertSubMessage, pendingIntent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.sound = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
	
		nm.notify(notificationId, notification);
	}

	private Vector<AlertItem> getLocalUndoneAlerts(Location myLocation, Vector<AlertItem> tasks) {
		Vector<AlertItem> localTasks = new Vector<AlertItem>();
		for (AlertItem taskItem : tasks) {
			Location taskItemLocation = buildLocationFromAlertItem(taskItem);
			
			if (isTaskLocationNearMyLocation(myLocation, taskItemLocation)) {
				localTasks.add(taskItem);
			}
		}
		return localTasks;
	}

	private Vector<AlertItem> getNonLocalUndoneAlerts(Location myLocation, Vector<AlertItem> tasks) {
		Vector<AlertItem> localTasks = new Vector<AlertItem>();
		for (AlertItem taskItem : tasks) {
			Location taskItemLocation = buildLocationFromAlertItem(taskItem);
			
			if (isTaskFarFromMyLocation(myLocation, taskItemLocation)) {
				localTasks.add(taskItem);
			}
		}
		return localTasks;
	}

	private boolean isTaskFarFromMyLocation(Location myLocation, Location taskItemLocation) {
		return taskItemLocation.distanceTo(myLocation) > DEFAULT_DISTANCE_THRESHOLD_FOR_LOCAL_ALERT;
	}

	private boolean isTaskLocationNearMyLocation(Location myLocation, Location taskItemLocation) {
		return taskItemLocation.distanceTo(myLocation) < DEFAULT_DISTANCE_THRESHOLD_FOR_LOCAL_ALERT;
	}

	private Location buildLocationFromAlertItem(AlertItem taskItem) {
		Location taskItemLocation = new Location("com.pyxistech.android.rabbitreminder.providers.AlertListProvider");
		taskItemLocation.setLatitude(taskItem.getLatitude());
		taskItemLocation.setLongitude(taskItem.getLongitude());
		return taskItemLocation;
	}

	private void threadWait(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private Vector<AlertItem> getUndoneAlerts() {
		return getAlerts(undoneAlertWhereClause());
	}
	
	private Vector<AlertItem> getDoneAlerts() {
		return getAlerts(doneAlertWhereClause());
	}
	
	private Vector<AlertItem> getAlerts(String whereClause) {
		Vector<AlertItem> alerts = new Vector<AlertItem>();
		
		Cursor alertsCursor = context.getContentResolver().query(AlertList.Items.CONTENT_URI, 
				PROJECTION, whereClause, null, 
				AlertList.Items.DEFAULT_SORT_ORDER);
		
		if( alertsCursor.moveToFirst() ) {
			do {
				addAlertFromCursor(alerts, alertsCursor);
	        } while(alertsCursor.moveToNext());
        }
		alertsCursor.close();
		
		return alerts;
	}

	private void addAlertFromCursor(Vector<AlertItem> alerts, Cursor alertsCursor) {
		alerts.add(new AlertItem(
					Integer.valueOf(alertsCursor.getString(0)), 
					alertsCursor.getString(1), 
					alertsCursor.getInt(2) == 1, 
					alertsCursor.getDouble(3), 
					alertsCursor.getDouble(4),
					alertsCursor.getInt(5)
			));
	}

	private String doneAlertWhereClause() {
		return AlertList.Items.DONE + "=1";
	}


	private String undoneAlertWhereClause() {
		return AlertList.Items.DONE + "=0";
	}
	
	private Location getLocationFromGPS() {
		return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}
	
	private Location getLocationFromNetwork() {
		return lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	
	private static final int NOTIFICATION_REFRESH_RATE = 10000;
	private static final int GPS_REFRESH_RATE = 30000;
    private static final int DEFAULT_DISTANCE_THRESHOLD_FOR_LOCAL_ALERT = 100;
	
	private boolean interrupted = false;
	private AlertService context;
	private int notificationId = 1;

	private LocationManager lm;
}
