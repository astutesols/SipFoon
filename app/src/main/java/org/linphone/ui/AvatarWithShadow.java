package org.linphone.ui;
/*
AvatarWithShadow.java
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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.linphone.R;

/**
 * @author Sylvain Berfini
 */
public class AvatarWithShadow extends LinearLayout {
    private ImageView contactPicture;

    private Bitmap pictureBitmap;

    public AvatarWithShadow(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AvatarWithShadow);
        int pictureResId = array.getResourceId(R.styleable.AvatarWithShadow_picture, 0);
        array.recycle();

        pictureBitmap = BitmapFactory.decodeResource(getResources(), pictureResId);

        View view = LayoutInflater.from(context).inflate(R.layout.avatar, this);
        contactPicture = (ImageView) view.findViewById(R.id.picture);
        //contactPicture.setImageBitmap(mCircleImage.getCircleBitmap(pictureBitmap));
        //contactPicture.setBackgroundResource(pictureResId);
        //pictureBitmap = Bitmap.createScaledBitmap(pictureBitmap, 150, 150, false);
        contactPicture.setImageBitmap(pictureBitmap);
    }

    public ImageView getView() {
        return contactPicture;
    }

    public void setImageBitmap(Bitmap bitmap) {
        //pictureBitmap = Bitmap.createScaledBitmap(pictureBitmap, 150, 150, false);
        contactPicture.setImageBitmap(bitmap);
    }

    public void setImageResource(int res) {
//        pictureBitmap = BitmapFactory.decodeResource(getResources(), res);
//        //pictureBitmap = Bitmap.createScaledBitmap(pictureBitmap, 125, 125, false);
//        contactPicture.setImageBitmap( pictureBitmap);
        contactPicture.setImageResource(res);
    }

    public void setImageDrawable(Drawable drawable) {
        contactPicture.setImageDrawable(drawable);
    }

}