package org.linphone;
/*
HistoryFragment.java
Copyright (C) 2012  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.linphone.api.ColorGenerator;
import org.linphone.api.Config;
import org.linphone.api.TextDrawable;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCallLog.CallStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class HistorySimpleFragment extends BaseFragment implements OnClickListener, OnItemClickListener {
    private ListView historyList;
    private LayoutInflater mInflater;
    private TextView noCallHistory, noMissedCallHistory;//allCalls, missedCalls,deleteAll,
    private ImageView edit, ok;
    private boolean onlyDisplayMissedCalls, isEditMode;
    private List<LinphoneCallLog> mLogs;
    HashMap<String, Integer> mappingList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mInflater = inflater;
        View view = inflater.inflate(R.layout.history_simple, container, false);


        mappingList = new HashMap<>();
        noCallHistory = (TextView) view.findViewById(R.id.noCallHistory);
        noMissedCallHistory = (TextView) view.findViewById(R.id.noMissedCallHistory);

        historyList = (ListView) view.findViewById(R.id.historyList);
        historyList.setOnItemClickListener(this);
        registerForContextMenu(historyList);


//        deleteAll = (TextView) view.findViewById(R.id.deleteAll);
//        deleteAll.setOnClickListener(this);
//        deleteAll.setVisibility(View.INVISIBLE);
//
//        allCalls = (TextView) view.findViewById(R.id.allCalls);
//        allCalls.setOnClickListener(this);
//
//        missedCalls = (TextView) view.findViewById(R.id.missedCalls);
//        missedCalls.setOnClickListener(this);
//
//        allCalls.setEnabled(false);
        onlyDisplayMissedCalls = false;

        edit = (ImageView) view.findViewById(R.id.edit);
        edit.setOnClickListener(this);

        ok = (ImageView) view.findViewById(R.id.ok);
        ok.setOnClickListener(this);

        return view;
    }

    private void removeNotMissedCallsFromLogs() {
        if (onlyDisplayMissedCalls) {
            List<LinphoneCallLog> missedCalls = new ArrayList<LinphoneCallLog>();
            for (LinphoneCallLog log : mLogs) {
                if (log.getStatus() == CallStatus.Missed) {
                    missedCalls.add(log);
                }
            }
            mLogs = missedCalls;
        }
    }

    private boolean hideHistoryListAndDisplayMessageIfEmpty() {
        removeNotMissedCallsFromLogs();
        if (mLogs.isEmpty()) {
            edit.setVisibility(View.GONE);
            ok.setVisibility(View.GONE);
            if (onlyDisplayMissedCalls) {
                noMissedCallHistory.setVisibility(View.VISIBLE);
            } else {
                noCallHistory.setVisibility(View.VISIBLE);
            }
            historyList.setVisibility(View.GONE);
            return true;
        } else {
            noCallHistory.setVisibility(View.GONE);
            noMissedCallHistory.setVisibility(View.GONE);
            historyList.setVisibility(View.VISIBLE);
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (LinphoneActivity.isInstanciated()) {
            LinphoneActivity.instance().selectMenu(FragmentsAvailable.HISTORY);

            if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
                LinphoneActivity.instance().hideStatusBar();
            }
        }

        mLogs = Arrays.asList(LinphoneManager.getLc().getCallLogs());
        if (isEditMode) {
            ok.setVisibility(View.VISIBLE);
            edit.setVisibility(View.GONE);
        } else {
            ok.setVisibility(View.GONE);
            edit.setVisibility(View.VISIBLE);
        }

        if (!hideHistoryListAndDisplayMessageIfEmpty()) {
            historyList.setAdapter(new CallHistoryAdapter(getActivity()));
        }


    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, v.getId(), 0, getString(R.string.delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        LinphoneCallLog log = mLogs.get(info.position);
        LinphoneManager.getLc().removeCallLog(log);
        mLogs = Arrays.asList(LinphoneManager.getLc().getCallLogs());
        if (!hideHistoryListAndDisplayMessageIfEmpty()) {
            historyList.setAdapter(new CallHistoryAdapter(getActivity()));
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.allCalls) {
//			allCalls.setEnabled(false);
//			missedCalls.setEnabled(true);
            onlyDisplayMissedCalls = false;

            mLogs = Arrays.asList(LinphoneManager.getLc().getCallLogs());
        } else if (id == R.id.missedCalls) {
//			allCalls.setEnabled(true);
//			missedCalls.setEnabled(false);
            onlyDisplayMissedCalls = true;
        } else if (id == R.id.ok) {
            edit.setVisibility(View.VISIBLE);
            ok.setVisibility(View.GONE);
            hideDeleteAllButton();
            isEditMode = false;
        } else if (id == R.id.edit) {
            edit.setVisibility(View.GONE);
            ok.setVisibility(View.VISIBLE);
            showDeleteAllButton();
            isEditMode = true;
        } else if (id == R.id.deleteAll) {
            LinphoneManager.getLc().clearCallLogs();
            mLogs = new ArrayList<LinphoneCallLog>();
        }

        if (!hideHistoryListAndDisplayMessageIfEmpty()) {
            historyList.setAdapter(new CallHistoryAdapter(getActivity().getApplicationContext()));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        if (isEditMode) {
            LinphoneCallLog log = mLogs.get(position);
            LinphoneManager.getLc().removeCallLog(log);
            mLogs = Arrays.asList(LinphoneManager.getLc().getCallLogs());
            if (!hideHistoryListAndDisplayMessageIfEmpty()) {
                historyList.setAdapter(new CallHistoryAdapter(getActivity().getApplicationContext()));
            }
        } else {
            if (LinphoneActivity.isInstanciated()) {
                LinphoneCallLog log = mLogs.get(position);
                LinphoneAddress address;
                if (log.getDirection() == CallDirection.Incoming) {
                    address = log.getFrom();
                } else {
                    address = log.getTo();
                }
                LinphoneActivity.instance().setAddresGoToDialerAndCall(address.asStringUriOnly(), address.getDisplayName(), null);
            }
        }
    }

    private void hideDeleteAllButton() {
//		if (deleteAll == null || deleteAll.getVisibility() != View.VISIBLE) {
//			return;
//		}

        if (LinphoneActivity.instance().isAnimationDisabled()) {
//			deleteAll.setVisibility(View.INVISIBLE);
        } else {
            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_right_to_left);
            animation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
//					deleteAll.setVisibility(View.INVISIBLE);
                    animation.setAnimationListener(null);
                }
            });
//			deleteAll.startAnimation(animation);
        }
    }

    private void showDeleteAllButton() {
//		if (deleteAll == null || deleteAll.getVisibility() == View.VISIBLE) {
//			return;
//		}

        if (LinphoneActivity.instance().isAnimationDisabled()) {
//			deleteAll.setVisibility(View.VISIBLE);
        } else {
            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_left_to_right);
            animation.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
//					deleteAll.setVisibility(View.VISIBLE);
                    animation.setAnimationListener(null);
                }
            });
//			deleteAll.startAnimation(animation);
        }
    }

    class CallHistoryAdapter extends BaseAdapter {
        private Bitmap missedCall, outgoingCall, incomingCall;
        private Bitmap bitmapUnknown;
        TextDrawable drawable;
        ColorGenerator generator;

        CallHistoryAdapter(Context aContext) {
            missedCall = BitmapFactory.decodeResource(getResources(), R.drawable.call_status_missed);
            generator = ColorGenerator.MATERIAL;
            bitmapUnknown = BitmapFactory.decodeResource(LinphoneActivity.instance().getResources(), R.drawable.unknown_small);
            if (!onlyDisplayMissedCalls) {
                outgoingCall = BitmapFactory.decodeResource(getResources(), R.drawable.call_status_outgoing);
                incomingCall = BitmapFactory.decodeResource(getResources(), R.drawable.call_status_incoming);
            }
        }

        public int getCount() {
            return mLogs.size();
        }

        public Object getItem(int position) {
            return mLogs.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("SimpleDateFormat")
        private String timestampToHumanDate(Calendar cal) {
            SimpleDateFormat dateFormat;
            if (isToday(cal)) {
                //return getString(R.string.today);
                // showing time for today instead of date
                dateFormat = new SimpleDateFormat(getResources().getString(R.string.today_date_format));
            } else if (isYesterday(cal)) {
                return getString(R.string.yesterday);
            } else {
                dateFormat = new SimpleDateFormat(getResources().getString(R.string.history_date_format));
            }

            return dateFormat.format(cal.getTime());
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            if (cal1 == null || cal2 == null) {
                return false;
            }

            return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                    cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
        }

        private boolean isToday(Calendar cal) {
            return isSameDay(cal, Calendar.getInstance());
        }

        private boolean isYesterday(Calendar cal) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.roll(Calendar.DAY_OF_MONTH, -1);
            return isSameDay(cal, yesterday);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.history_cell_simple, parent, false);
            }

            final LinphoneCallLog log = mLogs.get(position);
            long timestamp = log.getTimestamp();
            final LinphoneAddress address;

            TextView contact = (TextView) view.findViewById(R.id.sipUri);
            contact.setSelected(true); // For automated horizontal scrolling of long texts
            ImageView delete = (ImageView) view.findViewById(R.id.delete);
            ImageView callDirection = (ImageView) view.findViewById(R.id.icon);

            TextView separator = (TextView) view.findViewById(R.id.call_date);
            Calendar logTime = Calendar.getInstance();
            logTime.setTimeInMillis(timestamp);
            separator.setText(timestampToHumanDate(logTime));


//            if (position > 0) {
//                LinphoneCallLog previousLog = mLogs.get(position - 1);
//                long previousTimestamp = previousLog.getTimestamp();
//                Calendar previousLogTime = Calendar.getInstance();
//                previousLogTime.setTimeInMillis(previousTimestamp);
//
//                if (isSameDay(previousLogTime, logTime)) {
//                    separator.setVisibility(View.GONE);
//                } else {
//                    separator.setVisibility(View.VISIBLE);
//                }
//            } else {
//                separator.setVisibility(View.VISIBLE);
//            }

            if (log.getDirection() == CallDirection.Incoming) {
                address = log.getFrom();
                if (log.getStatus() == CallStatus.Missed) {
                    callDirection.setImageBitmap(missedCall);
                } else {
                    callDirection.setImageBitmap(incomingCall);
                }
            } else {
                address = log.getTo();
                callDirection.setImageBitmap(outgoingCall);
            }

            Contact c = ContactsManager.getInstance().findContactWithAddress(getActivity().getContentResolver(), address);
            String displayName = null;
            final String sipUri = address.asStringUriOnly();
            if (c != null) {
                displayName = c.getName();
            }

            if (displayName == null) {
                if (getResources().getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(sipUri)) {
                    contact.setText(address.getUserName());
//                    Log.e("displayName: ","1111");
                } else {
                    contact.setText(sipUri);
//                    Log.e("displayName: ", "2222");
                }
            } else {
//                if (getResources().getBoolean(R.bool.only_display_username_if_unknown) && LinphoneUtils.isSipAddress(address.getDisplayName())) {
//                    contact.setText(displayName);
//                    Log.e("displayName: ", "3333");
//                } else {
//                    contact.setText(sipUri);
//                    Log.e("displayName: ", "4444");
//                }
                contact.setText(displayName);
                //Log.e("displayName: ", "3333");
            }
            view.setTag(sipUri);

            callDirection.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (LinphoneActivity.isInstanciated()) {
                        LinphoneActivity.instance().displayHistoryDetail(sipUri, log);
                    }
                }
            });

            ImageView icon = (ImageView) view.findViewById(R.id.UserProfile);
            ImageView character_icon = (ImageView) view.findViewById(R.id.character_icon);
            if (c == null) {
                icon.setImageBitmap(bitmapUnknown);
            } else {
                if (c.getPhoto() != null) {
                    icon.setVisibility(View.VISIBLE);
                    character_icon.setVisibility(View.INVISIBLE);
                    icon.setImageBitmap(c.getPhoto());
                } else if (c.getPhotoUri() != null) {
                    icon.setVisibility(View.VISIBLE);
                    character_icon.setVisibility(View.INVISIBLE);
                    LinphoneUtils.setImagePictureFromUri(getActivity(), icon, c.getPhotoUri(), c.getThumbnailUri(), R.drawable.unknown_small);
                } else {
                    icon.setVisibility(View.INVISIBLE);
                    character_icon.setVisibility(View.VISIBLE);
                    int color = generator.getRandomColor();
                    if (!mappingList.containsKey(contact.getText().toString())) {
                        mappingList.put(contact.getText().toString(), color);
                    }

                    Log.e("CONTACT: ", mappingList.toString());
                    if (mappingList.containsKey(contact.getText().toString())) {
                        drawable = TextDrawable.builder()
                                .buildRoundRect(contact.getText().toString().substring(0, 1).toUpperCase(), mappingList.get(contact.getText().toString()), 96);
                    } else {
                        drawable = TextDrawable.builder()
                                .buildRoundRect(contact.getText().toString().substring(0, 1).toUpperCase(), color, 96);
                    }
                    character_icon.setImageDrawable(drawable);

                }
            }


            if (isEditMode) {
                delete.setVisibility(View.VISIBLE);
            } else {
                delete.setVisibility(View.GONE);

            }

            return view;
        }
    }
}