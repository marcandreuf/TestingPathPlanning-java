package org.emilythecamel.jtsutils;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import java.util.HashSet;
import java.util.Set;

/**
 * A decorator for {@linkplain com.vividsolutions.jts.geom.Polygon} that adds the ability
 * to weed out colinear outer vertices.
 *
 * http://lists.refractions.net/pipermail/jts-devel/2008-December/002763.html
 *
 * @todo needs a better name
 *
 * @author Michael Bedward
 */
class EditablePolygon extends Polygon {

    /**
     * Constructor
     * @param poly input polygon
     */
    EditablePolygon(Geometry poly) {
        super((LinearRing)((Polygon)poly).getExteriorRing(), null, poly.getFactory());
    }

    /**
     * Locate and remove any colinear vertices in the exterior ring of the polygon
     * @return the number of vertices removed
     */
    public int removeColinearOuterVertices() {
        Coordinate[] src = shell.getCoordinates();
        int n = src.length - 1;

        Set<Integer> toRm = new HashSet<Integer>();
        for (int i = 0; i < n; i++) {
            int h = (i - 1 + n) % n;
            int j = (i + 1) % n;
            if (CGAlgorithms.computeOrientation(src[h], src[i], src[j]) ==
                    CGAlgorithms.COLLINEAR) {
                toRm.add(i);
            }
        }

        int numColinear = toRm.size();

        if (numColinear > 0) {
            Coordinate[] dst = new Coordinate[src.length - toRm.size()];
            for (int i = 0, j = 0; i < n; i++) {
                if (toRm.contains(i)) {
                    toRm.remove((Integer) i);
                } else {
                    dst[j++] = src[i];
                }
            }
            dst[dst.length-1] = dst[0];
            shell = getFactory().createLinearRing(dst);
            geometryChanged();
        }

        return numColinear;
    }

}