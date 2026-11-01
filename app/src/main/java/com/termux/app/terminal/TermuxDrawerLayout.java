package com.termux.app.terminal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;

import java.lang.reflect.Field;

/**
 * Custom DrawerLayout that prevents ViewDragHelper from logging "Ignoring pointerId=... because ACTION_DOWN was not received"
 * errors when orphan ACTION_MOVE events arrive without an initial ACTION_DOWN in the event stream.
 */
public class TermuxDrawerLayout extends DrawerLayout {

    private int mTrackedPointersDown = 0;
    private ViewDragHelper mLeftDragger;
    private ViewDragHelper mRightDragger;
    private boolean mDraggersInitialized = false;

    public TermuxDrawerLayout(@NonNull Context context) {
        super(context);
    }

    public TermuxDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TermuxDrawerLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void ensureDraggersInitialized() {
        if (!mDraggersInitialized) {
            mDraggersInitialized = true;
            try {
                Field fLeft = DrawerLayout.class.getDeclaredField("mLeftDragger");
                fLeft.setAccessible(true);
                mLeftDragger = (ViewDragHelper) fLeft.get(this);

                Field fRight = DrawerLayout.class.getDeclaredField("mRightDragger");
                fRight.setAccessible(true);
                mRightDragger = (ViewDragHelper) fRight.get(this);
            } catch (Throwable ignored) {
            }
        }
    }

    private void updateTrackedPointers(MotionEvent ev, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mTrackedPointersDown = 1 << ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = ev.getActionIndex();
                if (index >= 0 && index < ev.getPointerCount()) {
                    mTrackedPointersDown |= 1 << ev.getPointerId(index);
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int index = ev.getActionIndex();
                if (index >= 0 && index < ev.getPointerCount()) {
                    mTrackedPointersDown &= ~(1 << ev.getPointerId(index));
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mTrackedPointersDown = 0;
                break;
            }
        }
    }

    private boolean isPointerTracked(int pointerId) {
        ensureDraggersInitialized();
        if (mLeftDragger != null && mRightDragger != null) {
            return mLeftDragger.isPointerDown(pointerId) || mRightDragger.isPointerDown(pointerId);
        }
        return (mTrackedPointersDown & (1 << pointerId)) != 0;
    }

    private boolean areAnyPointersTracked(MotionEvent ev) {
        final int count = ev.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (isPointerTracked(ev.getPointerId(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        updateTrackedPointers(ev, action);

        // If an ACTION_MOVE arrives but neither pointer was registered as down,
        // drop it immediately to avoid ViewDragHelper errors in the event stream.
        if (action == MotionEvent.ACTION_MOVE && !areAnyPointersTracked(ev)) {
            return false;
        }

        try {
            return super.dispatchTouchEvent(ev);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        updateTrackedPointers(ev, action);

        if (action == MotionEvent.ACTION_MOVE && !areAnyPointersTracked(ev)) {
            return false;
        }

        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        updateTrackedPointers(ev, action);

        if (action == MotionEvent.ACTION_MOVE && !areAnyPointersTracked(ev)) {
            return false;
        }

        try {
            return super.onTouchEvent(ev);
        } catch (Throwable t) {
            return false;
        }
    }
}
