package etomica;

/**
 * General-purpose integrator for hard potentials.
 * Integrates equations of motion through time by advancing atoms from one collision to the
 * next.  Determination of time of collision and implementation of collision
 * dynamics is handled by the potential between the atoms, which therefore must
 * implement PotentialHard.  Each atom keeps in its Agent (obtained from this integrator) the shortest
 * time to collision with any of the atoms uplist of it (as defined by the iterator from 
 * the phase).
 *
 * @author David Kofke
 *
 */
public class IntegratorHard extends IntegratorHardAbstract implements EtomicaElement {

    public String getVersion() {return "IntegratorHard:01.06.14/"+IntegratorHardAbstract.VERSION;}
    
    private static final IteratorDirective upList = new IteratorDirective.Up();
    private static final IteratorDirective downList = new IteratorDirective.Down();

    //collision handler is passed to the potential and is notified of each collision
    //the potential detects.  The collision handler contains the necessary logic to
    //process this information so that the collision lists are kept up to date.
    //The potential should call the handler's setPotential method, with itself as the
    //argument, before beginning to detect collisions. 
    
    //the up-handler has the logic of the Allen & Tildesley upList subroutine
    //sets collision time of given atom to minimum value for collisions with all atoms uplist of it
    public final CollisionHandler collisionHandlerUp = new CollisionHandler() {
        double minCollisionTime;
        IntegratorHardAbstract.Agent aia;
        Atom atom1;
        public void setAtom(Atom a) {
            atom1 = a;
            minCollisionTime = Double.MAX_VALUE;
            aia = (IntegratorHardAbstract.Agent)a.ia;
            aia.resetCollision();
        }
        public void addCollision(AtomPair pair, double collisionTime) {
            if(pair.atom1() != atom1) setAtom(pair.atom1()); //need this if doing minimum collision time calculation for more than one atom
            if(collisionTime < minCollisionTime) {
                minCollisionTime = collisionTime;
                aia.setCollision(collisionTime, pair.atom2(), this.potential);
            }
        }
    }; //end of collisionHandlerUp

    //the down-handler has the logic of the Allen & Tildesley downList subroutine
    //sets collision times of atoms downlist of given atom to minimum of their current
    //value and their value with given atom
    public final CollisionHandler collisionHandlerDown = new CollisionHandler() {
        public void addCollision(AtomPair pair, double collisionTime) {
            IntegratorHardAbstract.Agent aia = (IntegratorHardAbstract.Agent)pair.atom1().ia;
            if(collisionTime < aia.collisionTime()) {
                aia.setCollision(collisionTime, pair.atom2(), this.potential);
            }
        }
    }; //end of collisionHandlerDown

    public IntegratorHard() {
        this(Simulation.instance);
    }
    public IntegratorHard(Simulation sim) {
        super(sim);
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Collision-based molecular dynamics simulation of hard potentials");
        return info;
    }
    
    public boolean addPhase(Phase p) {
        if(!super.addPhase(p)) return false;
        upList.setPhase(p);   //set phase for iterator directives
        downList.setPhase(p);
        return true;
    }

	/**
	 * Overrides superclass method to instantiate iterators when iteratorFactory in phase is changed.
	 * Called by Integrator.addPhase and Integrator.iteratorFactoryObserver.
	 */
	protected void makeIterators(IteratorFactory factory) {
        upAtomIterator = factory.makeAtomIteratorUp();
    }

    /**
    * Loops through all atoms to identify the one with the smallest value of collisionTime
    * Collision time is obtained from the value stored in the Integrator.Agent from each atom.
    */
    protected void findNextCollider() {
        //find next collision pair by looking for minimum collisionTime
        double minCollisionTime = Double.MAX_VALUE;
        upAtomIterator.reset();
        while(upAtomIterator.hasNext()) {
            Agent ia = (Agent)upAtomIterator.next().ia;
            double ct = ia.collisionTime();
            if( ct < minCollisionTime) {
                minCollisionTime = ct;
                colliderAgent = ia;
            }
        }
    }

    /**
    * Applies collision dynamics to colliders and updates collision time/partners.
    * Also invokes collisionAction method of all registered integrator meters.
    */
    protected void updateCollisions() {
        
        Atom collider = colliderAgent.atom();
        Atom partner = colliderAgent.collisionPartner();
            
    //   Do upList for any atoms that were scheduled to collide with atoms colliding now
    //   Assumes collider and partner haven't moved in list
        upAtomIterator.reset();  //first atom in first cell
        while(upAtomIterator.hasNext()) {
            Atom a = upAtomIterator.next();
            if(a == collider) { //finished with atoms before collider...
                if(partner == null) break;  //and there is no partner, so we're done, or...
                else continue;              //...else just skip this atom and continue with loop
            }
            if(a == partner) break; //finished with atoms before partner; we're done
            Atom aPartner = ((IntegratorHardAbstract.Agent)a.ia).collisionPartner();
            if(aPartner == collider || aPartner == partner) {
                phasePotential.findCollisions(upList.setAtom(a), collisionHandlerUp);
            }
        }//end while
            //reset collision partners of atoms that are now up from this atom but still list it as their
            //collision partner.  Assumes this atom was moved down list, but this won't always be the case
            //This bit could be made more efficient
            
            //if(a movedInList) {  add a means for bump method to declare it moved atom in the list
       /*     upAtomIterator.reset(a);
            while(upAtomIterator.hasNext()) {
                Atom atom = upAtomIterator.next();
                if(((Agent)atom.ia).collisionPartner == a) {  //upList atom could have atom as collision partner if atom was just moved down list
                    phasePotential.findCollisions(atom, UP, collisionHandlerUp);
                }
            }*/
            //to keep collision lists perfect, should do an upList on atoms that had this
            //atom on its neighbor list, but no longer do because it has moved away


        phasePotential.findCollisions(upList.setAtom(collider), collisionHandlerUp);
        phasePotential.findCollisions(downList.setAtom(collider), collisionHandlerDown);
        if(partner != null) {
            phasePotential.findCollisions(upList.setAtom(partner), collisionHandlerUp);
            phasePotential.findCollisions(downList.setAtom(partner), collisionHandlerDown);
        }

    }//end of processCollision

    /**
    * Advances all atom coordinates by tStep, without any intervening collisions.
    * Uses free-flight kinematics.
    */
    protected void advanceAcrossTimeStep(double tStep) {
                
        for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
            ((Agent)a.ia).decrementCollisionTime(tStep);
            if(a.isStationary()) {continue;}  //skip if atom is stationary
            a.coordinate.freeFlight(tStep);
    //     a.translateBy(tStep*a.rm(),a.momentum());
        }
    }

    /**
    * Sets up the integrator for action.
    * Do an upList call for each atom and find the next collider
    */
    protected void doReset() {
        if(isothermal) scaleMomenta(Math.sqrt(this.temperature/(firstPhase.kineticTemperature())));
        phasePotential.findCollisions(ALL, collisionHandlerUp);
        findNextCollider();
    }
                
    /**
    * Produces the Agent defined by this integrator.
    * One instance of an Agent is placed in each atom controlled by this integrator.
    */
        public Integrator.Agent makeAgent(Atom a) {
            return new IntegratorHardAbstract.Agent(a);
        }
             
}//end of IntegratorHard

