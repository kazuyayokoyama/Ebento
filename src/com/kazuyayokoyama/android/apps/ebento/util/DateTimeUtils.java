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

package com.kazuyayokoyama.android.apps.ebento.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.text.format.Time;

public class DateTimeUtils {
	public static class DateTime {
		public int year = 1970;
		public int month = 1;
		public int day = 1;
		public int hour = 0;
		public int minute = 0;
	};
	
    public static String getDateWithFormat(int year, int month, int day) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
        return dateFormat.format(new Date((year - 1900), (month - 1), day));
    }
    public static String getDateWithShortFormat(int year, int month, int day) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy");
        return dateFormat.format(new Date((year - 1900), (month - 1), day));
    }
    
    public static String getTimeWithFormat(int year, int month, int day, int hour, int minute) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mma");
        return timeFormat.format(new Date((year - 1900), (month - 1), day, hour, minute));
    }
    
    public static String getTimeWithFormat(DateTime dateTime) {
        return getTimeWithFormat(dateTime.year, dateTime.month, dateTime.day, dateTime.hour, dateTime.minute);
    }
    
    public static String getDateAndTimeWithShortFormat(
    		boolean withYear, int year, int month, int day, int hour, int minute) {
        SimpleDateFormat dateTimeFormat;
        if (withYear) {
        	dateTimeFormat= new SimpleDateFormat("EEE, MMM d, yyyy h:mma");
        } else {
        	dateTimeFormat= new SimpleDateFormat("EEE, MMM d, h:mma");
    	}
        return dateTimeFormat.format(new Date((year - 1900), (month - 1), day, hour, minute));
    }
    
    public static long getMilliSeconds(int year, int month, int day, int hour, int minute) {

    	Calendar c = Calendar.getInstance();
    	//c.setTimeZone(TimeZone.getTimeZone("UTC"));
    	c.setTime(new Date((year - 1900), (month - 1), day, hour, minute));

    	return c.getTimeInMillis();
    }
    
    public static long convToEnglishTime(long dateMsec) {
    	Time androTime;
    	androTime = new Time();
    	androTime.switchTimezone(TimeZone.getDefault().getDisplayName(Locale.ENGLISH));
    	androTime.set(dateMsec);

    	return androTime.normalize(true);
    }
    
    public static DateTime getRoundedCurrentDateTime() {
        final Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.MINUTE) != 0) {
        	cal.add(Calendar.HOUR_OF_DAY, 1);
        	cal.set(Calendar.MINUTE, 0);
        }
        
    	return getDateTimeFromCal(cal);
    }
    
    public static DateTime getDateTimeFromCal(Calendar cal) {
    	DateTime dateTime = new DateTime();
    	dateTime.year = cal.get(Calendar.YEAR);
    	dateTime.month = cal.get(Calendar.MONTH) + 1;
    	dateTime.day = cal.get(Calendar.DAY_OF_MONTH);
    	dateTime.hour = cal.get(Calendar.HOUR_OF_DAY);
    	dateTime.minute = cal.get(Calendar.MINUTE);
    	return dateTime;
    }
    
    public static Calendar getCalFromDateTime(DateTime dateTime) {
    	Calendar cal = Calendar.getInstance();
    	cal.setTime(new Date(
    			(dateTime.year - 1900), 
    			(dateTime.month - 1), 
    			dateTime.day, 
    			dateTime.hour, 
    			dateTime.minute));
    	return cal;
    }
    
    public static DateTime getEndTimeFromStartTime(DateTime start, DateTime end) {
    	Calendar startCal = getCalFromDateTime(start);
    	Calendar endCal = getCalFromDateTime(end);
    	
    	Calendar newEndCal = Calendar.getInstance();
    	// if start > end
    	if (startCal.compareTo(endCal) > 0) {
    		newEndCal = startCal;
    		newEndCal.add(Calendar.HOUR_OF_DAY, 1);
    	} else {
    		newEndCal = endCal;
    	}
    	
    	return getDateTimeFromCal(newEndCal);
    }
    
    public static DateTime getStartTimeFromEndTime(DateTime start, DateTime end) {
    	Calendar startCal = getCalFromDateTime(start);
    	Calendar endCal = getCalFromDateTime(end);
    	
    	Calendar newStartCal = Calendar.getInstance();
    	// if start > end
    	if (startCal.compareTo(endCal) > 0) {
    		newStartCal = endCal;
    		newStartCal.add(Calendar.HOUR_OF_DAY, -1);
    	} else {
    		newStartCal = startCal;
    	}

    	return getDateTimeFromCal(newStartCal);
    }
    
    public static DateTime getOneHourAheadEndTimeFromStartTime(DateTime start) {
    	Calendar startCal = getCalFromDateTime(start);
    	startCal.add(Calendar.HOUR_OF_DAY, 1);
    	
    	return getDateTimeFromCal(startCal);
    }
}
