/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf.taste.hadoop.item;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.RandomAccessSparseVectorWritable;
import org.apache.mahout.math.Vector;

public final class VectorOrPrefWritable implements Writable {

  private RandomAccessSparseVector vector;
  private long userID;
  private float value;

  public VectorOrPrefWritable() {
  }

  public VectorOrPrefWritable(RandomAccessSparseVector vector) {
    this.vector = vector;
  }

  public VectorOrPrefWritable(long userID, float value) {
    this.userID = userID;
    this.value = value;
  }

  public Vector getVector() {
    return vector;
  }

  public long getUserID() {
    return userID;
  }

  public float getValue() {
    return value;
  }

  public void set(RandomAccessSparseVector vector) {
    this.vector = vector;
    this.userID = Long.MIN_VALUE;
    this.value = Float.NaN;
  }

  public void set(long userID, float value) {
    this.vector = null;
    this.userID = userID;
    this.value = value;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    if (vector == null) {
      out.writeBoolean(false);
      out.writeLong(userID);
      out.writeFloat(value);
    } else {
      out.writeBoolean(true);
      new RandomAccessSparseVectorWritable(vector).write(out);
    }
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    boolean hasVector = in.readBoolean();
    if (hasVector) {
      RandomAccessSparseVectorWritable writable = new RandomAccessSparseVectorWritable();
      writable.readFields(in);
      set((RandomAccessSparseVector) writable.get());
    } else {
      long theUserID = in.readLong();
      float theValue = in.readFloat();
      set(theUserID, theValue);
    }
  }

}