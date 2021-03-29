package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

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
        return getBestState(node, maxVal(node, depth, alpha, beta));
    }

    public double maxVal(GameStateChild node, int depth, double alpha, double beta) {
        if(depth == 0) {
            return node.state.getUtility();
        }
        double maxVal = Double.NEGATIVE_INFINITY;
        List<GameStateChild> children = orderChildrenWithHeuristics(node.state.getChildren());
        for(int i = 0; i < children.size(); i++) {
            maxVal = Math.max(maxVal, minVal(children.get(i), depth - 1, alpha, beta));
            if(beta <= maxVal) {
                return maxVal;
            }
            alpha = Math.max(alpha, maxVal);
        }
        return maxVal;
    }

    public double minVal(GameStateChild node, int depth, double alpha, double beta) {
        if(depth == 0) {
            return node.state.getUtility();
        }
        double minVal = Double.POSITIVE_INFINITY;
        List<GameStateChild> children = orderChildrenWithHeuristics(node.state.getChildren());
        for(int i = 0; i < children.size(); i++) {
            minVal = Math.min(minVal, maxVal(children.get(i), depth - 1, alpha, beta));
            if(alpha >= minVal) {
                return minVal;
            }
            beta = Math.min(beta, minVal);
        }
        return minVal;
    }

    private GameStateChild getBestState(GameStateChild node, double value) {
        List<GameStateChild> children = node.state.getChildren();
        for(int i = 0; i < children.size(); i++) {
            if(value == children.get(i).state.getUtility()) {
                return children.get(i);
            }
        }
        //if none of the children's utility match the specified value, use heuristics to return a hopefully decent match
        //(This should not happen)
        return orderChildrenWithHeuristics(node.state.getChildren()).get(0);
    }

    /*public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {

        if(depth == 0) { //maybe also add whether it's a terminal node, that might not be applicable to a zero-sum game though
            return node;
        }
        if(node.state.isPlayerTurn()) {
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

    }*/

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
     * equation: (
     */

    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children){
        List<GameStateChild> ordered = new LinkedList<GameStateChild>();
        List<GameStateChild> moves = new LinkedList<GameStateChild>();
        for(GameStateChild child : children){
            int numAttacks = 0;
            for(Action action : child.action.values()){
                if(action.getType() == ActionType.PRIMITIVEATTACK){
                    numAttacks++;
                }
            }
            if(numAttacks == child.action.size()){
                ordered.add(0, child);
            } else if (numAttacks > 0){
                if(ordered.isEmpty()){
                    ordered.add(0, child);
                } else {
                    ordered.add(1, child);
                }
            } else {
                moves.add(child);
            }
        }
        moves.sort(new Comparator<GameStateChild>() {
            @Override
            public int compare(GameStateChild gameStateChild, GameStateChild t1) {
                if(gameStateChild.state.getUtility() > t1.state.getUtility()){
                    return -1;
                } else if (gameStateChild.state.getUtility() < t1.state.getUtility()){
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        ordered.addAll(moves);
        return ordered;
    }
}
