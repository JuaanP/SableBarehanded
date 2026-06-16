package dev.juaanp.sablebarehanded.client;

import dev.juaanp.sablebarehanded.config.ClientConfig;
import dev.juaanp.sablebarehanded.config.ServerConfig;
import dev.juaanp.sablebarehanded.mixin.accesor.MultiPlayerGameModeAccessor;
import dev.juaanp.sablebarehanded.network.DisassembleRequestPacket;
import dev.juaanp.sablebarehanded.physics.GrabPhysicsController;
import dev.juaanp.sablebarehanded.platform.Services;
import dev.juaanp.sablebarehanded.util.AssemblyBehaviorHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

public class ClientTickOrchestrator {
    private static Level lastLevel = null;
    private static Player lastPlayer = null;
    private static boolean wasDisassembleKeyDown = false;
    private static boolean wasAssemblingLastTick = false;

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            ClientGrabSession.forceResetAndNotify();
            ClientAssemblyTracker.reset();
            lastLevel = null;
            lastPlayer = null;
            return;
        }

        if (mc.level != lastLevel || mc.player != lastPlayer) {
            ClientGrabSession.forceResetAndNotify();
            ClientAssemblyTracker.reset();
            lastLevel = mc.level;
            lastPlayer = mc.player;
        }

        if (ClientGrabSession.isWaitingForGrabSync) {
            ClientGrabSession.waitingTicks++;
            if (ClientGrabSession.waitingTicks > 40) {
                ClientGrabSession.forceResetAndNotify();
            }
        }

        if (ClientGrabSession.isHoldingGrab) {
            mc.player.yBodyRot = mc.player.yHeadRot;
            mc.player.yBodyRotO = mc.player.yHeadRotO;
            ClientGrabSession.tickTetherStrain(mc.player);

            if (!ClientGrabSession.isWaitingForGrabSync && ClientGrabSession.grabbedSubLevelId != null) {
                SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
                if (container == null || container.getSubLevel(ClientGrabSession.grabbedSubLevelId) == null) {
                    ClientGrabSession.forceResetAndNotify();
                }
            }
        }

        if (mc.screen != null) {
            if (ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync) {
                ClientGrabSession.forceResetAndNotify();
            }
            ClientAssemblyTracker.reset();
            return;
        }

        ClientAssemblyTracker.tickAssemblyTether(mc);

        boolean isAttackDown = mc.options.keyAttack.isDown();
        boolean isUseDown = mc.options.keyUse.isDown();
        boolean bothDown = isAttackDown && isUseDown;
        boolean isSneaking = mc.player.isShiftKeyDown();

        ClientInputTracker.tickDebounce(bothDown);

        if (bothDown && !ClientGrabSession.isHoldingGrab && mc.player.getMainHandItem().isEmpty() && ClientInputTracker.canInitiateGrab()) {
            if (!ClientAssemblyTracker.isActive()) {
                double reach = GrabPhysicsController.getGrabReach(mc.player);
                HitResult hit = mc.player.pick(reach, 0.0f, false);

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    BlockPos currentPos = blockHit.getBlockPos();
                    Vec3 blockCenter = Vec3.atCenterOf(currentPos);
                    double distanceToHit = mc.player.getEyePosition().distanceTo(blockCenter);
                    BlockState state = mc.level.getBlockState(currentPos);
                    boolean isIgnored = AssemblyBehaviorHelper.isIgnored(mc.level, currentPos, state);
                    Vector3d hitPos = new Vector3d(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);

                    boolean preventDueToMining = false;
                    if (ClientConfig.INSTANCE.preventAssemblyWhenMining && mc.gameMode != null) {
                        float miningProgress = ((MultiPlayerGameModeAccessor) mc.gameMode).getDestroyProgress();
                        if (miningProgress > ClientConfig.INSTANCE.barehandedAssemblyMiningThreshold) preventDueToMining = true;
                    }

                    if (Sable.HELPER.getContaining(mc.level, hitPos) != null) {
                        Services.NETWORK.sendRequestGrab(currentPos);
                        ClientGrabSession.startWaiting();
                        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
                        ClientAssemblyTracker.reset();
                    } else if (isSneaking && ServerConfig.INSTANCE.enableBarehandedAssembly && distanceToHit <= ServerConfig.INSTANCE.barehandedAssemblyMaxDistance && !isIgnored && !preventDueToMining) {
                        ClientAssemblyTracker.tryStartAssembly(mc, blockHit, isSneaking);
                    }
                }
            } else {
                ClientAssemblyTracker.tickCharge(mc, isSneaking);
            }
        } else if (!bothDown && (ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync)) {
            ClientGrabSession.forceResetAndNotify();
            ClientAssemblyTracker.reset();
        } else if (!ClientGrabSession.isHoldingGrab && !ClientGrabSession.isWaitingForGrabSync) {
            ClientAssemblyTracker.reset();
        }

        boolean isRotateKeyDown = KeyBindings.ROTATE_KEY.isDown();
        if (ClientGrabSession.isHoldingGrab && (isRotateKeyDown || ClientInputTracker.pendingYaw != 0.0 || ClientInputTracker.pendingPitch != 0.0)) {
            boolean useCenter = ClientConfig.INSTANCE.rotateAroundCenter ^ KeyBindings.PIVOT_KEY.isDown();
            Services.NETWORK.sendRotateGrab(ClientInputTracker.pendingYaw, ClientInputTracker.pendingPitch, useCenter);

            ClientInputTracker.pendingYaw = 0.0;
            ClientInputTracker.pendingPitch = 0.0;
        }

        boolean isDisassembleKeyDown = KeyBindings.DISASSEMBLE_KEY.isDown();
        if (ClientGrabSession.isHoldingGrab && isDisassembleKeyDown && !wasDisassembleKeyDown) {
            Services.NETWORK.sendDisassembleRequest();
        }
        wasDisassembleKeyDown = isDisassembleKeyDown;

        boolean currentAssemblyState = ClientAssemblyTracker.isActive();
        if (currentAssemblyState != wasAssemblingLastTick) {
            Services.NETWORK.sendAssemblyStateToServer(currentAssemblyState);
            wasAssemblingLastTick = currentAssemblyState;
        }
    }
}