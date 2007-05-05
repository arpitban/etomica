package etomica.lattice;

import etomica.atom.AtomFilter;
import etomica.atom.IAtom;
import etomica.atom.IAtomPositioned;
import etomica.lattice.crystal.Primitive;
import etomica.math.geometry.Plane;
import etomica.space.IVector;
import etomica.space.Space;
import etomica.space3d.Vector3D;

/**
 * Class describing a plane through a lattice.  Holds a Plane
 * object and a Primitive.  The orientation of the plane is 
 * specified using Miller indices defined according to the primitive.
 */
public class LatticePlane implements AtomFilter, java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
    private final Plane plane;
    private Primitive primitive;
    private Primitive reciprocal;
    private Vector3D normal, delta;
    private Space space;
    private int[] millerIndices;
    private Vector3D origin;
    
    public LatticePlane(Primitive primitive, int[] h) {
        this.primitive = primitive;
        reciprocal = primitive.reciprocal();
        space = primitive.getSpace();
        origin = (Vector3D)space.makeVector();
        millerIndices = new int[space.D()];
        if(space.D() != 3) {
            throw new IllegalArgumentException("LatticePlane not defined for other than 3D space");
        }
        plane = new Plane();
        plane.epsilon = 1.0e-2;//tolerance for defining a point to be in the plane
        normal = (Vector3D)space.makeVector();
        delta = (Vector3D)space.makeVector();
        setMillerIndices(h);
    }
    
    public boolean accept(IAtom a) {
        return !plane.isPositiveSide((Vector3D)((IAtomPositioned)a).getPosition());
    }

    public void setTolerance(double tolerance) {
    	plane.epsilon = tolerance;
    }

    public void setPrimitive(Primitive primitive) {
        this.primitive = primitive;
        reciprocal = primitive.reciprocal();
        //update orientation of plane using current miller indices and the new primitive
        setMillerIndices(millerIndices);
    }
    
    /**
     * Sets the plane to the orientation specified by the given Miller indices
     * applied to the current primitive.
     */
    public void setMillerIndices(int[] h) {
        if(h.length != space.D()) throw new IllegalArgumentException("Error: number of miller indices passed to LatticePlane.setMillerIndices inconsistent with spatial dimension");
        double currentPosition = getPosition();
        normal.E(0.0);
        IVector[] b = reciprocal.vectors();
        for(int i=0; i<h.length; i++) {
            normal.PEa1Tv1(h[i],b[i]);
            millerIndices[i] = h[i];
        }
        plane.setNormalVector(normal);
        setPosition(currentPosition);
    }
    
    /**
     * Returns the current value of the i-th Miller index.
     */
    public int getMillerIndex(int i) {return millerIndices[i];}
    /**
     * Sets the i-th Miller index to h, keeping the others fixed.
     * If setting causes all indices to be zero, change is not implemented.
     */
    public void setMillerIndex(int i, int h) {
        if(i < 0 || i >= space.D()) throw new IllegalArgumentException("Error: specified miller index passed to LatticePlane.setMillerIndex inconsistent with spatial dimension");
        int oldH = millerIndices[i];
        millerIndices[i] = h;
        boolean allZero = true;
        for(int j=0; j<millerIndices.length; j++) if(millerIndices[j] != 0) allZero = false;
        if(allZero) millerIndices[i] = oldH;
        else setMillerIndices(millerIndices);
    }
    
    /**
     * Sets the position of the plane to be the given distance from the
     * origin in the direction of the normal.
     */
    public void setSpacePosition(double d) {
        delta.E(origin);
        delta.PEa1Tv1(d/Math.sqrt(normal.squared()), normal);
        plane.moveTo(delta);
    }
    /**
     * Gets the position of the plane as the distance from the
     * origin in the direction of the normal.
     */
    public double getSpacePosition() {
        return -plane.distanceTo(origin);
    }
    
    /**
     * Sets to the position of the i-th plane from the one through the origin.
     */
    public void setPosition(double d) {
        //use normal as a work vector, giving a point contained by the plane
        //in its desired position
        delta.E(origin);
		delta.PEa1Tv1(d*2.0*Math.PI/normal.squared(),normal);
        plane.moveTo(delta);
    }
    
    /**
     * Returns the position i, such the the plane is located at the i-th from the origin.
     */
    public double getPosition() {
        double d = plane.distanceTo(origin);
//        return -Math.round((float)(d/(2.0*Math.PI)*Math.sqrt(normal.squared())));
      return -((float)(d/(2.0*Math.PI)*Math.sqrt(normal.squared())));
      }

    /**
     * Returns true if the given point is on the side of the 
     * plane toward which the normal vector points.  The direction
     * of the normal vector can be inverted using the invert method.
     */
    public boolean isPositiveSide(Vector3D p) {
        return plane.isPositiveSide(p);
    }
    
    /**
     * Returns true if the given point is inside the plane (within some small tolerance).
     */
    public boolean inPlane(Vector3D p) {
        return plane.inPlane(p);
    }
    
    /**
     * Returns a copy of the Plane that defines the orientation of this LatticePlane.
     * Only a copy is returned, so manipulation of the returned Plane does not 
     * affect this LatticePlane, and subsequent manipulations of this are not reflected
     * in the copy unless this method is invoked again.
     * Copy is returned using the Plane given in the argument; if that is null, a new
     * Plane is instantiated and returned as the copy.
     */
    public Plane planeCopy(Plane copy) {
        if(copy == null) {
            return new Plane(plane);
        }
        copy.E(plane); 
        return copy;
    }
    
    /**
     * Sets the origin from which the position of the atom is measured.
     */
    public void setOrigin(Vector3D origin) {
        this.origin.E(origin);
    }
    public IVector getOrigin() {return origin;}
    
    /**
     * Changes the direction of the normal vector so that it points
     * toward the other side of the plane from its present orientation.
     * Does not affect the location or absolute orientation of the plane.
     */
    public void invert() {
        plane.invert();
    }
}//end of LatticePlane
