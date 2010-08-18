/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.clustering.syntheticcontrol.meanshift;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.hadoop.fs.Path;
import org.apache.mahout.clustering.meanshift.MeanShiftCanopyDriver;
import org.apache.mahout.clustering.syntheticcontrol.Constants;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.utils.clustering.ClusterDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Job extends MeanShiftCanopyDriver {

  private static final Logger log = LoggerFactory.getLogger(Job.class);

  private Job() {
  }

  public static void main(String[] args) throws Exception {
    if (args.length > 0) {
      log.info("Running with only user-supplied arguments");
      new Job().run(args);
    } else {
      log.info("Running with default arguments");
      Path output = new Path("output");
      HadoopUtil.overwriteOutput(output);
      new Job().job(new Path("testdata"), output, new EuclideanDistanceMeasure(), 47.6, 1, 0.5, 10);
    }
  }

  @Override
  public int run(String[] args) throws Exception {
    addInputOption();
    addOutputOption();
    addOption(DefaultOptionCreator.convergenceOption().create());
    addOption(DefaultOptionCreator.maxIterationsOption().create());
    addOption(DefaultOptionCreator.overwriteOption().create());
    addOption(new DefaultOptionBuilder().withLongName(INPUT_IS_CANOPIES_OPTION).withRequired(false).withShortName("ic")
        .withArgument(new ArgumentBuilder().withName(INPUT_IS_CANOPIES_OPTION).withMinimum(1).withMaximum(1).create())
        .withDescription("If present, the input directory already contains MeanShiftCanopies").create());
    addOption(DefaultOptionCreator.distanceMeasureOption().create());
    addOption(DefaultOptionCreator.t1Option().create());
    addOption(DefaultOptionCreator.t2Option().create());
    addOption(DefaultOptionCreator.clusteringOption().create());

    Map<String, String> argMap = parseArguments(args);
    if (argMap == null) {
      return -1;
    }

    Path input = getInputPath();
    Path output = getOutputPath();
    if (hasOption(DefaultOptionCreator.OVERWRITE_OPTION)) {
      HadoopUtil.overwriteOutput(output);
    }
    String measureClass = getOption(DefaultOptionCreator.DISTANCE_MEASURE_OPTION);
    double t1 = Double.parseDouble(getOption(DefaultOptionCreator.T1_OPTION));
    double t2 = Double.parseDouble(getOption(DefaultOptionCreator.T2_OPTION));
    boolean runClustering = hasOption(DefaultOptionCreator.CLUSTERING_OPTION);
    double convergenceDelta = Double.parseDouble(getOption(DefaultOptionCreator.CONVERGENCE_DELTA_OPTION));
    int maxIterations = Integer.parseInt(getOption(DefaultOptionCreator.MAX_ITERATIONS_OPTION));
    boolean inputIsCanopies = hasOption(INPUT_IS_CANOPIES_OPTION);
    ClassLoader ccl = Thread.currentThread().getContextClassLoader();
    DistanceMeasure measure = (DistanceMeasure) ((Class<?>) ccl.loadClass(measureClass)).newInstance();

    runJob(input, output, measure, t1, t2, convergenceDelta, maxIterations, inputIsCanopies, runClustering, false);
    return 0;
  }

  /**
   * Run the meanshift clustering job on an input dataset using the given distance measure, t1, t2 and
   * iteration parameters. All output data will be written to the output directory, which will be initially
   * deleted if it exists. The clustered points will reside in the path <output>/clustered-points. By default,
   * the job expects the a file containing synthetic_control.data as obtained from
   * http://archive.ics.uci.edu/ml/datasets/Synthetic+Control+Chart+Time+Series resides in a directory named
   * "testdata", and writes output to a directory named "output".
   * 
   * @param input
   *          the String denoting the input directory path
   * @param output
   *          the String denoting the output directory path
   * @param measure
   *          the DistanceMeasure to use
   * @param t1
   *          the meanshift canopy T1 threshold
   * @param t2
   *          the meanshift canopy T2 threshold
   * @param convergenceDelta
   *          the double convergence criteria for iterations
   * @param maxIterations
   *          the int maximum number of iterations
   */
  private void job(Path input,
                   Path output,
                   DistanceMeasure measure,
                   double t1,
                   double t2,
                   double convergenceDelta,
                   int maxIterations) throws IOException, InterruptedException, ClassNotFoundException, InstantiationException,
      IllegalAccessException {
    Path directoryContainingConvertedInput = new Path(output, Constants.DIRECTORY_CONTAINING_CONVERTED_INPUT);
    InputDriver.runJob(input, directoryContainingConvertedInput);
    MeanShiftCanopyDriver.runJob(directoryContainingConvertedInput,
                                 output,
                                 measure,
                                 t1,
                                 t2,
                                 convergenceDelta,
                                 maxIterations,
                                 true,
                                 true,
                                 false);
    // run ClusterDumper
    ClusterDumper clusterDumper = new ClusterDumper(new Path(output, "clusters-" + maxIterations), new Path(output,
                                                                                                            "clusteredPoints"));
    clusterDumper.printClusters(null);
  }

}
