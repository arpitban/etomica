package etomica;

/**
 * Encapsulation of a set of instructions that an AtomIterator
 * uses to select the atoms it presents on iteration.
 *
 * @author David Kofke
 */
public class IteratorDirective implements java.io.Serializable {
    
    private Atom atom1, atom2;
    private Direction direction;
    private int atomCount;
    
    public IteratorDirective() {
        this(UP);
    }
    public IteratorDirective(Direction direction) {
        set(direction);
        set();
    }
    
    //returns itself as a convenience, so that it may be set while being passed as an
    //argument to a method
    public final IteratorDirective set() {
        atom1 = atom2 = null;
        atomCount = 0;
        return this;
    }
    public final IteratorDirective set(Atom a) {
        atom1 = a;
        atom2 = null;
        atomCount = 1;
        return this;
    }
    public final IteratorDirective set(Atom a1, Atom a2) {
        atom1 = a1;
        atom2 = a2;
        atomCount = 2;
        return this;
    }
    public final IteratorDirective set(Direction direction) {
        this.direction = direction;
        return this;
    }

    public final int atomCount() {return atomCount;}
    public final Direction direction() {return direction;}
    
    public final Atom atom1() {return atom1;}
    public final Atom atom2() {return atom2;}
    
    //IteratorDirective.Direction
    public static final class Direction extends Constants.TypedConstant {
            
        private Direction(String label) {super(label);}
        public static final Direction[] CHOICES = new Direction[] {
            new Direction("Up"),
            new Direction("Down"),
            new Direction("Neither"),
            new Direction("Both")
        };
        
        public final Constants.TypedConstant[] choices() {return CHOICES;}
    }//end of Direction
    public static final Direction UP = Direction.CHOICES[0];
    public static final Direction DOWN = Direction.CHOICES[1];
    public static final Direction NEITHER = Direction.CHOICES[2];
    public static final Direction BOTH = Direction.CHOICES[3];
    
}//end of IteratorDirective    