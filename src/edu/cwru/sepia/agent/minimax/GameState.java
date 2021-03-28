package edu.cwru.sepia.agent.minimax;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

public class GameState {
    public static final double MAX_UTILITY = Double.POSITIVE_INFINITY;
    public static final double MIN_UTILITY = Double.NEGATIVE_INFINITY;
    public static final String ACTION_MOVE_NAME = Action.createPrimitiveMove(0, null).getType().name();
    public static final String ACTION_ATTACK_NAME = Action.createPrimitiveAttack(0, 0).getType().name();

    private final Board board;
    private final boolean isPlayerTurn;
    private boolean utilityCalculated = false;
    private double utility = 0.0;

    /**
     * Class containing agents and resources (with locations) and several helper methods
     */
    private class Board {
        private final boolean[][] isOccupied;
        private final Map<Integer, MMAgent> agents = new HashMap<Integer, MMAgent>(4);
        private final ArrayList<MMAgent> goodAgents = new ArrayList<MMAgent>(2);
        private final ArrayList<MMAgent> badAgents = new ArrayList<MMAgent>(2);
        private final Map<Integer, Resource> resources = new HashMap<Integer, Resource>();
        private final int width, height;

        public Board(int x, int y) {
            isOccupied = new boolean[x][y];
            this.width = x;
            this.height = y;
        }

        public void addResource(int id, int x, int y) {
            Resource resource = new Resource(id, x, y);
            isOccupied[x][y] = true;
            resources.put(resource.getID(), resource);
        }

        public void addAgent(int id, int x, int y, int hp, int possibleHp, int attackDamage, int attackRange) {
            MMAgent agent = new MMAgent(id, x, y, hp, possibleHp, attackDamage, attackRange);
            agents.put(id, agent);
            if (agent.isGood()) {
                goodAgents.add(agent);
            } else {
                badAgents.add(agent);
            }
        }

        private void moveAgentBy(int id, int xOffset, int yOffset) {
            MMAgent agent = getAgent(id);
            int currentX = agent.getX();
            int currentY = agent.getY();
            int nextX = currentX + xOffset;
            int nextY = currentY + yOffset;
            agent.setX(nextX);
            agent.setY(nextY);
        }

        public void attackAgent(MMAgent attacker, MMAgent attacked) {
            if (attacked != null && attacker != null) {
                attacked.setHp(attacked.getHp() - attacker.getAttackDamage());
            }
        }

        public boolean isEmpty(int x, int y) {
            return !isOccupied[x][y];
        }

        public boolean isResource(int x, int y) {
            return isOccupied[x][y];
        }

        public boolean isOnBoard(int x, int y) {
            return x >= 0 && x < width && y >= 0 && y < height;
        }

        public boolean canMove(int x, int y) {
            return isOnBoard(x, y) && !isResource(x, y);
        }

        public MMAgent getAgent(int id) {
            MMAgent agent = agents.get(id);
            if (!agent.isAlive()) {
                return null;
            }
            return agent;
        }

        public Collection<MMAgent> getAllAgents() {
            return agents.values();
        }

        public ArrayList<MMAgent> getAliveGoodAgents() {
            ArrayList<MMAgent> alive = new ArrayList<>();
            for (MMAgent a : this.goodAgents) {
                if (a.isAlive())
                    alive.add(a);
            }

            return alive;
        }

        public ArrayList<MMAgent> getAliveBadAgents() {
            ArrayList<MMAgent> alive = new ArrayList<>();
            for (MMAgent a : this.badAgents) {
                if (a.isAlive())
                    alive.add(a);
            }

            return alive;
        }

        public double distance(MMAgent agent1, MMAgent agent2) {
            return (Math.abs(agent1.getX() - agent2.getX()) + Math.abs(agent1.getY() - agent2.getY())) - 1;
        }

        public double attackDistance(MMAgent agent1, MMAgent agent2) {
            return Math.floor(Math.hypot(Math.abs(agent1.getX() - agent2.getX()), Math.abs(agent1.getY() - agent2.getY())));
        }

        private List<Integer> findAttackableAgents(MMAgent agent) {
            List<Integer> attackable = new ArrayList<Integer>();
            for (MMAgent otherAgent : getAllAgents()) {
                if (otherAgent.getID() != agent.getID() && (otherAgent.isGood() != agent.isGood()) &&
                        attackDistance(agent, otherAgent) <= agent.getAttackRange()) {
                    attackable.add(otherAgent.getID());
                }
            }
            return attackable;
        }
    }

//    /**
//     * Represents a single location or square on the playing board
//     */
//    private abstract class Square {
//        private final int id;
//        private int x, y;
//
//        public Square(int id, int x, int y) {
//            this.id = id;
//            this.x = x;
//            this.y = y;
//        }
//
//        public int getID() {
//            return this.id;
//        }
//
//        public int getX() {
//            return this.x;
//        }
//
//        public void setX(int x) {
//            this.x = x;
//        }
//
//        public int getY() {
//            return this.y;
//        }
//
//        public void setY(int y) {
//            this.y = y;
//        }
//    }

    /**
     * A representation of an agent either good (footman) or bad (archer)
     */
    private class MMAgent {
        private int hp;
        private final int possibleHp, attackDamage, attackRange, id;
        private int x, y;

        public MMAgent(int id, int x, int y, int hp, int possibleHp, int attackDamage, int attackRange) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.hp = hp;
            this.possibleHp = possibleHp;
            this.attackDamage = attackDamage;
            this.attackRange = attackRange;
        }

        public int getID() {
            return id;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public boolean isGood() {
            return this.getID() == 0 || this.getID() == 1;
        }

        public boolean isAlive() {
            return hp > 0;
        }

        private int getHp() {
            return hp;
        }

        private void setHp(int hp) {
            this.hp = hp;
        }

        private int getPossibleHp() {
            return possibleHp;
        }

        private int getAttackDamage() {
            return attackDamage;
        }

        private int getAttackRange() {
            return attackRange;
        }
    }

    /**
     * A representation of non-agents on the board - trees
     */
    private class Resource {
        private final int id, x, y;
        
        public Resource(int id, int x, int y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public int getID() {
            return id;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    /**
     * Constructor that takes from a SEPIA state view to generate my representation of state
     * <p>
     * This is only called on the initial state all children state are generated by the other constructor.
     *
     * @param state
     */
    public GameState(State.StateView state) {
        this.board = new Board(state.getXExtent(), state.getYExtent());

        for (Unit.UnitView uv : state.getAllUnits()) {
            this.board.addAgent(uv.getID(), uv.getXPosition(), uv.getYPosition(), uv.getHP(), uv.getHP(), uv.getTemplateView().getBasicAttack(), uv.getTemplateView().getRange());
        }

        for (ResourceNode.ResourceView resource : state.getAllResourceNodes()) {
            this.board.addResource(resource.getID(), resource.getXPosition(), resource.getYPosition());
        }

        this.isPlayerTurn = true;
    }

    /**
     * This constructor uses the non-SEPIA representation of the game and is called for all
     * except the first creation of a GameState
     *
     * @param gameState
     */
    public GameState(GameState gameState) {
        this.board = new Board(gameState.board.width, gameState.board.height);

        for (MMAgent agent : gameState.board.getAllAgents()) {
            this.board.addAgent(agent.getID(), agent.getX(), agent.getY(), agent.getHp(), agent.getPossibleHp(), agent.getAttackDamage(), agent.getAttackRange());
        }

        for (Resource resource : gameState.board.resources.values()) {
            this.board.addResource(resource.getID(), resource.getX(), resource.getY());
        }

        this.isPlayerTurn = !gameState.isPlayerTurn;
        this.utilityCalculated = gameState.utilityCalculated;
        this.utility = gameState.utility;
    }


    /**
     * Determines the "goodness" of a state. Includes things like being able to attack an opponent
     * current health and location relative to obstacles (resources) and enemies
     * <p>
     * For more information on each specific feature see comments on each ...Utility() function
     *
     * @return
     */
    public double getUtility() {
        if (this.utilityCalculated) {
            return this.utility;
        }

        // Calculate features included
        this.utility += getHasGoodAgentsUtility();
        this.utility += getHasBadAgentsUtility();
        this.utility += getHealthUtility();
        this.utility += getDamageToEnemyUtility();
        this.utility += getCanAttackUtility();
        this.utility += getLocationUtility();

        this.utilityCalculated = true;
        return this.utility;
    }

    /**
     * @return the number of good agents or the MIN_UTILITY if all good agents are dead (the game is over and we lost)
     */
    private double getHasGoodAgentsUtility() {
        return this.board.getAliveGoodAgents().isEmpty() ? MIN_UTILITY : this.board.getAliveGoodAgents().size();
    }

    /**
     * @return the number of bad agents or the MAX_UTILITY if all bad agents are dead (the game is over and we won)
     */
    private double getHasBadAgentsUtility() {
        return this.board.getAliveBadAgents().isEmpty() ? MAX_UTILITY : this.board.getAliveBadAgents().size();
    }

    /**
     * @return the amount of health each footman has
     */
    private double getHealthUtility() {
        double utility = 0.0;
        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            utility += agent.getHp() / agent.getPossibleHp();
        }
        return utility;
    }

    /**
     * @return how much damage has been done to each archer
     */
    private double getDamageToEnemyUtility() {
        double utility = 0.0;
        for (MMAgent agent : this.board.getAliveBadAgents()) {
            utility += agent.getPossibleHp() - agent.getHp();
        }
        return utility;
    }

    /**
     * @return the number of agents that are within range of the footmen
     */
    private double getCanAttackUtility() {
        double utility = 0.0;
        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            utility += this.board.findAttackableAgents(agent).size();
        }
        return utility;
    }

    /**
     * @return how optimal the footman positions are attempts to deal with obstacles (resources)
     */
    private double getLocationUtility() {
        if (this.board.resources.isEmpty() ||
                noResourcesAreInTheArea()) {
            return distanceFromEnemy() * -1;
        }
        double percentageBlocked = percentageOfBlockedFootmen();
        if (percentageBlocked > 0) {
            return -200000 * percentageBlocked;
        }
        return distanceFromEnemy() * -1;
    }

    /**
     * a footman is blocked when the diaganol path between it an the enemy is blocked by trees
     *
     * @return number of blocked footman / total number of footmen
     */
    private double percentageOfBlockedFootmen() {
        int numBlocked = 0;
        int totalNumGood = 0;
        for (MMAgent goodGuy : this.board.getAliveGoodAgents()) {
            MMAgent badGuy = this.getClosestEnemy(goodGuy);
            if (badGuy != null) {
                int i = goodGuy.getX();
                int j = goodGuy.getY();
                while (i != badGuy.getX() || j != badGuy.getY()) {
                    if (this.board.isOnBoard(i, j) && this.board.isResource(i, j)) {
                        numBlocked++;
                    }
                    if (i < badGuy.getX()) {
                        i++;
                    } else if (i > badGuy.getX()) {
                        i--;
                    }
                    if (j < badGuy.getY()) {
                        j++;
                    } else if (j > badGuy.getY()) {
                        j--;
                    }
                }
            }
            totalNumGood++;
        }
        if (totalNumGood == 0) {
            return 0;
        }
        return numBlocked / totalNumGood;
    }

    /**
     * @return true if no resources even near either footman archer pair
     */
    private boolean noResourcesAreInTheArea() {
        int count = 0;
        int numGood = 0;
        for (MMAgent goodGuy : this.board.getAliveGoodAgents()) {
            for (MMAgent badGuy : this.board.getAliveBadAgents()) {
                if (numResourceInAreaBetween(goodGuy, badGuy) != 0) {
                    count++;
                }
            }
            numGood++;
        }
        return count < numGood;
    }

    /**
     * @param goodGuy
     * @param badGuy
     * @return the number of resources in the largest rectangle possible between the two agent's coordinates
     */
    private double numResourceInAreaBetween(MMAgent goodGuy, MMAgent badGuy) {
        double resources = 0.0;
        for (int i = Math.min(goodGuy.getX(), badGuy.getX()); i < Math.max(goodGuy.getX(), badGuy.getX()); i++) {
            for (int j = Math.min(goodGuy.getY(), badGuy.getY()); j < Math.max(goodGuy.getY(), badGuy.getY()); j++) {
                if (this.board.isResource(i, j)) {
                    resources += 1;
                }
            }
        }
        return resources;
    }

    /**
     * @return the sum of the distances to the closest enemy for each footman
     */
    private double distanceFromEnemy() {
        double utility = 0.0;
        for (MMAgent goodAgent : this.board.getAliveGoodAgents()) {
            double value = Double.POSITIVE_INFINITY;
            for (MMAgent badAgent : this.board.getAliveBadAgents()) {
                value = Math.min(this.board.distance(goodAgent, badAgent), value);
            }
            if (value != Double.POSITIVE_INFINITY) {
                utility += value;
            }
        }
        return utility;
    }

    /**
     * @param goodAgent
     * @return the closest aarcher to the footman given
     */
    private MMAgent getClosestEnemy(MMAgent goodAgent) {
        MMAgent closestEnemy = null;
        for (MMAgent badAgent : this.board.getAliveBadAgents()) {
            if (closestEnemy == null) {
                closestEnemy = badAgent;
            } else if (this.board.distance(goodAgent, badAgent) < this.board.distance(goodAgent, closestEnemy)) {
                closestEnemy = badAgent;
            }
        }
        return closestEnemy;
    }


    public List<GameStateChild> getChildren() {
        ArrayList<GameStateChild> gsc = new ArrayList<>();

        ArrayList<MMAgent> agents = isPlayerTurn ? board.getAliveGoodAgents() : board.getAliveBadAgents();

        ArrayList<ArrayList<Action>> actionsForEachAgent = new ArrayList<>();

        for (MMAgent agent : agents) {
            actionsForEachAgent.add(getAgentActions(agent));
        }

        List<Map<Integer, Action>> l = cartesianProductOf2(actionsForEachAgent);

        return convertToGameStateChildList(l);
    }

    private static final Direction[] r = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private ArrayList<Action> getAgentActions(MMAgent agent) {
        ArrayList<Action> actions = new ArrayList<Action>();
        for (Direction direction : r) {
            int nextX = agent.getX() + direction.xComponent();
            int nextY = agent.getY() + direction.yComponent();
            if (this.board.canMove(nextX, nextY)) {
                actions.add(Action.createPrimitiveMove(agent.getID(), direction));
            }
        }
        for (Integer id : this.board.findAttackableAgents(agent)) {
            actions.add(Action.createPrimitiveAttack(agent.getID(), id));
        }
        return actions;
    }


    private List<Map<Integer, Action>> cartesianProductOf2(ArrayList<ArrayList<Action>> actionList) {
        ArrayList<Map<Integer, Action>> maps = new ArrayList<>();

        if (actionList.size() == 0)
            return maps;

        List<Action> firstActions = actionList.get(0);
        for (Action action : firstActions) {
            if (actionList.size() > 1) {
                List<Action> secondActions = actionList.get(1);
                for (Action action2 : secondActions) {
                    HashMap<Integer, Action> map = new HashMap<>();

                    map.put(action2.getUnitId(), action2);
                    map.put(action.getUnitId(), action);

                    maps.add(map);
                }
            }

            Map<Integer, Action> map = new HashMap<>();

            map.put(action.getUnitId(), action);

            maps.add(map);
        }

        return maps;
    }

    private List<GameStateChild> convertToGameStateChildList(List<Map<Integer, Action>> maps) {
        List<GameStateChild> gsc = new ArrayList<GameStateChild>();
        for (Map<Integer, Action> actionMap : maps) {
            GameState gs = new GameState(this);
            for (Action action : actionMap.values()) {
                gs.applyAction(action);
            }
            gsc.add(new GameStateChild(actionMap, gs));
        }
        return gsc;
    }

    private void applyAction(Action action) {
        if (action.getType() == ActionType.PRIMITIVEMOVE) {
            Direction dir = ((DirectedAction) action).getDirection();
            board.moveAgentBy(action.getUnitId(), dir.xComponent(), dir.yComponent());
        } else if (action.getType() == ActionType.PRIMITIVEATTACK) {
            TargetedAction ta = (TargetedAction) action;
            MMAgent a1 = board.getAgent(ta.getUnitId());
            MMAgent a2 = board.getAgent(ta.getTargetId());
            board.attackAgent(a1, a2);
        }
    }

}
