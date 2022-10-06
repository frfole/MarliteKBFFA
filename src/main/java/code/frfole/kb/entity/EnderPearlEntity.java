package code.frfole.kb.entity;

import code.frfole.kb.Tags;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithBlockEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SetCooldownPacket;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class EnderPearlEntity extends EntityProjectile {

    @SuppressWarnings("UnstableApiUsage")
    public EnderPearlEntity(@NotNull Entity shooter) {
        super(shooter, EntityType.ENDER_PEARL);
        shooter.setTag(Tags.HAS_FLYING_PEARL, true);
        if (shooter instanceof Player player) {
            player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 200));
        }
        scheduleRemove(Duration.ofSeconds(10));
        eventNode().addListener(ProjectileCollideWithBlockEvent.class, EnderPearlEntity::onProjectileCollideBlock);
        eventNode().addListener(ProjectileCollideWithEntityEvent.class, EnderPearlEntity::onProjectileCollideEntity);
    }

    @Override
    public void remove() {
        Entity shooter = getShooter();
        if (shooter != null && !shooter.isRemoved()) {
            shooter.setTag(Tags.HAS_FLYING_PEARL, false);
            if (shooter instanceof Player player) {
                //noinspection UnstableApiUsage
                player.sendPacket(new SetCooldownPacket(Material.ENDER_PEARL.id(), 0));
            }
        }
        super.remove();
    }

    private static void onProjectileCollideEntity(@NotNull ProjectileCollideWithEntityEvent event) {
        if (event.getEntity().isRemoved()) return;
        Vec direction = event.getEntity().getPosition().direction();
        event.getTarget().takeKnockback(1f, direction.x(), -direction.z());
        teleportOwner(event.getCollisionPosition().add(0, 0.1, 0), event.getEntity());
        event.getEntity().remove();
    }

    private static void onProjectileCollideBlock(@NotNull ProjectileCollideWithBlockEvent event) {
        teleportOwner(event.getCollisionPosition().add(0, 0.1, 0), event.getEntity());
        event.getEntity().remove();
    }

    private static void teleportOwner(Pos pos, @NotNull Entity entity) {
        if (entity.isRemoved() || !(entity instanceof EntityProjectile projectile)) return;
        if (projectile.getShooter() instanceof Player player) {
            if (player.isOnline() && player.getInstance() == projectile.getInstance()) {
                player.setVelocity(Vec.ZERO);
                player.teleport(pos.add(0, 0.2, 0).withView(player.getPosition()));
            }
        }
    }
}
