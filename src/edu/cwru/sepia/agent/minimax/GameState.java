package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 * <p>
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    private Board board;
    private boolean isPlayerTurn, utilityComputed;
    private double utility;
    private List<Integer> playerIDs, enemyIDs;

    private class Board {
        private boolean[][] board;
        private Map<Integer, MMAgent> agents = new HashMap<Integer, MMAgent>();
        private ArrayList<MMAgent> goodAgents = new ArrayList<MMAgent>();
        private ArrayList<MMAgent> badAgents = new ArrayList<MMAgent>();
        private Map<Integer, Resource> resources = new HashMap<Integer, Resource>();
        private final int width, height;

        public Board(int x, int y) {
            board = new boolean[x][y];
            this.width = x;
            this.height = y;
        }

        public void addResource(int id, int x, int y) {
            Resource resource = new Resource(id, x, y);
            board[x][y] = true;
            resources.put(resource.getID(), resource);
        }

        public void addAgent(int id, int x, int y, int hp, int possibleHp, int attackDamage, int attackRange) {
            MMAgent agent = new MMAgent(id, x, y, hp, possibleHp, attackDamage, attackRange);
//            board[x][y] = agent;
            agents.put(id, agent);
            if (agent.isGood()) {
                goodAgents.add(agent);
            } else {
                badAgents.add(agent);
            }
        }

        private void moveAgent(int id, int xOffset, int yOffset) {
            MMAgent agent = getAgent(id);
            int currentX = agent.getX();
            int currentY = agent.getY();
            int nextX = currentX + xOffset;
            int nextY = currentY + yOffset;
//            board[currentX][currentY] = null;
            agent.setX(nextX);
            agent.setY(nextY);
//            board[nextX][nextY] = agent;
        }

        public void attackAgent(MMAgent attacker, MMAgent attacked) {
            if (attacked != null && attacker != null) {
                attacked.setHp(attacked.getHP() - attacker.getAttackDamage());
            }
        }


        public boolean canMove(int x, int y) {
            return x > 0 && y > 0 && x < width && y < width && !board[x][y];
        }

        public boolean isResource(int x, int y) {
            return board[x][y];
        }


        public MMAgent getAgent(int id) {
            MMAgent agent = agents.get(id);
            if (!agent.isAlive()) {
                return null;
            }
            return agent;
        }

        public ArrayList<MMAgent> getAllAgents() {
            return new ArrayList<>(agents.values());
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

    /**
     * Represents a single location or square on the playing board
     */

    /**
     * A representation of an agent either good (footman) or bad (archer)
     */
    private class MMAgent {
        private int hp, possibleHp, attackDamage, attackRange, id, x, y;

        public MMAgent(int id, int x, int y, int hp, int possibleHp, int attackDamage, int attackRange) {
            this.x = x;
            this.y = y;
            this.id = id;
            this.hp = hp;
            this.possibleHp = possibleHp;
            this.attackDamage = attackDamage;
            this.attackRange = attackRange;
        }

        public boolean isGood() {
            return id == 0 || id == 1;
        }

        public boolean isAlive() {
            return hp > 0;
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

        public void setX(int x) {
            this.x = x;
        }

        public void setY(int y) {
            this.y = y;
        }

        private int getHP() {
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
        private int id, x, y;
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
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     * <p>
     * You may find the following state methods useful:
     * <p>
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     * <p>
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * <p>
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs belonging to the player.
     * You control player 0, the enemy controls player 1.
     * <p>
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * <p>
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * <p>
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */

    public GameState(State.StateView state) {
        board = new Board(state.getXExtent(), state.getYExtent());

        playerIDs = state.getUnitIds(0);
        enemyIDs = state.getUnitIds(1);

        for (Unit.UnitView uv : state.getAllUnits()) {
            board.addAgent(uv.getID(), uv.getXPosition(), uv.getYPosition(), uv.getHP(), uv.getHP(), uv.getTemplateView().getBasicAttack(), uv.getTemplateView().getRange());
        }

        for (ResourceNode.ResourceView rv : state.getAllResourceNodes()) {
            board.addResource(rv.getID(), rv.getXPosition(), rv.getYPosition());
        }

        this.isPlayerTurn = true;
    }

    public GameState(GameState state) {
        this.board = new Board(state.board.width, state.board.height);

        for (MMAgent agent : state.board.getAllAgents()) {
            this.board.addAgent(agent.getID(), agent.getX(), agent.getY(), agent.getHP(), agent.getPossibleHp(), agent.getAttackDamage(), agent.getAttackRange());
        }

        for (GameState.Resource resource : state.board.resources.values()) {
            this.board.addResource(resource.getID(), resource.getX(), resource.getY());
        }

        this.isPlayerTurn = !state.isPlayerTurn;
        this.utilityComputed = state.utilityComputed;
        this.utility = state.utility;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public List<Integer> getUnitIds(int player) {
        return player == 0 ? playerIDs : enemyIDs;
    }

    public MMAgent getAgent(int id) {
        return board.getAgent(id);
    }

    /**
     * You will implement this function.
     * <p>
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     * <p>
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     * <p>
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    /*
    We wanted our utility function to incorporate three important aspects of the game: the footman's health, the
    archer's health, and the distance between the footman and their closest enemy. The reason why we included the footman's
    health is that, in a situation where the footman are attempting to kill the enemy, it's better for the footman to have
    as much health as possible so they can hopefully not die by the time they reach and kill the archers. The main reason
    why we included the archer's health (and associated it with a significant weight) is because the main goal
    of the game is for the footmen to take all of the archer's health away. Lastly, we decided to include the distance between
    the footman and their closest enemy is because the closer the footman is to the enemies, the closer the footman is within range
    of attacking and killing the enemy.
     */
//    public double getUtility() {
//        if (utilityComputed)
//            return this.utility;
//
//        this.utilityComputed = true;
//
//        this.utility = ThreadLocalRandom.current().nextDouble();
////        for (MMAgent goodAgent : board.goodAgents) {
////            for (MMAgent badAgent : board.badAgents) {
////                double d = Math.sqrt(Math.pow(goodAgent.getXPos() - badAgent.getXPos(), 2) + Math.pow(goodAgent.getYPos() - badAgent.getYPos(), 2));
////                if (d < goodAgent.getAttackRange()) {
////                    utility += 1;
////                }
////
////                utility += board.xExtent - Math.abs(goodAgent.getXPos() - badAgent.getXPos()) + board.yExtent - Math.abs(goodAgent.getYPos() - badAgent.getYPos());
////            }
////        }
//
//        return this.utility;
//
//
////        if (utilityComputed)
////            return this.utility;
////        //this.utility = ThreadLocalRandom.current().nextDouble(-100, 100);
////        this.utilityComputed = true;
////        double sumDistance = 0;
////        int playerHealth = 0;
////        int enemyHealth = 0;
////        //The sum all of the distances between the footman and the archers
////        double enemyOneDistance, enemyTwoDistance = 0;
////        if(board.badAgents.size() == 1){
////            for(MMAgent player : board.goodAgents){
////                sumDistance += straightLineDistance(board.badAgents.get(0).getXPos(), board.badAgents.get(0).getYPos(), player.getXPos(), player.getYPos());
////            }
////        }
////        else{
////            for(MMAgent player : board.goodAgents) {
////                enemyOneDistance = straightLineDistance(board.badAgents.get(0).getXPos(), board.badAgents.get(0).getYPos(), player.getXPos(), player.getYPos());
////                enemyTwoDistance = straightLineDistance(board.badAgents.get(1).getXPos(), board.badAgents.get(1).getYPos(), player.getXPos(), player.getYPos());
////                sumDistance += Math.min(enemyTwoDistance, enemyOneDistance)/2;
////            }
////        }
////        //the sum of all the player health and the sume of all the enemy health
////        for(MMAgent player : board.goodAgents){
////            playerHealth += player.getHP();
////        }
////
////        for(MMAgent enemy : board.badAgents){
////            enemyHealth += enemy.getHP();
////        }
////
////        //the individual weights for each feature in the utility
////        double playerHealthWeight = 2.0;
////        double enemyHealthWeight = -15.0;
////        double distanceBetweenWeight = -7.0;
////
////        this.utility = (distanceBetweenWeight * sumDistance) + (enemyHealthWeight * enemyHealth) + (playerHealthWeight * playerHealth);
////        return this.utility;
//    }
//    public double getUtility() {
//        if (this.utilityComputed) {
//            return this.utility;
//        }
//
//        // Calculate features included
//        this.utility += getHasGoodAgentsUtility();
//        this.utility += getHasBadAgentsUtility();
//        this.utility += getHealthUtility();
////        this.utility += getDamageToEnemyUtility();
////        this.utility += getCanAttackUtility();
////        this.utility += getLocationUtility();
//
//        this.utilityComputed = true;
//        return this.utility;
//    }
//
//    private double getHasGoodAgentsUtility() {
//        double utility = 0;
//        boolean t = false;
//        for (MMAgent a : this.board.goodAgents) {
//            if (a.getHP() > 0) {
//                utility += 1;
//                t = true;
//            }
//        }
//        return t ? utility : Double.NEGATIVE_INFINITY;
//    }
//
//    private double getHasBadAgentsUtility() {
//        double utility = 0;
//        boolean t = false;
//        for (MMAgent a : this.board.badAgents) {
//            if (a.getHP() > 0) {
//                utility += 1;
//                t = true;
//            }
//        }
//        return t ? utility : Double.POSITIVE_INFINITY;
//    }
//
//    private double getHealthUtility() {
//        double utility = 0.0;
//        for (MMAgent agent : this.board.goodAgents) {
//            if (agent.getHP() <= 0)
//                continue;
//
//            utility += (double) agent.getHP() / (double) agent.getPossibleHp();
//        }
//        return utility;
//    }

    public double getUtility() {
        if(this.utilityComputed){
            return this.utility;
        }

        // Calculate features included
        this.utility += getHasGoodAgentsUtility();
        this.utility += getHasBadAgentsUtility();
        this.utility += getHealthUtility();
        this.utility += getDamageToEnemyUtility();
        this.utility += getCanAttackUtility();
        this.utility += getLocationUtility();

        this.utilityComputed = true;
        return this.utility;
    }

    /**
     * @return the number of good agents or the MIN_UTILITY if all good agents are dead (the game is over and we lost)
     */
    private double getHasGoodAgentsUtility() {
        return this.board.getAliveGoodAgents().isEmpty() ? Double.NEGATIVE_INFINITY : this.board.getAliveGoodAgents().size();
    }

    /**
     * @return the number of bad agents or the MAX_UTILITY if all bad agents are dead (the game is over and we won)
     */
    private double getHasBadAgentsUtility() {
        return this.board.getAliveBadAgents().isEmpty() ? Double.POSITIVE_INFINITY : this.board.getAliveBadAgents().size();
    }

    /**
     * @return the amount of health each footman has
     */
    private double getHealthUtility() {
        double utility = 0.0;
        for(MMAgent agent : this.board.getAliveGoodAgents()){
            utility += agent.getHP()/agent.getPossibleHp();
        }
        return utility;
    }

    /**
     * @return how much damage has been done to each archer
     */
    private double getDamageToEnemyUtility() {
        double utility = 0.0;
        for(MMAgent agent : this.board.getAliveBadAgents()){
            utility += agent.getPossibleHp() - agent.getHP();
        }
        return utility;
    }

    /**
     * @return the number of agents that are within range of the footmen
     */
    private double getCanAttackUtility() {
        double utility = 0.0;
        for(MMAgent agent : this.board.getAliveGoodAgents()){
            utility += this.board.findAttackableAgents(agent).size();
        }
        return utility;
    }

    private double numResourceInAreaBetween(MMAgent goodGuy, MMAgent badGuy){
        double resources = 0.0;
        for(int i = Math.min(goodGuy.getX(), badGuy.getX()); i < Math.max(goodGuy.getX(), badGuy.getX()); i++){
            for(int j = Math.min(goodGuy.getY(), badGuy.getY()); j < Math.max(goodGuy.getY(), badGuy.getY()); j++){
                if(this.board.isResource(i, j)){
                    resources += 1;
                }
            }
        }
        return resources;
    }

    private MMAgent getClosestEnemy(MMAgent goodAgent) {
        MMAgent closestEnemy = null;
        for(MMAgent badAgent : this.board.getAliveBadAgents()){
            if(closestEnemy == null){
                closestEnemy = badAgent;
            } else if(this.board.distance(goodAgent, badAgent) < this.board.distance(goodAgent, closestEnemy)){
                closestEnemy = badAgent;
            }
        }
        return closestEnemy;
    }

    private double percentageOfBlockedFootmen() {
        int numBlocked = 0;
        int totalNumGood = 0;
        for(MMAgent goodGuy : this.board.getAliveGoodAgents()){
            MMAgent badGuy = this.getClosestEnemy(goodGuy);
            if(badGuy != null){
                int i = goodGuy.getX();
                int j = goodGuy.getY();
                while(i != badGuy.getX() || j != badGuy.getY()){
                    if(i < board.width && i > 0 && j < board.height && j > 0 && this.board.isResource(i, j) ){
                        numBlocked++;
                    }
                    if(i < badGuy.getX()){
                        i++;
                    } else if (i > badGuy.getX()) {
                        i--;
                    }
                    if(j < badGuy.getY()){
                        j++;
                    } else if(j > badGuy.getY()){
                        j--;
                    }
                }
            }
            totalNumGood++;
        }
        if(totalNumGood == 0){
            return 0;
        }
        return numBlocked/totalNumGood;
    }

    /**
     * @return true if no resources even near either footman archer pair
     */
    private boolean noResourcesAreInTheArea(){
        int count = 0;
        int numGood = 0;
        for(MMAgent goodGuy : this.board.getAliveGoodAgents()){
            for(MMAgent badGuy : this.board.getAliveBadAgents()){
                if(numResourceInAreaBetween(goodGuy, badGuy) != 0){
                    count++;
                }
            }
            numGood++;
        }
        return count < numGood;
    }

    /**
     * @return how optimal the footman positions are attempts to deal with obstacles (resources)
     */
    private double getLocationUtility() {
        if(this.board.resources.isEmpty() ||
                noResourcesAreInTheArea()){
            return distanceFromEnemy() * -1;
        }
        double percentageBlocked = percentageOfBlockedFootmen();
        if(percentageBlocked > 0){
            return -200000 * percentageBlocked;
        }
        return distanceFromEnemy() * -1;
    }
    private
    double distanceFromEnemy() {
        double utility = 0.0;
        for(MMAgent goodAgent : this.board.getAliveGoodAgents()){
            double value = Double.POSITIVE_INFINITY;
            for(MMAgent badAgent : this.board.getAliveBadAgents()){
                value = Math.min(this.board.distance(goodAgent, badAgent), value);
            }
            if(value != Double.POSITIVE_INFINITY){
                utility += value;
            }
        }
        return utility;
    }


    public double straightLineDistance(int xOne, int yOne, int xTwo, int yTwo) {
        return Math.sqrt(Math.pow((xTwo - xOne), 2) + Math.pow((yTwo - yOne), 2));
    }


    /**
     * You will implement this function.
     * <p>
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * <p>
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     * <p>
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     * <p>
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * <p>
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * <p>
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * <p>
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * <p>
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     *
     * @return All possible actions and their associated resulting game state
     */

    private static final Direction[] AGENT_POSSIBLE_DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

//    public List<GameStateChild> getChildren() {
//        ArrayList<MMAgent> agentsActiveThisTurn;
//        if (isPlayerTurn) {
//            agentsActiveThisTurn = this.board.goodAgents;
//        } else {
//            agentsActiveThisTurn = this.board.badAgents;
//        }
//        List<List<Action>> actionsForEachAgent = agentsActiveThisTurn.stream()
//                .map(this::getAgentActions)
//                .collect(Collectors.toList());
//        List<Map<Integer, Action>> actionMaps = cartesianProductOf2(actionsForEachAgent);
//        return convertToGameStateChildList(actionMaps);
//    }

    public List<GameStateChild> getChildren() {
        ArrayList<GameStateChild> gsc = new ArrayList<>();

        ArrayList<MMAgent> agents = isPlayerTurn ? board.getAliveGoodAgents() : board.getAliveBadAgents();

        ArrayList<ArrayList<Action>> actionsForEachAgent = new ArrayList<>();

        for (MMAgent agent : agents) {
            actionsForEachAgent.add(getAgentActions(agent));
        }

        List<Map<Integer, Action>> l = cartesianProductOf2(actionsForEachAgent);
        List<GameStateChild> gameStateChildren = convertToGameStateChildList(l);

        return gameStateChildren;
    }

    //      This is a more efficient version if we can assume only 2 in the actionList
    private List<Map<Integer, Action>> cartesianProductOf2(ArrayList<ArrayList<Action>> actionList) {
        ArrayList<Map<Integer, Action>> maps = new ArrayList<>();

        List<Action> firstActions = actionList.get(0);
        for (Action action : firstActions) {
            if (actionList.size() == 1) {
                Map<Integer, Action> map = new HashMap<>();

                map.put(action.getUnitId(), action);

                maps.add(map);
            } else {
                List<Action> secondActions = actionList.get(1);
                for (Action action2 : secondActions) {
                    HashMap<Integer, Action> map = new HashMap<>();

                    map.put(action2.getUnitId(), action2);
                    map.put(action.getUnitId(), action);

                    maps.add(map);
                }
            }
        }

        return maps;
    }

//    This is a generalized version that should work for n in actionList
//    private List<Map<Integer, Action>> enumerateActions(ArrayList<ArrayList<Action>> actionList) {
//        ArrayList<Map<Integer, Action>> actionMaps = new ArrayList<>();
//
//        for (int i = 0; i < actionList.size(); i++) {
//            List<Action> list = actionList.get(i);
//
//            for (Action action : list) {
//                for (int i2 = i; i2 < actionList.size(); i2++) {
//                    List<Action> list2 = actionList.get(i);
//
//                    for (Action action2 : list2) {
//                        Map<Integer, Action> map = new HashMap<>();
//                        map.put(action.getUnitId(), action);
//                        map.put(action2.getUnitId(), action2);
//
//                        actionMaps.add(map);
//                    }
//                }
//
//            }
//
//        }
//
//        return actionMaps;
//    }

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


    private ArrayList<Action> getAgentActions(MMAgent agent) {
        ArrayList<Action> actions = new ArrayList<Action>();
        for (Direction direction : Direction.values()) {
            if (direction != Direction.EAST && direction != Direction.NORTH && direction != Direction.SOUTH && direction != Direction.WEST)
                continue;

            int nextX = agent.getX() + direction.xComponent();
            int nextY = agent.getY() + direction.yComponent();
            if (board.canMove(nextX, nextY)) {
                actions.add(Action.createPrimitiveMove(agent.getID(), direction));
            }
        }
//        for(Integer id : this.board.findAttackableAgents(agent)){
//            actions.add(Action.createPrimitiveAttack(agent.getId(), id));
//        }
        return actions;
    }

//    private ArrayList<Action> getAgentActions(MMAgent agent) {
//        ArrayList<Action> actions = new ArrayList<>();
//
//        for (Direction d : Direction.values()) {
//            int tx = agent.getXPos() + d.xComponent();
//            int ty = agent.getYPos() + d.yComponent();
//            if (board.canMove(tx, ty))
//                actions.add(Action.createPrimitiveMove(agent.getID(), d));
//        }
//
//        for (MMAgent goodAgent : board.goodAgents) {
//            for (MMAgent badAgent : board.badAgents) {
//                double d = Math.hypot(goodAgent.getXPos() - badAgent.getXPos(), goodAgent.getYPos() - badAgent.getYPos());
//                if (d < badAgent.getAttackRange()) {
//                    actions.add(Action.createPrimitiveAttack(badAgent.getID(), goodAgent.getID()));
//                }
//                if (d < goodAgent.getAttackRange()) {
//                    actions.add(Action.createPrimitiveAttack(goodAgent.getID(), badAgent.getID()));
//                }
//            }
//        }
//
//        return actions;
//    }

    private void applyAction(Action action) {
        if (action.getType() == ActionType.PRIMITIVEMOVE) {
            Direction dir = ((DirectedAction) action).getDirection();
            board.moveAgent(action.getUnitId(), dir.xComponent(), dir.yComponent());
        } else if (action.getType() == ActionType.PRIMITIVEATTACK) {
            TargetedAction ta = (TargetedAction) action;
            MMAgent a1 = board.getAgent(ta.getUnitId());
            MMAgent a2 = board.getAgent(ta.getTargetId());
            board.attackAgent(a1, a2);
        }
    }
}
