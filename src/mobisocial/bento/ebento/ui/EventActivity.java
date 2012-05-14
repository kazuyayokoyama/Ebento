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

package mobisocial.bento.ebento.ui;

import mobisocial.bento.ebento.R;
import mobisocial.bento.ebento.io.Event;
import mobisocial.bento.ebento.io.EventManager;
import mobisocial.bento.ebento.io.EventManager.OnStateUpdatedListener;
import mobisocial.bento.ebento.ui.RsvpFragment.OnRsvpSelectedListener;
import mobisocial.bento.ebento.util.CalendarHelper;
import mobisocial.bento.ebento.util.InitialHelper;
import mobisocial.bento.ebento.util.InitialHelper.OnInitCompleteListener;
import mobisocial.socialkit.musubi.Musubi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

public class EventActivity extends FragmentActivity implements OnRsvpSelectedListener {
	public static final String EXTRA_LAUNCHED_FROM_LIST = "launched_from_list";
	
	//private static final String TAG = "EventActivity";
	private static final int REQUEST_PEOPLE = 0;
	private static final int REQUEST_EDIT = 1;
    private EventManager mManager = EventManager.getInstance();
    private EventFragment mEventFragment;
    private RsvpFragment mRsvpFragment;
    private PeopleListFragment mPeopleListFragment;
    private boolean mbLaunchedFromList = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        
        mbLaunchedFromList = getIntent().hasExtra(EXTRA_LAUNCHED_FROM_LIST);

        if (!mbLaunchedFromList) {
			// create Musubi Instance
	        InitialHelper initHelper = new InitialHelper(this, mInitCompleteListener);
			Musubi musubi = initHelper.initMusubiInstance(false);
			if (musubi == null) {
				return;
			}
        }
        
		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(true); // bad know-how for enabling home clickable on ICS.
		actionBar.setDisplayHomeAsUpEnabled(mbLaunchedFromList);
		actionBar.setDisplayUseLogoEnabled(false);
		actionBar.setDisplayShowTitleEnabled(false);

		FragmentManager fm = getSupportFragmentManager();
		mEventFragment = (EventFragment) fm.findFragmentById(R.id.fragment_event);
		mRsvpFragment = (RsvpFragment) fm.findFragmentById(R.id.fragment_rsvp);
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
		if (item.getItemId() == android.R.id.home) {
        	if (mbLaunchedFromList) {
        		finish();
        		return true;
        	} else {
        		goEventList();
        		return true;
        	}
		} else if (item.getItemId() == R.id.menu_people) {
			goPeople();
			return true;
		} else if (item.getItemId() == R.id.menu_edit) {
			goEdit();
			return true;
		} else if (item.getItemId() == R.id.menu_calendar) {
			goCalendar();
			return true;
		} else if (item.getItemId() == R.id.menu_event_list) {
			goEventList();
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
		refreshFragmentView();
	}
	
	// never fired because init will be done sync, not async
	// InitialHelper > OnInitCompleteListener
	private OnInitCompleteListener mInitCompleteListener = new OnInitCompleteListener() {
		@Override
		public void onInitCompleted() {
			refreshFragmentView();
		}
	};
	
	// EventManager > OnStateUpdatedListener
	private OnStateUpdatedListener mStateUpdatedListener = new OnStateUpdatedListener() {
		@Override
		public void onStateUpdated() {
			refreshFragmentView();
		}
	};
	
	private void refreshFragmentView() {
		this.invalidateOptionsMenu();
		mEventFragment.refreshView();
		mRsvpFragment.refreshView();
		if (mPeopleListFragment != null) mPeopleListFragment.refreshView();
	}
	
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
		CalendarHelper.addEventToCalendar(this, event);
	}
	
	private void goEventList() {

		// Load Event List
		EventListDialogAsyncTask task = new EventListDialogAsyncTask(this);
		task.execute();
	}
	
	class EventListDialogAsyncTask extends AsyncTask<Void, Void, Boolean> {
		private EventManager mManager = EventManager.getInstance();
		private Context mContext;
		private AlertDialog mEventListDialog = null;
		private ProgressDialog mProgressDialog = null;
		
		public EventListDialogAsyncTask(Context context) {
			mContext = context;
		}

		@Override
		protected void onPreExecute() {
			LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        final View listRoot = inflater.inflate(R.layout.fragment_event_list, null, false);
	        
	        // Dialog
			AlertDialog.Builder eventListDialogBuilder = new AlertDialog.Builder(mContext)
			.setTitle(R.string.event_list_dialog_title)
			.setView(listRoot)
			.setCancelable(true)
			.setNegativeButton(getResources().getString(R.string.event_list_dialog_cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// nothing to do
						}
					});

	        // ListView
			final ListView listView = (ListView) listRoot.findViewById(android.R.id.list);
	        listView.setFastScrollEnabled(true);
	        
	        // Create adapter
	        final EventListItemAdapter listAdapter = new EventListItemAdapter(
	        		mContext, 
	        		android.R.layout.simple_list_item_1,
	        		listView);
			listView.setAdapter(listAdapter);
			
			mEventListDialog = eventListDialogBuilder.create();
			
			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					EventListItem item = mManager.getEventListItem(position);
					mManager.setEventObjUri(item.objUri);
					
					mEventListDialog.dismiss();
					refreshFragmentView();
				}
			});
			
			// disable empty
			LinearLayout emptyLayout = (LinearLayout) listRoot.findViewById(android.R.id.empty);
			emptyLayout.setVisibility(View.GONE);
			
			// show progress dialog
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setMessage(mContext.getString(R.string.event_list_loading));
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			mManager.loadEventList();
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				// show dialog
				if (mEventListDialog != null) {
					mEventListDialog.show();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}