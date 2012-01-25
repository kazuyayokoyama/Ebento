/*
 * Copyright (C) 2012 Kazuya (Kaz) Yokoyama <kazuya.yokoyama@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kazuyayokoyama.android.apps.ebento.ui;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.widget.Toast;

import com.kazuyayokoyama.android.apps.ebento.R;
import com.kazuyayokoyama.android.apps.ebento.io.Event;
import com.kazuyayokoyama.android.apps.ebento.io.EventManager;
import com.kazuyayokoyama.android.apps.ebento.io.EventManager.OnStateUpdatedListener;
import com.kazuyayokoyama.android.apps.ebento.ui.RsvpFragment.OnRsvpSelectedListener;
import com.kazuyayokoyama.android.apps.ebento.util.DateTimeUtils;
import com.kazuyayokoyama.android.apps.ebento.util.UIUtils;

public class EventActivity extends FragmentActivity implements OnRsvpSelectedListener {
	private static final String TAG = "EventActivity";
	private static final int REQUEST_PEOPLE = 0;
	private static final int REQUEST_EDIT = 1;
    private EventManager mManager = EventManager.getInstance();
    private EventFragment mEventFragment;
    private PeopleListFragment mPeopleListFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);

		FragmentManager fm = getSupportFragmentManager();
		mEventFragment = (EventFragment) fm.findFragmentById(R.id.fragment_event);
		mPeopleListFragment = (PeopleListFragment) fm.findFragmentById(R.id.fragment_people_list);
		mManager.addListener(mStateUpdatedListener);
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mManager.removeListener(mStateUpdatedListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mManager.isCreator()) {
			getMenuInflater().inflate(R.menu.menu_items_event_host, menu);
		} else {
			getMenuInflater().inflate(R.menu.menu_items_event_guest, menu);
		}
		
		super.onCreateOptionsMenu(menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_people) {
			goPeople();
			return true;
		} else if (item.getItemId() == R.id.menu_edit) {
			goEdit();
			return true;
		} else if (item.getItemId() == R.id.menu_calendar) {
			goCalendar();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	// RsvpFragment > OnRsvpSelectedListener
	@Override
	public void onRsvpSelected(int state) {
		mEventFragment.refreshView();
		if (mPeopleListFragment != null) mPeopleListFragment.refreshView();
	}
	
	// EventManager > OnStateUpdatedListener
	private OnStateUpdatedListener mStateUpdatedListener = new OnStateUpdatedListener() {
		@Override
		public void onStateUpdated() {
			mEventFragment.refreshView();
			if (mPeopleListFragment != null) mPeopleListFragment.refreshView();
		}
	};
	
	private void goPeople() {
		// Intent
		Intent intent = new Intent(this, PeopleListActivity.class);
		startActivityForResult(intent, REQUEST_PEOPLE);
	}
	
	private void goEdit() {
		// Intent
		Intent intent = new Intent(this, EditActivity.class);
		intent.putExtra(EditActivity.EXTRA_EDIT, EditActivity.EXTRA_EDIT);
		startActivityForResult(intent, REQUEST_EDIT);
	}
	
	private void goCalendar() {
		AlertDialog.Builder addCalDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.add_cal_dialog_title)
				.setMessage(R.string.add_cal_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(true)
				.setPositiveButton(getResources().getString(R.string.add_cal_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// add the event to local calendar
						createNewEvent();
					}
				})
				.setNegativeButton(getResources().getString(R.string.add_cal_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								// nothing to do
							}
						});
		addCalDialog.create().show();
	}
	
	private void createNewEvent() {
		Event event = mManager.getEvent();
		
		long startMsec = DateTimeUtils.getMilliSeconds(
				event.startDate.year, event.startDate.month, event.startDate.day, 
				event.startTime.hour, event.startTime.minute);

		long endMsec = 0;
        if (event.endTime.hour >= 0 && event.endTime.minute >= 0) {
        	endMsec = DateTimeUtils.getMilliSeconds(
    				event.endDate.year, event.endDate.month, event.endDate.day, 
    				event.endTime.hour, event.endTime.minute);
        } else {
        	// an hour ahead
        	endMsec = startMsec + (1000 * 60 * 60);
        }
        
        // ICS
        if (UIUtils.isIceCreamSandwich()) {
    		Intent intent = new Intent(Intent.ACTION_INSERT);
    		intent.setData(CalendarContract.Events.CONTENT_URI);
    		intent.putExtra(CalendarContract.Events.TITLE, event.title);
    		intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMsec);
    		intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMsec);
    		if (event.place.length() > 0) intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.place);
    		if (event.details.length() > 0) intent.putExtra(CalendarContract.Events.DESCRIPTION, event.details);
    		startActivity(intent);
        } else {
        	Uri calendarUri;
        	Uri eventUri;
        	// Froyo
        	if (UIUtils.isFroyo()) {
        		calendarUri = Uri.parse("content://com.android.calendar/calendars");
        		eventUri = Uri.parse("content://com.android.calendar/events");
        	} else {
        		calendarUri = Uri.parse("content://calendar/calendars");
        		eventUri = Uri.parse("content://calendar/events");
        	}

        	Cursor cursor = getContentResolver().query(
        			calendarUri, new String[]{"_id", "displayname"}, null, null, null);
        	if (cursor != null && cursor.moveToFirst()) {
        		// TODO : first found calendar
        		int calId = cursor.getInt(0);
        		Log.d(TAG, "calId=" + calId + " calName=" + cursor.getString(1));
	        	
	        	ContentValues cv = new ContentValues();
	        	cv.put("calendar_id", calId);
	        	cv.put("title", event.title);
	        	cv.put("dtstart", startMsec);
	        	cv.put("dtend", endMsec);
	    		if (event.place.length() > 0) cv.put("eventLocation", event.place);
	    		if (event.details.length() > 0) cv.put("description", event.details);
	        	ContentResolver cr = getContentResolver();
	        	Uri added = cr.insert(eventUri, cv);
        		Log.d(TAG, "added calendar uri=" + added.toString());

				Toast.makeText(this, R.string.toast_calendar_added_success, Toast.LENGTH_SHORT).show();
        	} else {
				Toast.makeText(this, R.string.toast_calendar_added_failure, Toast.LENGTH_SHORT).show();
        	}
        	cursor.close();
        }
	}
}