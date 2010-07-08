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

package com.pyxistech.android.rabbitreminder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;

import com.pyxistech.android.rabbitreminder.R;
import com.pyxistech.android.rabbitreminder.services.AlertService;

public class SettingsActivity extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);
		
		CheckBoxPreference check = (CheckBoxPreference) findPreference("rabbit reminder alert service");
		check.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Intent alertServiceIntent = new Intent(getBaseContext(), AlertService.class);
				
				if( !isServiceActive(preference) ) {
					startService(alertServiceIntent);
				} else {
					stopService(alertServiceIntent);
				}
				
				return true;
			}

			private boolean isServiceActive(Preference preference) {
				return ((CheckBoxPreference) preference).isChecked();
			}
		});
	}

}
