package thaumcraft.common.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registry.ModBlockEntities;

import java.util.ArrayList;
import java.util.List;

/**
 * Хозяин и список допущенных — перенос TileOwned и TileArcanePressurePlate оригинала. Записи в списке начинаются с
 * цифры: «0» — железный ключ (можно пользоваться), «1» — золотой (можно ещё и выдавать ключи и менять настройки).
 * В 1.7.10 игроков узнавали по имени; здесь — по постоянному UUID, чтобы смена ника не лишала доступа.
 */
public class OwnedBlockEntity extends BlockEntity {
    public String owner = "";
    public final List<String> access = new ArrayList<>();
    /** Нажимная плита: 0 — срабатывает на всех, 1 — на всех, кроме своих, 2 — только на своих. */
    public byte setting;

    public OwnedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OWNED.get(), pos, state);
    }

    private static String id(Player player) {
        return player.getUUID().toString();
    }

    public void setOwner(Player player) {
        owner = id(player);
        sync();
    }

    public boolean isOwner(Player player) {
        return owner.equals(id(player));
    }

    /** Может ли игрок пользоваться (владелец или любой ключ). */
    public boolean allows(Player player) {
        return isOwner(player) || access.contains("0" + id(player)) || access.contains("1" + id(player));
    }

    /** Может ли игрок распоряжаться (владелец или золотой ключ). */
    public boolean manages(Player player) {
        return isOwner(player) || access.contains("1" + id(player));
    }

    public boolean hasAccess(Player player, int level) {
        return access.contains(level + id(player));
    }

    public void grant(Player player, int level) {
        access.add(level + id(player));
        sync();
    }

    /** Все, кто связан с блоком (владелец и допущенные), без цифры уровня. */
    public List<String> users() {
        List<String> users = new ArrayList<>();
        users.add(owner);
        for (String entry : access) {
            users.add(entry.substring(1));
        }
        return users;
    }

    /** Связан ли этот пользователь с блоком: владелец или в списке допущенных (на любом уровне). */
    public boolean knows(String user) {
        return owner.equals(user) || access.contains("0" + user) || access.contains("1" + user);
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("owner", owner);
        ListTag list = new ListTag();
        for (String entry : access) {
            list.add(StringTag.valueOf(entry));
        }
        tag.put("access", list);
        tag.putByte("setting", setting);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.getString("owner");
        access.clear();
        for (Tag entry : tag.getList("access", Tag.TAG_STRING)) {
            access.add(entry.getAsString());
        }
        setting = tag.getByte("setting");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
