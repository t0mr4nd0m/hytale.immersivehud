package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.serveraccess.Access;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class HudConfigPauseControllerTest {
    private static int checks;

    public static void main(String[] args) {
        verify("private singleplayer", allowed(true, true, Access.Private, Access.Private, 1));
        verify("initial unshared singleplayer", allowed(true, true, null, null, 1));
        verify("dedicated server with one player", !allowed(false, true, Access.Private, Access.Private, 1));
        verify("guest cannot pause", !allowed(true, false, Access.Private, Access.Private, 1));
        verify("two players in different worlds", !allowed(true, true, Access.Private, Access.Private, 2));
        for (Access access : new Access[]{Access.LAN, Access.Friend, Access.Open}) {
            verify("shared session: " + access, !allowed(true, true, access, access, 1));
            verify("opening sharing: " + access, !allowed(true, true, Access.Private, access, 1));
            verify("closing sharing: " + access, !allowed(true, true, access, Access.Private, 1));
        }

        var controller = new HudConfigPauseController();
        UUID player = UUID.randomUUID();
        var world = new FakeWorld();
        controller.pause(player, world, false);
        controller.release(player);
        verify("multiplayer does not alter pause state", world.changes == 0);

        controller.pause(player, world, true);
        verify("opening config pauses", world.paused && world.changes == 1);
        controller.pause(player, world, true);
        verify("repeated acquisition is harmless", world.changes == 1);
        controller.release(player);
        verify("closing config resumes owned world", !world.paused && world.changes == 2);
        controller.release(player);
        verify("apply then dismissal does not resume twice", world.changes == 2);

        world.paused = true;
        controller.pause(player, world, true);
        controller.release(player);
        verify("pre-existing pause is preserved", world.paused && world.changes == 2);

        world.paused = false;
        controller.pause(player, world, true);
        var destinationWorld = new FakeWorld();
        controller.release(player);
        verify("release affects original world after transfer", !world.paused && destinationWorld.changes == 0);

        controller.pause(player, world, true);
        controller.releaseAll();
        verify("shutdown or sharing releases owned pauses", !world.paused);
        world.paused = true;
        controller.releaseAll();
        verify("repeated cleanup leaves unrelated pause alone", world.paused);
        System.out.println("Passed " + checks + " configuration pause regression checks.");
    }

    private static boolean allowed(boolean singleplayer, boolean owner, Access access, Access requested, int players) {
        return HudConfigPauseController.canPause(singleplayer, owner, access, requested, players);
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }

    private static final class FakeWorld implements HudConfigPauseController.PauseTarget {
        boolean paused;
        int changes;
        @Override public boolean isPaused() { return paused; }
        @Override public void setPaused(boolean paused) { this.paused = paused; changes++; }
        @Override public void execute(@Nonnull Runnable action) { action.run(); }
    }
}
