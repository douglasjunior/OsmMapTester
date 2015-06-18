package com.github.douglasjunior.osmMapTester.mapsforge;

import android.graphics.Matrix;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rectangle;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidBitmap;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;

/**
 * Created by douglas on 17/06/15.
 */
public class RotateMarker extends Marker {

    private float heading;
    private RotateAndroidBitmap rotatedAndroidBitmap;

    /**
     * @param latLong          the initial geographical coordinates of this marker (may be null).
     * @param bitmap           the initial {@code Bitmap} of this marker (may be null).
     * @param horizontalOffset the horizontal marker offset.
     * @param verticalOffset
     */
    public RotateMarker(LatLong latLong, Bitmap bitmap, int horizontalOffset, int verticalOffset) {
        super(latLong, bitmap, horizontalOffset, verticalOffset);
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }

    @Override
    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
        if (getBitmap() == null || getLatLong() == null) {
            return;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(heading);

        android.graphics.Bitmap scaledBitmap = AndroidGraphicFactory.getBitmap(getBitmap());
        if (scaledBitmap == null || scaledBitmap.isRecycled()) {
            return;
        }

        android.graphics.Bitmap rotatedBitmap = android.graphics.Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        if (rotatedAndroidBitmap != null)
            rotatedAndroidBitmap.decrementRefCount();
        rotatedAndroidBitmap = new RotateAndroidBitmap(rotatedBitmap);

        long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
        double pixelX = MercatorProjection.longitudeToPixelX(getLatLong().longitude, mapSize);
        double pixelY = MercatorProjection.latitudeToPixelY(getLatLong().latitude, mapSize);

        int halfBitmapWidth = rotatedAndroidBitmap.getWidth() / 2;
        int halfBitmapHeight = rotatedAndroidBitmap.getHeight() / 2;

        int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + getHorizontalOffset());
        int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + getVerticalOffset());
        int right = left + rotatedAndroidBitmap.getWidth();
        int bottom = top + rotatedAndroidBitmap.getHeight();

        Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
        Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
        if (!canvasRectangle.intersects(bitmapRectangle)) {
            return;
        }

        canvas.drawBitmap(rotatedAndroidBitmap, left, top);
    }

    class RotateAndroidBitmap extends AndroidBitmap {
        public RotateAndroidBitmap(android.graphics.Bitmap bitmap) {
            super();
            if (bitmap.isRecycled()) {
                throw new IllegalArgumentException("Bitmap is recycled!");
            }
            this.bitmap = bitmap;
        }
    }
}
