package etomica.action;

import etomica.atom.IAtom;
import etomica.phase.Phase;
import etomica.space.BoundaryDeformablePeriodic;
import etomica.space.IVector;
import etomica.space.Space;
import etomica.space.Tensor;

/**
 * Performs actions that cause volume of a deformable system to expand, with molecule
 * positions scaled to keep them in the same relative positions. Inflation can
 * be isotropically or anisotropically.
 * 
 * @author cribbin
 */
public class PhaseInflateDeformable extends PhaseInflate{

    public PhaseInflateDeformable(Space space){
        super(space);
        setup();
    }
    
    public PhaseInflateDeformable(Phase phase){
        super(phase.getSpace());
        setPhase(phase);
        setup();
    }
    
    /*
     * Method that does the setup needed; used by the constructors.
     * This makes sure we only need to make changes one place.
     */
    private void setup(){
        tempTens = phase.getSpace().makeTensor();
        tempTensInv = phase.getSpace().makeTensor();
    }
    
    /**
     * Performs isotropic inflation.
     */
    public void actionPerformed() {
        if(phase == null) return;
        
        //First scale the locations of the molecules.
        
        //get the edge vectors, and invert the tensor with them in it.
        tempTensInv = ((BoundaryDeformablePeriodic)phase.getBoundary()).boundaryTensor();
        tempTensInv.inverse();
        tempTens = ((BoundaryDeformablePeriodic)phase.getBoundary()).boundaryTensor();
        
        /*
         * convert the location of each molecule from x,y,z coordinates
         * into coordinates based on the edge vectors, scale, 
         * convert back, and scale the molecule
         */
        moleculeIterator.reset();
        IVector translationVector = translator.getTranslationVector();
        // substract 1 from each dimension so that multiplying by it yields
        // the amount each coordinate is to be translated *by* (not to).
        scaleVector.PE(-1.0);
        for(IAtom molecule = moleculeIterator.nextAtom(); molecule != null;){
            translationVector.E(moleculeCenter.position(molecule));
            tempTensInv.transform(translationVector);
            translationVector.TE(scaleVector);
            tempTens.transform(translationVector);
            groupScaler.actionPerformed(molecule);
        }
       
        //Reverse the subtraction to the scaleVector
        scaleVector.PE(1.0);
//      Then scale the boundary
        IVector dimensions = phase.getBoundary().getDimensions();
        dimensions.TE(scaleVector);
        phase.setDimensions(dimensions);
  
    }

    protected Tensor tempTens;
    protected Tensor tempTensInv;
}
