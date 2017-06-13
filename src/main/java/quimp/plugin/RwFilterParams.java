package quimp.plugin;

import com.github.celldynamics.quimp.plugin.ParamList;
import com.github.celldynamics.quimp.plugin.randomwalk.BinaryFilters;
import com.github.celldynamics.quimp.plugin.randomwalk.RandomWalkParams;
import com.github.celldynamics.quimp.plugin.utils.QWindowBuilder;

/**
 * Simplifies transferring options between {@link ParamList} and {@link RandomWalkParams}
 * 
 * @author p.baniukiewicz
 *
 */
class RwFilterParams extends RandomWalkParams {

  /**
   * Copy selected parameters from {@link ParamList} to underlying {@link RandomWalkParams} class.
   * Parameters not included in list are set to theirs default values.
   * 
   * @param params List of parameters obtained from GUI
   * @see QWindowBuilder
   */
  public RwFilterParams(ParamList params) {
    super(params.getDoubleValue("alpha"), params.getDoubleValue("beta"), null, null,
            params.getIntValue("iter"), null,
            new Double[] { params.getDoubleValue("relim"), params.getDoubleValue("relim") / 2 },
            null, null);
    if (params.getBooleanValue("clean") == true) {
      finalFilter = new BinaryFilters.MedianMorpho();
    }
  }
}
