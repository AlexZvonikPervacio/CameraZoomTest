package altabel.pervacio.test.camerazoomtester.listener;

import altabel.pervacio.test.camerazoomtester.constant.ZoomNotSupportedReason;

/**
 * Created by alex on 14.9.16.
 */
public interface TestResultListener {

    void onZoomSupported();

    void onZoomNotSupported(ZoomNotSupportedReason reason);

}
