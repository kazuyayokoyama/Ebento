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

import java.util.ArrayList;

import mobisocial.bento.ebento.R;
import mobisocial.bento.ebento.io.EventManager;
import mobisocial.bento.ebento.util.BitmapHelper;
import mobisocial.bento.ebento.util.DateTimeUtils;
import mobisocial.bento.ebento.util.UIUtils;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class EventListItemAdapter extends ArrayAdapter<EventListItem> {
	private static final Boolean DEBUG = UIUtils.isDebugMode();
	private static final String TAG = "EventListItemAdapter";
    
	private static final int MAX_IMG_WIDTH = 160;
	private static final int MAX_IMG_HEIGHT = 120;

	private LayoutInflater mInflater;
	private EventManager mManager = EventManager.getInstance();
	private Context mContext = null;

	public EventListItemAdapter(Context context, int resourceId,
			ListView listView) {
		super(context, resourceId);
		
		mContext = context;
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	static class ViewHolder {
		ImageView image;
		TextView title;
		TextView startDateTime;
	}

	@Override
	public int getCount() {
		return mManager.getEventListCount();
	}

	@Override
	public EventListItem getItem(int position) {
		return mManager.getEventListItem(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;

		// Fetch item
		final EventListItem item = (EventListItem) getItem(position);

		if (isEnabled(position)) {
			if (convertView == null || convertView.getId() != R.layout.item_event_list) {
				// Create view from Layout File
				convertView = mInflater.inflate(R.layout.item_event_list, null);
				holder = new ViewHolder();
				holder.image = (ImageView) convertView.findViewById(R.id.image);
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.startDateTime = (TextView) convertView.findViewById(R.id.start_date_time);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
	
			// Set Title
			holder.title.setText(item.event.title);
			
			// Set Start Date and Time
			String dateTimeMsg = DateTimeUtils.getDateAndTimeWithShortFormat(
					false,
					item.event.startDate.year, item.event.startDate.month, item.event.startDate.day,
					item.event.startTime.hour, item.event.startTime.minute);
			holder.startDateTime.setText(dateTimeMsg);
	
			// Set Image
			if (holder.image != null) {
				holder.image.setTag(item.event.uuid);
				if (item.event.image == null) {
					holder.image.setImageBitmap(BitmapHelper.getResizedBitmap(
	        				BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_image_event_list), 
	        					MAX_IMG_WIDTH, MAX_IMG_HEIGHT, 0));
				} else {
					holder.image.setImageBitmap(
							BitmapHelper.getResizedBitmap(item.event.image, MAX_IMG_WIDTH, MAX_IMG_HEIGHT, 0));
				}
			}
		} else {
			if (convertView == null || convertView.getId() != R.layout.item_event_list_divider) {
				// Create view from Layout File
				convertView = mInflater.inflate(R.layout.item_event_list_divider, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.feed_name);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
	
			// Set Name
			holder.title.setText(getFeedName(item.feedId));
		}
		
		return convertView;
	}
	
	@Override
    public boolean isEnabled(int position) {
        return getItem(position).enabled;
    }
	
	private String getFeedName(long feedId) {
		String feedName = "";
		ArrayList<String> members = mManager.getMemberNames(feedId);

		if (members.size() > 0) {
	        StringBuilder text = new StringBuilder(100);
			for (String memeber : members) {
	            text.append(memeber).append(", ");
			}
	        text.setLength(text.length() - 2);
	        
	        feedName = text.toString();
		}
		
		if (DEBUG) Log.d(TAG, "feedName: " + feedName);
		
		return feedName;
	}
}
