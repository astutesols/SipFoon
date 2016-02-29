package org.linphone.setup;
/*
GenericLoginFragment.java
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

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import org.linphone.BaseFragment;
import org.linphone.R;

/**
 * @author Sylvain Berfini
 */
public class GenericLoginFragment extends BaseFragment implements OnClickListener {
    private EditText login, password, domain;
    private Button apply;


    // newInstance constructor for creating fragment with arguments
    public static GenericLoginFragment newInstance(int page, String title) {
        GenericLoginFragment fragmentFirst = new GenericLoginFragment();
        Bundle args = new Bundle();
        args.putInt("position", page);
        args.putString("title", title);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_generic_login, container, false);

        login = (EditText) view.findViewById(R.id.setup_username);
        password = (EditText) view.findViewById(R.id.setup_password);
        domain = (EditText) view.findViewById(R.id.setup_domain);
        apply = (Button) view.findViewById(R.id.setup_apply);
        apply.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.setup_apply) {
//			if (login.getText() == null || login.length() == 0 || password.getText() == null || password.length() == 0 || domain.getText() == null || domain.length() == 0) {
//				Toast.makeText(getActivity(), getString(R.string.first_launch_no_login_password), Toast.LENGTH_LONG).show();
//				return;
//			}
            if (login.getText().toString().trim().length() == 0) {
                Toast.makeText(getActivity(), "Username can't be empty.", Toast.LENGTH_SHORT).show();
            } else if (password.getText().toString().trim().length() == 0) {
                Toast.makeText(getActivity(), "Password can't be empty.", Toast.LENGTH_SHORT).show();
            }
            else if (domain.getText().toString().trim().length() == 0) {
                Toast.makeText(getActivity(), "Domain can't be empty.", Toast.LENGTH_SHORT).show();
            } else {
                SetupActivity.instance().genericLogIn(login.getText().toString(), password.getText().toString(), domain.getText().toString());
            }
        }
    }


}
