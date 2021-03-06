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
            int currentX = agent.getXPosition();
            int currentY = agent.getYPosition();
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
            return (Math.abs(agent1.getXPosition() - agent2.getXPosition()) + Math.abs(agent1.getYPosition() - agent2.getYPosition())) - 1;
        }

        public double attackDistance(MMAgent agent1, MMAgent agent2) {
            return Math.floor(Math.hypot(Math.abs(agent1.getXPosition() - agent2.getXPosition()), Math.abs(agent1.getYPosition() - agent2.getYPosition())));
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

        public int getXPosition() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getYPosition() {
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
            this.board.addAgent(agent.getID(), agent.getXPosition(), agent.getYPosition(), agent.getHp(), agent.getPossibleHp(), agent.getAttackDamage(), agent.getAttackRange());
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

        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            utility += (double) agent.getHp() / agent.getPossibleHp();
        }

        for (MMAgent agent : this.board.getAliveBadAgents()) {
            utility += agent.getPossibleHp() - agent.getHp();
        }

        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            utility += this.board.findAttackableAgents(agent).size();
        }


        double percentageBlocked = blockedPercent();
        if (percentageBlocked > 0) {
            this.utility += -1000 * percentageBlocked;
        } else {
            this.utility += -enemyDistance();
        }

        if (this.board.getAliveGoodAgents().isEmpty()) {
            this.utility = Double.NEGATIVE_INFINITY;
        }

        if (this.board.getAliveBadAgents().isEmpty()) {
            this.utility = Double.POSITIVE_INFINITY;
        }

        this.utilityCalculated = true;
        return this.utility;
    }


    private double blockedPercent() {
        int blocked = 0;
        int good = 0;
        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            MMAgent bad = this.getClosestEnemy(agent);
            int x = agent.getXPosition();
            int y = agent.getYPosition();

            if (bad == null)
                break;

            while (x != bad.getXPosition() || y != bad.getYPosition()) {
                if (this.board.isOnBoard(x, y) && this.board.isResource(x, y)) {
                    blocked++;
                }
                if (x < bad.getXPosition()) {
                    x++;
                } else if (x > bad.getXPosition()) {
                    x--;
                }
                if (y < bad.getYPosition()) {
                    y++;
                } else if (y > bad.getYPosition()) {
                    y--;
                }
            }

            good++;
        }
        if (good == 0) {
            return 0;
        }
        return (double) blocked / good;
    }

    private double enemyDistance() {
        double utility = 0.0;
        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            double value = Double.POSITIVE_INFINITY;
            for (MMAgent badAgent : this.board.getAliveBadAgents()) {
                value = Math.min(this.board.distance(agent, badAgent), value);
            }

            utility += Math.max(value, 0);
        }
        return utility;
    }

    private MMAgent getClosestEnemy(MMAgent goodAgent) {
        MMAgent enemy = null;
        for (MMAgent badAgent : this.board.getAliveBadAgents()) {
            if (enemy == null) {
                enemy = badAgent;
            } else if (this.board.distance(goodAgent, badAgent) < this.board.distance(goodAgent, enemy)) {
                enemy = badAgent;
            }
        }
        return enemy;
    }


    public List<GameStateChild> getChildren() {

        ArrayList<MMAgent> agents = isPlayerTurn ? board.getAliveGoodAgents() : board.getAliveBadAgents();

        ArrayList<ArrayList<Action>> actionsForEachAgent = new ArrayList<>();

        for (MMAgent agent : agents) {
            actionsForEachAgent.add(getAgentActions(agent));
        }

        List<Map<Integer, Action>> l = cartesianProductOf2(actionsForEachAgent);

        List<GameStateChild> gscTwo = new ArrayList<GameStateChild>();
        GameState gs = new GameState(this);
        for (Map<Integer, Action> actionHashMap : l) {
            for (Action action : actionHashMap.values()) {
                gs.applyAction(action);
            }
            GameStateChild resultingChild = new GameStateChild(actionHashMap, gs);
            gscTwo.add(resultingChild);
            gs = new GameState(this);
        }
        return gscTwo;
    }

    private static final Direction[] r = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private ArrayList<Action> getAgentActions(MMAgent agent) {
        ArrayList<Action> actions = new ArrayList<Action>();
        for (Direction direction : r) {
            int nextX = agent.getXPosition() + direction.xComponent();
            int nextY = agent.getYPosition() + direction.yComponent();
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
