package org.linphone;
/*
DialerFragment.java
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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.linphone.api.ColorGenerator;
import org.linphone.api.Config;
import org.linphone.api.TextDrawable;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.CallDirection;
import org.linphone.core.LinphoneCallLog;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PresenceActivityType;
import org.linphone.mediastream.Log;
import org.linphone.ui.AddressAware;
import org.linphone.ui.AddressText;
import org.linphone.ui.CallButton;
import org.linphone.ui.EraseButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * @author Sylvain Berfini
 */
public class DialerFragment extends BaseFragment implements AdapterView.OnItemClickListener {

    private LayoutInflater mInflater;
    private static DialerFragment instance;
    private static boolean isCallTransferOngoing = false;
    private static boolean isFromIncallScreen = false;
    private ListView contactsList;
    public boolean mVisible;
    private AddressText mAddress;
    //private ImageView mAddContact;
    private View.OnClickListener addContactListener, cancelListener, transferListener;
    private boolean shouldEmptyAddressField = true;
    private AddressTextInterface mAddressTextInterface;
    //private ToggleButton startVideoBtn;
    LinphonePreferences mLinphonePreferences;
    CallButton mCall;
    private AlphabetIndexer indexer;
    HashMap<String, Integer> mappingList;
    private boolean onlyDisplayLinphoneContacts;
    ImageView voicemail;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        instance = this;
        View view = inflater.inflate(R.layout.dialer, container, false);

        mInflater = inflater;
        mLinphonePreferences = LinphonePreferences.instance();

        mappingList = new HashMap<>();
        mAddress = (AddressText) view.findViewById(R.id.Adress);
        mAddress.setDialerFragment(this);
        voicemail = (ImageView) view.findViewById(R.id.voicemail);
        contactsList = (ListView) view.findViewById(R.id.contactsList);
        contactsList.setOnItemClickListener(this);

//        LinphoneActivity.instance().changeBottomBarColor(getResources().getColor(R.color.colorPrimary));
        EraseButton erase = (EraseButton) view.findViewById(R.id.Erase);
        erase.goBackToInCallScreen((LinphoneActivity) getActivity());
        erase.setAddressWidget(mAddress);

//        startVideoBtn = (ToggleButton) view.findViewById(R.id.start_video);
//        startVideoBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.e("initiate video call click before:" + mLinphonePreferences.shouldInitiateVideoCall());
//                if (mLinphonePreferences.shouldInitiateVideoCall()) {
//                    mLinphonePreferences.setInitiateVideoCall(false);
//                } else {
//                    mLinphonePreferences.setInitiateVideoCall(true);
//                }
//                Log.e("initiate video call click after:" + mLinphonePreferences.shouldInitiateVideoCall());
//            }
//        });

//        ImageView tenantLogo = (ImageView) view.findViewById(R.id.tenantLogo);
//        Config.loadImageFromStorage(getActivity(), tenantLogo, "logo.png");

        mCall = (CallButton) view.findViewById(R.id.Call);
        mCall.setAddressWidget(mAddress);
        if (LinphoneActivity.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0) {
            if (isCallTransferOngoing) {
                mCall.setImageResource(R.drawable.options_transfer_default);
            } else {
                mCall.setImageResource(R.drawable.options_add_default_alt);
            }
        } else {
            mCall.setImageResource(R.drawable.call);
        }

        AddressAware numpad = (AddressAware) view.findViewById(R.id.Dialer);
        if (numpad != null) {
            numpad.setAddressWidget(mAddress);
        }

        //mAddContact = (ImageView) view.findViewById(R.id.addContact);

        addContactListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinphoneActivity.instance().displayContactsForEdition(mAddress.getText().toString());
            }
        };
        cancelListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
            }
        };
        transferListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.util.Log.e("DIALER", "transfer call");
                LinphoneCore lc = LinphoneManager.getLc();
                if (lc.getCurrentCall() == null) {
                    return;
                }
                lc.transferCall(lc.getCurrentCall(), mAddress.getText().toString());
                isCallTransferOngoing = false;
                LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
            }
        };


        voicemail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String voiceMail = LinphonePreferences.instance().getVoiceMailUri();
                    if(voiceMail == null){
                        Toast.makeText(getActivity(), "Voice mail is not enabled", Toast.LENGTH_LONG).show();
                    }else {
                        if (mLinphonePreferences.shouldInitiateVideoCall()) {
                            mLinphonePreferences.setInitiateVideoCall(false);
                        }
                        mAddress.getEditableText().append(voiceMail);
                        LinphoneManager.getInstance().newOutgoingCall(mAddress);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        android.util.Log.e("DIALER", "isCallTransferOngoing: " + isCallTransferOngoing);
        //mAddContact.setEnabled(!(LinphoneActivity.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0));
        resetLayout(isCallTransferOngoing);

        if (getArguments() != null) {
            shouldEmptyAddressField = false;
            String number = getArguments().getString("SipUri");
            String displayName = getArguments().getString("DisplayName");
            String photo = getArguments().getString("PhotoUri");
            mAddress.setText(number);
            if (displayName != null) {
                mAddress.setDisplayedName(displayName);
            }
            if (photo != null) {
                mAddress.setPictureUri(Uri.parse(photo));
            }
        }

//        mAddress.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                //searchContacts(mAddress.getText().toString());
//
//                if (adapter != null) {
//                    if (mAddress.getText().toString().length() > 0) {
//                        adapter.getFilter().filter(mAddress.getText().toString());
//                    } else {
//                        changeContactsAdapter();
//                    }
//                }
//
//            }
//        });
        return view;
    }


//    private void searchContacts(String search) {
//        if (search == null || search.length() == 0) {
//            android.util.Log.e("CONTACTs" ,"search empty");
//            changeContactsAdapter();
//            return;
//        }
//
//        if (searchCursor != null) {
//            searchCursor.close();
//        }
//
//        android.util.Log.e("CONTACTs", "search: " + search);
//        if (onlyDisplayLinphoneContacts) {
//            searchCursor = Compatibility.getSIPContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
//            indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//            contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
//        } else {
//            searchCursor = Compatibility.getContactsCursor(getActivity().getContentResolver(), search, ContactsManager.getInstance().getContactsId());
//            indexer = new AlphabetIndexer(searchCursor, Compatibility.getCursorDisplayNameColumnIndex(searchCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//            contactsList.setAdapter(new ContactsListAdapter(null, searchCursor));
//
//        }
//    }

//    private void changeContactsAdapter() {
//
//
//        Cursor allContactsCursor = ContactsManager.getInstance().getAllContactsCursor();
//        Cursor sipContactsCursor = ContactsManager.getInstance().getSIPContactsCursor();
//
//        contactsList.setVisibility(View.GONE);
//
//        if (onlyDisplayLinphoneContacts) {
//            if (sipContactsCursor.getCount() == 0) {
//                contactsList.setVisibility(View.GONE);
//            } else {
//                indexer = new AlphabetIndexer(sipContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(sipContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//                adapter = new ContactsListAdapter(ContactsManager.getInstance().getSIPContacts(), sipContactsCursor);
//                contactsList.setAdapter(adapter);
//            }
//        } else {
//            if (allContactsCursor.getCount() == 0) {
//                contactsList.setVisibility(View.GONE);
//            } else {
//                indexer = new AlphabetIndexer(allContactsCursor, Compatibility.getCursorDisplayNameColumnIndex(allContactsCursor), " ABCDEFGHIJKLMNOPQRSTUVWXYZ");
//                adapter = new ContactsListAdapter(ContactsManager.getInstance().getAllContacts(), allContactsCursor);
//                contactsList.setAdapter(adapter);
//                contactsList.requestLayout();
//            }
//        }
//        ContactsManager.getInstance().setLinphoneContactsPrefered(onlyDisplayLinphoneContacts);
//    }


//    class ContactsListAdapter extends BaseAdapter implements SectionIndexer, Filterable {
//        private Cursor cursor;
//        TextDrawable drawable;
//        ColorGenerator generator;
//        private ItemFilter mFilter = new ItemFilter();
//        private ArrayList<Contact> originalData = null;
//        private ArrayList<Contact> filteredData = null;
//
//
//        ContactsListAdapter(ArrayList<Contact> contactsList, Cursor c) {
//            this.filteredData = contactsList;
//            this.originalData = contactsList;
//            cursor = c;
//            generator = ColorGenerator.MATERIAL;
//        }
//
//        public int getCount() {
//            if(filteredData != null){
//                return filteredData.size();
//            }else{
//                return 0;
//            }
//
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
//            if (!contact.getContact_number().isEmpty()) {
//                contact_number.setText(contact.getContact_number());
//            } else {
//                contact_number.setText(TextUtils.join(" , ", contact.getNumbersOrAddresses()));
//            }
//
//
//            ImageView icon = (ImageView) view.findViewById(R.id.icon);
//            ImageView character_icon = (ImageView) view.findViewById(R.id.character_icon);
//            if (contact.getPhoto() != null) {
//                icon.setImageBitmap(contact.getPhoto());
//                icon.setVisibility(View.VISIBLE);
//                character_icon.setVisibility(View.INVISIBLE);
//            } else if (contact.getPhotoUri() != null) {
//                icon.setVisibility(View.VISIBLE);
//                character_icon.setVisibility(View.INVISIBLE);
//                LinphoneUtils.setImagePictureFromUri(getActivity(), icon, contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
//            } else {
//                icon.setVisibility(View.INVISIBLE);
//                character_icon.setVisibility(View.VISIBLE);
//                int color = generator.getRandomColor();
//                if (!mappingList.containsKey(name.getText().toString())) {
//                    mappingList.put(name.getText().toString(), color);
//                }
//                if (mappingList.containsKey(name.getText().toString())) {
//                    drawable = TextDrawable.builder()
//                            .buildRoundRect(name.getText().toString().substring(0, 1), mappingList.get(name.getText().toString()), 96);
//                } else {
//                    drawable = TextDrawable.builder()
//                            .buildRoundRect(name.getText().toString().substring(0, 1), color, 96);
//                }
//
//                character_icon.setImageDrawable(drawable);
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
//                notifyDataSetChanged();
//                contactsList.requestLayout();
//                //contactsList.setVisibility(View.VISIBLE);
//            }
//
//        }
//    }

    /**
     * @return null if not ready yet
     */
    public static DialerFragment instance() {
        return instance;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (LinphoneActivity.isInstanciated()) {
            LinphoneActivity.instance().updateDialerFragment(this);
        }
        mAddressTextInterface = (AddressTextInterface) activity;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (LinphoneActivity.isInstanciated()) {
            LinphoneActivity.instance().selectMenu(FragmentsAvailable.DIALER);
            LinphoneActivity.instance().updateDialerFragment(this);
            LinphoneActivity.instance().showStatusBar();
        }

        //startVideoBtn.setChecked(mLinphonePreferences.shouldInitiateVideoCall());

        if (shouldEmptyAddressField) {
            mAddress.setText("");
        } else {
            shouldEmptyAddressField = true;
        }
        resetLayout(isCallTransferOngoing);

        if (mAddressTextInterface != null) {
            // Log.e("DialerFragment", "onResume ");
            mAddressTextInterface.getAddressText(mAddress);
            mAddressTextInterface.getAddressTextValue(mAddress.getText().toString());
        }


        Log.e("DIALER", "OnResumeCalled");
        //changeContactsAdapter();
    }

    public void resetLayout(boolean callTransfer) {
        isFromIncallScreen = true;
        isCallTransferOngoing = callTransfer;
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc == null) {
            return;
        }

        if (lc.getCallsNb() > 0) {
            if (isCallTransferOngoing) {
                mCall.setImageResource(R.drawable.options_transfer_default);
                mCall.setExternalClickListener(transferListener);
            } else {
                mCall.setImageResource(R.drawable.options_add_default_alt);
                mCall.resetClickListener();
            }
//			mAddContact.setEnabled(true);
//			mAddContact.setImageResource(R.drawable.cancel);
//			mAddContact.setOnClickListener(cancelListener);
        } else {
            mCall.setImageResource(R.drawable.call);
//			mAddContact.setEnabled(true);
//			mAddContact.setImageResource(R.drawable.add_contact);
//			mAddContact.setOnClickListener(addContactListener);
            enableDisableAddContact();
        }
    }


    public void enableDisableAddContact() {
        //mAddContact.setEnabled(LinphoneManager.getLc().getCallsNb() > 0 || !mAddress.getText().toString().equals(""));
    }

    public void displayTextInAddressBar(String numberOrSipAddress) {
        shouldEmptyAddressField = false;
        mAddress.setText(numberOrSipAddress);
    }

    public void newOutgoingCall(String numberOrSipAddress) {
        displayTextInAddressBar(numberOrSipAddress);
        LinphoneManager.getInstance().newOutgoingCall(mAddress);
    }

    public void newOutgoingCall(Intent intent) {
        if (intent != null && intent.getData() != null) {
            String scheme = intent.getData().getScheme();
            if (scheme.startsWith("imto")) {
                mAddress.setText("sip:" + intent.getData().getLastPathSegment());
            } else if (scheme.startsWith("call") || scheme.startsWith("sip")) {
                mAddress.setText(intent.getData().getSchemeSpecificPart());
            } else {
                Uri contactUri = intent.getData();
                String address = ContactsManager.getInstance().queryAddressOrNumber(LinphoneService.instance().getContentResolver(), contactUri);
                if (address != null) {
                    mAddress.setText(address);
                } else {
                    Log.e("Unknown scheme: ", scheme);
                    mAddress.setText(intent.getData().getSchemeSpecificPart());
                }
            }

            mAddress.clearDisplayedName();
            intent.setData(null);

            LinphoneManager.getInstance().newOutgoingCall(mAddress);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {

        if (isCallTransferOngoing) {
            LinphoneCore lc = LinphoneManager.getLc();
            if (lc.getCurrentCall() == null) {
                return;
            }
            lc.transferCall(lc.getCurrentCall(), mAddress.getText().toString());
            isCallTransferOngoing = false;
            LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();
        } else {
            Contact contact = (Contact) adapter.getItemAtPosition(position);
            android.util.Log.e("Contact details", "" + contact.getContact_number() + contact.getName());
            if (contact.getContact_number() != null) {
                LinphoneManager.getInstance().newOutgoingCall(contact.getContact_number(), contact.getName());
            }
        }

    }

    public interface AddressTextInterface {
        public AddressText getAddressText(AddressText addressText);

        public String getAddressTextValue(String string);
    }
}
