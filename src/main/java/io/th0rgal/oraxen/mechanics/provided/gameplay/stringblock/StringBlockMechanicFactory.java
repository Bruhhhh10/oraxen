package io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingListener;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.sapling.SaplingTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringBlockMechanicFactory extends MechanicFactory {

    public static final Map<Integer, StringBlockMechanic> BLOCK_PER_VARIATION = new HashMap<>();
    private static JsonObject variants;
    private static StringBlockMechanicFactory instance;
    public final List<String> toolTypes;
    private boolean sapling;
    private static SaplingTask saplingTask;
    public final int saplingGrowthCheckDelay;

    public StringBlockMechanicFactory(ConfigurationSection section) {
        super(section);
        instance = this;
        variants = new JsonObject();
        variants.add("east=false,west=false,south=false,north=false,attached=false,disarmed=false,powered=false", getModelJson("block/barrier"));
        toolTypes = section.getStringList("tool_types");
        saplingGrowthCheckDelay = section.getInt("sapling_growth_check_delay");
        sapling = false;
        // this modifier should be executed when all the items have been parsed, just
        // before zipping the pack
        OraxenPlugin.get().getResourcePack().addModifiers(getMechanicID(),
                packFolder ->
                        OraxenPlugin.get().getResourcePack()
                                .writeStringToVirtual("assets/minecraft/blockstates",
                                        "tripwire.json", getBlockstateContent())
        );
        MechanicsManager.registerListeners(OraxenPlugin.get(), new StringBlockMechanicListener(this));
        MechanicsManager.registerListeners(OraxenPlugin.get(), new SaplingListener());
    }

    public static JsonObject getModelJson(String modelName) {
        JsonObject content = new JsonObject();
        content.addProperty("model", modelName);
        return content;
    }

    public static String getBlockstateVariantName(int id) {
        return "east=" + ((id & 1) == 1 ? "true" : "false")
                + ",west=" + (((id >> 1) & 1) == 1 ? "true" : "false")
                + ",south=" + (((id >> 2) & 1) == 1 ? "true" : "false")
                + ",north=" + (((id >> 3) & 1) == 1 ? "true" : "false")
                + ",attached=" + (((id >> 4) & 1) == 1 ? "true" : "false")
                + ",disarmed=" + (((id >> 5) & 1) == 1 ? "true" : "false")
                + ",powered=" + (((id >> 6) & 1) == 1 ? "true" : "false");
    }

    public static StringBlockMechanic getBlockMechanic(int customVariation) {
        return BLOCK_PER_VARIATION.get(customVariation);
    }

    public static StringBlockMechanicFactory getInstance() {
        return instance;
    }


    /**
     * Attempts to set the block directly to the model and texture of an Oraxen item.
     *
     * @param block  The block to update.
     * @param itemId The Oraxen item ID.
     */
    public static void setBlockModel(Block block, String itemId) {
        final MechanicFactory mechanicFactory = MechanicsManager.getMechanicFactory("stringblock");
        StringBlockMechanic stringBlockMechanic = (StringBlockMechanic) mechanicFactory.getMechanic(itemId);
        block.setBlockData(createTripwireData(stringBlockMechanic.getCustomVariation()), false);
    }

    private String getBlockstateContent() {
        JsonObject tripwire = new JsonObject();
        tripwire.add("variants", variants);
        return tripwire.toString();
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        StringBlockMechanic mechanic = new StringBlockMechanic(this, itemMechanicConfiguration);
        variants.add(getBlockstateVariantName(mechanic.getCustomVariation()),
                getModelJson(mechanic.getModel(itemMechanicConfiguration.getParent()
                        .getParent())));
        BLOCK_PER_VARIATION.put(mechanic.getCustomVariation(), mechanic);
        addToImplemented(mechanic);
        return mechanic;
    }

    public static int getCode(final Tripwire blockData) {
        final List<BlockFace> properties = Arrays
                .asList(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);
        int sum = 0;
        for (final BlockFace blockFace : blockData.getFaces())
            sum += (int) Math.pow(2, properties.indexOf(blockFace));
        if (blockData.isAttached())
            sum += (int) Math.pow(2, 4);
        if (blockData.isDisarmed())
            sum += (int) Math.pow(2, 5);
        if (blockData.isPowered())
            sum += (int) Math.pow(2, 6);
        return sum;
    }

    /**
     * Generate a Tripwire blockdata from its id
     *
     * @param code The block id.
     */
    public static BlockData createTripwireData(final int code) {
        Tripwire data = ((Tripwire) Bukkit.createBlockData(Material.TRIPWIRE));
        int i = 0;
        for (BlockFace face : new BlockFace[]{BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH,
                BlockFace.NORTH})
            data.setFace(face, (code & 0x1 << i++) != 0);
        data.setAttached((code & 0x1 << i++) != 0);
        data.setDisarmed((code & 0x1 << i++) != 0);
        data.setPowered((code & 0x1 << i) != 0);
        return data;
    }

    /**
     * Generate a NoteBlock blockdata from an oraxen id
     *
     * @param itemID The id of an item implementing NoteBlockMechanic
     */
    public BlockData createTripwireData(String itemID) {
        /* We have 16 instruments with 25 notes. All of those blocks can be powered.
         * That's: 16*25*2 = 800 variations. The first 25 variations of PIANO (not powered)
         * will be reserved for the vanilla behavior. We still have 800-25 = 775 variations
         */
        return createTripwireData(((StringBlockMechanic) getInstance().getMechanic(itemID)).getCustomVariation());
    }

    public void registerSaplingMechanic() {
        if (sapling) return;
        if (saplingTask != null) saplingTask.cancel();

        // Dont register if there is no sapling in configs
        if (OraxenItems.getItems().stream().filter(item ->
                ((StringBlockMechanic) StringBlockMechanicFactory.getInstance()
                        .getMechanic(OraxenItems.getIdByItem(item.build()))).isSapling()).toList().isEmpty()) return;

        saplingTask = new SaplingTask(saplingGrowthCheckDelay);
        saplingTask.runTaskTimer(OraxenPlugin.get(), 0, saplingGrowthCheckDelay);
        sapling = true;
    }
}