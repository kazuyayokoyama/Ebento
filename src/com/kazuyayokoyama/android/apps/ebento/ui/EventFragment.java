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

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kazuyayokoyama.android.apps.ebento.R;
import com.kazuyayokoyama.android.apps.ebento.io.Event;
import com.kazuyayokoyama.android.apps.ebento.io.EventManager;
import com.kazuyayokoyama.android.apps.ebento.io.People;
import com.kazuyayokoyama.android.apps.ebento.util.BitmapHelper;
import com.kazuyayokoyama.android.apps.ebento.util.DateTimeUtils;
import com.kazuyayokoyama.android.apps.ebento.util.UIUtils;

public class EventFragment extends Fragment {
	//private static final String TAG = "EventFragment";
	
	private static final int MAX_IMG_WIDTH_PHONE = 320;
	private static final int MAX_IMG_HEIGHT_PHONE = 240;
	private static final int MAX_IMG_WIDTH_TABLET = 320;
	private static final int MAX_IMG_HEIGHT_TABLET = 240;

    private EventManager mManager = EventManager.getInstance();
	private ImageView mEventImage = null;
	private TextView mTitle = null;
	private TextView mDateTimeTop = null;
	private TextView mDateTimeBottom = null;
	private TextView mPlace = null;
	private TextView mDetails = null;
	private TextView mOrganizer = null;
	private TextView mOrganizerBy = null;
	private TextView mPeopleYes = null;
	private TextView mPeopleMaybe = null;
	private TextView mPeopleNo = null;
	private int mTargetWidth;
	private int mTargetHeight;
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event, container);

    	mTargetWidth = (UIUtils.isTablet(getActivity()) ? MAX_IMG_WIDTH_TABLET : MAX_IMG_WIDTH_PHONE);
    	mTargetHeight = (UIUtils.isTablet(getActivity()) ? MAX_IMG_HEIGHT_TABLET : MAX_IMG_HEIGHT_PHONE);
        
        mEventImage = (ImageView) root.findViewById(R.id.event_image);
        mTitle = (TextView) root.findViewById(R.id.event_title);
        mDateTimeTop = (TextView) root.findViewById(R.id.event_date_time_top);
        mDateTimeBottom = (TextView) root.findViewById(R.id.event_date_time_bottom);
        mPlace = (TextView) root.findViewById(R.id.event_place);
        mDetails = (TextView) root.findViewById(R.id.event_details);
        mOrganizer = (TextView) root.findViewById(R.id.event_organizer);
        mOrganizerBy = (TextView) root.findViewById(R.id.event_organizer_by);
        mPeopleYes = (TextView) root.findViewById(R.id.event_people_yes);
        mPeopleMaybe = (TextView) root.findViewById(R.id.event_people_maybe);
        mPeopleNo = (TextView) root.findViewById(R.id.event_people_no);
        
        return root;
    }

    
	@Override
	public void onResume() {
    	super.onResume();

    	refreshView();
	}
	
	public void refreshView() {
        Event event = mManager.getEvent();
        if (event.image == null) {
	        mEventImage.setImageBitmap(
	        		BitmapHelper.getResizedBitmap(
	        				BitmapFactory.decodeResource(getResources(), R.drawable.default_image), 
	        				mTargetWidth, mTargetHeight, 0));
        } else {
        	mEventImage.setImageBitmap(
	        		BitmapHelper.getResizedBitmap(
	        				event.image, mTargetWidth, mTargetHeight, 0));
        }
        
        mTitle.setText(event.title);

        // same day
        if ((event.startDate.year == event.endDate.year) &&
        	(event.startDate.month == event.endDate.month) &&
        	(event.startDate.day == event.endDate.day)) {

        	mDateTimeTop.setText(DateTimeUtils.getDateWithFormat(
            		event.startDate.year, event.startDate.month, event.startDate.day));

            String startTime = DateTimeUtils.getTimeWithFormat(
            		event.startDate.year, event.startDate.month, event.startDate.day,
            		event.startTime.hour, event.startTime.minute);
            
            String endTime = "";
            if (event.endTime.hour >= 0 && event.endTime.minute >= 0) {
                endTime = DateTimeUtils.getTimeWithFormat(
                		event.endDate.year, event.endDate.month, event.endDate.day,
                		event.endTime.hour, event.endTime.minute);
            }
            mDateTimeBottom.setText(startTime + getActivity().getString(R.string.event_time_to) + endTime);
        	
        } else {
        	String startDateTime = DateTimeUtils.getDateAndTimeWithShortFormat(
        			true,
            		event.startDate.year, event.startDate.month, event.startDate.day,
            		event.startTime.hour, event.startTime.minute)
            		+ "  " + getActivity().getString(R.string.event_time_to);
        	String endDateTime = DateTimeUtils.getDateAndTimeWithShortFormat(
        			true,
            		event.endDate.year, event.endDate.month, event.endDate.day,
            		event.endTime.hour, event.endTime.minute);
        	
        	mDateTimeTop.setText(startDateTime);
        	mDateTimeBottom.setText(endDateTime);
        	
        }
        
        mPlace.setText(event.place);
        mDetails.setText(event.details);
        if (event.creatorName == null || event.creatorName.length() == 0) {
        	mOrganizerBy.setVisibility(View.GONE);
        	mOrganizer.setVisibility(View.GONE);
        } else {
        	if (event.creContactId.equals(mManager.getLocalContactId())) {
        		mOrganizer.setText(event.creatorName + getActivity().getString(R.string.event_organizer_you));
        	} else {
        		mOrganizer.setText(event.creatorName);
        	}
        }
        mPeopleYes.setText(String.valueOf(mManager.getNumberOfState(People.STATE_YES)));
        mPeopleMaybe.setText(String.valueOf(mManager.getNumberOfState(People.STATE_MAYBE)));
        mPeopleNo.setText(String.valueOf(mManager.getNumberOfState(People.STATE_NO)));
	}
	
}
