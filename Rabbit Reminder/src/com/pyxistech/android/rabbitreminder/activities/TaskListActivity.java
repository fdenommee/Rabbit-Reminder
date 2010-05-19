package com.pyxistech.android.rabbitreminder.activities;

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.pyxistech.android.rabbitreminder.R;
import com.pyxistech.android.rabbitreminder.adaptaters.TaskListAdapter;
import com.pyxistech.android.rabbitreminder.models.TaskItem;
import com.pyxistech.android.rabbitreminder.models.TaskList;

public class TaskListActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	TaskListAdapter adapter = new TaskListAdapter(this, new TaskList());

    	if (savedInstanceState == null)
    		buildList(adapter);
    	else
    		adapter.setList( (TaskList) savedInstanceState.getParcelable("TaskList") );

    	setListAdapter(adapter);
    	registerForContextMenu(getListView());
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	TaskList list = getTaskListAdapter().getList();
		outState.putParcelable("TaskList", list);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, ADD_ITEM, Menu.NONE, R.string.add_item_menu_text);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case ADD_ITEM:
    		addItem();
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(Menu.NONE, EDIT_ITEM, Menu.NONE,  R.string.edit_item_menu_text);
    	menu.add(Menu.NONE, DELETE_ITEM, Menu.NONE, R.string.delete_item_menu_text);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	switch (item.getItemId()) {
    	case DELETE_ITEM:
    		deleteItem(info.id);
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    
    private void deleteItem(final long index) {
    	Builder builder = new Builder(this);
    	builder.setIcon(android.R.drawable.ic_dialog_alert);
    	builder.setTitle(R.string.delete_item_confirmation_dialog_title);
    	builder.setMessage(R.string.delete_item_confirmation_dialog_text);
    	builder.setPositiveButton(R.string.validation_button_text, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				getTaskListAdapter().deleteItem((int) index);				
			}
		});
    	builder.setNegativeButton(R.string.cancel_button_text, null);
    	builder.show();
    }
    
    private void addItem() {
		Intent intent = new Intent(this, AddTaskActivity.class);
		
		startActivityForResult(intent, 0);
	}
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	String data = intent.getExtras().get("newTaskText").toString();
		getTaskListAdapter().addItem(new TaskItem(data, false));
    }

	@Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	CheckedTextView checkableItem = (CheckedTextView) v.findViewById(R.id.task_item);
    	checkableItem.toggle();
    	getModel(position).setDone(checkableItem.isChecked());
    }
    
    private TaskItem getModel(int position) {
    	return getTaskListAdapter().getItem(position);
    }

	private TaskListAdapter getTaskListAdapter() {
		return ((TaskListAdapter) getListAdapter());
	}
     
    private TaskList buildList(TaskListAdapter adapter) {
    	adapter.addItem(new TaskItem("item 1", false));
    	adapter.addItem(new TaskItem("item 2", true));
    	adapter.addItem(new TaskItem("item 3", true));
    	adapter.addItem(new TaskItem("item 4", false));
    	adapter.addItem(new TaskItem("item 5", false));
    	adapter.addItem(new TaskItem("item 6", false));
    	adapter.addItem(new TaskItem("item 7", true));
    	adapter.addItem(new TaskItem("item 8", true));
    	adapter.addItem(new TaskItem("item 9", true));
    	adapter.addItem(new TaskItem("item 10", false));
    	adapter.addItem(new TaskItem("item 11", false));
    	adapter.addItem(new TaskItem("item 12", false));
    	adapter.addItem(new TaskItem("item 13", false));
    	adapter.addItem(new TaskItem("item 14", false));
    	
    	return adapter.getList();
    }
    
    public static final int ADD_ITEM = Menu.FIRST + 1;
    public static final int EDIT_ITEM = Menu.FIRST + 2;
    public static final int DELETE_ITEM = Menu.FIRST + 3;
}