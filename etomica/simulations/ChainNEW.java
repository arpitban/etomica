package etomica.simulations;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JPanel;

import etomica.Atom;
import etomica.AtomTreeNodeGroup;
import etomica.Controller;
import etomica.Default;
import etomica.Phase;
import etomica.PotentialGroup;
import etomica.Simulation;
import etomica.Species;
import etomica.SpeciesSpheres;
import etomica.SpeciesSpheresMono;
import etomica.atom.AtomFactoryHomo;
import etomica.atom.AtomFactoryMono;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.Device;
import etomica.graphics.DeviceTrioControllerButton;
import etomica.graphics.DisplayPhase;
import etomica.graphics.DisplayPhaseEvent;
import etomica.graphics.DisplayPhaseListener;
import etomica.graphics.DisplayTimer;
import etomica.graphics.Drawable;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHard;
import etomica.potential.P1TetheredHardSpheres;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.space.Vector;
import etomica.space2d.Space2D;

/**
 * Simple hard-sphere molecular dynamics simulation in 2D.
 *
 * @author David Kofke
 */
 
public class ChainNEW extends SimulationGraphic {
    
	public IntegratorHard integrator;
	public SpeciesSpheresMono species;
	public Phase phase;
	public Potential2 potential;
	public Controller controller;
	public DisplayPhase display;

	public ChainNEW() {
  //      super(new etomica.space.continuum.Space(2));
		super(new Space2D());
		Default.ATOM_SIZE = 1.0;
 //can't use cell list until integrator is updated for it      setIteratorFactory(new IteratorFactoryCell(this));
		Simulation.instance = this;
		integrator = new IntegratorHard(this);
//		integrator.setIsothermal(true);
		species = new SpeciesSpheresMono(this);
		SpeciesSpheres speciesChain = new SpeciesSpheres(1,10);
		species.setNMolecules(100);
		phase = new Phase(this);
		potential = new P2HardSphere();
		this.hamiltonian.potential.setSpecies(potential, new Species[] {species,species});
		P1TetheredHardSpheres potentialChainIntra = new P1TetheredHardSpheres();
		PotentialGroup p2Inter = new PotentialGroup(2, space);
		P2HardSphere chainSphere = new P2HardSphere(p2Inter);
		potentialChainIntra.p2Tether.setTetherLength(Default.ATOM_SIZE);
		hamiltonian.potential.setSpecies(potentialChainIntra, new Species[] {speciesChain});
		hamiltonian.potential.setSpecies(p2Inter, new Species[] {species, speciesChain});
		controller = new Controller(this);
		new DeviceTrioControllerButton(this, controller);
		display = new DisplayPhase(this);
		DisplayTimer timer = new DisplayTimer(integrator);
		timer.setUpdateInterval(10);
		ColorSchemeByType.setColor(species, java.awt.Color.red);
		ColorSchemeByType.setColor(((AtomFactoryMono)((AtomFactoryHomo)speciesChain.moleculeFactory()).childFactory()).getType(),java.awt.Color.blue);
//		panel().setBackground(java.awt.Color.yellow);
		DeviceKicker dk = new DeviceKicker(display);
//		elementCoordinator.go();
		
		Atom first = phase.getAgent(speciesChain).node.firstLeafAtom();
		Atom last = phase.getAgent(speciesChain).node.lastLeafAtom();
		first.coord.momentum().E(0.0);
		first.coord.setMass(Double.MAX_VALUE);
		Atom chain = ((AtomTreeNodeGroup)phase.getAgent(speciesChain).node).childList.getFirst();
		chain.coord.translateBy(new Vector(4,15));
		dk.setAtom(last);
		
	}
    
	public static class DeviceKicker extends Device implements Drawable {
    
		private DisplayPhase display;
		private boolean visible=false;
		private Atom target;
		private Vector impulse;
		private Vector targetpos;
		private Vector mousepos;
		private double impulsefactor;
		private double impulsefactor1 = .0051;

		public DeviceKicker(DisplayPhase display) {
			this(Simulation.instance, display);
		}
		public DeviceKicker(Simulation sim, DisplayPhase display) {
			super(sim);
			this.display = display;
			display.addDisplayPhaseListener(new Kicker());
			display.addDrawable(this);
			impulse = sim.space.makeVector();
			mousepos= (Vector) sim.space.makeVector();
			impulsefactor = 10;
			//impulsefactor = 50;
		}
    
		public void draw(java.awt.Graphics g, int[] origin, double scale)
		{
			if (!visible) return;
			g.setColor(Color.red);
			int x = origin[0]+(int)( targetpos.x(0) * scale*display.getToPixels());
			int y = origin[1]+(int)( targetpos.x(1) * scale*display.getToPixels());
			int xp = origin[0] + (int)(mousepos.x(0) *scale*display.getToPixels());
			int yp = origin[1] + (int)(mousepos.x(1) *scale*display.getToPixels());
			int radius = 25;
			g.drawOval(x-radius, y-radius, 2*radius, 2*radius);
			g.drawLine(x, y, xp, yp);
//			display.repaint();
		}
		public void setImpulseFactor(double d){
			impulsefactor =d;
		}
		public double getImpulseFactor(){
			return impulsefactor;
		}
		public void setAtom(Atom a){
			target = a;
		}
		public Atom getAtom(){
			return target;
		}
		public Component graphic(Object obj) {return new JPanel();}
    
    
		//  Start of DisplayPhaseListeners //
    
		private class Kicker implements DisplayPhaseListener {
			public void displayPhaseAction(DisplayPhaseEvent dpe) {
				java.awt.event.MouseEvent event =  dpe.getMouseEvent();
				targetpos = (Vector) target.coord.position();
				impulse.E(dpe.point());
				impulse.ME(targetpos);
				impulse.TE(-impulsefactor*Math.exp(impulsefactor1*impulse.squared()));
				mousepos.E(dpe.point());
				if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED)
					visible = true;
				else if (event.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED)
				{
					visible = false;}
					display.getPhase().integrator().pause();
					target.coord.accelerateBy(impulse);
					display.getPhase().integrator().unPause();
					display.getPhase().integrator().reset();
				
				display.repaint();
			}
		}
        
	}
    
}