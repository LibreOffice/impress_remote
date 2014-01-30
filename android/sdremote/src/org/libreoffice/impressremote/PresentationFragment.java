package org.libreoffice.impressremote;

import org.libreoffice.impressremote.communication.CommunicationService;
import org.libreoffice.impressremote.communication.SlideShow;

import pl.polidea.coverflow.AbstractCoverFlowImageAdapter;
import pl.polidea.coverflow.CoverFlow;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.TextView;

public class PresentationFragment extends Fragment {

	private CoverFlow mTopView;
	private ImageView mHandle;
	private View mLayout;
	private WebView mNotes;
	private Context mContext;
	private TextView mNumberText;

	private CommunicationService mCommunicationService;
	private SlideShow mSlideShow;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                Bundle savedInstanceState) {
		mContext = container.getContext();

		View v = inflater.inflate(R.layout.fragment_presentation, container,
		                false);

		mNotes = (WebView) v.findViewById(R.id.presentation_notes);

		String summary = "<html><body>This is just a test<br/><ul><li>And item</li><li>And again</li></ul>More text<br/>Blabla<br/>Blabla<br/>blabla<br/>Blabla</body></html>";
		mNotes.loadData(summary, "text/html", null);
		mNotes.setBackgroundColor(Color.TRANSPARENT);

		//		TextView aText = new TextView();
		//		aText.setText

		mTopView = (CoverFlow) v.findViewById(R.id.presentation_coverflow);

		mLayout = v.findViewById(R.id.presentation_layout);

		mNumberText = (TextView) v.findViewById(R.id.presentation_slidenumber);

		mHandle = (ImageView) v.findViewById(R.id.presentation_handle);
		mHandle.setOnTouchListener(new SizeListener());

		// Call again to set things up if necessary.
		setCommunicationService(mCommunicationService);

		return v;
	}

	// -------------------------------------------------- RESIZING LISTENER ----
	private class SizeListener implements OnTouchListener {

		@Override
		public boolean onTouch(View aView, MotionEvent aEvent) {

			switch (aEvent.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mHandle.setImageResource(R.drawable.handle_light);
				break;
			case MotionEvent.ACTION_UP:
				mHandle.setImageResource(R.drawable.handle_default);
				break;
			case MotionEvent.ACTION_MOVE:
				LayoutParams aParams = mTopView.getLayoutParams();
				int aHeightOriginal = mTopView.getHeight();
				int aHeight = mTopView.getHeight();

				final int DRAG_MARGIN = 120;

				// Set Height

				aParams.height = aHeight + (int) (aEvent.getY());
				int aViewSize = mLayout.getHeight();
				if (aParams.height < DRAG_MARGIN) {
					aParams.height = DRAG_MARGIN;
				} else if (aParams.height > aViewSize - DRAG_MARGIN) {
					aParams.height = aViewSize - DRAG_MARGIN;
				}

				int aDiff = aParams.height - aHeightOriginal;
				mTopView.setLayoutParams(aParams);

				// Now deal with the internal height
				System.out.println("Before:W:" + mTopView.getImageWidth()
				                + ":H:" + mTopView.getImageHeight());
				AbstractCoverFlowImageAdapter aAdapter = (AbstractCoverFlowImageAdapter) mTopView
				                .getAdapter();
				int aHeightNew = (int) (mTopView.getImageHeight() + aDiff);
				aAdapter.setHeight(aHeightNew);
				mTopView.setImageHeight(aHeightNew);
				int aWidthNew = aHeightNew * 180 / 150;
				aAdapter.setWidth(aWidthNew);
				mTopView.setImageWidth(aWidthNew);
				aAdapter.notifyDataSetChanged();
				System.out.println("After:W:" + mTopView.getImageWidth()
				                + ":H:" + mTopView.getImageHeight());
				break;
			}
			// TODO Auto-generated method stub
			return true;
		}
	}

	// ----------------------------------------------------- CLICK LISTENER ----

	protected class ClickListener implements OnItemSelectedListener {

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1,
		                int aPosition, long arg3) {
			if (mCommunicationService != null)
				mCommunicationService.getTransmitter().gotoSlide(aPosition);
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	// ---------------------------------------------------- MESSAGE HANDLER ----
	public void setCommunicationService(
	                CommunicationService aCommunicationService) {
		mCommunicationService = aCommunicationService;
		if (mCommunicationService == null)
			return;

		mSlideShow = mCommunicationService.getSlideShow();
		if (mTopView != null && mSlideShow != null) {
			mTopView.setAdapter(new ThumbnailAdapter(mContext, mSlideShow));
			mTopView.setSelection(mSlideShow.getCurrentSlide(), true);
			mTopView.setOnItemSelectedListener(new ClickListener());
		}

	}

	public void handleMessage(Message aMessage) {
		Bundle aData = aMessage.getData();
		switch (aMessage.what) {
		case CommunicationService.MSG_SLIDE_CHANGED:
			int aSlide = aData.getInt("slide_number");
			mTopView.setSelection(aSlide, true);

			mNumberText.setText(mSlideShow.getCurrentSlide() + "/"
			                + mSlideShow.getSize());

			break;
		case CommunicationService.MSG_SLIDE_PREVIEW:
			int aNSlide = aData.getInt("slide_number");
			if (mTopView.getSelectedItemPosition() == aNSlide) {
				// mTopView. // TODO: update the current item
			}
			break;

		}
	}

	// ------------------------------------------------- THUMBNAIL ADAPTER ----
	protected class ThumbnailAdapter extends AbstractCoverFlowImageAdapter {

		private Context mContext;

		private SlideShow mSlideShow;

		public ThumbnailAdapter(Context aContext, SlideShow aSlideShow) {
			mContext = aContext;
			mSlideShow = aSlideShow;
		}

		@Override
		public int getCount() {
			return mSlideShow.getSize();
		}

		@Override
		protected Bitmap createBitmap(int position) {
			Bitmap aBitmap = mSlideShow.getImage(position);
			final int borderWidth = 8;

			Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
			p.setShadowLayer(borderWidth, 0, 0, Color.BLACK);

			RectF aRect = new RectF(borderWidth, borderWidth, borderWidth
			                + aBitmap.getWidth(), borderWidth
			                + aBitmap.getHeight());
			Bitmap aOut = Bitmap.createBitmap(aBitmap.getWidth() + 2
			                * borderWidth, aBitmap.getHeight() + 2
			                * borderWidth, aBitmap.getConfig());
			Canvas canvas = new Canvas(aOut);
			canvas.drawColor(Color.TRANSPARENT);
			canvas.drawRect(aRect, p);
			canvas.drawBitmap(aBitmap, null, aRect, null);

			return aOut;
		}
	}
}
