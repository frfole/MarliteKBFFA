package code.frfole.kb;

import code.frfole.kb.world.GameInstance;
import code.frfole.kb.world.zone.Flag;
import code.frfole.kb.world.zone.Zone;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityProjectile;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.ProjectileMeta;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.entity.projectile.ProjectileCollideWithEntityEvent;
import net.minestom.server.event.item.ItemUpdateStateEvent;
import net.minestom.server.inventory.TransactionOption;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class CombatListener {
    public static void onItemUpdateState(ItemUpdateStateEvent event) {
        if (event.getItemStack().material() == Material.BOW) {
            final Player player = event.getPlayer();
            long chargeTime = System.currentTimeMillis() - player.getTag(Tags.BOW_CHARGE_START);
            if (chargeTime <= 0) {
                player.getInventory().update();
                return;
            }
            final double chargedFor = Math.min(chargeTime, 1000) / 1000D;
            final double power = Math.max(Math.min((chargedFor * chargedFor + 2 * chargedFor) / 2D, 1), 0);

            if (power <= 0.2 || !(player.getInstance() instanceof GameInstance gameInstance)) {
                player.getInventory().update();
                return;
            }
            if (Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.SAFE, player.getPosition(), false)) {
                player.getInventory().update();
                return;
            }
            if (!player.getInventory().takeItemStack(ItemStack.of(Material.ARROW, 1), TransactionOption.ALL_OR_NOTHING)) {
                return;
            }

            final EntityProjectile projectile = new EntityProjectile(event.getPlayer(), EntityType.ARROW);
            projectile.scheduleRemove(Duration.ofSeconds(10));
            //noinspection UnstableApiUsage
            projectile.eventNode().addListener(ProjectileCollideWithEntityEvent.class, hitEvent -> {
                if (!(hitEvent.getTarget().getInstance() instanceof GameInstance gameInstance2)) return;
                if (Zone.flagValue(gameInstance2.zones.values(), Flag.FlagType.SAFE, hitEvent.getTarget().getPosition(), false)) {
                    if (hitEvent.getEntity().getEntityMeta() instanceof ProjectileMeta projectileMeta && projectileMeta.getShooter() instanceof Player shooter) {
                        shooter.getInventory().addItemStack(ItemStack.of(Material.ARROW, 1));
                    }
                    return;
                }
                if (hitEvent.getEntity().getEntityMeta() instanceof ProjectileMeta projectileMeta && projectileMeta.getShooter() != null) {
                    hitEvent.getTarget().setTag(Tags.LAST_HIT, new Tags.HitRecord(projectileMeta.getShooter()));
                }
                Vec direction = hitEvent.getEntity().getPosition().withPitch(0).direction();
                hitEvent.getTarget().takeKnockback((float) (power * 0.54), -direction.x(), -direction.z());
            });
            final Pos position = player.getPosition().add(0, player.getEyeHeight(), 0);

            projectile.setInstance(gameInstance, position);
            projectile.shoot(position.add(projectile.getPosition().direction()).sub(0, 0.2, 0), power * 2.1, 1.0);
        }

    }

    public static void onAttack(@NotNull EntityAttackEvent event) {
        if (event.getEntity() instanceof LivingEntity attacker) {
            ItemStack attackItem = attacker.getItemInMainHand();
            if (attackItem.material() == Material.STICK) {
                if (!(attacker.getInstance() instanceof GameInstance gameInstance)) return;
                if (Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.SAFE, attacker.getPosition(), false)
                        || Zone.flagValue(gameInstance.zones.values(), Flag.FlagType.SAFE, event.getTarget().getPosition(), false)) {
                    return;
                }
                Vec direction = attacker.getPosition().withPitch(0f).direction().neg();
                event.getTarget().setTag(Tags.LAST_HIT, new Tags.HitRecord(attacker.getUuid(), System.currentTimeMillis()));
                event.getTarget().takeKnockback(1.0f, direction.x(), direction.z());
            }
        }
    }
}
