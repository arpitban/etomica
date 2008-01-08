package etomica.dimer;

import etomica.simulation.Simulation;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom of Sn on a surface, modeled with MEAM.
 * 
 * @author msellers
 *
 */

public class SimDimerMEAMadatomCluster extends Simulation{
	
	 public static void main(String[] args){
	        
	        String fileName = "one"; //args[0];
	        int mdSteps = 10; //Integer.parseInt(args[1]);
	        
	    	final String APP_NAME = "SimDimerMEAMadatomCluster";
	    	
	    	//Simulation 1 - MD and Dimer search
	    	final SimDimerMEAMadatom sim1 = new SimDimerMEAMadatom(fileName, false, false, false, false);
	    	sim1.activityIntegrateMD.setMaxSteps(mdSteps);
	    	sim1.activityIntegrateDimer.setMaxSteps(700);
	    	sim1.activityIntegrateMin.setMaxSteps(0);
	        sim1.getController().actionPerformed();
	  
	        //Simulation 2 - Fine grain Dimer search
	        final SimDimerMEAMadatom sim2 = new SimDimerMEAMadatom(fileName, true, false, false, false);
	        sim2.activityIntegrateMD.setMaxSteps(0);
	        sim2.activityIntegrateDimer.setMaxSteps(100);
	    	sim2.activityIntegrateMin.setMaxSteps(0);
	        sim2.getController().actionPerformed();
	        
	        //Simulation 3 - Vibrational normal mode analysis
	        final SimDimerMEAMadatom sim3 = new SimDimerMEAMadatom(fileName+"_fine_saddle", false, true, false, false);
	        sim3.activityIntegrateMD.setMaxSteps(0);
	        sim3.activityIntegrateDimer.setMaxSteps(0);
	    	sim3.activityIntegrateMin.setMaxSteps(0);
	        sim3.getController().actionPerformed();
	        
	        //Simulation 4 - Minimum Search
	        final SimDimerMEAMadatom sim4 = new SimDimerMEAMadatom(fileName, false, false, true, false);
	        sim4.activityIntegrateMD.setMaxSteps(0);
	        sim4.activityIntegrateDimer.setMaxSteps(0);
	    	sim4.activityIntegrateMin.setMaxSteps(500);
	        sim4.getController().actionPerformed();
	        
		    //Simulation 5 - Vibrational normal mode analysis
	        final SimDimerMEAMadatom sim5 = new SimDimerMEAMadatom(fileName+"_minimum", false, true, false, false);
	        sim5.activityIntegrateMD.setMaxSteps(0);
	        sim5.activityIntegrateDimer.setMaxSteps(0);
	    	sim5.activityIntegrateMin.setMaxSteps(0);
	        sim5.getController().actionPerformed();
	     
	    }




}
