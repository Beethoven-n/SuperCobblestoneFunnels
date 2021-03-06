package gd.rf.acro.scf;

import io.github.cottonmc.resources.BuiltinResources;
import io.github.cottonmc.resources.CottonResources;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class CobblestoneFunnelBlock extends Block {
    private int level;
    private int period;



    public CobblestoneFunnelBlock(Settings settings, int tlevel, int tperiod) {
        super(settings);
        level=tlevel;
        period=tperiod;

    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(2,1,2,14,16,14);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        super.scheduledTick(state, world, pos, random);
        if(hasLavaAndWater(pos,world) && world.getBlockState(pos.down()).isAir())
        {
            PacketByteBuf packetByteBuf = new PacketByteBuf(Unpooled.buffer());
            packetByteBuf.writeInt(1);
            PlayerStream.around(world,pos,5).forEach(pp->ServerSidePacketRegistry.INSTANCE.sendToPlayer(pp,SCF.SEND_SOUND,packetByteBuf));
            if(level==-1)
            {
                level=SCF.ORES.length;
            }
            long picked = RandomUtils.nextLong(0,countTotalWeights());
            System.out.println("picked: "+picked);
            world.setBlockState(pos.down(),Registry.BLOCK.get(Identifier.tryParse(getOreFromBigNumber(picked))).getDefaultState());
        }
        world.getBlockTickScheduler().schedule(pos,this,period);
    }

    private boolean hasLavaAndWater(BlockPos pos, World world)
    {
        boolean hasWater = false;
        boolean hasLava = false;
        if(FluidTags.WATER.contains(world.getBlockState(pos.east()).getFluidState().getFluid()))
        {
            hasWater=true;
        }
        if(FluidTags.LAVA.contains(world.getBlockState(pos.east()).getFluidState().getFluid()))
        {
            hasLava=true;
        }

        if(FluidTags.WATER.contains(world.getBlockState(pos.west()).getFluidState().getFluid()))
        {
            hasWater=true;
        }
        if(FluidTags.LAVA.contains(world.getBlockState(pos.west()).getFluidState().getFluid()))
        {
            hasLava=true;
        }

        if(FluidTags.WATER.contains(world.getBlockState(pos.south()).getFluidState().getFluid()))
        {
            hasWater=true;
        }
        if(FluidTags.LAVA.contains(world.getBlockState(pos.south()).getFluidState().getFluid()))
        {
            hasLava=true;
        }

        if(FluidTags.WATER.contains(world.getBlockState(pos.north()).getFluidState().getFluid()))
        {
            hasWater=true;
        }
        if(FluidTags.LAVA.contains(world.getBlockState(pos.north()).getFluidState().getFluid()))
        {
            hasLava=true;
        }


        return hasLava && hasWater;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        world.getBlockTickScheduler().schedule(pos,this,period);

    }

    @Override
    public void buildTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        super.buildTooltip(stack, world, tooltip, options);
        tooltip.add(new LiteralText("1 block every "+period/20f+" second(s)"));
        if(level==-1)
        {
            level=SCF.ORES.length;
        }
        tooltip.add(new LiteralText("best block out: "+Registry.BLOCK.get(Identifier.tryParse(SCF.ORES[level-1])).getName().getString()));
        tooltip.add(new LiteralText("(shift-use the block to see others!)"));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if(player.isSneaking() && hand==Hand.MAIN_HAND && world.isClient)
        {
            for (int i = 0; i < level; i++) {
                player.sendMessage(new LiteralText(Registry.BLOCK.get(Identifier.tryParse(SCF.ORES[i])).getName().getString()),false);
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    private long countTotalWeights()
    {
        long clap = 0;
        for (int i = 0; i < level; i++) {
            clap+=Integer.parseInt(SCF.WEIGHTS[i]);
        }
        System.out.println("total: "+clap);
        return clap+1; //to include the last number
    }
    private String getOreFromBigNumber(long bigNumber)
    {
        int counter =0;
        long inter = 0;
        while (inter<bigNumber)
        {
            inter+=Integer.parseInt(SCF.WEIGHTS[counter]);
            if(inter>bigNumber)
            {
                return SCF.ORES[counter];
            }
            counter++;
        }
        return SCF.ORES[counter];
    }
}
