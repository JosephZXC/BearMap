import java.util.HashMap;
import java.util.Map;


/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    public static final int max_depth = 7;
    public static final double[] DPPs = new double[max_depth + 1];
    public static final double[] widths = new double[max_depth + 1];
    public static final double[] heights = new double[max_depth + 1];
    public Rasterer() {
        // YOUR CODE HERE
        DPPs[0] = calcLonDPP(MapServer.ROOT_LRLON, MapServer.ROOT_ULLON, MapServer.TILE_SIZE);
        widths[0] = MapServer.ROOT_LRLON - MapServer.ROOT_ULLON;
        heights[0] = MapServer.ROOT_ULLAT - MapServer.ROOT_LRLAT;
        for (int i = 1; i <= max_depth; i++) {
            DPPs[i] = DPPs[i - 1] / 2;
            widths[i] = widths[i - 1] / 2;
            heights[i] = heights[i - 1] / 2;
        }
    }

    public double calcLonDPP(double lr_lon, double ul_lon, double pixel_size) {
        return (lr_lon - ul_lon) / pixel_size;
    }

    public int getDepth(double target) {
        int i;
        for (i = 0; i < DPPs.length; i++) {
            if (DPPs[i] <= target) {
                break;
            }
        }
        if (i == DPPs.length) return i - 1;
        return i;
    }

    public double[] getGridCo(int x, int y, int depth) {
        double[] grid = new double[4];
        // ullon, ullat, lrlon, lrlat
        grid[0] = MapServer.ROOT_ULLON + x * widths[depth];
        grid[1] = MapServer.ROOT_ULLAT - y * heights[depth];
        grid[2] = grid[0] + widths[depth];
        grid[3] = grid[1] - heights[depth];
        return grid;
    }

    public int[] getULGrid(double targetULLon, double targetULLat, int depth) {
        int[] res = new int[2];
        res[0] =  (int) Math.floor((targetULLon - MapServer.ROOT_ULLON) / widths[depth]);
        res[1] = (int) Math.floor((MapServer.ROOT_ULLAT - targetULLat) / heights[depth]);
        return res;
    }

    public int[] getLRGrid(double targetLRLon, double targetLRLat, int depth) {
        int[] res = new int[2];
        res[0] =  (int) Math.ceil((targetLRLon - MapServer.ROOT_ULLON) / widths[depth]) - 1;
        res[1] = (int) Math.ceil((MapServer.ROOT_ULLAT - targetLRLat) / heights[depth]) - 1;
        return res;
    }

    public int[] checkQuerySuccess(double targetULLon, double targetULLat, double targetLRLon, double targetLRLat, int depth) {
        int[] res = new int[4];
        if (targetLRLat >= targetULLat || targetULLon >= targetLRLon) return null;
        int[] ulGrid = getULGrid(targetULLon, targetULLat, depth);
        int[] lrGrid = getLRGrid(targetLRLon, targetLRLat, depth);
        if (lrGrid[0] < 0 || lrGrid[1] < 0) return null;
        if (ulGrid[0] >= Math.pow(2, depth) || ulGrid[1] >= Math.pow(2, depth)) return null;
        res[0] = ulGrid[0] < 0 ? 0 : ulGrid[0];
        res[1] = ulGrid[1] < 0 ? 0 : ulGrid[1];
        res[2] = lrGrid[0] >= Math.pow(2, depth) ? (int) Math.pow(2, depth) - 1 : lrGrid[0];
        res[3] = lrGrid[1] >= Math.pow(2, depth) ? (int) Math.pow(2, depth) - 1 : lrGrid[1];
        return res;
    }





    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        //System.out.println(params);
        Map<String, Object> results = new HashMap<>();

        double targetDPP = calcLonDPP(params.get("lrlon"), params.get("ullon"), params.get("w"));
        int depth = getDepth(targetDPP);
        //int[] ulGrid = getULGrid(params.get("ullon"), params.get("ullat"), depth);
        //int[] lrGrid = getLRGrid(params.get("lrlon"), params.get("lrlat"), depth);
        int[] resGrid = checkQuerySuccess(params.get("ullon"), params.get("ullat"), params.get("lrlon"), params.get("lrlat"), depth);
        boolean query_success = resGrid == null ? false : true;
        results.put("query_success", query_success);
        if (query_success) {
            double raster_ul_lon = getGridCo(resGrid[0], resGrid[1], depth)[0];
            double raster_ul_lat = getGridCo(resGrid[0], resGrid[1], depth)[1];
            double raster_lr_lon = getGridCo(resGrid[2], resGrid[3], depth)[2];
            double raster_lr_lat = getGridCo(resGrid[2], resGrid[3], depth)[3];
            String[][] render_grid = new String[resGrid[3] - resGrid[1] + 1][resGrid[2] - resGrid[0] + 1];
            for (int i = resGrid[0]; i <= resGrid[2]; i++) {
                for (int j = resGrid[1]; j <= resGrid[3]; j++) {
                    render_grid[j - resGrid[1]][i - resGrid[0]] = "d" + depth + "_x" + i + "_y" + j + ".png";
                }
            }
            results.put("depth", depth);
            results.put("raster_ul_lon", raster_ul_lon);
            results.put("raster_ul_lat", raster_ul_lat);
            results.put("raster_lr_lon", raster_lr_lon);
            results.put("raster_lr_lat", raster_lr_lat);
            results.put("render_grid", render_grid);
        } else {
            results.put("depth", 0);
            results.put("raster_ul_lon", 0.0);
            results.put("raster_ul_lat", 0.0);
            results.put("raster_lr_lon", 0.0);
            results.put("raster_lr_lat", 0.0);
            results.put("render_grid", new String[1][1]);
        }
        //System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
                     //      + "your browser.");
        return results;
    }

}
