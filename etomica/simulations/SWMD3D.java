//Source file generated by Etomica

package etomica.simulations;

import etomica.*;
import etomica.graphics.*;
import etomica.units.Dimension;

//remember to set up Space3D.CoordinatePair.reset if experiencing 
//problems with this simulation hanging

public class SWMD3D extends SimulationGraphic {

	/**
	 * @author kofke
	 *
	 * To change this generated comment edit the template variable "typecomment":
	 * Window>Preferences>Java>Templates.
	 * To enable and disable the creation of type comments go to
	 * Window>Preferences>Java>Code Generation.
	 */
	public class MyModulator extends ModulatorAbstract {

		/**
		 * @see etomica.DatumSource#getDimension()
		 */
		public Dimension getDimension() {
			return Dimension.LENGTH;
		}

		/**
		 * @see etomica.ModulatorAbstract#setValue(double)
		 */
		public void setValue(double d) {
			potential0.setCoreDiameter(d);
			speciesSpheres0.setDiameter(d);
		}

		/**
		 * @see etomica.ModulatorAbstract#getValue()
		 */
		public double getValue() {
			return potential0.getCoreDiameter();
		}

	}
	P2SquareWell potential0;
	SpeciesSpheresMono speciesSpheres0;
	Atom first;
	
  public SWMD3D() {
	super(new etomica.Space3D());
	Simulation.instance = this;
//	Default.makeLJDefaults();

	etomica.Phase phase0  = new etomica.Phase();
	phase0.setConfiguration(new ConfigurationFcc(this));
	potential0  = new etomica.P2SquareWell();
	etomica.Controller controller0  = new etomica.Controller();
	new DeviceTrioControllerButton(this, controller0);
	speciesSpheres0  = new etomica.SpeciesSpheresMono();
	potential0.setSpecies(speciesSpheres0);
	potential0.setLambda(1.6);
	speciesSpheres0.setNMolecules(108);

	etomica.graphics.DisplayPhase displayPhase0  = new etomica.graphics.DisplayPhase();

//	displayPhase0.setColorScheme(new ColorSchemeByType());
	displayPhase0.setColorScheme(new MyColorScheme());
	ColorSchemeByType.setColor(speciesSpheres0, java.awt.Color.blue);
 
	etomica.IntegratorHard integratorHard0  = new etomica.IntegratorHard();
      integratorHard0.setIsothermal(true);
      integratorHard0.setTemperature(300);
//	MeterPressureHard meterPressure = new MeterPressureHard();
//	DisplayBox box = new DisplayBox();
//	box.setDatumSource(meterPressure);
	
	DeviceSlider tControl = new DeviceSlider(integratorHard0, "temperature");
	DeviceSlider sigmaControl = new DeviceSlider(new MyModulator());
	DeviceSlider lambdaControl = new DeviceSlider(potential0, "lambda");
	tControl.setLabel("Temperature (K)");
	sigmaControl.setLabel("Atom size (Angstroms)");
	tControl.setShowValues(true);
	tControl.setShowBorder(true);
	tControl.setMinimum(100);
	tControl.setMaximum(700);
	sigmaControl.setShowValues(true);
	sigmaControl.setShowBorder(true);
	sigmaControl.setPrecision(2);
	sigmaControl.setMinimum(0.0);
	sigmaControl.setMaximum(3.0);
	lambdaControl.setShowValues(true);
	lambdaControl.setShowBorder(true);
	lambdaControl.setPrecision(2);
	lambdaControl.setMinimum(1.1);
	lambdaControl.setMaximum(2.1);
	lambdaControl.setValue(1.4);
	lambdaControl.setNMajor(5);


	mediator().go();
	first = speciesSpheres0.getAgent(phase0).firstMolecule();

//	DeviceNSelector nControl = new DeviceNSelector(speciesSpheres0.getAgent(phase0));
//	nControl.setMaximum(108);
	speciesSpheres0.setNMolecules(108);
	phase0.setDensity(0.0405);
	mediator().go();
  } //end of constructor

  public static void main(String[] args) {
	Simulation sim = new SWMD3D();
	sim.mediator().go(); 
	SimulationGraphic.makeAndDisplayFrame(sim);
  }//end of main
  
  public class MyColorScheme extends ColorScheme {
	  public java.awt.Color atomColor(Atom a) {
		  return (a == first) ? java.awt.Color.red : java.awt.Color.yellow;
	  }
  }

}//end of class
