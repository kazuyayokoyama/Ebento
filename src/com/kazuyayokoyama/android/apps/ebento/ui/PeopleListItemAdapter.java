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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kazuyayokoyama.android.apps.ebento.R;
import com.kazuyayokoyama.android.apps.ebento.io.EventManager;
import com.kazuyayokoyama.android.apps.ebento.io.People;
import com.kazuyayokoyama.android.apps.ebento.util.BitmapHelper;

public class PeopleListItemAdapter extends ArrayAdapter<PeopleListItem> {
	//private static final String TAG = "PeopleListItemAdapter";
	private static final int TYPE_SECTION_HEADER = 0;
    private static final int TYPE_LIST_ITEM  = 1;
    
	private static final int MAX_IMG_WIDTH = 80;
	private static final int MAX_IMG_HEIGHT = 80;
	private static final int DUMMY_IMG_WIDTH = 80;
	private static final int DUMMY_IMG_HEIGHT = 80;

	private LayoutInflater mInflater;
	private EventManager mManager = EventManager.getInstance();
	private Context mContext = null;

	public PeopleListItemAdapter(Context context, int resourceId,
			ListView listView) {
		super(context, resourceId);
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	static class ViewHolder {
		ImageView image;
		TextView name;
	}

	@Override
	public int getCount() {
		return mManager.getPeopleListCount();
	}

	@Override
	public PeopleListItem getItem(int position) {
		return mManager.getPeopleListItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;

		// Fetch item
		final PeopleListItem item = (PeopleListItem) getItem(position);
		
		if (convertView == null) {
			// Create view from Layout File
			if (item.header.bHeader) {
				convertView = mInflater.inflate(R.layout.item_people_list_header, null);
				holder = new ViewHolder();
				holder.image = null;
				holder.name = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(holder);
			} else {
				convertView = mInflater.inflate(R.layout.item_people_list, null);
				holder = new ViewHolder();
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				holder.name = (TextView) convertView.findViewById(R.id.name);
				convertView.setTag(holder);
			}
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// Set Name
		if (item.header.bHeader) {
			switch (item.header.state) {
			case People.STATE_YES:
				item.name = mContext.getString(R.string.people_yes_header, item.header.numberInHeader);
				break;
			case People.STATE_MAYBE:
				item.name = mContext.getString(R.string.people_maybe_header, item.header.numberInHeader);
				break;
			case People.STATE_NO:
				item.name = mContext.getString(R.string.people_no_header, item.header.numberInHeader);
				break;
			default:
				item.name = "";
				break;
			}
			holder.name.setText(item.name);
		} else {
			if (item.people.contactId.equals(mManager.getLocalContactId())) {
				holder.name.setText(item.name + mContext.getString(R.string.people_you));
			} else {
				holder.name.setText(item.name);
			}
		}
		

		// Set Image
		if (holder.image != null) {
			holder.image.setTag(item.people.contactId);
			if (item.image == null) {
				holder.image.setImageBitmap(
						BitmapHelper.getDummyBitmap(DUMMY_IMG_WIDTH, DUMMY_IMG_HEIGHT));
			} else {
				holder.image.setImageBitmap(
						BitmapHelper.getResizedBitmap(item.image, MAX_IMG_WIDTH, MAX_IMG_HEIGHT, 0));
			}
		}

		return convertView;
	}
	
	@Override
    public int getViewTypeCount() {
        return People.MAX_STATE;
    }
	
	@Override
    public int getItemViewType(int position) {
		PeopleListItem item = getItem(position);
        if (item.header.bHeader) {
            return TYPE_SECTION_HEADER;
        } else {
            return TYPE_LIST_ITEM;
        }
    }
}
