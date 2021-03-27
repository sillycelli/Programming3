package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import javax.sound.sampled.TargetDataLine;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

    private Board board;
    private boolean isPlayerTurn, utilityComputed;
    private double utility;
//    private int xExtent, yExtent;
    private List<Integer>  playerIDs, enemyIDs;
//    private List<ResourceNode.ResourceView> resourceNodes;
//    private List<Unit.UnitView> playerUnits, enemyUnits;
//    private HashMap<Integer, Unit.UnitView> hashEntityUnits;

    public class MMAgent {
        private final int id, attackDmg, attackRange;
        private int xPos, yPos, health;
        private final Unit.UnitView uv;

        public MMAgent(Unit.UnitView uv) {
            this.uv = uv;
            this.health = uv.getHP();
            this.xPos = uv.getXPosition();
            this.yPos = uv.getYPosition();
            this.id = uv.getID();
            this.attackDmg = uv.getTemplateView().getBasicAttack();
            this.attackRange = uv.getTemplateView().getRange();
        }

        public int getXPos() {
            return xPos;
        }

        public int getYPos() {
            return yPos;
        }

        public int getID() {
            return id;
        }

        public int getAttackDmg() {
            return attackDmg;
        }

        public int getAttackRange() {
            return attackRange;
        }

        public int getHP() {
            return health;
        }

        public Unit.UnitView getUV() {
            return uv;
        }
    }

    private class Resource {
        private int xPos, yPos;

        public Resource(int x, int y) {
            this.xPos = x;
            this.xPos = y;
        }

        public int getXPos() {
            return xPos;
        }

        public int getYPos() {
            return yPos;
        }

    }

    private class Board {
        private HashMap<Integer, MMAgent> agents;
        private ArrayList<MMAgent> goodAgents, badAgents;
        private ArrayList<Resource> resources;
        private final int xExtent, yExtent;

        public Board(int xExtent, int yExtent) {
            this.xExtent = xExtent;
            this.yExtent = yExtent;

            agents = new HashMap<>();
            resources = new ArrayList<>();
            goodAgents = new ArrayList<>();
            badAgents = new ArrayList<>();
        }

        public void addAgent(Unit.UnitView uv, boolean isGood) {
            MMAgent mma = new MMAgent(uv);
            if (isGood)
                goodAgents.add(mma);
            else
                badAgents.add(mma);

            agents.put(uv.getID(), mma);
        }

        public void addResource(ResourceNode.ResourceView rv) {
            Resource resource = new Resource(rv.getXPosition(), rv.getYPosition());
            resources.add(resource);
        }

        public List<Resource> getResources() {
            return resources;
        }

        public HashMap<Integer, MMAgent> getAgents() {
            return agents;
        }

        public ArrayList<MMAgent> getAgents(int player){
            return player == 0 ? goodAgents : badAgents;
        }

        public MMAgent getAgent(int id) {
            return agents.get(id);
        }

        public void moveAgent(int id, int dx, int dy) {
            MMAgent agent = agents.get(id);
            agent.xPos = agent.xPos + dx;
            agent.yPos = agent.yPos + dy;
        }

        public void attackAgent(int attacker, int attacked) {
            MMAgent aAttacker = getAgent(attacker);
            MMAgent aAttacked = getAgent(attacked);
            aAttacked.health = aAttacker.getAttackDmg();
        }

    }

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs belonging to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
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

        List<Unit.UnitView> playerUnits = state.getUnits(0);
        List<Unit.UnitView> enemyUnits = state.getUnits(1);

        for (Unit.UnitView uv : playerUnits) {
            board.addAgent(uv, true);
        }

        for (Unit.UnitView uv : enemyUnits) {
            board.addAgent(uv, false);
        }

        for (ResourceNode.ResourceView rv : state.getAllResourceNodes()) {
            board.addResource(rv);
        }

        this.isPlayerTurn = true;


//        xExtent = state.getXExtent();
//        yExtent = state.getYExtent();
//        resourceIDs = state.getAllResourceIds();
//        resourceNodes = state.getAllResourceNodes();
//        playerUnitIDs = state.getUnitIds(0);
//        enemyUnitIDs = state.getUnitIds(1);

//        playerUnits = state.getUnits(0);
//        enemyUnits = state.getUnits(1);

//        hashEntityUnits = new HashMap<>(playerUnits.size() + enemyUnits.size());
//
//        for (Unit.UnitView uv : playerUnits) {
//            hashEntityUnits.put(uv.getID(), uv);
//        }
//
//        for (Unit.UnitView uv : enemyUnits) {
//            hashEntityUnits.put(uv.getID(), uv);
//        }



    }

    public GameState(GameState state) {
        this.board = new Board(state.board.xExtent, state.board.yExtent);
        this.board.resources = new ArrayList<>(state.board.resources);
        this.board.agents = new HashMap<>(state.board.agents);
        this.board.goodAgents = state.board.goodAgents;
        this.board.badAgents = state.board.badAgents;

        this.isPlayerTurn = !state.isPlayerTurn;

        playerIDs = state.getUnitIds(0);
        enemyIDs = state.getUnitIds(1);

//        this.xExtent = state.xExtent;
//        this.yExtent = state.yExtent;
//        this.resourceIDs = state.resourceIDs;
//        this.playerUnitIDs = state.playerUnitIDs;
//        this.enemyUnitIDs = state.enemyUnitIDs;
//        this.resourceNodes = state.resourceNodes;
//        this.playerUnits = state.playerUnits;
//        this.enemyUnits = state.enemyUnits;
//        this.hashEntityUnits = state.hashEntityUnits;
    }

//    public List<Integer> getUnitIds(int player) {
//        return player == 0 ? playerUnitIDs : enemyUnitIDs;
//    }
//
//    public Unit.UnitView getUnit(int id) {
//        return board.getAgent(id).uv;
//
//    }

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
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
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
    public double getUtility() {
        if (utilityComputed)
            return this.utility;
        //this.utility = ThreadLocalRandom.current().nextDouble(-100, 100);
        this.utilityComputed = true;
        double sumDistance = 0;
        int playerHealth = 0;
        int enemyHealth = 0;
        //The sum all of the distances between the footman and the archers
        double enemyOneDistance, enemyTwoDistance = 0;
        if(board.badAgents.size() == 1){
            for(MMAgent player : board.goodAgents){
                sumDistance += straightLineDistance(board.badAgents.get(0).getXPos(), board.badAgents.get(0).getYPos(), player.getXPos(), player.getYPos());
            }
        }
        else{
            for(MMAgent player : board.goodAgents) {
                enemyOneDistance = straightLineDistance(board.badAgents.get(0).getXPos(), board.badAgents.get(0).getYPos(), player.getXPos(), player.getYPos());
                enemyTwoDistance = straightLineDistance(board.badAgents.get(1).getXPos(), board.badAgents.get(1).getYPos(), player.getXPos(), player.getYPos());
                sumDistance += Math.min(enemyTwoDistance, enemyOneDistance)/2;
            }
        }
        //the sum of all the player health and the sume of all the enemy health
        for(MMAgent player : board.goodAgents){
            playerHealth += player.getHP();
        }

        for(MMAgent enemy : board.badAgents){
            enemyHealth += enemy.getHP();
        }

        //the individual weights for each feature in the utility
        double playerHealthWeight = 2.0;
        double enemyHealthWeight = -15.0;
        double distanceBetweenWeight = 7.0;

        this.utility = (distanceBetweenWeight * sumDistance) + (enemyHealthWeight * enemyHealth) + (playerHealthWeight * playerHealth);
        return this.utility;
    }

    public double straightLineDistance(int xOne, int yOne, int xTwo, int yTwo){
        return Math.sqrt(Math.pow((xTwo-xOne), 2) + Math.pow((yTwo-yOne), 2));
    }



    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */

//    private static final Direction[] AGENT_POSSIBLE_DIRECTIONS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public List<GameStateChild> getChildren() {
        ArrayList<GameStateChild> gsc = new ArrayList<>();

        ArrayList<MMAgent> agents = board.getAgents(this.isPlayerTurn ? 0 : 1);

        ArrayList<ArrayList<Action>> actionsForEachAgent = new ArrayList<>();

        for (MMAgent agent : agents) {
            actionsForEachAgent.add(getAgentActions(agent));
        }

        List<Map<Integer, Action>> l = cartesianProductOf2(actionsForEachAgent);

        return convertToGameStateChildList(l);
    }
//      This is a more efficient version if we can assume only 2 in the actionList
    private List<Map<Integer, Action>> cartesianProductOf2(ArrayList<ArrayList<Action>> actionList) {
        ArrayList<Map<Integer, Action>> maps = new ArrayList<>();

        ArrayList<Action> firstActions = actionList.get(0);
        for (Action action : firstActions) {
            if (actionList.size() == 1) {
                Map<Integer, Action> map = new HashMap<>();

                map.put(action.getUnitId(), action);

                maps.add(map);
            } else {
                ArrayList<Action> secondActions = actionList.get(1);
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

    private List<GameStateChild> convertToGameStateChildList(List<Map<Integer, Action>> maps){
        List<GameStateChild> gsc = new ArrayList<GameStateChild>();
        for(Map<Integer, Action> actionMap : maps){
            GameState gs = new GameState(this);
            for(Action action : actionMap.values()){
                gs.applyAction(action);
            }
            gsc.add(new GameStateChild(actionMap, gs));
        }
        return gsc;
    }


    private ArrayList<Action> getAgentActions(MMAgent agent) {
        ArrayList<Action> actions = new ArrayList<>();

        for (Direction d : Direction.values()) {
//            for (Resource resource : board.getResources()) {
//                if (resource.getXPos() != agent.getXPos() && resource.getYPos() != agent.getYPos())
                    actions.add(Action.createPrimitiveMove(agent.getID(), d));
//            }
        }

        return actions;
    }

    private void applyAction(Action action) {
            if (action.getType() == ActionType.PRIMITIVEMOVE) {
                Direction dir = ((DirectedAction) action).getDirection();
                board.moveAgent(action.getUnitId(), dir.xComponent(), dir.yComponent());
            } else if (action.getType() == ActionType.PRIMITIVEATTACK) {
                TargetedAction ta = (TargetedAction) action;
                board.attackAgent(ta.getUnitId(), ta.getTargetId());
            }
        }
}
