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

import java.util.ArrayList;

import mobisocial.bento.ebento.ui.EventListItem;
import mobisocial.bento.ebento.ui.PeopleListItem;
import mobisocial.bento.ebento.util.BitmapHelper;
import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.DbUser;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.multiplayer.FeedRenderable;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;


public class EventManager {
    public interface OnStateUpdatedListener {
        public void onStateUpdated();
    }

    public static final String ANDROID_PACKAGE_NAME = "android_pkg";
    public static final String ANDROID_CLASS_NAME = "android_cls";
    public static final String ANDROID_ACTION = "android_action";
	public static final String TYPE_APP_STATE = "appstate";
	public static final String TYPE_APP = "app";
	
	// root > state
	public static final String STATE = "state";
	// root > state > event
	public static final String VERSION_CODE = "version_code";
	public static final String EVENT = "event";
	public static final String EVENT_UUID = "uuid";
	public static final String EVENT_TITLE = "title";
	public static final String EVENT_PLACE = "place";
	public static final String EVENT_DETAILS = "details";
	public static final String EVENT_DATE_START = "date_start";
	public static final String EVENT_DATE_END = "date_end";
	public static final String EVENT_DATE_YEAR = "year";
	public static final String EVENT_DATE_MONTH = "month";
	public static final String EVENT_DATE_DAY = "day";
	public static final String EVENT_DATE_ALLDAY = "allday";
	public static final String EVENT_TIME_START = "time_start";
	public static final String EVENT_TIME_END = "time_end";
	public static final String EVENT_TIME_HOUR = "hour";
	public static final String EVENT_TIME_MINUTE = "minute";
	public static final String EVENT_HAS_IMAGE = "has_image";
	public static final String EVENT_CRE_DATE = "cre_date";
	public static final String EVENT_MOD_DATE = "mod_date";
	public static final String EVENT_CRE_CONTACT_ID = "cre_contact_id";
	public static final String EVENT_MOD_CONTACT_ID = "mod_contact_id";
	// root > state > people
	public static final String PEOPLE_LIST = "people_list";
	public static final String PEOPLE_CONTACT_ID = "contact_id";
	public static final String PEOPLE_STATE = "state";
	// root > event_image
	public static final String EVENT_IMAGE = "event_image";
	public static final String EVENT_IMAGE_UUID = "uuid";
	public static final String B64JPGTHUMB = "b64jpgthumb";
	
	private static final String TAG = "EventManager";
	private static EventManager sInstance = null;
	private Musubi mMusubi = null;
	private DbFeed mDbFeed = null;
	private Uri mBaseUri = null;
	private String mLocalContactId = null;
	private String mLocalName = null;
    private Integer mLastInt = 0;
    private int mVersionCode = 0;
	
	private Event mEvent = null;
	private ArrayList<EventListItem> mEventList = new ArrayList<EventListItem>();
	private ArrayList<PeopleListItem> mPeopleList = null;
    private ArrayList<OnStateUpdatedListener> mListenerList = new ArrayList<OnStateUpdatedListener>();

	// ----------------------------------------------------------
	// Instance
	// ----------------------------------------------------------
	private EventManager() {
		initData();
	}

	public static EventManager getInstance() {
		if (sInstance == null) {
			sInstance = new EventManager();
		}

		return sInstance;
	}

	public void init(Musubi musubi, Uri baseUri, int versionCode) {
		// Musubi
		mMusubi = musubi;
		mBaseUri = baseUri;
		mLocalContactId = mMusubi.userForLocalDevice(mBaseUri).getId();
		mLocalName = mMusubi.userForLocalDevice(mBaseUri).getName();
		mVersionCode = versionCode;
		setDbFeed(mMusubi.getObj().getSubfeed());
	}

	public void fin() {
        mDbFeed.removeStateObserver(mStateObserver);
		initData();

		if (sInstance != null) {
			sInstance = null;
		}
	}
	
	// ----------------------------------------------------------
	// Get / Retrieve
	// ----------------------------------------------------------
	synchronized public boolean hasEvent() {
		return (mEvent == null ? false : true);
	}
	
	synchronized public Event getEvent() {
		return mEvent;
	}
	
	synchronized public PeopleListItem getPeopleListItem(int position) {
		return mPeopleList.get(position);
	}

	synchronized public int getPeopleListCount() {
		return mPeopleList.size();
	}

	synchronized public String getLocalContactId() {
		return mLocalContactId;
	}

	synchronized public String getLocalName() {
		return mLocalName;
	}
	
	synchronized public int getLocalState() {
		int state = People.STATE_UNKNOWN;
		if (mPeopleList != null) {
			for (int i=0; i<mPeopleList.size(); i++) {
				PeopleListItem item = mPeopleList.get(i);
				
				// skip header
				if (item.header.bHeader) {
					continue;
				}
				
				if (mLocalContactId.equals(item.people.contactId)) {
					state = item.people.state;
					break;
				}
			}
		}
		return state;
	}
	
	synchronized public int getNumberOfState(int state) {
		int number = 0;
		for (int i=0; i<mPeopleList.size(); i++) {
			PeopleListItem item = mPeopleList.get(i);
			if (item.header.state == state) {
				number = item.header.numberInHeader;
				break;
			}
		}
		return number;
	}
	
	synchronized public boolean isCreator() {
		boolean ret = true;
		if (mEvent != null) {
			ret = mLocalContactId.equals(mEvent.creContactId);
		}
		return ret;
	}

	// Event List
	synchronized public void loadEventList() {
        String[] projection = null;
        String selection = "type = ? AND feed_name = ?";
        String[] selectionArgs = new String[] { TYPE_APP, mMusubi.getObj().getFeedName() };
        String sortOrder = null;
        
		Cursor c = mMusubi.getAppFeed().query(projection, selection, selectionArgs, sortOrder);

		if (c != null && c.moveToFirst()) {
			mEventList = new ArrayList<EventListItem>();
			
			for (int i = 0; i < c.getCount(); i++) {
				EventListItem item = new EventListItem();
				item.event = new Event();
				
				DbObj dbObj = mMusubi.objForCursor(c);
				DbFeed dbFeed = dbObj.getSubfeed();
				Obj latestObj = dbFeed.getLatestObj();
				if (latestObj != null && latestObj.getJson() != null && latestObj.getJson().has(STATE)) {
					JSONObject stateObj = latestObj.getJson().optJSONObject(STATE);
					if (fetchEventObj(dbFeed, stateObj, item.event)) {
						item.feedUri = dbFeed.getUri();
						mEventList.add(0, item);
					}
				}
				c.moveToNext();
			}
		}
		c.close();
	}
	
	synchronized public EventListItem getEventListItem(int position) {
		return mEventList.get(position);
	}

	synchronized public int getEventListCount() {
		return mEventList.size();
	}

	// ----------------------------------------------------------
	// Update
	// ----------------------------------------------------------
	synchronized public void createEvent(Event event, String htmlMsg) {
		//if (!isEmptyDbFeed(mDbFeed)) {
		//	setNewDbFeed();
		//}
		newData();
		updateEvent(event, htmlMsg);
	}

	synchronized public void updateEvent(Event event, String htmlMsg) {
		mEvent = event;
		
		String uuid = null;
		String data = null;
		if (mEvent.image != null) {
			uuid = mEvent.uuid;
			data = Base64.encodeToString(BitmapHelper.bitmapToBytes(mEvent.image), Base64.DEFAULT);
		}
		pushUpdate(htmlMsg, uuid, data);
	}
	
	synchronized public void updatePeople(String contactId, int state, String htmlMsg) {
		PeopleListItem item = new PeopleListItem();
		item.people.contactId = contactId;
		item.people.state = state;
		
		updateToPeopleListWithState(contactId, state);
		
		pushUpdate(htmlMsg);
	}

	// ----------------------------------------------------------
	// Listener
	// ----------------------------------------------------------
	public void addListener(OnStateUpdatedListener listener){
		mListenerList.add(listener);
    }
	
	public void removeListener(OnStateUpdatedListener listener){
		mListenerList.remove(listener);
    }

	// ----------------------------------------------------------
	// Musubi
	// ----------------------------------------------------------
	public void pushUpdate(String htmlMsg) {
		pushUpdate(htmlMsg, null, null);
	}

	public void pushUpdate(String htmlMsg, String eventUuid, String data) {
		try {
			JSONObject rootObj = new JSONObject();
			rootObj.put(STATE, getStateObj());
			
			JSONObject out = new JSONObject(rootObj.toString());

			if (eventUuid != null && data != null) {
				JSONObject eventImageObj = new JSONObject();
				eventImageObj.put(EVENT_IMAGE_UUID, eventUuid);
				out.put(EVENT_IMAGE, eventImageObj);
				out.put(B64JPGTHUMB, data);
			}
			
			FeedRenderable renderable = FeedRenderable.fromHtml(htmlMsg);
			renderable.withJson(out);
			mDbFeed.postObj(new MemObj(TYPE_APP_STATE, out, null, ++mLastInt));
		} catch (JSONException e) {
			Log.e(TAG, "Failed to post JSON", e);
		}
	}

	public void setDbFeed(Uri feedUri) {
		setDbFeed(mMusubi.getFeed(feedUri));
	}
	
	public void setDbFeed(DbFeed dbFeed) {
		// previous feed
		if (mDbFeed != null) {
			mDbFeed.removeStateObserver(mStateObserver);
		}
        
        // new feed
		mDbFeed = dbFeed;

        String[] projection = null;
        String selection = "type = ?";
        String[] selectionArgs = new String[] { TYPE_APP_STATE };
        String sortOrder = DbObj.COL_KEY_INT + " desc";
        mDbFeed.setQueryArgs(projection, selection, selectionArgs, sortOrder);
        
        mDbFeed.registerStateObserver(mStateObserver);

		// json
		JSONObject stateObj = null;
		Obj obj = mDbFeed.getLatestObj();
		if (obj != null && obj.getJson() != null && obj.getJson().has(STATE)) {
			stateObj = obj.getJson().optJSONObject(STATE);
		}

		if (stateObj == null) {
			initData();
			mLastInt = 0;
		} else {
			setNewStateObj(stateObj);
			mLastInt = (obj.getInt() == null) ? 0 : obj.getInt();
		}
	}

    @Deprecated
	public void setNewDbFeed() {
		// post new app object
		DbFeed feed = mMusubi.getFeed();
        JSONObject json = new JSONObject();
        try {
            json.put(ANDROID_ACTION, "mobisocial.intent.action.CONNECTED");
            json.put(ANDROID_PACKAGE_NAME, "mobisocial.bento.ebento");
            json.put(ANDROID_CLASS_NAME, "mobisocial.bento.ebento.ui.HomeActivity");
        } catch (JSONException e) {
            Log.d(TAG, "Failed to put JSON", e);
        }
		feed.postObj(new MemObj(TYPE_APP, json));
		
		// search posted object
        String[] projection = null;
        String selection = "type = ? AND feed_name = ?";
        String[] selectionArgs = new String[] { TYPE_APP, mMusubi.getObj().getFeedName() };
        String sortOrder = null;
        
		Cursor c = mMusubi.getAppFeed().query(projection, selection, selectionArgs, sortOrder);
		// go to last
		if (c != null && c.moveToLast()) {
			DbObj dbObj = mMusubi.objForCursor(c);
			DbFeed dbFeed = dbObj.getSubfeed();
			// TODO : confirm if the dbFeed is the one created right now
			setDbFeed(dbFeed);
		}
		c.close();
	}

    @Deprecated
	public boolean isEmptyDbFeed(DbFeed dbFeed) {
		boolean isEmpty = true;

		Obj latestObj = dbFeed.getLatestObj();
		if (latestObj != null && latestObj.getJson() != null && latestObj.getJson().has(STATE)) {
			isEmpty = false;
		} else {
			isEmpty = true;
		}
		
		return isEmpty;
	}
	
	private final FeedObserver mStateObserver = new FeedObserver() {
		@Override
		synchronized public void onUpdate(DbObj obj) {

			mLastInt = (obj.getInt() == null) ? 0 : obj.getInt();
			
			JSONObject stateObj = null;
			if (obj != null && obj.getJson() != null && obj.getJson().has(STATE)) {
				stateObj = obj.getJson().optJSONObject(STATE);

				try {
					// TODO : just in case
					if (! isValidEvent(stateObj.getJSONObject(EVENT).optString(EVENT_UUID))) {

						Handler handler = new Handler();
						handler.post(new Runnable(){
							public void run(){
								for (OnStateUpdatedListener listener : mListenerList) {
									listener.onStateUpdated();
								}
							}
						});
						
						return;
					}
				} catch (JSONException e) {
					Log.e(TAG, "Failed to get JSON", e);
					return;
				}
				
				setNewStateObj(stateObj);

				Handler handler = new Handler();
				handler.post(new Runnable(){
					public void run(){
						for (OnStateUpdatedListener listener : mListenerList) {
							listener.onStateUpdated();
						}
					}
				});
			} else {
				return;
			}
		}
	};

	// ----------------------------------------------------------
	// Private
	// ----------------------------------------------------------
	private void initData() {
		mEvent = null;
		mPeopleList = null;
	}
	
	private void newData() {
		mEvent = new Event();
		
		mPeopleList = new ArrayList<PeopleListItem>();
		mPeopleList.add(new PeopleListItem(People.STATE_YES));
		mPeopleList.add(new PeopleListItem(People.STATE_MAYBE));
		mPeopleList.add(new PeopleListItem(People.STATE_NO));
	}
	
	private void setNewStateObj(JSONObject stateObj) {
		newData();
		setNewEventObj(stateObj);
		setNewPeopleListObj(stateObj);
	}
	
	private void setNewEventObj(JSONObject stateObj) {
		fetchEventObj(mDbFeed, stateObj, mEvent);
	}
	
	private boolean fetchEventObj(DbFeed dbFeed, JSONObject stateObj, Event event) {
		boolean ret = false;
		try {
			JSONObject eventObj = stateObj.getJSONObject(EVENT);
			event.uuid = eventObj.optString(EVENT_UUID);
			event.title = eventObj.optString(EVENT_TITLE);
			event.place = eventObj.optString(EVENT_PLACE);
			event.details = eventObj.optString(EVENT_DETAILS);
			
			JSONObject startDateObj = eventObj.getJSONObject(EVENT_DATE_START);
			event.startDate.year = startDateObj.optInt(EVENT_DATE_YEAR);
			event.startDate.month = startDateObj.optInt(EVENT_DATE_MONTH);
			event.startDate.day = startDateObj.optInt(EVENT_DATE_DAY);
			event.startDate.bAllDay = startDateObj.optBoolean(EVENT_DATE_ALLDAY);
			
			JSONObject endDateObj = eventObj.getJSONObject(EVENT_DATE_END);
			event.endDate.year = endDateObj.optInt(EVENT_DATE_YEAR);
			event.endDate.month = endDateObj.optInt(EVENT_DATE_MONTH);
			event.endDate.day = endDateObj.optInt(EVENT_DATE_DAY);
			event.endDate.bAllDay = endDateObj.optBoolean(EVENT_DATE_ALLDAY);

			JSONObject startTimeObj = eventObj.getJSONObject(EVENT_TIME_START);
			event.startTime.hour = startTimeObj.optInt(EVENT_TIME_HOUR);
			event.startTime.minute = startTimeObj.optInt(EVENT_TIME_MINUTE);

			JSONObject endTimeObj = eventObj.getJSONObject(EVENT_TIME_END);
			event.endTime.hour = endTimeObj.optInt(EVENT_TIME_HOUR);
			event.endTime.minute = endTimeObj.optInt(EVENT_TIME_MINUTE);
			
			event.creDateMillis = eventObj.optLong(EVENT_CRE_DATE);
			event.modDateMillis = eventObj.optLong(EVENT_MOD_DATE);
			event.creContactId = eventObj.optString(EVENT_CRE_CONTACT_ID);
			event.modContactId = eventObj.optString(EVENT_MOD_CONTACT_ID);
			DbUser dbUser = mDbFeed.userForGlobalId(event.creContactId);	
			event.creatorName = (dbUser != null ? dbUser.getName() : "");
			
			boolean bImage = eventObj.optBoolean(EVENT_HAS_IMAGE);
			if (bImage) {
				event.image = getEventImage(dbFeed, event.uuid);
			} else {
				event.image = null;
			}
			
			ret = true;
		} catch (JSONException e) {
			Log.e(TAG, "Failed to get JSON", e);
		}
		
		return ret;
	}

	private void setNewPeopleListObj(JSONObject stateObj) {
		try {
			JSONArray peopleListArray = stateObj.optJSONArray(PEOPLE_LIST);
			
			if (peopleListArray != null) {
				for (int i=0; i<peopleListArray.length(); i++) {
					JSONObject peopleObj = peopleListArray.getJSONObject(i);
					People people = new People();
					people.contactId = peopleObj.optString(PEOPLE_CONTACT_ID);
					people.state = peopleObj.optInt(PEOPLE_STATE);
					
					PeopleListItem item = new PeopleListItem();
					item.people = people;
					DbUser dbUser = mDbFeed.userForGlobalId(people.contactId);
					item.image = dbUser.getPicture();
					item.name = dbUser.getName();
					
					insertToPeopleListWithState(item);
				}
			}
			
		} catch (JSONException e) {
			Log.e(TAG, "Failed to get JSON", e);
		}
	}
	
	private void insertToPeopleListWithState(PeopleListItem addItem) {
		int insertIndex = 0;
		for (int i=0; i<mPeopleList.size(); i++) {
			PeopleListItem item = mPeopleList.get(i);
			if (item.header.state == addItem.people.state) {
				insertIndex = i + 1;
				item.header.numberInHeader++;
				break;
			}
		}
		mPeopleList.add(insertIndex, addItem);
	}
	
	private void updateToPeopleListWithState(String contactId, int state) {
		boolean bRemoved = false;
		boolean bFound = false;
		int headerIndex = 0;
		
		for (int i=0; i<mPeopleList.size(); i++) {
			PeopleListItem item = mPeopleList.get(i);

			// skip header
			if (item.header.bHeader) {
				headerIndex = i;
				continue;
			}
			
			// remove
			if (contactId.equals(item.people.contactId)) {
				if (item.people.state != state) {
					mPeopleList.remove(i);
					mPeopleList.get(headerIndex).header.numberInHeader--;
					bRemoved = true;
				}
				
				bFound = true;
				break;
			}
		}
		
		// removed and new face
		if (bRemoved | !bFound) {
			PeopleListItem item = new PeopleListItem();
			item.people.contactId = contactId;
			item.people.state = state;
			insertToPeopleListWithState(item);
		}
	}

	private Bitmap getEventImage(DbFeed dbFeed, String eventUuid) {
		Bitmap bitmap = null;
		int targetWidth = BitmapHelper.MAX_IMAGE_WIDTH;
		int targetHeight = BitmapHelper.MAX_IMAGE_HEIGHT;
		float degrees = 0;

		Cursor c = dbFeed.query();
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			Obj object = mMusubi.objForCursor(c);
			if (object != null && object.getJson() != null && object.getJson().has(EVENT_IMAGE)) {
				JSONObject imageObj = object.getJson().optJSONObject(EVENT_IMAGE);
				if (eventUuid.equals(imageObj.optString(EVENT_IMAGE_UUID))) {
					byte[] byteArray = Base64.decode(object.getJson().optString(B64JPGTHUMB), Base64.DEFAULT);
					bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
					break;
				}
			}
			c.moveToNext();
		}
		c.close();

		// dummy
		if (bitmap == null) {
			bitmap = BitmapHelper.getDummyBitmap(targetWidth, targetHeight);
		} else {
			bitmap = BitmapHelper.getResizedBitmap(bitmap, targetWidth, targetHeight, degrees);
		}

		return bitmap;
	}
	
	private JSONObject getEventObj() {
		JSONObject eventObj = new JSONObject();
		try {
			eventObj.put(EVENT_UUID, mEvent.uuid);
			eventObj.put(EVENT_TITLE, mEvent.title);
			eventObj.put(EVENT_PLACE, mEvent.place);
			eventObj.put(EVENT_DETAILS, mEvent.details);

			JSONObject startDateObj = new JSONObject();
			startDateObj.put(EVENT_DATE_YEAR, mEvent.startDate.year);
			startDateObj.put(EVENT_DATE_MONTH, mEvent.startDate.month);
			startDateObj.put(EVENT_DATE_DAY, mEvent.startDate.day);
			startDateObj.put(EVENT_DATE_ALLDAY, mEvent.startDate.bAllDay);
			eventObj.put(EVENT_DATE_START, startDateObj);

			JSONObject endDateObj = new JSONObject();
			endDateObj.put(EVENT_DATE_YEAR, mEvent.endDate.year);
			endDateObj.put(EVENT_DATE_MONTH, mEvent.endDate.month);
			endDateObj.put(EVENT_DATE_DAY, mEvent.endDate.day);
			endDateObj.put(EVENT_DATE_ALLDAY, mEvent.endDate.bAllDay);
			eventObj.put(EVENT_DATE_END, endDateObj);

			JSONObject startTimeObj = new JSONObject();
			startTimeObj.put(EVENT_TIME_HOUR, mEvent.startTime.hour);
			startTimeObj.put(EVENT_TIME_MINUTE, mEvent.startTime.minute);
			eventObj.put(EVENT_TIME_START, startTimeObj);

			JSONObject endTimeObj = new JSONObject();
			endTimeObj.put(EVENT_TIME_HOUR, mEvent.endTime.hour);
			endTimeObj.put(EVENT_TIME_MINUTE, mEvent.endTime.minute);
			eventObj.put(EVENT_TIME_END, endTimeObj);

			eventObj.put(EVENT_CRE_DATE, mEvent.creDateMillis);
			eventObj.put(EVENT_MOD_DATE, mEvent.modDateMillis);
			eventObj.put(EVENT_CRE_CONTACT_ID, mEvent.creContactId);
			eventObj.put(EVENT_MOD_CONTACT_ID, mEvent.modContactId);

			eventObj.put(EVENT_HAS_IMAGE, (mEvent.image == null ? false : true));

		} catch (JSONException e) {
			Log.e(TAG, "Failed to put JSON", e);
		}
		
		return eventObj;
	}
	
	private JSONArray getPeopleListArray() {
		JSONArray peopleListArray = new JSONArray();

		try {

			for (int i = 0; i < mPeopleList.size(); i++) {
				PeopleListItem item = mPeopleList.get(i);
				
				// skip header
				if (item.header.bHeader) {
					continue;
				}
				
				JSONObject peopleObj = new JSONObject();
				peopleObj.put(PEOPLE_CONTACT_ID, item.people.contactId);
				peopleObj.put(PEOPLE_STATE, item.people.state);
				
				peopleListArray.put(peopleObj);
			}
						
		} catch (JSONException e) {
			Log.e(TAG, "Failed to put JSON", e);
		}
		
		return peopleListArray;
	}

	private JSONObject getStateObj() {
		JSONObject stateObj = new JSONObject();
		try {
			stateObj.put(VERSION_CODE, mVersionCode);
			JSONObject eventObj = getEventObj();
			stateObj.put(EVENT, eventObj);
			JSONArray peopleListArray = getPeopleListArray();
			stateObj.put(PEOPLE_LIST, peopleListArray);
		} catch (JSONException e) {
			Log.e(TAG, "Failed to put JSON", e);
		}
		return stateObj;
	}
	
	private boolean isValidEvent(String uuid) {
		return (mEvent != null && mEvent.uuid != null && uuid != null && mEvent.uuid.equals(uuid));
	}

}
