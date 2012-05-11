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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

import mobisocial.bento.ebento.R;
import mobisocial.bento.ebento.io.Event;
import mobisocial.bento.ebento.io.EventManager;
import mobisocial.bento.ebento.io.EventManager.OnInitialEventListener;
import mobisocial.bento.ebento.ui.quickaction.ActionItem;
import mobisocial.bento.ebento.ui.quickaction.QuickAction;
import mobisocial.bento.ebento.util.BitmapHelper;
import mobisocial.bento.ebento.util.CalendarHelper;
import mobisocial.bento.ebento.util.DateTimeUtils;
import mobisocial.bento.ebento.util.DateTimeUtils.DateTime;
import mobisocial.bento.ebento.util.JpgFileHelper;
import mobisocial.bento.ebento.util.UIUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItem;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class EditFragment extends Fragment {
	
	private static final String TAG = "EditFragment";
	private static final Boolean DEBUG = UIUtils.isDebugMode();

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
    private CheckBox mAddCalCheckBox;
    private TextView mAddCalText;
    private LinearLayout mAddCalLayout;
    private TextView mAddCalSub;
    private Button mSaveButton;
    private Button mCancelButton;
    
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
                            	DateTime candidateStart = new DateTime();
                            	candidateStart.year = year;
                            	candidateStart.month = (monthOfYear + 1);
                            	candidateStart.day = dayOfMonth;
                            	candidateStart.hour = mStartDateTime.hour;
                            	candidateStart.minute = mStartDateTime.minute;
                            	if (DateTimeUtils.isPastDateTime(candidateStart)) {
                    				Toast.makeText(getActivity(), R.string.toast_edit_past_time, Toast.LENGTH_SHORT).show();
                            	} else {
	                            	mStartDateTime.year = year;
	                            	mStartDateTime.month = (monthOfYear + 1);
	                            	mStartDateTime.day = dayOfMonth;
	                                
	                            	mEndDateTime = DateTimeUtils.getEndTimeFromStartTime(mStartDateTime, mEndDateTime);
	                            	setDateTimeButtonText();
                            	}
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
                            	DateTime candidateEnd = new DateTime();
                            	candidateEnd.year = year;
                            	candidateEnd.month = (monthOfYear + 1);
                            	candidateEnd.day = dayOfMonth;
                            	candidateEnd.hour = mEndDateTime.hour;
                            	candidateEnd.minute = mEndDateTime.minute;

                            	DateTime candidateStart = new DateTime();
                            	candidateStart.year = mStartDateTime.year;
                            	candidateStart.month = mStartDateTime.month;
                            	candidateStart.day = mStartDateTime.day;
                            	candidateStart.hour = mStartDateTime.hour;
                            	candidateStart.minute = mStartDateTime.minute;
                            	candidateStart = DateTimeUtils.getStartTimeFromEndTime(candidateStart, candidateEnd);
                            	
                            	if (DateTimeUtils.isPastDateTime(candidateStart) || DateTimeUtils.isPastDateTime(candidateEnd)) {
                    				Toast.makeText(getActivity(), R.string.toast_edit_past_time, Toast.LENGTH_SHORT).show();
                            	} else {
	                            	mEndDateTime.year = year;
	                            	mEndDateTime.month = (monthOfYear + 1);
	                            	mEndDateTime.day = dayOfMonth;
	                                
	                            	mStartDateTime = DateTimeUtils.getStartTimeFromEndTime(mStartDateTime, mEndDateTime);
	                            	setDateTimeButtonText();
                            	}
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
                            	DateTime candidateStart = new DateTime();
                            	candidateStart.year = mStartDateTime.year;
                            	candidateStart.month = mStartDateTime.month;
                            	candidateStart.day = mStartDateTime.day;
                            	candidateStart.hour = hourOfDay;
                            	candidateStart.minute = minute;
                            	if (DateTimeUtils.isPastDateTime(candidateStart)) {
                    				Toast.makeText(getActivity(), R.string.toast_edit_past_time, Toast.LENGTH_SHORT).show();
                            	} else {
	                            	mStartDateTime.hour = hourOfDay;
	                            	mStartDateTime.minute = minute;
	                            	
	                            	mEndDateTime = DateTimeUtils.getEndTimeFromStartTime(mStartDateTime, mEndDateTime);
	                            	setDateTimeButtonText();
                            	}
                            }
                        },
                        mStartDateTime.hour, mStartDateTime.minute, false);
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
                            	DateTime candidateEnd = new DateTime();
                            	candidateEnd.year = mEndDateTime.year;
                            	candidateEnd.month = mEndDateTime.month;
                            	candidateEnd.day = mEndDateTime.day;
                            	candidateEnd.hour = hourOfDay;
                            	candidateEnd.minute = minute;

                            	DateTime candidateStart = new DateTime();
                            	candidateStart.year = mStartDateTime.year;
                            	candidateStart.month = mStartDateTime.month;
                            	candidateStart.day = mStartDateTime.day;
                            	candidateStart.hour = mStartDateTime.hour;
                            	candidateStart.minute = mStartDateTime.minute;
                            	candidateStart = DateTimeUtils.getStartTimeFromEndTime(candidateStart, candidateEnd);
                            	
                            	if (DateTimeUtils.isPastDateTime(candidateStart) || DateTimeUtils.isPastDateTime(candidateEnd)) {
                    				Toast.makeText(getActivity(), R.string.toast_edit_past_time, Toast.LENGTH_SHORT).show();
                            	} else {
	                            	mEndDateTime.hour = hourOfDay;
	                            	mEndDateTime.minute = minute;
	
	                            	mStartDateTime = DateTimeUtils.getStartTimeFromEndTime(mStartDateTime, mEndDateTime);
	                            	setDateTimeButtonText();
                            	}
                            }
                        },
                        mEndDateTime.hour, mEndDateTime.minute, false);
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
		
		// Add Cal
		mAddCalCheckBox = (CheckBox) mRootView.findViewById(R.id.add_cal_checkbox);
		mAddCalText = (TextView) mRootView.findViewById(R.id.add_cal_text);
		mAddCalLayout = (LinearLayout) mRootView.findViewById(R.id.add_cal_layout);
		mAddCalSub = (TextView) mRootView.findViewById(R.id.add_cal_sub);
		
		if (!mbNewEventMode) {
			mAddCalText.setVisibility(View.GONE);
			mAddCalLayout.setVisibility(View.GONE);
		} else {
			// not ICS
	        if (! UIUtils.isIceCreamSandwich()) {
	        	mAddCalSub.setVisibility(View.GONE);
	        }
		}

        // Save/Cancel Buttons
		mSaveButton = (Button) mRootView.findViewById(R.id.save_button);
		mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	goSave();
            }
        });
		mCancelButton = (Button) mRootView.findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	getActivity().finish();
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
		if (DEBUG) Log.d(TAG, "requestCode: " + requestCode + ", resultCode: " + resultCode);
		if (resultCode == Activity.RESULT_OK) {
			try {
				File imageFile = null;
				
				if (requestCode == REQUEST_IMAGE_CAPTURE) {
					
					imageFile = JpgFileHelper.getTmpFile();
					
				} else if (requestCode == REQUEST_GALLERY) {
					Uri uri = data.getData();
					if (uri == null || uri.toString().length() == 0) {
						return;
					}
					if (DEBUG) Log.d(TAG, "data URI: " + uri.toString());
					
					ContentResolver cr = getActivity().getContentResolver();
					String[] columns = { MediaColumns.DATA, MediaColumns.DISPLAY_NAME };
					Cursor c = cr.query(uri, columns, null, null, null);
					
					if (c != null && c.moveToFirst()) {
						if (c.getString(0) != null) {
							//regular processing for gallery files
							imageFile = new File(c.getString(0));
						} else {
							final InputStream is = getActivity().getContentResolver().openInputStream(uri);
							imageFile = JpgFileHelper.saveTmpFile(is);
							is.close();
						}
					} else {
						// http or https
						HttpURLConnection http = null;
						URL url = new URL(uri.toString());
						http = (HttpURLConnection)url.openConnection();
						http.setRequestMethod("GET");
						http.connect();
						
						final InputStream is = http.getInputStream();
						imageFile = JpgFileHelper.saveTmpFile(is);
						is.close();
						if (http != null) http.disconnect();
					}
				}
				
				if (imageFile.exists() && imageFile.length() > 0) {
					if (DEBUG) Log.d(TAG, "imageFile exists=" + imageFile.exists()
							+ " length=" + imageFile.length() + " path=" + imageFile.getPath());
					
					float degrees = 0;
					try {
						ExifInterface exif = new ExifInterface(
								imageFile.getPath());
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

					sImageOrg = BitmapHelper.getResizedBitmap(imageFile,
							BitmapHelper.MAX_IMAGE_WIDTH,
							BitmapHelper.MAX_IMAGE_HEIGHT, degrees);
					mbImageChanged = true;

					// ImageView
	        		mImageView.setImageBitmap(
	        				BitmapHelper.getResizedBitmap(
	        						sImageOrg, mTargetWidth, mTargetHeight, 0));
	            	mImageView.setVisibility(View.VISIBLE);
	            	mRootView.invalidate();
	            	
					imageFile.delete();
					JpgFileHelper.deleteTmpFile();
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
			final String textMsg = UIUtils.getPlainString(event.title, dateTimeMsg, msg.toString());
	
			// init event
			final Event finEvent = event;
			mManager.initEvent(event, textMsg, new OnInitialEventListener() {
				@Override
				public void onEventInitialized() {
					Event newEvent = finEvent;
					newEvent.creContactId = mManager.getLocalContactId();
					newEvent.modContactId = mManager.getLocalContactId();
					
					mManager.createEvent(newEvent, textMsg);
					
					// Add to Cal
					if (mAddCalCheckBox.isChecked()) {
						CalendarHelper.addEventToCalendar(getActivity(), newEvent);
					}
					
					// back to home
					Intent intent = new Intent();
					getActivity().setResult(Activity.RESULT_OK, intent);
					getActivity().finish();
				}
			});
			
			
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
			String textMsg = UIUtils.getPlainString(event.title, dateTimeMsg, msg.toString());
			
			mManager.updateEvent(event, textMsg, false);

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
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_PICK);
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
