package etomica.models.water;
import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IConformation;
import etomica.space.ISpace;

/**
 * Conformation for 3-point water molecule.
 */
public class ConformationWater3P implements IConformation {

    public static final double bondLengthOH = 1.0;
    public static final double angleHOH = 109.5*Math.PI/180.;

    public ConformationWater3P(ISpace space) {
        this.space = space;
    }
    
    public void initializePositions(IAtomList list){
        
        IAtomPositioned o = (IAtomPositioned)list.getAtom(2);
        o.getPosition().E(new double[] {0, 0, 0.0});

        double x = bondLengthOH*Math.sin(0.5*angleHOH);
        double y = bondLengthOH*Math.cos(0.5*angleHOH);
        
        IAtomPositioned h1 = (IAtomPositioned)list.getAtom(0);
        h1.getPosition().E(new double[] {-x, y, 0.0});
                
        IAtomPositioned h2 = (IAtomPositioned)list.getAtom(1);
        h2.getPosition().E(new double[] {+x, y, 0.0});
    }
    
    private static final long serialVersionUID = 1L;
    protected final ISpace space;
}
