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

package com.pyxistech.android.rabbitreminder.test.ui;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ContentValues;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.CheckedTextView;

import com.jayway.android.robotium.solo.Solo;
import com.pyxistech.android.rabbitreminder.activities.AlertListActivity;
import com.pyxistech.android.rabbitreminder.adaptaters.AlertListAdapter;
import com.pyxistech.android.rabbitreminder.models.AlertItem;
import com.pyxistech.android.rabbitreminder.models.AlertList;

public class AlertListActivityTest extends
		ActivityInstrumentationTestCase2<AlertListActivity> {

	public AlertListActivityTest() {
		super("com.pyxistech.android.rabbitreminder", AlertListActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());

		KeyguardManager mKeyGuardManager = (KeyguardManager) getActivity()
				.getSystemService(Activity.KEYGUARD_SERVICE);
		KeyguardLock mLock = mKeyGuardManager
				.newKeyguardLock("activity_classname");
		mLock.disableKeyguard();

		try {
			runTestOnUiThread(new Runnable() {
				public void run() {
					buildList();
				}
			});
		} catch (Throwable e) {
			throw new Exception(e);
		}

		initialListSize = getListSize();
	}

	@Override
	public void tearDown() throws Exception {
		try {
			solo.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		getActivity().finish();
		super.tearDown();
	}

	public void testPrecondition() {
		assertNotNull(getActivity());
	}

	@UiThreadTest
	public void testAddingElementsThroughtAdapter() {
		int numberOfNewItems = 10;

		for (int i = 0; i < numberOfNewItems; i++) {
			getListAdapter().addItem(
					new AlertItem("test item " + i, false, null, null, AlertItem.NOTIFY_WHEN_NEAR_OF));
		}
		int afterCount = getListSize();

		assertEquals(numberOfNewItems, afterCount - initialListSize);
	}

	public void testAddingElementsThroughtAddTaskActivity() {
		int numberOfNewItems = 5;

		for (int i = 0; i < numberOfNewItems; i++) {
			solo.clickOnMenuItem("Create Alert");
			solo.enterText(0, "item "
					+ String.valueOf(initialListSize + i + 1).toUpperCase());
			solo.clickOnButton("OK");
		}

		solo.sleep(500);

		int afterCount = getListSize();

		assertEquals(numberOfNewItems, afterCount - initialListSize);
	}

	public void testCheckStateAreSavedOnScrolling() {
		solo.clickOnText("item 49");
		solo.scrollDown();
		solo.scrollUp();
		solo.scrollUp();

		assertTrue(getListItemView(0).isChecked());
	}

	public void testCheckStateAreSavedOnRotation() {
		solo.clickOnText("item 49");
		solo.setActivityOrientation(Solo.LANDSCAPE);
		assertTrue(getListItemView(0).isChecked());
		solo.setActivityOrientation(Solo.PORTRAIT);
		assertTrue(getListItemView(0).isChecked());
	}

	public void testItemCanBeDeleted() {
		solo.clickLongOnText("item 1");
		solo.clickOnText("Delete");
		solo.clickOnText("OK");

		solo.sleep(500);

		assertEquals(initialListSize - 1, getListSize());
	}

	public void testItemDeletionCanBeCancelled() {
		solo.clickLongOnText("item 1");
		solo.clickOnText("Delete");
		solo.clickOnText("Cancel");

		solo.sleep(500);

		assertEquals(initialListSize, getListSize());
	}

	public void testItemCanBeEdited() {
		solo.clickLongOnText("item 49");
		solo.clickOnText("Edit");
		solo.enterText(0, " edited");
		solo.clickOnText("OK");

		solo.sleep(500);

		assertEquals(initialListSize, getListSize());
		assertEquals("item 49 edited", getListItemView(0).getText());
	}

	public void testRotationDoesNotAffectEdition() {
		solo.clickLongOnText("item 49");
		solo.clickOnText("Edit");
		solo.enterText(0, " edited");
		solo.setActivityOrientation(Solo.PORTRAIT);
		solo.setActivityOrientation(Solo.LANDSCAPE);
		solo.clickOnText("OK");

		solo.sleep(500);

		assertEquals(initialListSize, getListSize());
		assertEquals("item 49 edited", getListItemView(0).getText());
	}

	public void testCancelingTheCurrentTaskEditionWorks() {
		solo.clickOnMenuItem("Create Alert");
		solo.goBack();
	}

	private void buildList() {
		getListAdapter().clearList();
		getActivity().getContentResolver().delete(AlertList.Items.CONTENT_URI,
				"", null);
		for (int i = 0; i < 50; i++) {
			getListAdapter().addItem(
					new AlertItem("item " + i, false, null, null, AlertItem.NOTIFY_WHEN_NEAR_OF));

			ContentValues values = new ContentValues();
			values.put(AlertList.Items.NAME, "item " + i);
			values.put(AlertList.Items.DONE, 0);
			getActivity().getContentResolver().insert(
					AlertList.Items.CONTENT_URI, values);
		}
		getActivity().refreshList(
				(AlertListAdapter) getActivity().getListAdapter());
	}

	private CheckedTextView getListItemView(int index) {
		return ((CheckedTextView) solo.getCurrentListViews().get(0).getChildAt(
				index));
	}

	private int getListSize() {
		return getListAdapter().getCount();
	}

	private AlertListAdapter getListAdapter() {
		return ((AlertListAdapter) getActivity().getListAdapter());
	}

	private Solo solo;
	private int initialListSize;
}
