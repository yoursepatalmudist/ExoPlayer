/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.exoplayer2;

import static com.google.android.exoplayer2.MetadataRetriever.retrieveMetadata;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.exoplayer2.metadata.mp4.MotionPhoto;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link MetadataRetriever}. */
@RunWith(AndroidJUnit4.class)
public class MetadataRetrieverTest {

  private Context context;

  @Before
  public void setUp() throws Exception {
    context = ApplicationProvider.getApplicationContext();
  }

  @Test
  public void retrieveMetadata_singleMediaItem_outputsExpectedMetadata() throws Exception {
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse("asset://android_asset/media/mp4/sample.mp4"));

    ListenableFuture<TrackGroupArray> trackGroupsFuture = retrieveMetadata(context, mediaItem);
    TrackGroupArray trackGroups = waitAndGetTrackGroups(trackGroupsFuture);

    assertThat(trackGroups.length).isEqualTo(2);
    // Video group.
    assertThat(trackGroups.get(0).length).isEqualTo(1);
    assertThat(trackGroups.get(0).getFormat(0).sampleMimeType).isEqualTo(MimeTypes.VIDEO_H264);
    // Audio group.
    assertThat(trackGroups.get(1).length).isEqualTo(1);
    assertThat(trackGroups.get(1).getFormat(0).sampleMimeType).isEqualTo(MimeTypes.AUDIO_AAC);
  }

  @Test
  public void retrieveMetadata_multipleMediaItems_outputsExpectedMetadata() throws Exception {
    MediaItem mediaItem1 =
        MediaItem.fromUri(Uri.parse("asset://android_asset/media/mp4/sample.mp4"));
    MediaItem mediaItem2 =
        MediaItem.fromUri(Uri.parse("asset://android_asset/media/mp3/bear-id3.mp3"));

    ListenableFuture<TrackGroupArray> trackGroupsFuture1 = retrieveMetadata(context, mediaItem1);
    ListenableFuture<TrackGroupArray> trackGroupsFuture2 = retrieveMetadata(context, mediaItem2);
    TrackGroupArray trackGroups1 = waitAndGetTrackGroups(trackGroupsFuture1);
    TrackGroupArray trackGroups2 = waitAndGetTrackGroups(trackGroupsFuture2);

    // First track group.
    assertThat(trackGroups1.length).isEqualTo(2);
    // First track group - Video group.
    assertThat(trackGroups1.get(0).length).isEqualTo(1);
    assertThat(trackGroups1.get(0).getFormat(0).sampleMimeType).isEqualTo(MimeTypes.VIDEO_H264);
    // First track group - Audio group.
    assertThat(trackGroups1.get(1).length).isEqualTo(1);
    assertThat(trackGroups1.get(1).getFormat(0).sampleMimeType).isEqualTo(MimeTypes.AUDIO_AAC);

    // Second track group.
    assertThat(trackGroups2.length).isEqualTo(1);
    // Second track group - Audio group.
    assertThat(trackGroups2.get(0).length).isEqualTo(1);
    assertThat(trackGroups2.get(0).getFormat(0).sampleMimeType).isEqualTo(MimeTypes.AUDIO_MPEG);
  }

  @Test
  public void retrieveMetadata_heicMotionPhoto_outputsExpectedMetadata() throws Exception {
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse("asset://android_asset/media/mp4/sample_MP.heic"));
    MotionPhoto expectedMotionPhoto =
        new MotionPhoto(
            /* photoStartPosition= */ 0,
            /* photoSize= */ 28_853,
            /* videoStartPosition= */ 28_869,
            /* videoSize= */ 28_803);

    ListenableFuture<TrackGroupArray> trackGroupsFuture = retrieveMetadata(context, mediaItem);
    TrackGroupArray trackGroups = waitAndGetTrackGroups(trackGroupsFuture);

    assertThat(trackGroups.length).isEqualTo(1);
    assertThat(trackGroups.get(0).length).isEqualTo(1);
    assertThat(trackGroups.get(0).getFormat(0).metadata.length()).isEqualTo(1);
    assertThat(trackGroups.get(0).getFormat(0).metadata.get(0)).isEqualTo(expectedMotionPhoto);
  }

  @Test
  public void retrieveMetadata_heicStillPhoto_outputsEmptyMetadata() throws Exception {
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse("asset://android_asset/media/mp4/sample_still_photo.heic"));

    ListenableFuture<TrackGroupArray> trackGroupsFuture = retrieveMetadata(context, mediaItem);
    TrackGroupArray trackGroups = waitAndGetTrackGroups(trackGroupsFuture);

    assertThat(trackGroups.length).isEqualTo(1);
    assertThat(trackGroups.get(0).length).isEqualTo(1);
    assertThat(trackGroups.get(0).getFormat(0).metadata).isNull();
  }

  @Test
  public void retrieveMetadata_invalidMediaItem_throwsError() {
    MediaItem mediaItem =
        MediaItem.fromUri(Uri.parse("asset://android_asset/media/does_not_exist"));

    ListenableFuture<TrackGroupArray> trackGroupsFuture = retrieveMetadata(context, mediaItem);

    assertThrows(ExecutionException.class, () -> waitAndGetTrackGroups(trackGroupsFuture));
  }

  private static TrackGroupArray waitAndGetTrackGroups(
      ListenableFuture<TrackGroupArray> trackGroupsFuture)
      throws InterruptedException, ExecutionException {
    while (!trackGroupsFuture.isDone()) {
      // TODO: update once [Internal: b/168084145] is implemented.
      // Advance SystemClock so that messages that are sent with a delay to the MetadataRetriever
      // looper are received.
      SystemClock.setCurrentTimeMillis(SystemClock.uptimeMillis() + 100);
      Thread.sleep(/* millis= */ 100);
    }
    return trackGroupsFuture.get();
  }
}
