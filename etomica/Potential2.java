package simulate; 
import java.awt.Component;
import java.awt.Graphics;

public abstract class Potential2 extends Component {

  int species1Index, species2Index;
  Potential[][] potential;
  int nAtoms1, nAtoms2;
  double skinThickness, potentialCutoff, neighborRadius, squareNeighborRadius;

  public Potential2() {
    species1Index = species2Index = 0;
    neighborRadius = Double.MAX_VALUE;
    potentialCutoff = Double.MAX_VALUE;
    setSize(60,60);
  }
  
  public abstract Potential getPotential(Atom a1, Atom a2);
  
  public boolean isNeighbor(Molecule m1, Molecule m2) {return true;}
  
  public void setPhaseSpace(PhaseSpace p) {
    for(int i1=0; i1<nAtoms1; i1++) {
        for(int i2=0; i2<nAtoms2; i2++) {
            potential[i1][i2].setParentPhase(p);
        }
    }
  }
  
  public final double getNeighborRadius() {return neighborRadius;}
  
  public final double getSkinThickness() {return skinThickness;}
  public final void setSkinThickness(double s) {
    skinThickness = s;
    neighborRadius = potentialCutoff + skinThickness;
    squareNeighborRadius = neighborRadius * neighborRadius;
  }
  
  public final void setPotentialCutoff(double d) {
    potentialCutoff = d;
    setSkinThickness(skinThickness);  //update neighborRadius
  }
  
  public int getSpecies1Index() {return this.species1Index;}  
  public void setSpecies1Index(int index) {this.species1Index = index;}
  
  public int getSpecies2Index() {return this.species2Index;}
  public void setSpecies2Index(int index) {this.species2Index = index;}
  
  public void paint(Graphics g) {;}
}


