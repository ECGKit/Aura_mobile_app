/*
Aura Mobile Application
Copyright (C) 2017 Aura Healthcare

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/
*/

package com.wearablesensor.aura.authentification;

import com.wearablesensor.aura.utils.BasePresenter;
import com.wearablesensor.aura.utils.BaseView;

public class SignInContract {
    public interface View extends BaseView<Presenter> {
        void displayValidationError(String iErrorMessage);

        void displayAuthentificationProgressDialog();
        void closeAuthentificationProgressDialog();

        void enableLoginButton();
        void disableLoginButton();

        void displayFailLoginMessage();
    }

    public interface Presenter extends BasePresenter{
        void signIn(String iUsername, String iPassword);

        void signInSucceed();
        void signInFails();
        boolean validate(String iUsername, String iPassword);

        void firstSignIn();
        void continueWithFirstSignIn();
    }
}
