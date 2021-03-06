/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.chainequilibrium;

import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomLeafAgentManager.AgentSource;
import etomica.atom.AtomType;
import etomica.atom.IAtom;
import etomica.box.Box;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorMD.ThermostatType;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.molecule.IMoleculeList;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;

public class FreeRadicalPolymerizationSim extends Simulation implements AgentSource<IAtom[]> {

    public final PotentialMaster potentialMaster;
    public final ConfigurationLatticeFreeRadical config;
    public Controller controller1;
	public IntegratorHard integratorHard;
	public java.awt.Component display;
	public Box box;
	public SpeciesSpheresMono speciesA; // initiator
	public SpeciesSpheresMono speciesB; // monomer
	public P2SquareWellBonded p2AA;
	public P2SquareWellRadical p2AB, p2BB;
    public ActivityIntegrate activityIntegrate;
    public AtomLeafAgentManager<IAtom[]> agentManager = null;

    public FreeRadicalPolymerizationSim() {
        this(Space2D.getInstance());
    }
    
    public FreeRadicalPolymerizationSim(Space space) {
        super(space);

        speciesA = new SpeciesSpheresMono(this, space);
        speciesA.setIsDynamic(true);
        speciesB = new SpeciesSpheresMono(this, space);
        speciesB.setIsDynamic(true);
        addSpecies(speciesA);
        addSpecies(speciesB);

        potentialMaster = new PotentialMasterList(this, 3, space);
        ((PotentialMasterList) potentialMaster).setCellRange(1);

        controller1 = getController();

        double diameter = 1.0;
        double lambda = 2.0;

        box = this.makeBox(new BoundaryRectangularPeriodic(space, space.D() == 2 ? 60 : 20));

        box.setNMolecules(speciesA, 50);
        box.setNMolecules(speciesB, 100);
        config = new ConfigurationLatticeFreeRadical(space.D() == 2 ? new LatticeOrthorhombicHexagonal(space) : new LatticeCubicFcc(space), space, random);
        config.setSpecies(speciesA, speciesB);
        config.initializeCoordinates(box);

        integratorHard = new IntegratorHard(this, potentialMaster, box);
        integratorHard.setIsothermal(true);
        integratorHard.setTemperature(Kelvin.UNIT.toSim(300));
        integratorHard.setTimeStep(0.002);
        integratorHard.setThermostat(ThermostatType.ANDERSEN_SINGLE);
        integratorHard.setThermostatInterval(1);
        integratorHard.getEventManager().addListener(((PotentialMasterList) potentialMaster).getNeighborManager(box));

        agentManager = new AtomLeafAgentManager<IAtom[]>(this, box);
        resetBonds();

        //potentials
        p2AA = new P2SquareWellBonded(space, agentManager, diameter / lambda, lambda, 0);
        p2AB = new P2SquareWellRadical(space, agentManager, diameter / lambda, lambda, 0.0, random);
        p2BB = new P2SquareWellRadical(space, agentManager, diameter / lambda, lambda, 0.0, random);

        potentialMaster.addPotential(p2AA,
                new AtomType[]{speciesA.getLeafType(), speciesA.getLeafType()});
        potentialMaster.addPotential(p2AB,
                new AtomType[]{speciesA.getLeafType(), speciesB.getLeafType()});
        potentialMaster.addPotential(p2BB,
                new AtomType[]{speciesB.getLeafType(), speciesB.getLeafType()});

        // **** Setting Up the thermometer Meter *****

        activityIntegrate = new ActivityIntegrate(integratorHard, 1, true);
        getController().addAction(activityIntegrate);
    }
    
    public void resetBonds() {
        
        IMoleculeList initiators = box.getMoleculeList(speciesA);
        for (int i = 0; i<initiators.size(); i++) {
            IAtom initiator0 = initiators.get(i).getChildList().get(0);
            IAtom[] bonds0 = agentManager.getAgent(initiator0);
            if (i<initiators.size()-1) {
                i++;
                IAtom initiator1 = initiators.get(i).getChildList().get(0);
                IAtom[] bonds1 = agentManager.getAgent(initiator1);
                bonds0[0] = initiator1;
                bonds1[0] = initiator0;
            }
            else {
                bonds0[0] = null;
            }
        }
        
        IMoleculeList monomers = box.getMoleculeList(speciesB);
        for (int i = 0; i<monomers.size(); i++) {
            IAtom[] bonds = agentManager.getAgent(monomers.get(i).getChildList().get(0));
            bonds[0] = null;
            bonds[1] = null;
        }
    }

	/**
	 * Implementation of AtomAgentManager.AgentSource interface. Agent
     * is used to hold bonding partners.
	 */
	public IAtom[] makeAgent(IAtom a, Box agentBox) {
	    if (a.getType() == speciesB.getLeafType()) {
	        return new IAtom[2]; // monomer
	    }
		return new IAtom[1]; // initiator
	}
    
    public void releaseAgent(IAtom[] agent, IAtom atom, Box agentBox) {}
    
    public AtomLeafAgentManager<IAtom[]> getAgentManager() {
    	return agentManager;
    }
}
