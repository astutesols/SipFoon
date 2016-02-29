package org.linphone;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.splunk.mint.Mint;

import org.linphone.api.Config;

public class BaseFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Config.RELEASE_MODE) {
            Mint.initAndStartSession(getActivity().getApplicationContext(), Config.MINT_PROJECT_ID);
        }
    }

}
