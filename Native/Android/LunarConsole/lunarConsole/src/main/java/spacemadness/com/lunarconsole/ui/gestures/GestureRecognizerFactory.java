//
//  GestureRecognizerFactory.java
//
//  Lunar Unity Mobile Console
//  https://github.com/SpaceMadness/lunar-unity-console
//
//  Copyright 2015-2021 Alex Lementuev, SpaceMadness.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//


package spacemadness.com.lunarconsole.ui.gestures;

import android.content.Context;

import spacemadness.com.lunarconsole.settings.Gesture;

import static spacemadness.com.lunarconsole.ui.gestures.TwoFingerSwipeGestureRecognizer.SwipeDirection;
import static spacemadness.com.lunarconsole.utils.UIUtils.dpToPx;

public class GestureRecognizerFactory {
    public static GestureRecognizer create(Context context, Gesture gesture) {
        switch (gesture) {
            case SWIPE_DOWN: {
                final float SWIPE_THRESHOLD = dpToPx(context, 100);
                return new TwoFingerSwipeGestureRecognizer(SwipeDirection.Down, SWIPE_THRESHOLD);
            }
        }

        return new NullGestureRecognizer();
    }
}
