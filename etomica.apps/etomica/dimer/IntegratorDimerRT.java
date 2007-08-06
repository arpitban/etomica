package etomica.dimer;

import etomica.atom.AtomAgentManager;
import etomica.atom.AtomSet;
import etomica.atom.AtomTypeLeaf;
import etomica.atom.IAtom;
import etomica.atom.IAtomPositioned;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.atom.iterator.IteratorDirective;
import etomica.box.Box;
import etomica.exception.ConfigurationOverlapException;
import etomica.integrator.IntegratorBox;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialMaster;
import etomica.simulation.ISimulation;
import etomica.space.IVector;
import etomica.space.IVectorRandom;
import etomica.species.Species;
import etomica.util.IRandom;

/**
 * 	Henkelman's Dimer Method (Rotation and Translation).
 * 
 * 	 | Finding saddle points on high dimensional surfaces.
 * 	 |    J. Chem. Phys., Vol. 111, No. 15, 15 October 1999.
 * 
 * 	@author msellers
 */

public class IntegratorDimerRT extends IntegratorBox implements AgentSource {

	public Box box1, box2;
	public ISimulation sim;
	public double deltaT;
	public double deltaR;
	public double dTheta, dR, dRsquared;
	public double deltaTheta;
	public double curvature;
	public double Frot, Fprimerot, dFrot;
	public double sinDtheta, cosDtheta;
	public double saddleT;
	public double dFsq;
	public int counter, rotCounter;
	public boolean rotate;
	public IVector [] THETA, THETAstar;
	public IVector [] F, F1, F2;
	public IVector [] Fperp, F1perp, F2perp;
	public IVector [] Fstar, F1star, F2star, Fstarperp;
	public IVector [] Feff, Fr, Fpara;
	public IVector [] deltaV, V;
	public IVectorRandom [] N, Nstar;
	public IVector NDelta, NstarDelta;
	public IVector workVector1;
	public IVector workVector2;
	public IRandom random1;
	public PotentialCalculationForceSum force0, force1, force2;
	public AtomAgentManager atomAgent0, atomAgent1, atomAgent2;
	public IteratorDirective allatoms;
	
	
	public IntegratorDimerRT(ISimulation sim, PotentialMaster potentialMaster) {
		this(sim, potentialMaster, sim.getRandom(), 0.05, 1.0, sim.getBoxs()[0]);
	}
	
	public IntegratorDimerRT(ISimulation aSim, PotentialMaster potentialMaster, IRandom random, double timeStep, double temperature, Box abox) {
		super(potentialMaster, temperature);
		this.random1 = random;
		this.sim = aSim;
		this.force0 = new PotentialCalculationForceSum();
		this.force1 = new PotentialCalculationForceSum();
		this.force2 = new PotentialCalculationForceSum();
		this.allatoms = new IteratorDirective();
		this.box = abox;
		this.deltaT = timeStep;
		
		deltaR = 1E-2;
		dTheta = 1E-4;
		dFsq = 0.00000001;
		dR = 0.1;
		dRsquared = dR*dR;
		dFrot = 0.01;
		counter = 0;
		rotCounter = 0;
		Frot = 1;
		rotate = true;
		
		sinDtheta = Math.sin(dTheta)*deltaR;
		cosDtheta = Math.cos(dTheta)*deltaR;
		
		N = new IVectorRandom [box.atomCount()];
		Nstar = new IVectorRandom [box.atomCount()];
		THETA = new IVector [box.atomCount()];
		THETAstar = new IVector [box.atomCount()];
		F = new IVector [box.atomCount()];
		F1 = new IVector [box.atomCount()];
		F2 = new IVector [box.atomCount()];
		Fperp = new IVector [box.atomCount()];
		F1perp = new IVector [box.atomCount()];
		F2perp = new IVector [box.atomCount()];
		Fstar = new IVector [box.atomCount()];
		F1star = new IVector [box.atomCount()];
		F2star = new IVector [box.atomCount()];
		Fstarperp = new IVector [box.atomCount()];
		Feff = new IVector [box.atomCount()];
		Fr = new IVector [box.atomCount()];
		Fpara = new IVector [box.atomCount()];
		deltaV = new IVector [box.atomCount()];
		V = new IVector [box.atomCount()];
		
		for(int i=0; i<box.atomCount(); i++){
			N[i] = (IVectorRandom)box.getSpace().makeVector();
			Nstar[i] = (IVectorRandom)box.getSpace().makeVector();
			NDelta = box.getSpace().makeVector();
			NstarDelta = box.getSpace().makeVector();
			THETA[i] = box.getSpace().makeVector();
			THETAstar[i] = box.getSpace().makeVector();
			F[i] = box.getSpace().makeVector();
			F1[i] = box.getSpace().makeVector();
			F2[i] = box.getSpace().makeVector();
			Fperp[i] = box.getSpace().makeVector();
			F1perp[i] = box.getSpace().makeVector();
			F2perp[i] = box.getSpace().makeVector();
			Fstar[i] = box.getSpace().makeVector();
			F1star[i] = box.getSpace().makeVector();
			F2star[i] = box.getSpace().makeVector();
			Fstarperp[i] = box.getSpace().makeVector();
			Feff[i] = box.getSpace().makeVector();
			Fr[i] = box.getSpace().makeVector();
			Fpara[i] = box.getSpace().makeVector();
			deltaV[i] = box.getSpace().makeVector();
			V[i] = box.getSpace().makeVector();
			workVector1 = box.getSpace().makeVector();
			workVector2 = box.getSpace().makeVector();
		}
		
	}
	
	
	public void doStepInternal(){
		
		rotateDimerNewton();
		
		rotCounter=0;
		
		translateDimerQuickmin();
		
		if(counter%10==0){
			System.out.println(((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition().x(0)+"     "+((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition().x(1)
					+"     "+((IAtomPositioned)box1.getLeafList().getAtom(0)).getPosition().x(0)+"     "+((IAtomPositioned)box1.getLeafList().getAtom(0)).getPosition().x(1)
					+"     "+((IAtomPositioned)box2.getLeafList().getAtom(0)).getPosition().x(0)+"     "+((IAtomPositioned)box2.getLeafList().getAtom(0)).getPosition().x(1));
		
			dimerSaddleTolerance();
		
		}
		
		counter++;
	
	}
			
	// Takes in a current configuration of atoms (Rc) and creates a dimer of their positions (R1 and R2).
	// (R2)-------[Rc]-------(R1) ===> N
	protected void setup() throws ConfigurationOverlapException{
		super.setup();
		
		// Use random unit array for N to generate dimer.

		// Normalize N
		double mag = 0;
		for (int i=0; i<N.length; i++){ 
			N[i].setRandomSphere(random1);
			mag += N[i].squared();
		}
		
		mag = 1.0 / Math.sqrt(mag);
		for (int i=0; i<N.length; i++){
			N[i].Ea1Tv1(mag, N[i]);
		}
		
		//Offset R1 and R2 (dimer ends) from initial configuration, along N.
		box1 = new Box(box.getBoundary());
		box2 = new Box(box.getBoundary());
		
		sim.addBox(box1);
		sim.addBox(box2);
		
		atomAgent0 = new AtomAgentManager(this, box);
		atomAgent1 = new AtomAgentManager(this, box1);
		atomAgent2 = new AtomAgentManager(this, box2);
		
		force0.setAgentManager(atomAgent0);
		force1.setAgentManager(atomAgent1);
		force2.setAgentManager(atomAgent2);
		
		Species [] species = sim.getSpeciesManager().getSpecies();
		
		for(int i=0; i<species.length; i++){
			box1.setNMolecules(species[i], box.getNMolecules(species[i]));
			box2.setNMolecules(species[i], box.getNMolecules(species[i]));
		}
		
		AtomSet list = box.getLeafList();
		AtomSet list1 = box1.getLeafList();
		AtomSet list2 = box2.getLeafList();

		for(int i=0; i<box.atomCount(); i++){
			((IAtomPositioned)list1.getAtom(i)).getPosition().E(((IAtomPositioned)list.getAtom(i)).getPosition());
			((IAtomPositioned)list1.getAtom(i)).getPosition().PEa1Tv1(deltaR, N[i]);
			
			((IAtomPositioned)list2.getAtom(i)).getPosition().E(((IAtomPositioned)list.getAtom(i)).getPosition());
			((IAtomPositioned)list2.getAtom(i)).getPosition().PEa1Tv1(-deltaR, N[i]);
		}
		
		// Write out initial configuration
		System.out.println(N[0].x(0)+"     "+N[0].x(1));
		
		
		System.out.println(((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition().x(0)+"     "+((IAtomPositioned)box.getLeafList().getAtom(0)).getPosition().x(1)
				+"     "+((IAtomPositioned)box1.getLeafList().getAtom(0)).getPosition().x(0)+"     "+((IAtomPositioned)box1.getLeafList().getAtom(0)).getPosition().x(1)
				+"     "+((IAtomPositioned)box2.getLeafList().getAtom(0)).getPosition().x(0)+"     "+((IAtomPositioned)box2.getLeafList().getAtom(0)).getPosition().x(1));
		
		// Calculate F's
		dimerForces(F1, F2, F);
	}
	
	// Rotate the dimer to align it with the lowest curvature mode of the potential energy surface, NEWTON STYLE.
	public void rotateDimerNewton(){
		
		while(rotate){
			
			// Calculate F|_
			dimerForcePerp(N, F1, F2, Fperp);
			
			// Find THETA and normalize
			double mag = 0;
			for(int i=0; i<Fperp.length; i++){
				THETA[i].E(Fperp[i]);
				mag += THETA[i].squared();
			}
			mag = 1.0 / Math.sqrt(mag);
			
			for(int i=0; i<THETA.length; i++){
				THETA[i].Ea1Tv1(mag, THETA[i]);
			}
			
			// Find R*'s
			for(int i=0; i<box.atomCount(); i++){
				IVector workVector;
				
				NstarDelta.Ea1Tv1(cosDtheta, N[i]);
				NstarDelta.PEa1Tv1(sinDtheta, THETA[i]);
							
				// R1*
				workVector = ((IAtomPositioned)box1.getLeafList().getAtom(i)).getPosition();
				workVector.E(((IAtomPositioned)box.getLeafList().getAtom(i)).getPosition());
				workVector.PE(NstarDelta);
				
				// R2*
				workVector = ((IAtomPositioned)box2.getLeafList().getAtom(i)).getPosition();
				workVector.E(((IAtomPositioned)box.getLeafList().getAtom(i)).getPosition());
				workVector.ME(NstarDelta);
				
				// Find N*
				Nstar[i].Ea1Tv1(1.0/deltaR, NstarDelta);
			}
			
			// Calculate F*'s
			dimerForces(F1star, F2star, Fstar);
			
			// Calculate F*|_
			dimerForcePerp(Nstar, F1star, F2star, Fstarperp);
			
			// Find THETA* and normalize
			mag = 0;
			for(int i=0; i<Fstarperp.length; i++){
				THETAstar[i].E(Fstarperp[i]);
				mag += THETAstar[i].squared();
			}
			mag = 1.0 / Math.sqrt(mag);
			
			for(int i=0; i<THETAstar.length; i++){
				THETAstar[i].Ea1Tv1(mag, THETAstar[i]);
			}
			
			// Compute scalar rotational force and change in rotational force over length dTheta
			Frot = 0;
			Fprimerot = 0;
			for(int i=0; i<Fperp.length; i++){
				Frot += Fperp[i].dot(THETA[i]);
				Frot += Fstarperp[i].dot(THETAstar[i]);
			}
			Frot = Frot/2.0;
			
			for(int i=0; i<Fstar.length; i++){
				Fprimerot += Fstarperp[i].dot(THETAstar[i]);
			}
			for(int i=0; i<Fstar.length; i++){
				Fprimerot -= Fperp[i].dot(THETA[i]);
			}
			
			Fprimerot = Fprimerot / dTheta;
			
			
			// Find actual rotation angle to minimize energy
			
			// Adatom on surface 
			deltaTheta = -0.5 * Math.atan2(2.0*Frot,Fprimerot) - dTheta/2.0;
			
			
			if(Fprimerot<0){deltaTheta = deltaTheta + Math.PI/2.0;}
			
			// Rotate dimer to new positions and set new N
			double sindeltaTheta = Math.sin(deltaTheta) * deltaR;
			double cosdeltaTheta = Math.cos(deltaTheta) * deltaR;
			
			for(int i=0; i<box.atomCount(); i++){
				IVector workVector;
				
				NDelta.Ea1Tv1(cosdeltaTheta, N[i]);
				NDelta.PEa1Tv1(sindeltaTheta, THETA[i]);
							
				// R1**
				workVector = ((IAtomPositioned)box1.getLeafList().getAtom(i)).getPosition();
				workVector.E(((IAtomPositioned)box.getLeafList().getAtom(i)).getPosition());
				workVector.PE(NDelta);
				
				// R2**
				workVector = ((IAtomPositioned)box2.getLeafList().getAtom(i)).getPosition();
				workVector.E(((IAtomPositioned)box.getLeafList().getAtom(i)).getPosition());
				workVector.ME(NDelta);
			}
			
			// Calculate new F's
			dimerForces(F1, F2, F);
			
			// Calculate new Normal
			dimerNormal();
			
			rotCounter++;
			
			if(Frot<dFrot){rotate=false;}
			if(rotCounter>3){rotate=false;}
		}
		//Reset rotate
		rotate=true;
	}
	
	// Moves the dimer along the lowest curvature mode of the energy surface, Quickmin style.  Runs after rotateDimer[X]().
	public void translateDimerQuickmin(){
	
		// Calculate curvature value
		dimerCurvature(N, F1, F2);
		
		// Calculate F||
		dimerForcePara(N, F, Fpara);
		
		// Calculate Feff
		dimerForceEff(Fpara, Feff);
		
		// Calculate Velocity
		dimerGetVelocity();
		
		// Move Dimer
		dimerUpdatePositions();
		
		// Calculate new F's
		dimerForces(F1, F2, F);
		
		// Calculate new Normal
		dimerNormal();
		
	}
	
	// Compute curvature value, C, for the energy surface
	protected double dimerCurvature(IVectorRandom [] aN, IVector [] aF1, IVector [] aF2){
		curvature = 0.0;
		
		// (F2 - F1)[dot]N / 2*deltaR
		for(int i=0; i<aF1.length; i++){
			workVector1.E(aF2[i]);
			workVector1.ME(aF1[i]);
			
			curvature += workVector1.dot(aN[i]);
		}
		
		curvature = 0.5 * curvature / deltaR;		
		return curvature;
	}
	
	// Compute Normal vector for dimer orientation
	protected void dimerNormal(){
	
		IVector workvector;
		workvector = box.getSpace().makeVector();
		
		// N =  (R2 - R1) / (-2*deltaR)
		for (int i=0; i<box1.atomCount(); i++){	
		
			workvector.E(((IAtomPositioned)box2.getLeafList().getAtom(i)).getPosition());
			workvector.ME(((IAtomPositioned)box1.getLeafList().getAtom(i)).getPosition());
			
			N[i].Ea1Tv1(-1.0/(2.0*deltaR), workvector);
		}
	}
		
	// Reset forces in boxes 1 and 2, call calculate, and copy over new forces
	protected void dimerForces(IVector [] aF1, IVector [] aF2, IVector [] aF){
		force1.reset();
		force2.reset();
		
		potential.calculate(box1, allatoms, force1);
		potential.calculate(box2, allatoms, force2);
		
		// Copy forces of dimer ends (R1, R2) to local array
		for(int i=0; i<box.atomCount(); i++){
			aF1[i].E(((IntegratorVelocityVerlet.MyAgent)atomAgent1.getAgent(box1.getLeafList().getAtom(i))).force());
			aF2[i].E(((IntegratorVelocityVerlet.MyAgent)atomAgent2.getAgent(box2.getLeafList().getAtom(i))).force());
			aF[i].Ev1Pv2(aF1[i], aF2[i]);
			aF[i].Ea1Tv1(0.5, aF[i]);
		}
	}
	
	// Calculate the force, aF, perpendicular to dimer orientation N.
	protected void dimerForcePerp(IVectorRandom [] aN, IVector [] aF1, IVector [] aF2, IVector [] aFperp){
		
		double mag1 = 0;
		double mag2 = 0;
		
		// F|_ = F1 - (F1[dot]N)*N  - F2  +  (F2[dot]N)*N
		for(int i=0; i<aF1.length; i++){
			mag1 += aF1[i].dot(aN[i]);
			mag2 += aF2[i].dot(aN[i]);
		}
		for (int i=0; i<aFperp.length; i++){
			aFperp[i].E(aF1[i]);
			aFperp[i].PEa1Tv1(-mag1, aN[i]);	
			aFperp[i].ME(aF2[i]);
			aFperp[i].PEa1Tv1(mag2, aN[i]);
			aFperp[i].TE(1.0/deltaR);
		}

	}
	
	// Calculate the net force, aF, parallel to dimer orientation N.
	protected void dimerForcePara(IVectorRandom [] aN, IVector [] aF, IVector [] aFpara){
		
		double mag = 0;
		IVector workvector;
		workvector = box.getSpace().makeVector();
		
		// F|| = (F1[dot]N)*N +  (F2[dot]N)*N
		for(int i=0; i<aF.length; i++){
			workvector.E(aF[i]);
			mag += workvector.dot(aN[i]);
		}
		for (int i=0; i<aFpara.length; i++){
			aFpara[i].Ea1Tv1(mag, aN[i]);
		}

	}
	
	// Calculate the effective force on the dimer using curvature and F1, F2
	protected void dimerForceEff(IVector [] aF, IVector [] aFeff){
		
		// Feff = F - 2F||
		if(curvature<0){
			
			for(int i=0; i<aF.length; i++){
				aFeff[i].Ea1Tv1(-2.0, Fpara[i]);
				aFeff[i].PE(F[i]);
			}
		}
		// Feff = -F||
		else{
			for(int i=0; i<aFeff.length; i++){
				aFeff[i].Ea1Tv1(-1.0, Fpara[i]);
			}
		}
	}
	
	// Calculate new velocity from Feff and timestep
	protected void dimerGetVelocity(){
		// Calculate deltaV
		for(int i=0; i<Feff.length; i++){
			deltaV[i].Ea1Tv1(deltaT/((AtomTypeLeaf)box.getLeafList().getAtom(i).getType()).getMass(), Feff[i]);
		}
		
		// Calculate and check new Vi
		double numer = 0.0;
		double denomer = 0.0;
		double mag = 0.0;
		
		// Use previous Vi
		for(int i=0; i<deltaV.length; i++){
			numer += deltaV[i].dot(V[i]);
			denomer += deltaV[i].squared();
		}
		// Calculate new Vi
		for(int i=0; i<deltaV.length; i++){
			V[i].E(deltaV[i]);
			V[i].PEa1Tv1(numer/denomer, deltaV[i]);
			mag += V[i].dot(Feff[i]);
		}
		
		// Check if Vi[dot]Feff < 0
		if(mag<0){
			for(int i=0; i<deltaV.length; i++){
				V[i].E(deltaV[i]);
			}
		}
	}
	
	// Update positions according to Verlet algorithm.
	protected void dimerUpdatePositions(){
		
		IVector workvector;
		
		// r(t + dt) = r(t) + dt*v(t) + dt^2 * Feff / m / 2
		for(int i=0; i<box.atomCount(); i++){
			
			workVector2.Ea1Tv1(deltaT, V[i]);
			workVector2.PEa1Tv1(deltaT/2.0, deltaV[i]);
			
			// Update positions in all three boxes
			workvector = ((IAtomPositioned)box.getLeafList().getAtom(i)).getPosition();
			workvector.PE(workVector2);
			workvector = ((IAtomPositioned)box1.getLeafList().getAtom(i)).getPosition();
			workvector.PE(workVector2);
			workvector = ((IAtomPositioned)box2.getLeafList().getAtom(i)).getPosition();
			workvector.PE(workVector2);
		
		
		}	
		
		
	}
	
	// Calculates and checks magnitude of 3N dimensional force vector
	protected void dimerSaddleTolerance(){
		saddleT = 0.0;
		
		force0.reset();
		potential.calculate(box, allatoms, force0);
		
		for(int i=0; i<F.length; i++){
			saddleT += ((IntegratorVelocityVerlet.MyAgent)atomAgent0.getAgent(box.getLeafList().getAtom(i))).force().squared();
		}
		
		// Dimer has reached saddle point, write out relevant data
	 if(saddleT < dFsq ){
			System.out.println("Saddle Point Reached");
			System.out.println(counter+"   "+saddleT);
			System.exit(1);
		}
			
	}
	
	public Class getAgentClass() {
		return IntegratorVelocityVerlet.MyAgent.class;
	}

	public Object makeAgent(IAtom a) {
		return new IntegratorVelocityVerlet.MyAgent(box.getSpace());
	}

	public void releaseAgent(Object agent, IAtom atom) {
		// TODO Auto-generated method stub	
	}
	

}
