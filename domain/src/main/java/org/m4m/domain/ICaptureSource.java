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

package org.m4m.domain;

public interface ICaptureSource extends IPluginOutput {
    /**
     * Initialize encoder with specific with and height, should be called from GLSurfaceView.Renderer.onSurfaceChanged
     *
     * @param width
     * @param height
     */
    public void setSurfaceSize(int width, int height);

    /**
     * Will capture all draw function happened in-between
     */
    public boolean beginCaptureFrame();

    public void endCaptureFrame();

    /**
     * start delivering end of stream
     */
    //public void stopCapture();


    /**
     * Register listener responsible to create and set surfaces to this source
     *
     * @param listenMe
     */
    void addSetSurfaceListener(ISurfaceListener listenMe);
}
