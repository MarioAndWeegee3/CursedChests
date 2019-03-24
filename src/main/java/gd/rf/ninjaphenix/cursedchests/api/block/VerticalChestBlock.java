package gd.rf.ninjaphenix.cursedchests.api.block;

import gd.rf.ninjaphenix.cursedchests.api.block.entity.VerticalChestBlockEntity;
import gd.rf.ninjaphenix.cursedchests.api.item.ChestModifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.container.Container;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.VerticalEntityPosition;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sortme.ItemScatterer;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateFactory;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.TextComponent;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BoundingBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.List;

@SuppressWarnings("deprecation")
public abstract class VerticalChestBlock extends BlockWithEntity implements Waterloggable
{
	public enum VerticalChestType implements StringRepresentable
	{
		SINGLE("single"),
		TOP("top"),
		BOTTOM("bottom");

		private final String name;

		VerticalChestType(String string){ name = string; }

		public String asString(){ return name; }
	}

	interface someInterface<T>
	{
		T method_17465(VerticalChestBlockEntity var1, VerticalChestBlockEntity var2);
		T method_17464(VerticalChestBlockEntity var1);
	}

	private static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
	public static final DirectionProperty FACING = Properties.FACING_HORIZONTAL;
	public static final EnumProperty<VerticalChestType> TYPE = EnumProperty.create("type", VerticalChestType.class);
	private static final VoxelShape SINGLE_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);
	private static final VoxelShape TOP_SHAPE = Block.createCuboidShape(1, -16, 1, 15, 14, 15);
	private static final VoxelShape BOTTOM_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 30, 15);

	private static final someInterface<Inventory> inventoryCombiner = new someInterface<Inventory>()
	{
		@Override public Inventory method_17465(VerticalChestBlockEntity bottomChestBlockEntity, VerticalChestBlockEntity topChestBlockEntity){ return new DoubleInventory(bottomChestBlockEntity, topChestBlockEntity); }
		@Override public Inventory method_17464(VerticalChestBlockEntity chestBlockEntity){ return chestBlockEntity; }
	};

	private static final someInterface<TextComponent> displayNameCombiner = new someInterface<TextComponent>()
	{
		@Override public TextComponent method_17465(VerticalChestBlockEntity bottomChestBlockEntity, VerticalChestBlockEntity topChestBlockEntity)
		{
			if (bottomChestBlockEntity.hasCustomName()) return bottomChestBlockEntity.getDisplayName();
			if (topChestBlockEntity.hasCustomName()) return topChestBlockEntity.getDisplayName();
			return new TranslatableTextComponent("container.cursedchests.generic_double").append(bottomChestBlockEntity.getDisplayName());
		}

		@Override public TextComponent method_17464(VerticalChestBlockEntity chestBlockEntity){ return chestBlockEntity.getDisplayName(); }
	};

	public VerticalChestBlock(Settings settings)
	{
		super(settings);
		setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(WATERLOGGED, false).with(TYPE, VerticalChestType.SINGLE));
	}

	@Environment(EnvType.CLIENT) @Override public boolean hasBlockEntityBreakingRender(BlockState state){ return true; }
	@Override public BlockRenderType getRenderType(BlockState state){ return BlockRenderType.ENTITYBLOCK_ANIMATED; }
	@Override public FluidState getFluidState(BlockState state){ return state.get(WATERLOGGED) ? Fluids.WATER.getState(false) : super.getFluidState(state); }
	@Override protected void appendProperties(StateFactory.Builder<Block, BlockState> stateBuilder){ stateBuilder.with(FACING, TYPE, WATERLOGGED); }
	public abstract String getName();

	@Override public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, VerticalEntityPosition verticalEntityPosition)
	{
		switch(state.get(TYPE))
		{
			case TOP: return TOP_SHAPE;
			case BOTTOM: return BOTTOM_SHAPE;
			default: return SINGLE_SHAPE;
		}
	}

	@Override public BlockState getPlacementState(ItemPlacementContext context)
	{
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		VerticalChestType chestType_1 = VerticalChestType.SINGLE;
		Direction direction_1 = context.getPlayerHorizontalFacing().getOpposite();
		Direction direction_2 = context.getFacing();
		boolean sneaking = context.isPlayerSneaking();
		if (direction_2.getAxis().isVertical() && sneaking)
		{
			BlockState state = world.getBlockState(pos.offset(direction_2.getOpposite()));
			Direction direction_3 = state.getBlock() == this && state.get(TYPE) == VerticalChestType.SINGLE ? state.get(FACING) : null;
			if (direction_3 != null && direction_3.getAxis() != direction_2.getAxis()) chestType_1 = direction_2 == Direction.UP ? VerticalChestType.TOP : VerticalChestType.BOTTOM;
		}
		else if(!sneaking)
		{
			BlockState aboveBlockState = world.getBlockState(pos.offset(Direction.UP));
			if (aboveBlockState.getBlock() == this && aboveBlockState.get(TYPE) == VerticalChestType.SINGLE) chestType_1 = VerticalChestType.BOTTOM;
			else
			{
				BlockState belowBlockState = world.getBlockState(pos.offset(Direction.DOWN));
				if (belowBlockState.getBlock() == this && belowBlockState.get(TYPE) == VerticalChestType.SINGLE) chestType_1 = VerticalChestType.TOP;
			}
		}
		return getDefaultState().with(FACING, direction_1).with(TYPE, chestType_1).with(WATERLOGGED, world.getFluidState(pos).getFluid() == Fluids.WATER);
	}

	@Override public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState otherState, IWorld world, BlockPos pos, BlockPos otherPos)
	{

		if (state.get(WATERLOGGED)) world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		if (state.get(TYPE) == VerticalChestType.TOP && world.getBlockState(pos.offset(Direction.DOWN)).getBlock() != this) return state.with(TYPE, VerticalChestType.SINGLE);
		else if (state.get(TYPE) == VerticalChestType.BOTTOM && world.getBlockState(pos.offset(Direction.UP)).getBlock() != this) return state.with(TYPE, VerticalChestType.SINGLE);
		else if (state.get(TYPE) == VerticalChestType.SINGLE && direction.getAxis().isVertical())
		{
			BlockState realOtherState = world.getBlockState(pos.offset(direction));
			if (!realOtherState.contains(TYPE)) return state.with(TYPE, VerticalChestType.SINGLE);
			else if (direction == Direction.UP && realOtherState.get(TYPE) == VerticalChestType.TOP) return state.with(TYPE, VerticalChestType.BOTTOM);
			else if (direction == Direction.DOWN && realOtherState.get(TYPE) == VerticalChestType.BOTTOM) return state.with(TYPE, VerticalChestType.TOP);
			else return state.with(TYPE, VerticalChestType.SINGLE);
		}
		return super.getStateForNeighborUpdate(state, direction, otherState, world, pos, otherPos);
	}

	@Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		if (stack.hasDisplayName())
		{
			BlockEntity blockEntity_1 = world.getBlockEntity(pos);
			if (blockEntity_1 instanceof VerticalChestBlockEntity)((VerticalChestBlockEntity) blockEntity_1).setCustomName(stack.getDisplayName());
		}
	}

	@Override public void onBlockRemoved(BlockState state_1, World world, BlockPos pos, BlockState state_2, boolean boolean_1)
	{
		if (state_1.getBlock() != state_2.getBlock())
		{
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof Inventory)
			{
				ItemScatterer.spawn(world, pos, (Inventory) blockEntity);
				world.updateHorizontalAdjacent(pos, this);
			}
			super.onBlockRemoved(state_1, world, pos, state_2, boolean_1);
		}
	}

	@Override public boolean activate(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult)
	{
		if (world.isClient) return true;
		if (player.getStackInHand(hand).getItem() instanceof ChestModifier) return true;
		TextComponent containerName = method_17459(state, world, pos, displayNameCombiner);
		ContainerProviderRegistry.INSTANCE.openContainer(new Identifier("cursedchests", "scrollcontainer"), player, (packetByteBuf ->
		{
			packetByteBuf.writeBlockPos(pos);
			packetByteBuf.writeTextComponent(containerName);
		}));
		player.incrementStat(getOpenStat());
		return true;
	}

	// todo: maybe add custom stats for each chest type
	private Stat<Identifier> getOpenStat(){ return Stats.CUSTOM.getOrCreateStat(Stats.OPEN_CHEST); }

	private static <T> T method_17459(BlockState state_1, IWorld world, BlockPos pos_1, someInterface<T> var_1)
	{
		BlockEntity blockEntity_1 = world.getBlockEntity(pos_1);
		if (!(blockEntity_1 instanceof VerticalChestBlockEntity)) return null;
		else if (isChestBlocked(world, pos_1)) return null;
		else
		{
			VerticalChestBlockEntity chestBlockEntity1 = (VerticalChestBlockEntity) blockEntity_1;
			VerticalChestType chestType_1 = state_1.get(TYPE);
			if (chestType_1 == VerticalChestType.SINGLE) return var_1.method_17464(chestBlockEntity1);
			else
			{
				BlockPos pos_2;
				if (chestType_1 == VerticalChestType.TOP) pos_2 = pos_1.offset(Direction.DOWN);
				else pos_2 = pos_1.offset(Direction.UP);
				BlockState state_2 = world.getBlockState(pos_2);
				if (state_2.getBlock() == state_1.getBlock())
				{
					VerticalChestType chestType_2 = state_2.get(TYPE);
					if (chestType_2 != VerticalChestType.SINGLE && chestType_1 != chestType_2 && state_2.get(FACING) == state_1.get(FACING))
					{
						if (isChestBlocked(world, pos_2)) return null;
						BlockEntity blockEntity_2 = world.getBlockEntity(pos_2);
						if (blockEntity_2 instanceof VerticalChestBlockEntity)
						{
							VerticalChestBlockEntity chestBlockEntity_2 = chestType_1 == VerticalChestType.TOP ? chestBlockEntity1 : (VerticalChestBlockEntity)blockEntity_2;
							VerticalChestBlockEntity chestBlockEntity_3 = chestType_1 == VerticalChestType.TOP ? (VerticalChestBlockEntity)blockEntity_2 : chestBlockEntity1;
							return var_1.method_17465(chestBlockEntity_2, chestBlockEntity_3);
						}
					}
				}
				return var_1.method_17464(chestBlockEntity1);
			}
		}
	}

	public static Inventory createCombinedInventory(BlockState state, World world, BlockPos pos){ return method_17459(state, world, pos, inventoryCombiner); }

	private static boolean isChestBlocked(IWorld world, BlockPos pos)
	{
		return hasBlockOnTop(world, pos) || hasOcelotOnTop(world, pos);
	}

	private static boolean hasBlockOnTop(BlockView view, BlockPos pos)
	{
		BlockPos blockPos_2 = pos.up();
		return view.getBlockState(blockPos_2).isSimpleFullBlock(view, blockPos_2);
	}

	private static boolean hasOcelotOnTop(IWorld world, BlockPos pos){
		List<CatEntity> cats = world.method_18467(CatEntity.class, new BoundingBox(pos.getX(),pos.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1));
		if (!cats.isEmpty()) for (CatEntity catEntity_1 : cats) if (catEntity_1.isSitting()) return true;
		return false;
	}

	@Override public boolean hasComparatorOutput(BlockState state){ return true; }

	@Override public int getComparatorOutput(BlockState state, World world, BlockPos pos)
	{
		return Container.calculateComparatorOutput(createCombinedInventory(state, world, pos));
	}

	@Override public BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.with(FACING, rotation.rotate(state.get(FACING)));
	}

	@Override public BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.rotate(mirror.getRotation(state.get(FACING)));
	}
}
