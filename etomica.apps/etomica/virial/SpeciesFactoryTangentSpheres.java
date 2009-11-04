package etomica.virial;

import etomica.api.ISpecies;
import etomica.chem.elements.ElementSimple;
import etomica.config.IConformation;
import etomica.space.ISpace;
import etomica.species.SpeciesSpheres;

/**
 * SpeciesFactory that makes a tangent sphere species.
 */
public class SpeciesFactoryTangentSpheres implements SpeciesFactory, java.io.Serializable {
    public SpeciesFactoryTangentSpheres(int nA, IConformation conformation) {
        this.nA = nA;
        this.conformation = conformation;
    }
    
    public ISpecies makeSpecies(ISpace space) {
        return new SpeciesSpheres(nA, new ElementSimple("TS"), conformation, space);
    }
    
    private static final long serialVersionUID = 1L;
    private final int nA;
    private final IConformation conformation;
}
