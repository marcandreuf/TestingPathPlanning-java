package org.emilythecamel.jtsutils;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.*;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * A class to decompose a recti-linear (aka orthogonal) polygon into a set of
 * non-overlapping rectangular polygons.
 *
 * http://lists.refractions.net/pipermail/jts-devel/2008-December/002763.html
 *
 * @author Michael Bedward
 */
public class OrthoPolygonDecomposer {

    private GeometryFactory gf;

    /**
     * Constructor
     */
    public OrthoPolygonDecomposer() {
        gf = new GeometryFactory();
    }

    /**
     * Decompose an orthogonal (rectilinear) polygon into a series of non-overlapping
     * rectangles.
     * @param orthoPoly the input polygon; it must not contain any holes and
     * is ASSUMED to be orthogonal
     * @return a list of non-overlapping rectangular Polygons
     */
    public List<Polygon> decompose(Geometry orthoPoly) {

        if (((Polygon) orthoPoly).getNumInteriorRing() > 0) {
            throw new IllegalArgumentException("can't deal with holes yet");
        }

        List<Polygon> rectangles = new ArrayList<Polygon>();
        List<Coordinate> vertices = new ArrayList<Coordinate>();
        List<Integer> orientation;

        Geometry edPoly = new EditablePolygon((Geometry) orthoPoly.clone());
        Polygon rect;

        while (true) {
            ((EditablePolygon)edPoly).removeColinearOuterVertices();
            vertices.addAll(Arrays.asList(edPoly.getCoordinates()));
            if (vertices.size() <= 5) {
                break;
            }

            vertices.remove(vertices.size() - 1);
            orientation = getVertexOrientation(vertices);

            rect = search3(vertices, orientation);
            rectangles.add(rect);
            Geometry diff = edPoly.difference(rect);

            if (diff.getGeometryType().equals("Polygon")) {
                edPoly = new EditablePolygon(diff);
            } else {
                throw new RuntimeException("expected more polygons");
            }

            vertices.clear();
        }

        rect = makeRectPoly(vertices.get(0), vertices.get(2));
        rectangles.add(rect);

        return rectangles;
    }

    /**
     * Searches for a rectangle by looking for a concave vertex preceded by 3 or more
     * convex vertices. If this configuration is found a new rectangle is constructed
     * having, as its diagonal, the line segment between the 1st and 3rd preceding
     * convex vertices;
     * otherwise the search is passed to
     * {@linkplain #search2(java.util.List, java.util.List) }
     * @param vertices list of unique polygon vertices
     * @param orientation corresponding list of vertex orientations
     * @return a new rectangular polygon or null if one could not be constructed
     */
    private Polygon search3(List<Coordinate> vertices, List<Integer> orientation) {
        int n = vertices.size();
        Stack<Integer> convex = new Stack<Integer>();

        for (int i = 0; i < n; i++) {
            switch (orientation.get(i)) {
                case CGAlgorithms.LEFT:
                    if (convex.size() >= 3) {
                        int h = convex.pop();
                        convex.pop();
                        int f = convex.pop();
                        return makeRectPoly(vertices.get(f), vertices.get(h));
                    } else {
                        convex.clear();
                    }
                    break;

                case CGAlgorithms.RIGHT:
                    convex.push(i);
                    break;
            }
        }

        return search2(vertices, orientation);
    }

    /**
     * Searches for a rectangle by looking for a concave vertex preceded by 2 or more
     * convex vertices. If this configuration is found, a rectangle is constructed
     * that has, as its diagonal, the line segment between the concave vertex and the
     * convex vertex two positions before it.
     * @param vertices list of unique polygon vertices
     * @param orientation corresponding list of vertex orientations
     * @return a new rectangular polygon or null if one could not be constructed
     */
    private Polygon search2(List<Coordinate> vertices, List<Integer> orientation) {
        int n = vertices.size();
        Stack<Integer> convex = new Stack<Integer>();

        for (int i = 0; i < n; i++) {
            switch (orientation.get(i)) {
                case CGAlgorithms.LEFT:
                    if (convex.size() >= 2) {
                        convex.pop();
                        int g = convex.pop();
                        return makeRectPoly(vertices.get(g), vertices.get(i));
                    } else {
                        convex.clear();
                    }
                    break;

                case CGAlgorithms.RIGHT:
                    convex.push(i);
                    break;
            }
        }

        return null;
    }

    /**
     * Get the orientation of each vertex in the list of polygon vertices
     * @param vertices list of polygon vertices (start coordinate appears only once)
     * @return integer codes for orientation as used by {@link com.vividsolutions.jts.algorithm.CGAlgorithms}
     */
    private List<Integer> getVertexOrientation(List<Coordinate> vertices) {
        List<Integer> orientation = new ArrayList<Integer>();
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            int h = (i - 1 + n) % n;
            int j = (i + 1) % n;
            orientation.add(CGAlgorithms.computeOrientation(
                    vertices.get(h), vertices.get(i), vertices.get(j)));
        }

        return orientation;
    }

    /**
     * Make a rectangular polygon with diagonal given by the two coordinates
     * @param c0 first end-point of diagonal
     * @param c1 second end-point of diagonal
     * @return a new Polygon
     */
    private Polygon makeRectPoly(Coordinate c0, Coordinate c1) {
        double minX = Math.min(c0.x, c1.x);
        double minY = Math.min(c0.y, c1.y);
        double maxX = Math.max(c0.x, c1.x);
        double maxY = Math.max(c0.y, c1.y);

        Coordinate[] coords = {
                new Coordinate(minX, minY),
                new Coordinate(minX, maxY),
                new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY),
                new Coordinate(minX, minY)
        };

        return gf.createPolygon(gf.createLinearRing(coords), null);
    }
}