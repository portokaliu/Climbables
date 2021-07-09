// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.climbables;

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.math.Side;
import org.terasology.gestalt.entitysystem.component.Component;

public class ClamberComponent implements Component<ClamberComponent> {

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

    @Override
    public void copy(ClamberComponent other) {
        this.placingMode = other.placingMode;
        this.support = other.support;
        this.maxPlacementDistance = other.maxPlacementDistance;
    }
}
