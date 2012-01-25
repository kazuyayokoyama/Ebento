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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;

import com.kazuyayokoyama.android.apps.ebento.R;

public class EditActivity extends FragmentActivity {
	public static final String EXTRA_EDIT = "ExtraEdit";
	
	//private static final String TAG = "EditActivity";
	private EditFragment mEditFragment = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        boolean bNewEventMode = true;
        // Check if this activity launched from internal activity or not
     	if (getIntent().hasExtra(EXTRA_EDIT)) {
     		bNewEventMode = false;
   		}
        
        setContentView(R.layout.activity_edit);
        
		FragmentManager fm = getSupportFragmentManager();
		mEditFragment = (EditFragment) fm.findFragmentById(R.id.fragment_edit);

		final ActionBar actionBar = getSupportActionBar();
		// set defaults for logo & home up
		actionBar.setDisplayHomeAsUpEnabled(bNewEventMode ? false : true);
		actionBar.setDisplayUseLogoEnabled(bNewEventMode ? false : true);
		actionBar.setTitle(bNewEventMode ? R.string.label_create_event : R.string.label_edit_event);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_items_edit, menu);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return mEditFragment.onOptionsItemSelected(item);
        }
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
}