package etomica.junit.simulation;

import junit.framework.TestCase;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.ISpeciesManager;
import etomica.atom.AtomTypeLeaf;
import etomica.chem.elements.Element;
import etomica.chem.elements.ElementSimple;
import etomica.simulation.Simulation;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.species.SpeciesSpheresHetero;
import etomica.species.SpeciesSpheresMono;

public class SpeciesManagerTest extends TestCase {

	private ISpeciesManager sm;
	ISimulation simulation;
	ISpace space;
	Element element;
	
	public void setUp() {
		space = Space.getInstance(3);
		simulation = new Simulation(space);
		sm = simulation.getSpeciesManager();
		element = new ElementSimple(simulation);
		if(sm == null) {
			System.out.println("species manager is null");
		}
	}

	/*
	 * testAddSpecies
	 */
	public void testAddSpecies() {
		final int numSpecies = 5;
		ISpecies species[] = new ISpecies[numSpecies];

		for(int i = 0; i < numSpecies; i++) {
		    species[i] = new SpeciesSpheresMono(simulation, space, element);
		    sm.addSpecies(species[i]);
		}

		assertEquals(numSpecies, sm.getSpeciesCount());
		
		int expectedChildIndex = 0;
		for(int i = 0; i < sm.getSpeciesCount(); i++) {
			assertSame(species[i], sm.getSpecies(i));
			assertSame(i, species[i].getIndex());
			for(int j = 0; j < species[i].getChildTypeCount(); j++) {
			    assertSame(expectedChildIndex, species[i].getChildType(j).getIndex());
			    expectedChildIndex++;
			}
		}
	}

	/*
	 * testRemoveSpecies
	 */
	public void testRemoveSpecies() {
		int numSpecies = 5;
		ISpecies species[] = new ISpecies[numSpecies];

		for(int i = 0; i < numSpecies; i++) {
		    species[i] = new SpeciesSpheresMono(simulation, space, element);
		    sm.addSpecies(species[i]);
		}

		assertEquals(numSpecies, sm.getSpeciesCount());

		int expectedChildIndex = 0;
		for(int i = 0; i < sm.getSpeciesCount(); i++) {
			assertSame(species[i], sm.getSpecies(i));
			assertSame(i, species[i].getIndex());
	        for(int j = 0; j < species[i].getChildTypeCount(); j++) {
	            assertSame(expectedChildIndex, species[i].getChildType(j).getIndex());
	            expectedChildIndex++;
	        }
		}
		
		sm.removeSpecies(species[numSpecies-1]);
		numSpecies--;

		assertEquals(numSpecies, sm.getSpeciesCount());

		expectedChildIndex = 0;
		for(int i = 0; i < sm.getSpeciesCount(); i++) {
			assertSame(species[i], sm.getSpecies(i));
			assertSame(i, species[i].getIndex());
            for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                assertSame(expectedChildIndex, species[i].getChildType(j).getIndex());
                expectedChildIndex++;
            }
		}

		sm.removeSpecies(species[0]);
		numSpecies--;

		assertEquals(numSpecies, sm.getSpeciesCount());

		expectedChildIndex = 0;
		for(int i = 1; i < sm.getSpeciesCount(); i++) {
			assertSame(species[i], sm.getSpecies(i-1));
			assertSame(i-1, species[i].getIndex());
            for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                assertSame(expectedChildIndex, species[i].getChildType(j).getIndex());
                expectedChildIndex++;
            }
		}
	}

	/*
	 * testAddSpeciesChildIndex
	 */
	public void testAddSpeciesChildIndex() {
	    int INIT_NUM_SPECIES = 5;
		int numSpecies = INIT_NUM_SPECIES;
		final int numAtomsPerSpecies = 3;
		SpeciesSpheresHetero species[] = new SpeciesSpheresHetero[numSpecies];

		for(int i = 0; i < numSpecies; i++) {
		    species[i] = new SpeciesSpheresHetero(simulation, space, numAtomsPerSpecies);
		    sm.addSpecies(species[i]);
		}

		assertEquals(numSpecies, sm.getSpeciesCount());

		int expectedChildIndex = 0;
		for(int i = 0; i < numSpecies; i++) {
			assertEquals(i, sm.getSpecies(i).getIndex());
            for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                assertSame(expectedChildIndex, species[i].getChildType(j).getIndex());
                expectedChildIndex++;
            }
		}

		SpeciesSpheresMono newSpecies = new SpeciesSpheresMono(simulation, space, element);
		for(int j = 0; j < numAtomsPerSpecies-1; j++) {
		    newSpecies.addChildType(new AtomTypeLeaf(element));
		}
		sm.addSpecies(newSpecies);
		numSpecies++;

		assertEquals(numSpecies, sm.getSpeciesCount());

	    expectedChildIndex = 0;
		for(int i = 0; i < sm.getSpeciesCount(); i++) {
		    if(i < INIT_NUM_SPECIES) {
		        assertEquals(i, species[i].getIndex());
		    }
		    else {
		        assertEquals(i, newSpecies.getIndex());
		    }
			for(int j = 0; j < numAtomsPerSpecies; j++) {
			    assertEquals(expectedChildIndex, sm.getSpecies(i).getChildType(j).getIndex());
			    expectedChildIndex++;
			}
		}

	}

	/*
	 * testRemoveFirstSpeciesChildIndex
	 */
	public void testRemoveFirstSpeciesChildIndex() {
        int numSpecies = 6;
        int REMOVE_INDEX = 0;
        final int numAtomsPerSpecies = 3;
        SpeciesSpheresHetero species[] = new SpeciesSpheresHetero[numSpecies];

        for(int i = 0; i < numSpecies; i++) {
            species[i] = new SpeciesSpheresHetero(simulation, space, numAtomsPerSpecies);
            sm.addSpecies(species[i]);
        }

        assertEquals(numSpecies, sm.getSpeciesCount());

        int expectedChildIndex = 0;
        for(int i = 0; i < numSpecies; i++) {
            assertEquals(i, species[i].getIndex());
            for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                assertEquals(expectedChildIndex, species[i].getChildType(j).getIndex());
                expectedChildIndex++;
            }
        }

        sm.removeSpecies(species[REMOVE_INDEX]);
        numSpecies--;

        assertEquals(numSpecies, sm.getSpeciesCount());

        expectedChildIndex = 0;
        for(int i = 0; i < sm.getSpeciesCount(); i++) {
            if(i < REMOVE_INDEX) {
                assertEquals(i, species[i].getIndex());
                for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                    assertEquals(expectedChildIndex, species[i].getChildType(j).getIndex());
                    expectedChildIndex++;
                }
            }
            else {
                assertEquals(i, species[i+1].getIndex());
                for(int j = 0; j < species[i+1].getChildTypeCount(); j++) {
                    assertEquals(expectedChildIndex, species[i+1].getChildType(j).getIndex());
                    expectedChildIndex++;
                }
            }
        }
    }

	/*
	 * testRemoveSpeciesFromMiddleChildIndex
	 */
	public void testRemoveSpeciesFromMiddleChildIndex() {
		int numSpecies = 6;
		int REMOVE_INDEX = 2;
		final int numAtomsPerSpecies = 3;
		SpeciesSpheresHetero species[] = new SpeciesSpheresHetero[numSpecies];

		for(int i = 0; i < numSpecies; i++) {
		    species[i] = new SpeciesSpheresHetero(simulation, space, numAtomsPerSpecies);
		    sm.addSpecies(species[i]);
		}

		assertEquals(numSpecies, sm.getSpeciesCount());

		int expectedChildIndex = 0;
		for(int i = 0; i < numSpecies; i++) {
		    assertEquals(i, species[i].getIndex());
			for(int j = 0; j < species[i].getChildTypeCount(); j++) {
			    assertEquals(expectedChildIndex, species[i].getChildType(j).getIndex());
			    expectedChildIndex++;
			}
		}

		sm.removeSpecies(species[REMOVE_INDEX]);
		numSpecies--;

		assertEquals(numSpecies, sm.getSpeciesCount());

		expectedChildIndex = 0;
		for(int i = 0; i < sm.getSpeciesCount(); i++) {
		    if(i < REMOVE_INDEX) {
		        assertEquals(i, species[i].getIndex());
		        for(int j = 0; j < species[i].getChildTypeCount(); j++) {
		            assertEquals(expectedChildIndex, species[i].getChildType(j).getIndex());
		            expectedChildIndex++;
		        }
		    }
		    else {
		        assertEquals(i, species[i+1].getIndex());
	            for(int j = 0; j < species[i+1].getChildTypeCount(); j++) {
	                assertEquals(expectedChildIndex, species[i+1].getChildType(j).getIndex());
	                expectedChildIndex++;
	            }
		    }

		}
	}

	/*
	 * testRemoveLastSpeciesChildIndex
	 */
    public void testRemoveLastSpeciesChildIndex() {
        int numSpecies = 6;
        int REMOVE_INDEX = 5;
        final int numAtomsPerSpecies = 3;
        SpeciesSpheresHetero species[] = new SpeciesSpheresHetero[numSpecies];

        for(int i = 0; i < numSpecies; i++) {
            species[i] = new SpeciesSpheresHetero(simulation, space, numAtomsPerSpecies);
            sm.addSpecies(species[i]);
        }

        assertEquals(numSpecies, sm.getSpeciesCount());

        int expectedChildIndex = 0;
        for(int i = 0; i < numSpecies; i++) {
            assertEquals(i, species[i].getIndex());
            for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                assertEquals(expectedChildIndex, species[i].getChildType(j).getIndex());
                expectedChildIndex++;
            }
        }

        sm.removeSpecies(species[REMOVE_INDEX]);
        numSpecies--;

        assertEquals(numSpecies, sm.getSpeciesCount());

        expectedChildIndex = 0;
        for(int i = 0; i < sm.getSpeciesCount(); i++) {
            if(i < REMOVE_INDEX) {
                assertEquals(i, species[i].getIndex());
                for(int j = 0; j < species[i].getChildTypeCount(); j++) {
                    assertEquals(expectedChildIndex, species[i].getChildType(j).getIndex());
                    expectedChildIndex++;
                }
            }
            else {
                assertEquals(i, species[i+1].getIndex());
                for(int j = 0; j < species[i+1].getChildTypeCount(); j++) {
                    assertEquals(expectedChildIndex, species[i+1].getChildType(j).getIndex());
                    expectedChildIndex++;
                }
            }
        }
    }

}
