package etomica.virial;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IVectorMutable;
import etomica.config.IConformation;
import etomica.space.ISpace;

 /**
  *  Conformation for Phenanthrene
  *  Reference paper: Iwai
  * 3 site group H to the two side benzene rings , eihter 464 or 545 model
  * modified from ConformationAnthracene
 * @author Shu
 * March,9,2011
 */
public class ConformationPh3site implements IConformation, java.io.Serializable{
	
	public ConformationPh3site(ISpace space){
		this.space = space;
		vector = space.makeVector();
	}

	public void initializePositions(IAtomList atomList) {
			
		IAtom n1 = atomList.getAtom(SpeciesPh3site545.indexC1);
		n1.getPosition().E(new double[] {0, 0, 0});
		
		IAtom n2 = atomList.getAtom(SpeciesAnthracene3site545.indexCH1);
		n2.getPosition().E(new double[] {-bondlength, 0, 0});
		
		IAtom n3 = atomList.getAtom(SpeciesAnthracene3site545.indexCH2);
		n3.getPosition().E(new double[] {half_bondlength, minus_half_sqrt_bondlength, 0});
				
	}
		
	protected final ISpace space;
	protected static final double bondlength = 2.42;//converst nm to angstrom
	protected static final double half_bondlength = bondlength / 2;
	protected static final double minus_half_sqrt_bondlength = - Math.sqrt(3) / 2 * bondlength ; 
	protected IVectorMutable vector;
	
	private static final long serialVersionUID = 1L;
}