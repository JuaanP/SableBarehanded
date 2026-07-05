package dev.juaanp.barehanded.client;

import dev.juaanp.barehanded.config.ClientConfig;
import dev.juaanp.barehanded.config.ServerConfig;
import dev.juaanp.barehanded.mixin.accesor.MultiPlayerGameModeAccessor;
import dev.juaanp.barehanded.physics.GrabPhysicsController;
import dev.juaanp.barehanded.platform.Services;
import dev.juaanp.barehanded.util.AssemblyBehaviorHelper;
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
    private static boolean wasAltDownLastTick = false;

    public static boolean isActionDown(net.minecraft.client.KeyMapping mapping) {
        if (mapping.isDown()) return true;
        if (mapping.isUnbound()) return false;

        com.mojang.blaze3d.platform.InputConstants.Key key = com.mojang.blaze3d.platform.InputConstants.getKey(mapping.saveString());
        long window = Minecraft.getInstance().getWindow().getWindow();

        if (key.getType() == com.mojang.blaze3d.platform.InputConstants.Type.MOUSE) {
            return org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } else {
            return org.lwjgl.glfw.GLFW.glfwGetKey(window, key.getValue()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        }
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            if (ClientGrabSession.isHoldingGrab) Services.NETWORK.sendStopGrabbingRequest();
            ClientGrabSession.forceResetAndNotify();
            ClientAssemblyTracker.reset();
            ClientInputTracker.suppressMiningUntilRelease = false;
            ClientInputTracker.suppressUseUntilRelease = false;
            lastLevel = null;
            lastPlayer = null;
            return;
        }

        ClientPayloadHandler.GRABBING_PLAYERS.removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);

        if (mc.level != lastLevel || mc.player != lastPlayer) {
            if (ClientGrabSession.isHoldingGrab) Services.NETWORK.sendStopGrabbingRequest();
            ClientGrabSession.forceResetAndNotify();
            ClientAssemblyTracker.reset();
            ClientInputTracker.suppressMiningUntilRelease = false;
            ClientInputTracker.suppressUseUntilRelease = false;
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

            double distanceDelta = 0.0;
            if (isActionDown(KeyBindings.PULL_KEY)) distanceDelta -= ClientConfig.INSTANCE.scrollDistanceSensitivity * 0.25;
            if (isActionDown(KeyBindings.PUSH_KEY)) distanceDelta += ClientConfig.INSTANCE.scrollDistanceSensitivity * 0.25;
            if (distanceDelta != 0.0) {
                Services.NETWORK.sendAdjustDistance(distanceDelta);
            }

            if (!ClientGrabSession.isWaitingForGrabSync && ClientGrabSession.grabbedSubLevelId != null) {
                SubLevelContainer container = SubLevelContainer.getContainer(mc.level);
                if (container == null || container.getSubLevel(ClientGrabSession.grabbedSubLevelId) == null) {
                    Services.NETWORK.sendStopGrabbingRequest();
                    ClientGrabSession.forceResetAndNotify();
                }
            }
        }

        if (mc.screen != null) {
            if (ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync) {
                Services.NETWORK.sendStopGrabbingRequest();
                ClientGrabSession.forceResetAndNotify();
            }
            ClientAssemblyTracker.reset();
            ClientInputTracker.suppressMiningUntilRelease = false;
            ClientInputTracker.suppressUseUntilRelease = false;
            return;
        }

        ClientAssemblyTracker.tickAssemblyTether(mc);

        boolean attackDown = isActionDown(mc.options.keyAttack);
        boolean useDown = isActionDown(mc.options.keyUse);
        boolean bothDown = attackDown && useDown;
        boolean eitherDown = attackDown || useDown;

        boolean isGrabbingOrAssembling = ClientAssemblyTracker.isActive() || ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync;

        if (isGrabbingOrAssembling && attackDown) {
            ClientInputTracker.suppressMiningUntilRelease = true;
        }

        if (isGrabbingOrAssembling && useDown) {
            ClientInputTracker.suppressUseUntilRelease = true;
        }

        if (!attackDown) {
            ClientInputTracker.suppressMiningUntilRelease = false;
        }

        if (!useDown) {
            ClientInputTracker.suppressUseUntilRelease = false;
        }

        if (ClientInputTracker.suppressMiningUntilRelease) {
            if (mc.gameMode != null) {
                mc.gameMode.stopDestroyBlock();
            }
        }

        if (ClientInputTracker.suppressUseUntilRelease) {
            mc.options.keyUse.setDown(false);
        }

        boolean grabKeyPressed = isActionDown(KeyBindings.GRAB_KEY);
        boolean isSneaking = mc.player.isShiftKeyDown();
        boolean isAltDown = isActionDown(KeyBindings.PIVOT_KEY);

        if (isAltDown != wasAltDownLastTick) {
            Services.NETWORK.sendAltStateToServer(isAltDown);
            wasAltDownLastTick = isAltDown;
        }

        boolean isGrabbingState = ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync || ClientAssemblyTracker.isActive();

        ClientInputTracker.tickDebounce(eitherDown, bothDown, grabKeyPressed);

        boolean isTryingToGrab = bothDown || ClientInputTracker.grabToggleActive;
        boolean isMaintainingGrab = eitherDown || ClientInputTracker.grabToggleActive;

        if (isTryingToGrab && !isGrabbingState && mc.player.getMainHandItem().isEmpty() && ClientInputTracker.canInitiateGrab()) {
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

                    boolean isSubLevel = Sable.HELPER.getContaining(mc.level, hitPos) != null;

                    if (isSubLevel) {
                        if (isSneaking && isAltDown && ServerConfig.INSTANCE.enableRipOffBlocks) {
                            ClientAssemblyTracker.tryStartAssembly(mc, blockHit, isSneaking, isAltDown);
                        } else {
                            Services.NETWORK.sendRequestGrab(currentPos);
                            ClientGrabSession.startWaiting();
                            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
                            ClientAssemblyTracker.reset();
                        }
                    } else if (isSneaking && ServerConfig.INSTANCE.enableBarehandedAssembly && distanceToHit <= ServerConfig.INSTANCE.barehandedAssemblyMaxDistance && !isIgnored && !preventDueToMining) {
                        ClientAssemblyTracker.tryStartAssembly(mc, blockHit, isSneaking, isAltDown);
                    }
                }
            }
        } else if (ClientAssemblyTracker.isActive()) {
            if (isMaintainingGrab) {
                ClientAssemblyTracker.tickCharge(mc, isSneaking);
            } else {
                ClientAssemblyTracker.reset();
            }
        } else if (!isMaintainingGrab && (ClientGrabSession.isHoldingGrab || ClientGrabSession.isWaitingForGrabSync)) {
            Services.NETWORK.sendStopGrabbingRequest();
            ClientGrabSession.forceResetAndNotify();
            ClientAssemblyTracker.reset();
        } else if (ClientGrabSession.isHoldingGrab && !mc.player.getMainHandItem().isEmpty()) {
            Services.NETWORK.sendStopGrabbingRequest();
            ClientGrabSession.forceResetAndNotify();
            ClientInputTracker.grabToggleActive = false;
        }

        boolean isRotateKeyDown = isActionDown(KeyBindings.ROTATE_KEY);
        if (ClientGrabSession.isHoldingGrab && (isRotateKeyDown || ClientInputTracker.pendingYaw != 0.0 || ClientInputTracker.pendingPitch != 0.0)) {
            boolean useCenter = ClientConfig.INSTANCE.rotateAroundCenter ^ isAltDown;
            Services.NETWORK.sendRotateGrab(ClientInputTracker.pendingYaw, ClientInputTracker.pendingPitch, useCenter);

            ClientInputTracker.pendingYaw = 0.0;
            ClientInputTracker.pendingPitch = 0.0;
        }

        boolean isDisassembleKeyDown = isActionDown(KeyBindings.DISASSEMBLE_KEY);
        if (ClientGrabSession.isHoldingGrab && isDisassembleKeyDown && !wasDisassembleKeyDown) {
            Services.NETWORK.sendDisassembleRequest(isAltDown);
        }
        wasDisassembleKeyDown = isDisassembleKeyDown;

        boolean currentAssemblyState = ClientAssemblyTracker.isActive();
        if (currentAssemblyState != wasAssemblingLastTick) {
            Services.NETWORK.sendAssemblyStateToServer(currentAssemblyState);
            wasAssemblingLastTick = currentAssemblyState;
        }
    }
}