import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {
    public static class Pair implements Comparable<Pair> {
        private long id;
        private double dist;

        private Pair(long id, double dist) {
            this.id = id;
            this.dist = dist;
        }

        @Override
        public int compareTo(Pair pair) {
            return this.dist - pair.dist > 0 ? 1 : -1;
        }
    }
    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {
        long start = g.closest(stlon, stlat);
        long dest = g.closest(destlon, destlat);
        PriorityQueue<Pair> pq = new PriorityQueue<>();
        pq.offer(new Pair(start, g.distance(start, dest)));
        Map<Long, Double> shortestDist = new HashMap<>();
        shortestDist.put(start, 0.0);
        Set<Long> visited = new HashSet<>();
        Map<Long, Long> edgeToMap = new HashMap<>();
        while (!pq.isEmpty()) {
            Pair pair = pq.poll();
            if (visited.contains(pair.id)) {
                continue;
            }
            if (pair.id == dest) {
                break;
            }
            visited.add(pair.id);
            for (long nei : g.adjacent(pair.id)) {
                double tempDist = shortestDist.get(pair.id) + g.distance(pair.id, nei);
                if (visited.contains(nei) || shortestDist.getOrDefault(nei, Double.MAX_VALUE) <= tempDist) {
                    continue;
                }
                edgeToMap.put(nei, pair.id);
                shortestDist.put(nei, tempDist);
                pq.offer(new Pair(nei, g.distance(nei, dest) + tempDist));
            }
        }
        LinkedList<Long> res = new LinkedList<>();
        Long curr = dest;
        while (curr != start) {
            res.addFirst(curr);
            curr = edgeToMap.get(curr);
            if (curr == null) {
                return res;
            }
        }
        res.addFirst(start);
        return res;
    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigatiionDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        List<NavigationDirection> res = new ArrayList<>();
        double dist = 0.0;
        Long lastNodeID = route.get(0), currNodeID = route.get(1);
        String lastWayName = g.getNode(lastNodeID).neighbors.get(currNodeID).extrainfo.get("name");
        double lastBearing = g.bearing(lastNodeID, currNodeID);
        int relativeDirection = 0;
        for (int i = 1; i < route.size(); i++) {
            lastNodeID = route.get(i - 1);
            currNodeID = route.get(i);
            GraphDB.Node lastNode = g.getNode(lastNodeID);
            double currBearing = g.bearing(lastNodeID, currNodeID);
            String currWayName = lastNode.neighbors.get(currNodeID).extrainfo.get("name");
            if (currWayName.equals(lastWayName)) {
                dist += g.distance(currNodeID, lastNodeID);
            } else {
                NavigationDirection nd = new NavigationDirection();
                nd.direction = relativeDirection;
                relativeDirection = getDirection(lastBearing, currBearing);
                nd.distance = dist;
                nd.way = lastWayName;
                dist = g.distance(currNodeID, lastNodeID);
                lastWayName = currWayName;
                res.add(nd);
            }
            if (i == route.size() - 1) {
                NavigationDirection nd = new NavigationDirection();
                nd.direction = relativeDirection;
                nd.way = currWayName;;
                nd.distance = dist;
                res.add(nd);
            }
            lastBearing = currBearing;
        }
        return res; // FIXME
    }

    public static int getDirection(double lastBearing, double currBearing) {
        /*
        double deg = currBearing - lastBearing;
        if (deg > 180) {
            deg = -360 + deg;
        } else if (deg < -180) {
            deg = -360 - deg;
        }
        if (deg <= 15 && deg >= -15) {
            return 1;
        } else if (deg >= -30 && deg < -15) {
            return 2;
        } else if (deg <= 30 && deg > 15) {
            return 3;
        } else if (deg >= -100 && deg < -30) {
            return 4;
        } else if (deg <= 100 && deg > 30) {
            return 5;
        } else if (deg < -100) {
            return 6;
        } else {
            return 7;
        }*/
        double relativeBearing = currBearing - lastBearing;
        double absBearing = Math.abs(relativeBearing);
        if (absBearing > 180) {
            absBearing = 360 - absBearing;
            relativeBearing *= -1;
        }
        if (absBearing <= 15) {
            return 1;
        }
        if (absBearing <= 30) {
            return relativeBearing < 0 ? 2 : 3;
        }
        if (absBearing <= 100) {
            return relativeBearing < 0 ? 5 : 4;
        } else {
            return relativeBearing < 0 ? 6 : 7;
        }
    }


    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
