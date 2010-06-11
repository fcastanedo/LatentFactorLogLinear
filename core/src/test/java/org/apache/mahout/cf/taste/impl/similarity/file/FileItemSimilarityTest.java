/*
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

package org.apache.mahout.cf.taste.impl.similarity.file;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.mahout.cf.taste.impl.TasteTestCase;
import org.apache.mahout.cf.taste.impl.similarity.GenericItemSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.GenericItemSimilarity.ItemItemSimilarity;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

/** <p>Tests {@link FileItemSimilarity}.</p> */
public class FileItemSimilarityTest extends TasteTestCase {

  private static final String[] data = {
      "1,5,0.125",
      "1,7,0.5" };

  private static final String[] changedData = {
      "1,5,0.125",
      "1,7,0.9",
      "7,8,0.112" };

  private File testFile;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    testFile = getTestTempFile("test.txt");
    writeLines(testFile, data);
  }

  public void testLoadFromFile() throws Exception {
    ItemSimilarity similarity = new FileItemSimilarity(testFile);

    assertEquals(0.125d, similarity.itemSimilarity(1L, 5L));
    assertEquals(0.125d, similarity.itemSimilarity(5L, 1L));
    assertEquals(0.5d, similarity.itemSimilarity(1L, 7L));
    assertEquals(0.5d, similarity.itemSimilarity(7L, 1L));

    assertTrue(Double.isNaN(similarity.itemSimilarity(7L, 8L)));

    double[] valuesForOne = similarity.itemSimilarities(1L, new long[] { 5L, 7L });
    assertNotNull(valuesForOne);
    assertEquals(2, valuesForOne.length);
    assertEquals(0.125d, valuesForOne[0]);
    assertEquals(0.5d, valuesForOne[1]);
  }

  public void testNoRefreshAfterFileUpdate() throws Exception {
    ItemSimilarity similarity = new FileItemSimilarity(testFile, 0L);

    /* call a method to make sure the original file is loaded*/
    similarity.itemSimilarity(1L, 5L);

    /* change the underlying file,
     * we have to wait at least a second to see the change in the file's lastModified timestamp */
    Thread.sleep(2000L);
    writeLines(testFile, changedData);

    /* we shouldn't see any changes in the data as we have not yet refreshed */
    assertEquals(0.5d, similarity.itemSimilarity(1L, 7L));
    assertEquals(0.5d, similarity.itemSimilarity(7L, 1L));
    assertTrue(Double.isNaN(similarity.itemSimilarity(7L, 8L)));
  }

  public void testRefreshAfterFileUpdate() throws Exception {
    ItemSimilarity similarity = new FileItemSimilarity(testFile, 0L);

    /* call a method to make sure the original file is loaded */
    similarity.itemSimilarity(1L, 5L);

    /* change the underlying file,
     * we have to wait at least a second to see the change in the file's lastModified timestamp */
    Thread.sleep(2000L);
    writeLines(testFile, changedData);

    similarity.refresh(null);

    /* we should now see the changes in the data */
    assertEquals(0.9d, similarity.itemSimilarity(1L, 7L));
    assertEquals(0.9d, similarity.itemSimilarity(7L, 1L));
    assertEquals(0.125d, similarity.itemSimilarity(1L, 5L));
    assertEquals(0.125d, similarity.itemSimilarity(5L, 1L));

    assertFalse(Double.isNaN(similarity.itemSimilarity(7L, 8L)));
    assertEquals(0.112d, similarity.itemSimilarity(7L, 8L));
    assertEquals(0.112d, similarity.itemSimilarity(8L, 7L));
  }

  public void testFileNotFoundExceptionForNonExistingFile() throws Exception {
    try {
      new FileItemSimilarity(new File("xKsdfksdfsdf"));
      fail();
    } catch (FileNotFoundException e) {}
  }

  public void testFileItemItemSimilarityIterable() throws Exception {
    Iterable<ItemItemSimilarity> similarityIterable = new FileItemSimilarity.FileItemItemSimilarityIterable(testFile);
    GenericItemSimilarity similarity = new GenericItemSimilarity(similarityIterable);

    assertEquals(0.125d, similarity.itemSimilarity(1L, 5L));
    assertEquals(0.125d, similarity.itemSimilarity(5L, 1L));
    assertEquals(0.5d, similarity.itemSimilarity(1L, 7L));
    assertEquals(0.5d, similarity.itemSimilarity(7L, 1L));

    assertTrue(Double.isNaN(similarity.itemSimilarity(7L, 8L)));

    double[] valuesForOne = similarity.itemSimilarities(1L, new long[] { 5L, 7L });
    assertNotNull(valuesForOne);
    assertEquals(2, valuesForOne.length);
    assertEquals(0.125d, valuesForOne[0]);
    assertEquals(0.5d, valuesForOne[1]);
  }

  public void testToString() throws Exception {
    ItemSimilarity similarity = new FileItemSimilarity(testFile);
    assertTrue(similarity.toString().length() > 0);
  }

}
