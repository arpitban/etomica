/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

 package etomica.normalmode;

import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisCubicBcc;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.potential.P2SoftSphere;
import etomica.potential.P2SoftSphericalTruncated;
import etomica.potential.P2SoftSphericalTruncatedShifted;
import etomica.potential.Potential2SoftSpherical;
import etomica.space.Space;
import etomica.space3d.Space3D;

/**
 * Properties of a system of monatomic molecules occupying a lattice and interacting according
 * to a soft-sphere spherically-symmetric pair potential.  Properties are given by a lattice-dynamics treatment.
 * 
 * BCC Crystal Structure
 * 
 * @author Tai Boon Tan
 *
 */
public class HarmonicCrystalSoftSphereBCC {

    public static void main(String[] args) {
        double rho = 1.256;
        int exponent = 12;
        int maxLatticeShell = 3;
        int nC =4;
        int numAtom = nC*nC*nC*2;
        
        double temperature = 1.0;
        String fileName = "inputSSDB"+ numAtom+"_BCC";
        
        if (args.length > 0) {
            rho = Double.parseDouble(args[0]);
        }
        if (args.length > 1) {
            exponent = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            nC = Integer.parseInt(args[2]);
        }
        if (args.length > 3) {
            temperature = Double.parseDouble(args[3]);
        }
        if (args.length > 4) {
        	fileName = args[4];
        }
        
        System.out.println("BCC Harmonic Crystal with n = " + exponent);
        System.out.println("numAtom: " + numAtom);
        double a = Math.pow(2.0/rho, 1.0/3.0);
                
        Primitive primitive = new PrimitiveCubic(Space.getInstance(3), nC*a);
        Basis basisBCC = new BasisCubicBcc();
        
        Basis basis = new BasisBigCell(Space.getInstance(3), basisBCC, new int[]{nC,nC,nC});

        Space sp = Space3D.getInstance();
        final Potential2SoftSpherical potential = new P2SoftSphere(sp, 1.0, 1.0, exponent);
        double rc = 15; //nC*a*0.495;//15; //(maxLatticeShell*2)*nC*Math.pow(4.0/rho, 1.0/3.0)*0.495;
        System.out.println("truncation at "+rc);
        P2SoftSphericalTruncated pTruncated = new P2SoftSphericalTruncatedShifted(sp, potential, rc);
        
        int[] nCells = new int[] {1, 1, 1};
        long startTime = System.currentTimeMillis();
        
        pTruncated.setTruncationRadius(rc);
        HarmonicCrystal harmonicCrystal = new HarmonicCrystal(rho, nCells, primitive, basis, pTruncated, sp);
        harmonicCrystal.setMaxLatticeShell(maxLatticeShell);
        harmonicCrystal.getNormalModes().setFileName(fileName);
        
        double u = harmonicCrystal.getLatticeEnergy();
        System.out.println("lattice energy: " + u);

        
        double f = harmonicCrystal.getHelmholtzFreeEnergy(temperature);
        
        System.out.println("\nLattice Energy: " + u);
        System.out.println("Helmholtz Free Energy at T"+temperature+ " is: "+f);
        System.out.println("Harmonic-reference free energy: "+ (f-u));
      
        System.out.println("\nCalcHarmonicA from file (Temperature-independent)");
        CalcHarmonicA.doit(harmonicCrystal.getNormalModes(), 3, temperature, basis.getScaledCoordinates().length);

    }
}
