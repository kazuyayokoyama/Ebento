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


public class People {
	public static final int STATE_UNKNOWN 	= -1;
	public static final int STATE_YES 		= 0;
	public static final int STATE_MAYBE  	= 1;
	public static final int STATE_NO  		= 2;
	public static final int MAX_STATE  		= 3;
    
	public String contactId;
	public int state;
}
