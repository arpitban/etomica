package simulate;

public class PotentialTether extends Potential {

  private double tetherLength, tetherLengthSquared;

  public PotentialTether() {
    setTetherLength(0.1);
  }

  public final double getTetherLength() {return tetherLength;}
  public final void setTetherLength(double t) {
      tetherLength = t;
      tetherLengthSquared = t*t;
  }

//----------------------------------------------------------------------

  public final void bump(Atom atom1, Atom atom2) {
    space.uEr1Mr2(r12,atom2.r,atom1.r);  //use instance method   //r2-r1
    Space.uEa1Tv1Ma2Tv2(v12,atom2.rm,atom2.p,atom1.rm,atom1.p);  //v2-v1
    double r2 = tetherLengthSquared;
    double bij = Space.v1Dv2(v12, r12);
    double reduced_m = 1.0/((1.0/atom1.rm+ 1.0/atom2.rm)*atom1.rm*atom2.rm);
    double s = 2.0*reduced_m*bij/r2;  //same even if an inner-shell collision
    Space.uPEa1Tv1(atom1.p, s, r12);
    Space.uMEa1Tv1(atom2.p, s, r12);
  }

//----------------------------------------------------------------------

  public final double collisionTime(Atom atom1, Atom atom2) {

    space.uEr1Mr2(r12,atom2.r,atom1.r);  //use instance method   //r2-r1
    Space.uEa1Tv1Ma2Tv2(v12,atom2.rm,atom2.p,atom1.rm,atom1.p);  //v2-v1
    double bij = Space.v1Dv2(r12,v12);                           //r12 . v12
    double r2 = Space.v1S(r12);
    double v2 = Space.v1S(v12);

    double tij = Double.MAX_VALUE;
    double discr = bij*bij - v2 * ( r2 - tetherLengthSquared );
    return (-bij + Math.sqrt(discr))/v2;

/*    if(bij < 0.0) {    // Check for hard-core collision
	    discr = bij*bij - v2 * ( r2 - coreDiameterSquared );
	    if(discr > 0) {  // Hard cores collide next
	      tij = (-bij - Math.sqrt(discr))/v2;
	    }
	    else {           // Moving toward each other, but wells collide next
	      discr = bij*bij - v2 * ( r2 - wellDiameterSquared );
	      tij = (-bij + Math.sqrt(discr))/v2;
	    }
      }
      else {           // Moving away from each other, wells collide next
	    if(r2 < 0.9999*wellDiameterSquared) {   // Did not just have a well collision
	      discr = bij*bij - v2 * ( r2 - wellDiameterSquared );  // This is always > 0
	      tij = (-bij + Math.sqrt(discr))/v2;
	    }
      }
    }*/
  }
  
    public double energy(Atom atom1, Atom atom2) {
        return (space.r1Mr2_S(atom1.r, atom2.r) > tetherLengthSquared) ? Double.MAX_VALUE : 0.0;
    }
  
}
  