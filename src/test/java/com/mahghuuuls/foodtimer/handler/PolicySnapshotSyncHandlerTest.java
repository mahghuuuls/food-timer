package com.mahghuuuls.foodtimer.handler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicySnapshotSyncHandlerTest {

    @Test
    void loginRespawnAndDimensionRoutesAllRequestCompleteSnapshotSend() {
        List<EntityPlayer> sends = new ArrayList<>();
        PolicySnapshotSyncHandler handler = new PolicySnapshotSyncHandler(sends::add);

        handler.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(null));
        handler.onPlayerRespawn(new PlayerEvent.PlayerRespawnEvent(null, false));
        handler.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(null, 0, -1));

        assertEquals(3, sends.size());
    }
}
