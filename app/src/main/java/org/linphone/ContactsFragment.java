//package org.linphone;
///*
//ContactsFragment.java
//Copyright (C) 2012  Belledonne Communications, Grenoble, France
//
//This program is free software; you can redistribute it and/or
//modify it under the terms of the GNU General Public License
//as published by the Free Software Foundation; either version 2
//of the License, or (at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program; if not, write to the Free Software
//Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//*/
//
//import android.annotation.SuppressLint;
//import android.app.ProgressDialog;
//import android.database.Cursor;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.Bundle;
//import android.os.Handler;
//import android.support.v4.app.Fragment;
//import android.text.Editable;
//import android.text.TextUtils;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.View.OnClickListener;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.AdapterView.OnItemClickListener;
//import android.widget.AlphabetIndexer;
//import android.widget.BaseAdapter;
//import android.widget.EditText;
//import android.widget.Filter;
//import android.widget.Filterable;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ListView;
//import android.widget.SectionIndexer;
//import android.widget.TextView;
//
//import org.linphone.api.ColorGenerator;
//import org.linphone.api.Config;
//import org.linphone.api.TextDrawable;
//import org.linphone.compatibility.Compatibility;
//import org.linphone.core.LinphoneCore;
//import org.linphone.core.LinphoneFriend;
//import org.linphone.core.PresenceActivityType;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//
///**
// * @author Sylvain Berfini
// */
//@SuppressLint("DefaultLocale")
//public class ContactsFragment extends BaseFragment implements OnClickListener, OnItemClickListener {
//    private LayoutInflater mInflater;
//    private ListView contactsList;
//    private TextView noSipContact, noContact;//allContacts, linphoneContacts,
//    private ImageView newContact;
//    private boolean onlyDisplayLinphoneContacts;
//    private int lastKnownPosition;
//    private AlphabetIndexer indexer;
//    private boolean editOnClick = false, editConsumed = false, onlyDisplayChatAddress = false;
//    private String sipAddressToAdd;
//    //private ImageView clearSearchField;
//    private EditText searchField;
//    private Cursor searchCursor;
//    HashMap<String, Integer> mappingList;
//    private static ContactsFragment instance;
//    ContactsListAdapter adapter;
//
//    static final boolean isInstanciated() {
//        return instance != null;
//    }
//
//    public static final ContactsFragment instance() {
//        return instance;
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        mInflater = inflater;
//        View view = inflater.inflate(R.layout.contacts_list, container, false);
//        mappingList = new HashMap<>();
//        if (getArguments() != null) {
//            editOnClick = getArguments().getBoolean("EditOnClick");
//            sipAddressToAdd = getArguments().getString("SipAddress");
//            onlyDisplayChatAddress = getArguments().getBoolean("ChatAddressOnly");
//        }
//
////        ImageView tenantLogo = (ImageView) view.findViewById(R.id.tenantLogo);
////        Config.loadImageFromStorage(getActivity(), tenantLogo, "logo.png");
//
//        noSipContact = (TextView) view.findViewById(R.id.noSipContact);
//        noContact = (TextView) view.findViewById(R.id.noContact);
//
//        contactsList = (ListView) view.findViewById(R.id.contactsList);
//        contactsList.setOnItemClickListener(this);
//
////        allContacts = (TextView) view.findViewById(R.id.allContacts);
////        allContacts.setOnClickListener(this);
////
////        linphoneContacts = (TextView) view.findViewById(R.id.linphoneContacts);
////        linphoneContacts.setOnClickListener(this);
//
//        newContact = (ImageView) view.findViewById(R.id.newContact);
//        newContact.setOnClickListener(this);
//        newContact.setEnabled(LinphoneManager.getLc().getCallsNb() == 0);
//
////        allContacts.setEnabled(onlyDisplayLinphoneContacts);
////        linphoneContacts.setEnabled(!allContacts.isEnabled());
//
////		clearSearchField = (ImageView) view.findViewById(R.id.clearSearchField);
////		clearSearchField.setOnClickListener(this);
//
//        searchField = (EditText) view.findViewById(R.id.searchField);
//        searchField.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//            }
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//            @Override
//            public void afterTextChanged(Editable s) {
//                //searchContacts(searchField.getText().toString());
//
//                if(adapter != null){
//                    if(searchField.getText().toString().trim().length() > 0){
//                        adapter.getFilter().filter(searchField.getText().toString());
//                    } else {
////                        adapter.notifyDataSetChanged();
//                       changeContactsAdapter();
//                    }
//                }
//
//            }
//        });
//
//        return view;
//    }
//
//    @Override
//    public void onClick(View v) {
//        int id = v.getId();
//
//		/*if (id == R.id.allContacts) {
//			onlyDisplayLinphoneContacts = false;
//			if (searchField.getText().toString().length() > 0) {
//				searchContacts();
//			} else {
//				changeContactsAdapter();
//			}
//		}
//		else if (id == R.id.linphoneContacts) {
//			onlyDisplayLinphoneContacts = true;
//			if (searchField.getText().toString().length() > 0) {
//				searchContacts();
//			} else {
//				changeContactsAdapter();
//			}
//		}
//		else */
//        if (id == R.id.newContact) {
////            editConsumed = true;
//            LinphoneActivity.instance().addContact(null, sipAddressToAdd);
//        }
////		else if (id == R.id.clearSearchField) {
////			searchField.setText("");
////		}
//    }
//
////    private void searchContacts() {
////        searchContacts(searchField.getText().toString());
////    }
////
////    private void searchContacts(String search) {
////        if (search == null || search.length() == 0) {
////            changeContactsAdapter();
////            return;
////        }
////        changeContactsToggle();
////
////        if (searchCursor != null) {
////            searchCursor.close();
////        }
////
////        if (onlyDisplayLinphoneContacts) {
////            searchCursor = Compatibility.getSIPContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
////            indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
////            contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
////        } else {
////            searchCursor = Compatibility.getContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
////            indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
////            contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
////
////        }
////    }
//
//    private void changeContactsAdapter() {
////        changeContactsToggle();
////
////        if (searchCursor != null) {
////            searchCursor.close();
////        }
//        Cursor allContactsCursor = ContactsManager.getInstance().getAllContactsCursor();
//        Cursor sipContactsCursor = ContactsManager.getInstance().getSIPContactsCursor();
//
//        noSipContact.setVisibility(View.GONE);
//        noContact.setVisibility(View.GONE);
//
//        if (onlyDisplayLinphoneContacts) {
//            if (sipContactsCursor.getCount() == 0) {
//                noSipContact.setVisibility(View.VISIBLE);
//                contactsList.setVisibility(View.GONE);
//            } else {
//                indexer = new AlphabetIndexer(sipContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(sipContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//                adapter = new ContactsListAdapter(ContactsManager.getInstance().getSIPContacts(), sipContactsCursor);
//                contactsList.setAdapter(adapter);
//            }
//        } else {
//            if (allContactsCursor.getCount() == 0) {
//                noContact.setVisibility(View.VISIBLE);
//                contactsList.setVisibility(View.GONE);
//            } else {
//                indexer = new AlphabetIndexer(allContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(allContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//                adapter = new ContactsListAdapter(ContactsManager.getInstance().getAllContacts(), allContactsCursor);
//                contactsList.setAdapter(adapter);
//                contactsList.setVisibility(View.VISIBLE);
//                //contactsList.requestLayout();
//                Log.e("Contat listing screen: ", "Showing adapter count: "+ContactsManager.getInstance().getAllContacts().size());
//            }
//        }
//        ContactsManager.getInstance().setLinphoneContactsPrefered(onlyDisplayLinphoneContacts);
//    }
//
//    private void changeContactsToggle() {
////		if (onlyDisplayLinphoneContacts) {
////			allContacts.setEnabled(true);
////			linphoneContacts.setEnabled(false);
////		} else {
////			allContacts.setEnabled(false);
////			linphoneContacts.setEnabled(true);
////		}
//    }
//
//    @Override
//    public void onItemClick(AdapterView<?> adapters, View view, int position, long id) {
//        Contact contact = (Contact) adapters.getItemAtPosition(position);
//        if (editOnClick) {
//            editConsumed = true;
//            LinphoneActivity.instance().editContact(contact, sipAddressToAdd);
//        } else {
//            //editConsumed = true;
//            lastKnownPosition = contactsList.getFirstVisiblePosition();
//            LinphoneActivity.instance().displayContact(contact, onlyDisplayChatAddress);
//        }
//    }
//
//    @Override
//    public void onResume() {
//        instance = this;
//        super.onResume();
//
//        if (editConsumed) {
//            editOnClick = false;
//            sipAddressToAdd = null;
//        }
//
//        if (LinphoneActivity.isInstanciated()) {
//            LinphoneActivity.instance().selectMenu(FragmentsAvailable.CONTACTS);
//            onlyDisplayLinphoneContacts = ContactsManager.getInstance().isLinphoneContactsPrefered();
//            //onlyDisplayLinphoneContacts = true;
//            if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
//                LinphoneActivity.instance().hideStatusBar();
//            }
//        }
//invalidate();
////        final ProgressDialog pd = new ProgressDialog(LinphoneActivity.instance());
////        pd.setTitle(getString(R.string.wait));
////        pd.setMessage("Loading Contacts");
////        pd.setCancelable(false);
////        pd.setIndeterminate(true);
////        pd.show();
////
////        contactsList.setVisibility(View.GONE);
////        final Handler handler = new Handler();
////        Runnable runable = new Runnable() {
////            @Override
////            public void run() {
////                try {
////                    if (ContactsManager.getInstance().isContactsPrepared()) {
////                        invalidate();
////                        pd.dismiss();
////                        Log.e("Contat listing screen: ", "Dismissing dialog");
////                        handler.removeCallbacks(this);
////                    }  else {
////                        handler.postDelayed(this, 50);
////                    }
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////            }
////        };
////        handler.postDelayed(runable, 10);
//
//
//    }
//
//    @Override
//    public void onPause() {
//        instance = null;
//        if (searchCursor != null) {
//            searchCursor.close();
//        }
//        //ContactsManager.getInstance().setContactPreparedFalse();
//        super.onPause();
//    }
//
//    public void invalidate() {
//        if (searchField != null && searchField.getText().toString().trim().length() > 0) {
//            Log.e("Contat listing screen: ", "Filter from search: " + String.valueOf(editConsumed));
//
//            changeContactsAdapter();
//            adapter.getFilter().filter(searchField.getText().toString());
//
////            if(editConsumed){
////                changeContactsAdapter();
////                adapter.getFilter().filter(searchField.getText().toString());
////            }else {
////                adapter.getFilter().filter(searchField.getText().toString());
////            }
//        } else {
//            changeContactsAdapter();
//            Log.e("Contat listing screen: ", "Showing all data");
//        }
//        contactsList.setSelectionFromTop(lastKnownPosition, 0);
//    }
//
//    class ContactsListAdapter extends BaseAdapter implements SectionIndexer, Filterable {
//        private Cursor cursor;
//        TextDrawable drawable;
//        ColorGenerator generator;
//        private ItemFilter mFilter = new ItemFilter();
//        private ArrayList<Contact> originalData = null;
//        private ArrayList<Contact>filteredData = null;
//
//
//        ContactsListAdapter(ArrayList<Contact> contactsList, Cursor c) {
//            this.filteredData = contactsList ;
//            this.originalData = contactsList ;
//            cursor = c;
//            generator = ColorGenerator.MATERIAL;
//        }
//
//        public int getCount() {
//            return filteredData.size();
//        }
//
//        public Object getItem(int position) {
//            if (filteredData == null || position >= filteredData.size()) {
//                return Compatibility.getContact(getActivity().getContentResolver(), cursor, position);
//            } else {
//                return filteredData.get(position);
//            }
//        }
//
//        public long getItemId(int position) {
//            return position;
//        }
//
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View view = null;
//            Contact contact = null;
//            do {
//                contact = (Contact) getItem(position);
//            } while (contact == null);
//
//            if (convertView != null) {
//                view = convertView;
//            } else {
//                view = mInflater.inflate(R.layout.contact_cell, parent, false);
//            }
//
//            TextView name = (TextView) view.findViewById(R.id.name);
//            name.setText(contact.getName());
//            TextView contact_number = (TextView) view.findViewById(R.id.contact_number);
//
//            contact_number.setText(contact.getContact_number());
////            if(!contact.getContact_number().isEmpty()){
////                contact_number.setText(contact.getContact_number());
////            }else{
////                contact_number.setText(TextUtils.join(" , ", contact.getNumbersOrAddresses()));
////            }
//
//
//
////            TextView separator = (TextView) view.findViewById(R.id.separator);
////            LinearLayout layout = (LinearLayout) view.findViewById(R.id.layout);
////            if (getPositionForSection(getSectionForPosition(position)) != position) {
////                separator.setVisibility(View.GONE);
////                layout.setPadding(0, margin, 0, margin);
////            } else {
////                separator.setVisibility(View.VISIBLE);
////                separator.setText(String.valueOf(contact.getName().charAt(0)));
////                layout.setPadding(0, 0, 0, margin);
////            }
//
//            ImageView icon = (ImageView) view.findViewById(R.id.icon);
//            ImageView character_icon = (ImageView) view.findViewById(R.id.character_icon);
//            Log.e("Contat listing screen: ", String.valueOf(contact.getPhotoUri() == null));
////            if (contact.getPhoto() != null) {
////                icon.setVisibility(View.VISIBLE);
////                character_icon.setVisibility(View.INVISIBLE);
////                Log.e("Contat listing screen: ", "getPhoto");
////                icon.setImageBitmap(contact.getPhoto());
////
////            } else
//            if (contact.getPhotoUri() != null) {
//                icon.setVisibility(View.VISIBLE);
//                character_icon.setVisibility(View.INVISIBLE);
//                Log.e("Contat listing screen: ", "setImagePictureFromUri");
//                //LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture.getView(), contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
//                LinphoneUtils.setImagePictureFromUri(getActivity(), icon, contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
//            } else {
//                Log.e("Contat listing screen: ", "character_icon");
//                icon.setVisibility(View.INVISIBLE);
//                character_icon.setVisibility(View.VISIBLE);
//                int color = generator.getRandomColor();
//                if(!mappingList.containsKey(name.getText().toString())){
//                    mappingList.put(name.getText().toString(), color);
//                }
//                if(mappingList.containsKey(name.getText().toString())){
//                    drawable = TextDrawable.builder()
//                            .buildRoundRect(name.getText().toString().substring(0, 1), mappingList.get(name.getText().toString()), 96);
//                }else {
//                    drawable = TextDrawable.builder()
//                            .buildRoundRect(name.getText().toString().substring(0,1), color,96);
//                }
//
//                character_icon.setImageDrawable(drawable);
//
//            }
//
//            ImageView friendStatus = (ImageView) view.findViewById(R.id.friendStatus);
//            LinphoneFriend[] friends = LinphoneManager.getLc().getFriendList();
//            if (!ContactsManager.getInstance().isContactPresenceDisabled() && friends != null) {
//                friendStatus.setVisibility(View.VISIBLE);
//                PresenceActivityType presenceActivity = friends[0].getPresenceModel().getActivity().getType();
//                if (presenceActivity == PresenceActivityType.Online) {
//                    friendStatus.setImageResource(R.drawable.led_connected);
//                } else if (presenceActivity == PresenceActivityType.Busy) {
//                    friendStatus.setImageResource(R.drawable.led_error);
//                } else if (presenceActivity == PresenceActivityType.Away) {
//                    friendStatus.setImageResource(R.drawable.led_inprogress);
//                } else if (presenceActivity == PresenceActivityType.Offline) {
//                    friendStatus.setImageResource(R.drawable.led_disconnected);
//                } else {
//                    friendStatus.setImageResource(R.drawable.led_connected);
//                }
//            }
//
//            return view;
//        }
//
//        @Override
//        public int getPositionForSection(int section) {
//            return indexer.getPositionForSection(section);
//        }
//
//        @Override
//        public int getSectionForPosition(int position) {
//            return indexer.getSectionForPosition(position);
//        }
//
//        @Override
//        public Object[] getSections() {
//            return indexer.getSections();
//        }
//
//        public Filter getFilter() {
//            return mFilter;
//        }
//
//
//        private class ItemFilter extends Filter {
//            @Override
//            protected FilterResults performFiltering(CharSequence constraint) {
//
////                String filterString = constraint.toString().toLowerCase();
//
//                FilterResults results = new FilterResults();
//
//                final ArrayList<Contact> list = originalData;
//
//                int count = list.size();
//                final ArrayList<Contact> nlist = new ArrayList<Contact>(count);
//
//
//
//                for (int i = 0; i < count; i++) {
//                    Contact contact = list.get(i);
//                    if (contact.getContact_number().toLowerCase(Locale.getDefault()).contains(constraint.toString())) {
//                        nlist.add(contact);
//                    } else if (contact.getName().toLowerCase(Locale.getDefault()).contains(constraint.toString())) {
//                        nlist.add(contact);
//                    }
//                }
//
//                results.values = nlist;
//                results.count = nlist.size();
//
//                return results;
//            }
//
//            @SuppressWarnings("unchecked")
//            @Override
//            protected void publishResults(CharSequence constraint, FilterResults results) {
//                filteredData = (ArrayList<Contact>) results.values;
//                adapter.notifyDataSetChanged();
//
//                Log.e("Contat listing screen: ", "Showing search data");
//
//                //contactsList.requestLayout();
//            }
//
//        }
//    }
//}
