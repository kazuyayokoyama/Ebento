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
import mobisocial.bento.ebento.io.EventManager;
import mobisocial.bento.ebento.io.EventManager.OnStateUpdatedListener;
import mobisocial.bento.ebento.util.InitialHelper;
import mobisocial.bento.ebento.util.InitialHelper.OnInitCompleteListener;
import mobisocial.socialkit.musubi.Musubi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;

public class EventListActivity extends FragmentActivity {
	//private static final String TAG = "EventListActivity";
    private static final int REQUEST_CREATE_FEED = 1;
    private static final String ACTION_CREATE_FEED = "musubi.intent.action.CREATE_FEED";

    private EventManager mManager = EventManager.getInstance();
    private EventListFragment mEventListFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// create Musubi Instance
        InitialHelper initHelper = new InitialHelper(this, mInitCompleteListener);
		Musubi musubi = initHelper.initMusubiInstance(false);
		if (musubi == null) {
			return;
		}
		
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
            	if (mManager.isFromMusubi()) {
            		goCreate();
            	} else {
            		goNewFeed();
            	}
                return true;
            case R.id.menu_info:
            	goInfo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CREATE_FEED) {
            if (resultCode == RESULT_OK) {
                Uri feedUri = data.getData();
                mManager.setFeedUri(feedUri);
                
                goCreate();
            }
        }
    };

	// InitialHelper > OnInitCompleteListener
	private OnInitCompleteListener mInitCompleteListener = new OnInitCompleteListener() {
		@Override
		public void onInitCompleted() {
			mEventListFragment.refreshView();
		}
	};
	
	// EventManager > OnStateUpdatedListener
	private OnStateUpdatedListener mStateUpdatedListener = new OnStateUpdatedListener() {
		@Override
		public void onStateUpdated() {
			mEventListFragment.refreshView();
		}
	};
    
    public void goCreate() {
		// Intent
		Intent intent = new Intent(this, EditActivity.class);
		startActivity(intent);
    }
    
    private void goNewFeed() {
        Intent create = new Intent(ACTION_CREATE_FEED);
        startActivityForResult(create, REQUEST_CREATE_FEED);
    }
    
    private void goInfo() {
		// Show Add dialog
		LayoutInflater factory = LayoutInflater.from(this);
		final View inputView = factory.inflate(R.layout.dialog_info, null);

		// WebView
		final WebView webView = (WebView) inputView.findViewById(R.id.webview);
		webView.loadUrl("file:///android_asset/license.html");

		AlertDialog.Builder libDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.info_dialog_title)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView(inputView)
				.setCancelable(false)
				.setPositiveButton(R.string.info_dialog_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// 
					}
				});
		libDialog.create().show();
    }
}