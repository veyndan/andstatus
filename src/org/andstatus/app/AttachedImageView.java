/**
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.andstatus.app.support.android.v11.widget.MyImageView;
import org.andstatus.app.util.MyLog;

/**
 * The ImageView auto resizes to the width of the referenced view  
 * @author yvolk@yurivolkov.com
 */
public class AttachedImageView extends MyImageView {
    private View referencedView = null;
    private static final int MAX_HEIGH = 2500;

    public AttachedImageView(Context context) {
        super(context);
    }

    public AttachedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AttachedImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setReferencedView(View referencedViewIn) {
        referencedView = referencedViewIn;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (referencedView == null) {
            referencedView =  ((View)getParent()).findViewById(R.id.message_body);
        }
        if (referencedView == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        final String method = "onMeasure";
        int refWidthPixels = referencedView.getMeasuredWidth();
        int height = (int) Math.floor(refWidthPixels * getDrawableHeightToWidthRation());
        MyLog.v(this, method + "; refWidth=" + refWidthPixels + ", height=" + height + ", widthSpec=" + MeasureSpec.toString(widthMeasureSpec));
        int mode = MeasureSpec.EXACTLY;
        if (height == 0) {
            height = MAX_HEIGH;
            mode = MeasureSpec.AT_MOST;
        }
        if (height > 0.75 * getDisplayHeight()) {
            height = (int) Math.floor(0.75 * getDisplayHeight()) ;
        }
        getLayoutParams().height = height;
        int measuredWidth;
        int measuredHeight;
        if (android.os.Build.VERSION.SDK_INT > 14) {
            measuredWidth = MeasureSpec.makeMeasureSpec(refWidthPixels,  MeasureSpec.EXACTLY);
            measuredHeight = MeasureSpec.makeMeasureSpec(height, mode);
        } else {
            measuredWidth = myResolveSizeAndState(refWidthPixels, widthMeasureSpec, 0);
            measuredHeight = myResolveSizeAndState(height, heightMeasureSpec, 0);
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }
    
    private float getDrawableHeightToWidthRation() {
        float ratio = 9f / 19f;
        if (getDrawable() != null) {
            int width = getDrawable().getIntrinsicWidth();
            int height = getDrawable().getIntrinsicHeight();
            if (width > 0 && height > 0) {
                float ratio2 = 1f * height / width;
                if (ratio2 > ratio) {
                    ratio = ratio2;
                }
            }
        }
        return ratio;
    }
}