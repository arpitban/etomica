package etomica.lattice.crystal;

import etomica.math.geometry.Cube;
import etomica.math.geometry.Polytope;
import etomica.math.geometry.Square;
import etomica.space.Space;
import etomica.space.Vector;

/**
 * Primitive group for a cubic system.  All primitive
 * vectors orthogonal and of equal length.
 */
public class PrimitiveCubic extends Primitive {
    
    //primitive vectors are stored internally at unit length.  When requested
    //from the vectors() method, copies are scaled to size and returned.
    //default size is 1.0
    private double cubicSize;
    
    public PrimitiveCubic(Space space) {
        this(space, 1.0);
    }
    public PrimitiveCubic(Space space, double latticeConstant) {
        this(space, latticeConstant, true);
    }
    
    protected PrimitiveCubic(Space space, double latticeConstant, boolean makeReciprocal) {
        super(space, makeReciprocal); //also makes reciprocal
        //set up orthogonal vectors of unit size
        setCubicSize(latticeConstant); //also sets reciprocal via update
        double[] newAngles = new double[D];
        for (int i=0; i<D; i++) {
            newAngles[i] = rightAngle;
        }
        setAngles(newAngles);
    }
    
    //called by superclass constructor
    protected Primitive makeReciprocal() {
        return new PrimitiveCubic(space, 1, false);
    }
    
    //called by update method of superclass
    protected void updateReciprocal() {
        ((PrimitiveCubic)reciprocal).setCubicSize(2.0*Math.PI/size[0]);
    }
    
    /**
     * Returns a new PrimitiveCubic with the same size as this one.
     */
    public Primitive copy() {
        return new PrimitiveCubic(space, cubicSize);
    }
    
    /**
     * Sets the length of all primitive vectors to the given value.
     */
    public void setCubicSize(double newCubicSize) {
        if (newCubicSize == cubicSize) {
            // no change
            return;
        }
        double[] sizeArray = new double[D];
        for(int i=0; i<D; i++) {
            sizeArray[i] = newCubicSize;
        }
        setSize(sizeArray);
    }
    
    public void setSize(double[] newSize) {
        for (int i=1; i<D; i++) {
            if (newSize[0] != newSize[i]) {
                throw new RuntimeException("new size must be cubic");
            }
        }
        if (cubicSize == newSize[0]) {
            // no change
            return;
        }
        super.setSize(newSize);
        // set cubicSize after super.setSize just in case it throws
        cubicSize = newSize[0];
    }
    
    protected void update() {
        super.update();
        for(int i=0; i<D; i++) latticeVectors[i].setX(i, size[0]);
    }
    
    public void setAngles(double[] newAngle) {
        for (int i=0; i<D; i++) {
            if (newAngle[i] != rightAngle) {
                throw new IllegalArgumentException("PrimitiveCubic angles must be right angles");
            }
        }
        super.setAngles(newAngle);
    }
    
    /**
     * Returns the common length of all primitive vectors.
     */
    public double getCubicSize() {return cubicSize;}
    
    public void scaleSize(double scale) {
        setCubicSize(scale*cubicSize);
    }

    public int[] latticeIndex(Vector q) {
        for(int i=0; i<D; i++) {
            double x = q.x(i)/cubicSize;
            idx[i] = (x < 0) ? (int)x - 1 : (int)x; //we want idx to be the floor of x
        }
        return idx;
    }
    
    public int[] latticeIndex(Vector q, int[] dimensions) {
        for(int i=0; i<D; i++) {
            double x = q.x(i)/cubicSize;
            idx[i] = (x < 0) ? (int)x - 1 : (int)x; //we want idx to be the floor of x
            while(idx[i] >= dimensions[i]) idx[i] -= dimensions[i];
            while(idx[i] < 0)              idx[i] += dimensions[i];
        }
        return idx;
    }
    
    /**
     * Returns a new Square (if primitive is 2D) or Cube (if 3D) with edges
     * given by the size of the primitive vectors.
     */
    public Polytope wignerSeitzCell() {
        return (D == 2) ? (Polytope)new Square(space,cubicSize) :  (Polytope)new Cube(space,cubicSize);
    }
    
    /**
     * Returns a new Square (if primitive is 2D) or Cube (if 3D) with edges
     * given by the size of the primitive vectors.
     */
    public Polytope unitCell() {
        return (D == 2) ? (Polytope)new Square(space,cubicSize) :  (Polytope)new Cube(space,cubicSize);
    }
    
    public String toString() {return "Cubic";}
    
}
    
