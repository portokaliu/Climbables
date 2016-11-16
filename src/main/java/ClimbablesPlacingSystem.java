/*
 * Copyright 2016 MovingBlocks
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

import org.terasology.audio.AudioManager;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.ChunkMath;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.network.NetworkSystem;
import org.terasology.registry.In;
import org.terasology.utilities.Assets;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.items.BlockItemComponent;
import org.terasology.world.block.items.OnBlockItemPlaced;

import java.util.Optional;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ClimbablesPlacingSystem extends BaseComponentSystem {

    @In
    private WorldProvider worldProvider;

    @In
    private BlockEntityRegistry blockEntityRegistry;

    @In
    private AudioManager audioManager;

    @In
    private NetworkSystem networkSystem;

    @ReceiveEvent(components = {BlockItemComponent.class, ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onPlaceBlock(ActivateEvent event, EntityRef item) {
        if (!event.getTarget().exists()) {
            event.consume();
            return;
        }

        BlockItemComponent blockItem = item.getComponent(BlockItemComponent.class);
        BlockFamily type = blockItem.blockFamily;
        Side clamberSide = Side.TOP;
        Side secondaryDirection = ChunkMath.getSecondaryPlacementDirection(event.getDirection(), event.getHitNormal());

        BlockComponent blockComponent = event.getTarget().getComponent(BlockComponent.class);
        if (blockComponent == null) {
            // If there is no block there (i.e. it's a BlockGroup, we don't allow placing block, try somewhere else)
            event.consume();
            return;
        }

        boolean canPlaceClimbly = false;
        Vector3i placementDirection = Vector3i.zero();
        ClamberComponent clamberComponent = event.getTarget().getComponent(ClamberComponent.class);
        if (clamberComponent != null)
        {
            if (clamberComponent.support) {
                canPlaceClimbly = true;
                placementDirection = clamberComponent.getPlacingModeDirection();
            }
        }

        Vector3i climbablePlacementPos = new Vector3i(blockComponent.getPosition());

        Block block = type.getBlockForPlacement(worldProvider, blockEntityRegistry, climbablePlacementPos, Side.LEFT, secondaryDirection);
        if (block == null)
        {
            event.consume();
            return;
        }

        EntityRef blockEntity = block.getEntity();
        Prefab blockPrefab = block.getPrefab().get();
        ClamberComponent climbyComponent = null;
        if (blockPrefab != null) {
            climbyComponent = blockPrefab.getComponent(ClamberComponent.class);
        }

        if (climbyComponent != null) {
            if (!climbyComponent.support) {
                if (!canPlaceClimbly) {
                    event.consume();
                    return;
                }
            }
            else
            {
                return;
            }
        }
        else
        {
            return;
        }

        if (blockComponent.getBlock().isClimbable() && canPlaceClimbly && climbyComponent != null) {

            if (climbyComponent.placingMode != clamberComponent.placingMode)
            {
                event.consume();
                return;
            }

            Block newBlock = null;
            ClamberComponent newBlockClamberComponent = null;
            do
            {
                climbablePlacementPos.add(placementDirection);
                newBlock = worldProvider.getBlock(climbablePlacementPos);
                newBlockClamberComponent = newBlock.getEntity().getComponent(ClamberComponent.class);
                if (newBlockClamberComponent == null)
                {
                    break;
                }
            }
            while(clamberComponent.placingMode == newBlockClamberComponent.placingMode);

            if (newBlock.isReplacementAllowed()) {

                ClamberComponent newClamberComponent = new ClamberComponent();
                newClamberComponent.placingMode = climbyComponent.placingMode;
                newClamberComponent.support = true;
                blockEntity.addOrSaveComponent(newClamberComponent);

                PlaceBlocks placeBlocks = new PlaceBlocks(climbablePlacementPos, block, event.getInstigator());
                worldProvider.getWorldEntity().send(placeBlocks);

                if (!placeBlocks.isConsumed()) {
                    item.send(new OnBlockItemPlaced(climbablePlacementPos, blockEntityRegistry.getBlockEntityAt(climbablePlacementPos)));
                }
                event.getInstigator().send(new PlaySoundEvent(Assets.getSound("engine:PlaceBlock").get(), 0.5f));
            }
        }

        event.consume();
    }
}
