package etomica.potential;

import etomica.EtomicaInfo;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBoundary;
import etomica.api.IVector;
import etomica.atom.AtomSetSinglet;
import etomica.atom.IAtomKinetic;
import etomica.graphics.Drawable;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.units.Dimension;
import etomica.units.DimensionRatio;
import etomica.units.Force;
import etomica.units.Length;
import etomica.units.Mass;
import etomica.units.Pressure;
import etomica.units.Time;
import etomica.util.Debug;

/**
 * Potential that places hard repulsive walls that move and 
 * accelerate subject to an external force field (pressure).
 */
 
public class P1HardMovingBoundary extends Potential1 implements PotentialHard, Drawable {
    
    /**
     * Constructor for a hard moving (and accelerating) boundary.
     * @param space
     * @param wallDimension dimension which the wall is perpendicular to
     */
    public P1HardMovingBoundary(ISpace space, IBoundary boundary, int wallDimension, double mass,
            boolean ignoreOverlap) {
        super(space);
        D = space.D();
        wallD = wallDimension;
        setWallPosition(0);
        setMass(mass);
        force = 0.0;
        pistonBoundary = boundary;
        virialSum = 0.0;
        this.ignoreOverlap = ignoreOverlap;
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Hard moving wall");
        return info;
    }
    
    public void setWallPosition(double p) {
        wallPosition = p;
    }
    public double getWallPosition() {
        return wallPosition;
    }
    public Dimension getWallPositionDimension() {
        return Length.DIMENSION;
    }
    
    public double getWallVelocity() {
        return wallVelocity;
    }
    public void setWallVelocity(double v) {
        wallVelocity = v;
    }
    public Dimension getWallVelocityDimension() {
        return new DimensionRatio("Velocity", Length.DIMENSION, Time.DIMENSION);
    }
    
    public void setForce(double f) {
        force = f;
        isForced = true;
    }
    public double getForce() {
        return force;
    }
    public Dimension getForceDimension() {
        return Force.DIMENSION;
    }
    
    public void setPressure(double p) {
        pressure = p;
        isForced = false;
    }
    public double getPressure() {
        return pressure;
    }
    public Dimension getPressureDimension() {
        return Pressure.DIMENSION;
    }
    
    public void setStationary(boolean b) {
        if(b) {
            wallMass = Double.POSITIVE_INFINITY;
            wallVelocity = 0.0;
        } else {
            wallMass = setWallMass;
        }
    }
    
    public boolean isStationary() {
        return Double.isInfinite(wallMass);
    }

    /**
     * @return Returns the mass.
     */
    public double getMass() {
        return wallMass;
    }
    /**
     * @param mass The mass to set.
     */
    public void setMass(double mass) {
        wallMass = mass;
        setWallMass = mass;
    }
    public Dimension getMassDimension() {
        return Mass.DIMENSION;
    }
    
    public double energy(IAtomSet a) {
        double dx = ((IAtomPositioned)a.getAtom(0)).getPosition().x(wallD) - wallPosition;
        if (dx*dx < collisionRadius*collisionRadius) {
            return Double.POSITIVE_INFINITY;
        }
        return 0.0;
    }
     
    public double energyChange() {return 0.0;}
    
    public double collisionTime(IAtomSet atoms, double falseTime) {
        IAtomKinetic atom = (IAtomKinetic)atoms.getAtom(0);
        double dr = atom.getPosition().x(wallD) - wallPosition;
        double dv = atom.getVelocity().x(wallD) - wallVelocity;
        dr += dv*falseTime;
        if (!isForced) {
            double area = 1.0;
            if (pressure != 0.0) {
                final IVector dimensions = pistonBoundary.getDimensions();
                for (int i=0; i<D; i++) {
                    if (i != wallD) {
                        area *= (dimensions.x(i)-collisionRadius*2.0);
                    }
                }
            }
            force = pressure*area;
        }
        double a = -force/wallMass;   // atom acceleration - wall acceleration
        dv += a*falseTime;
        dr += 0.5*a*falseTime*falseTime;
        if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(atom))) {
            System.out.println(dr+" "+dv+" "+falseTime+" "+atom);
            System.out.println(atom.getVelocity().x(wallD));
            System.out.println(atom.getPosition().x(wallD));
        }
        double t = Double.POSITIVE_INFINITY;
        double discr = -1.0;
        if (dr*dv < 0.0 || dr*a < 0.0) {
            // either moving toward or accelerating toward each other
            if ((Debug.ON || ignoreOverlap) && Math.abs(dr) < collisionRadius && dr*dv < 0.0) {
                if (ignoreOverlap) return falseTime;
                throw new RuntimeException("overlap "+atoms+" "+dr+" "+dv+" "+a);
            }
            double drc;
            if (dr>0.0) {
                drc = dr - collisionRadius;
            }
            else {
                drc = dr + collisionRadius;
            }
            discr = dv*dv - 2.0*a*drc;
            if (discr >= 0.0) {
                discr = Math.sqrt(discr);
                if (dr*a < 0.0) {
                    t = -dv/a + discr/Math.abs(a);
                }
                else if (a == 0.0) {
                    if (dr*dv < 0.0) t = -drc/dv;
                }
                else if (dr*dv < 0.0 && dr*a > 0.0) {
                    t = -dv/a - discr/Math.abs(a);
                } else {
                    throw new RuntimeException("oops");
                }
            }
        }
        if (ignoreOverlap && t<0.0) t = 0.0;
        if (Debug.ON && (t<0.0 || Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(atom)))) {
            System.out.println(atom+" "+a+" "+dr+" "+dv+" "+discr+" "+t+" "+(t+falseTime)+" "+(atom.getPosition().x(wallD)+atom.getVelocity().x(wallD)*(t+falseTime))+" "+(wallPosition+wallVelocity*(t+falseTime)-0.5*a*(t+falseTime)*(t+falseTime)));
            if (t<0) throw new RuntimeException("foo");
        }
        return t + falseTime;
    }
                
    public void bump(IAtomSet atoms, double falseTime) {
        IAtomKinetic atom = (IAtomKinetic)atoms.getAtom(0);
        double r = atom.getPosition().x(wallD);
        IVector v = atom.getVelocity();
        if (!isForced) {
            double area = 1.0;
            if (pressure != 0.0) {
                final IVector dimensions = pistonBoundary.getDimensions();
                for (int i=0; i<D; i++) {
                    if (i != wallD) {
                        area *= (dimensions.x(i)-collisionRadius*2.0);
                    }
                }
            }
            force = pressure*area;
        }
        double trueWallVelocity = wallVelocity + falseTime*force/wallMass;
        if (Debug.ON) {
            double trueWallPosition = wallPosition + wallVelocity*falseTime + 0.5*falseTime*falseTime*(force/wallMass);
            if (Math.abs(Math.abs(trueWallPosition-(r+v.x(wallD)*falseTime)) - collisionRadius) > 1.e-7*collisionRadius) {
                System.out.println("bork at "+falseTime+" ! "+atom+" "+(r+v.x(wallD)*falseTime)+" "+v.x(wallD));
                System.out.println("wall bork! "+trueWallPosition+" "+trueWallVelocity+" "+force);
                System.out.println("dr bork! "+((r+v.x(wallD)*falseTime)-trueWallPosition)+" "+collisionRadius);
                System.out.println(atom.getPosition().x(wallD));
                throw new RuntimeException("bork!");
            }
        }
        double dp = 2.0/(1/wallMass + ((IAtomTypeLeaf)atom.getType()).rm())*(trueWallVelocity-v.x(wallD));
        virialSum += dp;
        v.setX(wallD,v.x(wallD)+dp*((IAtomTypeLeaf)atom.getType()).rm());
        atom.getPosition().setX(wallD,r-dp*((IAtomTypeLeaf)atom.getType()).rm()*falseTime);
        wallVelocity -= dp/wallMass;
        wallPosition += dp/wallMass*falseTime;
        
    }
    
    public double lastWallVirial() {
        double area = 1.0;
        final IVector dimensions = pistonBoundary.getDimensions();
        for (int i=0; i<D; i++) {
            if (i != wallD) {
                area *= (dimensions.x(i)-collisionRadius*2.0);
            }
        }
        double s = virialSum / area;
        virialSum = 0.0;
        return s;
    }

    public double lastCollisionVirial() {return 0;}
    
    /**
     * not yet implemented.
     */
    public Tensor lastCollisionVirialTensor() {return null;}
    
    /**
     * Distance from the center of the sphere to the boundary at collision.
     */
    public void setCollisionRadius(double d) {
        if (d < 0) {
            throw new IllegalArgumentException("collision radius must not be negative");
        }
        collisionRadius = d;
    }
    /**
     * Distance from the center of the sphere to the boundary at collision.
     */
    public double getCollisionRadius() {return collisionRadius;}
    /**
     * Indicates collision radius has dimensions of Length.
     */
    public etomica.units.Dimension getCollisionRadiusDimension() {return etomica.units.Length.DIMENSION;}

    
    public void advanceAcrossTimeStep(double tStep) {
        if (pressure >= 0.0) {
            double area = 1.0;
            final IVector dimensions = pistonBoundary.getDimensions();
            for (int i=0; i<D; i++) {
                if (i != wallD) {
                    area *= (dimensions.x(i)-collisionRadius*2.0);
                }
            }
            force = pressure*area;
        }
        double a = force/wallMass;
        wallPosition += wallVelocity * tStep + 0.5*tStep*tStep*a;
        wallVelocity += tStep*a;
//        System.out.println("pressure => velocity "+a+" "+wallVelocity+" "+wallPosition+" "+tStep);
    }
    
    public void setThickness(double t) {
        thickness = t;
    }
    
    public void draw(java.awt.Graphics g, int[] origin, double toPixel) {
        g.setColor(java.awt.Color.gray);
        double dx = pistonBoundary.getDimensions().x(0);
        double dy = pistonBoundary.getDimensions().x(1);
        int xP = origin[0] + (wallD==0 ? (int)((wallPosition+0.5*dx-thickness)*toPixel) : 0);
        int yP = origin[1] + (wallD==1 ? (int)((wallPosition+0.5*dy-thickness)*toPixel) : 0);
        int t = Math.max(1,(int)(thickness*toPixel));
        int wP = wallD==0 ? t : (int)(toPixel*dx);
        int hP = wallD==1 ? t : (int)(toPixel*dy);
        g.fillRect(xP,yP,wP,hP);
    }
    
    private static final long serialVersionUID = 1L;
    private double collisionRadius = 0.0;
    private final int D;
    private final int wallD;
    private double wallPosition;
    private double wallVelocity;
    private double wallMass;
    private double setWallMass;
    private double force;
    private double pressure;
    private boolean isForced;
    private final IBoundary pistonBoundary;
    private double thickness = 0.0;
    private double virialSum;
    private boolean ignoreOverlap;
}
   
