package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
<<<<<<< HEAD
import edu.cwru.sepia.environment.model.state.Unit;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Comparator;
=======

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
>>>>>>> ffd5fb968d1945d7f34f7a56918159e295e0f4ef

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {

        if(depth == 0) { //maybe also add whether it's a terminal node, that might not be applicable to a zero-sum game though
            return node;
        }
        if(node.state.isPlayer()) {
            double maxVal = Double.NEGATIVE_INFINITY;
            List<GameStateChild> children = node.state.getChildren();
            GameStateChild nodeEval = node;
            for(int i = 0; i < children.size(); i++) {
                nodeEval = alphaBetaSearch(children.get(i), depth - 1, alpha, beta);
                maxVal = Math.max(maxVal, nodeEval.state.getUtility());
                alpha = Math.max(alpha, maxVal);

                if(beta <= alpha) {
                    break;
                }
            }
            if(nodeEval.state.getUtility() == maxVal) {
                return nodeEval;
            }
            return node;
        } else {
            double minVal = Double.POSITIVE_INFINITY;
            List<GameStateChild> children = node.state.getChildren();
            GameStateChild nodeEval = node;
            for(int i = 0; i < children.size(); i++) {
                nodeEval = alphaBetaSearch(children.get(i), depth - 1, alpha, beta);
                minVal = Math.min(minVal, nodeEval.state.getUtility());
                beta = Math.min(beta, minVal);

                if(beta <= alpha) {
                    break;
                }
            }
            if(nodeEval.state.getUtility() == minVal) {
                return nodeEval;
            }
            return node;
        }

    }

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
<<<<<<< HEAD
     * equation: (
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children){

        PriorityQueue<GameStateChild> orderingChildren = new PriorityQueue<GameStateChild>(new Comparator<GameStateChild>() {
            @Override
            public int compare(GameStateChild childOne, GameStateChild childTwo) {
                return Integer.compare(
                        heuristic(childOne), heuristic(childTwo));
            }
        });
        for(GameStateChild child : children){
            orderingChildren.add(child);
        }

        ArrayList<GameStateChild> orderedChildren = new ArrayList<GameStateChild>(orderingChildren.size());
        while (!orderingChildren.isEmpty()) {
            orderedChildren.add(orderingChildren.poll());
        }

        return orderedChildren;
    }

    public int heuristic(GameStateChild child){
        List<Integer> playerUnits = child.state.getUnitIds(0);
        List<Integer> enemyUnits = child.state.getUnitIds(1);
        int sumDistance = 0;
        int enemyOneDistance = 0;
        int enemyTwoDistance = 0;
        if(enemyUnits.size() == 1){
            for(Integer playerID : playerUnits){
                sumDistance += Math.abs(child.state.getUnit(playerID).getXPosition() - child.state.getUnit(enemyUnits.get(0)).getXPosition()) + Math.abs(child.state.getUnit(playerID).getYPosition() - child.state.getUnit(enemyUnits.get(0)).getYPosition());
            }
        }
        else{
            for(Integer playerID : playerUnits) {
                enemyOneDistance = Math.abs(child.state.getUnit(playerID).getXPosition() - child.state.getUnit(enemyUnits.get(0)).getXPosition()) + Math.abs(child.state.getUnit(playerID).getYPosition() - child.state.getUnit(enemyUnits.get(0)).getYPosition());
                enemyTwoDistance = Math.abs(child.state.getUnit(playerID).getXPosition() - child.state.getUnit(enemyUnits.get(1)).getXPosition()) + Math.abs(child.state.getUnit(playerID).getYPosition() - child.state.getUnit(enemyUnits.get(1)).getYPosition());
                sumDistance += Math.min(enemyTwoDistance, enemyOneDistance);
            }
        }
        return sumDistance;
    }

=======
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
        return children;
    }
>>>>>>> ffd5fb968d1945d7f34f7a56918159e295e0f4ef
}
