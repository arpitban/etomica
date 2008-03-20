package etomica.api;


public interface IBoundary extends INearestImageTransformer {

	/**
	 * @return the volume enclosed by the boundary
	 */
	public abstract double volume();

	/**
	 * Determines the translation vector needed to apply a periodic-image
	 * transformation that moves the given point to an image point within the
	 * boundary (if it lies outside, in a direction subject to periodic
	 * imaging).
	 * 
	 * @param r
	 *            vector position of untransformed point; r is not changed by
	 *            this method
	 * @return the displacement that must be applied to r to move it to its
	 *         central-image location
	 */
	public abstract IVector centralImage(IVector r);

	/**
	 * Returns a copy of the dimensions, as a Vector. Manipulation of this copy
	 * will not cause any change to the boundary's dimensions.
	 * 
	 * @return a vector giving the nominal length of the boundary in each
	 *         direction. This has an obvious interpretation for rectangular
	 *         boundaries, while for others (e.g., octahedral) the definition is
	 *         particular to the boundary.
	 */
	public abstract IVector getDimensions();

	/**
	 * Sets the nominal length of the boundary in each direction. Specific
	 * interpretation of the given values (which are the elements of the given
	 * Vector) depends on the subclass.
	 */
	public abstract void setDimensions(IVector v);

	/**
	 * @return a point selected uniformly within the volume enclosed by the
	 *         boundary.
	 */
	public abstract IVector randomPosition();

	/**
	 * Provides information needed so that drawing can be done of portions of
	 * the periodic images of an atom that overflow into the volume (because the
	 * periodic image is just outside the volume).
	 * 
	 * @param r
	 *            position of the atom in the central image
	 * @param distance
	 *            size of the atom, indicating how far its image extends
	 * @return all displacements needed to draw all overflow images; first index
	 *         indicates each displacement, second index is the xyz translation
	 *         needed to the overflow image
	 */
	public abstract float[][] getOverflowShifts(IVector r, double distance);

	/**
	 * Returns the length of the sides of a rectangular box oriented in the lab
	 * frame and in which the boundary is inscribed.  Each element of the returned
	 * vector gives in that coordinate direction the maximum distance from one point 
	 * on the boundary to another.  Returned vector should be used immediately or
	 * copied to another vector.
	 */
	public abstract IVector getBoundingBox();

}