package org.linphone.setup;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;

import org.linphone.R;

/**
 * Created by astute on 18/11/15.
 * Copyright (c) 2015, Astute Solutions. All rights reserved.
 */
public class SetupViewPagerAdapter extends FragmentPagerAdapter {

    private static int FRAGMENT_COUNT = 3;
    private String tabTitles[] = new String[]{"Login", "New Register", "My SIP Login"};
    private Context mContext;

    public SetupViewPagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);
        mContext = ctx;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {

            case 0:
                return LinphoneLoginFragment.newInstance(position, "LinphoneLoginFragment");
            case 1:
                return RegisterFragment.newInstance(position, "RegisterFragment");
            case 2:
                return GenericLoginFragment.newInstance(position, "GenericLoginFragment");
            default:
                return LinphoneLoginFragment.newInstance(position, "LinphoneLoginFragment");
        }
    }

    @Override
    public int getCount() {
        return FRAGMENT_COUNT;
    }

    public View getTabView(int position) {
        // Given you have a custom layout in `res/layout/custom_tab.xml` with a TextView and ImageView
        //View v = LayoutInflater.from(mContext).inflate(R.layout.custom_tab, null);
        TextView tv = new TextView(mContext);
        tv.setTextColor(ContextCompat.getColor(mContext, R.color.white));
        tv.setText(tabTitles[position]);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        return tv;
    }
}
