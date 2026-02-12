package cz.mcsworld.eroded.death;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ErodedCompassItem extends Item {

    public ErodedCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {

        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

        ErodedDeathMemory mem =
                ErodedDeathStorage.get(sp.getUuid());

        if (mem == null || mem.isExpired(sp.getServer().getTicks())) {
            sp.sendMessage(
                    Text.translatable("eroded.compass.empty"),
                    true
            );
            return ActionResult.CONSUME;
        }

        long ticks = mem.getRemainingTicks(sp.getServer().getTicks());
        long seconds = ticks / 20;

        long min = seconds / 60;
        long sec = seconds % 60;

        String timeString = String.format("%02d:%02d", min, sec);

        sp.sendMessage(
                Text.translatable(
                        "eroded.compass.whisper",
                        timeString
                ), false );
        BlockPos pos = mem.getDeathPos();
        sp.sendMessage(
                Text.translatable(
                        "eroded.compass.whisper.coords",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                ).formatted(Formatting.DARK_GRAY),
                false
        );

        return ActionResult.CONSUME;
    }

}