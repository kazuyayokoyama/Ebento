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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobisocial.bento.ebento.ui.EventListItem;
import mobisocial.bento.ebento.ui.PeopleListItem;
import mobisocial.bento.ebento.util.BitmapHelper;
import mobisocial.bento.ebento.util.UIUtils;
import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.Musubi.DbThing;
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
    public interface OnInitialEventListener {
        public void onEventInitialized();
    }

    public static final String ANDROID_PACKAGE_NAME = "android_pkg";
    public static final String ANDROID_CLASS_NAME = "android_cls";
    public static final String ANDROID_ACTION = "android_action";
	public static final String TYPE_EBENTO = "ebento";
	public static final String TYPE_APPSTATE = "appstate";
	
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
	public static final String B64JPGTHUMB = FeedRenderable.OBJ_B64_JPEG;
	// root > initial
	public static final String EVENT_INIT = "event_init";

	private class LatestObj {
		public JSONObject json = null;
		public int intKey = 0;
	};
	private static final Boolean DEBUG = UIUtils.isDebugMode();
	private static final String TAG = "EventManager";
	private static EventManager sInstance = null;
	private Musubi mMusubi = null;
	private Uri mCurrentUri = null;
	private String mLocalContactId = null;
	private String mLocalName = null;
    private Integer mLastInt = 0;
    private int mVersionCode = 0;
    private boolean mbFromMusubi = false;
    
    private boolean mbInitialEvent = false;
    private OnInitialEventListener mInitEventListener = null;
	
	private Event mEvent = null;
	private ArrayList<EventListItem> mEventList = new ArrayList<EventListItem>();
	private ArrayList<PeopleListItem> mPeopleList = null;
	private Map<Long, ArrayList<String>> mMemberNameCache = new HashMap<Long, ArrayList<String>>();
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

	public void init(Musubi musubi) {
		if (DEBUG) Log.d(TAG, "init()");
		
		// remove previous data
		if (mCurrentUri != null) {
			musubi.objForUri(mCurrentUri).getSubfeed().unregisterStateObserver(mStateObserver);
		}
		mCurrentUri = null;
		initData();
	}

	public void setMusubi(Musubi musubi, int versionCode) {
		if (DEBUG) Log.d(TAG, "setMusubi()");
		
		mMusubi = musubi;
		mVersionCode = versionCode;
		
		// start new one
		if (mMusubi.getObj() != null && mMusubi.getObj().getSubfeed() != null) {
			setEventObjUri(mMusubi.getObj().getUri());
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
		long prevFeedId = -1;
        String[] projection = new String[] { DbObj.COL_ID, DbObj.COL_FEED_ID };
        Uri uri = Musubi.uriForDir(DbThing.OBJECT);
        String selection = "type = ?";
        String[] selectionArgs = new String[] { TYPE_EBENTO };
        String sortOrder = DbObj.COL_FEED_ID + " asc, " + DbObj.COL_LAST_MODIFIED_TIMESTAMP + " asc";
        
		Cursor c = mMusubi.getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);

		ArrayList<EventListItem> tmpList = new ArrayList<EventListItem>();
		if (c != null && c.moveToFirst()) {
			mEventList = new ArrayList<EventListItem>();
			
			for (int i = 0; i < c.getCount(); i++) {
				EventListItem item = new EventListItem();
				item.event = new Event();
				
				DbObj dbObj = mMusubi.objForCursor(c);
				LatestObj latestObj = null;
				
				latestObj = fetchLatestObj(c.getLong(0));
				if (latestObj != null && latestObj.json.has(STATE)) {
					JSONObject stateObj = latestObj.json.optJSONObject(STATE);
					if (fetchEventObj(dbObj.getUri(), stateObj, item.event)) {
						item.objUri = dbObj.getUri();
						item.feedId = c.getLong(1);
						if (DEBUG) {
							Log.d(TAG, item.objUri.toString());
							Log.d(TAG, item.feedId + "");
						}
						tmpList.add(0, item);
						
						// load members
						fetchMemberNames(item.feedId);
					}
				}
				c.moveToNext();
			}
		}
		c.close();
		
		// insert dividers
		if (tmpList.size() > 0) {
			for (int j = 0; j < tmpList.size(); j++) {
				EventListItem item = tmpList.get(j);
				if (prevFeedId != item.feedId) {
					EventListItem divider = new EventListItem();
					divider.enabled = false;
					divider.feedId = item.feedId;
					mEventList.add(divider);
					
					prevFeedId = item.feedId;
				}
				mEventList.add(item);
			}
		}
		
		if (DEBUG) {
			Log.d(TAG, "tmpList:" + tmpList.size() + " mEventList:" + mEventList.size());
		}
	}
	
	private LatestObj fetchLatestObj(long localId) {
        Uri uri = Musubi.uriForDir(DbThing.OBJECT);
        String[] projection = new String[] { DbObj.COL_JSON, DbObj.COL_INT_KEY };
        String selection = DbObj.COL_PARENT_ID + "=? and type= ?";
        String[] selectionArgs = new String[] { Long.toString(localId), TYPE_APPSTATE };
        String sortOrder = DbObj.COL_INT_KEY + " desc limit 1";
        Cursor c = mMusubi.getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        try {
            if (c.moveToFirst()) {
            	LatestObj latestObj = new LatestObj();
            	try {
					latestObj.json = new JSONObject(c.getString(0));
				} catch (JSONException e) {
					e.printStackTrace();
				}
            	latestObj.intKey = c.getInt(1);
                return latestObj;
            } else {
                c.close();
                selection = DbObj.COL_ID + "=?";
                selectionArgs = new String[] { Long.toString(localId) };
                c = mMusubi.getContext().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
                if (c.moveToFirst()) {
                	LatestObj latestObj = new LatestObj();
                	try {
						latestObj.json = new JSONObject(c.getString(0));
					} catch (JSONException e) {
						e.printStackTrace();
					}
                	latestObj.intKey = c.getInt(1);
                    return latestObj;
                } else {
                    return null;
                }
            }
        } finally {
            c.close();
        }
    }
	
	synchronized public EventListItem getEventListItem(int position) {
		return mEventList.get(position);
	}

	synchronized public int getEventListCount() {
		return mEventList.size();
	}
	
	public ArrayList<String> getMemberNames(long feedId) {
		if (mMemberNameCache == null || !mMemberNameCache.containsKey(feedId)) {
			fetchMemberNames(feedId);
		}
		return mMemberNameCache.get(feedId);
	}
	
	private void fetchMemberNames(long feedId) {
		if (mMemberNameCache == null) {
			mMemberNameCache = new HashMap<Long, ArrayList<String>>();
		}
		
		if (!mMemberNameCache.containsKey(feedId)) {
			ArrayList<String> names = new ArrayList<String>();
	        Uri feedUri = Musubi.uriForItem(DbThing.FEED, feedId);
	        
	    	List<DbIdentity> members = mMusubi.getFeed(feedUri).getMembers();
	        for (int i = 0; i < members.size(); i++) {
	            DbIdentity id = members.get(i);
	            if (id != null) {
	            	if (!id.isOwned()) {
	            		// skip me
	            		names.add(id.getName());
	            	}
	            }
	        }
	        
	        mMemberNameCache.put(feedId, names);
		}
	}

	// ----------------------------------------------------------
	// Update
	// ----------------------------------------------------------
	synchronized public void initEvent(Event event, String msg, OnInitialEventListener listener) {
		mbInitialEvent = true;
		mInitEventListener = listener;
		
		newData();
		updateEvent(event, msg, true);
	}

	synchronized public void createEvent(Event event, String msg) {
		updateEvent(event, msg, false);
	}

	synchronized public void updateEvent(Event event, String msg, boolean bFirst) {
		mEvent = event;
		
		String uuid = null;
		String data = null;
		if (mEvent.image != null) {
			uuid = mEvent.uuid;
			data = Base64.encodeToString(BitmapHelper.bitmapToBytes(mEvent.image), Base64.DEFAULT);
		}
		pushUpdate(msg, uuid, data, bFirst);
	}
	
	synchronized public void updatePeople(String contactId, int state, String msg) {
		PeopleListItem item = new PeopleListItem();
		item.people.contactId = contactId;
		item.people.state = state;
		
		updateToPeopleListWithState(contactId, state);
		
		pushUpdate(msg, false);
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
	public void setFromMusubi(boolean bFromMusubi) {
		mbFromMusubi = bFromMusubi;
	}

	public boolean isFromMusubi() {
		return mbFromMusubi;
	}
	
	public void setFeedUri(Uri feedUri) {
		mMusubi.setFeed(mMusubi.getFeed(feedUri));
	}
	
	public void pushUpdate(String msg, boolean bFirst) {
		pushUpdate(msg, null, null, bFirst);
	}

	public void pushUpdate(String msg, String eventUuid, String data, boolean bFirst) {
		try {
			JSONObject rootObj = new JSONObject();
			rootObj.put(Obj.FIELD_RENDER_TYPE, Obj.RENDER_LATEST);
			rootObj.put(STATE, getStateObj());
			if (DEBUG) Log.d(TAG, "pushUpdate - getStateObj():" + getStateObj().toString());
			
			JSONObject out = new JSONObject(rootObj.toString());
			
			// ignore data when initial posting
			if (!bFirst && eventUuid != null && data != null) {
				JSONObject eventImageObj = new JSONObject();
				eventImageObj.put(EVENT_IMAGE_UUID, eventUuid);
				out.put(EVENT_IMAGE, eventImageObj);
				out.put(B64JPGTHUMB, data);
			}
			
			// initial post
			if (bFirst) {
				out.put(EVENT_INIT, 1);
			}

			FeedRenderable renderable = FeedRenderable.fromText(msg);
			renderable.addToJson(out);
			
			if (bFirst) {
				Obj obj = new MemObj(TYPE_EBENTO, out, null);
				Uri eventUri = mMusubi.getFeed().insert(obj);
				setEventObjUri(eventUri);
			} else {
				Obj obj = new MemObj(TYPE_APPSTATE, out, null, ++mLastInt);
				mMusubi.objForUri(mCurrentUri).getSubfeed().postObj(obj);
			}
		} catch (JSONException e) {
			Log.e(TAG, "Failed to post JSON", e);
		}
	}
	
	public void setEventObjUri(Uri objUri) {
		// previous uri
		if (mCurrentUri != null) {
			mMusubi.objForUri(mCurrentUri).getSubfeed().unregisterStateObserver(mStateObserver);
		}

        // new uri
		mCurrentUri = objUri;
		
		DbObj dbObj = mMusubi.objForUri(objUri);
		Long localId = dbObj.getLocalId();
		
		LatestObj latestObj = null;
		latestObj = fetchLatestObj(localId);
		if (latestObj != null && latestObj.json.has(STATE)) {
			JSONObject stateObj = latestObj.json.optJSONObject(STATE);

			if (stateObj == null) {
				initData();
				mLastInt = 0;
			} else {
				setNewStateObj(stateObj);
				mLastInt = latestObj.intKey;
			}
		}

		DbFeed dbFeed = mMusubi.objForUri(mCurrentUri).getContainingFeed();
		List<DbIdentity> members = dbFeed.getMembers();
		for (DbIdentity member : members) {
			if (member.isOwned()) {
				mLocalContactId = member.getId();
				mLocalName = member.getName();
			}
		}
		if (DEBUG) {
			Log.d(TAG, "setEventObjUri");
			Log.d(TAG, "  mLocalContactId=" + mLocalContactId);
			Log.d(TAG, "  mLocalName=" + mLocalName);
		}
		
		mMusubi.objForUri(mCurrentUri).getSubfeed().registerStateObserver(mStateObserver);
	}
	
	private final FeedObserver mStateObserver = new FeedObserver() {
		@Override
		synchronized public void onUpdate(DbObj obj) {
			if (DEBUG) Log.d(TAG, "onUpdate:" + obj.toString());

			// ignore
			if (obj == null || obj.getJson() == null || !obj.getJson().has(STATE)) {
				if (DEBUG) Log.d(TAG, "onUpdate: ignore-1");
				return;
			}

			// notify refresh (always)
			Handler handler = new Handler();
			handler.post(new Runnable(){
				public void run(){
					for (OnStateUpdatedListener listener : mListenerList) {
						listener.onStateUpdated();
					}
				}
			});
			
			if (mEvent == null) {
				if (DEBUG) Log.d(TAG, "onUpdate: ignore-2");
				return;
			}
			
			JSONObject stateObj = null;
			stateObj = obj.getJson().optJSONObject(STATE);
			try {
				if (!isValidEvent(stateObj.getJSONObject(EVENT).optString(EVENT_UUID))) {
					if (DEBUG) Log.d(TAG, "onUpdate: ignore-3");
					return;
				}
			} catch (JSONException e) {
				Log.e(TAG, "Failed to get JSON", e);
				return;
			}

			// set new state
			setNewStateObj(stateObj);
			
			mLastInt = (obj.getIntKey() == null) ? 0 : obj.getIntKey();
			if (DEBUG) Log.d(TAG, "onUpdate - mLastInt: " + mLastInt);
			
			// initial post
			if (mbInitialEvent && obj.getJson().has(EVENT_INIT)) {
				mInitEventListener.onEventInitialized();
				mbInitialEvent = false;
			}
		}
	};

	// ----------------------------------------------------------
	// Private
	// ----------------------------------------------------------
	private void initData() {
		mEvent = null;
		mPeopleList = null;
		mMemberNameCache = null;
		mCurrentUri = null;
		mLocalContactId = null;
		mLocalName = null;
		mbInitialEvent = false;
		mInitEventListener = null;
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
		fetchEventObj(mCurrentUri, stateObj, mEvent);
	}
	
	private boolean fetchEventObj(Uri objUri, JSONObject stateObj, Event event) {
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
			DbFeed dbFeed = mMusubi.objForUri(objUri).getSubfeed();
			try {
				DbIdentity id = dbFeed.userForGlobalId(event.creContactId);	
				event.creatorName = (id != null ? id.getName() : "");
			} catch (Exception e) {
				event.creatorName = "";
			}
			
			boolean bImage = eventObj.optBoolean(EVENT_HAS_IMAGE);
			if (bImage) {
				event.image = getEventImage(objUri, event.uuid);
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
					DbFeed dbFeed = mMusubi.objForUri(mCurrentUri).getSubfeed();
					DbIdentity id = dbFeed.userForGlobalId(people.contactId);
					item.image = id.getPicture();
					item.name = id.getName();
					
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

	private Bitmap getEventImage(Uri objUri, String eventUuid) {
		Bitmap bitmap = null;
		int targetWidth = BitmapHelper.MAX_IMAGE_WIDTH;
		int targetHeight = BitmapHelper.MAX_IMAGE_HEIGHT;
		float degrees = 0;

		DbFeed dbFeed = mMusubi.objForUri(objUri).getSubfeed();

		Cursor c = dbFeed.query();
		int numberOfObjInSubFeed = c.getCount();
		c.moveToFirst();
		for (int i = 0; i < c.getCount(); i++) {
			Obj object = mMusubi.objForCursor(c);
			bitmap = parseEventImage(object, eventUuid);
			if (bitmap != null) break;
			c.moveToNext();
		}
		c.close();
		
		// if no images in subfeed
		if (numberOfObjInSubFeed == 0 && bitmap == null) {
			bitmap = parseEventImage(mMusubi.objForUri(objUri), eventUuid);
		}

		// dummy
		if (bitmap == null) {
			bitmap = BitmapHelper.getDummyBitmap(targetWidth, targetHeight);
		} else {
			bitmap = BitmapHelper.getResizedBitmap(bitmap, targetWidth, targetHeight, degrees);
		}

		return bitmap;
	}
	
	private Bitmap parseEventImage(Obj object, String eventUuid) {
		Bitmap bitmap = null;
		if (object != null && object.getJson() != null && object.getJson().has(EVENT_IMAGE)) {
			JSONObject imageObj = object.getJson().optJSONObject(EVENT_IMAGE);
			if (eventUuid.equals(imageObj.optString(EVENT_IMAGE_UUID))) {
				byte[] byteArray = Base64.decode(object.getJson().optString(B64JPGTHUMB), Base64.DEFAULT);
				bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			}
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
