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

import mobisocial.socialkit.musubi.Musubi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;

import com.kazuyayokoyama.android.apps.ebento.R;
import com.kazuyayokoyama.android.apps.ebento.io.EventManager;

public class HomeActivity extends FragmentActivity {
	//private static final String TAG = "HomeActivity";
	private static final int REQUEST_EVENT = 0;
	private static final int REQUEST_EDIT = 1;
	
	private EventManager mManager = EventManager.getInstance();
	private Musubi mMusubi = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);
		
		// Check if this activity launched from Home Screen
		if (!Musubi.isMusubiIntent(getIntent())) {
			boolean bInstalled = false;
			try {
				bInstalled = Musubi.isMusubiInstalled(getApplication());
			} catch (Exception e) {
				// be quiet
				bInstalled = false;
			}
			// Check if Musubi is installed
			if (bInstalled) {
				goMusubi();
			} else {
				goMarket();
			}
		} else {
			// create Musubi Instance
			Intent intent = getIntent();
			mMusubi = Musubi.getInstance(this);
			// get version code
			int versionCode = 0;
			try {
				PackageInfo packageInfo = getPackageManager().getPackageInfo(
						"com.kazuyayokoyama.android.apps.ebento", PackageManager.GET_META_DATA);
				versionCode = packageInfo.versionCode;
			} catch (NameNotFoundException e) {
			    e.printStackTrace();
			}
			mManager.init(mMusubi, (Uri) intent.getParcelableExtra(Musubi.EXTRA_FEED_URI), versionCode);

			// Has Event
			if (mManager.hasEvent()) {
				goEvent();
			} else {
				goCreate();
			}
		}
    }

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_EDIT) {
			// New Event Saved
			if (resultCode == Activity.RESULT_OK) {
				goEvent();
			} else {
				finish();
			}
		} else if (requestCode == REQUEST_EVENT) {
			finish();
		}
	}
    
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
    
	public void goMarket() {
		AlertDialog.Builder marketDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.market_dialog_title)
				.setMessage(R.string.market_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(true)
				.setPositiveButton(getResources().getString(R.string.market_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Go to Android Market
						startActivity(Musubi.getMarketIntent());
						finish();
					}
				})
				.setNegativeButton(getResources().getString(R.string.market_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						});
		marketDialog.create().show();
	}
    
	public void goMusubi() {
		AlertDialog.Builder musubiDialog = new AlertDialog.Builder(this)
				.setTitle(R.string.musubi_dialog_title)
				.setMessage(R.string.musubi_dialog_text)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setCancelable(true)
				.setPositiveButton(getResources().getString(R.string.musubi_dialog_yes), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						try {
							// Launching Musubi
			                Intent intent = new Intent(Intent.ACTION_MAIN);
			                intent.setClassName("edu.stanford.mobisocial.dungbeetle", "edu.stanford.mobisocial.dungbeetle.ui.FeedListActivity"); 
			                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
			                startActivity(intent);
			                finish();
						} catch (Exception e) {
							goMarket();
						}
					}
				})
				.setNegativeButton(getResources().getString(R.string.musubi_dialog_no),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								finish();
							}
						});
		musubiDialog.create().show();
	}
}