package quimp.plugin;

import static com.github.baniuk.ImageJTestSuite.dataaccess.ResourceLoader.loadResource;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.vecmath.Point2d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.celldynamics.quimp.Snake;
import com.github.celldynamics.quimp.plugin.ParamList;

import ij.IJ;
import ij.ImagePlus;

/**
 * @author p.baniukiewicz
 *
 */
public class RandomWalkSnakeFilter_Test {

  static final Logger LOGGER = LoggerFactory.getLogger(RandomWalkSnakeFilter_Test.class.getName());

  private RandomWalkSnakeFilter_ obj;
  private ParamList p;

  /**
   * Set all features on.
   * 
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    p = new ParamList();
    p.put("alpha", "400.0");
    p.put("beta", "50.0");
    p.put("iter", "100.0");
    p.put("relim", "0.008");
    p.put("shrinkpower", "2.0");
    p.put("localmean", "true");
    p.put("lmwindow", "9.0");
    p.put("clean", "true");
    p.put("maskpreview", "true");
    obj = new RandomWalkSnakeFilter_();

  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    obj = null;
  }

  /**
   * Get set plugin config.
   * 
   * @throws Exception
   */
  @Test
  public void testSetPluginConfig() throws Exception {
    obj.setPluginConfig(p);
    ParamList ret = obj.getPluginConfig();
    assertThat(ret, is(p));
  }

  /**
   * Check API compatibility.
   * 
   * @throws Exception
   */
  @Test
  public void testRunPlugin() throws Exception {
    ImagePlus img;
    Path path = loadResource(getClass().getClassLoader(), "July14ABD_GFP_actin_twoCells.tif");
    img = IJ.openImage(path.toString());
    // inside of one cell
    List<Point2d> l = new ArrayList<>();
    l.add(new Point2d(185, 279));
    l.add(new Point2d(216, 289));
    l.add(new Point2d(238, 309));
    l.add(new Point2d(226, 313));
    l.add(new Point2d(192, 300));
    Snake s = new Snake(l, 0);
    obj.setPluginConfig(p);
    obj.attachData(s);
    obj.attachImage(img.getProcessor());
    Snake ret = obj.runPlugin();
    LOGGER.debug(ret.toString());

  }

}