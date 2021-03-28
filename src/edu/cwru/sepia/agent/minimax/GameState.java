package edu.cwru.sepia.agent.minimax;

import java.util.*;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

public class GameState {
    private static final Direction[] POSSIBLE_MOVES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private final Board board;
    private final boolean isPlayerTurn;
    private boolean utilityCalculated = false;
    private double utility = 0.0;

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
                attacked.setHp(attacked.getHP() - attacker.getAttackDamage());
            }
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

    private class MMAgent {
        private int hp;
        private final int possibleHp, attackDamage, attackRange, id;
        private int x, y;

        private boolean backwards = false;

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

        private int getHP() {
            return hp;
        }

        private void setHp(int hp) {
            this.hp = hp;
        }

        private int getMaxHP() {
            return possibleHp;
        }

        private int getAttackDamage() {
            return attackDamage;
        }

        private int getAttackRange() {
            return attackRange;
        }
    }

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

    public GameState(GameState gameState) {
        this.board = new Board(gameState.board.width, gameState.board.height);

        for (MMAgent agent : gameState.board.getAllAgents()) {
            this.board.addAgent(agent.getID(), agent.getX(), agent.getY(), agent.getHP(), agent.getMaxHP(), agent.getAttackDamage(), agent.getAttackRange());
        }

        for (Resource resource : gameState.board.resources.values()) {
            this.board.addResource(resource.getID(), resource.getX(), resource.getY());
        }

        this.isPlayerTurn = !gameState.isPlayerTurn;
        this.utilityCalculated = gameState.utilityCalculated;
        this.utility = gameState.utility;
    }


    public double getUtility() {
        if (this.utilityCalculated) {
            return this.utility;
        }

        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            utility += (double) agent.getHP() / (double) agent.getMaxHP();
        }


        for (MMAgent agent : this.board.getAliveBadAgents()) {
            utility += agent.getMaxHP() - agent.getHP();
        }


        for (MMAgent agent : this.board.getAliveGoodAgents()) {
            utility += this.board.findAttackableAgents(agent).size();
        }


        this.utility += getLocationUtility();

        if (this.board.getAliveGoodAgents().isEmpty()) {
            this.utility = Double.POSITIVE_INFINITY;
        }

        if (this.board.getAliveBadAgents().isEmpty()) {
            this.utility = Double.NEGATIVE_INFINITY;
        }

        this.utilityCalculated = true;
        return this.utility;
    }


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


    private double percentageOfBlockedFootmen() {
        int numBlocked = 0;
        int totalNumGood = 0;
        for (MMAgent goodGuy : this.board.getAliveGoodAgents()) {
            MMAgent badGuy = this.getClosestEnemy(goodGuy);
            if (badGuy != null) {
                int i = goodGuy.getX();
                int j = goodGuy.getY();
                while (i != badGuy.getX() || j != badGuy.getY()) {
                    if (this.board.canMove(i, j)) {
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
        return (double) numBlocked / (double) totalNumGood;
    }

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

    private ArrayList<Action> getAgentActions(MMAgent agent) {
        ArrayList<Action> actions = new ArrayList<Action>();
        for (Direction direction : POSSIBLE_MOVES) {
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
