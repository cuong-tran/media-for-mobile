/*
 * Copyright 2014-2016 Media for Mobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.m4m.samples;

import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.widget.TextView;

import org.m4m.IVideoEffect;
import org.m4m.MediaComposer;
import org.m4m.MediaFile;
import org.m4m.Uri;
import org.m4m.domain.FileSegment;
import org.m4m.domain.Pair;
import org.m4m.effects.RotateEffect;

import java.io.IOException;

public class ComposerCutCoreActivity extends ComposerTranscodeCoreActivity {

    private long segmentFrom = 0;
    private long segmentTo = 0;
    private int video_rotation;

    @Override
    protected void getActivityInputs() {

        Bundle b = getIntent().getExtras();
        srcMediaName1 = b.getString("srcMediaName1");
        dstMediaPath = b.getString("dstMediaPath");
        mediaUri1 = new Uri(b.getString("srcUri1"));
        try {
            video_rotation = retrieveRotation(mediaUri1.toString());
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
        segmentFrom = b.getLong("segmentFrom");
        segmentTo = b.getLong("segmentTo");

    }

    public int retrieveRotation(String videoPath) throws IOException{


        // FileDescriptor fileDescriptor = new FileInputStream(android.net.Uri.parse(mediaUri1.getString()).getPath()).getFD();
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(this,android.net.Uri.parse(mediaUri1.getString()));

        String rotationString = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        return Integer.parseInt(rotationString);
    }

    @Override
    protected void setTranscodeParameters(MediaComposer mediaComposer) throws IOException {
segmentTo=4380160;
        segmentFrom=1434880;
        String mediaUri1 = "/storage/emulated/0/DCIM/Camera/20160516_165204.mp4";

        mediaComposer.setTargetFile(dstMediaPath);

        configureVideoEncoder(mediaComposer, 480, 480);
        configureAudioEncoder(mediaComposer);

//        IVideoEffect effect = new RotateEffect(video_rotation, factory.getEglUtil());
//        mediaComposer.addVideoEffect(effect);

        ///////////////////////////
        long t=segmentTo-segmentFrom;
        IVideoEffect effect = new RotateEffect(90, factory.getEglUtil());
        effect.setSegment(new FileSegment(0, t));
        mediaComposer.addVideoEffect(effect);
        mediaComposer.addSourceFile(mediaUri1);

        MediaFile mediaFile = mediaComposer.getSourceFiles().get(0);
        mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));

        effect = new RotateEffect(90, factory.getEglUtil());
        effect.setSegment(new FileSegment(t, 2*t));
        mediaComposer.addVideoEffect(effect);
        mediaComposer.addSourceFile(mediaUri1);
        mediaFile = mediaComposer.getSourceFiles().get(1);
        mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));

        effect = new RotateEffect(90, factory.getEglUtil());
        effect.setSegment(new FileSegment(2*t, 3*t));
        mediaComposer.addVideoEffect(effect);
        mediaComposer.addSourceFile(mediaUri1);
        mediaFile = mediaComposer.getSourceFiles().get(2);
        mediaFile.addSegment(new Pair<Long, Long>(segmentFrom, segmentTo));



    }

    @Override
    protected void printDuration() {

    	TextView v = (TextView)findViewById(R.id.durationInfo);
        v.setText(String.format("duration = %.1f sec\n", (float)(segmentTo - segmentFrom)/1e6));
        v.append(String.format("from = %.1f sec\nto = %.1f sec\n", (float)segmentFrom/1e6, (float)segmentTo/1e6));
    }
}

