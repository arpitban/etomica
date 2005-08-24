package etomica.space;



/**
 * Rectangular boundary that is periodic in every dimension.
 */

/*
 * History
 * Created on Apr 7, 2005 by kofke
 */
public class BoundaryRectangularPeriodic extends BoundaryRectangular {

    /**
     * @param space
     */
    public BoundaryRectangularPeriodic(Space space) {
        super(space, makePeriodicity(space.D()));
    }

    public void nearestImage(Vector dr) {
        dr.PE(dimensionsHalf);
        dr.mod(dimensions);
        dr.ME(dimensionsHalf);
    }

    public Vector centralImage(Vector r) {
        modShift.EModShift(r, dimensions);
        return modShift;
    }

    private static boolean[] makePeriodicity(int D) {
        boolean[] isPeriodic = new boolean[D];
        for (int i=0; i<D; i++) {
            isPeriodic[i] = true;
        }
        return isPeriodic;
    }
    
}
