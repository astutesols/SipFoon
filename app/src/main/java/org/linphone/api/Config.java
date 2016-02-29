package org.linphone.api;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.util.Patterns;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * @author: Aniruddh Bhilvare
 * @since: 11 May,2015
 */

public class Config {

    //public static final String API_URL = "http://172.31.40.243/advcampaign/V1/";
    //public static final String API_URL = "http://192.168.1.101/hodupbx_api/";
    final protected static char[] hexArray = "0123456789abcdef".toCharArray();

    public static final String SERVER_DOMAIN = "66.226.73.200";;
    public static final String MINT_PROJECT_ID = "1a80f061";
    public static final boolean RELEASE_MODE = false;
    public static final String INFO_REQ_TAG = "INFO_REQ_TAG";
    public static final String AUTH_REQ_TAG = "AUTH_REQ_TAG";
    public static final String LOGO_REQ_TAG = "LOGO_REQ_TAG";
    public static final String USER_REQ_TAG = "USER_REQ_TAG";

    public static final String PREF_IS_LOGIN_SUCCESS = "PREF_IS_LOGIN_SUCCESS";

    public static final Pattern IP_ADDRESS = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");

    public static final Pattern DOMAIN = Pattern.compile("^[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,6}$");

    public static boolean isNetConnected(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if ((activeNetworkInfo != null) && activeNetworkInfo.isConnected()) {
            return true;
        } else {
            //Toast.makeText(ctx, "Internet no available.", Toast.LENGTH_SHORT);
            return false;
        }
    }

    public static boolean isSipAddressValid(Context ctx, String sipServer) {
        if (sipServer.length() == 0) {
            Toast.makeText(ctx, "SIP domain can't be empty.", Toast.LENGTH_SHORT).show();
            return false;
        } else if (DOMAIN.matcher(sipServer).matches() || Patterns.IP_ADDRESS.matcher(sipServer).matches()) {
            //Toast.makeText(ctx, "Invalid SIP server address.", Toast.LENGTH_SHORT).show();
            Log.e("isSipAddressValid: ", "1111");
            return true;
        } else {
            Log.e("isSipAddressValid: ", "2222");
            Toast.makeText(ctx, "Invalid SIP server address.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static boolean isValidDomain(String sipServer) {
        if (DOMAIN.matcher(sipServer).matches() || Patterns.IP_ADDRESS.matcher(sipServer).matches()) {
            return true;
        } else {
            return false;
        }
    }


    public static String saveToInternalSorage(Context ctx, Bitmap bitmapImage, String name) {
        ContextWrapper cw = new ContextWrapper(ctx);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("itscanada", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, name);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to
            // the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return directory.getAbsolutePath();
    }

    public static void loadImageFromStorage(Context ctx, ImageView imageview, String name) {
        try {
            ContextWrapper cw = new ContextWrapper(ctx);
            // path to /data/data/yourapp/app_data/imageDir
            File directory = cw.getDir("itscanada", Context.MODE_PRIVATE);
            File image = new File(directory, name);
            if (image.exists()) {
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(image));
                imageview.setImageBitmap(b);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Boolean deleteImage(Context ctx, String name) {
        try {
            ContextWrapper cw = new ContextWrapper(ctx);
            File directory = cw.getDir("itscanada", Context.MODE_PRIVATE);
            File image = new File(directory, name);
            return image.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String bytesArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void showAlert(Context ctx, String msg) {
        final AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(msg);
        alertDialog.setIcon(android.R.drawable.ic_delete);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // here you can add functions
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    public static void displayToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static String getMd5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return bytesArrayToHexString(md.digest(data.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
    }

    public static int nthOccurrence(String str, String c, int n) {
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1)
            pos = str.indexOf(c, pos + 1);
        return pos;
    }


}
