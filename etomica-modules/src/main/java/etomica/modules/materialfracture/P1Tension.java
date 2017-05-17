/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.materialfracture;
import etomica.api.IAtomList;
import etomica.box.Box;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.potential.PotentialSoft;
import etomica.space.Space;
import etomica.space.Tensor;
import etomica.units.Null;

public class P1Tension implements PotentialSoft {
    
    protected final Space space;
    protected double w;
    protected final IVectorMutable[] force;
    protected Box box;
    
    public P1Tension(Space space) {
        this.space = space;
        force = new IVectorMutable[1];
        force[0] = space.makeVector();
        setSpringConstant(0.0);
    }
    
    public int nBody() {
        return 1;
    }
    
    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }
    
    public void setSpringConstant(double springConstant) {w = springConstant;}
    public double getSpringConstant() {return w;}
    
    /**
     * Not implemented correctly.  
     * Returns dimensionless for spring constant.  Should be energy/length^2.
     */
    public etomica.units.Dimension getSpringConstantDimension() {
        return Null.DIMENSION;
    }
    
    public void setBox(Box newBox) {
        box = newBox;
    }

    public double energy(IAtomList a) {
        IVectorMutable r = a.getAtom(0).getPosition();
        double aSum = 0.0;
        double x = r.getX(0);
        aSum += x*x;
        return 0.5*w*aSum;
    }

    public IVector[] gradient(IAtomList a, Tensor t){
        return gradient(a);
    }
    
    public IVector[] gradient(IAtomList a) {
        IVectorMutable r = a.getAtom(0).getPosition();
        force[0].setX(1, 0);
        double x = r.getX(0);
        force[0].setX(0,-w*x);
        return force;
    }
    
    public double virial (IAtomList a) {
        return 0;
    }
}
   
