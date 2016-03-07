package cspSolver;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import sudoku.Converter;
import sudoku.SudokuFile;
/**
 * Backtracking solver. 
 *
 */
public class BTSolver implements Runnable{

	//===============================================================================
	// Properties
	//===============================================================================

	private ConstraintNetwork network;
	private static Trail trail = Trail.getTrail();
	private boolean hasSolution = false;
	private SudokuFile sudokuGrid;

	private int numAssignments;
	private int numBacktracks;
	private long startTime;
	private long endTime;
	
	public enum VariableSelectionHeuristic 	{ None, MinimumRemainingValue, Degree, MRVDH };
	public enum ValueSelectionHeuristic 		{ None, LeastConstrainingValue };
	public enum ConsistencyCheck				{ None, ForwardChecking, ArcConsistency };
	
	private VariableSelectionHeuristic varHeuristics;
	private ValueSelectionHeuristic valHeuristics;
	private ConsistencyCheck cChecks;
	//===============================================================================
	// Constructors
	//===============================================================================

	public BTSolver(SudokuFile sf)
	{
		this.network = Converter.SudokuFileToConstraintNetwork(sf);
		this.sudokuGrid = sf;
		numAssignments = 0;
		numBacktracks = 0;
	}

	//===============================================================================
	// Modifiers
	//===============================================================================
	
	public void setVariableSelectionHeuristic(VariableSelectionHeuristic vsh)
	{
		this.varHeuristics = vsh;
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}
	//===============================================================================
	// Accessors
	//===============================================================================

	/** 
	 * @return true if a solution has been found, false otherwise. 
	 */
	public boolean hasSolution()
	{
		return hasSolution;
	}

	/**
	 * @return solution if a solution has been found, otherwise returns the unsolved puzzle.
	 */
	public SudokuFile getSolution()
	{
		return sudokuGrid;
	}

	public void printSolverStats()
	{
		System.out.println("Time taken:" + (endTime-startTime) + " ms");
		System.out.println("Number of assignments: " + numAssignments);
		System.out.println("Number of backtracks: " + numBacktracks);
	}

	/**
	 * 
	 * @return time required for the solver to attain in seconds
	 */
	public long getTimeTaken()
	{
		return endTime-startTime;
	}

	public int getNumAssignments()
	{
		return numAssignments;
	}

	public int getNumBacktracks()
	{
		return numBacktracks;
	}

	public ConstraintNetwork getNetwork()
	{
		return network;
	}

	public VariableSelectionHeuristic getVariableHeuristic()
	{
		return varHeuristics;
	}
	
	public ValueSelectionHeuristic getValueHeuristic()
	{
		return valHeuristics;
	}
	
	public ConsistencyCheck getConsistencyCheck()
	{
		return cChecks;
	}

	//===============================================================================
	// Helper Methods
	//===============================================================================

	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkConsistency()
	{
		boolean isConsistent = false;
		switch(cChecks)
		{
		case None: 				isConsistent = assignmentsCheck();
		break;
		case ForwardChecking: 	isConsistent = forwardChecking();
		break;
		case ArcConsistency: 	isConsistent = arcConsistency();
		break;
		default: 				isConsistent = assignmentsCheck();
		break;
		}
		return isConsistent;
	}
	
	/**
	 * default consistency check. Ensures no two variables are assigned to the same value.
	 * @return true if consistent, false otherwise. 
	 */
	private boolean assignmentsCheck()
	{
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement forward checking. 
	 */
	private boolean forwardChecking()
	{
		for (Variable v: network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther: network.getNeighborsOfVariable(v))
				{
					if(v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
					else
					{
						vOther.removeValueFromDomain(v.getAssignment());
						if(vOther.getDomain().isEmpty())
						{
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		return false;
	}
	
	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{
		Variable next = null;
		switch(varHeuristics)
		{
		case None: 					
			next = getfirstUnassignedVariable();
			break;
		case MinimumRemainingValue:
		case MRVDH:
			next = getMRV();
			break;
		case Degree:				
			next = getDegree();
			break;
		default:					
			next = getfirstUnassignedVariable();
			break;
		}
		return next;
	}
	
	/**
	 * default next variable selection heuristic. Selects the first unassigned variable. 
	 * @return first unassigned variable. null if no variables are unassigned. 
	 */
	private Variable getfirstUnassignedVariable()
	{
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				return v;
			}
		}
		return null;
	}

	/**
	 * TODO: Implement MRV heuristic
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()
	{ 
		int min = Integer.MAX_VALUE; // Initialize min to be highest possible value
		int minIndex = -1;	// Store the index of the variable with the MRV to avoid 
						// searching for variable again
		int currentIndex = 0;
		
		for(Variable v: network.getVariables())
		{
			
			if(!v.isAssigned())
			{
				if (v.size() < min)
				{
					min = v.size();
					minIndex = currentIndex;
				}
				
				// If DH is also on, break ties with lowest DH
				if(v.size() == min && varHeuristics == VariableSelectionHeuristic.MRVDH)
				{
					if(findDegree(v,network) > findDegree(network.getVariables().get(minIndex),network))
					{
						min = v.size();
						minIndex = currentIndex;
					}
						
				}
			}
			currentIndex++;
		}
		
		if(minIndex == -1) // Means every node has been assigned.
			return null;
		
		return network.getVariables().get(minIndex);
	}
	
	/**
	 * TODO: Implement Degree heuristic
	 * @return variable constrained by the most unassigned variables, null if all variables are assigned.
	 */
	private Variable getDegree()
	{
		int maxDegree = -1; // Initialize min degree to be highest possible value
		int maxIndex = -1; // save index to avoid searching for minDegree variable again
		
		int currentIndex = 0;
		
		for(Variable v: network.getVariables())
		{
			if(!v.isAssigned())
			{
				int degree = findDegree(v,network);
				
				// Now check if degree is the minimum.
				// If so, update minDegree and save Index
				if (degree > maxDegree)
				{
					maxDegree = degree;
					maxIndex = currentIndex;
				}
			}
			currentIndex++;
		}
		
		if(maxIndex == -1) // Means every node has been assigned.
			return null;
		
		return network.getVariables().get(maxIndex);
	}
	/**
	 * Finds the degree of Variable v
	 * @param v Variable to find degree for
	 * @param cn Constraint Network variable belongs to
	 * @return degree of Variable v
	 */
	private int findDegree(Variable v, ConstraintNetwork cn)
	{
		int degree = 0;
		List<Variable> neighbours = cn.getNeighborsOfVariable(v);
		
		// For each neighobur, add one to degree of variable v is
		// neighbour is unassigned
		for(Variable n: neighbours)
		{
			if(!n.isAssigned())
				degree++;
		}
		
		return degree;
	}
	
	/**
	 * Value Selection Heuristics. Orders the values in the domain of the variable 
	 * passed as a parameter and returns them as a list.
	 * @return List of values in the domain of a variable in a specified order. 
	 */
	public List<Integer> getNextValues(Variable v)
	{
		List<Integer> orderedValues;
		switch(valHeuristics)
		{
		case None: 						orderedValues = getValuesInOrder(v);
		break;
		case LeastConstrainingValue: 	orderedValues = getValuesLCVOrder(v);
		break;
		default:						orderedValues = getValuesInOrder(v);
		break;
		}
		return orderedValues;
	}
	
	/**
	 * Default value ordering. 
	 * @param v Variable whose values need to be ordered
	 * @return values ordered by lowest to highest. 
	 */
	public List<Integer> getValuesInOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	
	/**
	 * TODO: LCV heuristic
	 */
	public List<Integer> getValuesLCVOrder(Variable v)
	{
		// Key is the number of constraints
		// Value is the value in Domain
		List<Entry<Integer,Integer>> pairs = new ArrayList<Entry<Integer,Integer>>(); 

		
		List<Variable> neighbours = network.getNeighborsOfVariable(v);
		
		for(int value: v.getDomain().getValues())
		{
			int count = 0;
			for(Variable n: neighbours)
			{
				if(n.getDomain().contains(value))
					count++;
			}
			pairs.add(new AbstractMap.SimpleEntry<Integer,Integer>(count,value));
		}		

		// Comparator used to get pairs in order of constraints.
		Comparator<Entry<Integer,Integer>> valueComparator = new Comparator<Entry<Integer,Integer>>()
				{
			@Override
			public int compare(Entry<Integer,Integer> p1, Entry<Integer,Integer> p2)
			{
				return p1.getKey().compareTo(p2.getKey());
			}
				};
		
		Collections.sort(pairs,valueComparator);
		List<Integer> result = new ArrayList<Integer>();
		
		// Add only values onto the result
		for(Entry<Integer,Integer> p: pairs)
		{
			result.add(p.getValue());
		}
		return result;
	}
	/**
	 * Called when solver finds a solution
	 */
	private void success()
	{
		hasSolution = true;
		sudokuGrid = Converter.ConstraintNetworkToSudokuFile(network, sudokuGrid.getN(), sudokuGrid.getP(), sudokuGrid.getQ());
	}

	//===============================================================================
	// Solver
	//===============================================================================

	/**
	 * Method to start the solver
	 */
	public void solve()
	{
		startTime = System.currentTimeMillis();
		try {
			solve(0);
		}catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
		}
		endTime = System.currentTimeMillis();
		Trail.clearTrail();
	}

	/**
	 * Solver
	 * @param level How deep the solver is in its recursion. 
	 * @throws VariableSelectionException 
	 */

	private void solve(int level) throws VariableSelectionException
	{
		if(!Thread.currentThread().isInterrupted())

		{//Check if assignment is completed
			if(hasSolution)
			{
				return;
			}

			//Select unassigned variable
			Variable v = selectNextVariable();		

			//check if the assignment is complete
			if(v == null)
			{
				for(Variable var : network.getVariables())
				{
					if(!var.isAssigned())
					{
						throw new VariableSelectionException("Something happened with the variable selection heuristic");
					}
				}
				success();
				return;
			}

			//loop through the values of the variable being checked LCV

			
			for(Integer i : getNextValues(v))
			{
				trail.placeBreadCrumb();

				//check a value
				v.updateDomain(new Domain(i));
				numAssignments++;
				boolean isConsistent = checkConsistency();
				
				//move to the next assignment
				if(isConsistent)
				{		
					solve(level + 1);
				}

				//if this assignment failed at any stage, backtrack
				if(!hasSolution)
				{
					trail.undo();
					numBacktracks++;
				}
				
				else
				{
					return;
				}
			}	
		}	
	}

	@Override
	public void run() {
		solve();
	}
}
