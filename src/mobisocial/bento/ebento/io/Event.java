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

package mobisocial.bento.ebento.io;

import android.graphics.Bitmap;

public class Event {
	public class EventDate {
		public int year;
		public int month;
		public int day;
		public boolean bAllDay = false;
	};
	public class EventTime {
		public int hour;
		public int minute;
	};
	
	public String uuid;
	public String title;
	public String place;
	public String details;
	public EventDate startDate;
	public EventDate endDate;
	public EventTime startTime;
	public EventTime endTime;
	public Bitmap image;
	public long creDateMillis;
	public long modDateMillis;
	public String creContactId;
	public String modContactId;
	public String creatorName;
	
	public Event() {
		this.startDate = new EventDate();
		this.endDate = new EventDate();
		this.startTime = new EventTime();
		this.endTime = new EventTime();
	}
}
