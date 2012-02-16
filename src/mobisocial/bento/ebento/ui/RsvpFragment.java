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

import mobisocial.bento.ebento.io.Event;
import mobisocial.bento.ebento.io.EventManager;
import mobisocial.bento.ebento.io.People;
import mobisocial.bento.ebento.util.DateTimeUtils;
import mobisocial.bento.ebento.util.UIUtils;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import mobisocial.bento.ebento.R;

public class RsvpFragment extends Fragment {
    public interface OnRsvpSelectedListener {
        public void onRsvpSelected(int state);
    }

    private EventManager mManager = EventManager.getInstance();
    private ViewGroup mRootView;
    private Button mYesButton;
    private Button mMaybeButton;
    private Button mNoButton;
    private OnRsvpSelectedListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_rsvp, container);
        mYesButton = (Button) mRootView.findViewById(R.id.yes_button);
        mMaybeButton = (Button) mRootView.findViewById(R.id.maybe_button);
        mNoButton = (Button) mRootView.findViewById(R.id.no_button);
        
        return mRootView;
    }

	@Override
	public void onAttach(SupportActivity activity) {
		super.onAttach(activity);

        try {
        	mListener = (OnRsvpSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnRsvpSelectedListener");
        }
	}

	@Override
	public void onResume() {
    	super.onResume();

    	refreshView();
	}
	
	public void refreshView() {
        final Event event = mManager.getEvent();
        int currentState = mManager.getLocalState();
        
        mYesButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
    			// message
    			StringBuilder msg = new StringBuilder(getString(
    					R.string.feed_msg_yes, mManager.getLocalName()));
    			String dateTimeMsg = DateTimeUtils.getDateAndTimeWithShortFormat(
    					false,
    	        		event.startDate.year, event.startDate.month, event.startDate.day,
    	        		event.startTime.hour, event.startTime.minute);
    			String htmlMsg = UIUtils.getHtmlString(event.title, dateTimeMsg, msg.toString());
    			mManager.updatePeople(mManager.getLocalContactId(), People.STATE_YES, htmlMsg);
            	
            	setSelected(mYesButton);
            	setDeselected(mMaybeButton);
            	setDeselected(mNoButton);
            	
            	mListener.onRsvpSelected(People.STATE_YES);
            }
        });
        mMaybeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
    			// message
    			StringBuilder msg = new StringBuilder(getString(
    					R.string.feed_msg_maybe, mManager.getLocalName()));
    			String dateTimeMsg = DateTimeUtils.getDateAndTimeWithShortFormat(
    					false,
    	        		event.startDate.year, event.startDate.month, event.startDate.day,
    	        		event.startTime.hour, event.startTime.minute);
    			String htmlMsg = UIUtils.getHtmlString(event.title, dateTimeMsg, msg.toString());
    			mManager.updatePeople(mManager.getLocalContactId(), People.STATE_MAYBE, htmlMsg);
    			
            	setDeselected(mYesButton);
            	setSelected(mMaybeButton);
            	setDeselected(mNoButton);

            	mListener.onRsvpSelected(People.STATE_MAYBE);
            }
        });
        mNoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
    			// message
    			StringBuilder msg = new StringBuilder(getString(
    					R.string.feed_msg_no, mManager.getLocalName()));
    			String dateTimeMsg = DateTimeUtils.getDateAndTimeWithShortFormat(
    					false,
    	        		event.startDate.year, event.startDate.month, event.startDate.day,
    	        		event.startTime.hour, event.startTime.minute);
    			String htmlMsg = UIUtils.getHtmlString(event.title, dateTimeMsg, msg.toString());
    			mManager.updatePeople(mManager.getLocalContactId(), People.STATE_NO, htmlMsg);
    			
            	setDeselected(mYesButton);
            	setDeselected(mMaybeButton);
            	setSelected(mNoButton);
            	
            	mListener.onRsvpSelected(People.STATE_NO);
            }
        });
        
        switch (currentState) {
        case People.STATE_YES:
        	setSelected(mYesButton);
        	setDeselected(mMaybeButton);
        	setDeselected(mNoButton);
        	break;
        case People.STATE_MAYBE:
        	setDeselected(mYesButton);
        	setSelected(mMaybeButton);
        	setDeselected(mNoButton);
        	break;
        case People.STATE_NO:
        	setDeselected(mYesButton);
        	setDeselected(mMaybeButton);
        	setSelected(mNoButton);
        	break;
        default:
        	setDeselected(mYesButton);
        	setDeselected(mMaybeButton);
        	setDeselected(mNoButton);
        	break;
        }
	}
    
    private void setSelected(Button b) {
        if (UIUtils.isHoneycomb()) {
        	b.setTextColor(getActivity().getResources().getColor(R.color.system_blue));
        } else {
        	b.setTextColor(getActivity().getResources().getColor(R.color.system_green));
        }
        b.setTypeface(null, Typeface.BOLD);
    }

    private void setDeselected(Button b) {
        b.setTextColor(Color.BLACK);
        b.setTypeface(null, Typeface.NORMAL);
    }
}
