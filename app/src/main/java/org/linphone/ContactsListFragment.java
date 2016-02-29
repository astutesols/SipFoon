package org.linphone;
/*
ContactsListFragment.java
Copyright (C) 2015  Belledonne Communications, Grenoble, France

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
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AlphabetIndexer;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import org.linphone.api.ColorGenerator;
import org.linphone.api.TextDrawable;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.PresenceActivityType;
import org.linphone.mediastream.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sylvain Berfini
 */
@SuppressLint("DefaultLocale")
public class ContactsListFragment extends Fragment implements OnClickListener, OnItemClickListener {
	private LayoutInflater mInflater;
	private ListView contactsList;
	private TextView noSipContact, noContact;
	private ImageView newContact;
	private boolean onlyDisplayLinphoneContacts;
	private int lastKnownPosition;
	private AlphabetIndexer indexer;
	private boolean editOnClick = false, editConsumed = false, onlyDisplayChatAddress = false;
	private String sipAddressToAdd;
	private EditText searchField;
	private Cursor searchCursor;
	HashMap<String, Integer> mappingList;
	private static ContactsListFragment instance;
	
	static final boolean isInstanciated() {
		return instance != null;
	}

	public static final ContactsListFragment instance() {
		return instance;
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
		mInflater = inflater;
        View view = inflater.inflate(R.layout.contacts_list, container, false);
		mappingList = new HashMap<>();
        if (getArguments() != null) {
	        editOnClick = getArguments().getBoolean("EditOnClick");
	        sipAddressToAdd = getArguments().getString("SipAddress");
	        
	        onlyDisplayChatAddress = getArguments().getBoolean("ChatAddressOnly");
        }
        
        noSipContact = (TextView) view.findViewById(R.id.noSipContact);
        noContact = (TextView) view.findViewById(R.id.noContact);
        
        contactsList = (ListView) view.findViewById(R.id.contactsList);
        contactsList.setOnItemClickListener(this);

        
        newContact = (ImageView) view.findViewById(R.id.newContact);
        newContact.setOnClickListener(this);
        newContact.setEnabled(LinphoneManager.getLc().getCallsNb() == 0);
		
		searchField = (EditText) view.findViewById(R.id.searchField);
		searchField.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				searchContacts(searchField.getText().toString());
			}
		});

		return view;
    }

	public int getNbItemsChecked(){
		int size = contactsList.getAdapter().getCount();
		int nb = 0;
		for(int i=0; i<size; i++) {
			if(contactsList.isItemChecked(i)) {
				nb ++;
			}
		}
		return nb;
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.newContact) {
//            editConsumed = true;
            LinphoneActivity.instance().addContact(null, sipAddressToAdd);
        }
    }



	private void selectAllList(boolean isSelectAll){
		int size = contactsList.getAdapter().getCount();
		for(int i=0; i<size; i++) {
			contactsList.setItemChecked(i,isSelectAll);
		}
	}

	private void deleteExistingContact(Contact contact) {
		String select = ContactsContract.Data.CONTACT_ID + " = ?";
		String[] args = new String[] { contact.getID() };

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
		ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
						.withSelection(select, args)
						.build()
		);

		try {
			getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
			ContactsManager.getInstance().removeAllFriends(contact);
		} catch (Exception e) {
			Log.w(e.getMessage() + ":" + e.getStackTrace());
		}
	}

	private void removeContacts(){
		int size = contactsList.getAdapter().getCount();
		for(int i=0; i<size; i++) {
			if(contactsList.isItemChecked(i)){
				Contact contact = (Contact) contactsList.getAdapter().getItem(i);
				deleteExistingContact(contact);
				ContactsManager.getInstance().removeContactFromLists(getActivity().getContentResolver(), contact);
			}
		}
	}

//	public void quitEditMode(){
//		isEditMode = false;
//		editList.setVisibility(View.GONE);
//		topbar.setVisibility(View.VISIBLE);
//		invalidate();
//		if(getResources().getBoolean(R.bool.isTablet)){
//			displayFirstContact();
//		}
//	}


	private void searchContacts() {
		searchContacts(searchField.getText().toString());
	}

	private void searchContacts(String search) {
		if (search == null || search.length() == 0) {
			changeContactsAdapter();
			return;
		}
		
		if (searchCursor != null) {
			searchCursor.close();
		}
			if (onlyDisplayLinphoneContacts) {
				searchCursor = Compatibility.getSIPContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
				indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
				contactsList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
				contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
			} else {
				searchCursor = Compatibility.getContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
				contactsList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
				indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
				contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
			}

	}
	
	private void changeContactsAdapter() {
		
		if (searchCursor != null) {
			searchCursor.close();
		}
		
		Cursor allContactsCursor = ContactsManager.getInstance().getAllContactsCursor();
		Cursor sipContactsCursor = ContactsManager.getInstance().getSIPContactsCursor();

		noSipContact.setVisibility(View.GONE);
		noContact.setVisibility(View.GONE);
		contactsList.setVisibility(View.VISIBLE);
			if (onlyDisplayLinphoneContacts) {
				if (sipContactsCursor != null && sipContactsCursor.getCount() == 0) {
					noSipContact.setVisibility(View.VISIBLE);
					contactsList.setVisibility(View.GONE);
				} else if (sipContactsCursor != null) {
					indexer = new AlphabetIndexer(sipContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(sipContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
					contactsList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
					contactsList.setAdapter(new ContactsListAdapter(ContactsManager.getInstance().getSIPContacts(), sipContactsCursor));

				}
			} else {
				if (allContactsCursor != null && allContactsCursor.getCount() == 0) {
					noContact.setVisibility(View.VISIBLE);
					contactsList.setVisibility(View.GONE);
				} else if (allContactsCursor != null) {
					indexer = new AlphabetIndexer(allContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(allContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
					//contactsList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
					List<Contact> allContacts = ContactsManager.getInstance().getAllContacts();
//					Collections.sort(allContacts, new Comparator<Contact>() {
//
//                     /* This comparator will sort AppDetail objects alphabetically. */
//
//						@Override
//						public int compare(Contact a1, Contact a2) {
//							// String implements Comparable
//							return (a1.getName()).compareTo(a2.getName());
//						}
//					});
					contactsList.setAdapter( new ContactsListAdapter(allContacts, allContactsCursor));
				}
			}
        Log.e("list size:"+allContactsCursor.getCount());
		//ContactsManager.getInstance().setLinphoneContactsPrefered(onlyDisplayLinphoneContacts);
	}




	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		Contact contact = (Contact) adapter.getItemAtPosition(position);
		if (editOnClick) {
			editConsumed = true;
			LinphoneActivity.instance().editContact(contact, sipAddressToAdd);
		} else {
			lastKnownPosition = contactsList.getFirstVisiblePosition();
			LinphoneActivity.instance().displayContact(contact, onlyDisplayChatAddress);
		}
	}
	
	@Override
	public void onResume() {
		instance = this;
		super.onResume();

		if (editConsumed) {
			editOnClick = false;
			sipAddressToAdd = null;
		}

		if (LinphoneActivity.isInstanciated()) {
			LinphoneActivity.instance().selectMenu(FragmentsAvailable.CONTACTS);
			LinphoneActivity.instance().hideStatusBar();
			//onlyDisplayLinphoneContacts = ContactsManager.getInstance().isLinphoneContactsPrefered();
			onlyDisplayLinphoneContacts = false;
		}

		invalidate();
	}
	
	@Override
	public void onPause() {
		instance = null;
		if (searchCursor != null) {
			searchCursor.close();
		}
		super.onPause();
	}
	
	public void invalidate() {
		if (searchField != null && searchField.getText().toString().length() > 0) {
			searchContacts(searchField.getText().toString());
		} else {
			changeContactsAdapter();
		}
		contactsList.setSelectionFromTop(lastKnownPosition, 0);
	}
	
	class ContactsListAdapter extends BaseAdapter implements SectionIndexer {
		private List<Contact> contacts;
		private Cursor cursor;
		ColorGenerator generator;
        TextDrawable drawable;
		
		ContactsListAdapter(List<Contact> contactsList, Cursor c) {
			contacts = contactsList;
			cursor = c;
			generator = ColorGenerator.MATERIAL;
		}
		
		public int getCount() {
				return cursor.getCount();
		}

		public Object getItem(int position) {
			if (contacts == null || position >= contacts.size()) {
				return Compatibility.getContact(getActivity().getContentResolver(), cursor, position);
			} else {
				return contacts.get(position);
			}
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = null;
			Contact contact = null;
			do {
				contact = (Contact) getItem(position);
			} while (contact == null);
			
			if (convertView != null) {
				view = convertView;
			} else {
				view = mInflater.inflate(R.layout.contact_cell, parent, false);
			}

			TextView name = (TextView) view.findViewById(R.id.name);
			name.setText(contact.getName());

            ImageView icon = (ImageView) view.findViewById(R.id.icon);
            ImageView character_icon = (ImageView) view.findViewById(R.id.character_icon);
            Log.e("Contat listing screen: ", String.valueOf(contact.getPhotoUri() == null));
//            if (contact.getPhoto() != null) {
//                icon.setVisibility(View.VISIBLE);
//                character_icon.setVisibility(View.INVISIBLE);
//                Log.e("Contat listing screen: ", "getPhoto");
//                icon.setImageBitmap(contact.getPhoto());
//
//            } else
            if (contact.getPhotoUri() != null) {
                icon.setVisibility(View.VISIBLE);
                character_icon.setVisibility(View.INVISIBLE);
                Log.e("Contat listing screen: ", "setImagePictureFromUri");
                //LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture.getView(), contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
                LinphoneUtils.setImagePictureFromUri(getActivity(), icon, contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
            } else {
                Log.e("Contat listing screen: ", "character_icon");
                icon.setVisibility(View.INVISIBLE);
                character_icon.setVisibility(View.VISIBLE);
                int color = generator.getRandomColor();
                if(!mappingList.containsKey(name.getText().toString())){
                    mappingList.put(name.getText().toString(), color);
                }
                if(mappingList.containsKey(name.getText().toString())){
                    drawable = TextDrawable.builder()
                            .buildRoundRect(name.getText().toString().substring(0, 1), mappingList.get(name.getText().toString()), 96);
                }else {
                    drawable = TextDrawable.builder()
                            .buildRoundRect(name.getText().toString().substring(0,1), color,96);
                }

                character_icon.setImageDrawable(drawable);

            }
			ImageView friendStatus = (ImageView) view.findViewById(R.id.friendStatus);
			LinphoneFriend[] friends = LinphoneManager.getLc().getFriendList();
			if (!ContactsManager.getInstance().isContactPresenceDisabled() && friends != null) {
				friendStatus.setVisibility(View.VISIBLE);
				PresenceActivityType presenceActivity = friends[0].getPresenceModel().getActivity().getType();
				if (presenceActivity == PresenceActivityType.Online) {
					friendStatus.setImageResource(R.drawable.led_connected);
				} else if (presenceActivity == PresenceActivityType.Busy) {
					friendStatus.setImageResource(R.drawable.led_error);
				} else if (presenceActivity == PresenceActivityType.Away) {
					friendStatus.setImageResource(R.drawable.led_inprogress);
				} else if (presenceActivity == PresenceActivityType.Offline) {
					friendStatus.setImageResource(R.drawable.led_disconnected);
				} else {
					friendStatus.setImageResource(R.drawable.led_connected);
				}
			}
			
			return view;
		}

		@Override
		public int getPositionForSection(int section) {
			return indexer.getPositionForSection(section);
		}

		@Override
		public int getSectionForPosition(int position) {
			return indexer.getSectionForPosition(position);
		}

		@Override
		public Object[] getSections() {
			return indexer.getSections();
		}
	}
}
