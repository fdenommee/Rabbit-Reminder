package com.pyxistech.android.rabbitreminder;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

public class TaskListAdaptater extends ArrayAdapter<TaskItem> {

	public TaskListAdaptater(Activity context, TaskList list) {
		super(context, R.layout.task_item, list.toArray());
		
		this.context = context;
		this.list = list;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		View taskItem = convertView;
		
		if (taskItem == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			taskItem = inflater.inflate(R.layout.task_item, null);
		}
		
		CheckedTextView checkableItem = (CheckedTextView) taskItem.findViewById(R.id.task_item);
		checkableItem.setChecked(list.getItemAt(position).isDone());
		checkableItem.setText(list.getItemAt(position).getText());
		
		return taskItem;
	}

	private Activity context;
	private TaskList list;
}
