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

package org.apache.mahout.math.hadoop;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapred.join.CompositeInputFormat;
import org.apache.hadoop.mapred.join.TupleWritable;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class MatrixMultiplicationJob extends AbstractJob {

  private static final String OUT_CARD = "output.vector.cardinality";

  public static JobConf createMatrixMultiplyJobConf(Path aPath, Path bPath, Path outPath, int outCardinality) {
    JobConf conf = new JobConf(MatrixMultiplicationJob.class);
    conf.setInputFormat(CompositeInputFormat.class);
    conf.set("mapred.join.expr", CompositeInputFormat.compose(
          "inner", SequenceFileInputFormat.class, aPath, bPath));
    conf.setInt(OUT_CARD, outCardinality);
    conf.setOutputFormat(SequenceFileOutputFormat.class);
    FileOutputFormat.setOutputPath(conf, outPath);
    conf.setMapperClass(MatrixMultiplyMapper.class);
    conf.setCombinerClass(MatrixMultiplicationReducer.class);
    conf.setReducerClass(MatrixMultiplicationReducer.class);
    conf.setMapOutputKeyClass(IntWritable.class);
    conf.setMapOutputValueClass(VectorWritable.class);
    conf.setOutputKeyClass(IntWritable.class);
    conf.setOutputValueClass(VectorWritable.class);
    return conf;
  }

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new MatrixMultiplicationJob(), args);
  }

  @Override
  public int run(String[] strings) throws Exception {
    addOption("numRowsA", "nra", "Number of rows of the first input matrix");
    addOption("numColsA", "nca", "Number of columns of the first input matrix");
    addOption("numRowsB", "nrb", "Number of rows of the second input matrix");

    addOption("numColsB", "ncb", "Number of columns of the second input matrix");
    addOption("inputPathA", "ia", "Path to the first input matrix");
    addOption("inputPathB", "ib", "Path to the second input matrix");

    Map<String, String> argMap = parseArguments(strings);
    if (argMap == null) {
      return -1;
    }

    DistributedRowMatrix a = new DistributedRowMatrix(new Path(argMap.get("--inputPathA")),
                                                      new Path(argMap.get("--tempDir")),
                                                      Integer.parseInt(argMap.get("--numRowsA")),
                                                      Integer.parseInt(argMap.get("--numColsA")));
    DistributedRowMatrix b = new DistributedRowMatrix(new Path(argMap.get("--inputPathB")),
                                                      new Path(argMap.get("--tempDir")),
                                                      Integer.parseInt(argMap.get("--numRowsB")),
                                                      Integer.parseInt(argMap.get("--numColsB")));

    a.configure(new JobConf(getConf()));
    b.configure(new JobConf(getConf()));

    //DistributedRowMatrix c = a.times(b);
    a.times(b);
    return 0;
  }

  public static class MatrixMultiplyMapper extends MapReduceBase
      implements Mapper<IntWritable,TupleWritable,IntWritable,VectorWritable> {

    private int outCardinality;
    private final IntWritable row = new IntWritable();

    @Override
    public void configure(JobConf conf) {
      outCardinality = conf.getInt(OUT_CARD, Integer.MAX_VALUE);
    }

    @Override
    public void map(IntWritable index,
                    TupleWritable v,
                    OutputCollector<IntWritable,VectorWritable> out,
                    Reporter reporter) throws IOException {
      boolean firstIsOutFrag =  ((VectorWritable)v.get(0)).get().size() == outCardinality;
      Vector outFrag = firstIsOutFrag ? ((VectorWritable)v.get(0)).get() : ((VectorWritable)v.get(1)).get();
      Vector multiplier = firstIsOutFrag ? ((VectorWritable)v.get(1)).get() : ((VectorWritable)v.get(0)).get();

      Iterator<Vector.Element> it = multiplier.iterateNonZero();
      while (it.hasNext()) {
        Vector.Element e = it.next();
        row.set(e.index());
        VectorWritable outVector = new VectorWritable(outFrag.times(e.get()));
        out.collect(row, outVector);
      }
    }
  }

  public static class MatrixMultiplicationReducer extends MapReduceBase
      implements Reducer<IntWritable,VectorWritable,IntWritable,VectorWritable> {

    @Override
    public void reduce(IntWritable rowNum,
                       Iterator<VectorWritable> it,
                       OutputCollector<IntWritable,VectorWritable> out,
                       Reporter reporter) throws IOException {
      if (!it.hasNext()) {
        return;
      }
      Vector accumulator = new RandomAccessSparseVector(it.next().get());
      while (it.hasNext()) {
        Vector row = it.next().get();
        row.addTo(accumulator);
      }
      out.collect(rowNum, new VectorWritable(new SequentialAccessSparseVector(accumulator)));
    }
  }

}
