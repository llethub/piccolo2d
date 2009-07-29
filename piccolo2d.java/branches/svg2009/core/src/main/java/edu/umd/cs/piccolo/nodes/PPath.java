/*
 * Copyright (c) 2008-2009, Piccolo2D project, http://piccolo2d.org
 * Copyright (c) 1998-2008, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * None of the name of the University of Maryland, the name of the Piccolo2D project, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.umd.cs.piccolo.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PPath</b> is a wrapper around a java.awt.geom.GeneralPath. The setBounds
 * method works by scaling the path to fit into the specified bounds. This
 * normally works well, but if the specified base bounds get too small then it
 * is impossible to expand the path shape again since all its numbers have
 * tended to zero, so application code may need to take this into consideration.
 * <P>
 * One option that applications have is to call <code>startResizeBounds</code>
 * before starting an interaction that may make the bounds very small, and
 * calling <code>endResizeBounds</code> when this interaction is finished. When
 * this is done PPath will use a copy of the original path to do the resizing so
 * the numbers in the path wont loose resolution.
 * <P>
 * This class also provides methods for constructing common shapes using a
 * general path.
 * <P>
 * 
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PPath extends PNode {

    /**
     * Allows for future serialization code to understand versioned binary
     * formats.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The property name that identifies a change of this node's stroke paint
     * (see {@link #getStrokePaint getStrokePaint}). Both old and new value will
     * be set correctly to Paint objects in any property change event.
     */
    public static final String PROPERTY_STROKE_PAINT = "strokePaint";
    public static final int PROPERTY_CODE_STROKE_PAINT = 1 << 16;

    /**
     * The property name that identifies a change of this node's stroke (see
     * {@link #getStroke getStroke}). Both old and new value will be set
     * correctly to Stroke objects in any property change event.
     */
    public static final String PROPERTY_STROKE = "stroke";
    public static final int PROPERTY_CODE_STROKE = 1 << 17;

    /**
     * The property name that identifies a change of this node's path (see
     * {@link #getPathReference getPathReference}). In any property change event
     * the new value will be a reference to this node's path, but old value will
     * always be null.
     */
    public static final String PROPERTY_PATH = "path";
    public static final int PROPERTY_CODE_PATH = 1 << 18;

    private static final Rectangle2D.Float TEMP_RECTANGLE = new Rectangle2D.Float();
    private static final RoundRectangle2D.Float TEMP_ROUNDRECTANGLE = new RoundRectangle2D.Float();
    private static final Ellipse2D.Float TEMP_ELLIPSE = new Ellipse2D.Float();
    private static final PAffineTransform TEMP_TRANSFORM = new PAffineTransform();
    private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1.0f);
    private static final Color DEFAULT_STROKE_PAINT = Color.black;

    private transient GeneralPath path;
    private transient GeneralPath resizePath;
    private transient Stroke stroke;
    private transient boolean updatingBoundsFromPath;
    private Paint strokePaint;

    public static PPath createRectangle(final float x, final float y, final float width, final float height) {
        TEMP_RECTANGLE.setFrame(x, y, width, height);
        final PPath result = new PPath(TEMP_RECTANGLE);
        result.setPaint(Color.white);
        return result;
    }

    public static PPath createRoundRectangle(final float x, final float y, final float width, final float height,
            final float arcWidth, final float arcHeight) {
        TEMP_ROUNDRECTANGLE.setRoundRect(x, y, width, height, arcWidth, arcHeight);
        final PPath result = new PPath(TEMP_ROUNDRECTANGLE);
        result.setPaint(Color.white);
        return result;
    }

    public static PPath createEllipse(final float x, final float y, final float width, final float height) {
        TEMP_ELLIPSE.setFrame(x, y, width, height);
        final PPath result = new PPath(TEMP_ELLIPSE);
        result.setPaint(Color.white);
        return result;
    }

    public static PPath createLine(final float x1, final float y1, final float x2, final float y2) {
        final PPath result = new PPath();
        result.moveTo(x1, y1);
        result.lineTo(x2, y2);
        result.setPaint(Color.white);
        return result;
    }

    public static PPath createPolyline(final Point2D[] points) {
        final PPath result = new PPath();
        result.setPathToPolyline(points);
        result.setPaint(Color.white);
        return result;
    }

    public static PPath createPolyline(final float[] xp, final float[] yp) {
        final PPath result = new PPath();
        result.setPathToPolyline(xp, yp);
        result.setPaint(Color.white);
        return result;
    }

    public PPath() {
        strokePaint = DEFAULT_STROKE_PAINT;
        stroke = DEFAULT_STROKE;
        path = new GeneralPath();
    }

    public PPath(final Shape aShape) {
        this(aShape, DEFAULT_STROKE);
    }

    /**
     * Construct this path with the given shape and stroke. This method may be
     * used to optimize the creation of a large number of PPaths. Normally
     * PPaths have a default stroke of width one, but when a path has a non null
     * stroke it takes significantly longer to compute its bounds. This method
     * allows you to override that default stroke before the bounds are ever
     * calculated, so if you pass in a null stroke here you won't ever have to
     * pay that bounds calculation price if you don't need to.
     */
    public PPath(final Shape aShape, final Stroke aStroke) {
        this();
        stroke = aStroke;
        if (aShape != null) {
            append(aShape, false);
        }
    }

    // ****************************************************************
    // Stroke
    // ****************************************************************

    public Paint getStrokePaint() {
        return strokePaint;
    }

    public void setStrokePaint(final Paint aPaint) {
        final Paint old = strokePaint;
        strokePaint = aPaint;
        invalidatePaint();
        firePropertyChange(PROPERTY_CODE_STROKE_PAINT, PROPERTY_STROKE_PAINT, old, strokePaint);
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(final Stroke aStroke) {
        final Stroke old = stroke;
        stroke = aStroke;
        updateBoundsFromPath();
        invalidatePaint();
        firePropertyChange(PROPERTY_CODE_STROKE, PROPERTY_STROKE, old, stroke);
    }

    // ****************************************************************
    // Bounds
    // ****************************************************************

    public void startResizeBounds() {
        resizePath = new GeneralPath(path);
    }

    public void endResizeBounds() {
        resizePath = null;
    }

    /**
     * Set the bounds of this path. This method works by scaling the path to fit
     * into the specified bounds. This normally works well, but if the specified
     * base bounds get too small then it is impossible to expand the path shape
     * again since all its numbers have tended to zero, so application code may
     * need to take this into consideration.
     */
    protected void internalUpdateBounds(double x, double y, double width, double height) {
        if (updatingBoundsFromPath || path == null) {
            return;
        }

        if (resizePath != null) {
            path.reset();
            path.append(resizePath, false);
        }

        final Rectangle2D pathBounds = path.getBounds2D();
        final Rectangle2D pathStrokeBounds = getPathBoundsWithStroke();
        final double strokeOutset = Math.max(pathStrokeBounds.getWidth() - pathBounds.getWidth(), pathStrokeBounds
                .getHeight()
                - pathBounds.getHeight());

        x += strokeOutset / 2;
        y += strokeOutset / 2;
        width -= strokeOutset;
        height -= strokeOutset;

        final double scaleX = width == 0 || pathBounds.getWidth() == 0 ? 1 : width / pathBounds.getWidth();
        final double scaleY = height == 0 || pathBounds.getHeight() == 0 ? 1 : height / pathBounds.getHeight();

        TEMP_TRANSFORM.setToIdentity();
        TEMP_TRANSFORM.translate(x, y);
        TEMP_TRANSFORM.scale(scaleX, scaleY);
        TEMP_TRANSFORM.translate(-pathBounds.getX(), -pathBounds.getY());

        path.transform(TEMP_TRANSFORM);
    }

    public boolean intersects(final Rectangle2D aBounds) {
        if (super.intersects(aBounds)) {
            if (getPaint() != null && path.intersects(aBounds)) {
                return true;
            }
            else if (stroke != null && strokePaint != null) {
                return stroke.createStrokedShape(path).intersects(aBounds);
            }
        }
        return false;
    }

    public Rectangle2D getPathBoundsWithStroke() {
        if (stroke != null) {
            return stroke.createStrokedShape(path).getBounds2D();
        }
        else {
            return path.getBounds2D();
        }
    }

    public void updateBoundsFromPath() {
        updatingBoundsFromPath = true;
        if (path == null) {
            resetBounds();
        }
        else {
            final Rectangle2D b = getPathBoundsWithStroke();
            setBounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
        }
        updatingBoundsFromPath = false;
    }

    // ****************************************************************
    // Painting
    // ****************************************************************

    protected void paint(final PPaintContext paintContext) {
        final Paint p = getPaint();
        final Graphics2D g2 = paintContext.getGraphics();

        if (p != null) {
            g2.setPaint(p);
            g2.fill(path);
        }

        if (stroke != null && strokePaint != null) {
            g2.setPaint(strokePaint);
            g2.setStroke(stroke);
            g2.draw(path);
        }
    }

    // ****************************************************************
    // Path Support set java.awt.GeneralPath documentation for more
    // information on using these methods.
    // ****************************************************************

    public GeneralPath getPathReference() {
        return path;
    }

    public void moveTo(final float x, final float y) {
        path.moveTo(x, y);
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void lineTo(final float x, final float y) {
        path.lineTo(x, y);
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void quadTo(final float x1, final float y1, final float x2, final float y2) {
        path.quadTo(x1, y1, x2, y2);
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void curveTo(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3) {
        path.curveTo(x1, y1, x2, y2, x3, y3);
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void append(final Shape aShape, final boolean connect) {
        path.append(aShape, connect);
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void setPathTo(final Shape aShape) {
        path.reset();
        append(aShape, false);
    }

    public void setPathToRectangle(final float x, final float y, final float width, final float height) {
        TEMP_RECTANGLE.setFrame(x, y, width, height);
        setPathTo(TEMP_RECTANGLE);
    }

    public void setPathToEllipse(final float x, final float y, final float width, final float height) {
        TEMP_ELLIPSE.setFrame(x, y, width, height);
        setPathTo(TEMP_ELLIPSE);
    }

    public void setPathToPolyline(final Point2D[] points) {
        path.reset();
        path.moveTo((float) points[0].getX(), (float) points[0].getY());
        for (int i = 1; i < points.length; i++) {
            path.lineTo((float) points[i].getX(), (float) points[i].getY());
        }
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void setPathToPolyline(final float[] xp, final float[] yp) {
        path.reset();
        path.moveTo(xp[0], yp[0]);
        for (int i = 1; i < xp.length; i++) {
            path.lineTo(xp[i], yp[i]);
        }
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void closePath() {
        path.closePath();
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    public void reset() {
        path.reset();
        firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
        updateBoundsFromPath();
        invalidatePaint();
    }

    // ****************************************************************
    // Serialization
    // ****************************************************************

    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        PUtil.writeStroke(stroke, out);
        PUtil.writePath(path, out);
    }

    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        stroke = PUtil.readStroke(in);
        path = PUtil.readPath(in);
    }
}