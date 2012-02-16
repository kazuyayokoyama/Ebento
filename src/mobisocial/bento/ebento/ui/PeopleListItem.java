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

import mobisocial.bento.ebento.io.People;
import android.graphics.Bitmap;


public class PeopleListItem {
	public class ListHeader {
		public boolean bHeader;
		public int numberInHeader;
		public int state;
	}
	
	public ListHeader header;
	public People people;
	public Bitmap image;
	public String name;
	
	public PeopleListItem() {
		init();
	}
	
	public PeopleListItem(int headerState) {
		init();
		this.header.bHeader = true;
		this.header.state = headerState;
	}
	
	private void init() {
		this.header = new ListHeader();
		this.header.bHeader = false;
		this.header.state = People.STATE_UNKNOWN;
		this.header.numberInHeader = 0;
		this.people = new People();
		this.people.contactId = null;
		this.people.state = People.STATE_UNKNOWN;
		this.image = null;
		this.name = null;
	}
}
