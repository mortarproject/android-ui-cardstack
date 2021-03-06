package org.mp.android.ui.cardstack;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;


//detect both tap and drag
public class DragGestureDetector {
    public static String DEBUG_TAG = "DragGestureDetector";
    GestureDetectorCompat mGestrueDetector;
    DragListener mListener;
    private boolean mStarted = false;
    private MotionEvent mOriginalEvent;

    public static interface DragListener {
        public boolean onDragStart(MotionEvent e1, MotionEvent e2, float distanceX,
                                   float distanceY);

        public boolean onDragContinue(MotionEvent e1, MotionEvent e2, float distanceX,
                                      float distanceY);

        public boolean onDragEnd(MotionEvent e1, MotionEvent e2);

        public boolean onTapUp();

        public void onLongPress();
    }

    public DragGestureDetector(Context context, DragListener myDragListener) {
        mGestrueDetector = new GestureDetectorCompat(context, new MyGestureListener());
        mListener = myDragListener;
    }

    public void onTouchEvent(MotionEvent event) {
        mGestrueDetector.onTouchEvent(event);
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case (MotionEvent.ACTION_UP):
                //Log.d(DEBUG_TAG, "Action was UP");
                if (mStarted) {
                    mListener.onDragEnd(mOriginalEvent, event);
                }
                mStarted = false;
                break;
            case (MotionEvent.ACTION_DOWN):
                //need to set this, quick tap will not generate drap event, so the
                //originalEvent may be null for case action_up
                //which lead to null pointer
                mOriginalEvent = event;
                break;
            case (MotionEvent.ACTION_CANCEL):
                Log.d(DEBUG_TAG, "Action was cancel!");
                if (mStarted) {
                    mListener.onDragEnd(mOriginalEvent, event);
                }
                mStarted = false;
                break;
        }
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                                float distanceY) {
            if (mListener == null) return true;
            if (mStarted == false) {
                mListener.onDragStart(e1, e2, distanceX, distanceY);
                mStarted = true;
            } else {
                mListener.onDragContinue(e1, e2, distanceX, distanceY);
            }
            mOriginalEvent = e1;
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mListener.onLongPress();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return mListener.onTapUp();
        }
    }


}
