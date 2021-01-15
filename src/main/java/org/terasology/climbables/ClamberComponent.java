/*
 * Copyright 2018 MovingBlocks
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
package org.terasology.climbables;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.entitySystem.Component;
import org.terasology.math.Side;

public class ClamberComponent implements Component {

    public PlacingMode placingMode = PlacingMode.NORMAL;
    public boolean support = false;
    public int maxPlacementDistance = 3;

    public Vector3ic getPlacingModeDirection() {
        switch (this.placingMode) {
            case ROPING:
                return Side.BOTTOM.direction();
            case STACKING:
                return Side.TOP.direction();
            case NORMAL:
            default:
                return new Vector3i(0, 0, 0);
        }
    }
}
