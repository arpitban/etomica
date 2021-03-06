/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.freeenergy.npath;

import etomica.atom.AtomSetSinglet;
import etomica.atom.IAtomKinetic;
import etomica.atom.IAtomList;
import etomica.box.Box;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialMaster;
import etomica.space.Vector;
import etomica.util.Debug;
import etomica.util.random.IRandom;

/**
 * This integrator implements velocity Verlet algorithm and Tuckerman's
 * multi-step time step algorithm to handle propagating the spring forces.
 * <p>
 * Created by andrew on 4/30/17.
 */
public class IntegratorImageMultistepMD extends IntegratorVelocityVerlet {

    protected final AtomSetSinglet atomSetSinglet;
    protected P1ImageHarmonic p1;
    protected int numInnerSteps;

    public IntegratorImageMultistepMD(PotentialMaster potentialMaster, IRandom random, double timeStep, double temperature, Box box) {
        super(potentialMaster, random, timeStep, temperature, box);
        atomSetSinglet = new AtomSetSinglet();
        setForceSum(new PotentialCalculationForceSum());
    }

    public void setP1Harmonic(P1ImageHarmonic p1) {
        this.p1 = p1;
    }

    public void setNumInnerSteps(int numInnerSteps) {
        this.numInnerSteps = numInnerSteps;
    }

    protected void doStepInternal() {
        // from IntegratorMD
        currentTime += timeStep;
        // IntegratorVelocityVerlet code
        IAtomList leafList = box.getLeafList();
        int nLeaf = leafList.size();
        for (int iLeaf = 0; iLeaf < nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic) leafList.get(iLeaf);
            Vector force = agentManager.getAgent(a);
            Vector r = a.getPosition();
            Vector v = a.getVelocity();
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(a))) {
                System.out.println("first " + a + " r=" + r + ", v=" + v + ", f=" + force);
            }
            v.PEa1Tv1(0.5 * timeStep * a.getType().rm(), force);  // p += f(old)*dt/2
        }

        double tStepShort = timeStep / numInnerSteps;
        p1.setZeroForce(false);
        for (int iLeaf = 0; iLeaf < nLeaf; iLeaf++) {
            int iLeaf1 = p1.getPartner(iLeaf);
            if (iLeaf1 < iLeaf) {
                continue;
            }
            IAtomKinetic atom0 = (IAtomKinetic) leafList.get(iLeaf);
            IAtomKinetic atom1 = (IAtomKinetic) leafList.get(iLeaf1);
            Vector r0 = atom0.getPosition();
            Vector r1 = atom1.getPosition();
            Vector v0 = atom0.getVelocity();
            Vector v1 = atom1.getVelocity();
            atomSetSinglet.atom = atom0;
            Vector[] grad = p1.gradient(atomSetSinglet);
            Vector grad0 = grad[0];
            double rm0 = atom0.getType().rm();
            double rm1 = atom1.getType().rm();
            for (int i = 0; i < numInnerSteps; i++) {
                v0.PEa1Tv1(-0.5 * tStepShort * rm0, grad0);
                r0.PEa1Tv1(tStepShort, v0);

                v1.PEa1Tv1(0.5 * tStepShort * rm1, grad0);
                r1.PEa1Tv1(tStepShort, v1);
                grad = p1.gradient(atomSetSinglet);
                grad0 = grad[0];

                v0.PEa1Tv1(-0.5 * tStepShort * rm0, grad0);
                v1.PEa1Tv1(0.5 * tStepShort * rm1, grad0);
            }
        }
        p1.setZeroForce(true);

        eventManager.forcePrecomputed();

        forceSum.reset();
        //Compute forces on each atom
        potentialMaster.calculate(box, allAtoms, forceSum);

        eventManager.forceComputed();

        //Finish integration step
        for (int iLeaf = 0; iLeaf < nLeaf; iLeaf++) {
            IAtomKinetic a = (IAtomKinetic) leafList.get(iLeaf);
//            System.out.println("force: "+((MyAgent)a.ia).force.toString());
            Vector velocity = a.getVelocity();
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(a))) {
                System.out.println("second " + a + " v=" + velocity + ", f=" + agentManager.getAgent(a));
            }
            velocity.PEa1Tv1(0.5 * timeStep * a.getType().rm(), agentManager.getAgent(a));  //p += f(new)*dt/2
        }

        if (isothermal) {
            doThermostatInternal();
        }
    }
}
