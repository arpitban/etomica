package etomica.threaded;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IPotentialAtomic;
import etomica.potential.Potential;
import etomica.space.ISpace;

public class PotentialThreaded extends Potential {

	final protected IPotentialAtomic[] potential;
	
	public PotentialThreaded(ISpace space, IPotentialAtomic[] potential) {
		super(potential[0].nBody(), space);
		this.potential = potential;
	
	}

	public double energy(IAtomList atoms) {
		//Only the energy from one thread (a partition of atoms)
		return potential[0].energy(atoms);
	}

	public double getRange() {
		
		return potential[0].getRange();
	}

	public void setBox(IBox box) {
		
		for(int i=0; i<potential.length; i++){
			potential[i].setBox(box);
		}

	}
	
	public IPotentialAtomic[] getPotentials(){
		return potential;
	}

}
