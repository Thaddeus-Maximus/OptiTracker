package thadhughes.org.optitrack;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class MonitorActivity extends ActionBarActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    public String TAG = "OptiTrack";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_SETTINGS = 3;

    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;

    public static final String TOAST = "toast";

    private boolean NO_BT = false;
    private int mState = NXTTalker.STATE_NONE;
    private int mSavedState = NXTTalker.STATE_NONE;
    private boolean mNewLaunch = true;
    private String mDeviceAddress = null;

    private BluetoothAdapter mBluetoothAdapter;
    private NXTTalker mNXTTalker;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG,"OpenCV Manager Connected");
                    //from now onwards, you can use OpenCV API
                    Mat m = new Mat(5, 10, CvType.CV_8UC1, new Scalar(0));

                    break;
                case LoaderCallbackInterface.INIT_FAILED:
                    Log.i(TAG,"Init Failed");
                    break;
                case LoaderCallbackInterface.INSTALL_CANCELED:
                    Log.i(TAG,"Install Cancelled");
                    break;
                case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION:
                    Log.i(TAG,"Incompatible Version");
                    break;
                case LoaderCallbackInterface.MARKET_ERROR:
                    Log.i(TAG,"Market Error");
                    break;
                default:
                    Log.i(TAG,"OpenCV Manager Install");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //initialize OpenCV manager
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_monitor);

        if (mIsJavaCamera)
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        else
            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.setMaxFrameSize(240, 240);//(960,540);

        mOpenCvCameraView.enableView();

        if (savedInstanceState != null) {
            mNewLaunch = false;
            mDeviceAddress = savedInstanceState.getString("device_address");
            if (mDeviceAddress != null) {
                mSavedState = NXTTalker.STATE_CONNECTED;
            }
        }

        if (!NO_BT) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        mNXTTalker = new NXTTalker(mHandler);

        if (!NO_BT) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                if (mSavedState == NXTTalker.STATE_CONNECTED) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    mNXTTalker.connect(device);
                } else {
                    if (mNewLaunch) {
                        mNewLaunch = false;
                        findBrick();
                        mNXTTalker.messageWrite(0,"100");
                    }
                }
            }
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_STATE_CHANGE:

                    break;
            }
        }
    };

    public void onStart() {
        super.onStart();

    }

    private void findBrick() {
        Intent intent = new Intent(this, ChooseDeviceActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    findBrick();
                } else {
                    Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    //Toast.makeText(this, address, Toast.LENGTH_LONG).show();
                    mDeviceAddress = address;
                    mNXTTalker.connect(device);
                }
                break;
            case REQUEST_SETTINGS:
                //XXX?
                break;
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemSwitchCamera = menu.add("Toggle Native/Java camera");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String toastMesage = new String();
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemSwitchCamera) {
            mOpenCvCameraView.setVisibility(SurfaceView.GONE);
            mIsJavaCamera = !mIsJavaCamera;

            if (mIsJavaCamera) {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
                toastMesage = "Java Camera";
            } else {
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_native_surface_view);
                toastMesage = "Native Camera";
            }

            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
            mOpenCvCameraView.setCvCameraViewListener(this);
            mOpenCvCameraView.setMaxFrameSize(240, 240);
            mOpenCvCameraView.enableView();

            Toast toast = Toast.makeText(this, toastMesage, Toast.LENGTH_LONG);
            toast.show();
        }

        return true;
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    Scalar CONTOUR_COLOR = new Scalar(0,200,255);

    public class LineDataPoint {
        LineDataPoint(double x, double y, double width) {
            this.x=x; this.y=y; this.width=width;
        }
        double x, y;
        double width;
    }

    public int scaleRelativeToAbsolute(double val, int pixels) {
        int output = (int) (pixels * (val+1)/2);
        if (output >= pixels) return pixels - 1;
        if (output < 0) return 0;
        return output;
    }

    public double scaleAbsoluteToRelative(double val, int pixels) {
        double output = val/pixels*2-1;
        if (output > 1) return 1;
        if (output < -1) return -1;
        return output;
    }

    /*
     * @param img the image to process
     * @param overlay the image to add an overlay to
     * @param dataPoints a set of LineDataPoints to add a single LineDataPoint to. Will add null if no line is found during this instance.
     * @param startHeightRel the height to start scanning at: scaled from -1 to 1 (-1 is bottom, +1 is top)
     * @param interval the amount to space each scan point by, scaled from -1 to 1.
     * @param times the number of points to check for in total
     */
    public Mat targetLine(Mat img, Mat overlay, ArrayList<LineDataPoint> dataPoints, double startHeightRel, double interval, int times) {
        if (img==null) {
            Log.e(TAG, "img=null!");
            return null;
        }
        try {
            // We'll do up to three scans, putting data about the first successful one into the @param dataPoints array.
            // If no scan, we'll end up putting null in the @param dataPoints array.
            final int MAX_ADDITIONAL =2;
            for (int additional=0;additional<=MAX_ADDITIONAL;additional++) {
                // Compute the starting height in pixels, make sure that it is in bounds.
                int startHeightAbs = scaleRelativeToAbsolute(-startHeightRel, img.rows()) + additional;
                if (startHeightAbs>=img.rows()-2) startHeightAbs = img.rows()-3;

                // This will be the array we will store derivative values in.
                // final int[] derivativeMap = new int[img.cols()];
                // Actually, we only really need two value, so let's just declare those.
                double maxDerivative=0, minDerivative=0;
                int targetPixel = img.cols() / 2;
                int maxIndex = 0;
                int minIndex = 0;
                int lastDataPointIndex = dataPoints.size() - 1;
                int skippedDataPoints = 0;
                boolean hasFoundAPoint = false;
                while (lastDataPointIndex >= 0) {
                    if (dataPoints.get(lastDataPointIndex) != null) {
                        targetPixel = scaleRelativeToAbsolute(dataPoints.get(lastDataPointIndex).x, img.cols());
                        hasFoundAPoint = true;
                        break;
                    }
                    lastDataPointIndex--;
                    skippedDataPoints++;
                }

                // If we haven't found a point yet, we'll do a simple, primitive scan through the entire line of pixels.
                if (!hasFoundAPoint) {
                    for (int i = 1; i < img.cols() - 1; i++) {
                        // Derivative is merely the current pixel value minus the last pixel value.
                        int derivative = (int) img.get(startHeightAbs, i)[0] - (int) img.get(startHeightAbs, i - 1)[0];

                        // If it's positive and bigger than the previous largest value, replace it.
                        if (derivative > 17 && derivative > maxDerivative) {
                            maxIndex = i;
                            maxDerivative = derivative;
                        }
                        // If it's smaller and more negative than the previous min value, replace it.
                        if (derivative < 17 && derivative < minDerivative) {
                            minIndex = i;
                            minDerivative = derivative;
                        }
                    }
                // Otherwise, do a smarter scan focused around the last point. This should be faster, and produce better results.
                } else {
                    // starting index should be a sixth screen width to the left of the last point
                    int i = targetPixel - (int)(img.cols()/6);
                    if (i < 1) i = 1;
                    if (i >= img.cols() - 1) i = img.cols() - 2;

                    // ending index should be a sixth screen width to the right of the last point
                    int upperBound = targetPixel + (int)(img.cols()/6);
                    if (upperBound < 1) upperBound = 1;
                    if (upperBound >= img.cols() - 1) upperBound = img.cols() - 2;
                    for (; i < upperBound; i++) {

                        // Same idea as the last one...
                        int derivative = (int) img.get(startHeightAbs, i)[0] - (int) img.get(startHeightAbs, i - 1)[0];

                        // If the derivative is significant, a little smaller or bigger than the previous max derivative, and closer to the last point than the previous max derivative, store that data.
                        if (derivative > 17 && derivative > maxDerivative - 17 && Math.abs(targetPixel - i) < Math.abs(targetPixel - maxIndex)) {
                            maxIndex = i;
                            maxDerivative = derivative;
                        }
                        // Same thing but negative.
                        if (derivative < -17 && derivative < minDerivative + 17 && Math.abs(targetPixel - i) < Math.abs(targetPixel - minIndex)) {
                            minIndex = i;
                            minDerivative = derivative;
                        }
                    }
                }
                // If we actually did find a max and min, and they correspond to a black line on a white plane (not white line on black plane)
                if (maxIndex > 0 && minIndex > 0 && maxIndex>minIndex) {
                    // Do some more checks, starting by computing relative positions of the line.
                    LineDataPoint newDataPoint = new LineDataPoint(
                            scaleAbsoluteToRelative((maxIndex + minIndex) / 2, img.cols()),
                            startHeightRel,
                            Math.abs((double) maxIndex - (double) minIndex) / img.cols()
                    );

                    // If we've found a point before, we can do some better checks.
                    if (hasFoundAPoint) {
                        // Compare the size ratio to the last point
                        double sizeRatio = (dataPoints.get(lastDataPointIndex).width / newDataPoint.width);
                        // Make sure the ratio between it and the last point is between 1.17 and 0.97. These values grow exponentially with the number of points skipped.
                        if (sizeRatio < Math.pow(1.17, (skippedDataPoints + 1)) && sizeRatio > Math.pow(0.7, (skippedDataPoints + 1))) {
                            // Draw a circle with a center on the output image
                            Core.circle(overlay, new Point(scaleRelativeToAbsolute(newDataPoint.x, img.cols()), scaleRelativeToAbsolute(-newDataPoint.y, img.rows())), 2, new Scalar(0, 255, 0), 3);
                            Core.circle(overlay, new Point(scaleRelativeToAbsolute(newDataPoint.x, img.cols()), scaleRelativeToAbsolute(-newDataPoint.y, img.rows())), (int) (newDataPoint.width * img.cols() / 2), new Scalar(0, 200, 255), 3);
                            // Add to the @param dataPoint array.
                            dataPoints.add(newDataPoint);
                            // Stop looping; no sense in taking more processing power than needed.
                            break;
                        // If we didn't pass that ratio test and it's the last run through, give up and add a null point to the data set.
                        } else if (additional == MAX_ADDITIONAL) {
                            dataPoints.add(null);
                        }
                    // If we haven't found a point before, we'll do a simple size comparison.
                    } else {
                        // If the size of the point is between 0.08 and 0.17 of half a screen, draw, log, and stop.
                        if (newDataPoint.width < 0.165 && newDataPoint.width > 0.04) {
                            Core.circle(overlay, new Point(scaleRelativeToAbsolute(newDataPoint.x, img.cols()), scaleRelativeToAbsolute(-newDataPoint.y, img.rows())), 2, new Scalar(0, 255, 0), 3);
                            Core.circle(overlay, new Point(scaleRelativeToAbsolute(newDataPoint.x, img.cols()), scaleRelativeToAbsolute(-newDataPoint.y, img.rows())), (int) (newDataPoint.width * img.cols() / 2), new Scalar(0, 200, 255), 3);
                            dataPoints.add(newDataPoint);
                            break;
                        // If we didn't pass that size test and it's the last run through, give up and add a null point to the data set.
                        } else if (additional == MAX_ADDITIONAL) {
                            dataPoints.add(null);
                        }
                    }
                    // If we didn't pass the order test and it's the last run through, give up and add a null point to the data set.
                } else if (additional == MAX_ADDITIONAL) {
                    dataPoints.add(null);
                }
            }

            // If it is the last run through, just return the overlay. Otherwise make another call.
                if (times <= 1)
                    return overlay;
                else
                    return targetLine(img, overlay, dataPoints, startHeightRel + interval, interval, times - 1);

        } catch(Exception e) {
            Log.e(TAG, "Error in targetLine", e);
            return null;
        }
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat img = inputFrame.rgba();
        Mat overlay = img.clone();
        ArrayList<LineDataPoint> dataPoints = new ArrayList<LineDataPoint>();
        targetLine(img, overlay, dataPoints, -0.9, 0.075, 24);

        String closeError="ERR", overallError="ERR", confidence="ERR", overallDerivative="ERR", overallSecondDerivative="ERR";
        int runsThrough = 0;

        long accumulatedError = 0;
        int liveDataPoints = 0;
        long lastError = 0;
        int skippedDataPoints = 0;
        double accumulatedDerivative = 0;
        double lastDerivative = 0;
        double accumulatedSecondDerivative = 0;

        int index=0; while(index<dataPoints.size()) {
            if (dataPoints.get(index) != null) {
                liveDataPoints++;
                long scaledVal = (long) (dataPoints.get(index).x * 1000);
                long currentDerivative = 0;
                if (runsThrough==0) {

                    closeError = Long.toString(scaledVal);
                } else {
                    currentDerivative = (scaledVal - lastError)/(skippedDataPoints+1);
                    accumulatedDerivative += currentDerivative;
                }
                if (runsThrough>1) {
                    accumulatedSecondDerivative += (currentDerivative - lastDerivative)/(skippedDataPoints+1);
                }

                lastError = scaledVal;
                lastDerivative = currentDerivative;
                accumulatedError += scaledVal;
                skippedDataPoints=0;
                runsThrough++;
            } else {
                skippedDataPoints++;
            }
            index++;
        }
        try {
            overallError = Long.toString(accumulatedError / liveDataPoints);
        }catch(Exception e) {}
        try {
            confidence = Long.toString((liveDataPoints * 1000) / dataPoints.size());
        }catch(Exception e){}
        try {
            overallDerivative = Long.toString((long)(accumulatedDerivative*1000) / dataPoints.size());
        }catch(Exception e){}
        try {
            overallSecondDerivative = Long.toString((long)(accumulatedSecondDerivative*1000) / dataPoints.size());
        }catch(Exception e){}


        mNXTTalker.messageWrite(0, confidence);
        mNXTTalker.messageWrite(1, closeError);
        mNXTTalker.messageWrite(2, overallError);
        mNXTTalker.messageWrite(3, overallDerivative);
        mNXTTalker.messageWrite(4, overallSecondDerivative);

        Log.i("VisionAnalyzer", "Confidence: "+confidence+", Close Error: "+closeError+" Overall Error: "+overallError+" Derivative: "+overallDerivative+" 2nd Derivative: "+overallSecondDerivative);

        return overlay;
    }
}
