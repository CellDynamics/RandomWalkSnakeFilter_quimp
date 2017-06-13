package quimp.plugin;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import com.github.celldynamics.quimp.Outline;
import com.github.celldynamics.quimp.PropertyReader;
import com.github.celldynamics.quimp.Snake;
import com.github.celldynamics.quimp.ViewUpdater;
import com.github.celldynamics.quimp.geom.TrackOutline;
import com.github.celldynamics.quimp.plugin.IQuimpPluginAttachImage;
import com.github.celldynamics.quimp.plugin.IQuimpPluginSynchro;
import com.github.celldynamics.quimp.plugin.ParamList;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;
import com.github.celldynamics.quimp.plugin.randomwalk.PropagateSeeds;
import com.github.celldynamics.quimp.plugin.randomwalk.PropagateSeeds.Propagators;
import com.github.celldynamics.quimp.plugin.randomwalk.RandomWalkParams;
import com.github.celldynamics.quimp.plugin.randomwalk.RandomWalkSegmentation;
import com.github.celldynamics.quimp.plugin.randomwalk.RandomWalkSegmentation.Seeds;
import com.github.celldynamics.quimp.plugin.snakes.IQuimpBOASnakeFilter;
import com.github.celldynamics.quimp.plugin.utils.QWindowBuilder;
import com.github.celldynamics.quimp.plugin.utils.QuimpDataConverter;
import com.github.celldynamics.quimp.utils.IJTools;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Random Walk segmentation filter supporting BOA Active Contour segmentation.
 * 
 * @author p.baniukiewicz
 *
 */
public class RandomWalkSnakeFilter_ extends QWindowBuilder
        implements IQuimpPluginAttachImage, IQuimpBOASnakeFilter, IQuimpPluginSynchro {

  final private int TRACKING_STEP = 2; // resolution of Snake converted from mask [pix]
  private Snake inputSnake;
  private ImageProcessor ip;
  private ViewUpdater qcontext;
  private RandomWalkParams params;

  /**
   * Default constructor initialising GUI.
   */
  public RandomWalkSnakeFilter_() {
    ParamList uiDefinition = new ParamList(); // will hold ui definitions
    // configure window, names of UI elements are also names of variables
    // exported/imported by set/getPluginConfig
    params = new RandomWalkParams();
    uiDefinition.put("name", "RandomWalkSnakeFilter"); // name of win
    uiDefinition.put("alpha", "spinner: 1: 10000: 1:" + Double.toString(params.alpha));
    uiDefinition.put("beta", "spinner: 1: 10000: 1:" + Double.toString(params.beta));
    uiDefinition.put("iter", "spinner: 1: 100000: 1:" + Integer.toString(params.iter));
    uiDefinition.put("relim", "spinner: 1e-4: 10: 1:" + Double.toString(params.relim[0]));
    uiDefinition.put("clean", "checkbox: Median: false");
    uiDefinition.put("localMean", "checkbox: Local Mean: false");
    uiDefinition.put("LmWindow",
            "spinner: 3: 101: 2:" + Integer.toString(params.localMeanMaskSize));

    //!>
    uiDefinition.put("help", "<font size=\"3\">"
            + "<p><strong>Median</strong> - Apply 3x3 median filter to segmented image before "
            + "converting it to Snake"
            + "</p></font>");
    //!<
    buildWindow(uiDefinition); // construct ui (not shown yet)
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpPluginAttachImage#attachImage(ij.process.
   * ImageProcessor)
   */
  @Override
  public void attachImage(ImageProcessor img) {
    ip = img;

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin#setup()
   */
  @Override
  public int setup() {
    return DOES_SNAKES + CHANGE_SIZE;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.celldynamics.quimp.plugin.IQuimpCorePlugin#setPluginConfig(com.github.celldynamics.
   * quimp.plugin.ParamList)
   */
  @Override
  public void setPluginConfig(ParamList par) throws QuimpPluginException {
    try {
      setValues(par); // populate loaded values to UI
      params = new RwFilterParams(par); // convert list of params to RW compatible structure
    } catch (Exception e) {
      throw new QuimpPluginException("Wrong input argument-> " + e.getMessage(), e);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin#getPluginConfig()
   */
  @Override
  public ParamList getPluginConfig() {
    return getValues();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin#showUi(boolean)
   */
  @Override
  public int showUi(boolean val) {
    toggleWindow(val);
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin#getVersion()
   */
  @Override
  public String getVersion() {
    String trimmedClassName = getClass().getSimpleName();
    trimmedClassName = trimmedClassName.substring(0, trimmedClassName.length() - 1); // no _
    // _ at the end of class does not appears in final jar name, we need it to
    // distinguish between plugins
    return PropertyReader.readProperty(getClass(), trimmedClassName,
            "quimp/plugin/plugin.properties", "internalVersion");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.celldynamics.quimp.plugin.IQuimpCorePlugin#about()
   */
  @Override
  public String about() {
    return "Random Walk segmentation filter\nAuthor: Piotr Baniukiewicz\n"
            + "mail: p.baniukiewicz@warwick.ac.uk";
  }

  @Override
  public Snake runPlugin() throws QuimpPluginException {
    params = new RwFilterParams(getValues()); // get options from UI
    RandomWalkSegmentation rws = new RandomWalkSegmentation(ip, params); // build RW segmentation
    ImageProcessor mask = new ByteProcessor(ip.getWidth(), ip.getHeight()); // template of mask
    Roi roi = inputSnake.asIntRoi(); // convert snake to ROI for creating mask
    roi.setFillColor(Color.WHITE);
    roi.setStrokeColor(Color.WHITE);
    mask.drawRoi(roi); // create mask on template image
    mask = mask.convertToRGB();

    // convert mask to seeds
    Map<Seeds, ImageProcessor> seeds =
            RandomWalkSegmentation.decodeSeeds(mask, Color.WHITE, Color.BLACK);
    // use contour propagator for shrinking
    PropagateSeeds propagateSeeds = PropagateSeeds.getPropagator(Propagators.CONTOUR, true);
    // get new seeds using FG seed processed by propagator (shrink->new FG and expand->new BG)
    seeds = propagateSeeds.propagateSeed(seeds.get(Seeds.FOREGROUND), 10, 10);
    // mask to local mean
    seeds.put(Seeds.ROUGHMASK, mask.convertToByte(false));

    // new ImagePlus("", mask).show();
    // new ImagePlus("FG", seeds.get(Seeds.FOREGROUND)).show();
    // new ImagePlus("BG", seeds.get(Seeds.BACKGROUND)).show();
    ImageProcessor dup = ip.duplicate(); // for sed visualisation
    dup.setLut(IJTools.getGrayLut()); // convert to gray
    propagateSeeds.getCompositeSeed(new ImagePlus("", dup), 0).show(); // and show seeds

    ImageProcessor ret = rws.run(seeds); // run segmentaiton
    // new ImagePlus("res", ret).show();
    TrackOutline track = new TrackOutline(ret, 0); // for converting BW mask to snake
    List<Outline> outline = track.getOutlines(TRACKING_STEP, false); // get outline
    return new QuimpDataConverter(outline.get(0)).getSnake(inputSnake.getSnakeID()); // convert to
                                                                                     // Snake
  }

  @Override
  public void attachData(Snake data) {
    if (data == null) {
      return;
    }
    this.inputSnake = data; // FIXME make copy ?

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.celldynamics.quimp.plugin.utils.QWindowBuilder#buildWindow(com.github.celldynamics.
   * quimp.plugin.ParamList)
   */
  @Override
  public void buildWindow(ParamList def) {
    super.buildWindow(def);
    applyB.addActionListener(new ActionListener() { // update BOA on appy

      @Override
      public void actionPerformed(ActionEvent e) {
        Object b = e.getSource();
        if (b == applyB) { // pressed apply, copy ui inputSnake to plugin
          qcontext.updateView();
        }

      }
    }); // attach listener to apply button
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.github.celldynamics.quimp.plugin.IQuimpPluginSynchro#attachContext(com.github.celldynamics.
   * quimp.ViewUpdater)
   */
  @Override
  public void attachContext(ViewUpdater b) {
    qcontext = b;
  }

}
