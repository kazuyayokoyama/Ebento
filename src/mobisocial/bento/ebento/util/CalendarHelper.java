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

package mobisocial.bento.ebento.util;

import mobisocial.bento.ebento.R;
import mobisocial.bento.ebento.io.Event;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

public class CalendarHelper {
	private static final String TAG = "CalendarHelper";
	
	public static void addEventToCalendar(Context context, Event event) {

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
    		context.startActivity(intent);
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

        	Cursor cursor = context.getContentResolver().query(
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
	        	ContentResolver cr = context.getContentResolver();
	        	Uri added = cr.insert(eventUri, cv);
        		Log.d(TAG, "added calendar uri=" + added.toString());

				Toast.makeText(context, R.string.toast_calendar_added_success, Toast.LENGTH_SHORT).show();
        	} else {
				Toast.makeText(context, R.string.toast_calendar_added_failure, Toast.LENGTH_SHORT).show();
        	}
        	cursor.close();
        }
	}
}
