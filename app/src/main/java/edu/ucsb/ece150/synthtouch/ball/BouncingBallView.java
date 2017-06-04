/*
 *  Copyright (c) 2017 - present, Xuan Wang
 *  All rights reserved.
 *
 *  This source code is licensed under the BSD-style license found in the
 *  LICENSE file in the root directory of this source tree.
 *
 */

package edu.ucsb.ece150.synthtouch.ball;

import android.support.v7.widget.AppCompatImageView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.widget.AppCompatImageView;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class BouncingBallView extends AppCompatImageView {
    private Ball ball;
    private BoundaryBox box;
    private boolean fingerDown;

    public boolean isFingerDown() {
        return fingerDown;
    }

    // For touch inputs - previous touch (x, y)
    private float previousX;
    private float previousY;

    public void setAccel(float accelX, float accelY) {
        ball.setAccelX(accelX);
        ball.setAccelY(accelY);
    }

    public void setBallPosition(float x, float y){
        ball.x = x;
        ball.y = y;
    }

    public float[] getBallPosition(){
        return new float[]{ball.x, ball.y};
    }

    public void setBallSpeed(float x, float y){
        ball.speedX = x;
        ball.speedY = y;
    }

    public float[] getBallSpeed(){
        return new float[]{ball.speedX, ball.speedY};
    }

    public BouncingBallView(Context context) {
        super(context);
        // TODO: change colors
        box = new BoundaryBox(android.R.color.transparent);  // ARGB
        ball = new Ball(Color.YELLOW);
        fingerDown = false;

        this.setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        box.draw(canvas);
        ball.draw(canvas);

        ball.moveWithCollisionDetection(box);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        box.set(0, 0, w, h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currentX = event.getX();
        float currentY = event.getY();
        float deltaX, deltaY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                fingerDown = true;
                ball.x = currentX;
                ball.y = currentY;
                ball.speedX = 0.0f;
                ball.speedY = 0.0f;
                ball.setAccelX(0.0f);
                ball.setAccelY(0.0f);
                break;

            case MotionEvent.ACTION_MOVE:
                // Modify rotational angles according to movement
                deltaX = currentX - previousX;
                deltaY = currentY - previousY;
                ball.x = currentX;
                ball.y = currentY;
                ball.speedX = deltaX;
                ball.speedY = deltaY;
                ball.setAccelX(0.0f);
                ball.setAccelY(0.0f);
                break;
            case MotionEvent.ACTION_UP:
                fingerDown = false;
                break;
        }

        previousX = currentX;
        previousY = currentY;

        return true;
    }
}