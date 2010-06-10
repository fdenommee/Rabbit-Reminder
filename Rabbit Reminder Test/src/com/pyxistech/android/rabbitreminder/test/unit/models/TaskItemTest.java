package com.pyxistech.android.rabbitreminder.test.unit.models;

import android.test.AndroidTestCase;

import com.pyxistech.android.rabbitreminder.models.TaskItem;

public class TaskItemTest extends AndroidTestCase {
	public void setUp() {
		
	}
	
	public void testEqualsWorks() {
		TaskItem item1 = new TaskItem("item", true, 0.0, 0.0);
		TaskItem item2 = new TaskItem("item", true, 0.0, 0.0);
		TaskItem item3 = new TaskItem("item", false, 0.0, 0.0);
		TaskItem item4 = new TaskItem("foo", true, 0.0, 0.0);
		
		assertTrue(item1.equals(item2));
		assertFalse(item1.equals(item3));
		assertFalse(item1.equals(item4));
	}
}
