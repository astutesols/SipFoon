package org.linphone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import org.linphone.compatibility.Compatibility;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Version;
import org.linphone.ui.AvatarWithShadow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class EditContactFragment extends BaseFragment {
    private View view;
    private ImageView ok;
    private TextView title;
    private Button contact_delete_button;
    ImageView contact_save_button;
    private EditText firstName, lastName;
    private LayoutInflater inflater;

    private boolean isNewContact = true;
    private Contact contact;
    private int contactID;
    private List<NewOrUpdatedNumberOrAddress> numbersAndAddresses;
    private ArrayList<ContentProviderOperation> ops;
    private int firstSipAddressIndex = -1;
    private String newSipOrNumberToAdd;
    private ContactsManager contactsManager;

    final CharSequence[] items = {"Take Photo", "Choose from Library", "Cancel"};
    int REQUEST_CAMERA = 0;
    int SELECT_FILE = 1;
    AvatarWithShadow contactPicture;
    Bitmap pictureBitmap;
    float scale;
    DisplayMetrics metrics;
    ProgressDialog pd;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.inflater = inflater;

        metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_HIGH:
                this.scale = 1.5f;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                this.scale = 1.0f;
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                this.scale = 2.0f;
                break;
            default:
                this.scale = 1.0f;
        }

        contact = null;
        if (getArguments() != null) {
            if (getArguments().getSerializable("Contact") != null) {
                contact = (Contact) getArguments().getSerializable("Contact");
                isNewContact = false;
                contactID = Integer.parseInt(contact.getID());
                contact.refresh(getActivity().getContentResolver());
                if (getArguments().getString("NewSipAdress") != null) {
                    newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
                }

            } else if (getArguments().getString("NewSipAdress") != null) {
                newSipOrNumberToAdd = getArguments().getString("NewSipAdress");
                isNewContact = true;
            }
        }

        contactsManager = ContactsManager.getInstance();

        view = inflater.inflate(R.layout.edit_contact, container, false);

        ImageView cancel = (ImageView) view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStackImmediate();
            }
        });

        contact_save_button = (ImageView) view.findViewById(R.id.contact_save_button);
        contact_delete_button = (Button) view.findViewById(R.id.contact_delete_button);
        ok = (ImageView) view.findViewById(R.id.ok);
        if (isNewContact) {
            ok.setVisibility(View.GONE);
            contact_delete_button.setVisibility(View.GONE);
            contact_save_button.setVisibility(View.VISIBLE);
            contact_save_button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
//                    pd = new ProgressDialog(LinphoneActivity.instance());
//                    pd.setTitle(getString(R.string.wait));
//                    pd.setMessage("Adding Contact");
//                    pd.setCancelable(false);
//                    pd.setIndeterminate(true);
//                    pd.show();
                    if (getResources().getBoolean(R.bool.forbid_empty_new_contact_in_editor)) {
                        boolean areAllFielsEmpty = true;
                        for (NewOrUpdatedNumberOrAddress nounoa : numbersAndAddresses) {
                            if (nounoa.newNumberOrAddress != null && !nounoa.newNumberOrAddress.equals("")) {
                                areAllFielsEmpty = false;
                                break;
                            }
                        }
                        if (areAllFielsEmpty) {
                            //pd.dismiss();
                            getFragmentManager().popBackStackImmediate();
                            return;
                        }
                    }
                    //android.util.Log.e("", "BITMAP NULL: "+String.valueOf(pictureBitmap==null));
                    contactsManager.createNewContact(ops, firstName.getText().toString(), lastName.getText().toString(), pictureBitmap);


                    for (NewOrUpdatedNumberOrAddress numberOrAddress : numbersAndAddresses) {
                        numberOrAddress.save();
                    }

                    try {
                        getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                        addLinphoneFriendIfNeeded();
                        removeLinphoneTagIfNeeded();
                        SharedPreferences preferance = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        SharedPreferences.Editor editor = preferance.edit();
                        editor.putString("TEMP_CONTACT_NAME", lastName.getText().toString().trim() + " " + firstName.getText().toString().trim());
                        editor.commit();
                        contactsManager.prepareContactsInBackground();
                        //contactsManager.prepareContactsInBackgroundWithUI(getActivity(), getFragmentManager(), pd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                     getFragmentManager().popBackStackImmediate();

                    if (LinphoneActivity.instance().getResources().getBoolean(R.bool.isTablet))
                        ContactsListFragment.instance().invalidate();
                }
            });
        } else {
            contact_delete_button.setVisibility(View.VISIBLE);
            contact_delete_button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setMessage(getString(R.string.delete_contact_dialog));
                    alertDialog.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            deleteExistingContact();
                            ContactsManager.getInstance().removeContactFromLists(getActivity().getContentResolver(), contact);
                            LinphoneActivity.instance().displayContacts(false);
                        }
                    });
                    alertDialog.setNegativeButton(getString(R.string.button_cancel), null);
                    alertDialog.show();
                }
            });
            ok.setVisibility(View.VISIBLE);
            contact_save_button.setVisibility(View.GONE);
            ok.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
//                    pd = new ProgressDialog(LinphoneActivity.instance());
//                    pd.setTitle(getString(R.string.wait));
//                    pd.setMessage("Updating Contact");
//                    pd.setCancelable(false);
//                    pd.setIndeterminate(true);
//                    pd.show();
                    contactsManager.updateExistingContact(ops, contact, firstName.getText().toString(), lastName.getText().toString());

                    if (pictureBitmap != null) {
                        ByteArrayOutputStream image = new ByteArrayOutputStream();
                        pictureBitmap.compress(Bitmap.CompressFormat.PNG, 100, image);
                        byte[] image_data = image.toByteArray();
                        if (!isNewContact) {
                            setContactPhoto(image_data, contactID);
                        }
                    }


                    for (NewOrUpdatedNumberOrAddress numberOrAddress : numbersAndAddresses) {
                        numberOrAddress.save();
                    }

                    try {
                        getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

                        addLinphoneFriendIfNeeded();
                        removeLinphoneTagIfNeeded();
                        //contactsManager.prepareContactsInBackground();
                        SharedPreferences preferance = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        SharedPreferences.Editor editor = preferance.edit();
                        editor.putString("TEMP_CONTACT_NAME", firstName.getText().toString().trim() + " " + lastName.getText().toString().trim());
                        editor.commit();
                       contactsManager.prepareContactsInBackground();
                        //contactsManager.prepareContactsInBackgroundWithUI(getActivity(), getFragmentManager(), pd);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    getFragmentManager().popBackStackImmediate();

                    if (LinphoneActivity.instance().getResources().getBoolean(R.bool.isTablet))
                        ContactsListFragment.instance().invalidate();
                }
            });
        }

        contactPicture = (AvatarWithShadow) view.findViewById(R.id.contactPicture);
        if (contact != null && contact.getPhotoUri() != null) {
            contactPicture.setVisibility(View.VISIBLE);
            LinphoneUtils.setImagePictureFromUri(view.getContext(), contactPicture.getView(), contact.getPhotoUri(), contact.getThumbnailUri(), R.drawable.unknown_small);
        }
        else {
            contactPicture.setVisibility(View.VISIBLE);
            contactPicture.setAlpha(0.0f);
        }
        contactPicture.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Change Picture");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        if (items[item].equals("Take Photo")) {
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            File f = new File(android.os.Environment.getExternalStorageDirectory(), "temp.jpg");
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                            startActivityForResult(intent, REQUEST_CAMERA);
                        } else if (items[item].equals("Choose from Library")) {
                            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            intent.setType("image/*");
                            //intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                            startActivityForResult(intent, SELECT_FILE);

//                            startActivityForResult(
//                                    Intent.createChooser(
//                                            new Intent(Intent.ACTION_GET_CONTENT)
//                                                    .setType("image/*"), "Choose an image"),
//                                    SELECT_FILE);
                        } else if (items[item].equals("Cancel")) {
                            dialog.dismiss();
                        }
                    }
                });
                builder.show();
            }
        });

        lastName = (EditText) view.findViewById(R.id.contactLastName);
        // Hack to display keyboard when touching focused edittext on Nexus One
        if (Version.sdkStrictlyBelow(Version.API11_HONEYCOMB_30)) {
            lastName.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm = (InputMethodManager) LinphoneActivity.instance().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
            });
        }
        lastName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (lastName.getText().length() > 0 || firstName.getText().length() > 0) {
                    ok.setEnabled(true);
                } else {
                    ok.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        firstName = (EditText) view.findViewById(R.id.contactFirstName);
        firstName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (firstName.getText().length() > 0 || lastName.getText().length() > 0) {
                    ok.setEnabled(true);
                } else {
                    ok.setEnabled(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        title = (TextView) view.findViewById(R.id.title);
        if (!isNewContact) {
            String fn = findContactFirstName(String.valueOf(contactID));
            String ln = findContactLastName(String.valueOf(contactID));
            if (fn != null || ln != null) {
                firstName.setText(fn);
                lastName.setText(ln);
            } else {
                lastName.setText(contact.getName());
                firstName.setText("");
            }
            title.setText(getString(R.string.edit_contact_title));
        } else {
            title.setText(getString(R.string.add_contact_title));
            //pictureBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.unknown_small);
        }



        initNumbersFields((TableLayout) view.findViewById(R.id.controls), contact);

        ops = new ArrayList<ContentProviderOperation>();
        lastName.requestFocus();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (LinphoneActivity.isInstanciated()) {
            if (getResources().getBoolean(R.bool.show_statusbar_only_on_dialer)) {
                LinphoneActivity.instance().hideStatusBar();
            }
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
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                contactPicture.setVisibility(View.VISIBLE);
                contactPicture.setAlpha(1.0f);
                File f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    BitmapFactory.Options btmapOptions = new BitmapFactory.Options();
                    pictureBitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), btmapOptions);

                    ExifInterface exif = new ExifInterface(f.getAbsolutePath());
                    int pictureOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
                    Matrix matrix = new Matrix();
                    if (pictureOrientation == 6) {
                        matrix.postRotate(90);
                    } else if (pictureOrientation == 3) {
                        matrix.postRotate(180);
                    } else if (pictureOrientation == 8) {
                        matrix.postRotate(270);
                    }
                    pictureBitmap = Bitmap.createBitmap(pictureBitmap, 0, 0, pictureBitmap.getWidth(), pictureBitmap.getHeight(), matrix, true);

                    pictureBitmap = Bitmap.createScaledBitmap(pictureBitmap, (int) (300 * scale), (int) (300 * scale), true);
                    contactPicture.setImageBitmap(pictureBitmap);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SELECT_FILE) {
                contactPicture.setVisibility(View.VISIBLE);
                contactPicture.setAlpha(1.0f);
                Uri selectedImageUri = data.getData();
                String tempPath = getPath(selectedImageUri, getActivity());
                String url = data.getData().toString();
                if (url.startsWith("content://com.google.android.apps.photos.content")){
                    try {
                        Log.e("Profile","Fetching image"+data.getData().toString());
                        InputStream is = getActivity().getContentResolver().openInputStream(selectedImageUri);
                        if (is != null) {
                            pictureBitmap = BitmapFactory.decodeStream(is);
                            pictureBitmap = Bitmap.createScaledBitmap(pictureBitmap, (int) (300 * scale), (int) (300 * scale), true);
                            contactPicture.setImageBitmap(pictureBitmap);
                        }
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }else {
                    BitmapFactory.Options btmapOptions = new BitmapFactory.Options();
                    pictureBitmap = BitmapFactory.decodeFile(tempPath, btmapOptions);
                    if (pictureBitmap != null) {
                        pictureBitmap = Bitmap.createScaledBitmap(pictureBitmap, (int) (300 * scale), (int) (300 * scale), true);
                        contactPicture.setImageBitmap(pictureBitmap);
                    }
                }
            }
        }
    }

//    @SuppressLint("NewApi")
//    private String getPath(Uri uri, Activity activity) {
//        if( uri == null ) {
//            return null;
//        }
//
//        String[] projection = { MediaStore.Images.Media.DATA };
//
//        Cursor cursor;
//        if(Build.VERSION.SDK_INT >19)
//        {
//            // Will return "image:x*"
//            String wholeID = DocumentsContract.getDocumentId(uri);
//            // Split at colon, use second item in the array
//            String id = wholeID.split(":")[1];
//            // where id is equal to
//            String sel = MediaStore.Images.Media._ID + "=?";
//
//            cursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    projection, sel, new String[]{ id }, null);
//        }
//        else
//        {
//            cursor = activity.getContentResolver().query(uri, projection, null, null, null);
//        }
//        String path = null;
//        try
//        {
//            int column_index = cursor
//                    .getColumnIndex(MediaStore.Images.Media.DATA);
//            cursor.moveToFirst();
//            path = cursor.getString(column_index).toString();
//            cursor.close();
//        }
//        catch(NullPointerException e) {
//
//        }
//        return path;
//    }

    public String getPath(Uri uri, Activity activity) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.MediaColumns.DATA};
            // cursor = activity.managedQuery(uri, projection, null, null,
            // null);
            cursor = activity.getContentResolver().query(uri, projection, null, null, null);
            if (cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {

        } finally {
            cursor.close();
        }
        return "";
    }

    public void setContactPhoto(byte[] bytes, int personId) {
        Log.e("", "setting contact photo for: " + personId);
        ContentResolver c = getActivity().getContentResolver();
        ContentValues values = new ContentValues();
        personId = getRawContactId(personId);
        int photoRow = -1;
        String where = ContactsContract.Data.RAW_CONTACT_ID + " = " + personId + " AND " + ContactsContract.Data.MIMETYPE + "=='"
                + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'";
        Cursor cursor = c.query(ContactsContract.Data.CONTENT_URI, null, where, null, null);
        int idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Data._ID);
        if (cursor.moveToFirst()) {
            photoRow = cursor.getInt(idIdx);
        }
        cursor.close();

        values.put(ContactsContract.Data.RAW_CONTACT_ID, personId);
        values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

        if (photoRow >= 0) {
            c.update(ContactsContract.Data.CONTENT_URI, values, ContactsContract.Data._ID + " = " + photoRow, null);
        } else {
            c.insert(ContactsContract.Data.CONTENT_URI, values);
        }
    }

    public int getRawContactId(int contactId) {
        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(contactId)};
        // Cursor c =
        // getActivity().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI,
        // projection, selection, selectionArgs, null);
        Cursor c = getActivity().getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            return c.getInt(c.getColumnIndex(ContactsContract.RawContacts._ID));
        }
        return -1;
    }

    private void initNumbersFields(final TableLayout controls, final Contact contact) {
        controls.removeAllViews();
        numbersAndAddresses = new ArrayList<NewOrUpdatedNumberOrAddress>();

        if (contact != null) {
            for (String numberOrAddress : contact.getNumbersOrAddresses()) {
                View view = displayNumberOrAddress(controls, numberOrAddress);
                if (view != null)
                    controls.addView(view);
            }
        }
        if (newSipOrNumberToAdd != null) {
            View view = displayNumberOrAddress(controls, newSipOrNumberToAdd);
            if (view != null)
                controls.addView(view);
        }

        // Add one for phone numbers, one for SIP address
        if (!getResources().getBoolean(R.bool.hide_phone_numbers_in_editor)) {
            addEmptyRowToAllowNewNumberOrAddress(controls, false);
        }

        if (!getResources().getBoolean(R.bool.hide_sip_addresses_in_editor)) {
            firstSipAddressIndex = controls.getChildCount() - 2; // Update the value to always display phone numbers before SIP accounts
            addEmptyRowToAllowNewNumberOrAddress(controls, true);
        }
    }

    private View displayNumberOrAddress(final TableLayout controls, String numberOrAddress) {
        return displayNumberOrAddress(controls, numberOrAddress, false);
    }

    @SuppressLint("InflateParams")
    private View displayNumberOrAddress(final TableLayout controls, String numberOrAddress, boolean forceAddNumber) {
        boolean isSip = LinphoneUtils.isStrictSipAddress(numberOrAddress) || !LinphoneUtils.isNumberAddress(numberOrAddress);

        if (isSip) {
            if (firstSipAddressIndex == -1) {
                firstSipAddressIndex = controls.getChildCount();
            }
            numberOrAddress = numberOrAddress.replace("sip:", "");
        }
        if ((getResources().getBoolean(R.bool.hide_phone_numbers_in_editor) && !isSip) || (getResources().getBoolean(R.bool.hide_sip_addresses_in_editor) && isSip)) {
            if (forceAddNumber)
                isSip = !isSip; // If number can't be displayed because we hide a sort of number, change that category
            else
                return null;
        }

        NewOrUpdatedNumberOrAddress tempNounoa;
        if (forceAddNumber) {
            tempNounoa = new NewOrUpdatedNumberOrAddress(isSip);
        } else {
            if (isNewContact || newSipOrNumberToAdd != null) {
                tempNounoa = new NewOrUpdatedNumberOrAddress(isSip, numberOrAddress);
            } else {
                tempNounoa = new NewOrUpdatedNumberOrAddress(numberOrAddress, isSip);
            }
        }
        final NewOrUpdatedNumberOrAddress nounoa = tempNounoa;
        numbersAndAddresses.add(nounoa);

        final View view = inflater.inflate(R.layout.contact_edit_row, null);

        final EditText noa = (EditText) view.findViewById(R.id.numoraddr);
        noa.setInputType(isSip ? InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_PHONE);
        noa.setText(numberOrAddress);
        noa.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                nounoa.setNewNumberOrAddress(noa.getText().toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        if (forceAddNumber) {
            nounoa.setNewNumberOrAddress(noa.getText().toString());
        }

        ImageView delete = (ImageView) view.findViewById(R.id.delete);
        delete.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                nounoa.delete();
                numbersAndAddresses.remove(nounoa);
                view.setVisibility(View.GONE);

            }
        });
        return view;
    }

    @SuppressLint("InflateParams")
    private void addEmptyRowToAllowNewNumberOrAddress(final TableLayout controls, final boolean isSip) {
        final View view = inflater.inflate(R.layout.contact_add_row, null);

        final NewOrUpdatedNumberOrAddress nounoa = new NewOrUpdatedNumberOrAddress(isSip);

        final EditText noa = (EditText) view.findViewById(R.id.numoraddr);
        numbersAndAddresses.add(nounoa);
        noa.setHint(isSip ? getString(R.string.sip_address) : getString(R.string.phone_number));
        noa.setInputType(isSip ? InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_PHONE);
        noa.requestFocus();
        noa.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                nounoa.setNewNumberOrAddress(noa.getText().toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final ImageView add = (ImageView) view.findViewById(R.id.add);
        add.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add a line, and change add button for a delete button
                add.setImageResource(R.drawable.ic_close_grey);
                ImageView delete = add;
                delete.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        nounoa.delete();
                        numbersAndAddresses.remove(nounoa);
                        view.setVisibility(View.GONE);
                    }
                });
                if (!isSip) {
                    firstSipAddressIndex++;
                    addEmptyRowToAllowNewNumberOrAddress(controls, false);
                } else {
                    addEmptyRowToAllowNewNumberOrAddress(controls, true);
                }
            }
        });

        if (isSip) {
            controls.addView(view, controls.getChildCount());
        } else {
            if (firstSipAddressIndex != -1) {
                controls.addView(view, firstSipAddressIndex);
            } else {
                controls.addView(view);
            }
        }
    }

    private String findContactFirstName(String contactID) {
        Cursor c = getActivity().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME},
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{contactID, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE}, null);
        if (c != null) {
            String result = null;
            if (c.moveToFirst()) {
                result = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
            }
            c.close();
            return result;
        }
        return null;
    }

    private String findContactLastName(String contactID) {
        Cursor c = getActivity().getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME},
                ContactsContract.Data.CONTACT_ID + "=? AND " + ContactsContract.Data.MIMETYPE + "=?",
                new String[]{contactID, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE}, null);
        if (c != null) {
            String result = null;
            if (c.moveToFirst()) {
                result = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            }
            c.close();
            return result;
        }
        return null;
    }

    private void addLinphoneFriendIfNeeded() {
        for (NewOrUpdatedNumberOrAddress numberOrAddress : numbersAndAddresses) {
            if (numberOrAddress.newNumberOrAddress != null && numberOrAddress.isSipAddress) {
                if (isNewContact) {
                    Contact c = contactsManager.findContactWithDisplayName(ContactsManager.getInstance().getDisplayName(firstName.getText().toString(), lastName.getText().toString()));
                    if (c != null && !contactsManager.isContactHasAddress(c, numberOrAddress.newNumberOrAddress)) {
                        contactsManager.createNewFriend(c, numberOrAddress.newNumberOrAddress);
                    }
                } else {
                    if (!contactsManager.isContactHasAddress(contact, numberOrAddress.newNumberOrAddress)) {
                        if (numberOrAddress.oldNumberOrAddress == null) {
                            contactsManager.createNewFriend(contact, numberOrAddress.newNumberOrAddress);
                        } else {
                            if (contact.hasFriends())
                                contactsManager.updateFriend(numberOrAddress.oldNumberOrAddress, numberOrAddress.newNumberOrAddress);
                        }
                    }
                }
            }
        }
    }

    private void removeLinphoneTagIfNeeded() {
        if (!isNewContact) {
            boolean areAllSipFielsEmpty = true;
            for (NewOrUpdatedNumberOrAddress nounoa : numbersAndAddresses) {
                if (!nounoa.isSipAddress && (nounoa.oldNumberOrAddress != null && !nounoa.oldNumberOrAddress.equals("") || nounoa.newNumberOrAddress != null && !nounoa.newNumberOrAddress.equals(""))) {
                    areAllSipFielsEmpty = false;
                    break;
                }
            }
            if (areAllSipFielsEmpty && contactsManager.findRawLinphoneContactID(contact.getID()) != null) {
                contactsManager.removeLinphoneContactTag(contact);
            }
        }
    }

    class NewOrUpdatedNumberOrAddress {
        private String oldNumberOrAddress;
        private String newNumberOrAddress;
        private boolean isSipAddress;

        public NewOrUpdatedNumberOrAddress() {
            oldNumberOrAddress = null;
            newNumberOrAddress = null;
            isSipAddress = false;
        }

        public NewOrUpdatedNumberOrAddress(boolean isSip) {
            oldNumberOrAddress = null;
            newNumberOrAddress = null;
            isSipAddress = isSip;
        }

        public NewOrUpdatedNumberOrAddress(String old, boolean isSip) {
            oldNumberOrAddress = old;
            newNumberOrAddress = null;
            isSipAddress = isSip;
        }

        public NewOrUpdatedNumberOrAddress(boolean isSip, String newSip) {
            oldNumberOrAddress = null;
            newNumberOrAddress = newSip;
            isSipAddress = isSip;
        }

        public void setNewNumberOrAddress(String newN) {
            newNumberOrAddress = newN;
        }

        public void save() {
            if (newNumberOrAddress == null || newNumberOrAddress.equals(oldNumberOrAddress))
                return;

            if (oldNumberOrAddress == null) {
                // New number to add
                addNewNumber();
            } else {
                // Old number to update
                updateNumber();
            }
        }

        public void delete() {
            if (isSipAddress) {
                if (contact != null && contact.hasFriends()) {
                    ContactsManager.getInstance().removeFriend(oldNumberOrAddress);
                } else {
                    Compatibility.deleteSipAddressFromContact(ops, oldNumberOrAddress, String.valueOf(contactID));
                }
                if (getResources().getBoolean(R.bool.use_linphone_tag)) {
                    Compatibility.deleteLinphoneContactTag(ops, oldNumberOrAddress, contactsManager.findRawLinphoneContactID(String.valueOf(contactID)));
                }
            } else {
                String select = ContactsContract.Data.CONTACT_ID + "=? AND "
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' AND "
                        + ContactsContract.CommonDataKinds.Phone.NUMBER + "=?";
                String[] args = new String[]{String.valueOf(contactID), oldNumberOrAddress};

                ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                .withSelection(select, args)
                                .build()
                );
            }
        }

        private void addNewNumber() {
            if (newNumberOrAddress == null || newNumberOrAddress.length() == 0) {
                return;
            }

            if (isNewContact) {
                if (isSipAddress) {
                    if (newNumberOrAddress.startsWith("sip:"))
                        newNumberOrAddress = newNumberOrAddress.substring(4);
                    if (!newNumberOrAddress.contains("@")) {
                        //Use default proxy config domain if it exists
                        LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
                        if (lpc != null) {
                            newNumberOrAddress = newNumberOrAddress + "@" + lpc.getDomain();
                        } else {
                            newNumberOrAddress = newNumberOrAddress + "@" + getResources().getString(R.string.default_domain);
                        }
                    }
                    Compatibility.addSipAddressToContact(getActivity(), ops, newNumberOrAddress);
                } else {

//                    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
//                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());

                    // Commented by Rajesh Jadav
                    //ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //pictureBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    //byte[] photo = baos.toByteArray();

                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumberOrAddress)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getString(R.string.addressbook_label))
                                    .build()
                    );

                    // Commented by Rajesh Jadav for disbling adding default profile picture in contact
//                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
//                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, contactID)
//                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
//                            .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photo).build());
                }
            } else {
                String rawContactId = contactsManager.findRawContactID(getActivity().getContentResolver(), String.valueOf(contactID));
                if (isSipAddress) {
                    if (newNumberOrAddress.startsWith("sip:"))
                        newNumberOrAddress = newNumberOrAddress.substring(4);
                    if (!newNumberOrAddress.contains("@")) {
                        //Use default proxy config domain if it exists
                        LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
                        if (lpc != null) {
                            newNumberOrAddress = newNumberOrAddress + "@" + lpc.getDomain();
                        } else {
                            newNumberOrAddress = newNumberOrAddress + "@" + getResources().getString(R.string.default_domain);
                        }
                    }

                    Compatibility.addSipAddressToContact(getActivity(), ops, newNumberOrAddress, rawContactId);
                    if (getResources().getBoolean(R.bool.use_linphone_tag)) {
                        Compatibility.addLinphoneContactTag(getActivity(), ops, newNumberOrAddress, contactsManager.findRawLinphoneContactID(String.valueOf(contactID)));
                    }
                } else {
                    ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumberOrAddress)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM)
                                    .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, getString(R.string.addressbook_label))
                                    .build()
                    );
                }
            }
        }

        private void updateNumber() {
            if (newNumberOrAddress == null || newNumberOrAddress.length() == 0) {
                return;
            }

            if (isSipAddress) {
                if (newNumberOrAddress.startsWith("sip:"))
                    newNumberOrAddress = newNumberOrAddress.substring(4);
                if (!newNumberOrAddress.contains("@")) {
                    //Use default proxy config domain if it exists
                    LinphoneProxyConfig lpc = LinphoneManager.getLc().getDefaultProxyConfig();
                    if (lpc != null) {
                        newNumberOrAddress = newNumberOrAddress + "@" + lpc.getDomain();
                    } else {
                        newNumberOrAddress = newNumberOrAddress + "@" + getResources().getString(R.string.default_domain);
                    }
                }
                Compatibility.updateSipAddressForContact(ops, oldNumberOrAddress, newNumberOrAddress, String.valueOf(contactID));
                if (getResources().getBoolean(R.bool.use_linphone_tag)) {
                    Compatibility.updateLinphoneContactTag(getActivity(), ops, newNumberOrAddress, oldNumberOrAddress, contactsManager.findRawLinphoneContactID(String.valueOf(contactID)));
                }
            } else {
                String select = ContactsContract.Data.CONTACT_ID + "=? AND "
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "' AND "
                        + ContactsContract.CommonDataKinds.Phone.NUMBER + "=?";
                String[] args = new String[]{String.valueOf(contactID), oldNumberOrAddress};

                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection(select, args)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumberOrAddress)
                                .build()
                );
            }
        }
    }
}