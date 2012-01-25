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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TimePicker;

import com.kazuyayokoyama.android.apps.ebento.R;
import com.kazuyayokoyama.android.apps.ebento.io.Event;
import com.kazuyayokoyama.android.apps.ebento.io.EventManager;
import com.kazuyayokoyama.android.apps.ebento.ui.quickaction.ActionItem;
import com.kazuyayokoyama.android.apps.ebento.ui.quickaction.QuickAction;
import com.kazuyayokoyama.android.apps.ebento.util.BitmapHelper;
import com.kazuyayokoyama.android.apps.ebento.util.DateTimeUtils;
import com.kazuyayokoyama.android.apps.ebento.util.JpgFileHelper;
import com.kazuyayokoyama.android.apps.ebento.util.UIUtils;

public class EditFragment extends Fragment {
	
	private static final String TAG = "EditFragment";

	private static final int MAX_IMG_WIDTH_PHONE = 320;
	private static final int MAX_IMG_HEIGHT_PHONE = 240;
	private static final int MAX_IMG_WIDTH_TABLET = 320;
	private static final int MAX_IMG_HEIGHT_TABLET = 240;
	
	private static final int REQUEST_IMAGE_CAPTURE = 0;
	private static final int REQUEST_GALLERY = 1;
	private static final int REQUEST_REMOVE = 2;
	
	private static final String INSTANCE_STATE_IMAGE_CHANGED = "imageChanged";

    private static Bitmap sImageOrg = null;
    
	private QuickAction mQuickAction;
    private ViewGroup mRootView;
    private EditText mTitle;
    private EditText mPlace;
    private EditText mDetails;
    private Button mStartDateButton;
    private Button mEndDateButton;
    private Button mStartTimeButton;
    private Button mEndTimeButton;
    private DateTimeUtils.DateTime mStartDateTime = new DateTimeUtils.DateTime();
    private DateTimeUtils.DateTime mEndDateTime = new DateTimeUtils.DateTime();
    private ImageView mImageView;
    private Button mImageButton;
    
    private EventManager mManager = EventManager.getInstance();
	private boolean mbNewEventMode = true;
	private boolean mbImageChanged = false;
	private int mTargetWidth;
	private int mTargetHeight;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	if (savedInstanceState != null) {
    		mbImageChanged = savedInstanceState.getBoolean(INSTANCE_STATE_IMAGE_CHANGED);
    	}
    	
    	mTargetWidth = (UIUtils.isTablet(getActivity()) ? MAX_IMG_WIDTH_TABLET : MAX_IMG_WIDTH_PHONE);
    	mTargetHeight = (UIUtils.isTablet(getActivity()) ? MAX_IMG_HEIGHT_TABLET : MAX_IMG_HEIGHT_PHONE);

        // Check if this activity launched from internal activity or not
     	if (getActivity().getIntent().hasExtra(EditActivity.EXTRA_EDIT)) {
     		mbNewEventMode = false;
   		}
    	
    	// Event
    	Event event = new Event();
    	if (!mbNewEventMode) {
    		event = mManager.getEvent();
    	}
    	
    	// View
        mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_edit, container);
        
        // EditText
        View.OnFocusChangeListener unfocusListener = new View.OnFocusChangeListener(){
    		@Override
    		public void onFocusChange(View v, boolean flag){
    			if(flag == false){
    				InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
    						Context.INPUT_METHOD_SERVICE);
    				imm.hideSoftInputFromWindow(v.getWindowToken(),0);
    			}
    		}
    	};
        mTitle = (EditText) mRootView.findViewById(R.id.title);
        mTitle.setOnFocusChangeListener(unfocusListener);
        if (!mbNewEventMode) mTitle.setText(event.title);
        mPlace = (EditText) mRootView.findViewById(R.id.place);
        mPlace.setOnFocusChangeListener(unfocusListener);
        if (!mbNewEventMode) mPlace.setText(event.place);
        mDetails = (EditText) mRootView.findViewById(R.id.details);
        mDetails.setOnFocusChangeListener(unfocusListener);
        if (!mbNewEventMode) mDetails.setText(event.details);

        // Date & Time Picker
        // TODO : All day option
        if (mbNewEventMode) {
            // start
        	mStartDateTime = DateTimeUtils.getRoundedCurrentDateTime();
        	// end
        	mEndDateTime = DateTimeUtils.getOneHourAheadEndTimeFromStartTime(mStartDateTime);
        } else {
        	// start
        	mStartDateTime.year = event.startDate.year;
        	mStartDateTime.month = event.startDate.month;
        	mStartDateTime.day = event.startDate.day;
        	mStartDateTime.hour = event.startTime.hour;
        	mStartDateTime.minute = event.startTime.minute;
        	// end
        	mEndDateTime.year = event.endDate.year;
        	mEndDateTime.month = event.endDate.month;
        	mEndDateTime.day = event.endDate.day;
        	mEndDateTime.hour = event.endTime.hour;
        	mEndDateTime.minute = event.endTime.minute;
        }
        mStartDateButton = (Button) mRootView.findViewById(R.id.start_date_button);
        mEndDateButton = (Button) mRootView.findViewById(R.id.end_date_button);
        mStartTimeButton = (Button) mRootView.findViewById(R.id.start_time_button);
        mEndTimeButton = (Button) mRootView.findViewById(R.id.end_time_button);
        setDateTimeButtonText();
        
        // Date
        mStartDateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	final DatePickerDialog datePickerDialog = new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            	mStartDateTime.year = year;
                            	mStartDateTime.month = (monthOfYear + 1);
                            	mStartDateTime.day = dayOfMonth;
                                
                            	mEndDateTime = DateTimeUtils.getEndTimeFromStartTime(mStartDateTime, mEndDateTime);
                            	setDateTimeButtonText();
                            }
                        },
                        mStartDateTime.year, (mStartDateTime.month - 1), mStartDateTime.day);
                datePickerDialog.show();
            }
        });

        mEndDateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	final DatePickerDialog datePickerDialog = new DatePickerDialog(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            	mEndDateTime.year = year;
                            	mEndDateTime.month = (monthOfYear + 1);
                            	mEndDateTime.day = dayOfMonth;
                                
                            	mStartDateTime = DateTimeUtils.getStartTimeFromEndTime(mStartDateTime, mEndDateTime);
                            	setDateTimeButtonText();
                            }
                        },
                        mEndDateTime.year, (mEndDateTime.month - 1), mEndDateTime.day);
                datePickerDialog.show();
            }
        });

        // Time
        mStartTimeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	final TimePickerDialog timePickerDialog = new FiveMinutesIntervalTimePickerDialog(
                        getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            	mStartDateTime.hour = hourOfDay;
                            	mStartDateTime.minute = minute;
                            	
                            	mEndDateTime = DateTimeUtils.getEndTimeFromStartTime(mStartDateTime, mEndDateTime);
                            	setDateTimeButtonText();
                            }
                        },
                        mStartDateTime.hour, mStartDateTime.minute, true);
            	timePickerDialog.show();
            }
        });
        
        mEndTimeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	final TimePickerDialog timePickerDialog = new FiveMinutesIntervalTimePickerDialog(
                        getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            	mEndDateTime.hour = hourOfDay;
                            	mEndDateTime.minute = minute;

                            	mStartDateTime = DateTimeUtils.getStartTimeFromEndTime(mStartDateTime, mEndDateTime);
                            	setDateTimeButtonText();
                            }
                        },
                        mEndDateTime.hour, mEndDateTime.minute, true);
            	timePickerDialog.show();
            }
        });
    	
        // Image
        mImageButton = (Button) mRootView.findViewById(R.id.image_button);
        mImageButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	goPicture(v);
            }
        });
        
        mImageView = (ImageView) mRootView.findViewById(R.id.image);
        if (mbNewEventMode) {
        	if (!mbImageChanged) {
        		sImageOrg = null;
        	}
        } else {
        	if (!mbImageChanged) {
        		sImageOrg = event.image;
        	}
        }

    	if (sImageOrg != null) {
    		mImageView.setImageBitmap(
    				BitmapHelper.getResizedBitmap(
    						sImageOrg, mTargetWidth, mTargetHeight, 0));
        	mImageView.setVisibility(View.VISIBLE);
    	} else {
        	mImageView.setVisibility(View.GONE);
    	}

		// Quick Action
		ActionItem cameraItem = new ActionItem(REQUEST_IMAGE_CAPTURE,
				getResources().getString(R.string.label_camera),
				getResources().getDrawable(R.drawable.ic_menu_camera));
		ActionItem galleryItem = new ActionItem(REQUEST_GALLERY, 
				getResources().getString(R.string.label_gallery), 
				getResources().getDrawable(R.drawable.ic_menu_gallery));
		ActionItem removeItem = new ActionItem(REQUEST_REMOVE, 
				getResources().getString(R.string.label_remove_picture), 
				getResources().getDrawable(R.drawable.ic_menu_remove_picture));

		mQuickAction = new QuickAction(getActivity());
		mQuickAction.addActionItem(cameraItem);
		mQuickAction.addActionItem(galleryItem);
		mQuickAction.addActionItem(removeItem);
		mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
			@Override
			public void onItemClick(QuickAction quickAction, int pos, int actionId) {
				if (actionId == REQUEST_IMAGE_CAPTURE) {
					goCamera();
				} else if (actionId == REQUEST_GALLERY) {
					goGallery();
				} else {
					goRemovePicture();
				}
			}
		});
		mQuickAction.setOnDismissListener(new QuickAction.OnDismissListener() {
			@Override
			public void onDismiss() {
			}
		});
        
        return mRootView;
    }

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(INSTANCE_STATE_IMAGE_CHANGED, mbImageChanged);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_save:
        	goSave();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			try {
				// uncomment below if you want to get small image
				// Bitmap b = (Bitmap)data.getExtras().get("data");

				File tmpFile = JpgFileHelper.getTmpFile();
				if (tmpFile.exists() && tmpFile.length() > 0) {
					float degrees = 0;
					try {
						ExifInterface exif = new ExifInterface(
								tmpFile.getPath());
						switch (exif.getAttributeInt(
								ExifInterface.TAG_ORIENTATION,
								ExifInterface.ORIENTATION_NORMAL)) {
						case ExifInterface.ORIENTATION_ROTATE_90:
							degrees = 90;
							break;
						case ExifInterface.ORIENTATION_ROTATE_180:
							degrees = 180;
							break;
						case ExifInterface.ORIENTATION_ROTATE_270:
							degrees = 270;
							break;
						default:
							degrees = 0;
							break;
						}
						Log.d(TAG, exif
								.getAttribute(ExifInterface.TAG_ORIENTATION));
					} catch (IOException e) {
						e.printStackTrace();
					}

					sImageOrg = BitmapHelper.getResizedBitmap(tmpFile,
							BitmapHelper.MAX_IMAGE_WIDTH,
							BitmapHelper.MAX_IMAGE_HEIGHT, degrees);
					mbImageChanged = true;

					// ImageView
	        		mImageView.setImageBitmap(
	        				BitmapHelper.getResizedBitmap(
	        						sImageOrg, mTargetWidth, mTargetHeight, 0));
	            	mImageView.setVisibility(View.VISIBLE);
	            	mRootView.invalidate();
	            	
					tmpFile.delete();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setDateTimeButtonText() {
        mStartDateButton.setText(
    			DateTimeUtils.getDateWithShortFormat(
    					mStartDateTime.year, mStartDateTime.month, mStartDateTime.day));
        mEndDateButton.setText(
    			DateTimeUtils.getDateWithShortFormat(
    					mEndDateTime.year, mEndDateTime.month, mEndDateTime.day));
        mStartTimeButton.setText(DateTimeUtils.getTimeWithFormat(mStartDateTime));
        mEndTimeButton.setText(DateTimeUtils.getTimeWithFormat(mEndDateTime));
	}
		
	private void goSave() {
		// check input
		if ((mTitle.getText().toString().length() == 0) ||
			(mPlace.getText().toString().length() == 0) ||
			(mStartDateTime.year < 1970) ||
			(mStartDateTime.month < 1) ||
			(mStartDateTime.day < 1) ||
			(mStartDateTime.hour < 0) ||
			(mStartDateTime.minute < 0) ||
			(mEndDateTime.year < 1970) ||
			(mEndDateTime.month < 1) ||
			(mEndDateTime.day < 1) ||
			(mEndDateTime.hour < 0) ||
			(mEndDateTime.minute < 0)) {

			final AlertDialog.Builder checkDialog = new AlertDialog.Builder(getActivity())
					.setTitle(R.string.check_dialog_title)
					.setMessage(R.string.check_dialog_text)
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setCancelable(false)
					.setPositiveButton(getResources().getString(R.string.check_dialog_ok), 
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// nothing
						}
					});
			checkDialog.create().show();
			
			return;
		}
		
		Event event = new Event();
		
		event.title = mTitle.getText().toString();
		event.place = mPlace.getText().toString();
		event.details = mDetails.getText().toString();
		event.startDate.year = mStartDateTime.year;
		event.startDate.month = mStartDateTime.month;
		event.startDate.day = mStartDateTime.day;
		event.startDate.bAllDay = false;
		event.endDate.year = mEndDateTime.year;
		event.endDate.month = mEndDateTime.month;
		event.endDate.day = mEndDateTime.day;
		event.endDate.bAllDay = false;
		event.startTime.hour = mStartDateTime.hour;
		event.startTime.minute = mStartDateTime.minute;
		event.endTime.hour = mEndDateTime.hour;
		event.endTime.minute = mEndDateTime.minute;
		event.image = sImageOrg;
		event.modDateMillis = System.currentTimeMillis();
		event.modContactId = mManager.getLocalContactId();
		
		if (mbNewEventMode) {
			event.uuid = UUID.randomUUID().toString();
			event.creDateMillis = System.currentTimeMillis();
			event.creContactId = mManager.getLocalContactId();
			
			// message
			StringBuilder msg = new StringBuilder(getString(
					R.string.feed_msg_event_created));
			String dateTimeMsg = DateTimeUtils.getDateAndTimeWithShortFormat(
					false,
	        		event.startDate.year, event.startDate.month, event.startDate.day,
	        		event.startTime.hour, event.startTime.minute);
			String htmlMsg = UIUtils.getHtmlString(event.title, dateTimeMsg, msg.toString());
			
			mManager.createEvent(event, htmlMsg);
			
			// back to home
			Intent intent = new Intent();
			getActivity().setResult(Activity.RESULT_OK, intent);
			getActivity().finish();
			
		} else {
			Event prevEvent = mManager.getEvent();
			event.uuid = prevEvent.uuid;
			event.creDateMillis = prevEvent.creDateMillis;
			event.creContactId = prevEvent.creContactId;

			// message
			StringBuilder msg = new StringBuilder(getString(
					R.string.feed_msg_event_updated));
			String dateTimeMsg = DateTimeUtils.getDateAndTimeWithShortFormat(
					false,
	        		event.startDate.year, event.startDate.month, event.startDate.day,
	        		event.startTime.hour, event.startTime.minute);
			String htmlMsg = UIUtils.getHtmlString(event.title, dateTimeMsg, msg.toString());
			
			mManager.updateEvent(event, htmlMsg);

			// back to home
			getActivity().finish();
		}
	}

	private void goPicture(View view) {
		mQuickAction.show(view);
		mQuickAction.setAnimStyle(QuickAction.ANIM_GROW_FROM_CENTER);
	}

	private void goCamera() {
		File tmpFile = JpgFileHelper.getTmpFile();
		Intent intent = new Intent();
		intent.setAction("android.media.action.IMAGE_CAPTURE");
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpFile));
		startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
	}

	public void goGallery() {
		File tmpFile = JpgFileHelper.getTmpFile();
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmpFile));
		startActivityForResult(intent, REQUEST_GALLERY);
	}
	
	private void goRemovePicture() {
    	mImageView.setVisibility(View.GONE);
    	mImageView.setImageBitmap(null);
    	sImageOrg = null;
    	mbImageChanged = true;
	}

    
    // TimePickerDialog with 5 minutes interval
	private class FiveMinutesIntervalTimePickerDialog extends TimePickerDialog {

		public FiveMinutesIntervalTimePickerDialog(Context context, int theme,
				OnTimeSetListener callBack, int hourOfDay, int minute,
				boolean is24HourView) {
			super(context, theme, callBack, hourOfDay, minute, is24HourView);
		}

		public FiveMinutesIntervalTimePickerDialog(Context context,
				OnTimeSetListener callBack, int hourOfDay, int minute,
				boolean is24HourView) {
			super(context, callBack, hourOfDay, minute, is24HourView);
		}

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			// Don't use function getRoundedMinutes() for avoiding StackOverflow
	        int[] up = {1,6,11,16,21,26,31,36,41,46,51,56};
	        int[] down = {4,9,14,19,24,29,34,39,44,49,54,59};
	        if (Arrays.binarySearch(up, minute) >= 0){
	            if(minute == 56){
	            	view.setCurrentMinute(0);
	            } else {
		        	view.setCurrentMinute(minute + 4);
	            }
	        } else if (Arrays.binarySearch(down, minute) >= 0){
	        	view.setCurrentMinute(minute - 4);
	        }
		}
    	
    };
}
