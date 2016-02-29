package org.linphone.ui;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by Aniruddh bhilvare on 2/26/15.
 */

public class MainViewPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;
    Context mContext;

//    private int[] imageResId = {
//            R.drawable.contacts_default,
//            R.drawable.history_default,
//            R.drawable.dialer_default,
//            R.drawable.chat_default,
//            R.drawable.settings_default
//    };

    public MainViewPagerAdapter(Context ctx, FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
        mContext = ctx;
    }

    @Override
    public Fragment getItem(int position) {
//        Bundle extras = new Bundle();
//        extras.putString("SipUri", sipUri);
//        if (contact != null) {
//            extras.putString("DisplayName", displayName);
//            extras.putString("PictureUri", pictureUri);
//            extras.putString("ThumbnailUri", thumbnailUri);
//        }
//        fragments.get(position).setArguments(extras);
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        // return tabTitles[position];
//        Drawable image = mContext.getResources().getDrawable(imageResId[position]);
//        image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
//        SpannableString sb = new SpannableString(" ");
//        ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
//        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        return sb;
        return  ""+position;
    }
}
