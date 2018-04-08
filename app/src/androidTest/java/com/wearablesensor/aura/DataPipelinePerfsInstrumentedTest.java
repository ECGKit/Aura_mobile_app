package com.wearablesensor.aura;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.wearablesensor.aura.data_repository.DateIso8601Mapper;
import com.wearablesensor.aura.data_repository.LocalDataFileRepository;
import com.wearablesensor.aura.data_repository.RemoteDataInfluxDBRepository;
import com.wearablesensor.aura.data_repository.models.MotionAccelerometerModel;
import com.wearablesensor.aura.data_sync.DataSyncService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DataPipelinePerfsInstrumentedTest {

    private final String TAG = DataPipelinePerfsInstrumentedTest.class.getSimpleName();
    private String TEST_INFLUX_DB = "http://192.168.1.16:8086";

    private final int SMALL_NUMBER_OF_ENTRIES = 1000;
    private final int MEDIUM_NUMBER_OF_ENTRIES = 100000;
    private final int ONE_DAY_NUMBER_OF_ENTRIES = 24000000;

    private DataSyncService mDataSyncService;

    private Context mApplicationContext;
    private LocalDataFileRepository mLocalDataFileRepository;
    private RemoteDataInfluxDBRepository mRemoteDataRepository;

    private DataFileHelper mDataFileHelper;


    /**
     * @brief force the test to end only when all data files has been uploaded or when the timeout is reached
     * @param iTimeout in seconds
     * @param iDataFileHelper allows to check file system storage
     *
     * @return true if succeed, false if timeout
     * @throws InterruptedException
     */
    private boolean waitUntilAllFilesHasBeenUploaded(DataFileHelper iDataFileHelper, int iTimeout) throws InterruptedException {
        // wait until all files are transfered or timeout
        int lDuration = 0; /* in seconds */
        int lRemainingLocalDataFile = iDataFileHelper.getDataFiles().length;

        while(lRemainingLocalDataFile != 0){
            if(lDuration > iTimeout){
                return false;
            }

            Thread.sleep(1000);
            lRemainingLocalDataFile = iDataFileHelper.getDataFiles().length;
            lDuration++;
            Log.d(TAG, "Waiting - files remaining: " + lRemainingLocalDataFile );
        }

        return true;
    }

    @Before
    public void setUp()throws Exception{
        mApplicationContext = InstrumentationRegistry.getTargetContext();
        mDataFileHelper = new DataFileHelper();
        mDataFileHelper.cleanPrivateFiles();

        mLocalDataFileRepository = new LocalDataFileRepository(mApplicationContext);
        mRemoteDataRepository = new RemoteDataInfluxDBRepository();
        mRemoteDataRepository.connect(TEST_INFLUX_DB,"test", "test");

        mDataSyncService = new DataSyncService(mLocalDataFileRepository, mRemoteDataRepository, mApplicationContext);
    }

    @Ignore
    @Test
    public void cache_SmallNumberOfEntries() throws Exception {


        for (int i = 0; i < SMALL_NUMBER_OF_ENTRIES; i++) {
            String lPhysioUuid = UUID.randomUUID().toString();
            String lDeviceAdressUuid = UUID.randomUUID().toString();
            String lUserUuid = UUID.randomUUID().toString();

            final String lTimestamp = DateIso8601Mapper.getString(new Date());
            mLocalDataFileRepository.cachePhysioSignalSample(new MotionAccelerometerModel(lPhysioUuid, lDeviceAdressUuid, lUserUuid, lTimestamp, new float[]{0.5f, 1.0f, -0.5f}, "2G"));
        }

        Log.d(TAG, "cacheSmallNumberOfEntries - files:" + mDataFileHelper.getDataFiles().length);
        assertThat("Data not properly recorded", mDataFileHelper.getDataFiles().length == 9);

        mDataSyncService.startDataSync();
        boolean lUploadComplete = waitUntilAllFilesHasBeenUploaded(mDataFileHelper, 100);

        assertThat("Data upload timed out", lUploadComplete);
        assertThat("Data not properly uploaded", mDataFileHelper.getDataFiles().length == 0);
        mDataFileHelper.cleanPrivateFiles();
    }



    @Ignore
    @Test
    public void cache_MediumNumberOfEntries() throws Exception {

        for (int i = 0; i < MEDIUM_NUMBER_OF_ENTRIES; i++) {
            String lPhysioUuid = UUID.randomUUID().toString();
            String lDeviceAdressUuid = UUID.randomUUID().toString();
            String lUserUuid = UUID.randomUUID().toString();

            final String lTimestamp = DateIso8601Mapper.getString(new Date());
            mLocalDataFileRepository.cachePhysioSignalSample(new MotionAccelerometerModel(lPhysioUuid, lDeviceAdressUuid, lUserUuid, lTimestamp, new float[]{0.5f, 1.0f, -0.5f}, "2G"));
        }

        Log.d(TAG, "cacheMediumNumberOfEntries - files:" + mDataFileHelper.getDataFiles().length);
        assertThat("Data not properly recorded", mDataFileHelper.getDataFiles().length == 990);

        mDataSyncService.startDataSync();
        boolean lUploadComplete = waitUntilAllFilesHasBeenUploaded(mDataFileHelper, 600);

        assertThat("Data upload has timed out", lUploadComplete);
        assertThat("Data not properly uploaded", mDataFileHelper.getDataFiles().length == 0);
        mDataFileHelper.cleanPrivateFiles();
    }

    @Ignore
    @Test
    public void cache_HighNumberOfEntries() throws Exception {
        for (int i = 0; i < ONE_DAY_NUMBER_OF_ENTRIES; i++) {
            String lPhysioUuid = UUID.randomUUID().toString();
            String lDeviceAdressUuid = UUID.randomUUID().toString();
            String lUserUuid = UUID.randomUUID().toString();

            final String lTimestamp = DateIso8601Mapper.getString(new Date());
            mLocalDataFileRepository.cachePhysioSignalSample(new MotionAccelerometerModel(lPhysioUuid, lDeviceAdressUuid, lUserUuid, lTimestamp, new float[]{0.5f, 1.0f, -0.5f}, "2G"));
        }

        Log.d(TAG, "cacheHighNumberOfEntries - files:" + mDataFileHelper.getDataFiles().length);
        assertThat("Data not properly recorded", mDataFileHelper.getDataFiles().length == 23763);

        mDataSyncService.startDataSync();
        boolean lUploadComplete = waitUntilAllFilesHasBeenUploaded(mDataFileHelper, 600);

        assertThat("Data upload has timed out", lUploadComplete);
        assertThat("Data not properly uploaded", mDataFileHelper.getDataFiles().length == 0);
        mDataFileHelper.cleanPrivateFiles();
    }

    @After
    public void tearDown(){

    }
}