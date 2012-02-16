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

import mobisocial.bento.ebento.io.EventManager;
import mobisocial.bento.ebento.io.EventManager.OnStateUpdatedListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

import mobisocial.bento.ebento.R;

public class EventListActivity extends FragmentActivity {
	//private static final String TAG = "EventListActivity";
	private static final int REQUEST_EVENT = 0;
	private static final int REQUEST_EDIT = 1;

    private EventManager mManager = EventManager.getInstance();
    private EventListFragment mEventListFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		
		FragmentManager fm = getSupportFragmentManager();
		mEventListFragment = (EventListFragment) fm.findFragmentById(R.id.fragment_event_list);
		mManager.addListener(mStateUpdatedListener);
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mManager.removeListener(mStateUpdatedListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_items_event_list, menu);
		
		super.onCreateOptionsMenu(menu);
		return true;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_add:
            	goCreate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_EDIT) {
			// New Event Saved
			if (resultCode == Activity.RESULT_OK) {
				// return to home
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	}
	
	// EventManager > OnStateUpdatedListener
	private OnStateUpdatedListener mStateUpdatedListener = new OnStateUpdatedListener() {
		@Override
		public void onStateUpdated() {
			mEventListFragment.refreshView();
		}
	};
    
    public void goEvent() {
		// Intent
		Intent intent = new Intent(this, EventActivity.class);
		startActivityForResult(intent, REQUEST_EVENT);
    }
    
    public void goCreate() {
		// Intent
		Intent intent = new Intent(this, EditActivity.class);
		startActivityForResult(intent, REQUEST_EDIT);
    }
}