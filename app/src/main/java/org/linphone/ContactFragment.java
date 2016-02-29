package org.linphone;
/*
ContactFragment.java
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
import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.linphone.api.ColorGenerator;
import org.linphone.api.TextDrawable;
import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.ui.AvatarWithShadow;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author Sylvain Berfini
 */
public class ContactFragment extends BaseFragment implements OnClickListener {
    private Contact contact;
    private ImageView editContact;
    private LayoutInflater inflater;
    private View view;
    private boolean displayChatAddressOnly = false;

    private OnClickListener dialListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (LinphoneActivity.isInstanciated()) {
                LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
                if (lc != null) {
                    LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
                    String to;
                    if (lpc != null) {
                        String address = v.getTag().toString();
                        if (!address.contains("@")) {
                            to = lpc.normalizePhoneNumber(address);
                        } else {
                            to = v.getTag().toString();
                        }
                    } else {
                        to = v.getTag().toString();
                    }
                    LinphoneActivity.instance().setAddresGoToDialerAndCall(to, contact.getName(), contact.getPhotoUri());
                }
            }
        }
    };

    private OnClickListener chatListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (LinphoneActivity.isInstanciated()) {
                LinphoneActivity.instance().displayChat(v.getTag().toString());
            }
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contact = (Contact) getArguments().getSerializable("Contact");

        this.inflater = inflater;
        view = inflater.inflate(R.layout.contact, container, false);

        if (getArguments() != null) {
            displayChatAddressOnly = getArguments().getBoolean("ChatAddressOnly");
        }

        editContact = (ImageView) view.findViewById(R.id.editContact);
        editContact.setOnClickListener(this);
        view.findViewById(R.id.backBtn).setOnClickListener(this);

//		deleteContact = (TextView) view.findViewById(R.id.deleteContact);
//		deleteContact.setOnClickListener(this);

        return view;
    }

    public void changeDisplayedContact(Contact newContact) {
        contact = newContact;
        contact.refresh(getActivity().getContentResolver());
        displayContact(inflater, view);
    }

    @SuppressLint("InflateParams")
    private void displayContact(LayoutInflater inflater, View view) {
        AvatarWithShadow contactPicture = (AvatarWithShadow) view.findViewById(R.id.contactPicture);
        if (contact.getPhotoUri() != null && getPhotoUriById(getActivity(), contact.getID()) != null) {
            contactPicture.setVisibility(View.VISIBLE);
            LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture.getView(), contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
//            InputStream input = Compatibility.getContactPictureInputStream(LinphoneActivity.instance().getContentResolver(), contact.getID());
//            contactPicture.setImageBitmap(BitmapFactory.decodeStream(input));
        }else{
            contactPicture.setVisibility(View.GONE);
        }

        TextView contactName = (TextView) view.findViewById(R.id.contactName);
        contactName.setText(contact.getName());

        ImageView UserCharacter = (ImageView) view.findViewById(R.id.UserCharacter);

        if (contactName.getText().toString().length()> 0)
        UserCharacter.setImageDrawable(TextDrawable.builder()
                .buildRoundRect(contactName.getText().toString().substring(0, 1), getResources().getColor(R.color.colorPrimary), 80));



        TableLayout controls = (TableLayout) view.findViewById(R.id.controls);
        controls.removeAllViews();



        for (String numberOrAddress : contact.getNumbersOrAddresses()) {
            View v = inflater.inflate(R.layout.contact_control_row, null);

            String displayednumberOrAddress = numberOrAddress;
            if (numberOrAddress.startsWith("sip:")) {
                displayednumberOrAddress = displayednumberOrAddress.replace("sip:", "");
            }

            TextView tv = (TextView) v.findViewById(R.id.numeroOrAddress);
            tv.setText(displayednumberOrAddress);
            tv.setSelected(true);

            if (!displayChatAddressOnly) {
                v.findViewById(R.id.dial).setOnClickListener(dialListener);
                v.findViewById(R.id.dial).setTag(displayednumberOrAddress);
            } else {
                v.findViewById(R.id.dial).setVisibility(View.GONE);
            }

            v.findViewById(R.id.start_chat).setOnClickListener(chatListener);
            LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
            if (lpc != null) {
                displayednumberOrAddress = lpc.normalizePhoneNumber(displayednumberOrAddress);
                if (!displayednumberOrAddress.startsWith("sip:")) {
                    numberOrAddress = "sip:" + displayednumberOrAddress;
                }

                String tag = numberOrAddress;
                if (!numberOrAddress.contains("@")) {
                    tag = numberOrAddress + "@" + lpc.getDomain();
                }
                v.findViewById(R.id.start_chat).setTag(tag);
            } else {
                v.findViewById(R.id.start_chat).setTag(numberOrAddress);
            }

            final String finalNumberOrAddress = numberOrAddress;
            ImageView friend = (ImageView) v.findViewById(R.id.addFriend);
            if (getResources().getBoolean(R.bool.enable_linphone_friends) && !displayChatAddressOnly) {
                friend.setVisibility(View.VISIBLE);

                boolean isAlreadyAFriend = LinphoneManager.getLc().findFriendByAddress(finalNumberOrAddress) != null;
                if (!isAlreadyAFriend) {
                    friend.setImageResource(R.drawable.friend_add);
                    friend.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ContactsManager.getInstance().createNewFriend(contact, finalNumberOrAddress)) {
                                displayContact(ContactFragment.this.inflater, ContactFragment.this.view);
                            }
                        }
                    });
                } else {
                    friend.setImageResource(R.drawable.friend_remove);
                    friend.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ContactsManager.getInstance().removeFriend(finalNumberOrAddress)) {
                                displayContact(ContactFragment.this.inflater, ContactFragment.this.view);
                            }
                        }
                    });
                }
            }

            if (getResources().getBoolean(R.bool.disable_chat)) {
                v.findViewById(R.id.start_chat).setVisibility(View.GONE);
            }

            View divider = new View(getActivity());
            divider.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 4));
            divider.setBackgroundColor(getActivity().getResources().getColor(R.color.listview_cell_divider_color));

            controls.addView(v);
            controls.addView(divider);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (LinphoneActivity.isInstanciated()) {
            LinphoneActivity.instance().selectMenu(FragmentsAvailable.CONTACT);

            if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
                LinphoneActivity.instance().hideStatusBar();
            }
        }

        contact.refresh(getActivity().getContentResolver());

        // To solve issue of contact detail screen getting empty
        if (contact.getName() == null) {
            SharedPreferences preferance = PreferenceManager.getDefaultSharedPreferences(getActivity());

            android.util.Log.e("name from preference:", preferance.getString("TEMP_CONTACT_NAME",""));

            Cursor cur = getActivity().getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts._ID},
                    ContactsContract.Contacts.DISPLAY_NAME + "=?",
                    new String[]{preferance.getString("TEMP_CONTACT_NAME", "")},
                    null);
            if (cur.getCount() > 0) {
                while (cur.moveToNext()) {
                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    contact.setID(id);
                    Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
                    contact.setPhotoUri(Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.DISPLAY_PHOTO));
                    contact.refresh(getActivity().getContentResolver());
                    //Log.e("Retrived ID: " + id);
                }
            }
        }

        if (contact.getName() == null || contact.getName().equals("")) {
            //Contact has been deleted, return
            LinphoneActivity.instance().displayContacts(false);
        }

        contact.setPhotoUri(getPhotoUriById(getActivity(), contact.getID()));
        displayContact(inflater, view);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.editContact) {
            LinphoneActivity.instance().editContact(contact);
        }
//        else if (id == R.id.deleteContact) {
//            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
//            alertDialog.setMessage(getString(R.string.delete_contact_dialog));
//            alertDialog.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    deleteExistingContact();
//                    ContactsManager.getInstance().removeContactFromLists(getActivity().getContentResolver(), contact);
//                    LinphoneActivity.instance().displayContacts(false);
//                }
//            });
//            alertDialog.setNegativeButton(getString(R.string.button_cancel), null);
//            alertDialog.show();
//        }
        else if (id == R.id.backBtn) {
            getActivity().getSupportFragmentManager().popBackStackImmediate();
        }
    }

    private void deleteExistingContact() {
        String select = ContactsContract.Data.CONTACT_ID + " = ?";
        String[] args = new String[]{contact.getID()};

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

    public static Uri getPhotoUriById(Context context, String id) {
        if (id == null || context == null) return null;

        Uri photo = null;//Uri.parse( "android.resource://"+ context.getPackageName() +"/" + R.drawable.unknown_small );

        try {
            Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.PHOTO_ID, "photo_uri"},
                    ContactsContract.Contacts._ID + " = " + id + " AND " + ContactsContract.Contacts.PHOTO_ID + " != 0",
                    null,
                    null);
            if (cursor == null)
                return photo;
            if (!cursor.moveToFirst()) {
                cursor.close();
                return photo;
            }
            String sUri;
            if ((sUri = cursor.getString(cursor.getColumnIndex("photo_uri"))) != null) {
                cursor.close(); // content://com.android.contacts/display_photo/1
                return Uri.parse(sUri);
            }
            cursor.close();
            return photo;
        } catch (Exception e) {
            Cursor cursor = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
                    new String[]{ContactsContract.Contacts.PHOTO_ID},
                    ContactsContract.Contacts._ID + " = " + id + " AND " + ContactsContract.Contacts.PHOTO_ID + " != 0",
                    null,
                    null);
            if (cursor == null)
                return photo;
            if (!cursor.moveToFirst()) {
                cursor.close();
                return photo;
            }
            cursor.close();
            photo = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(id));
            photo = Uri.withAppendedPath(photo, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            return photo;
        }
    }
}
