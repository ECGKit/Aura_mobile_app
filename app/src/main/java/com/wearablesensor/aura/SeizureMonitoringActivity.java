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

package com.wearablesensor.aura;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.wearablesensor.aura.data_sync.DataSyncFragment;
import com.wearablesensor.aura.data_sync.DataSyncPresenter;
import com.wearablesensor.aura.data_visualisation.DataVisualisationPresenter;
import com.wearablesensor.aura.data_visualisation.PhysioSignalVisualisationFragment;
import com.wearablesensor.aura.device_pairing_details.DevicePairingDetailsFragment;
import com.wearablesensor.aura.device_pairing_details.DevicePairingDetailsPresenter;
import com.wearablesensor.aura.seizure_report.SeizureReportFragment;
import com.wearablesensor.aura.seizure_report.SeizureReportPresenter;
import com.wearablesensor.aura.seizure_report.SeizureStatusFragment;
import com.wearablesensor.aura.seizure_report.SeizureStatusPresenter;
import com.wearablesensor.aura.user_session.UserModel;
import com.wearablesensor.aura.user_session.UserSessionService;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class SeizureMonitoringActivity extends AppCompatActivity implements DevicePairingDetailsFragment.OnFragmentInteractionListener, DataSyncFragment.OnFragmentInteractionListener, PhysioSignalVisualisationFragment.OnFragmentInteractionListener, SeizureStatusFragment.OnFragmentInteractionListener, SeizureReportFragment.OnFragmentInteractionListener{

    private final static String TAG = SeizureMonitoringActivity.class.getSimpleName();
    private String[] mDrawerTitles;
    private ActionBarDrawerToggle mDrawerToggle;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.nav_view) NavigationView mNavigationView;
    @BindView(R.id.drawer_menu_button) ImageButton mDrawerImageButton;
    @OnClick(R.id.drawer_menu_button)
    public void openDrawerMenu(){
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }

    private DevicePairingDetailsPresenter mDevicePairingDetailsPresenter;
    private DevicePairingDetailsFragment mDevicePairingFragment;

    private DataSyncPresenter mDataSyncPresenter;
    private DataSyncFragment mDataSyncFragment;

    private DataVisualisationPresenter mDataVisualisationPresenter;
    private PhysioSignalVisualisationFragment mPhysioSignalVisualisationFragment;

    private SeizureStatusFragment mSeizureStatusFragment;
    private SeizureStatusPresenter mSeizureStatusPresenter;

    private SeizureReportFragment mSeizureReportFragment;
    private SeizureReportPresenter mSeizureReportPresenter;

    private static final int REQUEST_ENABLE_BT = 1;

    private DataCollectorService mDataCollectorService;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            mDataCollectorService = ((DataCollectorService.LocalBinder)service).getService();

            mDevicePairingDetailsPresenter.setDevicePairingService(mDataCollectorService.getDevicePairingService());
            mDevicePairingDetailsPresenter.start();

            mDataSyncPresenter.setDataSyncService(mDataCollectorService.getDataSyncService());
            mDataSyncPresenter.start();
        }

        public void onServiceDisconnected(ComponentName className) {
            mDataCollectorService = null;
        }
    };
    private Boolean mIsDataCollectorBound = false;

    void doBindService() {

        bindService(new Intent(getApplicationContext(), DataCollectorService.class),
                mConnection,
                Context.BIND_AUTO_CREATE);
        mIsDataCollectorBound = true;
    }

    void doUnbindService() {
        if (mIsDataCollectorBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsDataCollectorBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_seizure_monitoring);

        loadUser();

        Crashlytics.setUserIdentifier(((AuraApplication) getApplication()).getUserSessionService().getUser().getUuid());

        mDevicePairingFragment = new DevicePairingDetailsFragment();
        mDevicePairingDetailsPresenter = new DevicePairingDetailsPresenter(( (mDataCollectorService != null) ? mDataCollectorService.getDevicePairingService():null), mDevicePairingFragment);

        mDataSyncFragment = new DataSyncFragment();
        mDataSyncPresenter = new DataSyncPresenter(getApplicationContext(),( (mDataCollectorService != null) ?  mDataCollectorService.getDataSyncService():null), mDataSyncFragment);

        mPhysioSignalVisualisationFragment = new PhysioSignalVisualisationFragment();
        mDataVisualisationPresenter = new DataVisualisationPresenter(mPhysioSignalVisualisationFragment);

        mSeizureReportFragment = new SeizureReportFragment();
        mSeizureReportPresenter = new SeizureReportPresenter(mSeizureReportFragment, this, ((AuraApplication) getApplication()).getLocalDataRepository(), ((AuraApplication) getApplication()).getUserSessionService());

        mSeizureStatusFragment = new SeizureStatusFragment();
        mSeizureStatusPresenter = new SeizureStatusPresenter(mSeizureStatusFragment, mSeizureReportFragment, this);

        displayFragments();

        ButterKnife.bind(this);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0) {

        /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
           }

            /** Called when a drawer has settled in a completely open state. */

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView.setCheckedItem(R.id.nav_SuiviContinu);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_SuiviContinu:
                        break;
                    case R.id.nav_LeaveApp:
                        quitApplication();
                        break;
                }
                return true;
            }

        });
        //wait the fragment to be fully displayed before starting automatic pairing
        startDataCollector();
    }

    private void quitApplication() {
        stopDataCollector();

        finish();
    }

    private void stopDataCollector() {
        doUnbindService();

        Intent stopIntent = new Intent(SeizureMonitoringActivity.this, DataCollectorService.class);
        stopIntent.setAction(DataCollectorServiceConstants.ACTION.STOPFOREGROUND_ACTION);
        stopService(stopIntent);
    }

    private void startDataCollector(){
        // no running Aura Data Collector service
        if(!isMyServiceRunning(DataCollectorService.class)){
            Intent startIntent = new Intent(SeizureMonitoringActivity.this, DataCollectorService.class);
            startIntent.putExtra("UserUUID", ((AuraApplication) getApplication()).getUserSessionService().getUser().getUuid());
            startIntent.setAction(DataCollectorServiceConstants.ACTION.STARTFOREGROUND_ACTION);
            startService(startIntent);

            doBindService();
        }
        // running Aura Data Collector service but not binded to Activity
        else if(isMyServiceRunning(DataCollectorService.class) && mDataCollectorService == null){
            doBindService();
        }
    }

    private void loadUser() {
        SharedPreferences lSharedPref = getSharedPreferences(UserSessionService.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String lUserUUID = lSharedPref.getString(UserSessionService.SHARED_PREFS_USER_UUID, null);
        String lUserAmazonId = lSharedPref.getString(UserSessionService.SHARED_PREFS_USER_AMAZON_ID, "");
        String lUserAlias = lSharedPref.getString(UserSessionService.SHARED_PREFS_USER_ALIAS,"");

        if(lUserUUID != null) {
            ((AuraApplication) getApplication()).getUserSessionService().setUser(new UserModel(lUserUUID, lUserAmazonId, lUserAlias));
        }
    }

    @Override
    public void onStart(){
        super.onStart();

    }

    @Override
    public void onStop(){
        try {
            ((AuraApplication) getApplication()).getLocalDataRepository().forceSavingPhysioSignalSamples();

        }
        catch(Exception e){
            Log.d(TAG, "Fail to save cache data on exit");
        }

        super.onStop();
    }

    @Override
    public void onBackPressed() {
        //disable leaving activity on back button pressed
    }



    private boolean isMyServiceRunning(Class<?> iServiceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (iServiceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void displayFragments(){
        FragmentTransaction lTransaction = getSupportFragmentManager().beginTransaction();

        lTransaction.add(R.id.content_frame, mDevicePairingFragment, DevicePairingDetailsFragment.class.getSimpleName());
        lTransaction.add(R.id.content_frame, mPhysioSignalVisualisationFragment, PhysioSignalVisualisationFragment.class.getSimpleName() );
        lTransaction.add(R.id.content_frame, mDataSyncFragment , DataSyncFragment.class.getSimpleName());
        lTransaction.add(R.id.content_frame, mSeizureStatusFragment, SeizureStatusFragment.class.getSimpleName());
        lTransaction.addToBackStack(null);

        // Commit the transaction
        lTransaction.commit();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDataSyncFragmentInteraction(Uri uri) {

    }

    @Override
    public void onDevicePairingAttempt() {
        Intent intent = new Intent(this, DeviceScanActivity.class);
        startActivity(intent);

        this.finish();
    }

    @Override
    public void onHRVRealTimeDisplayFragmentInteraction(Uri uri) {

    }

    @Override
    public void onSeizureStatusFragmentInteraction(Uri uri) {

    }


    @Override
    public void onSeizureReportFragmentInteraction(Uri uri) {

    }

    @Override
    public void onDestroy(){
        doUnbindService();
        super.onDestroy();
    }

}
