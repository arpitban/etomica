package etomica.virial.simulations;

import etomica.*;
import etomica.graphics.*;
import etomica.virial.*;
import etomica.virial.cluster.Standard;
import etomica.virial.overlap.*;

/**
 * @author kofke
 *
 * Simulation implementing the overlap-sampling approach to evaluating a cluster
 * diagram.
 */
public class SimulationVirialOS extends SimulationGraphic {

	/**
	 * Default constructor, using a 3D space and a hard-sphere reference cluster
	 * with same connectivity as target cluster.
	 * @see java.lang.Object#Object()
	 */
	public SimulationVirialOS(double temperature, Cluster targetCluster, double sigmaHSRef) {
		this(temperature, targetCluster,
				new Cluster(new MayerHardSphere(sigmaHSRef), targetCluster));
	}
	
	public SimulationVirialOS(double temperature, Cluster targetCluster, Cluster refCluster) {
		this(new Space3D(), temperature, targetCluster, refCluster);
	}
	
	public SimulationVirialOS(Space space, double temperature, Cluster targetCluster, Cluster refCluster) {
		super(space);

		Default.makeLJDefaults();
		Default.TRUNCATE_POTENTIALS = false;
		
		
		int nMolecules = targetCluster.pointCount();
		simTemperature = temperature;
		double sigmaHSRef = 1.0*((MayerHardSphere)refCluster.bondGroup()[0].f).getSigma();
		boolean refPositive = !refCluster.hasOddBondCount();

///////// reference-system simulation				
		refSimulation = new SimulationGraphic(space);
		phase = new Phase(refSimulation);
		phase.setBoundary(space.makeBoundary(Space3D.Boundary.NONE));	
		species = new SpeciesSpheresMono(refSimulation);
		species.setNMolecules(nMolecules);
		species.setDiameter(sigmaHSRef);
		refSimulation.elementCoordinator.go();
		pairs = new PairSet(((AtomTreeNodeGroup)phase.getAgent(species).node).childList);

		refCluster.setPairSet(pairs);
		targetCluster.setPairSet(pairs);		
		
		Controller controller = new Controller(refSimulation);		
		DeviceTrioControllerButton controlPanel = new DeviceTrioControllerButton(refSimulation);
		integrator = new IntegratorMC(refSimulation);
		integrator.setSleepPeriod(1);
		integrator.setTemperature(simTemperature);
		MCMoveAtom mcMoveAtom1 = new MeterVirial.MyMCMoveAtom(integrator);
		MCMoveAtomMulti mcMoveAtom2 = new MCMoveAtomMulti(integrator,2);
		for(int n=3; n<nMolecules; n++) {
			new MCMoveAtomMulti(integrator, n);
		}
		
		//set up simulation potential for reference cluster
		P2ClusterSigned p2 = new P2ClusterSigned(refSimulation.hamiltonian.potential, pairs);
		p2.setCluster(refCluster);
		p2.setSignPositive(refPositive);
		p2.setTemperature(simTemperature);			

	  boolean simulatingTarget = false;
	  boolean targetPositive = false;
	  Cluster simCluster = simulatingTarget ? targetCluster : refCluster;
	  Cluster nonSimCluster = simulatingTarget ? refCluster : targetCluster;
	
	  ConfigurationCluster configuration = new ConfigurationCluster(refSimulation);
	  configuration.setPhase(phase);
	  configuration.setCluster(simCluster);
	  configuration.setSignPositive(simulatingTarget ? targetPositive : refPositive);
	  phase.setConfiguration(configuration);						
			
	  MeterOverlapReference meter = new MeterOverlapReference(refSimulation, simCluster, nonSimCluster);
	  meter.setTemperature(simTemperature);
	  meter.setActive(true);
		
	  DisplayPlot clusterPlot = new DisplayPlot(refSimulation);
	  clusterPlot.setDataSources(meter.allMeters());
	  clusterPlot.setWhichValue(MeterAbstract.AVERAGE);
		
	  refSimulation.elementCoordinator.go();
	  clusterPlot.setDataSources(meter.allMeters());
	  clusterPlot.setLabel("Reference");
//	  bPlot.setDataSources(bMeter.getHistory());
	
/////////////////

		DisplayPhase display = new DisplayPhase();
		ColorSchemeByType.setColor(species, java.awt.Color.green);
		
		refSimulation.elementCoordinator.go();
		
	}
	
	/**
	 * Returns the separation r for the LJ potential at which beta*u(r) = -Ln(2)
	 * (so that f(r) = 1)
	 * @param beta
	 * @return double
	 */
	public static double sigmaLJ1B(double beta) {
		double log2 = Math.log(2.0);
		if(beta <= log2) return Math.pow(2.0, 1./6.);
		else return Math.pow( 2.0*(beta + Math.sqrt(beta*(beta-log2)) )/log2, 1./6.);
	}
	private MeterOverlap meter;
	private double simTemperature;
	private double refTemperature;
	private PairSet pairs;
	private P2Cluster p2;
	private SpeciesSpheresMono species;
	protected IntegratorMC integrator;
	private Phase phase;
	public final SimulationGraphic refSimulation; 
	
	public Phase phase() {return phase;}
	
	public IntegratorMC integrator() {return integrator;}
	/**
	 * Returns the meterVirial.
	 * @return MeterVirial
	 */
	public MeterOverlap getMeter() {
		return meter;
	}

	/**
	 * Returns the refTemperature.
	 * @return double
	 */
	public double getRefTemperature() {
		return refTemperature;
	}

	/**
	 * Returns the simTemperature.
	 * @return double
	 */
	public double getSimTemperature() {
		return simTemperature;
	}

	/**
	 * Sets the refTemperature.
	 * @param refTemperature The refTemperature to set
	 */
	public void setRefTemperature(double refTemperature) {
		this.refTemperature = refTemperature;
	}

	/**
	 * Sets the simTemperature.
	 * @param simTemperature The simTemperature to set
	 */
	public void setSimTemperature(double simTemperature) {
		this.simTemperature = simTemperature;
	}
	
	public PairSet pairs() {
		return pairs;
	}
		
	public P2Cluster getSimPotential() {
		return p2;
	}
	
	public void setSimPotential(P2Cluster p2) {
		this.p2 = p2;
	}
	
	public SpeciesSpheresMono species() {
		return species;
	}
		
	public static void main(String[] args) {
		Default.makeLJDefaults();
		Default.TRUNCATE_POTENTIALS = false;

		double temperature = 1.3; //temperature governing sampling of configurations
		double sigmaHSRef = 1.0*sigmaLJ1B(1.0/temperature);  //diameter of reference HS system
		double b0 = 2*Math.PI/3. * Math.pow(sigmaHSRef,3);
		System.out.println("sigmaHSRef: "+sigmaHSRef);
		System.out.println("b0: "+b0);
		System.out.println("B3HS: "+(-5./8.*b0*b0));
//		sigmaHSRef = 1.0;
		int nMolecules = 6;
		
		P2LennardJones p2LJ = new P2LennardJones(Simulation.instance.hamiltonian.potential);//parent of this potential will not be connected to the simulation
		MayerGeneral f = new MayerGeneral(p2LJ);
		Cluster targetCluster = new etomica.virial.cluster.Ring(nMolecules, 1.0, f);
//		Cluster targetCluster = new etomica.virial.cluster.ReeHoover(4, 1.0, new Cluster.BondGroup(f, Standard.D4));

		SimulationVirialOS sim = new SimulationVirialOS(temperature, targetCluster, sigmaHSRef);
		sim.integrator.setDoSleep(false);
		
		
		sim.refSimulation.makeAndDisplayFrame();
//		sim.elementCoordinator.go();
	}//end of main
}
