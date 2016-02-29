package org.linphone;
/*
ChatListFragment.java
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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.mediastream.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
public class ChatListFragment extends BaseFragment implements OnClickListener, OnItemClickListener {
    private LayoutInflater mInflater;
    private List<String> mConversations, mDrafts;
    private ListView chatList;
    private TextView delete_all, noChatHistory;
    private  ImageView edit, newDiscussion;
    ImageView back;
    //	private ImageView clearFastChat;
//	private EditText fastNewChat;
    private boolean isEditMode = false;
    private boolean useLinphoneStorage;


    private ArrayList<String> removeChatList;

    HashMap<String, Integer> mappingList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;

        View view = inflater.inflate(R.layout.chatlist, container, false);
        chatList = (ListView) view.findViewById(R.id.chatList);
        chatList.setOnItemClickListener(this);
        registerForContextMenu(chatList);

//		ImageView tenantLogo  = (ImageView) view.findViewById(R.id.tenantLogo);
//		Config.loadImageFromStorage(getActivity(), tenantLogo, "logo.png");
        mappingList = new HashMap<>();
        removeChatList = new ArrayList<>();
        noChatHistory = (TextView) view.findViewById(R.id.noChatHistory);

        edit = (ImageView) view.findViewById(R.id.edit);
        edit.setOnClickListener(this);

        back = (ImageView) view.findViewById(R.id.back);
        back.setOnClickListener(this);

		newDiscussion = (ImageView) view.findViewById(R.id.newDiscussion);
		newDiscussion.setOnClickListener(this);

        delete_all = (TextView) view.findViewById(R.id.delete_all);
        delete_all.setOnClickListener(this);

//		clearFastChat = (ImageView) view.findViewById(R.id.clearFastChatField);
//		clearFastChat.setOnClickListener(this);
//
//		fastNewChat = (EditText) view.findViewById(R.id.newFastChat);

        return view;
    }

    private void hideAndDisplayMessageIfNoChat() {
        if (mConversations.size() == 0 && mDrafts.size() == 0) {
            noChatHistory.setVisibility(View.VISIBLE);
            edit.setVisibility(View.GONE);
            delete_all.setVisibility(View.GONE);
            chatList.setVisibility(View.GONE);
            back.setVisibility(View.GONE);
        } else {
            noChatHistory.setVisibility(View.GONE);
            chatList.setVisibility(View.VISIBLE);
            chatList.setAdapter(new ChatListAdapter(useLinphoneStorage));
        }
    }

    public void refresh() {
        mConversations = LinphoneActivity.instance().getChatList();
        mDrafts = LinphoneActivity.instance().getDraftChatList();
        for (int i = 0; i < mConversations.size(); i++) {
            String contact;
            if (i >= mDrafts.size()) {
                contact = mConversations.get(i - mDrafts.size());
            } else {
                contact = mDrafts.get(i);
            }
            removeChatList.add(contact);
        }


        mConversations.removeAll(mDrafts);
        hideAndDisplayMessageIfNoChat();
    }


    private void deleteAllChats() {
        for (int i = 0; i < removeChatList.size(); i++) {
            LinphoneActivity.instance().removeFromChatList(removeChatList.get(i));
        }
        //String sipUri = (String) info.targetView.getTag();
        //LinphoneActivity.instance().removeFromChatList(sipUri);
        mConversations = LinphoneActivity.instance().getChatList();
        mDrafts = LinphoneActivity.instance().getDraftChatList();
        mConversations.removeAll(mDrafts);
        hideAndDisplayMessageIfNoChat();
    }

    private boolean isVersionUsingNewChatStorage() {
        try {
            Context context = LinphoneActivity.instance();
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode >= 2200;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        //Check if the is the first time we show the chat view since we use liblinphone chat storage
        useLinphoneStorage = getResources().getBoolean(R.bool.use_linphone_chat_storage);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LinphoneActivity.instance());
        boolean updateNeeded = prefs.getBoolean(getString(R.string.pref_first_time_linphone_chat_storage), true);
        updateNeeded = updateNeeded && !isVersionUsingNewChatStorage();
        if (useLinphoneStorage && updateNeeded) {
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                private ProgressDialog pd;

                @Override
                protected void onPreExecute() {
                    pd = new ProgressDialog(LinphoneActivity.instance());
                    pd.setTitle(getString(R.string.wait));
                    pd.setMessage(getString(R.string.importing_messages));
                    pd.setCancelable(false);
                    pd.setIndeterminate(true);
                    pd.show();
                }

                @Override
                protected Void doInBackground(Void... arg0) {
                    try {
                        if (importAndroidStoredMessagedIntoLibLinphoneStorage()) {
                            prefs.edit().putBoolean(getString(R.string.pref_first_time_linphone_chat_storage), false).commit();
                            LinphoneActivity.instance().getChatStorage().restartChatStorage();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    pd.dismiss();
                }
            };
            task.execute((Void[]) null);
        }

        if (LinphoneActivity.isInstanciated()) {
            LinphoneActivity.instance().selectMenu(FragmentsAvailable.CHATLIST);
            LinphoneActivity.instance().updateChatListFragment(this);

            if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
                LinphoneActivity.instance().hideStatusBar();
            }
        }

        refresh();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, getString(R.string.delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null || info.targetView == null) {
            return false;
        }

        if (item.getItemId() == 1) {
            String sipUri = (String) info.targetView.getTag();
            LinphoneActivity.instance().removeFromChatList(sipUri);
            mConversations = LinphoneActivity.instance().getChatList();
            mDrafts = LinphoneActivity.instance().getDraftChatList();
            mConversations.removeAll(mDrafts);
            hideAndDisplayMessageIfNoChat();
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

//		if (id == R.id.clearFastChatField) {
//			fastNewChat.setText("");
//		}
        if (id == R.id.delete_all) {
            edit.setVisibility(View.VISIBLE);
            newDiscussion.setVisibility(View.VISIBLE);
            back.setVisibility(View.GONE);
            delete_all.setVisibility(View.GONE);
            isEditMode = false;
            deleteAllChats();
        } else if (id == R.id.edit) {
            edit.setVisibility(View.GONE);
            newDiscussion.setVisibility(View.GONE);
            back.setVisibility(View.VISIBLE);
            delete_all.setVisibility(View.VISIBLE);
            isEditMode = true;
            hideAndDisplayMessageIfNoChat();
        }else if (id == R.id.back) {
            edit.setVisibility(View.VISIBLE);
            newDiscussion.setVisibility(View.VISIBLE);
            back.setVisibility(View.GONE);
            delete_all.setVisibility(View.GONE);
            isEditMode = false;
            hideAndDisplayMessageIfNoChat();
        }
		else if (id == R.id.newDiscussion) {
			String sipUri = "";//fastNewChat.getText().toString();
			if (sipUri.equals("")) {
				LinphoneActivity.instance().displayContacts(true);
			} else {
				if (!LinphoneUtils.isSipAddress(sipUri)) {
					if (LinphoneManager.getLc().getDefaultProxyConfig() == null) {
						return;
					}
					sipUri = sipUri + "@" + LinphoneManager.getLc().getDefaultProxyConfig().getDomain();
				}
				if (!LinphoneUtils.isStrictSipAddress(sipUri)) {
					sipUri = "sip:" + sipUri;
				}
				LinphoneActivity.instance().displayChat(sipUri);
			}
		}
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        String sipUri = (String) view.getTag();

        if (LinphoneActivity.isInstanciated() && !isEditMode) {
            LinphoneActivity.instance().displayChat(sipUri);
        } else if (LinphoneActivity.isInstanciated()) {
            LinphoneActivity.instance().removeFromChatList(sipUri);
            LinphoneActivity.instance().removeFromDrafts(sipUri);

            mConversations = LinphoneActivity.instance().getChatList();
            mDrafts = LinphoneActivity.instance().getDraftChatList();
            mConversations.removeAll(mDrafts);
            hideAndDisplayMessageIfNoChat();

            LinphoneActivity.instance().updateMissedChatCount();
        }
    }

    private boolean importAndroidStoredMessagedIntoLibLinphoneStorage() {
        Log.w("Importing previous messages into new database...");
        try {
            ChatStorage db = LinphoneActivity.instance().getChatStorage();
            List<String> conversations = db.getChatList();
            for (int j = conversations.size() - 1; j >= 0; j--) {
                String correspondent = conversations.get(j);
                LinphoneChatRoom room = LinphoneManager.getLc().getOrCreateChatRoom(correspondent);
                for (ChatMessage message : db.getMessages(correspondent)) {
                    LinphoneChatMessage msg = room.createLinphoneChatMessage(message.getMessage(), message.getUrl(), message.getStatus(), Long.parseLong(message.getTimestamp()), true, message.isIncoming());
                    if (message.getImage() != null) {
                        String path = saveImageAsFile(message.getId(), message.getImage());
                        if (path != null)
                            msg.setExternalBodyUrl(path);
                    }
                    msg.store();
                }
                db.removeDiscussion(correspondent);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String saveImageAsFile(int id, Bitmap bm) {
        try {
            String path = Environment.getExternalStorageDirectory().toString();
            if (!path.endsWith("/"))
                path += "/";
            path += "Pictures/";
            File directory = new File(path);
            directory.mkdirs();

            String filename = getString(R.string.picture_name_format).replace("%s", String.valueOf(id));
            File file = new File(path, filename);

            OutputStream fOut = null;
            fOut = new FileOutputStream(file);

            bm.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            return path + filename;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String timestampToHumanDate(Context context, long timestamp) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);

            SimpleDateFormat dateFormat;
            if (isToday(cal)) {
                dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.today_date_format));
            } else if (isYesterday(cal)) {
                return getString(R.string.yesterday);
            } else {
                dateFormat = new SimpleDateFormat(context.getResources().getString(R.string.history_date_format));
            }

            return dateFormat.format(cal.getTime());
        } catch (NumberFormatException nfe) {
            return String.valueOf(timestamp);
        }
    }

    private boolean isYesterday(Calendar cal) {
        Calendar yesterday = Calendar.getInstance();
        yesterday.roll(Calendar.DAY_OF_MONTH, -1);
        return isSameDay(cal, yesterday);
    }

    private boolean isToday(Calendar cal) {
        return isSameDay(cal, Calendar.getInstance());
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            return false;
        }

        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    class ChatListAdapter extends BaseAdapter {
        private boolean useNativeAPI;
        private Bitmap bitmapUnknown;
        TextDrawable drawable;
        ColorGenerator generator;

        ChatListAdapter(boolean useNativeAPI) {
            this.useNativeAPI = useNativeAPI;
            generator = ColorGenerator.MATERIAL;
            bitmapUnknown = BitmapFactory.decodeResource(LinphoneActivity.instance().getResources(), R.drawable.unknown_small);
        }

        public int getCount() {
            return mConversations.size() + mDrafts.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;

            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.chatlist_cell, parent, false);

            }
            String contact;
            boolean isDraft = false;
            if (position >= mDrafts.size()) {
                contact = mConversations.get(position - mDrafts.size());
            } else {
                contact = mDrafts.get(position);
                isDraft = true;
            }
            view.setTag(contact);
            int unreadMessagesCount = LinphoneActivity.instance().getChatStorage().getUnreadMessageCount(contact);

            LinphoneAddress address;
            try {
                address = LinphoneCoreFactory.instance().createLinphoneAddress(contact);
            } catch (LinphoneCoreException e) {
                Log.e("Chat view cannot parse address", e);
                return view;
            }
            Contact lContact = ContactsManager.getInstance().findContactWithAddress(getActivity().getContentResolver(), address);

            String message = "";
            long lastMessageTime = 0;
            if (useNativeAPI) {
                LinphoneChatRoom chatRoom = LinphoneManager.getLc().getOrCreateChatRoom(contact);
                LinphoneChatMessage[] history = chatRoom.getHistory(20);
                if (history != null && history.length > 0) {
                    for (int i = history.length - 1; i >= 0; i--) {
                        LinphoneChatMessage msg = history[i];
                        if (msg.getText() != null && msg.getText().length() > 0 && msg.getFileTransferInformation() == null) {
                            message = msg.getText();
                            lastMessageTime = msg.getTime();
                            break;
                        }
                    }
                }
            } else {
                List<ChatMessage> messages = LinphoneActivity.instance().getChatMessages(contact);
                if (messages != null && messages.size() > 0) {
                    int iterator = messages.size() - 1;
                    ChatMessage lastMessage = null;

                    while (iterator >= 0) {
                        lastMessage = messages.get(iterator);
                        if (lastMessage.getMessage() == null) {
                            iterator--;
                        } else {
                            iterator = -1;
                        }
                    }
                    message = (lastMessage == null || lastMessage.getMessage() == null) ? "" : lastMessage.getMessage();
                }
            }
            TextView lastMessageView = (TextView) view.findViewById(R.id.lastMessage);
            lastMessageView.setText(message);

            TextView MessageTime = (TextView) view.findViewById(R.id.lastMessageTime);
            android.util.Log.e("CHAT MESSAGE TIME: ", "TIME: " + timestampToHumanDate(getActivity(), lastMessageTime));
            MessageTime.setText(timestampToHumanDate(getActivity(), lastMessageTime));

            TextView sipUri = (TextView) view.findViewById(R.id.sipUri);
            sipUri.setSelected(true); // For animation

            if (getResources().getBoolean(R.bool.only_display_username_if_unknown)) {
                sipUri.setText(lContact == null ? address.getUserName() : lContact.getName());
            } else {
                sipUri.setText(lContact == null ? address.asStringUriOnly() : lContact.getName());
            }

            if (isDraft) {
                view.findViewById(R.id.draft).setVisibility(View.VISIBLE);
            }

            ImageView icon = (ImageView) view.findViewById(R.id.UserProfile);
            ImageView character_icon = (ImageView) view.findViewById(R.id.character_icon);
            if (lContact == null) {
                icon.setImageBitmap(bitmapUnknown);
            } else {
                if (lContact.getPhoto() != null) {
                    icon.setImageBitmap(lContact.getPhoto());
                    icon.setVisibility(View.VISIBLE);
                    character_icon.setVisibility(View.INVISIBLE);
                } else if (lContact.getPhotoUri() != null) {
                    LinphoneUtils.setImagePictureFromUri(getActivity(), icon, lContact.getPhotoUri(), lContact.getThumbnailUri(), R.drawable.unknown_small);
                    icon.setVisibility(View.VISIBLE);
                    character_icon.setVisibility(View.INVISIBLE);
                } else {
                    icon.setVisibility(View.INVISIBLE);
                    character_icon.setVisibility(View.VISIBLE);
                    int color = generator.getRandomColor();
                    if (!mappingList.containsKey(sipUri.getText().toString())) {
                        mappingList.put(sipUri.getText().toString(), color);
                    }
                    android.util.Log.e("CONTACT: ", mappingList.toString());
                    if (mappingList.containsKey(sipUri.getText().toString())) {
                        drawable = TextDrawable.builder()
                                .buildRoundRect(sipUri.getText().toString().substring(0, 1), mappingList.get(sipUri.getText().toString()), 96);
                    } else {
                        drawable = TextDrawable.builder()
                                .buildRoundRect(sipUri.getText().toString().substring(0, 1), color, 96);
                    }

                    character_icon.setImageDrawable(drawable);

                }
            }

            ImageView delete = (ImageView) view.findViewById(R.id.delete);
            TextView unreadMessages = (TextView) view.findViewById(R.id.unreadMessages);

            if (unreadMessagesCount > 0) {
                unreadMessages.setVisibility(View.VISIBLE);
                unreadMessages.setText(String.valueOf(unreadMessagesCount));
            } else {
                unreadMessages.setVisibility(View.GONE);
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


