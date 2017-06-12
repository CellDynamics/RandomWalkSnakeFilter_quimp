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

  private Snake data;
  private ImageProcessor ip;
  private RandomWalkSegmentation rws;
  private ViewUpdater qcontext;
  private ParamList uiDefinition;
  private RandomWalkParams params;

  public RandomWalkSnakeFilter_() {
    params = new RandomWalkParams();
    uiDefinition = new ParamList(); // will hold ui definitions
    // configure window, names of UI elements are also names of variables
    // exported/imported by set/getPluginConfig
    uiDefinition.put("name", "RandomWalkSnakeFilter"); // name of win
    uiDefinition.put("alpha", "spinner, 1, 10000, 1," + Double.toString(params.alpha));
    uiDefinition.put("beta", "spinner, 1, 10000, 1," + Double.toString(params.beta));
    uiDefinition.put("iter", "spinner, 1, 100000, 1," + Integer.toString(params.iter));
    uiDefinition.put("relim", "spinner, 1e-4, 10, 1," + Double.toString(params.relim[0]));

    buildWindow(uiDefinition); // construct ui (not shown yet)
  }

  @Override
  public void attachImage(ImageProcessor img) {
    ip = img;
    // new ImagePlus("", img).show();

  }

  @Override
  public int setup() {
    return DOES_SNAKES + CHANGE_SIZE;
  }

  @Override
  public void setPluginConfig(ParamList par) throws QuimpPluginException {
    try {
      setValues(par); // populate loaded values to UI
      Double[] relimTmp =
              new Double[] { par.getDoubleValue("relim"), par.getDoubleValue("relim") / 2 };
      // nulls stand for default values
      params = new RandomWalkParams(par.getDoubleValue("alpha"), par.getDoubleValue("beta"), null,
              null, par.getIntValue("iter"), null, relimTmp, null, null);
    } catch (Exception e) {
      throw new QuimpPluginException("Wrong input argument-> " + e.getMessage(), e);
    }

  }

  @Override
  public ParamList getPluginConfig() {
    return getValues();
  }

  @Override
  public int showUi(boolean val) {
    toggleWindow(val);
    return 0;
  }

  @Override
  public String getVersion() {
    String trimmedClassName = getClass().getSimpleName();
    trimmedClassName = trimmedClassName.substring(0, trimmedClassName.length() - 1); // no _
    // _ at the end of class does not appears in final jar name, we need it to
    // distinguish between plugins
    return PropertyReader.readProperty(getClass(), trimmedClassName,
            "quimp/plugin/plugin.properties", "internalVersion");
  }

  @Override
  public String about() {
    return "Random Walk segmentation filter\nAuthor: Piotr Baniukiewicz\n"
            + "mail: p.baniukiewicz@warwick.ac.uk";
  }

  @Override
  public Snake runPlugin() throws QuimpPluginException {
    Double[] relimTmp = new Double[] { getDoubleFromUI("relim"), getDoubleFromUI("relim") / 2 };
    params = new RandomWalkParams(getDoubleFromUI("alpha"), getDoubleFromUI("beta"), null, null,
            getIntegerFromUI("iter"), null, relimTmp, null, null);
    rws = new RandomWalkSegmentation(ip, params);
    ImageProcessor mask = new ByteProcessor(ip.getWidth(), ip.getHeight());
    Roi roi = data.asIntRoi();
    roi.setFillColor(Color.WHITE);
    roi.setStrokeColor(Color.WHITE);
    mask.drawRoi(roi);
    mask = mask.convertToRGB();

    Map<Seeds, ImageProcessor> seeds =
            RandomWalkSegmentation.decodeSeeds(mask, Color.WHITE, Color.BLACK);
    PropagateSeeds propagateSeeds = PropagateSeeds.getPropagator(Propagators.CONTOUR, true);
    seeds = propagateSeeds.propagateSeed(seeds.get(Seeds.FOREGROUND), 10, 10);

    // new ImagePlus("", mask).show();
    // new ImagePlus("FG", seeds.get(Seeds.FOREGROUND)).show();
    // new ImagePlus("BG", seeds.get(Seeds.BACKGROUND)).show();
    ImageProcessor dup = ip.duplicate();
    dup.setLut(IJTools.getGrayLut());
    propagateSeeds.getCompositeSeed(new ImagePlus("", dup), 0).show();

    ImageProcessor ret = rws.run(seeds);
    // new ImagePlus("res", ret).show();
    TrackOutline track = new TrackOutline(ret, 0);
    List<Outline> outline = track.getOutlines(2, false);
    return new QuimpDataConverter(outline.get(0)).getSnake(data.getSnakeID());
  }

  @Override
  public void attachData(Snake data) {
    if (data == null) {
      return;
    }
    this.data = data; // FIXME make copy ?

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
    applyB.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        Object b = e.getSource();
        if (b == applyB) { // pressed apply, copy ui data to plugin
          qcontext.updateView();
        }

      }
    }); // attach listener to apply button
  }

  @Override
  public void attachContext(ViewUpdater b) {
    qcontext = b;
  }

}
