package thadhughes.org.optitrack;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

/**
 * Created by Thaddeus on 7/5/2015.
 */
public class FlashlightJavaCameraView extends JavaCameraView {
    public FlashlightJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //turnFlashlightOn();
    }

    public void turnFlashlightOn() {
        /*Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(p);
        mCamera.startPreview();*/
    }
}
