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

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.terasology.audio.AudioManager;
import org.terasology.audio.events.PlaySoundEvent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.logic.health.DoDestroyEvent;
import org.terasology.logic.health.EngineDamageTypes;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.ChunkMath;
import org.terasology.math.Side;
import org.terasology.registry.In;
import org.terasology.utilities.Assets;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockComponent;
import org.terasology.world.block.entity.placement.PlaceBlocks;
import org.terasology.world.block.family.BlockFamily;
import org.terasology.world.block.family.BlockPlacementData;
import org.terasology.world.block.items.BlockItemComponent;
import org.terasology.world.block.items.OnBlockItemPlaced;

@RegisterSystem(RegisterMode.AUTHORITY)
public class ClimbablesPlacingSystem extends BaseComponentSystem {

    @In
    private WorldProvider worldProvider;

    @In
    private BlockEntityRegistry blockEntityRegistry;

    @In
    private AudioManager audioManager;
    @In
    private InventoryManager inventoryManager;

    @ReceiveEvent(priority = EventPriority.PRIORITY_HIGH)
    public void onDestroyed(DoDestroyEvent event, EntityRef entity, ClamberComponent clamberComponent, BlockComponent blockComponent) {
        Vector3i nextBlockPos = blockComponent.getPosition(new Vector3i());
        nextBlockPos.add(clamberComponent.getPlacingModeDirection());

        EntityRef nextBlockEntity = blockEntityRegistry.getBlockEntityAt(nextBlockPos);
        ClamberComponent nextClamberComponent = nextBlockEntity.getComponent(ClamberComponent.class);
        if (nextClamberComponent != null && clamberComponent.placingMode == nextClamberComponent.placingMode && !nextClamberComponent.support) {
            nextBlockEntity.send(new DoDestroyEvent(entity, entity, EngineDamageTypes.PHYSICAL.get()));
        }
    }

    @ReceiveEvent(components = {BlockItemComponent.class, ItemComponent.class}, priority = EventPriority.PRIORITY_HIGH)
    public void onPlaceBlock(ActivateEvent event, EntityRef item) {

        EntityRef targetEntity = event.getTarget();

        if (!targetEntity.exists()) {
            event.consume();
            return;
        }

        BlockItemComponent blockItem = item.getComponent(BlockItemComponent.class);
        BlockFamily type = blockItem.blockFamily;
        //Side clamberSide = Side.TOP;
        Side secondaryDirection = ChunkMath.getSecondaryPlacementDirection(event.getDirection(), event.getHitNormal());

        BlockComponent blockComponent = targetEntity.getComponent(BlockComponent.class);
        if (blockComponent == null) {
            // If there is no block there (i.e. it's a BlockGroup, we don't allow placing block, try somewhere else)
            event.consume();
            return;
        }

        Vector3i placementDirection = new Vector3i();
        ClamberComponent targetClamberComponent = targetEntity.getComponent(ClamberComponent.class);

        Vector3i climbablePlacementPos = blockComponent.getPosition(new Vector3i());
        Block blockToPlace = type.getBlockForPlacement(new BlockPlacementData(climbablePlacementPos, Side.LEFT, new Vector3f(secondaryDirection.direction())));
        if (blockToPlace == null) {
            event.consume();
            return;
        }

        ClamberComponent placingClamberComponent = blockToPlace.getEntity().getComponent(ClamberComponent.class);
        if (placingClamberComponent != null && !placingClamberComponent.support) {
            if (targetClamberComponent == null) {
                event.consume();
                return;
            } else {
                placementDirection.set(placingClamberComponent.getPlacingModeDirection());
            }
        } else {
            return;
        }

        if (placingClamberComponent.placingMode != targetClamberComponent.placingMode) {
            event.consume();
            return;
        }

        Block newBlock = null;
        ClamberComponent newBlockClamberComponent = null;
        int placementDistance = 0;
        do {
            placementDistance++;
            climbablePlacementPos.add(placementDirection);
            newBlock = worldProvider.getBlock(climbablePlacementPos);
            newBlockClamberComponent = newBlock.getEntity().getComponent(ClamberComponent.class);
            if (newBlockClamberComponent == null) {
                break;
            }
        }
        while (targetClamberComponent.placingMode == newBlockClamberComponent.placingMode && placementDistance < targetClamberComponent.maxPlacementDistance);

        if (newBlock.isReplacementAllowed()) {
            PlaceBlocks placeBlocks = new PlaceBlocks(climbablePlacementPos, blockToPlace, event.getInstigator());
            worldProvider.getWorldEntity().send(placeBlocks);

            if (!placeBlocks.isConsumed()) {
                item.send(new OnBlockItemPlaced(climbablePlacementPos, blockEntityRegistry.getBlockEntityAt(climbablePlacementPos), EntityRef.NULL));
            }

            event.getInstigator().send(new PlaySoundEvent(Assets.getSound("engine:PlaceBlock").get(), 0.5f));

            // ItemAuthoritySystem should be using another event instead of catching the same one as the BlockItemSystem.
            // OnBlockItemPlaced is a good replacement.
            ItemComponent itemComp = item.getComponent(ItemComponent.class);
            if (itemComp.consumedOnUse) {
                int slot = InventoryUtils.getSlotWithItem(event.getInstigator(), item);
                inventoryManager.removeItem(event.getInstigator(), event.getInstigator(), slot, true, 1);
            }
        }

        event.consume();
    }
}
