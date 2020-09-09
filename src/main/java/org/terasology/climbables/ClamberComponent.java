// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.climbables;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.math.Side;
import org.terasology.math.geom.Vector3i;

public class ClamberComponent implements Component {

    public PlacingMode placingMode = PlacingMode.NORMAL;
    public boolean support = false;
    public int maxPlacementDistance = 3;

    public Vector3i getPlacingModeDirection() {
        switch (this.placingMode) {
            case ROPING:
                return Side.BOTTOM.getVector3i();
            case STACKING:
                return Side.TOP.getVector3i();
            case NORMAL:
                return Vector3i.zero();
        }
        return Vector3i.zero();
    }
}
