/*
 * Android Flick Button
 * 
 * Copyright (c) 2011 Andrew Mort
 * 
 * Licensed under The MIT License (MIT)
 * http://www.opensource.org/licenses/mit-license.php
 */

package benegon.android.widget;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A button that can be either flicked or tapped to change states.
 * 
 * (Placing this view inside of a table layout with its column set to stretch
 * will ensure this view resizes correctly)
 * 
 * Future revisions of this code hope to create a view that extends compound button
 * and does not require the extra internal views.
 * 
 * Author: Andrew Mort
 * Company: Benegon Enterprises LLC (www.benegon.com)
 * Version: 1.0-rc1
 * Date: 08/17/2011
 * 
 */
public class FlickButtonView extends FrameLayout implements Checkable{
	// Views that are contained inside the flickbutton to get it to appear and work like it does
	private HorizontalScrollView horizontalScrollView;
	private TextView onTextView, offTextView;
	private TextView thumb;
	
	private boolean enabled;
	private boolean state;
	
	
	private PointF lastTouch;
	
	// Used for log messages to determine which flick button is logging
	private static int count; 
	private int myCount;
	
	private OnFlickChangeListener onFlickChangeListener;
	
	// Max distance a finger can move between down and up to count as a tap
	private final static float TAP_X_DISTANCE = 1;
	private final static float TAP_Y_DISTANCE = 1;
	
	// Value to multiply by height to find the minimum width of each textview
	// Ratio of width to height in original pictures to keep similar ratio of dimensions
	private final static double MIN_WIDTH_TEXT_MULTIPLIER = 57/29.;
	
	// Value to multiply by textview width to find the width of the thumb
	// Ratio of thumb width to text width in original pictures so that the thumb does not look small when text is long
	private final static double THUMB_WIDTH_MULTIPLIER = 40/57.;
	
	// The minimum height of the view
	private final static int MIN_HEIGHT = 50;
	
	// The padding around the view
	private final static int PADDING = 5;
	
	// The ratio in the scroll view the thumb must move before the view switches states
	private final static double FLICK_RATIO = 1/5.;
	
	// Locations of where the view should scroll to appear on and off
	private int onScrollX, offScrollX;
	
	// Maximum scroll length
	private int maxScrollX;
	
	// Debugging output
	private final static boolean D = true;
	
	/**
	 * Constructor 
	 * @param context
	 */
	public FlickButtonView(Context context){
		this(context, null);
	}
	
	public FlickButtonView(Context context, AttributeSet attrs){
		this(context, attrs, 0);
	}
	
	public FlickButtonView(Context context, AttributeSet attrs, int defStyle){
		super(context, attrs, defStyle);
		
		myCount = count++;
		
		if(D) Log.d("FlickButtonView", "Constructor " + myCount);
		
		horizontalScrollView = new HorizontalScrollView(context);
		LinearLayout linearLayout = new LinearLayout(context);
		onTextView = new TextView(context);
		offTextView = new TextView(context);
		thumb = new TextView(context);
		
		addView(horizontalScrollView);
		horizontalScrollView.addView(linearLayout);
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.addView(onTextView);
		linearLayout.addView(thumb);
		linearLayout.addView(offTextView);
		
		// Disable the horizontal scroll bar and fading edge
		horizontalScrollView.setHorizontalScrollBarEnabled(false);
		horizontalScrollView.setFadingEdgeLength(0);
		
		// Set backgrounds to the button images
		onTextView.setBackgroundResource(R.drawable.switch_on_selector);
		offTextView.setBackgroundResource(R.drawable.switch_off_selector);
		thumb.setBackgroundResource(R.drawable.switch_thumb_selector);
		
		ViewGroup.LayoutParams onLayoutParams = onTextView.getLayoutParams();
		ViewGroup.LayoutParams offLayoutParams = offTextView.getLayoutParams();
		
		// Set the height and width to wrap content so that they will expand to fit all of their text
		onLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		onLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
				
		offLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		offLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		
		lastTouch = new PointF();
		
		// Set gravity to center so text is center aligned
		onTextView.setGravity(Gravity.CENTER);
		offTextView.setGravity(Gravity.CENTER);
		
		onTextView.setText("On");
		offTextView.setText("Off");
		
		state = true;
		setEnabled(true);
		
		onTextView.setTextAppearance(context, R.style.default_on_appearance);
		offTextView.setTextAppearance(context, R.style.default_off_appearance);
		
		// Must be defined so it is the same size as the other text views
		thumb.setTextAppearance(context, R.style.default_off_appearance);
		
		setPadding(PADDING, PADDING, PADDING, PADDING);
	}
	
	/**
	 * Set the text that is displayed when the button is in the on state
	 * @param text text 
	 */
	public void setTextOn(String text){
		onTextView.setText(text);
		
		//updateViewDimensions();
	}
	
	/**
	 * Set the text that is displayed when the button is in the off state
	 * @param text text 
	 */
	public void setTextOff(String text){
		offTextView.setText(text);
		
		//updateViewDimensions();
	}
	
	/**
	 * Set the default text size to the given value, interpreted as "scaled pixel" units. This size is adjusted based on the current density and user font size preference.
	 * @param size the scaled pixel size
	 */
	public void setTextSize(float size){
		onTextView.setTextSize(size);
		offTextView.setTextSize(size);
		thumb.setTextSize(size);
	}
	
	/**
	 * Set the default text size to a given unit and value.  See {@link
	 * TypedValue} for the possible dimension units.
	 * @param unit The desired dimension unit.
	 * @param size The desired size in the given units
	 * @attr ref android.R.styleable#TextView_textSize
	 */
	public void setTextSize(int unit, float size) {
		onTextView.setTextSize(unit, size);
		offTextView.setTextSize(unit, size);
		thumb.setTextSize(unit, size);
	}

	@Override
	public void setChecked(boolean checked){
		setChecked(checked, false);
	}
	
	
	public void setChecked(boolean checked, boolean smoothScroll){
		if(D) Log.d("FlickButtonView", "setChecked: " + checked);
		
		if(state != checked){
			state = checked;
			
			if(onFlickChangeListener != null){
				onFlickChangeListener.onFlickChanged(this, checked);
			}
		}
		
		refreshScroll(smoothScroll);
	}
	
	@Override
	public void setEnabled(boolean enabled){
		this.enabled = enabled;
		
		super.setEnabled(enabled);
		horizontalScrollView.setEnabled(enabled);
		onTextView.setEnabled(enabled);
		offTextView.setEnabled(enabled);
		thumb.setEnabled(enabled);
	}

	/**
	 * Register a callback to be invoked when the state of this button changes.
	 * @param onFlickChangeListener the callback to call on checked state change
	 */
	public void setOnFlickChangeListener(OnFlickChangeListener onFlickChangeListener){
		this.onFlickChangeListener = onFlickChangeListener;
	}
	
	/**
	 * Returns the currently displayed text on the button
	 * @return text on the button
	 */
	public CharSequence getText(){
		return state ? onTextView.getText() : offTextView.getText();
	}
	
	@Override
	public boolean isChecked(){
		return state;
	}
	
	@Override
	public boolean isEnabled(){
		return enabled;
	}
	
	
	@Override
	public void toggle(){
		setChecked(!state);
	}
	
	/**
	 * Update size of view so that both textviews are the same size and fit inside the view with the thumb
	 */
	private void updateViewDimensions(){
		int width;
		int height;
		
		// Get the current layout params for the views
		ViewGroup.LayoutParams onLayoutParams = onTextView.getLayoutParams();
		ViewGroup.LayoutParams offLayoutParams = offTextView.getLayoutParams();
		ViewGroup.LayoutParams thumbLayoutParams = thumb.getLayoutParams();
		ViewGroup.LayoutParams scrollLayoutParams = horizontalScrollView.getLayoutParams();
		
		
		// Set the height and width to wrap content so that they will expand to fit all of their text
		onLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		onLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		
		offLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		offLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
		
		// Get the width and height when the text views can fit all of their text
		width = Math.max(onTextView.getMeasuredWidth(), offTextView.getMeasuredWidth());
		height = Math.max(onTextView.getMeasuredHeight(), offTextView.getMeasuredHeight());
		
		// Set the width to either the larger textview or to the minimum width based off of the height
		width = Math.max(width, (int)(MIN_WIDTH_TEXT_MULTIPLIER * height));
		
		height = Math.max(height, MIN_HEIGHT);
		
		// Set the layout params with the new height and width
		onLayoutParams.width = width;
		onLayoutParams.height = height;
		
		offLayoutParams.width = width;
		offLayoutParams.height = height;
		
		// Set the height and width of the thumb
		thumbLayoutParams.width = (int)(width * THUMB_WIDTH_MULTIPLIER);
		thumbLayoutParams.height = height;
		
		// Set the height and width of the scroll view so it can fit one textview and the thumb at a time
		scrollLayoutParams.height = height;
		scrollLayoutParams.width = width + thumbLayoutParams.width;
		
		// Set the x location where the view appears in the on and off states
		onScrollX = 0;
		offScrollX = width;
		
		// Get the maximum scrollable width
		maxScrollX = width;
		
		invalidate();
		
		ViewGroup.LayoutParams frameLayoutParams = getLayoutParams();
		
		if(D){ 
			Log.d("FlickButtonView", myCount + " updateViewDimensions");
			Log.d("FlickButtonView", "On:  Width: " + onLayoutParams.width +" Height: " + onLayoutParams.height);
			Log.d("FlickButtonView", "Off:  Width: " + offLayoutParams.width +" Height: " + offLayoutParams.height);
			Log.d("FlickButtonView", "Thumb:  Width: " + thumbLayoutParams.width +" Height: " + thumbLayoutParams.height);
			Log.d("FlickButtonView", "Scroll:  Width: " + scrollLayoutParams.width +" Height: " + scrollLayoutParams.height);
			Log.d("FlickButtonView", "Frame:  Width: " + frameLayoutParams.width +" Height: " + frameLayoutParams.height);
		}
	}
	
	/**
	 * Refresh the scroll so the view is drawn in the correct state
	 * @param smooth if true scroll smoothly
	 */
	private void refreshScroll(boolean smooth){
		int scrollX = state ? onScrollX : offScrollX;
		
		if(D) Log.d("FlickButtonView", myCount + " RefreshScroll, State: " + state + " Location: " + scrollX);
		
		if(smooth){
			horizontalScrollView.smoothScrollTo(scrollX, horizontalScrollView.getScrollY());
		}else{
			horizontalScrollView.scrollTo(scrollX, horizontalScrollView.getScrollY());
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		if(D) Log.d("FlickButtonView", myCount + " onMeasure");
		
		// Update dimensions when measured so that they are drawn correctly
		// Must happen after super class is called
		updateViewDimensions();
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		// Prevent scroll view from getting touch events when the view is disabled
		if(!enabled){
			return true;
		}
		
		switch(ev.getAction()){
		case MotionEvent.ACTION_DOWN:
			// Get the location of where the user presses down
			lastTouch.x = ev.getX();
			lastTouch.y = ev.getY();
			
			break;
			
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			// Count the motion as a tap if motion event does move more than tap_distance away from action down
			if(Math.abs(lastTouch.x - ev.getX()) < TAP_X_DISTANCE && Math.abs(lastTouch.y - ev.getY()) < TAP_Y_DISTANCE){
				setChecked(!state, true);
			}else{
				if(state){
					setChecked(horizontalScrollView.getScrollX() < maxScrollX * FLICK_RATIO, true);
				}else {
					setChecked(horizontalScrollView.getScrollX() < maxScrollX * (1 - FLICK_RATIO), true);
				}
			}
			
			return true;
		default:
			break;
		}
		
		return super.onInterceptTouchEvent(ev);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom){
		super.onLayout(changed, left, top, right, bottom);
		
		if(D) Log.d("FlickButtonView", myCount + " onLayout");
		
		// Refresh the scroll position based on the state
		refreshScroll(false);
	}
	
	public interface OnFlickChangeListener{
		public void onFlickChanged(FlickButtonView flickButton, boolean isChecked);
	}
}
