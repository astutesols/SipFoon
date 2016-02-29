package org.linphone;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;

import com.splunk.mint.Mint;

import org.linphone.api.Config;

public class BaseFragmentActivity extends FragmentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Config.RELEASE_MODE) {
            Mint.initAndStartSession(getApplicationContext(), Config.MINT_PROJECT_ID);
        }
    }

}

