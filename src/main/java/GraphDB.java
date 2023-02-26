import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */

    private final Map<Long, Node> graph = new HashMap<>();
    private final Trie trie = new Trie();
    private final Map<String, List<Long>> names = new HashMap<>();
    private final Map<Long, Node> rawGraph = new HashMap<>();
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // TODO: Your code here.
        Set<Long> temp = new HashSet<>();
        for (long id : graph.keySet()) {
            if (graph.get(id).neighbors.isEmpty()) {
                temp.add(id);
            }
        }
        for (long tempID : temp) {
            graph.remove(tempID);
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return graph.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        return graph.get(v).neighbors.keySet();
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long resID = 0;
        double dis = Double.MAX_VALUE;
        for (long nodeID : graph.keySet()) {
            double candDis = distance(graph.get(nodeID).lon, graph.get(nodeID).lat, lon, lat);
            if (candDis < dis) {
                dis = candDis;
                resID = nodeID;
            }
        }
        return resID;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        return graph.get(v).lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        return graph.get(v).lat;
    }

    Node getNode(long v) {
        return graph.get(v);
    }

    void addNode(Node node) {
        graph.put(node.id, node);
        rawGraph.put(node.id, node);
    }

    void addNodeName(Node node) {
        String name = node.extrainfo.get("name");
        String cleanedName = cleanString(name);
        trie.insert(cleanedName, name);
        List<Long> listOfIDs = names.getOrDefault(cleanedName, new ArrayList<>());
        listOfIDs.add(node.id);
        names.put(cleanedName, listOfIDs);
    }

    List<String> getLocationsByPrefix(String prefix) {
        return trie.colStringsStartsWith(cleanString(prefix));
    }

    List<Map<String, Object>> getLocations(String locationName) {
        List<Long> listOfIDs = names.get(cleanString(locationName));
        List<Map<String, Object>> infoList = new ArrayList<>();
        for (long id : listOfIDs) {
            Node node = rawGraph.get(id);
            Map<String, Object> info = new HashMap<>();
            info.put("lat", node.lat);
            info.put("lon", node.lon);
            info.put("name", node.extrainfo.get("name"));
            info.put("id", node.id);
            infoList.add(info);
        }
        return infoList;
    }


    void adEdge(long end1, long end2, Edge edge) {
        graph.get(end1).neighbors.put(end2, edge);
        graph.get(end2).neighbors.put(end1, edge);
    }

    public static class Node {
        long id;
        double lat;
        double lon;
        Map<Long, Edge> neighbors;
        Map<String, String> extrainfo;
        public Node(long id, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.neighbors = new HashMap<>();
            this.extrainfo = new HashMap<>();
        }
    }

    public static class Edge {
        long id;
        List<Long> nodeList;
        Map<String, String> extrainfo;
        boolean valid;
        public Edge(long id) {
            this.id = id;
            this.valid = false;
            this.extrainfo = new HashMap<>();
            this.nodeList = new ArrayList<>();
        }
    }

    public static class Trie {
        public static class TrieNode {
            boolean isEndOfWord;
            Map<Character, TrieNode> children;
            Set<String> wordSet = new TreeSet<>();
            TrieNode(boolean isEndOfWord) {
                this.isEndOfWord = isEndOfWord;
                this.children = new TreeMap<>();
            }
        }

        public TrieNode root;
        public Trie() {
            this.root = new TrieNode(true);
        }

        public void insert(String word, String name) {
            TrieNode node = root;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (!node.children.containsKey(c)) {
                    node.children.put(c, new TrieNode(false));
                }
                node = node.children.get(c);
            }
            node.isEndOfWord = true;
            node.wordSet.add(name);
        }

        public TrieNode startsWith(String prefix) {
            if (prefix == null) {
                return null;
            }
            TrieNode node = root;
            for (int i = 0; i < prefix.length(); i++) {
                char curr = prefix.charAt(i);
                node = node.children.get(curr);
                if (node == null) return null;
            }
            return node;
        }

        public void traverse(List<String> res, TrieNode node) {
            if (node == null) {
                return;
            }
            if (node.isEndOfWord) {
                res.addAll(node.wordSet);
            }
            for (Character c : node.children.keySet()) {
                traverse(res, node.children.get(c));
            }
        }

        public List<String> colStringsStartsWith(String prefix) {
            TrieNode trieNode = startsWith(prefix);
            List<String> res = new ArrayList<>();
            traverse(res, trieNode);
            return res;
        }
    }

}
