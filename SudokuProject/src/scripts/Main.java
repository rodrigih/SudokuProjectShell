package scripts;

import cspSolver.BTSolver;
import cspSolver.BTSolver.ConsistencyCheck;
import cspSolver.BTSolver.ValueSelectionHeuristic;
import cspSolver.BTSolver.VariableSelectionHeuristic;
import sudoku.SudokuBoardReader;
import sudoku.SudokuFile;
import java.io.FileWriter;

public class Main
{
	public enum STATUSES {success,timeout,failure};
	
	public static void main(String[] args)
	{
		long TOTAL_START = System.currentTimeMillis() / 1000;
		long SEARCH_START = 0;
		long SEARCH_DONE = 0;
		long SOLUTION_TIME = 0;
		
		// ARC not implemented, so set preprocessing to current time
		long PREPROCESSING_START = System.currentTimeMillis() / 1000;
		long PREPROCESSING_DONE = PREPROCESSING_START;
		
		
		
		STATUSES STATUS; // TODO: Change STATUS to enum
		
		
		SudokuFile sf = SudokuBoardReader.readFile(args[0]);
		BTSolver solver = new BTSolver(sf);
		int timeout = 60000; // Default time for timeout
		
		solver.setValueSelectionHeuristic(ValueSelectionHeuristic.None);
		solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.None);
		solver.setConsistencyChecks(ConsistencyCheck.None);
		
		for(String arg: args)
		{
			switch(arg)
			{
			case "FC":
				solver.setConsistencyChecks(ConsistencyCheck.ForwardChecking);
				break;
				
			case "MRV":
				if(solver.getVariableHeuristic().equals(VariableSelectionHeuristic.Degree))
					solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MRVDH);
				else
					solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MinimumRemainingValue);
				break;
				
			case "DH":
				if(solver.getVariableHeuristic().equals(VariableSelectionHeuristic.MinimumRemainingValue))
					solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.MRVDH);
				else
					solver.setVariableSelectionHeuristic(VariableSelectionHeuristic.Degree);
				break;
				
			case "LCV":
				solver.setValueSelectionHeuristic(ValueSelectionHeuristic.LeastConstrainingValue);
				break;
				
			default:
				// Check if the value is an integer. If so, then set it as the
				try
				{
					timeout = new Integer(arg) * 1000;
					
				}catch(Exception e)
				{
					// Do Nothing if string is not an integer
				}
				break;
			}
		}		
			
		
		Thread t1 = new Thread(solver);
		try
		{
			SEARCH_START = System.currentTimeMillis() / 1000;
			t1.start();
			t1.join(timeout);
			if(t1.isAlive())
			{
				SEARCH_DONE = System.currentTimeMillis() / 1000;
				STATUS = STATUSES.timeout;
				t1.interrupt();
			}
			if(SEARCH_DONE == 0) //Check if timeout happened
				SEARCH_DONE = System.currentTimeMillis() / 1000;
			
			STATUS = STATUSES.success;
		}catch(InterruptedException e)
		{
			STATUS = STATUSES.failure;
		}

		SOLUTION_TIME = (PREPROCESSING_DONE-PREPROCESSING_START) + 
				(SEARCH_DONE - SEARCH_START);
		// Write contents to file
		try
		{
			FileWriter writer = new FileWriter(args[1]);
			writer.write("TOTAL_START=" + (int)TOTAL_START + "\n" +
						 "PREPROCESSING_START=" + (int)PREPROCESSING_START + "\n" + 
						 "PREPROCESSING_DONE=" + (int)PREPROCESSING_DONE + "\n" + 
						 "SEARCH_START=" + (int)SEARCH_START + "\n" + 
						 "SEARCH_DONE=" + (int)SEARCH_DONE + "\n" + 
						 "SOLUTION_TIME=" + (int)SOLUTION_TIME + "\n" + 
						 "STATUS=" + STATUS + "\n");
			
			// Check if it has solution
			if(solver.hasSolution())
			{
				writer.write("SOLUTION=" + solver.getSolution().toTuple() + "\n");
							 
				writer.write("COUNT_NODES=" + solver.getNumAssignments() + "\n" + 
							 "COUNT_DEADENDS=" + solver.getNumBacktracks());
				
				solver.printSolverStats();
				System.out.println(solver.getSolution());	
				writer.close();
			}

			else
			{
				System.out.println("Failed to find a solution");
				writer.close();
			}
			
		}catch(Exception e)
		{
			System.out.println("Encounterd Problem writing to file.");
		}
		

	}
}
