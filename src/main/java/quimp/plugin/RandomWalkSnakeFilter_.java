package quimp.plugin;

import com.github.celldynamics.quimp.Snake;
import com.github.celldynamics.quimp.plugin.IQuimpPluginAttachImage;
import com.github.celldynamics.quimp.plugin.ParamList;
import com.github.celldynamics.quimp.plugin.QuimpPluginException;
import com.github.celldynamics.quimp.plugin.snakes.IQuimpBOASnakeFilter;

import ij.process.ImageProcessor;

public class RandomWalkSnakeFilter_ implements IQuimpPluginAttachImage, IQuimpBOASnakeFilter {

  @Override
  public void attachImage(ImageProcessor img) {
    // TODO Auto-generated method stub

  }

  @Override
  public int setup() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void setPluginConfig(ParamList par) throws QuimpPluginException {
    // TODO Auto-generated method stub

  }

  @Override
  public ParamList getPluginConfig() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int showUi(boolean val) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getVersion() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String about() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Snake runPlugin() throws QuimpPluginException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void attachData(Snake data) {
    // TODO Auto-generated method stub

  }

}
